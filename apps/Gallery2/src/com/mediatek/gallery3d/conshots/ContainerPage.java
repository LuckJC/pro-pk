package com.mediatek.gallery3d.conshots;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.gallery3d.R;
import com.android.gallery3d.app.ActivityState;
import com.android.gallery3d.app.AlbumDataLoader;
import com.android.gallery3d.app.Config;
import com.android.gallery3d.app.Gallery;
import com.android.gallery3d.app.GalleryActionBar;
import com.android.gallery3d.app.GalleryActivity;
import com.android.gallery3d.app.LoadingListener;
import com.android.gallery3d.app.PhotoPage;
import com.android.gallery3d.app.SlideshowPage;
import com.android.gallery3d.app.Wallpaper;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaDetails;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.ui.ActionModeHandler;
import com.android.gallery3d.ui.ActionModeHandler.ActionModeListener;
import com.android.gallery3d.ui.AlbumSlotRenderer;
import com.android.gallery3d.ui.DetailsHelper;
import com.android.gallery3d.ui.DetailsHelper.CloseListener;
import com.android.gallery3d.filtershow.crop.CropActivity;
import com.android.gallery3d.filtershow.crop.CropExtras;
import com.android.gallery3d.glrenderer.FadeTexture;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.MenuExecutor;
import com.android.gallery3d.ui.PhotoFallbackEffect;
import com.android.gallery3d.ui.RelativePosition;
import com.android.gallery3d.ui.SelectionManager;
import com.android.gallery3d.ui.SlotView;
import com.android.gallery3d.ui.SynchronizedHandler;
import com.android.gallery3d.util.GalleryUtils;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.RotateProgressFragment;

