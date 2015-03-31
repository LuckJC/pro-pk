package com.mediatek.gallery3d.stereo;

import java.util.ArrayDeque;

import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.util.Log;
import com.mediatek.gallery3d.util.BackgroundRenderer;
import com.mediatek.gallery3d.util.BackgroundRenderer.BackgroundGLTask;

import android.media.effect.EffectContext;
import android.os.SystemClock;

public class StereoEffectHandle implements BackgroundGLTask {
    private final static String TAG = "StereoEffectHandle";
    private final static int INIT_CAPACITY = 8;
    private final static int EFFECT_LIMIT = 40;
    
    private final ArrayDeque<StereoEffect> mEffects =
        new ArrayDeque<StereoEffect>(INIT_CAPACITY);
    private boolean mIsQueued = false;
    private EffectContext mEffectContext = null;
    private static StereoEffectHandle mInstance;
    
    private  StereoEffectHandle() {
    }
    
    public static synchronized StereoEffectHandle getInstance() {
        if (mInstance == null) {
            mInstance = new StereoEffectHandle();
        }
        return mInstance;
    }

    public synchronized void clear() {
        mEffects.clear();
    }
    
    public synchronized void addEffect(StereoEffect effect) {
        mEffects.addLast(effect);

        if (mIsQueued) return;
        mIsQueued = true;
        BackgroundRenderer.getInstance().addGLTask(this);
        BackgroundRenderer.getInstance().requestRender();
    }
    
    @Override
    public boolean run(GLCanvas canvas) {
        ArrayDeque<StereoEffect> deque = mEffects;
        synchronized (this) {
            long now = SystemClock.uptimeMillis();
            long dueTime = now + EFFECT_LIMIT;
            while(now < dueTime && !deque.isEmpty()) {
                StereoEffect t = deque.pollFirst();
                mEffectContext = EffectContext.createWithCurrentGlContext();
                t.run(mEffectContext);
                mEffectContext.release();
                mEffectContext = null;
                now = SystemClock.uptimeMillis();
            }
            mIsQueued = !mEffects.isEmpty();
            // return true to keep this listener in the queue
            return mIsQueued;
        }
    }
    
    public interface StereoEffect {
        public void run(EffectContext effectContext);
        public boolean isEffectDone();
    }
}