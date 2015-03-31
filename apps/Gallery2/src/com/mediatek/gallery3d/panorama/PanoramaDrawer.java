package com.mediatek.gallery3d.panorama;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL11;

import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.GLId;
import com.android.gallery3d.glrenderer.RawTexture;
import com.mediatek.gallery3d.panorama.PanoramaHelper;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Bitmap.Config;
import android.opengl.GLES11;
import android.opengl.GLES20;
import android.os.SystemClock;

public class PanoramaDrawer {
    private static final String TAG = "Gallery2/PanoramaDrawer";
    private static float[] sClearColor = { 0.f, 0.f, 0.f, 0.f };

    private Bitmap mBitmap = null;
    private Bitmap mColorBitmap = null;
    private boolean mColorDrawer = false;
    private boolean mFree = false;

    private PanoramaTexture mTexture = null;

    private int mXyzBuffer = -1;
    private int mUvBuffer = -1;

    private PanoramaMesh mPanoramaMesh;
    private PanoramaConfig mPanoramaConfig;
    private GLCanvas mCanvasRef;

    private int mCanvasWidth;
    private int mCanvasHeight;
    private int[] mTexEnvMode = new int[1];

    public PanoramaDrawer(Bitmap bitmap, PanoramaConfig config) {
        mBitmap = bitmap;
        mPanoramaConfig = config;
        mPanoramaMesh = PanoramaMesh.getInstance(mPanoramaConfig.mNewWidth,
                mPanoramaConfig.mNewHeight);
    }

    public PanoramaDrawer(int color, PanoramaConfig config) {
        mBitmap = null;
        mColorBitmap = Bitmap.createBitmap(config.mNewWidth / config.mNewHeight, 1,
                Config.ARGB_8888);
        Canvas canvas = new Canvas(mColorBitmap);
        canvas.drawColor(color);
        mColorDrawer = true;
        mPanoramaConfig = config;
        mPanoramaMesh = PanoramaMesh.getInstance(mPanoramaConfig.mNewWidth,
                mPanoramaConfig.mNewHeight);
    }

    public synchronized void setBitmap(Bitmap bitmap) {
        if (mColorBitmap != null) {
            mColorBitmap.recycle();
            mColorBitmap = null;
        }
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
        if (mTexture != null) {
            mTexture.recycle();
            mTexture = null;
        }
        mColorDrawer = false;
        mBitmap = bitmap;
    }

    public synchronized void drawOnTexture(GLCanvas canvas, RawTexture targetTexture, float degree) {
        if (mFree) {
            MtkLog.i(TAG, "<drawOnTexture> Has been free, not render, return");
            return;
        }
        mCanvasRef = canvas;
        // prepare
        prepareBuffers(canvas);
        prepareTexture();
        // set RawTexture as render targert
        beginRenderTarget(canvas, targetTexture);
        // draw
        draw(canvas, degree, false);
        // restore render target
        endRenderTarget(canvas);
    }

