package com.mediatek.gallery3d.stereo;

import javax.microedition.khronos.opengles.GL11;

import android.graphics.Bitmap;
import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.media.effect.EffectUpdateListener;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.GLId;
import com.android.gallery3d.util.Log;
import com.mediatek.effect.effects.MTKEffectFactory;
import com.mediatek.effect.effects.Stereo3D2Dto3DEffect;
import com.mediatek.gallery3d.stereo.StereoEffectHandle.StereoEffect;
import com.mediatek.gallery3d.util.MtkUtils;

public class StereoConvert2DTo3D implements StereoEffect {
    private static final String TAG = "StereoConvert2DTo3D";
    
    private Bitmap mBitmap = null;
    private Bitmap mBitmapAfterConvert = null;
    private boolean mEffectDone;
    private static int[] mTextures = new int[2];
   
    static {
        mTextures[0] = -1;
        mTextures[1] = -1;
    }
    
    public StereoConvert2DTo3D(Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public Bitmap getBitmapAfterConvert() {
        return mBitmapAfterConvert;
    }

    public void run(EffectContext effectContext) {
        Log.i(TAG, "<run> begin");
        if (mTextures[0] == -1 && mTextures[1] == -1) {
            GLES20.glGenTextures(2, mTextures, 0);
        }
        // Upload to texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        
        EffectFactory effectFactory = effectContext.getFactory();
        Stereo3D2Dto3DEffect effect = (Stereo3D2Dto3DEffect) effectFactory
                .createEffect(MTKEffectFactory.EFFECT_2DTO3D);
        effect.setUpdateListener(new EffectUpdateListener() {
            public void onEffectUpdated(Effect effect, Object info) {
                mBitmapAfterConvert = (Bitmap) info;
                Log.i(TAG, "<onEffectUpdated> get bitmap after convert = " + mBitmapAfterConvert
                        + ", w = " + mBitmapAfterConvert.getWidth() + ", h = "
                        + mBitmapAfterConvert.getHeight());
                mBitmap = null;
                mEffectDone = true;
                synchronized (StereoConvert2DTo3D.this) {
                    StereoConvert2DTo3D.this.notifyAll();
                }
            }
        });
        mEffectDone = false;
        try {
            effect.apply(mTextures[0], mBitmap.getWidth(), mBitmap.getHeight(), mTextures[1]);
        } catch (Exception e) {
            Log.i(TAG, "<run> exception occur: " + e.getMessage());
            mEffectDone = true;
            synchronized (StereoConvert2DTo3D.this) {
                StereoConvert2DTo3D.this.notifyAll();
            }
        }
    }

    public boolean isEffectDone() {
        return mEffectDone;
    }
}