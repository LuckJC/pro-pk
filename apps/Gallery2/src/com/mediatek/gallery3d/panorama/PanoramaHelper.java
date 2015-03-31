package com.mediatek.gallery3d.panorama;

import java.io.FileDescriptor;
import java.io.FileInputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.BitmapFactory.Options;

import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DecodeUtils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.ScreenNail;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.mediatek.gallery3d.panorama.PanoramaConfig;
import com.mediatek.gallery3d.panorama.PanoramaDrawer;
import com.mediatek.gallery3d.util.BackgroundRenderer.BackgroundGLTask;
import com.mediatek.gallery3d.util.BackgroundRenderer;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

public class PanoramaHelper {
    private static final String TAG = "Gallery2/PanoramaHelper";

    public static final int PANORAMA_ASPECT_RATIO_RESIZE = 4;
    public static final int PANORAMA_ASPECT_RATIO_MIN = 3;
    public static final int PANORAMA_ASPECT_RATIO_MAX = 10;
    public static final float MAX_HEIGHT_DEGREE = 50.f;
    public static final int MESH_RADIUS = 4;
    public static final int FRAME_TIME_GAP = 50;
    public static final int FRAME_DEGREE_GAP = 1;

    public static final float MICRO_THUMBNAIL_ANTIALIAS_SCALE = 1.5f;
    public static final float SHARE_VIDEO_ANTIALIAS_SCALE = 1.0f;
    public static final float SCREENNAIL_ANTIALIAS_SCALE = 2.0f;

    public static final float PANORAMA_P80_WIDTHPERCENT = 0.8f;
    public static final float PANORAMA_MIN_WIDTHPERCENT = 0.5f;

    private static int mPanoramaScreenNailWidth;
    private static int mPanoramaScreenNailHeight;

    public static void setPanoramaScreenNailSize(int w, int h) {
        mPanoramaScreenNailWidth = w > h ? w : h;
        mPanoramaScreenNailHeight = h > w ? w : h;
    }

    public static int getPanoramaScreenNailWidth() {
        return mPanoramaScreenNailWidth;
    }

    public static int getPanoramaScreenNailHeight() {
        return mPanoramaScreenNailHeight;
    }

