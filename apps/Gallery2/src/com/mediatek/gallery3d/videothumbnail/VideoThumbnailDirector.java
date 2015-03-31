package com.mediatek.gallery3d.videothumbnail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.LocalMediaItem;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLRoot;
import com.android.gallery3d.ui.GLRootView;
import com.mediatek.gallery3d.videothumbnail.VideoThumbnailSourceWindow.StageContext;

import android.util.Log;

public class VideoThumbnailDirector {
    private static final String TAG = "Gallery2/VideoThumbnailDirector";
    private final int HANDLER_CONCURRENCY = 2;

    private VideoThumbnailPlayer mThumbnailPlayer;
    private AbstractGalleryActivity mGalleryActivity;
    private VideoThumbnailSourceWindow mThumbnailSource;
    private StageContext mStageContext;

    private DirectorSecretary mSecretary;
    private Object mLockSecretaryBeauty = new Object();
    private int mCurrentStarterIndex;
    private int mCurrentStoperIndex;
    private volatile int mActiveStart = 0;
    private volatile int mActiveEnd = 0;
//    private volatile int mContentStart = 0;
//    private volatile int mContentEnd = 0;
    private volatile boolean mIsStageUpdated;
    private PlayerHandler[] mThumbnailStarters = new PlayerHandler[HANDLER_CONCURRENCY];
    private PlayerHandler[] mThumbnailStopers = new PlayerHandler[HANDLER_CONCURRENCY];

    public VideoThumbnailDirector(StageContext stageContext) {
        mStageContext = stageContext;
        mGalleryActivity = stageContext.getGalleryActivity();
    }

