package com.mediatek.gallery3d.videothumbnail;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.gallery3d.util.MediatekFeature;

public class VideoThumbnailFeatureOption {
    // whether we enable video thumbnail play feature
    public static final boolean OPTION_ENABLE_THIS_FEATRUE
        = MediatekFeature.IS_HEAVY_FEATURE_SUPPORTED
            && FeatureOption.MTK_VIDEO_THUMBNAIL_PLAY_SUPPORT;
    //mav
    public static final float MAV_THUMBNAILVIDEO_FPS = 15;
    public static final float MAV_SHAREVIDEO_FPS = 15;
    public static final int MAV_THUMBNAILVIDEO_BITRATE = 32 * 1000;
    public static final int MAV_SHAREVIDEO_BITRATE = 1500 * 1000;
    public static final int MAV_THUMBNAILVIDEO_TARGETSIZE = 200;
    public static final int MAV_SHAREVIDEO_TARGETSIZE = 640;
    //panorama
    public static final float PANORAMA_THUMBNAILVIDEO_FPS = 15;
    public static final float PANORAMA_SHAREVIDEO_FPS = 15;
    public static final int PANORAMA_THUMBNAILVIDEO_BITRATE = 32 * 1000;
    public static final int PANORAMA_SHAREVIDEO_BITRATE = 20 * 1000 * 1000;
    public static final int PANORAMA_THUMBNAILVIDEO_TARGETSIZE = 200;
    public static final int PANORAMA_SHAREVIDEO_TARGETSIZE = 640;
    //continue shot
    public static final float CONTAINER_THUMBNAILVIDEO_FPS = (float) 2.5;
    public static final float CONTAINER_SHAREVIDEO_FPS = (float) 2.5;
    public static final int CONTAINER_THUMBNAILVIDEO_BITRATE = 256 * 1000;
    public static final int CONTAINER_SHAREVIDEO_BITRATE = 1500 * 1000;
    public static final int CONTAINER_THUMBNAILVIDEO_TARGETSIZE = 200;
    public static final int CONTAINER_SHAREVIDEO_TARGETSIZE = 640;
}
