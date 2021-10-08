package com.videoeditor;

import static com.facebook.yoga.YogaStyleInputs.MIN_HEIGHT;
import static com.facebook.yoga.YogaStyleInputs.MIN_WIDTH;

import com.abedelazizshe.lightcompressorlibrary.CompressionListener;
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor;
import com.abedelazizshe.lightcompressorlibrary.VideoQuality;
import com.abedelazizshe.lightcompressorlibrary.config.Configuration;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import com.reactnativevideohelper.video.MP4Builder;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;

//import com.videoeditor.BuildVars;
////import org.telegram.messenger.FileLog;
//import org.telegram.messenger.MediaController;
//import org.telegram.messenger.VideoEditedInfo;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import java.io.File;
import java.util.Date;

public class VideoEditor extends ReactContextBaseJavaModule {
	// constructor
	private MP4Builder mediaMuxer;
	private MediaExtractor extractor;

	private long endPresentationTime;

//	private MediaController.VideoConvertorListener callback;
	Handler mHandler = new Handler(Looper.getMainLooper());

	public VideoEditor(ReactApplicationContext reactContext) {
		super(reactContext);
	}

	// Mandatory function getName that specifies the module name
	@Override
	public String getName() {
		return "VideoEditor";
	}

	// Custom function that we are going to export to JS
	@ReactMethod
	public String getPath(String uri) {
		String[] projection = { MediaStore.Images.Media.DATA };
		// Toast.makeText(this.getContext(), uri.toString(), Toast.LENGTH_LONG);
		Log.i("uri", uri.toString());
		Cursor cursor = this.getReactApplicationContext().getContentResolver().query(Uri.parse(uri), projection, null,
				null, null);
		Log.i("cursor", cursor.toString());
		if (cursor != null) {
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			Log.i("column_index", Integer.toString(column_index));
			cursor.moveToFirst();
			Log.i("column count", Integer.toString(cursor.getColumnCount()));
			Log.i("file path", "message" + cursor.getString(column_index));
			return cursor.getString(column_index);
		} else
			return null;
	}

