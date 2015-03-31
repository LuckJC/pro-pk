package com.mediatek.gallery3d.conshots;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;

import android.graphics.Bitmap;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.Log;
import com.android.gallery3d.util.ThreadPool.Job;
import com.android.gallery3d.util.ThreadPool.JobContext;
import com.mediatek.gallery3d.util.MtkLog;

public class BestShotSelector{
    private static final String TAG = "Gallery2/BestShotSelector";
    private static final int HIGH_START = 205;
    private static final int HIGH_END = 255;
    private static final int LOW_START = 0;
    private static final int LOW_END = 50;
    private static final float CLEAR_THRESOLD = 0.2f;
    private static final int MSG_BEST_SHOT_DONE = 1;
    private static final int WAIT_FIRST_CREST = 1;
    private static final int WAIT_FIRST_TROUGH = 2;
    private static final int WAIT_SECOND_CREST = 3;
    private static final int WAIT_SECOND_TROUGH = 4;
    
    private ArrayList<Entry> mEntryArray = null;
    private ArrayList<MediaItem> mMediaItemList = null;
    private int mBestShotNum = 0;
    private int mWaveCrestCount = 0;
    private int mWaveTroughCount = 0;

    public BestShotSelector(ArrayList<MediaItem> mediaItemArray) {
        mMediaItemList = mediaItemArray;
        mEntryArray = new ArrayList<Entry>(mMediaItemList.size());
        for (int i = 0; i <  mediaItemArray.size(); i++) {
            MediaItem item = mediaItemArray.get(i);
            MtkLog.i(TAG, "<BestShotSelector> index =" + i + ", focusValue = "
                    + item.getFocusValue() + ", name = " + item.getName());
            mEntryArray.add(new Entry(item, i));
        }
        
        /// M: if focus val is illegal, could replace with file size
        if(!isFocusValLegal()){
            replaceFocuseValByFileSize();
        }
        mBestShotNum = mEntryArray.size() / 10;
        mBestShotNum = mBestShotNum > 1 ? mBestShotNum : 1;
        MtkLog.i(TAG, "<BestShotSelector.init> mBestShotNum = " + mBestShotNum);
    }
    
    public void markBestShot() {
        markWaveCrest();
        markWaveTrough();
        mergeWaveCrest();
        
        for (Entry entry : mEntryArray) {
            entry.mItem.setIsBestShot(entry.mWave == Entry.WAVE_CREST ? MediaItem.IMAGE_BEST_SHOT_MARK_TRUE
                    : MediaItem.IMAGE_BEST_SHOT_MARK_FALSE);
        }
    }

    private void markWaveCrest() {
        Entry entry = null;
        Entry nextEntry = null;
        Entry prevEntry = null;

        if (mEntryArray.get(0).mFocusValue.compareTo(mEntryArray.get(1).mFocusValue) == 1) {
            mEntryArray.get(0).mWave = Entry.WAVE_CREST;
            Log.i(TAG, "<markWaveCrest> mark 0 as crest");
            mWaveCrestCount++;
        }
        if (mEntryArray.get(mEntryArray.size() - 1).mFocusValue.compareTo(mEntryArray
                .get(mEntryArray.size() - 2).mFocusValue) == 1) {
            mEntryArray.get(mEntryArray.size() - 1).mWave = Entry.WAVE_CREST;
            Log.i(TAG, "<markWaveCrest> mark " + (mEntryArray.size() - 1) +" as crest");
            mWaveCrestCount++;
        }
        for (int i = 1; i < mEntryArray.size() - 1; i++) {
            entry = mEntryArray.get(i);
            nextEntry = mEntryArray.get(i + 1);
            prevEntry = mEntryArray.get(i - 1);
            if (entry.mFocusValue.compareTo(nextEntry.mFocusValue) == 1
                    && entry.mFocusValue.compareTo(prevEntry.mFocusValue) == 1) {
                entry.mWave = Entry.WAVE_CREST;
                Log.i(TAG, "<markWaveCrest> mark " + i +" as crest");
                mWaveCrestCount++;
            }
        }
    }

