package com.mediatek.gallery3d.pq;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import com.android.gallery3d.data.MediaItem;
import com.mediatek.gallery3d.util.MediatekFeature;
import com.mediatek.gallery3d.util.MtkLog;
import com.mediatek.gallery3d.util.MediatekFeature.Params;
import com.android.gallery3d.common.BitmapUtils;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;

public class ImageDecoder {
    private static String TAG ="Gallery2/ImageDecoder";
    int mScreenWidth;
    int mScreenHeight;
    int mOriginalImageWidth;
    int mOriginalImageHeight;
    int mGLviewWidth;
    int mGLviewHeight;
    public String mUri = null;
    int targetSize ;
    Context mContext;
    BitmapFactory.Options options = null;
    Bitmap mScreenNail = null;
    Runnable mApply = null;
    int mLevelCount;
    public ImageDecoder(Context context, String mPqUri, int width, int height, int targetSize , int viewWidth, int viewHeight,int levelCount) {
        // TODO Auto-generated constructor stub
        mScreenWidth = width;
        mScreenHeight = height;
        mGLviewWidth = viewWidth;
        mGLviewHeight = viewHeight;
        mLevelCount = levelCount;
        this.mUri = mPqUri;
        this.targetSize = targetSize;
        mContext = context;
        options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        if (MediatekFeature.isPictureQualityEnhanceSupported()) {
            options.inPostProc = true;
        }
    }

    public Bitmap apply() {
        Bitmap bitmap = decoder();
        if (bitmap != null) {
            mScreenNail = bitmap;
        } else {
            Log.d(TAG, "apply bitmap == null");
        }
        return mScreenNail;
    }

    public Bitmap decoder() {
        FileDescriptor fd = null;
        FileInputStream fis = null;
        Bitmap mBitmap = null;
        try {
            fis = getFileInputStream(mUri);
            if (fis != null) {
                fd = fis.getFD();
                mBitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
            }
        } catch (FileNotFoundException e) {
            MtkLog.e(TAG, "bitmapfactory decodestream fail");
        } catch (IOException e) {
            MtkLog.e(TAG, "bitmapfactory decodestream fail");
        } catch (Exception e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (mBitmap != null) {
                float scale = (float) targetSize / Math.max(mBitmap.getWidth(), mBitmap.getHeight());
                if (scale <= 0.5) {
                    return BitmapUtils.resizeBitmapByScale(mBitmap, scale, true);
                }
            }
               // return mBitmap;
        }
        return mBitmap;
    }

    public Bitmap decodeImage() {
        caculateInSampleSize();
        options.inJustDecodeBounds = false;
        mScreenNail = decoder();
        return mScreenNail;
    }

    public int caculateInSampleSize() {
        FileDescriptor fd = null;
        FileInputStream fis = null;
        options.inJustDecodeBounds = true;
        try {
            fis = getFileInputStream(mUri);
            if (fis != null) {
                fd = fis.getFD();
                BitmapFactory.decodeFileDescriptor(fd, null, options);
            }
            }catch (FileNotFoundException e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
            }catch (IOException e) {
                MtkLog.e(TAG, "bitmapfactory decodestream fail");
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            float scale = 1;
            if (options.outWidth > 0 && options.outHeight > 0) {
                mOriginalImageWidth = options.outWidth;
                mOriginalImageHeight = options.outHeight;
                scale = (float) targetSize / Math.max(options.outWidth, options.outHeight);
            }
        options.inSampleSize = BitmapUtils.computeSampleSizeLarger(scale);
        MtkLog.d(TAG, " pq  options.inSampleSize=="+options.inSampleSize +" width=="+options.outWidth+ " height=="+options.outHeight + "targetSize=="+targetSize);
        return options.inSampleSize;
    }

    public void setApply (Runnable apply) {
        mApply = apply;
    }

    public void recycle() {
        // TODO Auto-generated method stub
        if (mScreenNail != null) {
            mScreenNail.recycle();
            mScreenNail = null;
        }
    }

    public FileInputStream getFileInputStream(String uri) {
        FileInputStream fis = null;
        //FileDescriptor fd = null;
        try {
            final String[] fullPath = new String[1];
            MtkLog.d(TAG, "Uri.parse(mUri)=="+mUri);
            querySource(mContext,
                    Uri.parse(uri), new String[] { ImageColumns.DATA },
                    new ContentResolverQueryCallback() {
                        @Override
                        public void onCursorResult(Cursor cursor) {
                            fullPath[0] = cursor.getString(0);
                        }
                    }
            );
            MtkLog.d(TAG, "fullPath[0]=="+fullPath[0]);
            fis = new FileInputStream(fullPath[0]);
            //fd = fis.getFD();
        } catch (FileNotFoundException e) {
            MtkLog.e(TAG, "bitmapfactory decodestream fail");
        } catch (IOException e) {
            MtkLog.e(TAG, "bitmapfactory decodestream fail");
        } finally {
/*            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }*/
        }
        return fis;
    }
    
    public static void querySource(Context context, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        ContentResolver contentResolver = context.getContentResolver();
        querySourceFromContentResolver(contentResolver, sourceUri, projection, callback);
    }

    private static void querySourceFromContentResolver(
            ContentResolver contentResolver, Uri sourceUri, String[] projection,
            ContentResolverQueryCallback callback) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(sourceUri, projection, null, null,
                    null);
            if ((cursor != null) && cursor.moveToNext()) {
                callback.onCursorResult(cursor);
            }
        } catch (Exception e) {
            // Ignore error for lacking the data column from the source.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public interface ContentResolverQueryCallback {
        void onCursorResult(Cursor cursor);
    }
}