public class ContainerPage extends ActivityState implements
        SelectionManager.SelectionListener {
    @SuppressWarnings("unused")
    private static final String TAG = "Gallery2/ContainerPage";

    private static final int MSG_PICK_PHOTO = 1;
    private static final int MSG_UP_PRESS = 2;
    private static final int MSG_INTO_MOTION_PREVIEW = 3;
    
    public static final String KEY_MEDIA_PATH = "media-path";
    public static final String KEY_PARENT_MEDIA_PATH = "parent-media-path";
    public static final String KEY_SET_CENTER = "set-center";
    public static final String KEY_AUTO_SELECT_ALL = "auto-select-all";
    public static final String KEY_RESUME_ANIMATION = "resume_animation";
    public static final String KEY_BACK_SET_PATH = "back-set-path";
    public static final String KEY_BACK_INDEX = "back-index";
    public static final String KEY_MOTION_SELECT_ENABLE = "motion_select_enable";
    
    private static final int REQUEST_SLIDESHOW = 1;
    private static final int REQUEST_PHOTO = 2;
    private static final int REQUEST_DO_ANIMATION = 3;
    private static final int REQUEST_CROP = 100;
    private static final int REQUEST_CROP_WALLPAPER = 101;
    
    private static final int BIT_LOADING_RELOAD = 1;
//    private static final int BIT_LOADING_SYNC = 2;

    private static final float USER_DISTANCE_METER = 0.3f;

    /// M: for motion manual edit feature
    private static final int MOTION_MANUAL_EDIT_MIN_PIC = 2;
    private static final int MOTION_MANUAL_EDIT_MAX_PIC = 8;
    
    private boolean mIsActive = false;
    private AlbumSlotRenderer mConShotsView;
    private Path mMediaSetPath;
    private String mParentMediaSetString;
    private SlotView mSlotView;
    private GalleryActionBar mActionBar;
    private AlbumDataLoader mConShotsDataAdapter;
    protected SelectionManager mSelectionManager;
    private Vibrator mVibrator;
    private boolean mGetContent;
    private String confirmMsg;
    private MenuExecutor mMenuExecutor;
    private ActionModeHandler mActionModeHandler;
    private int mFocusIndex = 0;
    private DetailsHelper mDetailsHelper;
    private MyDetailsSource mDetailsSource;
    private MediaSet mMediaSet;
    private boolean mShowDetails;
    private float mUserDistance; // in pixel
    private Handler mHandler;
    private AlertDialog mAlertDialog;

    private int mLoadingBits = 0;
    private RelativePosition mOpenCenter = new RelativePosition();

    private MotionTrack mMotionTrack;
    private boolean mInMotionMode = false;
    private boolean mNeedShowPrevState;
    private BroadcastReceiver mStorageReceiver;
    private boolean mIsInBlending = false;
    
    private PhotoFallbackEffect mResumeEffect;
    private PhotoFallbackEffect.PositionProvider mPositionProvider = new PhotoFallbackEffect.PositionProvider() {
        @Override
        public Rect getPosition(int index) {
            Rect rect = mSlotView.getSlotRect(index);
            Rect bounds = mSlotView.bounds();
            rect.offset(bounds.left - mSlotView.getScrollX(), bounds.top
                    - mSlotView.getScrollY());
            return rect;
        }

        @Override
        public int getItemIndex(Path path) {
            int start = mSlotView.getVisibleStart();
            int end = mSlotView.getVisibleEnd();
            for (int i = start; i < end; ++i) {
                MediaItem item = mConShotsDataAdapter.get(i);
                if (item != null && item.getPath() == path) {
                    return i;
                }
            }
            return -1;
        }
    };
    public ArrayList<Path> getAllNotBestShotInConShots() {
        ArrayList<Path> notBestShot = new ArrayList<Path>();
        int total = mMediaSet.getMediaItemCount();
        ArrayList<MediaItem> list = mMediaSet.getMediaItem(0, total);
        for (MediaItem item : list) {
            if(item.getIsBestShot() != MediaItem.IMAGE_BEST_SHOT_MARK_TRUE) {
                Path id = item.getPath();
                notBestShot.add(id);
            }
        }
        return notBestShot;
    }
    
    private final GLView mRootPane = new GLView() {
        private final float mMatrix[] = new float[16];

        @Override
        protected void renderBackground(GLCanvas view) {
            view.clearBuffer();
        }

        @Override
        protected void onLayout(boolean changed, int left, int top, int right,
                int bottom) {

            int slotViewTop = mActivity.getGalleryActionBar().getHeight();
            int slotViewBottom = bottom - top;
            int slotViewRight = right - left;

            if (mShowDetails) {
                mDetailsHelper.layout(left, slotViewTop, right, bottom);
            } else {
                mConShotsView.setHighlightItemPath(null);
            }

            // Set the mSlotView as a reference point to the open animation
            mOpenCenter.setReferencePosition(0, slotViewTop);
            mSlotView.layout(0, slotViewTop, slotViewRight, slotViewBottom);
            GalleryUtils.setViewPointMatrix(mMatrix, (right - left) / 2,
                    (bottom - top) / 2, -mUserDistance);
        }

        @Override
        protected void render(GLCanvas canvas) {
            canvas.save(GLCanvas.SAVE_FLAG_MATRIX);
            canvas.multiplyMatrix(mMatrix, 0);
            super.render(canvas);

            if (mResumeEffect != null) {
                boolean more = mResumeEffect.draw(canvas);
                if (!more) {
                    mResumeEffect = null;
                    mConShotsView.setSlotFilter(null);
                }
                // We want to render one more time even when no more effect
                // required. So that the animated thumbnails could be draw
                // with declarations in super.render().
                invalidate();
            }
            canvas.restore();
        }
    };

    @Override
    protected void onBackPressed() {
        if (mShowDetails) {
            hideDetails();
        } else if (mSelectionManager.inSelectionMode()) {
            mSelectionManager.leaveSelectionMode();
        } else {
            onUpPressed();
        }
    }

    private void onUpPressed() {
        if (mActivity.getStateManager().getStateCount() > 1) {
            super.onBackPressed();
        } else if (mParentMediaSetString != null) {
            Bundle data = new Bundle(getData());
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH, mParentMediaSetString);
            mActivity.getStateManager()
                    .switchState(this, PhotoPage.class, data);
        }
    }

    private void onDown(int index) {
        mConShotsView.setPressedIndex(index);
    }

    private void onUp(boolean followedByLongPress) {
        if (followedByLongPress) {
            // Avoid showing press-up animations for long-press.
            mConShotsView.setPressedIndex(-1);
        } else {
            mConShotsView.setPressedUp();
        }
    }

    private void onSingleTapUp(int slotIndex) {
        if (!mIsActive) {
            MtkLog.i(TAG, "not active, ignore the click");
            return;
        }
        if (mSelectionManager.inSelectionMode()) {
            MediaItem item = mConShotsDataAdapter.get(slotIndex);
            if (item == null) {
                MtkLog.i(TAG, "Item not ready yet, ignore the click");
                return; 
            }
            /// M: added for ConShots @{
            if(mInMotionMode){
                
                if(item.isDisabled()){
                    return;
                }else if( mSelectionManager.getSelectedCount() >= MOTION_MANUAL_EDIT_MAX_PIC
                            && !mSelectionManager.contains(item.getPath())){
                    confirmMsg = mActivity.getResources().getString(R.string.motion_at_most,MOTION_MANUAL_EDIT_MAX_PIC);
                    mAlertDialog = new AlertDialog.Builder(mActivity.getAndroidContext())
                    .setMessage(confirmMsg)
                    .setPositiveButton(R.string.ok, null)
                    .create();
                    mAlertDialog.show();
                    return;
                }else if(mSelectionManager.getSelectedCount() == 0){
                    markItemsDisable(slotIndex);
                }else if(mSelectionManager.getSelectedCount() == 1 && mSelectionManager.contains(item.getPath())){
                    clearItemsDisable();
                }
            }
            /// @}
            mSelectionManager.toggle(item.getPath());
            mDetailsSource.findIndex(slotIndex);
            mSlotView.invalidate();
        } else {
            // Show pressed-up animation for the single-tap.
            mConShotsView.setPressedIndex(slotIndex);
            mConShotsView.setPressedUp();

            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_PICK_PHOTO,
                    slotIndex, 0), FadeTexture.DURATION);
        }
    }

    /**
     * view selected item of continuous shots in ConshotsDetailPage
     * @param slotIndex
     */
    private void pickPhoto(int slotIndex) {
        if (!mIsActive) {
            MtkLog.i(TAG, "not active, ignore the click");
            return;
        }
        MediaItem item;
        try {
            item = mConShotsDataAdapter.get(slotIndex);
        } catch (Exception e) {
            MtkLog.i(TAG, "get item fail!");
            return;
        }

        if (item == null) {
            MtkLog.i(TAG, "Item not ready yet, ignore the click");
            return;
        }
        if (mGetContent) {
            onGetContent(item);
        } else {
            Bundle data = new Bundle();
            data.putInt(PhotoPage.KEY_INDEX_HINT, slotIndex);
            data.putParcelable(PhotoPage.KEY_OPEN_ANIMATION_RECT,
                    mSlotView.getSlotRect(slotIndex, mRootPane));
            data.putString(PhotoPage.KEY_MEDIA_SET_PATH,
                    mMediaSetPath.toString());
            data.putString(PhotoPage.KEY_MEDIA_ITEM_PATH,
                    item.getPath().toString());
            data.putInt(PhotoPage.KEY_ALBUMPAGE_TRANSITION,
                    PhotoPage.MSG_ALBUMPAGE_STARTED);
            data.putBoolean(PhotoPage.KEY_IN_CAMERA_ROLL, mMediaSet.isCameraRoll());
            mActivity.getStateManager().startStateForResult(PhotoPage.class, REQUEST_PHOTO, data);
        }

    }
    
    private void onGetContent(final MediaItem item) {
        DataManager dm = mActivity.getDataManager();
        Activity activity = mActivity;
        if (mData.getString(GalleryActivity.EXTRA_CROP) != null) {
            // M: try handling MTK-specific pick-and-crop flow first
            if (!startMtkCropFlow(item)) {
            Uri uri = dm.getContentUri(item.getPath());
            Intent intent = new Intent(CropActivity.CROP_ACTION, uri)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .putExtras(getData());
            if (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null) {
                intent.putExtra(CropExtras.KEY_RETURN_DATA, true);
            }
            activity.startActivity(intent);
            activity.finish();
            }
        } else {
            activity.setResult(Activity.RESULT_OK,
                    new Intent(null, item.getContentUri()));
            activity.finish();
        }
    }
    
    // M: added for MTK-specific pick-and-crop flow
    private boolean startMtkCropFlow(final MediaItem item) {
        if (!MediatekFeature.MTK_CHANGE_PICK_CROP_FLOW) {
            return false;
        }
        
        mPickedItem = item;
        DataManager dm = mActivity.getDataManager();
        Activity activity = (Activity) mActivity;
        Uri uri = dm.getContentUri(item.getPath());
        // M: for MTK pick-and-crop flow, we do not forward activity result anymore;
        // instead, all crop results will be handled here in onStateResult
        Intent intent = new Intent(CropActivity.CROP_ACTION, uri)
                .putExtras(getData());
        MtkLog.d(TAG, "startMtkCropFlow: EXTRA_OUTPUT=" + mData.getParcelable(MediaStore.EXTRA_OUTPUT));
        boolean cropForWallpaper = Wallpaper.EXTRA_CROP_FOR_WALLPAPER.equals(
                mData.getString(GalleryActivity.EXTRA_CROP));
        boolean shouldReturnData = !cropForWallpaper && 
                (mData.getParcelable(MediaStore.EXTRA_OUTPUT) == null);
        if (shouldReturnData) {
            intent.putExtra(CropExtras.KEY_RETURN_DATA, true);
            MtkLog.i(TAG, "startMtkCropFlow: KEY_RETURN_DATA");
        }
        
        if (cropForWallpaper) {
            activity.startActivityForResult(intent, REQUEST_CROP_WALLPAPER);
            MtkLog.d(TAG, "startMtkCropFlow: start for result: REQUEST_CROP_WALLPAPER");
        } else {
            activity.startActivityForResult(intent, REQUEST_CROP);
            MtkLog.d(TAG, "startMtkCropFlow: start for result: REQUEST_CROP");
        }
        return true;
    }
    
    // M: this holds the item picked in onGetContent
    private MediaItem mPickedItem;
    
    private void handleMtkCropResult(int request, int result, Intent data) {
        MtkLog.d(TAG, "handleMtkCropFlow: request=" + request + ", result=" + result + 
                ", dataString=" + (data != null ? data.getDataString() : "null"));
        switch (request) {
        case REQUEST_CROP:
            /* Fall through */
        case REQUEST_CROP_WALLPAPER:
            if (result == Activity.RESULT_OK) {
                // M: as long as the result is OK, we just setResult and finish
                Activity activity = (Activity) mActivity;
                // M: if data does not contain uri, we add the one we pick;
                // otherwise don't modify data
                if (data != null && mPickedItem != null) {
                    data.setDataAndType(mPickedItem.getContentUri(), data.getType());
                }
                activity.setResult(Activity.RESULT_OK, data);
                activity.finish();
            }
            break;
        default:
            MtkLog.w(TAG, "unknown MTK crop request!!");
        }
    }

    public void onLongTap(int slotIndex) {
        if (mGetContent || mInMotionMode) return;
        MediaItem item = mConShotsDataAdapter.get(slotIndex);
        if (item == null) {
            return;
        }
        mSelectionManager.setAutoLeaveSelectionMode(true);
        mSelectionManager.toggle(item.getPath());
        mDetailsSource.findIndex(slotIndex);
        mSlotView.invalidate();
    }

    @Override
    protected void onCreate(Bundle data, Bundle restoreState) {
        MtkLog.d(TAG, "onCreate");
        mUserDistance = GalleryUtils.meterToPixel(USER_DISTANCE_METER);
        initializeViews();
        initializeData(data);
        mGetContent = data.getBoolean(GalleryActivity.KEY_GET_CONTENT, false);
        
        mDetailsSource = new MyDetailsSource();
        Context context = mActivity.getAndroidContext();
        mVibrator = (Vibrator) context
                .getSystemService(Context.VIBRATOR_SERVICE);

        // Enable auto-select-all for mtp album
        if (data.getBoolean(KEY_AUTO_SELECT_ALL)) {
            mSelectionManager.selectAll();
        }

        // Don't show animation if it is restored
        if (restoreState == null && data != null) {
            int[] center = data.getIntArray(KEY_SET_CENTER);
            if (center != null) {
                mOpenCenter.setAbsolutePosition(center[0], center[1]);
                mSlotView.startScatteringAnimation(mOpenCenter);
            }
        }

        mHandler = new SynchronizedHandler(mActivity.getGLRoot()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                case MSG_PICK_PHOTO: {
                    pickPhoto(message.arg1);
                    break;
                }
                case MSG_UP_PRESS:
                    onUpPressed();
                    break;
                case MSG_INTO_MOTION_PREVIEW:
                    mSelectionManager.leaveSelectionMode();
                    onUpPressed();
                    break;
                default:
                    throw new AssertionError(message.what);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        MtkLog.d(TAG, "onResume");
        mIsActive = true;

        showConShotsIcon();

        mResumeEffect = mActivity.getTransitionStore()
                .get(KEY_RESUME_ANIMATION);
        if (mResumeEffect != null) {
            mConShotsView.setSlotFilter(mResumeEffect);
            mResumeEffect.setPositionProvider(mPositionProvider);
            mResumeEffect.start();
        }

        setContentPane(mRootPane);

        boolean enableHomeButton = (mActivity.getStateManager().getStateCount() > 1)
                | mParentMediaSetString != null;
        mActivity.getGalleryActionBar().setDisplayOptions(enableHomeButton,
                true);

        // Set the reload bit here to prevent it exit this page in
        // clearLoadingBit().
        setLoadingBit(BIT_LOADING_RELOAD);
        mConShotsDataAdapter.resume();

        mConShotsView.resume();
        mActionModeHandler.resume();
        
        if(mInMotionMode){
            motionTrackInit();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsActive = false;
        mConShotsView.setSlotFilter(null);
        mConShotsDataAdapter.pause();
        mConShotsView.pause();
        DetailsHelper.pause();
        mActionModeHandler.pause();
        if(mActionBar != null){
            //mActionBar.setLogo(R.drawable.ic_launcher_gallery);
            mActionBar.setLogo(R.mipmap.ic_launcher_gallery);
        }
        if(mInMotionMode && !mIsInBlending){
            motionTrackRelease();
        }
        if (mSelectionManager != null && mSelectionManager.inSelectionMode()) {
            mSelectionManager.saveSelection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConShotsDataAdapter != null) {
            mConShotsDataAdapter.setLoadingListener(null);
        }
        if(mActionBar != null){
            //mActionBar.setLogo(R.drawable.ic_launcher_gallery);
            mActionBar.setLogo(R.mipmap.ic_launcher_gallery);
        }
        if(mAlertDialog != null) mAlertDialog.dismiss();
        if(mInMotionMode){
            unregisterStorageReceiver();
        }

    }

    private void initializeViews() {
        mSelectionManager = new SelectionManager(mActivity, false);
        mSelectionManager.setSelectionListener(this);
        Config.AlbumPage config = Config.AlbumPage.get(mActivity);
        mSlotView = new SlotView(mActivity, config.slotViewSpec);
        mConShotsView = new AlbumSlotRenderer(mActivity,
                mSlotView, mSelectionManager,0);
        mSlotView.setSlotRenderer(mConShotsView);
        mRootPane.addComponent(mSlotView);
        mSlotView.setListener(new SlotView.SimpleListener() {
            @Override
            public void onDown(int index) {
                ContainerPage.this.onDown(index);
            }

            @Override
            public void onUp(boolean followedByLongPress) {
                ContainerPage.this.onUp(followedByLongPress);
            }

            @Override
            public void onSingleTapUp(int slotIndex) {
                ContainerPage.this.onSingleTapUp(slotIndex);
            }

            @Override
            public void onLongTap(int slotIndex) {
                ContainerPage.this.onLongTap(slotIndex);
            }
        });
        mActionModeHandler = new ActionModeHandler(mActivity, mSelectionManager);
        mActionModeHandler.setActionModeListener(new ActionModeListener() {
            public boolean onActionItemClicked(MenuItem item) {
                return onItemSelected(item);
            }
            public boolean onPopUpItemClicked(int itemId) {
                return true;
            }
        });
        mMenuExecutor = new MenuExecutor(mActivity, mSelectionManager);
    }

    private void initializeData(Bundle data) {
        mMediaSetPath = Path.fromString(data.getString(KEY_MEDIA_PATH));
        mParentMediaSetString = data.getString(KEY_PARENT_MEDIA_PATH);
        mInMotionMode = data.getBoolean(KEY_MOTION_SELECT_ENABLE, false);
        mMediaSet = mActivity.getDataManager().getMediaSet(mMediaSetPath);
        if (mMediaSet == null) {
            Utils.fail("MediaSet is null. Path = %s", mMediaSetPath);
        }
        mSelectionManager.setSourceMediaSet(mMediaSet);
        mConShotsDataAdapter = new AlbumDataLoader(mActivity,
                mMediaSet);
        mConShotsDataAdapter.setLoadingListener(new MyLoadingListener());
        mConShotsView.setModel(mConShotsDataAdapter);
        if(mInMotionMode){
            mNeedShowPrevState = true;
            registerStorageReceiver();
        }
        MtkLog.d(TAG, "initializeData, mInMotionMode:"+mInMotionMode);
    }

    private void showDetails() {
        mShowDetails = true;
        if (mDetailsHelper == null) {
            mDetailsHelper = new DetailsHelper(mActivity, mRootPane,
                    mDetailsSource);
            mDetailsHelper.setCloseListener(new CloseListener() {
                public void onClose() {
                    hideDetails();
                }
            });
        }
        mDetailsHelper.show();
    }

    private void hideDetails() {
        mShowDetails = false;
        mDetailsHelper.hide();
        mConShotsView.setHighlightItemPath(null);
        mSlotView.invalidate();
    }

    @Override
    protected boolean onCreateActionBar(Menu menu) {
        Activity activity = (Activity) mActivity;
        mActionBar = mActivity.getGalleryActionBar();
        MenuInflater inflater = activity.getMenuInflater();
        mActionBar.setTitle(null);
        showConShotsIcon();
//        mActionBar.setTitle(R.string.conshots_title);
//        mActionBar.setSubtitle(mMediaSet.getMediaItemCount() + " "+((Activity)mActivity).getString(R.string.shots));
        if (mGetContent) {
            inflater.inflate(R.menu.pickup, menu);
        } else {
            inflater.inflate(R.menu.conshotsdetail, menu);
            ContainerHelper.updateBestShotMenu(mActivity);
        }
        return true;
    }

    @Override
    protected boolean onItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home: {
            onUpPressed();
            return true;
        }
        case R.id.action_select: {
            mSelectionManager.setAutoLeaveSelectionMode(false);
            mSelectionManager.enterSelectionMode();
            return true;
        }
        case R.id.action_details: {
            if (mShowDetails) {
                hideDetails();
            } else {
                showDetails();
            }
            return true;
        }
        case R.id.action_best_shots: {
            confirmMsg = mActivity.getResources().getString(
                    R.string.best_shots_confirm);
            mSelectionManager.setPrepared(getAllNotBestShotInConShots());
            mMenuExecutor.onMenuClicked(item, confirmMsg, null);
            return true;
        }
        case R.id.action_motion_preview:{
            motionTrackBlend();
            return true;
        }
        default:
            return false;
        }
    }

    @Override
    protected void onStateResult(int request, int result, Intent data) {
        switch (request) {
            case REQUEST_SLIDESHOW: {
                // data could be null, if there is no images in the album
                if (data == null) return;
                mFocusIndex = data.getIntExtra(SlideshowPage.KEY_PHOTO_INDEX, 0);
                mSlotView.setCenterIndex(mFocusIndex);
                break;
            }
            case REQUEST_PHOTO: {
                if (data == null) return;
                mFocusIndex = data.getIntExtra(PhotoPage.KEY_RETURN_INDEX_HINT, 0);
                mSlotView.makeSlotVisible(mFocusIndex);
                break;
            }
            case REQUEST_DO_ANIMATION: {
                mSlotView.startRisingAnimation();
                break;
            }
            // M: default case is added for MTK-specific pick-and-crop flow
            default:
                handleMtkCropResult(request, result, data);
        }
    }

    public void onSelectionModeChange(int mode) {
        switch (mode) {
        case SelectionManager.ENTER_SELECTION_MODE: {
            if(mInMotionMode){
                mActionModeHandler.startActionModeForMotion();
            }else{
                mActionModeHandler.startActionMode();
                mVibrator.vibrate(100);
            } 
            break;
        }
        case SelectionManager.LEAVE_SELECTION_MODE: {
            mActionModeHandler.finishActionMode();
            mRootPane.invalidate();
            if(mInMotionMode){
                mHandler.sendMessage(mHandler.obtainMessage(MSG_UP_PRESS,0, 0));
            }
            break;
        }
        case SelectionManager.DESELECT_ALL_MODE:
        case SelectionManager.SELECT_ALL_MODE: {
            mActionModeHandler.updateSupportedOperation();
            mRootPane.invalidate();
            break;
        }
        default:
            break;
        }
    }

    public void onSelectionChange(Path path, boolean selected) {
        int count = mSelectionManager.getSelectedCount();
        String format = mActivity.getResources().getQuantityString(
                R.plurals.number_of_items_selected, count);
        mActionModeHandler.setTitle(String.format(format, count));
        mActionModeHandler.updateSupportedOperation(path, selected);
    }

    public void onSelectionRestoreDone() {
        ///M: because SelectionManager.restoreSelection() has been changed to asyn
        // so we would update menu when restore done
        mActionModeHandler.updateSupportedOperation();
        mActionModeHandler.updateSelectionMenu();
    }

    private void setLoadingBit(int loadTaskBit) {
        mLoadingBits |= loadTaskBit;
    }

    private void clearLoadingBit(int loadTaskBit) {
        mLoadingBits &= ~loadTaskBit;
        if (mLoadingBits == 0 && mIsActive) {
            if (mConShotsDataAdapter.size() == 0) {
                Toast.makeText((Context) mActivity, R.string.empty_conshots,
                        Toast.LENGTH_SHORT).show();
                mActivity.getStateManager().finishState(ContainerPage.this);
            }
        }
    }

    private class MyLoadingListener implements LoadingListener {
        @Override
        public void onLoadingStarted() {
            setLoadingBit(BIT_LOADING_RELOAD);
        }

        @Override
        public void onLoadingFinished(boolean loadingFailed) {
            clearLoadingBit(BIT_LOADING_RELOAD);

            boolean inSelectionMode = (mSelectionManager != null && mSelectionManager
                    .inSelectionMode());
            int itemCount = mMediaSet != null ? mMediaSet.getMediaItemCount()
                    : 0;
            mSelectionManager.onSourceContentChanged();
            if (itemCount > 0 && inSelectionMode) {
                mSelectionManager.restoreSelection();
                mActionModeHandler.updateSupportedOperation();
                mActionModeHandler.updateSelectionMenu();
            }
            if(!mInMotionMode){
                ContainerHelper.markBestShotItems(mActivity, mMediaSet);
            } else {
                if (!((MotionSet) mMediaSet).isParentExist()) {
                    mHandler.sendMessage(mHandler.obtainMessage(
                            MSG_INTO_MOTION_PREVIEW, 0, 0));
                }
            }
        }
    }

    private class MyDetailsSource implements DetailsHelper.DetailsSource {
        private int mIndex = -1;

        public int size() {
            return mConShotsDataAdapter.size();
        }

        // If requested index is out of active window, suggest a valid index.
        // If there is no valid index available, return -1.
        // only selected one MediaObject, detail will be meaningful
        public int findIndex(int indexHint) {
            ArrayList<Path> ids = mSelectionManager.getSelected(false);
            if (ids != null && ids.size() == 0) {
                mIndex = -1;
                return mIndex;
            }
            if (mConShotsDataAdapter.isActive(indexHint)) {
                if (ids != null && ids.size() == 1) {
                    int mediaItemSize = mConShotsDataAdapter.size();
                    MediaItem mediaItem;
                    for (int i = 0; i < mediaItemSize; i++) {
                        mediaItem = mConShotsDataAdapter.get(i);
                        if (mediaItem != null && mediaItem.getPath() != null
                                && mediaItem.getPath().equals(ids.get(0))) {
                            mIndex = i;
                            return mIndex;
                        }
                    }
                }
            } else {
                mIndex = mConShotsDataAdapter.getActiveStart();
                if (!mConShotsDataAdapter.isActive(mIndex)) {
                    mIndex = -1;
                }
            }
            return mIndex;
        }

        public MediaDetails getDetails() {
            if(!mSelectionManager.inSelectionMode()) {
                MediaItem item = mConShotsDataAdapter.get(0);
                if(item != null) {
                    MediaDetails details = item.getDetails();
                    return details;
                }
            } else {
                MediaItem item = mConShotsDataAdapter.get(mIndex);
                if (item != null) {
                    mConShotsView.setHighlightItemPath(item.getPath());
                    return item.getDetails();
                }
            }
            return null;
        }

        @Override
        public int setIndex() {
            if (mSelectionManager != null 
                    && mSelectionManager.inSelectionMode()
                    && mSelectionManager.getSelectedCount() == 1) {
                Path path = mSelectionManager.getSelected(false).get(0);
                mIndex = mConShotsDataAdapter.findItem(path);
                return mIndex;
            }
            return 0;
        }
    }
    
    private void showConShotsIcon() {
        if (mActionBar != null) {
            mActionBar.setDisplayUseLogoEnabled(true);
            mActionBar.setLogo(R.drawable.conshots_folder);
        }
    }
    /// M: added for MotionTrack @{
    private void markItemsDisable(int slotIndex) {
        int count = mMediaSet.getMediaItemCount();
        int[] refImage;
        refImage = mMotionTrack.getDisableArray(slotIndex);
        ArrayList<MediaItem> mediaItemList = mMediaSet.getMediaItem(0, count);
        for (int i = 0; i < count; i++) {
            if (refImage[i] == 0) {
                mediaItemList.get(i).setDisable(true);
            }

        }
    }
    
    private void clearItemsDisable() {
        int count = mMediaSet.getMediaItemCount();
        ArrayList<MediaItem> mediaItemList = mMediaSet.getMediaItem(0, count);
        for (int i = 0; i < count; i++) {
            mediaItemList.get(i).setDisable(false);
        }
    }

    private void showPrevState() {
        int[] prevFocus;
        int[] prevDisable;
        int itmeCount = mMediaSet.getTotalMediaItemCount();
        ArrayList<MediaItem> items = mMediaSet.getMediaItem(0, itmeCount);
        
        // show prev focus item
        prevFocus = mMotionTrack.getPrevFocusArray();
        for (int i = 0; i < itmeCount; i++) {
            if (prevFocus[i] == 1) {
                mSelectionManager.toggle(items.get(i).getPath());
            }
        }
        // show prev disable item
        prevDisable = mMotionTrack.getPrevDisableArray();
        for (int i = 0; i < itmeCount; i++) {
            items.get(i).setDisable(false);
            if (prevDisable[i] == 0 && prevFocus[i] != 1) {
                items.get(i).setDisable(true);
            }
        }
    }

    private void motionTrackInit() {
        MtkLog.d(TAG, "motionTrackInit");
        mSelectionManager.setAutoLeaveSelectionMode(false);
        mSelectionManager.enterSelectionMode();
        if (mMediaSet.getTotalMediaItemCount() == 0) {
            mHandler.sendMessage(mHandler.obtainMessage(
                    MSG_INTO_MOTION_PREVIEW, 0, 0));
            return;
        }

        mMotionTrack = new MotionTrack();
        int itemCount = mMediaSet.getTotalMediaItemCount();
        ArrayList<MediaItem> items = mMediaSet.getMediaItem(0, itemCount);
        MediaItem mediaItem = items.get(0);
        String workPath = ((MotionSet) mMediaSet).getWorkPath();
        mMotionTrack.init(workPath, mMediaSet.getName(), mediaItem.getWidth(),
                mediaItem.getHeight(), 20, mediaItem.getWidth(),
                mediaItem.getHeight());
        if (mNeedShowPrevState) {
            mNeedShowPrevState = false;
            showPrevState();
        }
    }

    private void motionTrackRelease() {
        MtkLog.d(TAG, "motionTrackRelease");
        mMotionTrack.release();
    }

    private void motionTrackBlend() {
        if (mSelectionManager.getSelectedCount() < MOTION_MANUAL_EDIT_MIN_PIC) {
            confirmMsg = mActivity.getResources().getString(
                    R.string.motion_at_least, MOTION_MANUAL_EDIT_MIN_PIC + "");
            mAlertDialog = new AlertDialog.Builder(
                    mActivity.getAndroidContext()).setMessage(confirmMsg)
                    .setPositiveButton(R.string.ok, null).create();
            mAlertDialog.show();
            return;
        }
        final DialogFragment genProgressDialog = new RotateProgressFragment(
                R.string.generate_animation);
        genProgressDialog.setCancelable(false);
        genProgressDialog.show(mActivity.getFragmentManager(), "");
        new Thread() {
            public void run() {
                long beginTime = System.currentTimeMillis();
                mIsInBlending = true;
                ArrayList<Path> paths = mSelectionManager.getSelected(false);
                mMotionTrack.loadSelected(paths);
                mMotionTrack.doBlend();
                genProgressDialog.dismissAllowingStateLoss();
                mHandler.sendMessage(mHandler.obtainMessage(
                        MSG_INTO_MOTION_PREVIEW, 0, 0));
                mIsInBlending = false;
                long endTime = System.currentTimeMillis();
                MtkLog.d(TAG, "MM_PROFILE blend time:" + (endTime - beginTime));
            }
        }.start();
    }
    
    // M: for closing/re-opening cache
    private void registerStorageReceiver() {
        MtkLog.d(TAG, ">> registerStorageReceiver");
        // register BroadcastReceiver for SD card mount/unmount broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addDataScheme("file");
        mStorageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
                    MtkLog.d(TAG, "storage is unmount");
                    if (mInMotionMode && mMediaSet.getTotalMediaItemCount() == 0) {
                        mHandler.sendMessage(mHandler.obtainMessage(
                                MSG_INTO_MOTION_PREVIEW, 0, 0));
                    }
                }
            }
        };
        mActivity.registerReceiver(mStorageReceiver, filter);
        MtkLog.d(TAG, "<< registerStorageReceiver: receiver registered");
    }

    private void unregisterStorageReceiver() {
        if (mStorageReceiver != null) {
            mActivity.unregisterReceiver(mStorageReceiver);
        }
    }
    /// @}
}
