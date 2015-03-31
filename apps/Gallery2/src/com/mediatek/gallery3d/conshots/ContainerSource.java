package com.mediatek.gallery3d.conshots;

import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSource;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.data.PathMatcher;
import com.android.gallery3d.ui.Log;

public class ContainerSource extends MediaSource {

    private static final String TAG = "ContainerSource";

    private static final int CONTAINER_CONSHOT_SET = 0;
    private static final int CONTAINER_MOTION_SET = 1;
    
    private GalleryApp mApplication;
    private PathMatcher mMatcher;
    public static final String CONSHOTS_GROUP = "/container/conshot";
    public static final String MOTION_GROUP = "/container/motion";

    private String subSegment1;
    private String subSegment2;
    
    public ContainerSource(GalleryApp application) {
        super("container");
        mApplication = application;
        mMatcher = new PathMatcher();
        mMatcher.add("/container/conshot/*", CONTAINER_CONSHOT_SET);
        mMatcher.add("/container/motion/*", CONTAINER_MOTION_SET);
    }

    @Override
    public MediaObject createMediaObject(Path path) {
        Log.d("ConShots", "ContainerSource.createMediaObject, path is " + path);
        switch (mMatcher.match(path)) {
        case CONTAINER_CONSHOT_SET:
            splitSegment(mMatcher.getVar(0));
            return new ConShotSet(path, mApplication, Long.parseLong(subSegment1), Integer.parseInt(subSegment2));
        case CONTAINER_MOTION_SET:
            splitSegment(mMatcher.getVar(0));
            return new MotionSet(path, mApplication, subSegment1, Integer.parseInt(subSegment2));
        default:
            throw new RuntimeException("bad path: " + path);
        }
    }

    private void splitSegment(String segment){
        int position = segment.lastIndexOf(":");
        subSegment1 = segment.substring(0, position);
        subSegment2 = segment.substring(position+1);
        Log.d(TAG, "subSegment1:"+subSegment1+" subSegment2:"+subSegment2);
    }

}
