package com.mediatek.gallery3d.videothumbnail;

import com.android.gallery3d.data.LocalMediaItem;

public abstract class AbstractVideoGenerator {
    public static final int VTYPE_THUMB = 0;
    public static final int VTYPE_SHARE = 1;
    
    public static final int STATE_NEED_GENERATE = 0;
    public static final int STATE_GENERATING = 1;
    public static final int STATE_GENERATED = 2;
    public static final int STATE_GENERATED_FAIL = 3;
    
    protected static final int DEFAULT_THUMBNAIL_SIZE = 224;

    protected static final int GENERATE_OK = 0;
    protected static final int GENERATE_CANCEL = 1;
    protected static final int GENERATE_ERROR = 2;

    public int[] videoState = {STATE_NEED_GENERATE, STATE_NEED_GENERATE};

    /**
     * Generate a certain type of video for an media item. <br/>
     * This invocation may be blocked in its implementation and presumably
     * time-consuming. To make sure the generating process can be canceled as
     * soon as possible, shouldCancel() should be judged before every
     * significant and time-consuming generating step and if "shouldCancel",
     * give up the following generating routine and return GENERATE_CANCEL.
     * 
     * @param item
     *            the media item for which to generate video
     * @param videoType
     *            the type of video to generate
     * @return GENERATE_OK, GENERATE_CANCEL, GENERATE_ERROR
     */
    public abstract int generate(LocalMediaItem item, int videoType);

    /**
     * Called when canceling a certain type of video generating for an media
     * item. <br/>
     * This is typically useful if the implementation of generate() blocks the
     * generating thread (e.g. by Object.wait()), and in that case,
     * onCancelRequested() should notify (wake up) the thread as soon.
     * 
     * @param item
     *            the media item for which to generate video
     * @param videoType
     *            the type of video to generate
     */
    public abstract void onCancelRequested(LocalMediaItem item, int videoType);

    /**
     * Generally called during generate(), to judge whether the caller intended
     * to cancel that generating pass. It is suggested to call shouldCancel()
     * before every significant and time-consuming generating step
     * 
     */
    protected boolean shouldCancel() {
        return Thread.currentThread().isInterrupted();
    }

    // public volatile int thumbVideoState = STATE_TO_BE_GENERATE;
    // public volatile int shareVideoState = STATE_TO_BE_GENERATE;

    /////////////////////// the following properties would appear in the first phase
    // public String thumbVideoPath = null;
    // public String shareVideoPath = null;
    // or
    public String[] videoPath = {null, null};

    public void prepareToRegenerate(LocalMediaItem item) {
        videoState[VTYPE_THUMB] = videoState[VTYPE_SHARE] = STATE_NEED_GENERATE;
        videoPath[VTYPE_THUMB] = videoPath[VTYPE_SHARE] = null;
        VideoThumbnailHelper.deleteThumbnailFile(item, VTYPE_THUMB);
        VideoThumbnailHelper.deleteThumbnailFile(item, VTYPE_SHARE);
    }
}
