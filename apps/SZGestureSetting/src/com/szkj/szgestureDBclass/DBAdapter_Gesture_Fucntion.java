package com.szkj.szgestureDBclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;



public class DBAdapter_Gesture_Fucntion {
	static final String KEY_ROWGID = "gesture_function_gid";  
    static final String KEY_ROWFID = "gesture_function_fid";  
    static final String KEY_NAME = "gesture_function_peoplename"; 
    static final String KEY_NUMBER = "gesture_function_phonenumber"; 
    static final String TAG = "DBAdapter";  
      
    static final String DATABASE_NAME = "MyDB";  
    static final String DATABASE_TABLE = "gesture_function";  
    static final int DATABASE_VERSION = 1;  
      
    static final String DATABASE_CREATE =   
            "create table gesture_function( gesture_function_gid integer primary key  not null, " +   
            "gesture_function_fid integer not null, gesture_function_peoplename text, gesture_function_phonenumber text);";  
    
    
    final Context context;  
      
    DatabaseHelper DBHelper;  
    SQLiteDatabase db;  
      
    public DBAdapter_Gesture_Fucntion(Context cxt)  
    {  
        this.context = cxt;  
        DBHelper = new DatabaseHelper(context);  
    }  
     
    private static class DatabaseHelper extends SQLiteOpenHelper  
    {  
  
        DatabaseHelper(Context context)  
        {  
            super(context, DATABASE_NAME, null, DATABASE_VERSION);  
        }  
        @Override  
        public void onCreate(SQLiteDatabase db) {  
            // TODO Auto-generated method stub  
            try  
            {  
                db.execSQL(DATABASE_CREATE);  
            }  
            catch(SQLException e)  
            {  
                e.printStackTrace();  
            }  
        }  
  
        @Override  
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {  
            // TODO Auto-generated method stub  
            Log.wtf(TAG, "Upgrading database from version "+ oldVersion + "to "+  
             newVersion + ", which will destroy all old data");  
            db.execSQL("DROP TABLE IF EXISTS gesture_function");  
            onCreate(db);  
        }  
    }  
      
    /**
     * 功能详细描述:打开数据库
     */
    public DBAdapter_Gesture_Fucntion open() throws SQLException  
    {  
        db = DBHelper.getWritableDatabase();  
        return this;  
    }  
    /**
     * 关闭数据库 
     */
    public void close()  
    {  
        DBHelper.close();  
    }  
      
    /**
     * 插入一条已定义过的手势功能关联数据  gid为手势id  fid为功能id 人名 手机号码是针对直接拨号这一功能的    别的功能可以为空
     */ 
    public long insertGesture_Fucntion(int gid, int fid, String name, String number)  
    {  
        ContentValues initialValues = new ContentValues();  
        initialValues.put(KEY_ROWGID, gid);  
        initialValues.put(KEY_ROWFID, fid);  
        initialValues.put(KEY_NAME, name);
        initialValues.put(KEY_NUMBER, number);
        return db.insert(DATABASE_TABLE, null, initialValues);  
    }  
    /**
     * 删除一条已定义过的手势功能关联数据
     */  
    public boolean deleteGesture_Fucntion(int rowId)  
    {  
        return db.delete(DATABASE_TABLE, KEY_ROWGID + "=" +rowId, null) > 0;  
    }  
    /**
     * 获取所有已定义过的手势功能关联数据
     */  
    public Cursor getAllGesture_Fucntion()  
    {  
        return db.query(DATABASE_TABLE, new String[]{ KEY_ROWGID,KEY_ROWFID,KEY_NAME,KEY_NUMBER}, null, null, null, null, null);  
    } 
    
//    public Cursor getAllGesture_Fucntion()  
//    {  
//        return db.query(DATABASE_TABLE, new String[]{ KEY_ROWGID,KEY_ROWFID,KEY_NAME,KEY_NUMBER}, null, null, null, null, null);  
//    }
//    
    /**
     * 根据id获取一条已定义过的手势功能关联数据  
     */
    public Cursor getGesture_Fucntion(int rowId) throws SQLException  
    {  
        Cursor mCursor =   
                db.query(true, DATABASE_TABLE, new String[]{ KEY_ROWGID,  
                         KEY_ROWFID, KEY_NAME, KEY_NUMBER}, KEY_ROWGID + "=" + rowId, null, null, null, null, null);  
        if (mCursor != null)  
            mCursor.moveToFirst();  
        return mCursor;  
    }  
    /**
     * 更新已定义过的手势功能关联数据第一个参数是手势id  第二个功能id  第三个联系人姓名   第四个联系人电话号码
     */
    public boolean updateGesture_Fucntion(int gid, int fid, String name, String phonenumber)  
    {  
        ContentValues args = new ContentValues();  
        args.put(KEY_ROWGID, gid);
        args.put(KEY_ROWFID, fid); 
        args.put(KEY_NAME, name);
        args.put(KEY_NUMBER, phonenumber);
        int i = db.update(DATABASE_TABLE, args, KEY_ROWGID + "=" +gid, null);
         return  i> 0;  
    } 
}
