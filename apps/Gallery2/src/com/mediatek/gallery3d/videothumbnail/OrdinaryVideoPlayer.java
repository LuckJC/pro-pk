package com.mediatek.gallery3d.videothumbnail;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.glrenderer.ExtTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRoot.OnGLIdleListener;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES11Ext;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;

public class OrdinaryVideoPlayer extends AbstractVideoPlayer {

    // Key and value for enable ClearMotion
    private static final int CLEAR_MOTION_KEY = 1700;
    private static final int CLEAR_MOTION_DISABLE = 1;

    public static final int STATE_UNINITIALIZED = 0;
    public static final int STATE_WORKING = 1;
    public static final int STATE_RECYCLED = 2;
    public final Object stateObject = new Object();
    public int mState = STATE_UNINITIALIZED;

    public final MediaPlayer mediaPlayer = new MediaPlayer();
    public final VideoThumbnail renderTarget = new VideoThumbnail() {
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            if (renderTarget.isWorking) {
                // Uh..., renderContext can be cached
                // But can it be null or changed during Gallery scenario?
                synchronized (renderTarget) {
                    mHasNewFrame = true;
                }
                GLRoot renderContext = mGalleryActivity.getGLRoot();
                if (renderContext != null) {
                    renderContext.requestRender();
                    renderContext.addOnGLIdleListener(renderTarget);
                }
            }
        }
    };
    public Surface surface;

    private static final String TAG = "Gallery2/VideoThumbnailPlayInfo";

    public boolean prepare() {
        mediaPlayer.setOnVideoSizeChangedListener(renderTarget);
        mediaPlayer.setLooping(true);
        mediaPlayer.setVolume(0, 0);
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "error happened in video thumbnail's internal player. \n\tmay triggered by video deletion");
                //closeThumbnailByPlayer(mp);
                //release();
                renderTarget.isWorking = false;
                renderTarget.isReadyForRender = false;
                return false;
            }
        });

        /*mGalleryActivity.getGLRoot().*/addGLContextRequiredOnGLIdleListener(new OnGLIdleListener() {
            public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
                synchronized (stateObject) {
                    if (mState == STATE_UNINITIALIZED) {
                        renderTarget.acquireSurfaceTexture(canvas);
                        SurfaceTexture texture = renderTarget.getSurfaceTexture();
                        surface = new Surface(texture);
                        mediaPlayer.setSurface(surface);
                        mState = STATE_WORKING;
                    }
                }
                return false;
            }
        });

        try{
            mediaPlayer.reset();
            mediaPlayer.setLooping(true);

            try {
                mediaPlayer.setDataSource(mPath);

                // Disable ClearMotion
                mediaPlayer.setParameter(CLEAR_MOTION_KEY, CLEAR_MOTION_DISABLE);

                mediaPlayer.prepare();
            } catch (Exception e) {
                e.printStackTrace();
                release();
                Log.e(TAG, e.getClass().getName()
                      + "happens when openning video thumbnail");
                return false;
            }

            renderTarget.isWorking = true;
        } catch (IllegalStateException e) {
            Log.v(TAG, "thumbnail is released by pausing, give up openning");
            return false;
        }
        return true;
    }

    public void release() {
        renderTarget.isWorking = false;
        renderTarget.isReadyForRender = false;
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
        } catch (IllegalStateException e) {
            Log.v(TAG, "thumbnail is released by pausing, give up recycling once again");
        }

        // mediaPlayer.release();
        // surface.release();
        // renderTarget.releaseSurfaceTexture();
        boolean needTextureRelease = false;
        synchronized (stateObject) {
            if (mState == STATE_WORKING) {
                needTextureRelease = true;
            }
            mState = STATE_RECYCLED;
        }
        mediaPlayer.release();
        if (needTextureRelease) {
            surface.release();
            renderTarget.releaseSurfaceTexture();
        }
    }

    public boolean start() {
        mediaPlayer.start();
        return true;
    }

    public boolean pause() {
        mediaPlayer.pause();
        return true;
    }

    public boolean stop() {
        // cause no defect found by forgetting calling this guy for IT period, we skip it
        // mediaPlayer.stop();
        return true;
    }

    public boolean render(GLCanvas canvas, int width, int height) {
        try {
            if (!renderTarget.isReadyForRender) {
                return false;
            }
            renderTarget.draw(canvas, width, height);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        return true;
    }

    public static abstract class VideoThumbnail implements
            SurfaceTexture.OnFrameAvailableListener,
            MediaPlayer.OnVideoSizeChangedListener, GLRoot.OnGLIdleListener {

        // this class can be removed from now
        // for google has make BasicTexture.setSize() from protected to public
        // ps: this class was created to avoid modify the visibility of BasicTexture.setSize()
        private class VideoFrameTexture extends ExtTexture {
            public VideoFrameTexture(GLCanvas canvas, int target) {
                super(canvas, target, true);                
            }

            public void setSize(int width, int height) {
                super.setSize(width, height);
            }
        }

        @SuppressWarnings("unused")
        private static final String TAG = "Gallery2/VideoThumbnail";
        static final int TEXTURE_HEIGHT = 128;
        static final int TEXTURE_WIDTH = 128;

        private VideoFrameTexture mVideoFrameTexture;
        private SurfaceTexture mSurfaceTexture;
        private int mWidth = TEXTURE_WIDTH;
        private int mHeight = TEXTURE_HEIGHT;
        private float[] mTransformFromSurfaceTexture = new float[16];
        private float[] mTransformForCropingCenter = new float[16];
        private float[] mTransformFinal = new float[16];
        private boolean mHasTexture = false;
        protected boolean mHasNewFrame = false;
        protected boolean isReadyForRender = false;
        public volatile boolean isWorking = false;

        public void acquireSurfaceTexture(GLCanvas canvas) {
            mVideoFrameTexture = new VideoFrameTexture(canvas,
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            mVideoFrameTexture.setSize(TEXTURE_WIDTH, TEXTURE_HEIGHT);
            mSurfaceTexture = new SurfaceTexture(mVideoFrameTexture.getId());
            setDefaultBufferSize(mSurfaceTexture, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            mSurfaceTexture.setOnFrameAvailableListener(this);
            synchronized (this) {
                mHasTexture = true;
            }
        }

        private static void setDefaultBufferSize(SurfaceTexture st, int width,
                int height) {
            if (ApiHelper.HAS_SET_DEFALT_BUFFER_SIZE) {
                st.setDefaultBufferSize(width, height);
            }
        }

        private static void releaseSurfaceTexture(SurfaceTexture st) {
            // The thread calling this function may not the same thread as create SurfaceTexture,
            // and if setOnFrameAvailableListener as null, SurfaceTexture may throw
            // nullpointer exception
            // st.setOnFrameAvailableListener(null);
            if (ApiHelper.HAS_RELEASE_SURFACE_TEXTURE) {
                st.release();
            }
        }

        public SurfaceTexture getSurfaceTexture() {
            return mSurfaceTexture;
        }

        public void releaseSurfaceTexture() {
            synchronized (this) {
                if (!mHasTexture)
                    return;
                mHasTexture = false;
            }
            mVideoFrameTexture.recycle();
            mVideoFrameTexture = null;
            releaseSurfaceTexture(mSurfaceTexture);
            mSurfaceTexture = null;
        }

        public void draw(GLCanvas canvas, int slotWidth, int slotHeight) {
            synchronized (this) {
                if (!mHasTexture || !isWorking) {
                    return;
                }

                mSurfaceTexture
                        .getTransformMatrix(mTransformFromSurfaceTexture);

                // Flip vertically.
                canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
                int cx = slotWidth / 2;
                int cy = slotHeight / 2;
                canvas.translate(cx, cy);
                canvas.scale(1, -1, 1);
                canvas.translate(-cx, -cy);
                float longSideStart;
                float longSideEnd;
                RectF sourceRect;
                RectF targetRect = new RectF(0, 0, slotWidth, slotHeight);
                // To reduce computing complexity, the following logic based on
                // the fact that slotWidth = slotHeight. Strictly, the condition should be
                // if ((float)mWidth / mHeight > (float)slotWidth / slotHeight)
                if (mWidth > mHeight) {
                    longSideStart = (mWidth - mHeight) * TEXTURE_WIDTH
                            / (float) ((mWidth * 2));
                    longSideEnd = TEXTURE_WIDTH - longSideStart;
                    sourceRect = new RectF(longSideStart, 0, longSideEnd,
                            TEXTURE_HEIGHT);
                } else {
                    longSideStart = (mHeight - mWidth) * TEXTURE_HEIGHT
                            / (float) ((mHeight * 2));
                    longSideEnd = TEXTURE_HEIGHT - longSideStart;
                    sourceRect = new RectF(0, longSideStart, TEXTURE_WIDTH,
                            longSideEnd);
                }

                genCononTexCoords(sourceRect, targetRect, mVideoFrameTexture);
                genExtTexMatForSubTile(sourceRect);
                Matrix.multiplyMM(mTransformFinal, 0,
                        mTransformFromSurfaceTexture, 0,
                        mTransformForCropingCenter, 0);
                canvas.drawTexture(mVideoFrameTexture, mTransformFinal,
                        (int) targetRect.left, (int) targetRect.top,
                        (int) targetRect.width(), (int) targetRect.height());
                canvas.restore();
            }
        }

        abstract public void onFrameAvailable(SurfaceTexture surfaceTexture);

        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public boolean onGLIdle(GLCanvas canvas, boolean renderRequested) {
            // if (renderRequested) return true;
            synchronized (this) {
                if (isWorking && mHasTexture && mHasNewFrame) {
                    if (mSurfaceTexture != null) {
                        try {
                            mSurfaceTexture.updateTexImage();
                        } catch (IllegalStateException e) {
                            Log.v(TAG, "notify author that mSurfaceTexture in thumbnail released when updating tex img");
                            return false;
                        }
                    }
                    mHasNewFrame = false;
                    isReadyForRender = true;
                }
            }
            return false;
        }

        // This function changes the source coordinate to the texture coordinates.
        // It also clips the source and target coordinates if it is beyond the
        // bound of the texture.
        private static void genCononTexCoords(RectF source, RectF target,
                VideoFrameTexture texture) {

            int width = texture.getWidth();
            int height = texture.getHeight();
            int texWidth = texture.getTextureWidth();
            int texHeight = texture.getTextureHeight();
            // Convert to texture coordinates
            source.left /= texWidth;
            source.right /= texWidth;
            source.top /= texHeight;
            source.bottom /= texHeight;

            // Clip if the rendering range is beyond the bound of the texture.
            float xBound = (float) width / texWidth;
            if (source.right > xBound) {
                target.right = target.left + target.width()
                        * (xBound - source.left) / source.width();
                source.right = xBound;
            }
            float yBound = (float) height / texHeight;
            if (source.bottom > yBound) {
                target.bottom = target.top + target.height()
                        * (yBound - source.top) / source.height();
                source.bottom = yBound;
            }
        }

        private void genExtTexMatForSubTile(RectF subRange) {
            mTransformForCropingCenter[0] = subRange.right - subRange.left;
            mTransformForCropingCenter[5] = subRange.bottom - subRange.top;
            mTransformForCropingCenter[10] = 1;
            mTransformForCropingCenter[12] = subRange.left;
            mTransformForCropingCenter[13] = subRange.top;
            mTransformForCropingCenter[15] = 1;
        }
    }
}
