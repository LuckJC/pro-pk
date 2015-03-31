package com.mediatek.gallery3d.videothumbnail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.mediatek.gallery3d.util.MtkUtils;
import com.mediatek.gallery3d.videowriter.VideoWriter;

public abstract class BitmapStreamToVideoGenerator extends AbstractVideoGenerator {
    private static final String TAG = "Gallery2/BitmapStreamToVideoGenerator";
    
    public static final class VideoConfig {
        public int bitRate;
        // no effect at present, determined by frame rate
        public int framesBetweenIFrames;
        public float frameInterval;
        public int frameCount = 0;
        public int frameWidth;
        public int frameHeight;

        public static VideoConfig copy(final VideoConfig another) {
            VideoConfig vInfo = new VideoConfig();
            vInfo.bitRate = another.bitRate;
            vInfo.framesBetweenIFrames = another.framesBetweenIFrames;
            vInfo.frameInterval = another.frameInterval;
            vInfo.frameCount = another.frameCount;
            vInfo.frameWidth = another.frameWidth;
            vInfo.frameHeight = another.frameHeight;
            return vInfo;
        }
    }

    private static final VideoConfig[] sPredefinedVideoConfigs = {
            new VideoConfig(), new VideoConfig() };
    
    static {
        // pre-defined video configuration for thumbnail
        sPredefinedVideoConfigs[VTYPE_THUMB]
                              .bitRate = 32 * 1024; // 32k
        sPredefinedVideoConfigs[VTYPE_THUMB]
                              .framesBetweenIFrames = 15;
        sPredefinedVideoConfigs[VTYPE_THUMB]
                              .frameInterval = 200;
        sPredefinedVideoConfigs[VTYPE_THUMB]
                              .frameWidth = DEFAULT_THUMBNAIL_SIZE;
        sPredefinedVideoConfigs[VTYPE_THUMB]
                              .frameHeight = DEFAULT_THUMBNAIL_SIZE;
        
        // pre-defined video configuration for sharing
        sPredefinedVideoConfigs[VTYPE_SHARE]
                              .bitRate = 128 * 1024; // 128k
        sPredefinedVideoConfigs[VTYPE_SHARE]
                              .framesBetweenIFrames = 15;
        sPredefinedVideoConfigs[VTYPE_SHARE]
                              .frameInterval = 200;
        sPredefinedVideoConfigs[VTYPE_SHARE]
                              .frameWidth = 640;
        sPredefinedVideoConfigs[VTYPE_SHARE]
                              .frameHeight = 480;
    }
    
    public abstract Bitmap getBitmapAtFrame(MediaItem item,
            int videoType, int frameIndex);


    /**
     * Make preparations for a certain type of video generating.<br/>
     * And meanwhile, <font color="purple">you should at least configure frame
     * rate in parameter 'config', and other configurations in it are optional
     * (default values will be used instead)</font>
     * 
     * @param item
     *            the media item for which to generate video
     * @param videoType
     *            the type of video to generate
     * @param config
     *            configure generating information in this object
     */
    public abstract void init(MediaItem item, int videoType, VideoConfig config/*in,out*/);

    /**
     * Do finalizing work for a certain type of video generating.<br/>
     * Specially, <font color="purple">you should release all resources
     * allocated in init() to avoid memory leak</font>
     * 
     * @param item
     *            the media item for which to generate video
     * @param videoType
     *            the type of video to generate
     */
    public abstract void deInit(MediaItem item, int videoType);

