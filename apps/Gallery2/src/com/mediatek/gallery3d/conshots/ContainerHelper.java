package com.mediatek.gallery3d.conshots;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;

import com.android.gallery3d.R;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.app.GalleryApp;
import com.android.gallery3d.common.BitmapUtils;
import com.android.gallery3d.data.LocalImage;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaObject;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.glrenderer.ResourceTexture;
import com.android.gallery3d.glrenderer.Texture;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Log;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MtkUtils;

public class ContainerHelper {
    private static final String TAG = "Gallery2/ContainerHelper";
    
    public static final String CONSHOTS_FOLDER = "/.ConShots";

    public static final int INDEX_GROUP_ID_INVALID = 0;
    
    public static final int CONTAINER_ANIMATION_DELAY = 400; // 200 ms
    public static final int CONTAINER_ANIMATION_MAX_COUNT = 10;
    public static final int CONTAINER_ANIMATION_LAST_REPEAT = 4;
    
    private static AbstractGalleryActivity mActivity;
    private static MediaSet mSource;
    private static boolean mIsAllBestShot;
    private static ResourceTexture mBestShotTexture = null;
    private static ResourceTexture mDisableTexture = null;
    
    public static void copyFile(String fromPath, String toPath) {
        int bytesum = 0;
        int byteread = 0;
        try {
            FileInputStream fsFrom = new FileInputStream(fromPath);
            FileOutputStream fsTo = new FileOutputStream(toPath);
            byte[] buffer = new byte[1024];
            while ((byteread = fsFrom.read(buffer)) != -1) {
                bytesum += byteread;
                fsTo.write(buffer, 0, byteread);
            }
            fsFrom.close();
        } catch (Exception e) {
            e.printStackTrace();
        } 
    }
    
    public static MediaSet getConShotSet(GalleryApp app, long groupID, int bucketId){
        MediaSet mediaset = (MediaSet) app.getDataManager().getMediaObject(
                Path.fromString(ContainerSource.CONSHOTS_GROUP).getChild(groupID + ":"+bucketId));
        return mediaset;
    }
    
    public static MediaSet getMotionSet(GalleryApp app, String fileName, int id){
        MediaSet mediaset = (MediaSet) app.getDataManager().getMediaObject(
                Path.fromString(ContainerSource.MOTION_GROUP).getChild(fileName+":"+id));
        return mediaset;
    }
    
    public static MediaSet getMotionTrackSet(GalleryApp app, String fileName, int id){
        String trackName = fileName+"TK";
        MediaSet mediaset = (MediaSet) app.getDataManager().getMediaObject(
                Path.fromString(ContainerSource.MOTION_GROUP).getChild(trackName+":"+id));
        return mediaset;
    }
    
    public static String getMotionDir(String folderName){
        return MtkUtils.getMtkDefaultPath()+"/DCIM/Camera"+CONSHOTS_FOLDER+"/"+folderName;
    }
    
    public static String getMotionDir(String workDir, String folderName){
        return workDir+CONSHOTS_FOLDER+"/"+folderName;
    }
    
    public static String getSavePath(){
        return MtkUtils.getMtkDefaultPath()+"/DCIM/Camera";
    }
    
    public static void renderOverLay(Context context, GLCanvas canvas, int width, int height, 
            MediaItem item) {
        if(item == null) return;
        
        if(item.isConShot() && MediaItem.IMAGE_BEST_SHOT_MARK_TRUE == item.getIsBestShot()) {
            if (mBestShotTexture == null) {
                mBestShotTexture = new ResourceTexture(context, R.drawable.ic_best_shot);
            }
            int side = Math.min(width, height) / 5;
            int texWidth = mBestShotTexture.getWidth();
            int texHeight = mBestShotTexture.getHeight();
            mBestShotTexture.draw(canvas, 0, height - texHeight, texWidth, texHeight);
        }
        if(item.isMotion() && item.isDisabled()){
            if (mDisableTexture == null) {
                mDisableTexture = new ResourceTexture(context, R.drawable.ic_motion_disable);
            }
            mDisableTexture.draw(canvas, 0, 0, width, height);
        }
    }
    
    public static boolean isConShotImage(MediaObject object) {
        if (object instanceof ConShotImage) {
            return true;
        } else {
            return false;
        }
    }
    
    static public void markBestShotItems(AbstractGalleryActivity activity, MediaSet source) {
        mActivity = activity;
        mSource = source;
        mIsAllBestShot = false;
        if (alreadyMarkBestShot()) {
            Log.i(TAG, "<markBestShot> Has already mark best shot, not mark again, return");
            mIsAllBestShot = isAllBestShot();
            updateBestShotMenu(mActivity);
            return;
        }
         
        if (source.getMediaItemCount() <= 1) {
            MtkLog.i(TAG, "<markBestShot> media item count <= 1, return");
            return;
        }
        activity.getThreadPool().submit(new BestShotSelectJob(),new BestShotSelectListener());
    }

