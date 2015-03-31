package com.mediatek.gallery3d.panorama;

import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;

import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.ScreenNail;
import com.mediatek.gallery3d.panorama.PanoramaDrawer;
import com.mediatek.gallery3d.util.MtkLog;

public class PanoramaScreenNail implements ScreenNail {
    private static final String TAG = "Gallery2/PanoramaScreenNail";
    private static final int DEFAULT_DEGREE = 0;

    private int mColor;
    private boolean mColorPanorama = false;
    private RawTexture mTexture;
    private PanoramaDrawer mPanoramaDrawer;
    private PanoramaConfig mConfig;
    private float mLastDegree = -1;
    public PanoramaScreenNail(Bitmap bitmap, PanoramaConfig config) {
        mConfig = config;
        mPanoramaDrawer = new PanoramaDrawer(bitmap, mConfig);
    }

    public PanoramaScreenNail(int color, PanoramaConfig config) {
        mColor = color;
        mColorPanorama = true;
        mConfig = config;
        mPanoramaDrawer = new PanoramaDrawer(color, mConfig);
    }

    public boolean isColorPanorma() {
        return mColorPanorama;
    }

    @Override
    public int getWidth() {
        return (int)(mConfig.mCanvasWidth / mConfig.mCanvasScale);
    }

    @Override
    public int getHeight() {
        return (int)(mConfig.mCanvasHeight / mConfig.mCanvasScale);
    }

    @Override
    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        draw(canvas, x, y, width, height, DEFAULT_DEGREE);
    }

    @Override
    public void draw(GLCanvas canvas, RectF source, RectF dest) {
        draw(canvas, source, dest, DEFAULT_DEGREE);
    }

    @Override
    public void recycle() {
        if (mTexture != null) {
            mTexture.recycle();
            mTexture = null;
        }
        if (mPanoramaDrawer != null) {
            mPanoramaDrawer.freeResources();
        }
        mLastDegree = -1;
    }

    @Override
    public void noDraw(GLCanvas canvas) {
    }

    public void draw(GLCanvas canvas, int x, int y, int width, int height, float degree) {
        if (mTexture == null) {
            mTexture = new RawTexture(mConfig.mCanvasWidth, mConfig.mCanvasHeight, false);
        }
        if (mLastDegree != degree) {
            mPanoramaDrawer.drawOnTexture(canvas, mTexture, degree);
            mLastDegree = degree;
        }
        mTexture.draw(canvas, x, y, width, height);
    }

    public void draw(GLCanvas canvas, RectF source, RectF dest, int degree) {
        if (mTexture == null) {
            mTexture = new RawTexture(mConfig.mCanvasWidth, mConfig.mCanvasHeight, false);
        }
        if (mLastDegree != degree) {
            mPanoramaDrawer.drawOnTexture(canvas, mTexture, degree);
            mLastDegree = degree;
        }
        canvas.drawTexture(mTexture, source, dest);
    }

    public void setBitmap(Bitmap bitmap) {
        mColorPanorama = false;
        mLastDegree = -1;
        mPanoramaDrawer.setBitmap(bitmap);
    }
}