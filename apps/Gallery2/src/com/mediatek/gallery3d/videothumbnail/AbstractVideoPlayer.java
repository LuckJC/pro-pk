package com.mediatek.gallery3d.videothumbnail;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRoot.OnGLIdleListener;

public abstract class AbstractVideoPlayer {
    protected String mPath = null;
    protected MediaItem mItem = null;
    protected AbstractGalleryActivity mGalleryActivity;

    public abstract boolean prepare();
    public abstract void release();
    public abstract boolean start();
    // place holder, not called in present logic, randomly implementing it is ok
    // we may use this routine in the future code
    public abstract boolean pause();
    public abstract boolean stop();
    public abstract boolean render(GLCanvas canvas, int width, int height);

    protected void requestRender() {
        GLRoot renderContext = mGalleryActivity.getGLRoot();
        if (renderContext != null) {
            renderContext.requestRender();
        }
    }

    protected void addOnGLIdleListener(OnGLIdleListener listener) {
        GLRoot renderContext = mGalleryActivity.getGLRoot();
        if (renderContext != null) {
            renderContext.addOnGLIdleListener(listener);
        }
    }

    protected void addGLContextRequiredOnGLIdleListener(OnGLIdleListener listener) {
        GLRoot renderContext = mGalleryActivity.getGLRoot();
        if (renderContext != null) {
            renderContext.addGLContextRequiredOnGLIdleListener(listener);
        }
    }
}
