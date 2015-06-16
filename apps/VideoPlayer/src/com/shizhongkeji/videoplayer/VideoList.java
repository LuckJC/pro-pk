package com.shizhongkeji.videoplayer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

import com.shizhongkeji.videoplayer.VideoChooseActivity.PublicTools;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.util.Log;

/**
 * Class Name: VideoList.java<br>
 * Function:��Ƶ�б�ķ�װ��������һ�� <code>mCache</code> ���԰��������е���Ƶ��Ϣ<br>
 * <strong>�÷�������Context�����£� </strong><br>
 * Context ctx = this;<br>
 * ContentResolver cr = this.getContentResolver();<br>
 * Uri sVideoStorageURI = Uri.parse("content://media/external/video/media");<br>
 * int sort = 0;//sort��������ֵ<br>
 * VideoList mVideoList = VideoList(ctx, cr, sVideoStorageURI, sort);<br>
 * int count = mVideoList.getCount();//��ȡ��Ƶ����<br>
 * VideoObject mVideoObject = VideoObject mVideoObject
 * mVideoList.getVideoAt(i);//���һ��VideoObject����(0<=i<count)<br>
 * ��������mVideoObject���������Ի����Ƶ�����֣��洢λ�ã�Ԥ��ͼ��ʱ�䣬��С����Ϣ{@link VideoObject}<br>
 * 
 * Modifications:<br>
 * 
 * @author ZYT DateTime 2014-5-15 ����10:35:14<br>
 * @version 1.0<br>
 * <br>
 */
public class VideoList {
	private static final String TAG = "VideoList";
	private final String[] sProjection = new String[] { Video.Media._ID,
			Video.Media.DATA, Video.Media.DATE_MODIFIED, Video.Media.BUCKET_ID,
			Video.Media.TITLE, Video.Media.MINI_THUMB_MAGIC,
			Video.Media.MIME_TYPE, Video.Media.DURATION, Video.Media.SIZE,
			Video.Media.DATE_ADDED, };
	final public int INDEX_ID = indexOf(sProjection, Video.Media._ID);
	final public int INDEX_DATA = indexOf(sProjection, Video.Media.DATA);
	final public int INDEX_DATE_MODIFIED = indexOf(sProjection,
			Video.Media.DATE_MODIFIED);
	final public int INDEX_BUCKET_ID = indexOf(sProjection,
			Video.Media.BUCKET_ID);
	final public int INDEX_TITLE = indexOf(sProjection, Video.Media.TITLE);
	final public int INDEX_MINI_THUMB_MAGIC = indexOf(sProjection,
			Video.Media.MINI_THUMB_MAGIC);
	final public int INDEX_MIME_TYPE = indexOf(sProjection,
			Video.Media.MIME_TYPE);
	final public int INDEX_DURATION = indexOf(sProjection, Video.Media.DURATION);
	final public int INDEX_SIZE = indexOf(sProjection, Video.Media.SIZE);
	final public int INDEX_DATE_ADDED = indexOf(sProjection,
			Video.Media.DATE_ADDED);

	private Context mContext;
	private ContentResolver mContentResolver;
	private Uri mStroageUri;
	private int mSort;
	private Cursor mCursor;
	protected HashMap<Long, VideoObject> mCache = new HashMap<Long, VideoObject>();
	private RandomAccessFile mMiniThumbData;

	/**
	 * public functions
	 * 
	 * @param ctx
	 *            VideoPlayerActivity
	 * @param cr
	 *            getContentResolver()
	 * @param uri
	 *            use: Uri.parse("content://media/external/video/media")
	 * @param sort
	 *            {@linkplain VideoManager#SORT_ASCENDING}��
	 *            {@linkplain VideoManager#SORT_DESCENDING} <br>
	 */
	public VideoList(Context ctx, ContentResolver cr, Uri uri, int sort) {
		mContext = ctx;
		mContentResolver = cr;
		mStroageUri = uri;
		mSort = sort;

		mCursor = createCursor();
		if (mCursor == null) {
			Log.e("Exception", "unable to create video cursor for "
					+ mStroageUri);
			throw new UnsupportedOperationException();
		}

		if (mCursor != null && mCursor.moveToFirst()) {
			int row = 0;
			do {
				long imageId = mCursor.getLong(INDEX_ID);
				mCache.put(imageId, new VideoObject(imageId, mContentResolver,
						this, row++));
			} while (mCursor.moveToNext());
		}
	} // end VideoList

	private Cursor createCursor() {
		Cursor c = Images.Media.query(mContentResolver, mStroageUri,
				sProjection, null, null, sortOrder());
		return c;
	}

	public Cursor getCursor() {
		if (mCursor == null || mCursor.isClosed()) {
			mCursor = createCursor();
		}
		synchronized (mCursor) {
			return mCursor;
		}
	}

	private void requery() {
		if (mCursor == null || mCursor.isClosed()) {
			mCursor = createCursor();
		}
		mCursor.requery();
	}

	private void refreshCache() {
		Cursor c = getCursor();

		synchronized (c) {
			try {
				int i = 0;
				c.moveToFirst();
				while (!c.isAfterLast()) {
					long idFromCursor = c.getLong(INDEX_ID);

					if (mCache.get(idFromCursor) != null) {
						mCache.get(idFromCursor).mCursorRow = i++;
					}

					c.moveToNext();
				}
			} catch (Exception ex) {
			}
		}
	}

