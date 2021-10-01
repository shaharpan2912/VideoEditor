/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow strict-local
 */

import React, { useState, } from 'react';
import type { Node } from 'react';
import * as RNFS from 'react-native-fs';
import Video from 'react-native-video';
import { TouchableOpacity, Dimensions, Platform, Button } from 'react-native';
import { LogLevel, RNFFprobe, RNFFmpeg, RNFFmpegConfig } from 'react-native-ffmpeg';
import MediaMeta from 'react-native-media-meta';
import * as ImagePicker from 'react-native-image-picker';
// import RNFetchBlob from 'rn-fetch-blob';
// import * as RNGRP from 'react-native-get-real-path';
import { executeFFmpeg, getMediaInformation, executeFFmpegAsync } from './src/react-native-ffmpeg-api-wrapper';

import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
  Alert
} from 'react-native';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

// RNFFmpeg.setFontDirectory(['/system/fonts/']);
const { width: screenWidth, height: screenHeight } = Dimensions.get('window');
const directoryPath = RNFS.TemporaryDirectoryPath;
let downloaDirectoryPath = RNFS.DownloadDirectoryPath;

//Users/dipakbhoot/Library/Developer/CoreSimulator/Devices/CC405925-114D-4B14-B10F-08BA87F4391A/data/Containers/Bundle/Application/AB374989-D17F-4F8D-A82E-C76044592508/VideoEditor.app/OpenSans-Italic.ttf

if (Platform.OS === 'ios') {
  downloaDirectoryPath = RNFS.TemporaryDirectoryPath;
  console.log("RNFS.MainBundlePath", RNFS.MainBundlePath)
  RNFS.readDir(RNFS.MainBundlePath) // On Android, use "RNFS.DocumentDirectoryPath" (MainBundlePath is not defined)
    .then((result) => {
      console.log('GOT RESULT', result);

      //   // stat the first file
      //   return Promise.all([RNFS.stat(result[0].path), result[0].path]);
      // })
      // .then((statResult) => {
      //   if (statResult[0].isFile()) {
      //     // if we have a file, read it
      //     return RNFS.readFile(statResult[1], 'utf8');
      //   }

      //   return 'no file';
      // })
      // .then((contents) => {
      //   // log the file contents
      //   console.log(contents);
    })
    .catch((err) => {
      console.log(err.message, err.code);
    });
}
const font = 'OpenSans-ExtraBold';
const fontFile = `${font}.ttf`
if (Platform.OS === 'android') {
  RNFS.copyFileAssets(`fonts/${fontFile}`, `${RNFS.DocumentDirectoryPath}/${fontFile}`)
    .then(data => {
      RNFFmpegConfig.setFontDirectory(RNFS.DocumentDirectoryPath);
    })
    .catch(e => {
      console.log("e", e);
    })
} else {
  RNFFmpegConfig.setFontDirectory(`${RNFS.MainBundlePath}`);
}

const androidFontPath = '/system/fonts/DroidSans.ttf';
const iosFontPath = `${RNFS.MainBundlePath}assets/fonts/${fontFile}`;
// const iosFontPath = `./assets/fonts/OpenSans-Regular.ttf`;

let fontPath = androidFontPath;
if (Platform.OS === 'ios') {
  fontPath = iosFontPath
}
// RNFFmpegConfig.setFontDirectory('./assets/fonts', null);
const Section = ({ children, title }): Node => {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
};

