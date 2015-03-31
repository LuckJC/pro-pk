package com.mediatek.gallery3d.ext;

import android.net.Uri;
/**
 * Movie info class
 */
public interface IMovieItem {
    /**
     * @return movie Uri, it's may be not the original Uri.
     */
    Uri getUri();
    /**
     * 
     * @return MIME type of video
     */
    String getMimeType();
    /**
     * 
     * @return title of video
     */
    String getTitle();
    /**
     * 
     * @return whether error occured or not.
     */
    boolean getError();
    /**
     * 
     * @return id of video
     */
    int getId();
    /**
     * 
     * @return stereo convergence of video
     */
    int getConvergence();
    /**
     * 
     * @return stereo type of video
     */
    int getStereoType();
    /**
     * set title of video
     * @param title
     */
    void setTitle(String title);
    /**
     * set video Uri
     * @param uri
     */
    void setUri(Uri uri);
    /**
     * Set MIME type of video
     * @param mimeType
     */
    void setMimeType(String mimeType);
    /**
     * Set id of video
     * @param id
     */
    void setId(int id);
    /**
     * Set stereo convergence of video
     * @param convergence
     */
    void setConvergence(int convergence);
    /**
     * Set stereo type of video
     * @param stereoType
     */
    void setStereoType(int stereoType);
    /**
     * Set error occured flag
     */
    void setError();
    /**
     * 
     * @return return original Uri of video.
     */
    Uri getOriginalUri();
    /**
     * Set video original Uri.
     * @param uri
     */
    void setOriginalUri(Uri uri);
}