    private void markWaveTrough() {
        Entry trough = null;
        for (int i = 0; i < mEntryArray.size() - 1; i++) {
            if (mEntryArray.get(i).mWave == Entry.WAVE_CREST) {
                if(trough != null) {
                    trough.mWave = Entry.WAVE_TROUGH;
                    Log.i(TAG, "<markWaveCrest> mark " + (trough.mIndex) +" as through");
                    trough = null;
                    mWaveTroughCount++;
                }
            } else if (trough == null
                    || trough.mFocusValue.compareTo(mEntryArray.get(i).mFocusValue) >= 0) {
                trough = mEntryArray.get(i);
            }
        }
        if (trough != null) {
            trough.mWave = Entry.WAVE_TROUGH;
            Log.i(TAG, "<markWaveCrest> mark " + (trough.mIndex) +" as through");
            mWaveTroughCount++;
        }
    }
    
    private void mergeWaveCrest() {
        ArrayList<BigInteger> fvList1 = null;
        ArrayList<BigInteger> fvList2 = null;
        ArrayList<BigInteger> fvList = null;

        BigInteger average1 = BigInteger.ZERO;
        BigInteger average2 = BigInteger.ZERO;
        BigInteger average = BigInteger.ZERO;

        int state = WAIT_FIRST_CREST;
        int firstCrest = -1;
        int secondCrest = -1;
        int fvlist1Start = 0;
        int fvlist1End = 0;
        int fvlist2Start = 0;
        int fvlist2End = 0;
        int fvlistStart = 0;
        int fvlistEnd = 0;
        
        for (Entry entry : mEntryArray) {
            Log.i(TAG, "<markBestShot> current index = " + entry.mIndex);
            Utils.assertTrue(!(entry.mWave == Entry.WAVE_CREST && state == WAIT_FIRST_TROUGH));
            Utils.assertTrue(!(entry.mWave == Entry.WAVE_CREST && state == WAIT_SECOND_TROUGH));
            Utils.assertTrue(!(entry.mWave == Entry.WAVE_TROUGH && state == WAIT_SECOND_CREST));
            if (fvList == null) {
                fvList = new ArrayList<BigInteger>();
            }
            if (fvList1 == null) {
                fvList1 = new ArrayList<BigInteger>();
            }
            if (fvList2 == null) {
                fvList2 = new ArrayList<BigInteger>();
            }
            if (entry.mWave == Entry.WAVE_CREST) {
                if (state == WAIT_FIRST_CREST) {
                    firstCrest = entry.mIndex;
                    fvList1.add(entry.mFocusValue);
                    if(fvList1.size() == 1) {
                        fvlist1Start = entry.mIndex;
                    } else {
                        fvlist1End = entry.mIndex;
                    }
                } else {
                    secondCrest = entry.mIndex;
                    fvList2.add(entry.mFocusValue);
                    if(fvList2.size() == 1) {
                        fvlist2Start = entry.mIndex;
                    } else {
                        fvlist2End = entry.mIndex;
                    }
                }
                fvList.add(entry.mFocusValue);
                if(fvList.size() == 1) {
                    fvlistStart = entry.mIndex;
                } else {
                    fvlistEnd = entry.mIndex;
                }
                state++;
            } else if (entry.mWave == Entry.WAVE_TROUGH) {
                fvList.add(entry.mFocusValue);
                if(fvList.size() == 1) {
                    fvlistStart = entry.mIndex;
                } else {
                    fvlistEnd = entry.mIndex;
                }
                if (state == WAIT_FIRST_CREST) {
                    fvList1.add(entry.mFocusValue);
                    fvList.add(entry.mFocusValue);
                    if(fvList.size() == 1) {
                        fvlistStart = entry.mIndex;
                    } else {
                        fvlistEnd = entry.mIndex;
                    }
                    if(fvList1.size() == 1) {
                        fvlist1Start = entry.mIndex;
                    } else {
                        fvlist1End = entry.mIndex;
                    }
                } else if (state == WAIT_FIRST_TROUGH) {
                    fvList1.add(entry.mFocusValue);
                    if(fvList1.size() == 1) {
                        fvlist1Start = entry.mIndex;
                    } else {
                        fvlist1End = entry.mIndex;
                    }
                    average1 = getVariance(fvList1);
                    state++;
                } else {
                    fvList2.add(entry.mFocusValue);
                    if(fvList2.size() == 1) {
                        fvlist2Start = entry.mIndex;
                    } else {
                        fvlist2End = entry.mIndex;
                    }
                    average2 = getVariance(fvList2);
                    average = getVariance(fvList);
                    Log.i(TAG, "<mergeWaveCrest> index = " + firstCrest + ",start = " + fvlist1Start
                            + ", end = " + fvlist1End + ", average = " + average1);
                    Log.i(TAG, "<mergeWaveCrest> index = " + secondCrest + ",start = " + fvlist2Start
                            + ", end = " + fvlist2End + ", average = " + average2);
                    Log.i(TAG, "<mergeWaveCrest> index above, start = " + fvlistStart
                            + ", end = " + fvlistEnd + ", average = " + average);
                    if (average.compareTo(average1) < 0 || average.compareTo(average2) < 0) {
                        if (mEntryArray.get(firstCrest).mFocusValue.compareTo(mEntryArray
                                .get(secondCrest).mFocusValue) < 0) {
                            Log.i(TAG, "<mergeWaveCrest> set index = " + firstCrest + " as WAVE_NORMAL");
                            mEntryArray.get(firstCrest).mWave = Entry.WAVE_NORMAL;
                        } else {
                            Log.i(TAG, "<mergeWaveCrest> set index = " + secondCrest + " as WAVE_NORMAL");
                            mEntryArray.get(secondCrest).mWave = Entry.WAVE_NORMAL;
                        }
                        fvList1 = null;
                        fvList2 = null;
                        fvList = null;
                        average1 = BigInteger.ZERO;
                        average2 = BigInteger.ZERO;
                        average = BigInteger.ZERO;
                        state = WAIT_FIRST_CREST;
                        firstCrest = -1;
                        secondCrest = -1;
                        fvlist1Start = 0;
                        fvlist1End = 0;
                        fvlist2Start = 0;
                        fvlist2End = 0;
                        fvlistStart = 0;
                        fvlistEnd = 0;
                    } else {
                        fvList1 = fvList2;
                        fvList2 = null;
                        fvList = new ArrayList<BigInteger>(fvList1);
                        average1 = average2;
                        average2 = BigInteger.ZERO;
                        average = BigInteger.ZERO;
                        state = WAIT_SECOND_CREST;
                        firstCrest = secondCrest;
                        secondCrest = -1;
                        fvlist1Start =fvlist2Start;
                        fvlist1End = fvlist2End;
                        fvlist2Start = 0;
                        fvlist2End = 0;
                        fvlistStart = fvlist1Start;
                        fvlistEnd = 0;
                    }
                }
            } else {
                if (state == WAIT_FIRST_CREST || state == WAIT_FIRST_TROUGH) {
                    fvList1.add(entry.mFocusValue);
                    if(fvList1.size() == 1) {
                        fvlist1Start = entry.mIndex;
                    } else {
                        fvlist1End = entry.mIndex;
                    }
                } else if (state == WAIT_SECOND_CREST || state == WAIT_SECOND_TROUGH) {
                    fvList2.add(entry.mFocusValue);
                    if(fvList2.size() == 1) {
                        fvlist2Start = entry.mIndex;
                    } else {
                        fvlist2End = entry.mIndex;
                    }
                }
                fvList.add(entry.mFocusValue);
                if(fvList.size() == 1) {
                    fvlistStart = entry.mIndex;
                } else {
                    fvlistEnd = entry.mIndex;
                }
            }
        }
        mWaveCrestCount = 0;
        for (Entry entry : mEntryArray) {
            if (entry.mWave != Entry.WAVE_CREST) {
                entry.mWave = Entry.WAVE_NORMAL;
            } else {
                mWaveCrestCount++;
            }
        }
        markWaveTrough();
    }