    public static Bitmap resizeBitmapToProperRatio(Bitmap bitmap, boolean recycle) {
        if (bitmap == null) {
            MtkLog.i(TAG, "<resizeBitmapToProperRatio> bitmap == null, return null");
            return null;
        }
        int newWidth = getProperRatioBitmapWidth(bitmap.getWidth(), bitmap.getHeight());
        if (newWidth == bitmap.getWidth())
            return bitmap;

        Bitmap target = Bitmap.createBitmap(newWidth, bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(target);
        canvas.scale((float) newWidth / (float) bitmap.getWidth(), 1.0f);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (recycle)
            bitmap.recycle();
        MtkLog.i(TAG, "<resizeBitmapToProperRatio> resize to w = " + target.getWidth() + ", h = "
                + target.getHeight());
        return target;
    }

    public static int getProperRatioBitmapWidth(int width, int height) {
        if ((float) width / (float) height > PANORAMA_ASPECT_RATIO_RESIZE) {
            return width;
        }
        return height * PANORAMA_ASPECT_RATIO_RESIZE;
    }

    public static Bitmap decodePanoramaBitmap(String filePath, int targetSize) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            FileDescriptor fd = fis.getFD();
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFileDescriptor(fd, null, options);

            int w = options.outWidth;
            int h = options.outHeight;

            float scale = (float) targetSize / Math.max(w, h);
            options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
            options.inJustDecodeBounds = false;
            DecodeUtils.setOptionsMutable(options);
            Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, options);
            if (result == null) {
                return null;
            }
            scale = (float) targetSize / Math.max(result.getWidth(), result.getHeight());
            if (scale <= 0.5)
                result = BitmapUtils.resizeBitmapByScale(result, scale, true);
            return DecodeUtils.ensureGLCompatibleBitmap(result);
        } catch (Exception ex) {
            MtkLog.w(TAG, "<decodePanoramaBitmap> exception occur, " + ex.getMessage());
            return null;
        } finally {
            Utils.closeSilently(fis);
        }
    }

    public static class PanoramaMicroThumbnailEntry {
        public Bitmap mBitmap;
        public PanoramaConfig mConfig;
    }

    public static PanoramaMicroThumbnailEntry getMicroThumbnailEntry(MediaItem item) {
        PanoramaMicroThumbnailEntry entry = new PanoramaMicroThumbnailEntry();
        // decode bitmap
        entry.mBitmap = PanoramaHelper.decodePanoramaBitmap(item.getFilePath(),
                MediatekFeature.Params.MICRO_THUMBNAIL_TARGET_SIZE_PANORAMA);
        if (entry.mBitmap == null)
            return null;
        entry.mBitmap = BitmapUtils.rotateBitmap(entry.mBitmap, item.getRotation(), true);
        entry.mBitmap = PanoramaHelper.resizeBitmapToProperRatio(entry.mBitmap, true);
        // prepare config
        int canvasWidth = MediaItem.getTargetSize(MediaItem.TYPE_MICROTHUMBNAIL);
        int canvasHeight = (int) ((float) PanoramaHelper.getPanoramaScreenNailHeight()
                * (float) canvasWidth / (float) PanoramaHelper.getPanoramaScreenNailWidth());
        entry.mConfig = new PanoramaConfig(entry.mBitmap.getWidth(), entry.mBitmap.getHeight(),
                canvasWidth, canvasHeight, PanoramaHelper.MICRO_THUMBNAIL_ANTIALIAS_SCALE);
        return entry;
    }

    public static float getWidthPercent(int width, int height) {
        float ratio = (float) width / (float) height;
        float widthPercent = 1.f - ratio * 0.04f;
        //widthPercent = widthPercent > PANORAMA_P80_WIDTHPERCENT ? PANORAMA_P80_WIDTHPERCENT
        //        : widthPercent;
        widthPercent = widthPercent < PANORAMA_MIN_WIDTHPERCENT ? PANORAMA_MIN_WIDTHPERCENT
                : widthPercent;
        return widthPercent;
    }

    public static Bitmap getPanoramaMicroThumbnail(MediaItem item, JobContext jc) {
        if (jc.isCancelled()) {
            MtkLog.i(TAG, "<getPanoramaMicroThumbnail> item = " + item.getName() + ", jc is cancel, return null 0");
            return null;
        }
        // prepare
        PanoramaMicroThumbnailEntry entry = PanoramaHelper.getMicroThumbnailEntry(item);
        Bitmap originBitmap = entry.mBitmap;
        PanoramaDrawer panoramaDrawer = new PanoramaDrawer(originBitmap, entry.mConfig);

        // get first frame
        PanoramaFrameTask task = new PanoramaFrameTask(panoramaDrawer, entry.mConfig.mCanvasWidth,
                entry.mConfig.mCanvasHeight);
        BackgroundRenderer.getInstance().addGLTask(task);
        MtkLog.i(TAG, "<getPanoramaMicroThumbnail> add BackgroundGLTask, task = " + task);
        BackgroundRenderer.getInstance().requestRender();
        synchronized (task) {
            while (!task.isDone() && !jc.isCancelled()) {
                try {
                    task.wait(1000);
                } catch (InterruptedException e) {
                    MtkLog.i(TAG, "<getPanoramaMicroThumbnail> InterruptedException: "
                            + e.getMessage());
                }
            }
        }
        if (jc.isCancelled()) {
            Bitmap bitmap = task.get();
            if (bitmap != null) {
                bitmap.recycle();
                bitmap = null;
            }
            if (panoramaDrawer != null) {
                panoramaDrawer.freeResources();
            }
            MtkLog.i(TAG, "<getPanoramaMicroThumbnail> item = " + item.getName() + ", jc is cancel, return null 1");
            return null;
        }

        Bitmap bitmap = task.get();

        if (bitmap == null) {
            if (panoramaDrawer != null) {
                panoramaDrawer.freeResources();
            }
            MtkLog.i(TAG, "<getPanoramaMicroThumbnail> task.get() == null, return null");
            return null;
        }

        // resize and rotate first frame
        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), bitmap
                .getConfig());
        Canvas canvas = new Canvas(newBitmap);
        canvas.drawBitmap(bitmap, 0, (bitmap.getWidth() - bitmap.getHeight()) / 2, null);
        if (item.getRotation() != 0) {
            newBitmap = BitmapUtils.rotateBitmap(newBitmap, -item.getRotation(), true);
        }
        
        // release resources
        bitmap.recycle();
        if (originBitmap != null) {
            originBitmap.recycle();
            originBitmap = null;
        }
        if (panoramaDrawer != null) {
            panoramaDrawer.freeResources();
        }
        // MtkUtils.dumpBitmap(newBitmap, item.getName() + "-MT");
        return newBitmap;
    }

    static class PanoramaFrameTask implements BackgroundGLTask {
        private Bitmap mFrame;
        private boolean mDone = false;
        private PanoramaDrawer mPanoramaDrawer;
        private int mFrameWidth;
        private int mFrameHeight;

        public PanoramaFrameTask(PanoramaDrawer drawer, int width, int height) {
            mPanoramaDrawer = drawer;
            mFrameWidth = width;
            mFrameHeight = height;
        }

        @Override
        public boolean run(GLCanvas canvas) {
            MtkLog.i(TAG, "<PanoramaFrameTask.run> begin to run, task = " + this);
            try {
                if (mPanoramaDrawer == null) {
                    mFrame = null;
                    mDone = true;
                    MtkLog.i(TAG, "<PanoramaFrameTask.run> mPanoramaDrawer == null, return");
                }
                RawTexture texture = new RawTexture(mFrameWidth, mFrameHeight, false);
                mFrame = mPanoramaDrawer.drawOnBitmap(canvas, texture, 0);
                texture.recycle();
                mDone = true;
            } catch (Exception e) {
                mFrame = null;
                mDone = true;
                MtkLog.i(TAG, "<PanoramaFrameTask.run> exception occur, " + e.getMessage());
            }
            MtkLog.i(TAG, "<PanoramaFrameTask.run> end run, task = " + this + ", mFrame = " + mFrame);
            synchronized (PanoramaFrameTask.this) {
                PanoramaFrameTask.this.notifyAll();
            }
            return false;
        }

        public Bitmap get() {
            return mFrame;
        }

        public boolean isDone() {
            return mDone;
        }
    }
    
    
    public static ScreenNail newPlaceholderPanoramaScreenNail(MediaItem item, int color) {
        PanoramaConfig config = null;
        int width;
        int height;
        if(item.getRotation() == 90 || item.getRotation() == 270) {
            height = item.getWidth();
            width = item.getHeight();
        } else {
            height = item.getHeight();
            width = item.getWidth();
        }
        width = PanoramaHelper.getProperRatioBitmapWidth(width, height);
        config = new PanoramaConfig(width, height, PanoramaHelper.getPanoramaScreenNailWidth(),
                PanoramaHelper.getPanoramaScreenNailHeight());
        return new PanoramaScreenNail(color, config);
    }
}