	@ReactMethod
	public void compressVideo(String uri, Callback callback) {
		// Toast.makeText(this.getContext(), "Video selected", Toast.LENGTH_LONG);
		Log.i("video", uri);
		String selectedVideoPath = getPath(uri);
		Log.i("selectedVideoPath", selectedVideoPath);
		MediaMetadataRetriever retriever = new MediaMetadataRetriever();
		retriever.setDataSource(selectedVideoPath);
		int width = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
		int height = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
		int bitrate = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
		int duration = Integer.valueOf(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		if(bitrate < 2000000){
			callback.invoke(selectedVideoPath);
			return;
		}
		Log.i("width", String.valueOf(width));
		Log.i("height", String.valueOf(height));
		Log.i("duration", String.valueOf(duration));
		// binding.textViewTwo.setText(Integer.toString(bitrate));
		// Log.i("width", Integer.toString(width));
		// Log.i("height", Integer.toString(height));
		retriever.release();

		int newWidth = 0;
		int newHeight = 0;
		if (width >= 1920 || height >= 1920) {
			// newWidth = (int) (((width * 0.2) / 16)* 16);
			// newHeight = (int) (((height * 0.2) / 16f) * 16);
			newWidth = (int) (((width * 0.5) / 16f)) * 16;
			newHeight = (int) (((height * 0.5) / 16f)) * 16;
		} else if (width >= 1280 || height >= 1280) {
			newWidth = (int) (((width * 0.75) / 16f)) * 16;
			newHeight = (int) (((height * 0.75) / 16f)) * 16;
		} else if (width >= 960 || height >= 960) {
			newWidth = (int) (((MIN_WIDTH * 0.95) / 16f)) * 16;
			newHeight = (int) (((MIN_HEIGHT * 0.95) / 16f)) * 16;
		} else {
			newWidth = (int) (((width * 0.9) / 16f)) * 16;
			newHeight = (int) (((height * 0.9) / 16f)) * 16;
		}
		try {
			if (selectedVideoPath == null) {
				// Log.e("selected video path = null!");
				// finish();
			} else {
				File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
				// Log.i("download path", downloadDir.getPath());
				String destFilePath = String.valueOf(new Date().getTime());
				compressVideoLightCompressor(selectedVideoPath, downloadDir.getPath() +"/"+ destFilePath+".mp4", newWidth,
						newHeight, bitrate, this.getReactApplicationContext(),callback);
				// compressVideoMediaCodec(selectedVideoPath,
				// downloadDir.getPath()+"/temp1.mp4",newWidth,newHeight,bitrate);
				// compressVideo(selectedVideoPath,
				// downloadDir.getPath()+"/temp1.mp4",newWidth,newHeight,bitrate);
				/**
				 * try to do something there selectedVideoPath is path to the selected video
				 */
			}
		} catch (Exception e) {
			// #debug
			e.printStackTrace();
		}

		// finish();
	}

	public void compressVideoLightCompressor(String srcPath, String destPath, int width, int height, int bitrate,
			Context context,Callback callback) {
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
		VideoCompressor.start(this.getReactApplicationContext(), // => This is required if srcUri is provided. If not,
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
						mHandler.post(new Runnable() {
							public void run() {
								callback.invoke(destPath);
								Toast toast = Toast.makeText(context, "video edit finish", Toast.LENGTH_LONG);
								toast.show();
							}
						});
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

						mHandler.post(new Runnable() {
							public void run() {
								// binding.textviewFirst.setText(Float.toString(v));
								// Toast toast = Toast.makeText(context,Float.toString(v),Toast.LENGTH_SHORT);
								// toast.show();
							}
						});
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

//	@TargetApi(18)
//	private boolean convertVideoInternal(String videoPath, File cacheFile,
//										 int rotationValue, boolean isSecret,
//										 int resultWidth, int resultHeight,
//										 int framerate, int bitrate, int originalBitrate,
//										 long startTime, long endTime, long avatarStartTime,
//										 long duration,
//										 boolean needCompress, boolean increaseTimeout,
//										 MediaController.SavedFilterState savedFilterState,
//										 String paintPath,
//										 ArrayList<VideoEditedInfo.MediaEntity> mediaEntities,
//										 boolean isPhoto,
//										 MediaController.CropState cropState) {
//
//		boolean error = false;
//		boolean repeatWithIncreasedTimeout = false;
//		int videoTrackIndex = -5;
//
//		try {
//			MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//			com.reactnativevideohelper.video.Mp4Movie movie = new com.reactnativevideohelper.video.Mp4Movie();
//			movie.setCacheFile(cacheFile);
//			movie.setRotation(0);
//			movie.setSize(resultWidth, resultHeight);
//			mediaMuxer = new com.reactnativevideohelper.video.MP4Builder().createMovie(movie, isSecret);
//
//			long currentPts = 0;
//			float durationS = duration / 1000f;
//			MediaCodec encoder = null;
//			InputSurface inputSurface = null;
//			OutputSurface outputSurface = null;
//			int prependHeaderSize = 0;
//			endPresentationTime = duration * 1000;
//			checkConversionCanceled();
//
//			if (isPhoto) {
//				try {
//					boolean outputDone = false;
//					boolean decoderDone = false;
//					int framesCount = 0;
//
//					if (avatarStartTime >= 0) {
//						if (durationS <= 2000) {
//							bitrate = 2600000;
//						} else if (durationS <= 5000) {
//							bitrate = 2200000;
//						} else {
//							bitrate = 1560000;
//						}
//					} else if (bitrate <= 0) {
//						bitrate = 921600;
//					}
//
//					if (resultWidth % 16 != 0) {
//						if (BuildVars.LOGS_ENABLED) {
////							FileLog.d("changing width from " + resultWidth + " to " + Math.round(resultWidth / 16.0f) * 16);
//						}
//						resultWidth = Math.round(resultWidth / 16.0f) * 16;
//					}
//					if (resultHeight % 16 != 0) {
//						if (BuildVars.LOGS_ENABLED) {
////							FileLog.d("changing height from " + resultHeight + " to " + Math.round(resultHeight / 16.0f) * 16);
//						}
//						resultHeight = Math.round(resultHeight / 16.0f) * 16;
//					}
//
//					if (BuildVars.LOGS_ENABLED) {
////						FileLog.d("create photo encoder " + resultWidth + " " + resultHeight + " duration = " + duration);
//					}
//
//					MediaFormat outputFormat = MediaFormat.createVideoFormat(MediaController.VIDEO_MIME_TYPE, resultWidth, resultHeight);
//					outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//					outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
//					outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
//					outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
//
//					encoder = MediaCodec.createEncoderByType(MediaController.VIDEO_MIME_TYPE);
//					encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//					inputSurface = new com.reactnativevideohelper.video.InputSurface(encoder.createInputSurface());
//					inputSurface.makeCurrent();
//					encoder.start();
//
//					outputSurface = new com.reactnativevideohelper.video.OutputSurface(savedFilterState, videoPath, paintPath, mediaEntities, null, resultWidth, resultHeight, rotationValue, framerate, true);
//
//					ByteBuffer[] encoderOutputBuffers = null;
//					ByteBuffer[] encoderInputBuffers = null;
//					if (Build.VERSION.SDK_INT < 21) {
//						encoderOutputBuffers = encoder.getOutputBuffers();
//					}
//
//					boolean firstEncode = true;
//
//					checkConversionCanceled();
//
//					while (!outputDone) {
//						checkConversionCanceled();
//
//						boolean decoderOutputAvailable = !decoderDone;
//						boolean encoderOutputAvailable = true;
//						while (decoderOutputAvailable || encoderOutputAvailable) {
//							checkConversionCanceled();
//							int encoderStatus = encoder.dequeueOutputBuffer(info, increaseTimeout ? MEDIACODEC_TIMEOUT_INCREASED : MEDIACODEC_TIMEOUT_DEFAULT);
//							if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//								encoderOutputAvailable = false;
//							} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//								if (Build.VERSION.SDK_INT < 21) {
//									encoderOutputBuffers = encoder.getOutputBuffers();
//								}
//							} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//								MediaFormat newFormat = encoder.getOutputFormat();
//								if (BuildVars.LOGS_ENABLED) {
////									FileLog.d("photo encoder new format " + newFormat);
//								}
//								if (videoTrackIndex == -5 && newFormat != null) {
//									videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
//									if (newFormat.containsKey(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES) && newFormat.getInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES) == 1) {
//										ByteBuffer spsBuff = newFormat.getByteBuffer("csd-0");
//										ByteBuffer ppsBuff = newFormat.getByteBuffer("csd-1");
//										prependHeaderSize = spsBuff.limit() + ppsBuff.limit();
//									}
//								}
//							} else if (encoderStatus < 0) {
//								throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
//							} else {
//								ByteBuffer encodedData;
//								if (Build.VERSION.SDK_INT < 21) {
//									encodedData = encoderOutputBuffers[encoderStatus];
//								} else {
//									encodedData = encoder.getOutputBuffer(encoderStatus);
//								}
//								if (encodedData == null) {
//									throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
//								}
//								if (info.size > 1) {
//									if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
//										if (prependHeaderSize != 0 && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//											info.offset += prependHeaderSize;
//											info.size -= prependHeaderSize;
//										}
//										if (firstEncode && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//											if (info.size > 100) {
//												encodedData.position(info.offset);
//												byte[] temp = new byte[100];
//												encodedData.get(temp);
//												int nalCount = 0;
//												for (int a = 0; a < temp.length - 4; a++) {
//													if (temp[a] == 0 && temp[a + 1] == 0 && temp[a + 2] == 0 && temp[a + 3] == 1) {
//														nalCount++;
//														if (nalCount > 1) {
//															info.offset += a;
//															info.size -= a;
//															break;
//														}
//													}
//												}
//											}
//											firstEncode = false;
//										}
//										long availableSize = mediaMuxer.writeSampleData(videoTrackIndex, encodedData, info, true);
//										if (availableSize != 0) {
//											if (callback != null) {
//												callback.didWriteData(availableSize, (currentPts / 1000f) / durationS);
//											}
//										}
//									} else if (videoTrackIndex == -5) {
//										byte[] csd = new byte[info.size];
//										encodedData.limit(info.offset + info.size);
//										encodedData.position(info.offset);
//										encodedData.get(csd);
//										ByteBuffer sps = null;
//										ByteBuffer pps = null;
//										for (int a = info.size - 1; a >= 0; a--) {
//											if (a > 3) {
//												if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
//													sps = ByteBuffer.allocate(a - 3);
//													pps = ByteBuffer.allocate(info.size - (a - 3));
//													sps.put(csd, 0, a - 3).position(0);
//													pps.put(csd, a - 3, info.size - (a - 3)).position(0);
//													break;
//												}
//											} else {
//												break;
//											}
//										}
//
//										MediaFormat newFormat = MediaFormat.createVideoFormat(MediaController.VIDEO_MIME_TYPE, resultWidth, resultHeight);
//										if (sps != null && pps != null) {
//											newFormat.setByteBuffer("csd-0", sps);
//											newFormat.setByteBuffer("csd-1", pps);
//										}
//										videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
//									}
//								}
//								outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
//								encoder.releaseOutputBuffer(encoderStatus, false);
//							}
//							if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
//								continue;
//							}
//
//							if (!decoderDone) {
//								outputSurface.drawImage();
//								long presentationTime = (long) (framesCount / 30.0f * 1000L * 1000L * 1000L);
//								inputSurface.setPresentationTime(presentationTime);
//								inputSurface.swapBuffers();
//								framesCount++;
//
//								if (framesCount >= duration / 1000.0f * 30) {
//									decoderDone = true;
//									decoderOutputAvailable = false;
//									encoder.signalEndOfInputStream();
//								}
//							}
//						}
//					}
//				} catch (Exception e) {
//					// in some case encoder.dequeueOutputBuffer return IllegalStateException
//					// stable reproduced on xiaomi
//					// fix it by increasing timeout
//					if (e instanceof IllegalStateException && !increaseTimeout) {
//						repeatWithIncreasedTimeout = true;
//					}
////					FileLog.e("bitrate: " + bitrate + " framerate: " + framerate + " size: " + resultHeight + "x" + resultWidth);
////					FileLog.e(e);
//					error = true;
//				}
//
//				if (outputSurface != null) {
//					outputSurface.release();
//				}
//				if (inputSurface != null) {
//					inputSurface.release();
//				}
//				if (encoder != null) {
//					encoder.stop();
//					encoder.release();
//				}
//				checkConversionCanceled();
//			} else {
//				extractor = new MediaExtractor();
//				extractor.setDataSource(videoPath);
//
//				int videoIndex = MediaController.findTrack(extractor, false);
//				int audioIndex = bitrate != -1 ? MediaController.findTrack(extractor, true) : -1;
//				boolean needConvertVideo = false;
//				if (videoIndex >= 0 && !extractor.getTrackFormat(videoIndex).getString(MediaFormat.KEY_MIME).equals(MediaController.VIDEO_MIME_TYPE)) {
//					needConvertVideo = true;
//				}
//
//				if (needCompress || needConvertVideo) {
//					AudioRecoder audioRecoder = null;
//					ByteBuffer audioBuffer = null;
//					boolean copyAudioBuffer = true;
//
//					if (videoIndex >= 0) {
//						MediaCodec decoder = null;
//
//						try {
//							long videoTime = -1;
//							boolean outputDone = false;
//							boolean inputDone = false;
//							boolean decoderDone = false;
//							int swapUV = 0;
//							int audioTrackIndex = -5;
//							long additionalPresentationTime = 0;
//							long minPresentationTime = Integer.MIN_VALUE;
//							long frameDelta = 1000 / framerate * 1000;
//
//							extractor.selectTrack(videoIndex);
//							MediaFormat videoFormat = extractor.getTrackFormat(videoIndex);
//
//							if (avatarStartTime >= 0) {
//								if (durationS <= 2000) {
//									bitrate = 2600000;
//								} else if (durationS <= 5000) {
//									bitrate = 2200000;
//								} else {
//									bitrate = 1560000;
//								}
//								avatarStartTime = 0;
//							} else if (bitrate <= 0) {
//								bitrate = 921600;
//							}
//							if (originalBitrate > 0) {
//								bitrate = Math.min(originalBitrate, bitrate);
//							}
//
//							long trueStartTime;// = startTime < 0 ? 0 : startTime;
//							if (avatarStartTime >= 0/* && trueStartTime == avatarStartTime*/) {
//								avatarStartTime = -1;
//							}
//
//							if (avatarStartTime >= 0) {
//								extractor.seekTo(avatarStartTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//							} else if (startTime > 0) {
//								extractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//							} else {
//								extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//							}
//
//							int w;
//							int h;
//							if (cropState != null) {
//								if (rotationValue == 90 || rotationValue == 270) {
//									w = cropState.transformHeight;
//									h = cropState.transformWidth;
//								} else {
//									w = cropState.transformWidth;
//									h = cropState.transformHeight;
//								}
//							} else {
//								w = resultWidth;
//								h = resultHeight;
//							}
//							if (BuildVars.LOGS_ENABLED) {
////								FileLog.d("create encoder with w = " + w + " h = " + h);
//							}
//							MediaFormat outputFormat = MediaFormat.createVideoFormat(MediaController.VIDEO_MIME_TYPE, w, h);
//							outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//							outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
//							outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
//							outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
//
//							if (Build.VERSION.SDK_INT < 23 && Math.min(h, w) <= 480) {
//								if (bitrate > 921600) {
//									bitrate = 921600;
//								}
//								outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
//							}
//
//							encoder = MediaCodec.createEncoderByType(MediaController.VIDEO_MIME_TYPE);
//							encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//							inputSurface = new InputSurface(encoder.createInputSurface());
//							inputSurface.makeCurrent();
//							encoder.start();
//
//							decoder = MediaCodec.createDecoderByType(videoFormat.getString(MediaFormat.KEY_MIME));
//							outputSurface = new OutputSurface(savedFilterState, null, paintPath, mediaEntities, cropState, resultWidth, resultHeight, rotationValue, framerate, false);
//							decoder.configure(videoFormat, outputSurface.getSurface(), null, 0);
//							decoder.start();
//
//							ByteBuffer[] decoderInputBuffers = null;
//							ByteBuffer[] encoderOutputBuffers = null;
//							ByteBuffer[] encoderInputBuffers = null;
//							if (Build.VERSION.SDK_INT < 21) {
//								decoderInputBuffers = decoder.getInputBuffers();
//								encoderOutputBuffers = encoder.getOutputBuffers();
//							}
//
//							if (audioIndex >= 0) {
//								MediaFormat audioFormat = extractor.getTrackFormat(audioIndex);
//								copyAudioBuffer = audioFormat.getString(MediaFormat.KEY_MIME).equals(MediaController.AUIDO_MIME_TYPE) || audioFormat.getString(MediaFormat.KEY_MIME).equals("audio/mpeg");
//
//								if (audioFormat.getString(MediaFormat.KEY_MIME).equals("audio/unknown")) {
//									audioIndex = -1;
//								}
//
//								if (audioIndex >= 0) {
//									if (copyAudioBuffer) {
//										audioTrackIndex = mediaMuxer.addTrack(audioFormat, true);
//										extractor.selectTrack(audioIndex);
//										int maxBufferSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
//										audioBuffer = ByteBuffer.allocateDirect(maxBufferSize);
//
//										if (startTime > 0) {
//											extractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//										} else {
//											extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//										}
//									} else {
//										MediaExtractor audioExtractor = new MediaExtractor();
//										audioExtractor.setDataSource(videoPath);
//										audioExtractor.selectTrack(audioIndex);
//
//										if (startTime > 0) {
//											audioExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//										} else {
//											audioExtractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//										}
//
//										audioRecoder = new AudioRecoder(audioFormat, audioExtractor, audioIndex);
//										audioRecoder.startTime = startTime;
//										audioRecoder.endTime = endTime;
//										audioTrackIndex = mediaMuxer.addTrack(audioRecoder.format, true);
//									}
//								}
//							}
//
//							boolean audioEncoderDone = audioIndex < 0;
//
//							boolean firstEncode = true;
//
//							checkConversionCanceled();
//
//							while (!outputDone || (!copyAudioBuffer && !audioEncoderDone)) {
//								checkConversionCanceled();
//
//								if (!copyAudioBuffer && audioRecoder != null) {
//									audioEncoderDone = audioRecoder.step(mediaMuxer, audioTrackIndex);
//								}
//
//								if (!inputDone) {
//									boolean eof = false;
//									int index = extractor.getSampleTrackIndex();
//									if (index == videoIndex) {
//										int inputBufIndex = decoder.dequeueInputBuffer(MEDIACODEC_TIMEOUT_DEFAULT);
//										if (inputBufIndex >= 0) {
//											ByteBuffer inputBuf;
//											if (Build.VERSION.SDK_INT < 21) {
//												inputBuf = decoderInputBuffers[inputBufIndex];
//											} else {
//												inputBuf = decoder.getInputBuffer(inputBufIndex);
//											}
//											int chunkSize = extractor.readSampleData(inputBuf, 0);
//											if (chunkSize < 0) {
//												decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//												inputDone = true;
//											} else {
//												decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, extractor.getSampleTime(), 0);
//												extractor.advance();
//											}
//										}
//									} else if (copyAudioBuffer && audioIndex != -1 && index == audioIndex) {
//										info.size = extractor.readSampleData(audioBuffer, 0);
//										if (Build.VERSION.SDK_INT < 21) {
//											audioBuffer.position(0);
//											audioBuffer.limit(info.size);
//										}
//										if (info.size >= 0) {
//											info.presentationTimeUs = extractor.getSampleTime();
//											extractor.advance();
//										} else {
//											info.size = 0;
//											inputDone = true;
//										}
//										if (info.size > 0 && (endTime < 0 || info.presentationTimeUs < endTime)) {
//											info.offset = 0;
//											info.flags = extractor.getSampleFlags();
//											long availableSize = mediaMuxer.writeSampleData(audioTrackIndex, audioBuffer, info, false);
//											if (availableSize != 0) {
//												if (callback != null) {
//													if (info.presentationTimeUs - startTime > currentPts) {
//														currentPts = info.presentationTimeUs - startTime;
//													}
//													callback.didWriteData(availableSize, (currentPts / 1000f) / durationS);
//												}
//											}
//										}
//									} else if (index == -1) {
//										eof = true;
//									}
//									if (eof) {
//										int inputBufIndex = decoder.dequeueInputBuffer(MEDIACODEC_TIMEOUT_DEFAULT);
//										if (inputBufIndex >= 0) {
//											decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//											inputDone = true;
//										}
//									}
//								}
//
//								boolean decoderOutputAvailable = !decoderDone;
//								boolean encoderOutputAvailable = true;
//								while (decoderOutputAvailable || encoderOutputAvailable) {
//									checkConversionCanceled();
//									int encoderStatus = encoder.dequeueOutputBuffer(info, increaseTimeout ? MEDIACODEC_TIMEOUT_INCREASED : MEDIACODEC_TIMEOUT_DEFAULT);
//									if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//										encoderOutputAvailable = false;
//									} else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//										if (Build.VERSION.SDK_INT < 21) {
//											encoderOutputBuffers = encoder.getOutputBuffers();
//										}
//									} else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//										MediaFormat newFormat = encoder.getOutputFormat();
//										if (videoTrackIndex == -5 && newFormat != null) {
//											videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
//											if (newFormat.containsKey(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES) && newFormat.getInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES) == 1) {
//												ByteBuffer spsBuff = newFormat.getByteBuffer("csd-0");
//												ByteBuffer ppsBuff = newFormat.getByteBuffer("csd-1");
//												prependHeaderSize = spsBuff.limit() + ppsBuff.limit();
//											}
//										}
//									} else if (encoderStatus < 0) {
//										throw new RuntimeException("unexpected result from encoder.dequeueOutputBuffer: " + encoderStatus);
//									} else {
//										ByteBuffer encodedData;
//										if (Build.VERSION.SDK_INT < 21) {
//											encodedData = encoderOutputBuffers[encoderStatus];
//										} else {
//											encodedData = encoder.getOutputBuffer(encoderStatus);
//										}
//										if (encodedData == null) {
//											throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
//										}
//										if (info.size > 1) {
//											if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
//												if (prependHeaderSize != 0 && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//													info.offset += prependHeaderSize;
//													info.size -= prependHeaderSize;
//												}
//												if (firstEncode && (info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
//													if (info.size > 100) {
//														encodedData.position(info.offset);
//														byte[] temp = new byte[100];
//														encodedData.get(temp);
//														int nalCount = 0;
//														for (int a = 0; a < temp.length - 4; a++) {
//															if (temp[a] == 0 && temp[a + 1] == 0 && temp[a + 2] == 0 && temp[a + 3] == 1) {
//																nalCount++;
//																if (nalCount > 1) {
//																	info.offset += a;
//																	info.size -= a;
//																	break;
//																}
//															}
//														}
//													}
//													firstEncode = false;
//												}
//												long availableSize = mediaMuxer.writeSampleData(videoTrackIndex, encodedData, info, true);
//												if (availableSize != 0) {
//													if (callback != null) {
//														if (info.presentationTimeUs - startTime > currentPts) {
//															currentPts = info.presentationTimeUs - startTime;
//														}
//														callback.didWriteData(availableSize, (currentPts / 1000f) / durationS);
//													}
//												}
//											} else if (videoTrackIndex == -5) {
//												byte[] csd = new byte[info.size];
//												encodedData.limit(info.offset + info.size);
//												encodedData.position(info.offset);
//												encodedData.get(csd);
//												ByteBuffer sps = null;
//												ByteBuffer pps = null;
//												for (int a = info.size - 1; a >= 0; a--) {
//													if (a > 3) {
//														if (csd[a] == 1 && csd[a - 1] == 0 && csd[a - 2] == 0 && csd[a - 3] == 0) {
//															sps = ByteBuffer.allocate(a - 3);
//															pps = ByteBuffer.allocate(info.size - (a - 3));
//															sps.put(csd, 0, a - 3).position(0);
//															pps.put(csd, a - 3, info.size - (a - 3)).position(0);
//															break;
//														}
//													} else {
//														break;
//													}
//												}
//
//												MediaFormat newFormat = MediaFormat.createVideoFormat(MediaController.VIDEO_MIME_TYPE, w, h);
//												if (sps != null && pps != null) {
//													newFormat.setByteBuffer("csd-0", sps);
//													newFormat.setByteBuffer("csd-1", pps);
//												}
//												videoTrackIndex = mediaMuxer.addTrack(newFormat, false);
//											}
//										}
//										outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
//										encoder.releaseOutputBuffer(encoderStatus, false);
//									}
//									if (encoderStatus != MediaCodec.INFO_TRY_AGAIN_LATER) {
//										continue;
//									}
//
//									if (!decoderDone) {
//										int decoderStatus = decoder.dequeueOutputBuffer(info, MEDIACODEC_TIMEOUT_DEFAULT);
//										if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
//											decoderOutputAvailable = false;
//										} else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//
//										} else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
//											MediaFormat newFormat = decoder.getOutputFormat();
//											if (BuildVars.LOGS_ENABLED) {
////												FileLog.d("newFormat = " + newFormat);
//											}
//										} else if (decoderStatus < 0) {
//											throw new RuntimeException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);
//										} else {
//											boolean doRender = info.size != 0;
//											long originalPresentationTime = info.presentationTimeUs;
//											if (endTime > 0 && originalPresentationTime >= endTime) {
//												inputDone = true;
//												decoderDone = true;
//												doRender = false;
//												info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
//											}
//											boolean flushed = false;
//											if (avatarStartTime >= 0 && (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0 && Math.abs(avatarStartTime - startTime) > 1000000 / framerate) {
//												if (startTime > 0) {
//													extractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//												} else {
//													extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
//												}
//												additionalPresentationTime = minPresentationTime + frameDelta;
//												endTime = avatarStartTime;
//												avatarStartTime = -1;
//												inputDone = false;
//												decoderDone = false;
//												doRender = false;
//												info.flags &=~ MediaCodec.BUFFER_FLAG_END_OF_STREAM;
//												decoder.flush();
//												flushed = true;
//											}
//											trueStartTime = avatarStartTime >= 0 ? avatarStartTime : startTime;
//											if (trueStartTime > 0 && videoTime == -1) {
//												if (originalPresentationTime < trueStartTime) {
//													doRender = false;
//													if (BuildVars.LOGS_ENABLED) {
////														FileLog.d("drop frame startTime = " + trueStartTime + " present time = " + info.presentationTimeUs);
//													}
//												} else {
//													videoTime = info.presentationTimeUs;
//													if (minPresentationTime != Integer.MIN_VALUE) {
//														additionalPresentationTime -= videoTime;
//													}
//												}
//											}
//											if (flushed) {
//												videoTime = -1;
//											} else {
//												if (avatarStartTime == -1 && additionalPresentationTime != 0) {
//													info.presentationTimeUs += additionalPresentationTime;
//												}
//												decoder.releaseOutputBuffer(decoderStatus, doRender);
//											}
//											if (doRender) {
//												if (avatarStartTime >= 0) {
//													minPresentationTime = Math.max(minPresentationTime, info.presentationTimeUs);
//												}
//												boolean errorWait = false;
//												try {
//													outputSurface.awaitNewImage();
//												} catch (Exception e) {
//													errorWait = true;
////													FileLog.e(e);
//												}
//												if (!errorWait) {
//													outputSurface.drawImage();
//													inputSurface.setPresentationTime(info.presentationTimeUs * 1000);
//													inputSurface.swapBuffers();
//												}
//											}
//											if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
//												decoderOutputAvailable = false;
//												if (BuildVars.LOGS_ENABLED) {
////													FileLog.d("decoder stream end");
//												}
//												encoder.signalEndOfInputStream();
//											}
//										}
//									}
//								}
//							}
//						} catch (Exception e) {
//							// in some case encoder.dequeueOutputBuffer return IllegalStateException
//							// stable reproduced on xiaomi
//							// fix it by increasing timeout
//							if (e instanceof IllegalStateException && !increaseTimeout) {
//								repeatWithIncreasedTimeout = true;
//							}
////							FileLog.e("bitrate: " + bitrate + " framerate: " + framerate + " size: " + resultHeight + "x" + resultWidth);
////							FileLog.e(e);
//							error = true;
//						}
//
//						extractor.unselectTrack(videoIndex);
//						if (decoder != null) {
//							decoder.stop();
//							decoder.release();
//						}
//					}
//					if (outputSurface != null) {
//						outputSurface.release();
//					}
//					if (inputSurface != null) {
//						inputSurface.release();
//					}
//					if (encoder != null) {
//						encoder.stop();
//						encoder.release();
//					}
//					if (audioRecoder != null) {
//						audioRecoder.release();
//					}
//					checkConversionCanceled();
//				} else {
//					readAndWriteTracks(extractor, mediaMuxer, info, startTime, endTime, duration, cacheFile, bitrate != -1);
//				}
//			}
//		} catch (Exception e) {
//			error = true;
////			FileLog.e("bitrate: " + bitrate + " framerate: " + framerate + " size: " + resultHeight + "x" + resultWidth);
////			FileLog.e(e);
//		} finally {
//			if (extractor != null) {
//				extractor.release();
//			}
//			if (mediaMuxer != null) {
//				try {
//					mediaMuxer.finishMovie();
//					endPresentationTime = mediaMuxer.getLastFrameTimestamp(videoTrackIndex);
//				} catch (Exception e) {
////					FileLog.e(e);
//				}
//			}
//		}
//
//		if (repeatWithIncreasedTimeout) {
//			return convertVideoInternal(videoPath, cacheFile, rotationValue, isSecret,
//					resultWidth, resultHeight, framerate, bitrate, originalBitrate, startTime, endTime, avatarStartTime, duration,
//					needCompress, true, savedFilterState, paintPath, mediaEntities,
//					isPhoto, cropState);
//		}
//
//		return error;
//	}
//	private void checkConversionCanceled() {
//		if (callback != null && callback.checkConversionCanceled())
//			throw new RuntimeException("canceled conversion");
//	}
}