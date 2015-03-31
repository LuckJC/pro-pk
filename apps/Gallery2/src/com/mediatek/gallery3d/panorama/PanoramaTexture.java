package com.mediatek.gallery3d.panorama;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.gallery3d.glrenderer.BitmapTexture;

public class PanoramaTexture extends BitmapTexture {
    private static String TAG = "Gallery2/PanoramaTexture";

    public PanoramaTexture(Bitmap bitmap) {
        super(bitmap);
    }

    public PanoramaTexture(Bitmap bitmap, boolean hasBorder) {
        super(bitmap, hasBorder);
    }

    /**
     * Sets the content size of this texture. Not set the actual texture
     * size as power of 2 like BasicTexture
     */
    public void setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        mTextureWidth = width;
        mTextureHeight = height;
    }
}