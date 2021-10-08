package com.videoeditor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

//import com.abedelazizshe.lightcompressorlibrary.CompressionListener;

//import com.abedelazizshe.lightcompressorlibrary.VideoCompressor;
//import com.abedelazizshe.lightcompressorlibrary.VideoQuality;
//import com.abedelazizshe.lightcompressorlibrary.config.Configuration;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.videoeditor.compressor.CompressionListener;
import com.videoeditor.compressor.VideoCompressor;
import com.videoeditor.compressor.VideoQuality;
import com.videoeditor.compressor.config.Configuration;
//import com.videoeditor.compressor.config.Configuration;

public class VideoEditorThread extends Thread{
    String srcPath;
    String destPath;
    int bitrate;
    Context context;
    Callback callback;

    public VideoEditorThread(String srcPath, String destPath,int bitrate,
                             Context context,Callback callback) {
        this.srcPath = srcPath;
        this.destPath=destPath;
        this.bitrate=bitrate;
        this.context=context;
        this.callback=callback;
    }

    public void run() {
        // compute primes larger than minPrime
            VideoQuality videoQuality = null;

            if (bitrate >= 25000000) {
                videoQuality = VideoQuality.VERY_LOW;
            } else if (bitrate >= 5000000) {
                videoQuality = VideoQuality.LOW;
            } else {
                videoQuality = VideoQuality.HIGH;
            }
            // if(width * height <= 640*360){
            // 	if(bitrate > 1200000){
            // 		bitrate=1200000;
            // 	}
            // } else if(width * height <= 960 *540){
            // 	if(bitrate > 1500000){
            // 		bitrate=1500000;
            // 	}
            // } else if(width * height <= 1280 *720){
            // 	if(bitrate > 4000000){
            // 		bitrate=4000000;
            // 	}
            // } else {
            // 	if(bitrate > 4500000){
            // 		bitrate=4500000;
            // 	}
            // }
            // binding.textViewThird.setText(videoQuality.toString());
            VideoCompressor.start(context, // => This is required if srcUri is provided. If not,
                    // pass null.
                    null, // => Source can be provided as content uri, it requires context.
                    srcPath, // => This could be null if srcUri and context are provided.
                    destPath, null, /* String, or null */
                    new CompressionListener() {
                        @Override
                        public void onStart() {
                            // Compression start
                        }

                        @Override
                        public void onSuccess() {
                            // Log.i("progress", destPath);
//                            mHandler.post(new Runnable() {
//                                public void run() {
//                                    callback.invoke(destPath);
//                                    Toast toast = Toast.makeText(context, "video edit finish", Toast.LENGTH_LONG);
//                                    toast.show();
//                                }
//                            });
                            //
                            // On Compression success
                        }

                        @Override
                        public void onFailure(String failureMessage) {
                            // On Failure
                        }

                        @Override
                        public void onProgress(float v) {
                            Log.i("progress", Float.toString(v));

//                            mHandler.post(new Runnable() {
//                                public void run() {
//                                    // binding.textviewFirst.setText(Float.toString(v));
//                                    // Toast toast = Toast.makeText(context,Float.toString(v),Toast.LENGTH_SHORT);
//                                    // toast.show();
//                                }
//                            });
                            // Update UI with progress value
                            // runOnUiThread(new Runnable() {
                            // public void run() {
                            // progress.setText(progressPercent + "%");
                            // progressBar.setProgress((int) progressPercent);
                            // }
                            // });
                        }

                        @Override
                        public void onCancelled() {
                            // On Cancelled
                        }
                    }, new Configuration(videoQuality, null, /* frameRate: int, or null */
                            false, null /* videoBitrate: int, or null */
                    ));
        }


}
