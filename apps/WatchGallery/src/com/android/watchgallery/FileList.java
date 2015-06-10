package com.android.watchgallery;
import java.util.*;
import java.io.*;

import android.util.Log;
public class FileList {
	public static List<String> lsmap=new ArrayList<String>();
	public static List<String> findFile(String fpath){
		File fl=new File(fpath);
		File[] fls=fl.listFiles();
		if(fls!=null){
			for (int i = 0; i < fls.length; i++) {
				if(fls[i].isDirectory()){
					findFile(fls[i].getAbsolutePath());
				}else{
					Map<String,Object> map=new HashMap<String, Object>();
					String fname=fls[i].getAbsolutePath();
					Log.v("fpath-->",fname);
					//String ext=fname.substring(fname.lastIndexOf("."),fname.length());
					//Log.v("ext===>", ext);
					
					if (fls[i].getName().endsWith(".jpg")||fls[i].getName().endsWith(".png")||fls[i].getName().endsWith(".icon")){
						lsmap.add(fls[i].getAbsolutePath());
					}
					
//					if(ext.equalsIgnoreCase(".jpg")||ext.equalsIgnoreCase(".png")||ext.equalsIgnoreCase(".icon")){
//						map.put("fname",fname);
//						lsmap.add(fname);
//					}
				}
			}
		}
		return lsmap;
	}
}