    static public void updateBestShotMenu(AbstractGalleryActivity activity){
        Menu menu = activity.getGalleryActionBar().getMenu();
        if (menu == null) return;
        
        if (mIsAllBestShot) {
            MenuExecutor.setMenuItemVisible(menu, R.id.action_best_shots, false);
        } else {
            MenuExecutor.setMenuItemVisible(menu, R.id.action_best_shots, true);
        }
    }
    
    private static boolean alreadyMarkBestShot() {
        ArrayList<MediaItem> mediaItemList = mSource.getMediaItem(0,mSource.getMediaItemCount());
        for (MediaItem item : mediaItemList) {
            if (!item.isConShot()) return true;
            
            if (item.getIsBestShot() == MediaItem.IMAGE_BEST_SHOT_MARK_TRUE
                    || item.getIsBestShot() == MediaItem.IMAGE_BEST_SHOT_MARK_FALSE) {
                return true;
            }
        }
        return false;
    }
    private static boolean isAllBestShot() {
        ArrayList<MediaItem> mediaItemList = mSource.getMediaItem(0,mSource.getMediaItemCount());
        boolean IsHasBestShot = false;
        boolean IsHasNotBestShot = false;
        for (MediaItem item : mediaItemList) {
            if (!item.isConShot()) return false;
            
            if (item.getIsBestShot() == MediaItem.IMAGE_BEST_SHOT_MARK_TRUE) {
                IsHasBestShot = true;
            } else {
                IsHasNotBestShot = true;
            }
            if (IsHasBestShot && IsHasNotBestShot) {
                return false;
            }
        }
        return true;
    }
    
    static private class BestShotSelectJob implements Job<Void> {
        private ArrayList<MediaItem> mMediaItemList = null;
        public BestShotSelectJob() {
            mMediaItemList = mSource.getMediaItem(0, mSource.getMediaItemCount());
        }
        @Override
        public Void run(JobContext jc) {
            Log.i(TAG, "<BestShotSelectJob.run> begin");
            BestShotSelector selector = new BestShotSelector(mMediaItemList/*, mActivity.getGLRoot(),
                    mThreadPool*/);
            selector.markBestShot();
            return null;
        }
    }
    static private class BestShotSelectListener implements FutureListener<Void> {
        @Override
        public void onFutureDone(Future<Void> future) {
            Log.i(TAG, "<BestShotSelectListener> onFutureDone");
            mActivity.getGLRoot().requestRender();
        }
    }
    public static ArrayList<MediaItem> getAnimationArray(GalleryApp app, MediaItem item){
        ArrayList<MediaItem>  origineItems;
        ArrayList<MediaItem>  ountputItems;
        MediaSet mediaSet;
        int animationMaxCount;
        int animationCount;
        float space;
        int itemNum;
        
        if(!item.isContainer()) return null;
        
        if(item.isConShot()){
            mediaSet = getConShotSet(app, item.getGroupId(), ((LocalMediaItem)item).getBucketId());
        }else if(item.isMotion()){
            mediaSet = getMotionTrackSet(app, item.getName(), ((LocalMediaItem)item).id);
        }else{
            return null;
        }
        if(mediaSet == null || mediaSet.getTotalMediaItemCount() == 0) return null;
        
        itemNum = mediaSet.getMediaItemCount();
        animationMaxCount = CONTAINER_ANIMATION_MAX_COUNT;
        origineItems = mediaSet.getMediaItem(0, itemNum);
        
        if (itemNum == 0 || origineItems.size() == 0) return null;
        
        if(item.isConShot() && itemNum > animationMaxCount)
        {
            space = ((float)itemNum+1)/animationMaxCount;
            animationCount = animationMaxCount;
        }else{
            space = 1;
            animationCount = itemNum;
        }
        
        ountputItems = new ArrayList<MediaItem>(animationCount);
        
        for(int i = 0; i < animationCount; i++){
            if ((int)(i * space) >= origineItems.size()) break;
            ountputItems.add(origineItems.get((int)(i * space)));
            if(i == (animationCount-1) && item.isMotion()){
                for(int j=1; j<CONTAINER_ANIMATION_LAST_REPEAT; j++){
                    ountputItems.add(origineItems.get((int)(i * space)));
                }
            }
        }
        /*
        for (int i = animationCount - 2; i >= 0; i--) {
            ountputItems.add(origineItems.get((int)(i * space)));
        }*/
        return ountputItems;
    }
    
    public static int getConShotDspIndex(MediaSet mediaset, long id){
        ArrayList<MediaItem>  items;

        items = mediaset.getMediaItem(0, mediaset.getMediaItemCount());
        for(int i=0; i<items.size(); i++){
            LocalImage item = (LocalImage)items.get(i);
            if(item.id == id) return i;
        }
        return 0;
    }
    
    public static Bitmap decoderBitmap(String filePath, int targetSize){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(
                options.outWidth, options.outHeight, targetSize);

        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }
}