    public synchronized Bitmap drawOnBitmap(GLCanvas canvas, RawTexture targetTexture, float degree) {
        if (mFree) {
            MtkLog.i(TAG, "<drawOnBitmap> Has been free, not render, return null");
            return null;
        }
        mCanvasRef = canvas;
        // prepare
        prepareBuffers(canvas);
        prepareTexture();
        // set RawTexture as render targert
        beginRenderTarget(canvas, targetTexture);
        // draw
        draw(canvas, degree, true);
        // read pixels
        Bitmap bitmap = readPixels(canvas);
        // mirror
        Matrix m = new Matrix();
        m.postScale(-1, 1);
        Bitmap res = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m,
                true);
        bitmap.recycle();
        // restore render target
        endRenderTarget(canvas);
        return res;
    }

    private synchronized Bitmap readPixels(GLCanvas canvas) {
        int b[] = new int[mPanoramaConfig.mCanvasWidth * mPanoramaConfig.mCanvasHeight];
        int bt[] = new int[mPanoramaConfig.mCanvasWidth * mPanoramaConfig.mCanvasHeight];
        IntBuffer ib = IntBuffer.wrap(b);
        ib.position(0);
        long start = SystemClock.currentThreadTimeMillis();
        if (canvas.getGLVersion() == 1) {
            canvas.readPixels(0, 0, mPanoramaConfig.mCanvasWidth, mPanoramaConfig.mCanvasHeight,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, ib);
        } else if (canvas.getGLVersion() == 2) {
            canvas.readPixels(0, 0, mPanoramaConfig.mCanvasWidth, mPanoramaConfig.mCanvasHeight,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, ib);
        }
        long end = SystemClock.currentThreadTimeMillis();
        MtkLog.i(TAG, "<readPixels> [" + mPanoramaConfig.mCanvasWidth + ","
                + mPanoramaConfig.mCanvasHeight + "], glReadPixels cost" + (end - start) + " ms");
        for (int i = 0; i < mPanoramaConfig.mCanvasHeight; i++) {
            for (int j = 0; j < mPanoramaConfig.mCanvasWidth; j++) {
                int pix = b[i * mPanoramaConfig.mCanvasWidth + j];
                int pb = (pix >> 16) & 0xff;
                int pr = (pix << 16) & 0x00ff0000;
                int pix1 = (pix & 0xff00ff00) | pr | pb;
                bt[(mPanoramaConfig.mCanvasHeight - i - 1) * mPanoramaConfig.mCanvasWidth + j] = pix1;
            }
        }
        long end2 = SystemClock.currentThreadTimeMillis();
        Bitmap sb = Bitmap.createBitmap(bt, mPanoramaConfig.mCanvasWidth,
                mPanoramaConfig.mCanvasHeight, Bitmap.Config.ARGB_8888);
        MtkLog.i(TAG, "<readPixels> [" + mPanoramaConfig.mCanvasWidth + ","
                + mPanoramaConfig.mCanvasHeight + "], covert buffer to bitmap cost " + (end2 - end)
                + " ms");
        return sb;
    }

    public synchronized void freeResources() {
        mFree = true;
        if (mCanvasRef != null) {
            mCanvasRef.deleteBuffer(mXyzBuffer);
            mCanvasRef.deleteBuffer(mUvBuffer);
            mXyzBuffer = -1;
            mUvBuffer = -1;
        }
        if (mTexture != null) {
            mTexture.recycle();
            mTexture = null;
        }
        if (mColorBitmap != null) {
            mColorBitmap.recycle();
            mColorBitmap = null;
        }
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    private void prepareBuffers(GLCanvas canvas) {
        if (mXyzBuffer == -1) {
            mXyzBuffer = canvas.uploadBuffer(mPanoramaMesh.getVertexBuffer());
        }
        if (mUvBuffer == -1) {
            FloatBuffer texCoordsBuffer = mPanoramaMesh
                    .getTexCoordsBuffer((float) mPanoramaConfig.mOriginWidth
                            / (float) mPanoramaConfig.mNewWidth);
            mUvBuffer = canvas.uploadBuffer(texCoordsBuffer);
        }
    }

    private void prepareTexture() {
        if (mTexture == null) {
            if (!mColorDrawer) {
                mTexture = new PanoramaTexture(mBitmap, false);
            } else {
                mTexture = new PanoramaTexture(mColorBitmap, false);
            }
        }
    }

    private void beginRenderTarget(GLCanvas canvas, RawTexture texture) {
        canvas.beginRenderTarget(texture);
    }

    private void endRenderTarget(GLCanvas canvas) {
        canvas.endRenderTarget();
    }

    private void draw(GLCanvas canvas, float degree, boolean isBitmapTarget) {
        canvas.clearBuffer(sClearColor);
        canvas.setPerspective(mPanoramaConfig.mFovy, (float) mPanoramaConfig.mCanvasWidth
                / (float) mPanoramaConfig.mCanvasHeight, 0.1f, 1000.0f);
        if (isBitmapTarget) {
            canvas.setLookAt(mPanoramaConfig.mCameraDistance, 0, 0, -1.0f, 0, 0, 0, 0, 1);
        } else {
            canvas.setLookAt(mPanoramaConfig.mCameraDistance, 0, 0, -1.0f, 0, 0, 0, 0, -1);
        }
        // canvas.setLookAt(0, 0, 20.f, 0, 0, -1.0f, 0, 1, 0);
        canvas.setAlpha(1.0f);
        canvas.drawMesh(mTexture, -degree + mPanoramaConfig.mRotateDegree, 0, 0, 1, mXyzBuffer,
                mUvBuffer, mPanoramaMesh.getVertexCount());
    }
}