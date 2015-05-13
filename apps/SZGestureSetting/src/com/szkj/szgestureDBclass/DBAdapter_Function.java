package com.szkj.szgestureDBclass;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DBAdapter_Function {
	static final String KEY_ROWID = "function_id";  
    static final String KEY_FUNCTIONNAME = "function_name";  
    static final String TAG = "DBAdapter";  
      
    static final String DATABASE_NAME = "MyDB";  
    static final String DATABASE_TABLE = "function";  
    static final int DATABASE_VERSION = 1;  
      
    static final String DATABASE_CREATE =   
            "create table function( function_id integer primary key autoincrement, " +   
            "function_name text not null );";  
    final Context context;  
      
    DatabaseHelper DBHelper;  
    SQLiteDatabase db;  
      
    public DBAdapter_Function(Context cxt)  
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
            db.execSQL("DROP TABLE IF EXISTS function");  
            onCreate(db);  
        }  
    }  
      
    /**
     * 功能详细描述:打开数据库
     */
    public DBAdapter_Function open() throws SQLException  
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
     * 插入一条功能名数据
     */ 
    public long insertFunction(String functionname)  
    {  
        ContentValues initialValues = new ContentValues();  
        initialValues.put(KEY_FUNCTIONNAME, functionname);  
        return db.insert(DATABASE_TABLE, null, initialValues);  
    }  
    /**
     * 删除一条功能名数据
     */  
    public boolean deleteFunction(int rowId)  
    {  
        return db.delete(DATABASE_TABLE, KEY_ROWID + "=" +rowId, null) > 0;  
    }  
    /**
     * 获取所有功能名数据
     */  
    public Cursor getAllFunction()  
    {  
        return db.query(DATABASE_TABLE, new String[]{ KEY_ROWID,KEY_FUNCTIONNAME }, null, null, null, null, null);  
    }  
    /**
     * 根据id获取一条功能名数据  
     */
    public Cursor getFunction(int rowId) throws SQLException  
    {  
        Cursor mCursor =   
                db.query(true, DATABASE_TABLE, new String[]{ KEY_ROWID,  
                         KEY_FUNCTIONNAME }, KEY_ROWID + "=" + rowId, null, null, null, null, null);  
        if (mCursor != null)  
            mCursor.moveToFirst();  
        return mCursor;  
    }  
    /**
     * 更新功能名
     */
    public boolean updateFunction(int rowId, String functionname)  
    {  
        ContentValues args = new ContentValues();  
        args.put(KEY_FUNCTIONNAME, functionname);  
        return db.update(DATABASE_TABLE, args, KEY_ROWID + "=" +rowId, null) > 0;  
    } 
}
