package com.mediatek.gallery3d.videowriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import android.util.Log;

public class FileWriter {
    public static String TAG = "Gallery2/FileWriter";

    private static FileChannel mFileChannel;
    private static ByteBuffer mHeaderBuf = ByteBuffer.allocate(2*1024);

    public static void openFile(String path) {
        Log.d(TAG, "openFile: " + path);
        try {
            File file = new File(path.substring(0, path.lastIndexOf("/")));
            if(!file.exists()) {
                file.mkdirs();
            }
            mFileChannel = new RandomAccessFile(path, "rw").getChannel();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "openFile: file not found exception");
        } catch (Exception e) {
            Log.d(TAG, "openFile error");
        }
        mHeaderBuf.clear();
    }

    public static int getCurBufPos() {
        return mHeaderBuf.position();
    }

    public static void setBufferData(int pos, int data) {
        int curPos = mHeaderBuf.position();
        mHeaderBuf.position(pos);
        mHeaderBuf.putInt(data);
        mHeaderBuf.position(curPos);
    }

    public static void setFileData(int pos, int data) {
        mHeaderBuf.putInt(data);
        mHeaderBuf.flip();
        try {
            mFileChannel.write(mHeaderBuf, pos);
        } catch (IOException e) {
            Log.d(TAG, "set file data error");
        }
        mHeaderBuf.clear();
    }

    public static void writeBufToFile() {
        if(mFileChannel == null) {
            Log.d(TAG, "FileChannel is null");
            return;
        }

        Log.d(TAG, "write buf to file,lenght:" + mHeaderBuf.position());
        mHeaderBuf.flip();
        try {
            mFileChannel.write(mHeaderBuf);
        } catch (IOException e) {
            Log.d(TAG, "write buf to file error");
        }
        mHeaderBuf.clear();
    }

    public static void close() {
        Log.d(TAG, "file writer close");
        try {
            mFileChannel.close();
        } catch (IOException e) {
            Log.d(TAG, "file writer close error");
        }
    }

    public static void writeInt8(byte data) {
        mHeaderBuf.put(data);
    }
    public static void writeBytes(byte[] data) {
        mHeaderBuf.put(data);
    }
    public static void writeInt16(short data) {
        mHeaderBuf.putShort(data);
    }
    public static void writeInt32(int data) {
        mHeaderBuf.putInt(data);
    }
    public static void writeString(String str, int len) {
        if(str.length() != len) {
            throw new AssertionError();
        }
        mHeaderBuf.put(str.getBytes());
    }

    public static void writeBitStreamToFile(byte[] outData, int length) {
        if(mFileChannel == null) {
            Log.d(TAG, "FileChannel is null");
            return;
        }
        if(outData.length != length) {
            throw new AssertionError();
        }

        Log.d(TAG, "writeBitStream,length:" + outData.length);
        try {
            mFileChannel.write(ByteBuffer.wrap(outData));
        } catch (IOException e) {
            Log.d(TAG, "write bit stream error");
        }
    }
}