    public int generate(LocalMediaItem item, int videoType) {
        VideoConfig config = VideoConfig.copy(sPredefinedVideoConfigs[videoType]);
        init(item, videoType, config);
        if ((config.frameCount <= 0)) {
            Log.e(TAG, "frame count never appropriately initialized!");
            deInit(item, videoType);
            return GENERATE_ERROR;
        }
 
        if (shouldCancel()) {
            deInit(item, videoType);
            return GENERATE_CANCEL;
        }

        Bitmap bitmap = getBitmapAtFrame(item, videoType, 0);
        if (bitmap == null) {
            deInit(item, videoType);
            return GENERATE_ERROR;
        }
        config.frameWidth = bitmap.getWidth();
        config.frameHeight = bitmap.getHeight();

        BitmapToVideoEncoder myEncoder = new BitmapToVideoEncoder(config,
                VideoThumbnailHelper.getTempFilePathForMediaItem(item, videoType));

        myEncoder.addFrame(bitmap);
        for (int i = 1; i < config.frameCount; i++) {
            bitmap.recycle();
            if (shouldCancel()) {
                deInit(item, videoType);
                myEncoder.close();
                return GENERATE_CANCEL;
            }
            bitmap = getBitmapAtFrame(item, videoType, i);
            if (bitmap == null) {
                deInit(item, videoType);
                myEncoder.close();
                return GENERATE_ERROR;
            }
            myEncoder.addFrame(bitmap);
        }
        // add two more last frames
        for (int i = 0; i < 2; i++) {
            myEncoder.addFrame(bitmap);
        }
        bitmap.recycle();
        myEncoder.close();
        deInit(item, videoType);
        return GENERATE_OK;
    }

    private static class BitmapToVideoEncoder {
        private static final String TAG = "Gallery2/BitmapToVideoEncoder";

        private MediaCodec mediaCodec;
        VideoWriter mVideoW;
        private BufferedOutputStream outputStream;
        private boolean dumpBitmap = false;
        private boolean dumpStream = false;
        private int frameNumber = 0;

        public BitmapToVideoEncoder(VideoConfig config, String outputPath) {
            if(dumpStream) {
                File file = new File(Environment.getExternalStorageDirectory(),
                        "Download/debug.mp4");
                try {
                    outputStream = new BufferedOutputStream(new FileOutputStream(file));
                } catch (Exception e) {
                    Log.i(TAG, "outputStream initialized fail!");
                }
            }
            Log.d(TAG, "init VideoWriter, size: " + config.frameWidth + " * " + config.frameHeight);
            mVideoW = new VideoWriter(outputPath, config.frameWidth, config.frameHeight, 1, null);
            mVideoW.setParameter(VideoWriter.KEY_FRAME_RATE, 1000/config.frameInterval);
            mVideoW.start();

            mediaCodec = MediaCodec.createEncoderByType("video/mp4v-es");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/mp4v-es",
                    config.frameWidth, config.frameHeight);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, (int) (1000/config.frameInterval));
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_Format24bitRGB888);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, config.framesBetweenIFrames);
            mediaCodec.configure(mediaFormat, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
            mediaCodec.start();
        }

        public void close() {
            mVideoW.close();
            try {
                mediaCodec.stop();
                mediaCodec.release();
                if(dumpStream) {
                    outputStream.flush();
                    outputStream.close();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        private void offerEncoder(byte[] input) {
            try {
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
                }

                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                while (outputBufferIndex >= 0) {
                       ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        byte[] outData = new byte[bufferInfo.size];
                        outputBuffer.get(outData);
                        if(dumpStream) {
                            outputStream.write(outData, 0, outData.length);
                            Log.i(TAG, "buffer flag:" + bufferInfo.flags + ",length:" + bufferInfo.size);
                        }
                        if((bufferInfo.flags & 0x02) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                            mVideoW.setCodecSpecifiData(outData);
                        } else {
                            mVideoW.receiveFrameBuffer(outData, bufferInfo.size, 
                                    (bufferInfo.flags & 0x01) == MediaCodec.BUFFER_FLAG_SYNC_FRAME);
                        }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            } catch (IOException e) {
                Log.d(TAG, "");
            }
        }

        public void addFrame(Bitmap bitmap) {
            int len = 0;
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();

            int [] intArray = new int[bitmapWidth * bitmapHeight];
            bitmap.getPixels(intArray, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);
            byte [] byteArray = new byte[bitmapWidth * bitmapHeight * 3];
            for(int i : intArray) {
                byteArray[len*3 + 0] = (byte) ((i & 0xff0000) >> 16);
                byteArray[len*3 + 1] = (byte) ((i & 0xff00) >> 8);
                byteArray[len++*3 + 2] = (byte) ((i & 0xff) >> 0);
            }

            if(dumpBitmap) {
                String strName = "Download/bitmapToVideo_" + frameNumber++;
                MtkUtils.dumpBitmap(bitmap, strName);
            }
            offerEncoder(byteArray);
        }
    }
}