    private boolean isFocusValLegal(){
        if(mEntryArray == null || mEntryArray.size()==0) return false;
        BigInteger prevFocuseVal = mEntryArray.get(0).mFocusValue;
        for (int i = 0; i <  mEntryArray.size(); i++) {
            Entry entry = mEntryArray.get(i);
            if(!prevFocuseVal.equals(entry.mFocusValue)) return true;
        }
        return false;
    }
    
    private void replaceFocuseValByFileSize()
    {
        Log.d(TAG, "replayFocuseValByFileSize");
        if(mEntryArray == null || mEntryArray.size()==0) return;

        for (int i = 0; i <  mEntryArray.size(); i++) {
            Entry entry = mEntryArray.get(i);
            entry.mFocusValue = entry.mFileSize;
        }
    }
    
    private class Entry {
        public static final int WAVE_NORMAL = 0;
        public static final int WAVE_CREST = 1;
        public static final int WAVE_TROUGH = 2;
        
        public MediaItem mItem;
        public int mIndex;
        public BigInteger mFocusValue;
        public BigInteger mFileSize;
        public boolean mBestShot = false;
        public int mWave = WAVE_NORMAL;
        public float mGrayPercent;

        public Entry(MediaItem item, int index) {
            Utils.assertTrue(item != null && index >= 0);
            mItem = item;
            mIndex = index;
            mFocusValue = mItem.getFocusValue();
            mFileSize = BigInteger.valueOf(mItem.getSize());
            mWave = WAVE_NORMAL;
        }
    }

