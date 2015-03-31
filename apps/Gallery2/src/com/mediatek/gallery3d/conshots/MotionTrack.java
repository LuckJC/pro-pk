/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mediatek.gallery3d.conshots;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import com.android.gallery3d.data.Path;

import android.util.Log;


class MotionTrack {
    private static final String TAG = "Gallery2/MotionTrack";
    
    private int[] selectedIndexes;
    int selectedNum;
    
    static {
        // The runtime will add "lib" on the front and ".o" on the end of
        // the name supplied to loadLibrary.
        System.loadLibrary("jni_motion_track");
    }
    
    MotionTrack(){
        selectedIndexes = new int[8];
        selectedNum = 0;
    }

    private class pathComparator implements Comparator<Path>{

        @Override
        public int compare(Path path1, Path paht2) {
            return path1.toString().compareTo(paht2.toString());
        }
    }
    
    public void loadSelected(ArrayList<Path> paths){
        Collections.sort(paths, new pathComparator());
        selectedNum = paths.size();
        if(selectedNum == 0 || selectedNum > 8) return;
        
        for(int i=0; i<selectedNum; i++){
            String path = paths.get(i).toString();
            int start = path.lastIndexOf("MT");
            String sid = path.substring(start+2, start+4);
            Log.d(TAG, "sid:"+sid);
            try{
                int iid = Integer.parseInt(sid)-1;
                Log.d(TAG, "select id:"+iid);
                selectedIndexes[i] = iid;
            }catch(NumberFormatException NFE){
                Log.d(TAG, "loadSelected format error!");
                selectedNum--;
            }
        }
        
        setManualIndexes(selectedIndexes, selectedNum);
    }
    //motion track JNI api
    static native void init(String workPath, String prefixName,
            int inImgWidth, int inImgHeight, int inImgNum, int outImgWidth, int outImgHeight);
    static native int[] getPrevFocusArray();
    static native int[] getPrevDisableArray();
    static native int[] getDisableArray(int firstIndex);
    static native void setManualIndexes(int[] indexs, int number);
    static native void doBlend();
    static native void release();
}
