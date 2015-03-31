package com.mediatek.gallery3d.ext;

/**
 * MoviePlayer extension functions interface
 */
public interface IMoviePlayer {
    /**
     * add new bookmark Uri.
     */
    void addBookmark();
    /**
     * start current item and stop playing video.
     * @param item
     */
    void startNextVideo(IMovieItem item);
    /**
     * Loop current video.
     * @param loop
     */
    void setLoop(boolean loop);
    /**
     * Loop current video or not
     * @return
     */
    boolean getLoop();
    /**
     * Show video details.
     */
    void showDetail();
    /**
     * Can stop current video or not.
     * @return
     */
    boolean canStop();
    /**
     * Stop current video.
     */
    void stopVideo();
    /**
     * Set parameter.
     */
    boolean setParameter(int key, int value);
    /**
     * Update MediaPlayer UI element
     */
    void updateUI();
    /**
     *Hide Cotroller UI 
     */
    void hideController();
    /**
     * show current SubtitleView.
     */
    void showSubtitleViewSetDialog();   
    /*
     * Refresh slow motion speed.
     */
    void refreshSlowMotionSpeed(int speed);

}