    private class SortByFocusValue implements Comparator<Entry> {
        public int compare(Entry entry1, Entry entry2) {
            if (entry1.mWave == Entry.WAVE_CREST && entry2.mWave != Entry.WAVE_CREST) {
                return -1;
            } else if (entry1.mWave != Entry.WAVE_CREST && entry2.mWave == Entry.WAVE_CREST) {
                return 1;
            } else if (entry1.mWave == Entry.WAVE_CREST && entry2.mWave == Entry.WAVE_CREST) {
                return entry2.mFocusValue.subtract(entry1.mFocusValue).intValue();
            } else {
                return -1;
            }
        }
    }
      
    private class SortByGrayPercent implements Comparator<Entry> {
        public int compare(Entry entry1, Entry entry2) {
            BigInteger gray1 = BigInteger.valueOf((long)(entry1.mGrayPercent*1000)).multiply(entry1.mFocusValue);
            BigInteger gray2 = BigInteger.valueOf((long)(entry2.mGrayPercent*1000)).multiply(entry2.mFocusValue);
            return gray2.compareTo(gray1);
        }
    }
    
    public float getGrayPercent(Bitmap bitmap, int lowstart, int lowend, int highstart, int highend) {
        if (bitmap == null) {
            Log.i(TAG, "<getGrayPercent> bitmap = null, return 0.0");
            return 0.0f;
        }
        int[] pixelsData = new int[bitmap.getWidth() * bitmap.getHeight()];
        int[] dumpBitmap = new int[bitmap.getWidth() * bitmap.getHeight()];
        int temp = 0;
        int grayCount = 0;
        int gray = 0;
        int r = 0;
        int g = 0;
        int b = 0;
        Bitmap.Config config = bitmap.getConfig();
        Utils.assertTrue(config == Bitmap.Config.ARGB_8888);
        bitmap.getPixels(pixelsData, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap
                .getHeight());

        for (int i = 0; i < pixelsData.length; i++) {
            r = (pixelsData[i] >> 16) & 0xff;
            g = (pixelsData[i] >> 8) & 0xff;
            b = pixelsData[i] & 0xff;
            gray = Math.round(r * 0.3f + g * 0.59f + b * 0.11f);
            if ((gray >= lowstart && gray <= lowend) || (gray >= highstart && gray <= highend)) {
                grayCount++;
            }
        }
        return (float) grayCount / (float) bitmap.getWidth() / (float) bitmap.getHeight();
    }
    
    public BigInteger getVariance(ArrayList<BigInteger> list) {
        BigInteger average = getAverage(list);
        BigInteger sum = BigInteger.ZERO;
        for(BigInteger i: list) {
            sum = sum.add(i.subtract(average).pow(2));
        }
        sum = sum.divide(BigInteger.valueOf(list.size()));
        return sum;
    }
    
    public BigInteger getAverage(ArrayList<BigInteger> list) {
        BigInteger sum = BigInteger.ZERO;
        for(BigInteger i: list) {
            sum = sum.add(i);
        }
        return sum.divide(BigInteger.valueOf(list.size()));
    }
}