    public void resume(VideoThumbnailSourceWindow dataWindow) {
        VideoThumbnailMaker.setDirector(this);
        pumpLiveThumbnails();
        mThumbnailSource = dataWindow;
        VideoThumbnailMaker.start();
        mThumbnailPlayer = VideoThumbnailPlayer.create(mGalleryActivity);
        mThumbnailPlayer.resume();
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            mThumbnailStarters[i] = new PlayerHandler();
            mThumbnailStopers[i] = new PlayerHandler();
            mThumbnailStarters[i].start();
            mThumbnailStopers[i].start();            
        }
        mSecretary = new DirectorSecretary();
        mSecretary.start();
    }

    public void pause() {
        mSecretary.interrupt();
        mSecretary = null;
        new Thread() {
            public void run() {
                setName("thumbnail player pauser");
                mThumbnailPlayer.pause();
            }
        }.start();
        VideoThumbnailMaker.pause();
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            if (mThumbnailStarters[i] != null) {
                mThumbnailStarters[i].interrupt();
            }
            if (mThumbnailStopers[i] != null) {
                mThumbnailStopers[i].interrupt();
            }
        }
    }

    // in GL thread
    public boolean renderThumbnail(
            final VideoThumbnailSourceWindow.DataEntry entry, final GLCanvas canvas,
            final int width, final int height) {
        LocalMediaItem mediaItem;
        try {
            mediaItem = (LocalMediaItem) (entry.getPlayItem());
        } catch (ClassCastException e) {
            mediaItem = null;
        }
        if (mediaItem != null) {
            if (LocalMediaItem.TPT_GENERATE_PLAY == mediaItem.thumbnailPlayType) {
                AbstractVideoGenerator videoGenerator = mediaItem.mVideoGenerator;
                if (videoGenerator.videoState[AbstractVideoGenerator.VTYPE_THUMB] == AbstractVideoGenerator.STATE_GENERATED) {
                    GLRootView.sVideoThumbnailPlaying = true;
//                    return mThumbnailPlayer
//                        .renderThumbnail(
//                                videoGenerator.videoPath[AbstractVideoGenerator.VTYPE_THUMB],
//                                canvas, width, height);
                    // wa-hint: for continous shot alps1296847
                    // in simple cases, it seems workable and harmless
                    // but this should be re-evaluated when adding new animating image types,
                    // or when fancy layout enabled one day, or ...
                    // I'll move this code snippet into newly created ContraninerImagePlayer later
                    // to narrow the influence of this code
                    int rotation = mediaItem.getRotation();
                    if (rotation != 0) {
                        canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
                        int offset = Math.min(width, height) / 2;
                        if (rotation != 0) {
                            canvas.translate(offset, offset);
                            canvas.rotate(rotation, 0, 0, 1);
                            canvas.translate(-offset, -offset);
                        }
                    }
                    /*return */boolean isRenderSuccess = mThumbnailPlayer
                        .renderThumbnail(
                                videoGenerator.videoPath[AbstractVideoGenerator.VTYPE_THUMB],
                                canvas, width, height);
                    if (rotation != 0) {
                        canvas.restore();
                    }
                    return isRenderSuccess;
                }
            } else if (LocalMediaItem.TPT_DIRECT_PLAY == mediaItem.thumbnailPlayType) {
                return mThumbnailPlayer.renderThumbnail(mediaItem.filePath, canvas, width,
                        height);
            }
        }
        return false;
    }

    // in main thread
    public void updateStage() {
        mActiveStart = mThumbnailSource.getActiveStart();
        mActiveEnd = mThumbnailSource.getActiveEnd();
//        mContentStart = mDataWindow.getContentStart();
//        mContentEnd = mDataWindow.getContentEnd();
        pumpLiveThumbnails();
    }

    public void pumpLiveThumbnails() {
        synchronized (mLockSecretaryBeauty) {
            mIsStageUpdated = true;
            mLockSecretaryBeauty.notifyAll();
        }
    }

    private boolean isThumbnailInStage(final String thumbnailPath) {
        VideoThumbnailSourceWindow.DataEntry entry;
        boolean isInStage = false;
        LocalMediaItem mediaItem;
        for (int i = mActiveStart; i < mActiveEnd; i++) {
            if (mThumbnailSource == null) {
                break;
            }
            entry = mThumbnailSource.getThumbnailEntryAt(i);
            if (entry == null) {
                continue;
            }
            try {
                mediaItem = (LocalMediaItem) (entry.getPlayItem());
            } catch (ClassCastException e) {
                mediaItem = null;
            }
            if (mediaItem == null) {
                continue;
            }
            if (LocalMediaItem.TPT_GENERATE_PLAY == mediaItem.thumbnailPlayType) {
                if (thumbnailPath
                        .equals(mediaItem.mVideoGenerator.videoPath[AbstractVideoGenerator.VTYPE_THUMB])) {
                    isInStage = true;
                    break;
                }
            } else if (LocalMediaItem.TPT_DIRECT_PLAY == mediaItem.thumbnailPlayType) {
                if (thumbnailPath.equals(mediaItem.filePath)) {
                    isInStage = true;
                    break;
                }
            }
        }
        return isInStage;
    }

    // in GL thread
    // TODO you know ...
    private void respondToStageUpdate() {
        if (!mIsStageUpdated) {
            return;
        }

        // make sure all static thumbnails go first
        if (!mThumbnailSource.isAllActiveSlotsStaticThumbnailReady()) {
            return;
        }
        
        List<String> thumbnailPaths = new ArrayList<String>();
        mThumbnailPlayer.getPlayingThumbnails(thumbnailPaths);
        if (!scrollThumbnailsOutOfScene(thumbnailPaths)) {
            return;
        }

        List<LocalMediaItem> thumbnailsOnStage = new ArrayList<LocalMediaItem>();
        this.collectAllThumbnailsOnStage(thumbnailsOnStage);

        this.requestThumbnails(thumbnailsOnStage);        

        // start new visible videos (enqueue for starting)
        // temprarily ...
        List<LocalMediaItem> preparedThumbnails = new ArrayList<LocalMediaItem>();
        this.getPreparedThumbnails(thumbnailsOnStage, preparedThumbnails);
        if (!scrollThumbnailsIntoScene(preparedThumbnails)) {
            return;   
        }

        mIsStageUpdated = false;
    }

    private void getPreparedThumbnails(
            final List<LocalMediaItem> candidateThumbnails,
            List<LocalMediaItem> preparedThumbnails) {
        preparedThumbnails.clear();
        for (LocalMediaItem mediaItem : candidateThumbnails) {
            if (LocalMediaItem.TPT_GENERATE_PLAY == mediaItem.thumbnailPlayType) {
                if (mediaItem.mVideoGenerator.videoState[AbstractVideoGenerator.VTYPE_THUMB] == AbstractVideoGenerator.STATE_GENERATED) {
                    preparedThumbnails.add(mediaItem);
                }
            } else if (LocalMediaItem.TPT_DIRECT_PLAY == mediaItem.thumbnailPlayType) {
                preparedThumbnails.add(mediaItem);
            }
        }
    }

    private void requestThumbnails(List<LocalMediaItem> thumbnails) {
        VideoThumbnailMaker.cancelPendingTranscode();
        for (LocalMediaItem mediaItem : thumbnails) {
            mediaItem.prepareThumbnailPlay();
            if (LocalMediaItem.TPT_GENERATE_PLAY == mediaItem.thumbnailPlayType
                    && mediaItem.mVideoGenerator.videoState[AbstractVideoGenerator.VTYPE_THUMB] == AbstractVideoGenerator.STATE_NEED_GENERATE) {
                VideoThumbnailMaker.requestThumbnail(mediaItem);
            }
        }
    }

    // returning false: without real starting
    private boolean scrollThumbnailsIntoScene(final List<LocalMediaItem> items) {
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            if (mThumbnailStarters[i] != null) {
                mThumbnailStarters[i].cancelPendingRunnables();
            }
        }
        if (mStageContext.isStageChanging()) {
            return false;
        }
        return startThumbnails(items);
    }

    private final Set<String> mPlayingPaths = new HashSet<String>();
    private final Object mPlayingPathLock = new Object();
    private boolean startThumbnails(final List<LocalMediaItem> items) {
        int currentStarterIndex;
        String thumbnailPath = null;
        for (final LocalMediaItem item : items) {
            currentStarterIndex = mCurrentStarterIndex;
            currentStarterIndex %= HANDLER_CONCURRENCY;
            mCurrentStarterIndex ++;
            mCurrentStarterIndex %= HANDLER_CONCURRENCY;
            final PlayerHandler playerStarter = mThumbnailStarters[currentStarterIndex];
            if (playerStarter != null) {                
                if (LocalMediaItem.TPT_GENERATE_PLAY == item.thumbnailPlayType) {
                    AbstractVideoGenerator videoGenerator = item.mVideoGenerator;
                    if (videoGenerator.videoState[AbstractVideoGenerator.VTYPE_THUMB] == AbstractVideoGenerator.STATE_GENERATED) {
                        thumbnailPath = videoGenerator.videoPath[AbstractVideoGenerator.VTYPE_THUMB];
                    }
                } else if (LocalMediaItem.TPT_DIRECT_PLAY == item.thumbnailPlayType) {
                    thumbnailPath = item.filePath;
                }

                final String path = new String(thumbnailPath);              
                playerStarter.submit(new Runnable() {
                    public void run() {
                        if (!mThumbnailPlayer.isThumbnailPlaying(path)
                                && isThumbnailInStage(path)) {
                            if (!mStageContext.isStageChanging()) {
                                synchronized (mPlayingPathLock) {
                                    if (!mPlayingPaths.add(path)) {
                                        // this opening already in process
                                        return;
                                    }
                                }
                                boolean isOpenSuc = mThumbnailPlayer.openThumbnail(path, item);
                                synchronized (mPlayingPathLock) {
                                    mPlayingPaths.remove(path);
                                }
                                if (isOpenSuc) {
                                    return;
                                }
                            }
                            // hey, retry
                            playerStarter.submit(this);
                        }
                    }
                });
            }
        }
        return true;
    }

    // returning false: without real stopping
    private boolean scrollThumbnailsOutOfScene(final List<String> thumbnailPaths) {
        for (int i = 0; i < HANDLER_CONCURRENCY; i++) {
            if (mThumbnailStopers[i] != null) {
                mThumbnailStopers[i].cancelPendingRunnables();
            }
        }
        if (mStageContext.isStageChanging()) {
            return false;
        }
        return stopThumbnails(thumbnailPaths, false);
    }

    // returning false: without real stopping
    private boolean stopThumbnails(final List<String> thumbnailPaths, final boolean forceClose) {
        int currentStoperIndex;
        for (final String path : thumbnailPaths) {
            currentStoperIndex = mCurrentStoperIndex;
            currentStoperIndex %= HANDLER_CONCURRENCY;
            mCurrentStoperIndex ++;
            mCurrentStoperIndex %= HANDLER_CONCURRENCY;
            final PlayerHandler playerStoper = mThumbnailStopers[currentStoperIndex];
            if (playerStoper != null) {
                playerStoper.submit(
                    new Runnable() {
                        public void run() {
                            boolean shouldDelete = forceClose ? true : !isThumbnailInStage(path);
                            if (shouldDelete) {
                                mThumbnailPlayer.closeThumbnail(path);
                            } else {
                                try {
                                    playerStoper.mRunnableQueue.take().run();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }
                    }
                );
            }
        }
        return true;
    }

    private void collectAllThumbnailsOnStage(List<LocalMediaItem> mediaItems) {
        mediaItems.clear();
        VideoThumbnailSourceWindow.DataEntry entry;
        LocalMediaItem mediaItem;
        for (int i = mActiveStart; i < mActiveEnd; i++) {
            // TODO: move the below outside of for
            if (mThumbnailSource == null) {
                break;
            }
            entry = mThumbnailSource.getThumbnailEntryAt(i);
            if (entry == null) {
                continue;
            }
            try {
                mediaItem = (LocalMediaItem) (entry.getPlayItem());
            } catch (ClassCastException e) {
                mediaItem = null;
            }
            if (mediaItem != null) {
                mediaItems.add(mediaItem);
            }
        }
    }

    public void requestRender() {
        // Uh..., renderContext can be cached
        // But can it be null or changed during Gallery scenario?
        GLRoot renderContext = mGalleryActivity.getGLRoot();
        if (renderContext != null) {
            renderContext.requestRender();
        }
    }

    private static class PlayerHandler extends Thread {
        // TODO: use blocking stack is more friendly, to be modified
        public final BlockingQueue<Runnable> mRunnableQueue;
        private Runnable mCurrentRunnable;

        public PlayerHandler() {
            super("Player handler");
            mRunnableQueue = new LinkedBlockingQueue<Runnable>();
        }

        public void run() {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                while(!Thread.currentThread().isInterrupted()) {
                    synchronized (this) {
                        while (mPausing) {
                            wait();
                        }
                        mCurrentRunnable = mRunnableQueue.take();                    
                    }
                    mCurrentRunnable.run();
//                    try {
//                        Thread.sleep(300);
//                    } catch (InterruptedException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
                }
            } catch (InterruptedException e) {
                this.interrupt();
            }
        }

        public void submit(Runnable runnable) {
            if (isAlive()) {
                mRunnableQueue.add(runnable);
            } else {
                Log.e(TAG, getName() + " should be started before submitting tasks.");
            }
        }

        private volatile boolean mPausing;

        public void cancelPendingRunnables() {
            mRunnableQueue.clear();
        }
    }

    private class DirectorSecretary extends Thread {
        public void run() {
            setName("pretty secretary");
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (mLockSecretaryBeauty) {
                    while (!mIsStageUpdated) {
                        try {
                            mLockSecretaryBeauty.wait();
                        } catch (InterruptedException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                            return;
                        }
                    }
                }
                if (!mStageContext.isStageChanging()) {
                    respondToStageUpdate();
                    try {
                        sleep(80);  // not good
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }
    }

    private static class VideoThumbnailPlayer {
        private static final String TAG = "Gallery2/VideoThumbnailPlayer";
        private static final int INIT_POOL_SIZE = 16;
        private static final int MAX_PLAYER_COUNT = INIT_POOL_SIZE;

        private AbstractGalleryActivity mGalleryActivity;
        //TODO the following two structures can be merged (redundant)
        private final List<AbstractVideoPlayer> mCheckOuts;
        private final Object mCheckOutsLock = new Object();

        // do no harm to performance, for I'll remove it later
        private volatile int mPlayerCount;
        private volatile boolean mIsWorking;

        public VideoThumbnailPlayer(int initialSize, AbstractGalleryActivity activity) {
            mCheckOuts = new ArrayList<AbstractVideoPlayer>(initialSize);
            mGalleryActivity = activity;
        }

        public VideoThumbnailPlayer(AbstractGalleryActivity activity) {
            this(INIT_POOL_SIZE, activity);
        }

        public static VideoThumbnailPlayer create(AbstractGalleryActivity activity) {
            return new VideoThumbnailPlayer(activity);
        }

        private AbstractVideoPlayer getPlayInfoFromPath(String thumbnailPath) {
            synchronized (mCheckOutsLock) {
                if (mIsWorking) {
                    for (AbstractVideoPlayer vtPlayInfo : mCheckOuts) {
                        if (vtPlayInfo.mPath.equals(thumbnailPath)) {
                            return vtPlayInfo;
                        }
                    }
                }
            }
            return null;
        }

        public boolean isThumbnailPlaying(final String thumbnailPath) {
            return (getPlayInfoFromPath(thumbnailPath) != null);
        }

        public void getPlayingThumbnails(List<String> thumbnailPaths) {
            thumbnailPaths.clear();
            synchronized (mCheckOutsLock) {
                for (AbstractVideoPlayer playerInfo : mCheckOuts) {
                    thumbnailPaths.add(playerInfo.mPath);
                }
            }
        }

        public boolean renderThumbnail(final String thumbnailPath, GLCanvas canvas, int width,
                int height) {
            AbstractVideoPlayer vtPlayInfo = getPlayInfoFromPath(thumbnailPath);
            if (vtPlayInfo == null) {
                return false;
            }
            return vtPlayInfo.render(canvas, width, height);
        }

        // open a media indicated by its path
        // parameters seems strange here just due to code refactor in process
        public/* synchronized */boolean openThumbnail(String thumbnailPath, LocalMediaItem item) {
            if (!mIsWorking)
                return true;
            if (mPlayerCount == MAX_PLAYER_COUNT) {
                return false;
            }
            if (GLRootView.DThumbClean) {
                return false;
            }
            Log.d(TAG, "create new player in the pool");
            AbstractVideoPlayer vtPlayInfo = item.getVideoPlayer();
                //new CommonVideoPlayer();//new CommonVideoPlayer(mGalleryActivity);
            vtPlayInfo.mGalleryActivity = mGalleryActivity;
            vtPlayInfo.mPath = thumbnailPath;
            vtPlayInfo.mItem = (MediaItem)item;
            mPlayerCount++;

            assert vtPlayInfo != null;
            if (!vtPlayInfo.prepare()) return false;

            // all done, and add to mCheckOuts
            synchronized (mCheckOutsLock) {
                if (mIsWorking && vtPlayInfo.start()) {
                    mCheckOuts.add(vtPlayInfo);
                } else {
                    vtPlayInfo.release();
                }
            }
            return true;
        }

        public/* synchronized */boolean closeThumbnail(String thumbnailPath) {
            AbstractVideoPlayer vtPlayInfo;
            synchronized (mCheckOutsLock) {
                vtPlayInfo = getPlayInfoFromPath(thumbnailPath);
                if (vtPlayInfo == null) {
                    return false;
                }
                mCheckOuts.remove(vtPlayInfo);
            }
            vtPlayInfo.stop();
            vtPlayInfo.release();
            mPlayerCount--;
            return true;
        }

        public void resume() {
            mIsWorking = true;
        }

        public/* synchronized */void pause() {
            synchronized (mCheckOutsLock) {
                mIsWorking = false;
                for (AbstractVideoPlayer vtPlayInfo : mCheckOuts) {
                    //playInfos.add(vtPlayInfo);
                    vtPlayInfo.stop();
                    vtPlayInfo.release();
                }
                mCheckOuts.clear();
                mPlayerCount = MAX_PLAYER_COUNT;
            }
            System.gc();
        }
    }
}
