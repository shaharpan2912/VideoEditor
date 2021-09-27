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
import { TouchableOpacity, Dimensions, Platform } from 'react-native';
import { LogLevel, RNFFprobe, RNFFmpeg, RNFFmpegConfig } from 'react-native-ffmpeg';
import * as ImagePicker from 'react-native-image-picker';
// import RNFetchBlob from 'rn-fetch-blob';
// import * as RNGRP from 'react-native-get-real-path';
import { executeFFmpeg, getMediaInformation } from './src/react-native-ffmpeg-api-wrapper';

import {
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';

import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

// RNFFmpeg.setFontDirectory(['/system/fonts/']);
const { width, height } = Dimensions.get('window');
const directoryPath = RNFS.TemporaryDirectoryPath;
let downloaDirectoryPath = RNFS.DownloadDirectoryPath;
if (Platform.OS === 'ios') {
  downloaDirectoryPath = RNFS.DocumentDirectoryPath;
}
const androidFontPath = '/system/fonts/DroidSans.ttf';
const iosFontPath = `${RNFS.MainBundlePath}assets/fonts/OpenSans-Regular.ttf`;
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
    ImagePicker.launchImageLibrary({ mediaType: 'video', includeBase64: true, selectionLimit: 0 }, async (response) => {
      console.log(response);
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
        const fileLocation = await getFileUrl(currentData.uri, `${directoryPath}/temp${i}.mp4`);
        // const getVideoHeight = `-v error -show_entries stream=width,height -of json=compact=1 ${fileLocation}`
        // const statistics = await executeFFprobe(getVideoHeight);
        const statistics = await getMediaInformation(fileLocation);
        // const allProperty = await statistics.getAllProperties();
        const videoStats = await statistics.getStreams();
        // const videoStream = await videoStats[1].getAllProperties();
        // console.log("typeof videoStats", typeof videoStats);
        // const height = videoStats.getAllProperties();
        // const width = videoStats.width;
        let height = 0;
        let width = 0;
        for (let j = 0; j < videoStats.length; j++) {
          const stream = videoStats[j];
          const properties = await stream.getAllProperties();
          if (properties.height) {
            height = properties.height;
            width = properties.width;
          }
        }

        const fileObj = {
          location: fileLocation,
          originalHeight: height,
          originalWidth: width,
          height,
          width,
          sar: width / height
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
      // let darValue = null;
      console.log("scaleHeight", scaleHeight);
      console.log("scaleWidth", scaleWidth);
      if (files[0].width > files[0].height || files[1].width > files[1].height) {
        darValue = '16/9'
      } else if (files[0].width < files[0].height && files[1].width < files[1].height) {
        darValue = '10/16'
      }
      // 65535/2733
      // -r 24000/1001 
      // force_original_aspect_ratio=decrease,
      // setdar=16/9
      // -b:v 2000k 
      let command = `-i  ${files[0].location} -i ${files[1].location} -b:v 1000k -r 50  -filter_complex "`
      if (files[0].addPad) {
        command += `[0:v]scale=${files[0].width - 1}:${files[0].height - 1},pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setdar=${darValue}[v0];`
      } else {
        command += `[0:v]scale=${files[0].width - 1}:${files[0].height - 1},pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setdar=${darValue}[v0];`
      }
      if (files[1].addPad) {
        command += `[1:v]scale=${files[1].width - 1}:${files[1].height - 1},pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setdar=${darValue}[v1];`
      } else {
        command += `[1:v]scale=${files[1].width - 1}:${files[1].height - 1},pad=${scaleWidth}:${scaleHeight}:(${scaleWidth}-iw)/2:(${scaleWidth}-ih)/2:black,setdar=${darValue}[v1];`
      }
      command += `[v0] [0:a] [v1] [1:a] concat=n=2:v=1:a=1 [v] [a];`
      command += `[v]drawtext=fontfile=${fontPath}:text='Qwarke App for Scientist Community':enable='between(t,0,30)':fontcolor=black:fontsize=14:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2,
      drawtext=fontfile=${fontPath}:text='Video merging is going on':enable='between(t,30,60)':fontcolor=black:fontsize=14:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2[vs]"`

      // command += `[v]drawtext=fontfile=/system/fonts/DroidSans.ttf:text='Qwarke App for Scientist Community':enable='between(t,0,30)':fontcolor=black:fontsize=14:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2,
      //   drawtext=fontfile=/system/fonts/DroidSans.ttf:text='Video merging is going on':enable='between(t,30,60)':fontcolor=black:fontsize=14:box=1:boxcolor=white@0.5:boxborderw=5:x=(w-text_w)/2:y=(h-text_h)/2[vs]"`
      command += ` -map "[vs]" -map "[a]" -y ${downloaDirectoryPath}/output1.mp4`
      console.log("command", command);
      console.log(JSON.stringify(files));

      await executeFFmpeg(command);
      setIsLoading(false);
      setVideo(`${downloaDirectoryPath}/output1.mp4`)

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
        <Header />
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          {/* <Section title="Step One">
            Edit <Text style={styles.highlight}>App.js</Text> to change this
            screen and then come back to see your edits.
          </Section> */}
          <TouchableOpacity onPress={() => selectVideo()} ><Text>Select Video</Text></TouchableOpacity>
          {video && <Video source={{ uri: video }}   // Can be a URL or a local file.
            ref={(ref) => {
              this.player = ref
            }}                                      // Store reference
            // onBuffer={this.onBuffer}                // Callback when remote video is buffering
            // onError={this.videoError}               // Callback when video cannot be loaded
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              bottom: 0,
              right: 0,
            }}
          />}
          {/* <Section title="See Your Changes">
            <ReloadInstructions />
          </Section> */}
          {/* <Section title="Debug">
            <DebugInstructions />
          </Section> */}
          <TouchableOpacity onPress={() => selectVideo()} ><Text>Edit Video</Text></TouchableOpacity>
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