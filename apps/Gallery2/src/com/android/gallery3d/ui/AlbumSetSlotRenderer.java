/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.ui;

import android.content.Context;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.AlbumSetDataLoader;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.ColorTexture;
import com.android.gallery3d.glrenderer.FadeInTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.glrenderer.TiledTexture;
import com.android.gallery3d.glrenderer.UploadedTexture;
import com.android.gallery3d.ui.AlbumSetSlidingWindow.AlbumSetEntry;

// M: Mediatek import
import com.mediatek.gallery3d.util.MediatekFeature;
/// M: Video thumbnail play @{
import com.mediatek.gallery3d.videothumbnail.VideoThumbnailSourceWindow;
import com.mediatek.gallery3d.videothumbnail.VideoThumbnailDirector;
import com.mediatek.gallery3d.videothumbnail.VideoThumbnailFeatureOption;
/// @}

public class AlbumSetSlotRenderer extends AbstractSlotRenderer implements
        VideoThumbnailSourceWindow.StageContext {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/AlbumSetSlotRenderer";
    private static final int CACHE_SIZE = 96;
    private final int mPlaceholderColor;

    private final ColorTexture mWaitLoadingTexture;
    private final ResourceTexture mCameraOverlay;
    private final AbstractGalleryActivity mActivity;
    private final SelectionManager mSelectionManager;
    protected final LabelSpec mLabelSpec;

    protected AlbumSetSlidingWindow mDataWindow;
    private SlotView mSlotView;

    private int mPressedIndex = -1;
    private boolean mAnimatePressedUp;
    private Path mHighlightItemPath = null;
    private boolean mInSelectionMode;

    /// M: added for performance auto test
    public static long mWaitFinishedTime = 0;
    
    public static class LabelSpec {
        public int labelBackgroundHeight;
        public int titleOffset;
        public int countOffset;
        public int titleFontSize;
        public int countFontSize;
        public int leftMargin;
        public int iconSize;
        public int titleRightMargin;
        public int backgroundColor;
        public int titleColor;
        public int countColor;
        public int borderSize;
    }

    public AlbumSetSlotRenderer(AbstractGalleryActivity activity,
            SelectionManager selectionManager,
            SlotView slotView, LabelSpec labelSpec, int placeholderColor) {
        super (activity);
        mActivity = activity;
        mSelectionManager = selectionManager;
        mSlotView = slotView;
        mLabelSpec = labelSpec;
        mPlaceholderColor = placeholderColor;

        mWaitLoadingTexture = new ColorTexture(mPlaceholderColor);
        mWaitLoadingTexture.setSize(1, 1);
        mCameraOverlay = new ResourceTexture(activity,
                R.drawable.ic_cameraalbum_overlay);
        /// M: Video thumbnail play @{
        if (VideoThumbnailFeatureOption.OPTION_ENABLE_THIS_FEATRUE) {
            mVideoThumbnailDirector = new VideoThumbnailDirector(this);
        }
        /// @}
    }

    public void setPressedIndex(int index) {
        if (mPressedIndex == index) return;
        mPressedIndex = index;
        mSlotView.invalidate();
    }

    public void setPressedUp() {
        if (mPressedIndex == -1) return;
        mAnimatePressedUp = true;
        mSlotView.invalidate();
    }

    public void setHighlightItemPath(Path path) {
        if (mHighlightItemPath == path) return;
        mHighlightItemPath = path;
        mSlotView.invalidate();
    }

    public void setModel(AlbumSetDataLoader model) {
        if (mDataWindow != null) {
            mDataWindow.setListener(null);
            mDataWindow = null;
            mSlotView.setSlotCount(0);
        }
        if (model != null) {
            mDataWindow = new AlbumSetSlidingWindow(
                    mActivity, model, mLabelSpec, CACHE_SIZE);
            mDataWindow.setListener(new MyCacheListener());
            mSlotView.setSlotCount(mDataWindow.size());
        }
    }

    private static Texture checkLabelTexture(Texture texture) {
        return ((texture instanceof UploadedTexture)
                && ((UploadedTexture) texture).isUploading())
                ? null
                : texture;
    }

    private static Texture checkContentTexture(Texture texture) {
        return ((texture instanceof TiledTexture)
                && !((TiledTexture) texture).isReady())
                ? null
                : texture;
    }

    @Override
    public int renderSlot(GLCanvas canvas, int index, int pass, int width, int height) {
        AlbumSetEntry entry = mDataWindow.get(index);
        int renderRequestFlags = 0;
        renderRequestFlags |= renderContent(canvas, entry, width, height);
        renderRequestFlags |= renderLabel(canvas, entry, width, height);
        renderRequestFlags |= renderOverlay(canvas, index, entry, width, height);
        return renderRequestFlags;
    }

    protected int renderOverlay(
            GLCanvas canvas, int index, AlbumSetEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        if (entry.album != null && entry.album.isCameraRoll()) {
            int uncoveredHeight = height - mLabelSpec.labelBackgroundHeight;
            int dim = uncoveredHeight / 2;
            mCameraOverlay.draw(canvas, (width - dim) / 2,
                    (uncoveredHeight - dim) / 2, dim, dim);
        }
        if (mPressedIndex == index) {
            if (mAnimatePressedUp) {
                drawPressedUpFrame(canvas, width, height);
                renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
                if (isPressedUpFrameFinished()) {
                    mAnimatePressedUp = false;
                    mPressedIndex = -1;
                }
            } else {
                drawPressedFrame(canvas, width, height);
            }
        } else if ((mHighlightItemPath != null) && (mHighlightItemPath == entry.setPath)) {
            drawSelectedFrame(canvas, width, height);
        } else if (mInSelectionMode && mSelectionManager.isItemSelected(entry.setPath)) {
            drawSelectedFrame(canvas, width, height);
        }
        return renderRequestFlags;
    }

    protected int renderContent(
            GLCanvas canvas, AlbumSetEntry entry, int width, int height) {
        int renderRequestFlags = 0;
        
        Texture content = checkContentTexture(entry.content);
        if (content == null) {
            content = mWaitLoadingTexture;
            entry.isWaitLoadingDisplayed = true;
        } else if (entry.isWaitLoadingDisplayed) {
            entry.isWaitLoadingDisplayed = false;
            //FadeInTexture will be transparent when launch gallery
            //content = new FadeInTexture(mPlaceholderColor, entry.bitmapTexture);
            content = entry.bitmapTexture;
            entry.content = content;
            /// M: added for performance auto test 
            mWaitFinishedTime = System.currentTimeMillis();
        }
        /// M: Video thumbnail play @{
        if ((!VideoThumbnailFeatureOption.OPTION_ENABLE_THIS_FEATRUE)
                || (!mVideoThumbnailDirector.renderThumbnail(entry, canvas,
                        width, height))) {
        /// @}
        if (MediatekFeature.permitShowThumb(entry.subType)) {
            drawContent(canvas, content, width, height, entry.rotation);
        } else {
            drawContent(canvas, mWaitLoadingTexture, width, height, entry.rotation);
        }
        if ((content instanceof FadeInTexture) &&
                ((FadeInTexture) content).isAnimating()) {
            renderRequestFlags |= SlotView.RENDER_MORE_FRAME;
        }
        /// M: Video thumbnail play @{
        }
        /// @}

        /// M: livephoto doesn't show videoOverlay.
        if (entry.mediaType == MediaObject.MEDIA_TYPE_VIDEO && entry.subType != MediaObject.SUBTYPE_LIVEPHOTO) {
            drawVideoOverlay(canvas, width, height);
        }

        /// M: don't show Panorama Icon at AlbumSetPage
        /*if (entry.isPanorama) {
            //drawPanoramaBorder(canvas, width, height);
            drawPanoramaIcon(canvas, width, height);
        }*/

        /// M: don't show all SubType Icon at AlbumSetPage
        /*MediatekFeature.renderSubTypeOverlay(mActivity.getAndroidContext(),
            canvas, width, height, entry.subType & ~MediatekFeature.allStereoSubType());*/

        return renderRequestFlags;
    }

    protected int renderLabel(
            GLCanvas canvas, AlbumSetEntry entry, int width, int height) {
        Texture content = checkLabelTexture(entry.labelTexture);
        if (content == null) {
            content = mWaitLoadingTexture;
        }
        int b = AlbumLabelMaker.getBorderSize();
        int h = mLabelSpec.labelBackgroundHeight;
        content.draw(canvas, -b, height - h + b, width + b + b, h);

        return 0;
    }

    @Override
    public void prepareDrawing() {
        mInSelectionMode = mSelectionManager.inSelectionMode();
    }

    private class MyCacheListener implements AlbumSetSlidingWindow.Listener {

        @Override
        public void onSizeChanged(int size) {
            mSlotView.setSlotCount(size);
            // M: don't forget to invalidate, or UI will not refresh...
            mSlotView.invalidate();
        }

        @Override
        public void onContentChanged() {
            /// M: Video thumbnail play @{
            //After AlbumSetEntry update, we should re-collection new cover item
            if (VideoThumbnailFeatureOption.OPTION_ENABLE_THIS_FEATRUE) {
                if (mDataWindow.getActiveRequestCount() == 0) {
                    mVideoThumbnailDirector.pumpLiveThumbnails();
                }
            }
            /// @}
            mSlotView.invalidate();
        }
    }

    public void pause() {
        mDataWindow.pause();
        /// M: Video thumbnail play @{
        if (VideoThumbnailFeatureOption.OPTION_ENABLE_THIS_FEATRUE) {
            mVideoThumbnailDirector.pause();
        }
        /// @}
    }

    public void resume() {
        mDataWindow.resume();
        /// M: Video thumbnail play @{
        if (VideoThumbnailFeatureOption.OPTION_ENABLE_THIS_FEATRUE) {
            mVideoThumbnailDirector.resume(mDataWindow);
        }
        /// @}
    }

    @Override
    public void onVisibleRangeChanged(int visibleStart, int visibleEnd) {
        if (mDataWindow != null) {
            mDataWindow.setActiveWindow(visibleStart, visibleEnd);
            /// M: Video thumbnail play @{
            if (VideoThumbnailFeatureOption.OPTION_ENABLE_THIS_FEATRUE) {
                mVideoThumbnailDirector.updateStage();
            }
            /// @}
        }
    }

    @Override
    public void onSlotSizeChanged(int width, int height) {
        if (mDataWindow != null) {
            mDataWindow.onSlotSizeChanged(width, height);
        }
    }

  /// M: Video thumbnail play @{
    private VideoThumbnailDirector mVideoThumbnailDirector;

    public boolean isStageChanging() {
        return !mSlotView.isScollingFinished();
        // return false; // assume not scrolling
    }

    public AbstractGalleryActivity getGalleryActivity() {
        return mActivity;
    }
/// @}
}