const App: () => Node = () => {
  const [video, setVideo] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const isDarkMode = useColorScheme() === 'dark';
  let videoUrl = null;
  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  // const executeFfmpeg = () => {
  //   RNFFmpeg.execute('-i file1.mp4 -c:v mpeg4 file2.mp4').then(result => console.log(`FFmpeg process exited with rc=${result}.`));
  // }

  selectVideo = async () => {
    try {
      ImagePicker.launchImageLibrary({ mediaType: 'video', includeBase64: true, selectionLimit: 100 }, async (response) => {
        console.log(response);
        setVideo(null);
        if (response.didCancel === true) {
          return;
        }
        if (response.assets.length < 2) {
          Alert.alert(
            "Error",
            "Please select two videos",
            [
              {
                text: "Cancel",
                onPress: () => console.log("Cancel Pressed"),
                style: "cancel"
              },
              { text: "OK", onPress: () => console.log("OK Pressed") }
            ]
          );
          return;
        }
        // ffmpeg -i output1.mp4 -c copy -bsf:v h264_mp4toannexb -f mpegts fileIntermediate1.ts
        const files = [];
        setIsLoading(true);
        try {
          await RNFS.unlink(`${directoryPath}/temp0.mp4`);
          await RNFS.unlink(`${directoryPath}/temp1.mp4`);
        } catch (e) {
          console.log("e", e);
        }
        for (let i = 0; i < response.assets.length; i++) {
          const currentData = response.assets[i];
          let fileLocation = await getFileUrl(currentData.uri, `${directoryPath}/temp${i}.mp4`);
          // const getVideoHeight = `-v error -show_entries stream=width,height -of json=compact=1 ${fileLocation}`
          // const statistics = await executeFFprobe(getVideoHeight);
          const mediaInfo = await MediaMeta.get(fileLocation);
          const { height, width } = mediaInfo;
          // const command = `-i ${fileLocation}  -codec:a copy -preset ultrafast -y ${directoryPath}/temp${i}.mp4`;
          // const result = await executeFFmpeg(command);
          // fileLocation = `${directoryPath}/temp${i}.mp4`;
          // const statistics = await getMediaInformation(fileLocation);
          // // const allProperty = await statistics.getAllProperties();
          // const videoStats = await statistics.getStreams();
          // // const videoStream = await videoStats[1].getAllProperties();
          // // console.log("typeof videoStats", typeof videoStats);
          // // const height = videoStats.getAllProperties();
          // // const width = videoStats.width;
          // let height = 0;
          // let width = 0;
          // for (let j = 0; j < videoStats.length; j++) {
          //   const stream = videoStats[j];
          //   const properties = await stream.getAllProperties();
          //   if (properties.height) {
          //     height = properties.height;
          //     width = properties.width;
          //   }
          // }

          const fileObj = {
            location: fileLocation,
            originalHeight: height,
            originalWidth: width,
            height,
            width,
            sar: width / height,
            isLandscape: width < height ? true : false
          }
          files.push(fileObj);
        }

        let scaleHeight = null;
        let scaleWidth = null;

        if (files[0].originalWidth > files[1].originalWidth && files[0].originalHeight > files[1].originalHeight) {
          files[0].height = files[1].originalHeight;
          files[0].width = Math.floor(files[0].height * files[0].sar)
          files[1].addPad = true;
        } else if (files[0].originalWidth > files[1].originalWidth && files[0].originalHeight < files[1].originalHeight) {
          files[1].height = files[0].originalHeight;
          files[1].width = Math.floor(files[1].height * files[1].sar);
          files[1].addPad = true;
        } else if (files[1].originalWidth > files[0].originalWidth && files[1].originalHeight < files[0].originalHeight) {
          files[0].height = files[1].originalHeight;
          files[0].width = Math.floor(files[0].height * files[0].sar);
          files[0].addPad = true;
        } else if (files[1].originalWidth > files[0].originalWidth && files[1].originalHeight > files[0].originalHeight) {
          files[1].height = files[0].originalHeight;
          files[1].width = Math.floor(files[1].height * files[1].sar);
          files[0].addPad = true;
        }
        scaleHeight = files[0].sar > files[1].sar ? files[0].height : files[1].height;
        scaleWidth = files[0].sar > files[1].sar ? files[0].width : files[1].width;
        let darValue = '10/16';
        let fontSize = 14;
        // let darValue = null;
        console.log("scaleHeight", scaleHeight);
        console.log("scaleWidth", scaleWidth);
        console.log("screenHeight", screenHeight);
        console.log("screenWidth", screenWidth);
        if (files[0].width > files[0].height || files[1].width > files[1].height) {
          darValue = '16/9'
          fontSize = 30;
        } else if (files[0].width < files[0].height && files[1].width < files[1].height) {
          darValue = '10/16';
          fontSize = 14;

        }
        // 65535/2733
        // -r 24000/1001 
        // force_original_aspect_ratio=decrease,
        // setdar=16/9
        // -b:v 2000k
        // -crf 24
        let command = `-i  ${files[0].location} -i ${files[1].location}  -benchmark  -preset ultrafast -r 24 -q 10 -filter_complex "`
        if (files[0].addPad) {
          command += `[0:v]scale=${files[0].width - 1}:${files[0].height - 1}:force_original_aspect_ratio=decrease,pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setsar=1,setdar=${darValue}[v0];`
        } else {
          command += `[0:v]scale=${files[0].width - 1}:${files[0].height - 1}:force_original_aspect_ratio=decrease,pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setsar=1,setdar=${darValue}[v0];`
        }
        if (files[1].addPad) {
          command += `[1:v]scale=${files[1].width - 1}:${files[1].height - 1}:force_original_aspect_ratio=decrease,pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setsar=1,setdar=${darValue}[v1];`
        } else {
          command += `[1:v]scale=${files[1].width - 1}:${files[1].height - 1}:force_original_aspect_ratio=decrease,pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setsar=1,setdar=${darValue}[v1];`
        }
        command += `[v0] [0:a] [v1] [1:a] concat=n=2:v=1:a=1 [v] [a];`
        // command += `[v]drawtext=fontfile=${fontPath}:text='Qwarke App for Scientist Community':enable='between(t,0,30)':fontcolor=black:fontsize=14:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2,
        // drawtext=fontfile=${fontPath}:text='Video merging is going on':enable='between(t,30,60)':fontcolor=black:fontsize=14:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2[vs]"`

        command += `[v]drawtext=font=${font}:text='Qwarke App for Scientist Community':enable='between(t,0,30)':fontcolor=black:fontsize=${fontSize}:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2,
        drawtext=font=${font}:text='Video merging is going on':enable='between(t,30,60)':fontcolor=black:fontsize=${fontSize}:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2[vs]"`

        command += ` -map "[vs]" -map "[a]" -y ${downloaDirectoryPath}/output1.mp4`
        console.log("command", command);
        console.log(JSON.stringify(files));

        const result = await executeFFmpeg(command);
        if (result === 1) {
          Alert.alert(
            "Error",
            "Error in video merging",
            [
              {
                text: "Cancel",
                onPress: () => console.log("Cancel Pressed"),
                style: "cancel"

              },
              { text: "OK", onPress: () => console.log("OK Pressed") }
            ]
          );
        }
        if (Platform.OS === 'ios') {
          await RNFS.moveFile(`${downloaDirectoryPath}/output1.mp4`, `${RNFS.DocumentDirectoryPath}/output1.mp4`)
          setVideo(`${RNFS.DocumentDirectoryPath}/output1.mp4`);
        } else {
          setVideo(`${downloaDirectoryPath}/output1.mp4`);
        }
        setIsLoading(false);

        for (let i = 0; i < response.assets.length; i++) {
          // RNFS.unlink(`${directoryPath}/fileIntermediate${i}.ts`);
          // console.log("file", `${directoryPath}/fileIntermediate${i}.ts deleted`);
          // const currentData = response.assets[i];
          // const command = `-i ${currentData.uri} -c copy -bsf:v h264_mp4toannexb -f mpegts ${directoryPath}/fileIntermediate${i}.ts`
          // executeFFmpeg(command);
        }
        let ffCommand = [``];
        // videoUrl = response.assets[0].uri;
        // setVideo(videoUrl);
        // this.setState({ video: response });
      })
    } catch (e) {
      Alert.alert(
        "Error",
        e.toString(),
        [
          {
            text: "Cancel",
            onPress: () => console.log("Cancel Pressed"),
            style: "cancel"
          },
          { text: "OK", onPress: () => console.log("OK Pressed") }
        ]
      );
    }
  }

  editVideo = () => {
    setVideo(`${downloaDirectoryPath}/output1.mp4`);
  }
  getFileUrl = async (url, path) => {

    await RNFS.copyFile(url, path);
    // const data = await RNFS.readFile(url, 'base64')
    // await RNFS.writeFile(path, data, 'utf8');
    // setVideo(path);
    console.log("path", path);
    return path
  }

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          {/* <Section title="Step One">
            Edit <Text style={styles.highlight}>App.js</Text> to change this
            screen and then come back to see your edits.
          </Section> */}
          <TouchableOpacity  ><Button style={{
            fontFamily: 'OpenSans-Regular',
            fontSize: 20,
            height: 50,
            width: 200
          }}
            onPress={() => selectVideo()}
            title="Select Video"
          ></Button>
          </TouchableOpacity>
          {video && <Video source={{ uri: video }}   // Can be a URL or a local file.
            ref={(ref) => {
              this.player = ref
            }}
            // Store reference
            // onBuffer={this.onBuffer}                // Callback when remote video is buffering
            // onError={this.videoError}               // Callback when video cannot be loaded
            style={{
              height: screenHeight,
              width: '100%',
              // position: 'absolute',
              // top: 0,
              // left: 0,
              // bottom: 0,
              // right: 0,
            }}
            fullscreen={true}
            resizeMode="contain"
            controls={true}
          />}
          {/* <Section title="See Your Changes">
            <ReloadInstructions />
          </Section> */}
          {/* <Section title="Debug">
            <DebugInstructions />
          </Section> */}
          {/* <TouchableOpacity onPress={() => editVideo()} ><Text>Edit Video</Text></TouchableOpacity> */}
          {isLoading && <Text>Video merging is in process....</Text>}
          {/* <Section title="Learn More">
            Read the docs to discover what to do next:
          </Section> */}
          {/* <LearnMoreLinks /> */}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;