	/**
	 * Function:���VideoList������<br>
	 * 
	 * @author ZYT DateTime 2014-5-16 ����11:29:14<br>
	 * @return<br>
	 */
	public int getCount() {
		Cursor c = getCursor();
		synchronized (c) {
			try {
				return c.getCount();
			} catch (Exception ex) {
			}
			return 0;
		}
	}

	/**
	 * Function:���ָ��λ�õ�VideoObject����<br>
	 * 
	 * @author ZYT DateTime 2014-5-16 ����11:29:32<br>
	 * @param i
	 * @return<br>
	 */
	public VideoObject getVideoAt(int i) {
		Cursor c = getCursor();
		synchronized (c) {
			boolean moved;

			try {
				moved = c.moveToPosition(i);
			} catch (Exception ex) {
				return null;
			}

			if (moved) {
				try {
					long id = c.getLong(INDEX_ID);

					VideoObject img = mCache.get(id);
					if (img == null) {
						img = new VideoObject(id, mContentResolver, this, i);
						mCache.put(id, img);
					}
					return img;
				} catch (Exception ex) {
					Log.e(TAG,
							"got this exception trying to create image object: "
									+ ex);
					return null;
				}
			} else {
				Log.e(TAG,
						"unable to moveTo to " + i + "; count is "
								+ c.getCount());
				return null;
			}
		}
	}

	protected Uri contentUri(long id) {
		try {
			// does our uri already have an id (single image query)?
			// if so just return it
			long existingId = ContentUris.parseId(mStroageUri);
			if (existingId != id) {
				Log.e(TAG, "id mismatch");
			}
			return mStroageUri;
		} catch (NumberFormatException ex) {
			// otherwise tack on the id
			return ContentUris.withAppendedId(mStroageUri, id);
		}
	}

	/**
	 * Function:�û�ɾ����Ƶ<br>
	 * 
	 * @author ZYT DateTime 2014-5-16 ����4:39:04<br>
	 * @param video
	 * @return<br>
	 */
	public boolean removeVideo(VideoObject video) {
		if (null == video)
			return false;

		if (isIdExist(video.mId)) {
			if (PublicTools.isFileExist(video.getMediapath())) {
				File file = new File(video.getMediapath());
				if (!file.delete()) {
					Log.i(TAG, "delete file failure");
					return false;
				}
			}

			Uri u = video.fullSizeImageUri();
			mContentResolver.delete(u, null, null);
			video.onRemove();
			requery();
			refreshCache();
			return true;
		} else {
			return false;
		}
	}

	public boolean renameImage(Context context, VideoObject image, String name) {
		Log.i(TAG, "call renameImage");

		if (!isIdExist(image.mId)) {
			Log.i(TAG, "renameImage id not exist");
			return false;
		}

		String oldpath = image.getMediapath();
		String newPath = PublicTools.replaceFilename(oldpath, name);

		if (name != null && name.length() > 0) {
			// rename file first
			Log.v(TAG, "renameImage => from oldpath : " + oldpath
					+ " to newpath : " + newPath);
			File oldfile = new File(oldpath);
			File newFile = new File(newPath);
			if (!oldfile.renameTo(newFile)) {
				Log.i(TAG, "rename file failure : oldfile.renameTo(newFile)");
				return false;
			}
			String id = String.valueOf(image.getMediaId()).toString();
			ContentValues values = new ContentValues();
			values.put(MediaStore.Video.Media.TITLE, name);
			values.put(MediaStore.Video.Media.DATA, newPath);
			String fileName = newPath.substring(newPath.lastIndexOf("/") + 1,
					newPath.length());
			values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
			mContentResolver.update(
					MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values,
					MediaStore.Video.Media._ID + "=?", new String[] { Integer
							.valueOf(image.getMediaId()).toString() });

			// context.sendBroadcast(new
			// Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
			// + newPath)));

			requery();

			return true;
		} else
			return false;
	}

	public boolean isFilenameExist(VideoObject image, String name) {
		String newPath = PublicTools
				.replaceFilename(image.getMediapath(), name);
		boolean exist = false;

		Cursor c = getCursor();
		synchronized (c) {
			try {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					String oldpath = c.getString(INDEX_DATA);
					// if (newPath.equals(oldpath)) {
					if (PublicTools.getBucketId(newPath) == PublicTools
							.getBucketId(oldpath)) {
						exist = true;
						break;
					}
					c.moveToNext();
				}
			} catch (Exception ex) {
			}
		}

		return exist;
	}

	// help functions
	private boolean isIdExist(long id) {
		boolean exist = false;
		Cursor c = getCursor();
		synchronized (c) {
			try {
				c.moveToFirst();
				while (!c.isAfterLast()) {
					long idFromCursor = c.getLong(INDEX_ID);

					if (idFromCursor == id) {
						exist = true;
						break;
					}
					c.moveToNext();
				}
			} catch (Exception ex) {
			}
		}

		return exist;
	}

	private static int indexOf(String[] array, String s) {
		for (int i = 0; i < array.length; i++) {
			if (array[i].equals(s)) {
				return i;
			}
		}
		return -1;
	}

	private String sortOrder() {
		// return Video.Media._ID + (mSort == ImageManager.SORT_ASCENDING ?
		// " ASC " : " DESC");
		return Video.Media.DATE_ADDED + " DESC ";
	}

	public void onDestory() {
		try {
			if (mMiniThumbData != null)
				mMiniThumbData.close();
			/** hejn, for bug 72373/72398 B ****/
			if (null != this.mCursor) {
				this.mCursor.close();
				this.mCursor = null;
			}
			/** hejn, for bug 72373/72398 S ****/
		} catch (IOException ex) {
		}
	}
}