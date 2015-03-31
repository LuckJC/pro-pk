package com.mediatek.gallery3d.videothumbnail;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.MediaItem;

public interface VideoThumbnailSourceWindow {
    public static interface DataEntry {
        MediaItem getPlayItem();
    }
    
    public static interface StageContext {
        boolean isStageChanging();
        AbstractGalleryActivity getGalleryActivity();
    }
    
    int getActiveStart();
    int getActiveEnd();
    int getContentStart();
    int getContentEnd();
    
    DataEntry getThumbnailEntryAt(int slotIndex);
    boolean isAllActiveSlotsStaticThumbnailReady();
}
