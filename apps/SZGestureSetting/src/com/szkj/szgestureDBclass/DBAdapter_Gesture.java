package com.szkj.szgestureDBclass;

import android.content.ContentValues;  
import android.content.Context;  
import android.database.Cursor;  
import android.database.SQLException;  
import android.database.sqlite.SQLiteDatabase;  
import android.database.sqlite.SQLiteOpenHelper;  
import android.util.Log;  

public class DBAdapter_Gesture {
	static final String KEY_ROWID = "gesture_id";  
    static final String KEY_STYLE = "gesture_style";  
    static final String KEY_MODIFY = "gesture_ismodify";  
    static final String TAG = "DBAdapter";  
      
    static final String DATABASE_NAME = "MyDB";  
    static final String DATABASE_TABLE = "gesture";  
    static final int DATABASE_VERSION = 1;  
      
    static final String DATABASE_CREATE =   
            "create table gesture( gesture_id integer primary key autoincrement, " +   
            "gesture_style text not null, gesture_ismodify integer not null);";  
    final Context context;  
      
    DatabaseHelper DBHelper;  
    SQLiteDatabase db;  
      
    public DBAdapter_Gesture(Context cxt)  
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
            db.execSQL("DROP TABLE IF EXISTS gesture");  
            onCreate(db);  
        }  
    }  
      
    /**
     * 功能详细描述:打开数据库
     */
    public DBAdapter_Gesture open() throws SQLException  
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
     * 插入一条手势数据  int值0代表可以修改  1代表不可以修改
     */ 
    public long insertGesture(String style, int modify)  
    {  
        ContentValues initialValues = new ContentValues();  
        initialValues.put(KEY_STYLE, style);  
        initialValues.put(KEY_MODIFY, modify);  
        return db.insert(DATABASE_TABLE, null, initialValues);  
    }  
    /**
     * 删除一条手势数据
     */  
    public boolean deleteGesture(int rowId)  
    {  
        return db.delete(DATABASE_TABLE, KEY_ROWID + "=" +rowId, null) > 0;  
    }
    
    /**
     * 删除大于某个id的手势数据
     */  
    public boolean deleteGesture(int rowId,int type)  
    {  
        return db.delete(DATABASE_TABLE, KEY_ROWID + ">=" +rowId, null) > 0;  
    }
    
    /**
     * 获取所有手势数据
     */  
    public Cursor getAllGesture()  
    {  
        return db.query(DATABASE_TABLE, new String[]{ KEY_ROWID,KEY_STYLE,KEY_MODIFY}, null, null, null, null, null);  
    }  
    /**
     * 根据id获取一条手势数据  
     */
    public Cursor getGesture(int rowId) throws SQLException  
    {  
        Cursor mCursor =   
                db.query(true, DATABASE_TABLE, new String[]{ KEY_ROWID,  
                         KEY_STYLE, KEY_MODIFY}, KEY_ROWID + "=" + rowId, null, null, null, null, null);  
        if (mCursor != null)  
            mCursor.moveToFirst();  
        return mCursor;  
    }  
    /**
     * 更新手势 int值0代表可以修改  1代表不可以修改
     */
    public boolean updateGesture(int rowId, String style, int modify)  
    {  
        ContentValues args = new ContentValues();  
        args.put(KEY_STYLE, style);  
        args.put(KEY_MODIFY, modify);  
        return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" +rowId, null) > 0;  
    } 
}
