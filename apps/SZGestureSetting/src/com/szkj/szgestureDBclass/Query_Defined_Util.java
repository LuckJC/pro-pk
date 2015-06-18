package com.szkj.szgestureDBclass;


import java.util.ArrayList;
import java.util.List;

import com.szkj.szgesturesetting.FunctionBean;
import com.szkj.szgesturesetting.GestureBean;
import com.szkj.szgesturesetting.Have_DefinedGestureBean;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
/**
 * 
 * <br>类描述:工具类
 * <br>功能详细描述:创建数据库 创建手势表 功能名称表  手势功能关联表   已经联合查询的相关方法
 * 
 * @author  lixd
 * @date  [2015-5-13]
 */
public class Query_Defined_Util{
//	private static String[] Ges = new String[]{"↑","↓","→","←","<",">","∨","∧","双击","O","2","3","6","7","8","9","a","b","c","d","e","g","h","k","l","m","n","p","q","r","s","u","w","y","z"};
	private static String[] Ges = new String[]{"↑","↓","→","←","<",">","∨","∧","双击","O","2","3","6","7","9","a","b","d","e","m","n","s","u","w"};
	private static String[] Fes = new String[]{"音量+","音量-","下一首","上一首","咕咚","助听器","音乐","语音助手","录音","清除后台程序","照相机","图库","直接拨号"};
	private static String[] GFes = new String[]{"音量+","音量-","下一首","上一首","咕咚","助听器","音乐","语音助手","录音","清除后台程序"};
	
	//--------------------手势表字段--------------------------
	static final String KEY_ROWID = "gesture_id";  
    static final String KEY_STYLE = "gesture_style";  
    static final String KEY_MODIFY = "gesture_ismodify";  
      
    static final String DATABASE_G_TABLE = "gesture";  
      
    static final String DATABASE_CREATE_G =   
            "create table gesture( gesture_id integer primary key autoincrement, " +   
            "gesture_style text not null, gesture_ismodify integer not null);"; 
  //--------------------手势表字段结束--------------------------
    
  //--------------------功能表字段--------------------------
    static final String KEY_F_ROWID = "function_id";  
    static final String KEY_FUNCTIONNAME = "function_name";  
      
    static final String DATABASE_F_TABLE = "function";  
      
    static final String DATABASE_CREATE_F =   
            "create table function( function_id integer primary key autoincrement, " +   
            "function_name text not null );";  
    //--------------------功能表字段结束--------------------------
    
    //--------------------功能手势表字段--------------------------
    static final String KEY_ROWGID = "gesture_function_gid";  
    static final String KEY_ROWFID = "gesture_function_fid";  
    static final String KEY_NAME = "gesture_function_peoplename"; 
    static final String KEY_NUMBER = "gesture_function_phonenumber"; 
      
    static final String DATABASE_GF_TABLE = "gesture_function";  
      
    static final String DATABASE_CREATE_GF =   
            "create table gesture_function( gesture_function_gid integer primary key  not null, " +   
            "gesture_function_fid integer not null, gesture_function_peoplename text, gesture_function_phonenumber text);"; 
    //--------------------功能手势表字段结束--------------------------
	
    static final String TAG = "DBAdapter";  
      
    static final String DATABASE_NAME = "MyDB";  
    static final String DATABASE_TABLE = "gesture_function";  
    static final int DATABASE_VERSION = 1;  
      
    static final String DATABASE_QUERY_DEFINED =   
    		"select a.*,b.*,c.[gesture_function_peoplename],c.[gesture_function_phonenumber] " +
    		"from gesture_function c ,gesture a,function b " +
    		"where c.[gesture_function_gid]=a.[gesture_id] and c.[gesture_function_fid]=b.[function_id]";
    
    static final String DATABASE_QUERY_NOT_DEFINED =   
    		"select a.* from gesture a where a.[gesture_id] not in (select c.[gesture_function_gid] from gesture_function c )";  
    
    static final String  DATABASE_QUERY_FUNCTION = "select * from function where function.[function_id] not in (1,2,3,4)";
    
    static final String CONTENT_PROVIDER="select a.[gesture_style],b.[function_name] from gesture_function c ,gesture a,function b where c.[gesture_function_gid]=a.[gesture_id] and c.[gesture_function_fid]=b.[function_id]";
   
    final Context context;  
     
    static DatabaseHelper DBHelper;  
    static SQLiteDatabase db;   
      
    public Query_Defined_Util(Context cxt)  
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
                db.execSQL(DATABASE_CREATE_G); 
                db.execSQL(DATABASE_CREATE_F);
                db.execSQL(DATABASE_CREATE_GF);
                for(int i=1;i<=Ges.length;i++)
                {
                	ContentValues values = new ContentValues();
                	values.put(KEY_STYLE, Ges[i-1]);
                	values.put(KEY_MODIFY, 0);
                	db.insert(DATABASE_G_TABLE, null, values);
                }
                for(int i=1;i<=4;i++)
                {
                	ContentValues args = new ContentValues();  
                    args.put(KEY_STYLE, Ges[i-1]);  
                    args.put(KEY_MODIFY, 1);  
                    db.update(DATABASE_G_TABLE, args, KEY_ROWID + "=" +i, null);  
                }
                for(int i=1;i<=Fes.length;i++)
                {
                	ContentValues values = new ContentValues();
                	values.put(KEY_FUNCTIONNAME, Fes[i-1]);
                	db.insert(DATABASE_F_TABLE, null, values);
                }
                for(int i=1;i<=GFes.length;i++)
                {
                	ContentValues initialValues = new ContentValues();  
                    initialValues.put(KEY_ROWGID, i);  
                    initialValues.put(KEY_ROWFID, i);  
                    initialValues.put(KEY_NAME, "");
                    initialValues.put(KEY_NUMBER, "");
                    db.insert(DATABASE_GF_TABLE, null, initialValues); 
                }
                
                System.out.println(db.getVersion());
            	
            }  
            catch(SQLException e)  
            {  
                e.printStackTrace();  
            }  
        }  
  
        @Override  
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {  
            // TODO Auto-generated method stub  
        	System.out.println("oldVersion="+oldVersion+"  newVersion="+newVersion);
        }  
    }  
      
    /**
     * 功能详细描述:打开数据库
     */
    public SQLiteDatabase open() throws SQLException  
    {  
        db = DBHelper.getWritableDatabase();  
        return db;  
    }    
    /**
     * 关闭数据库 
     */
    public void close()  
    {  
        DBHelper.close();  
    }  
    /**
     * 获取已经定义的手势关联数据
     */
     public List<Have_DefinedGestureBean> getDefinedData()
      {
    	 List<Have_DefinedGestureBean> list = new ArrayList<Have_DefinedGestureBean>();
    	  Cursor cs = db.rawQuery(DATABASE_QUERY_DEFINED, null);
    	  int mm = cs.getColumnCount();
    	  Log.i("lixianda", ""+mm);
    	  while (cs.moveToNext()) {
				Have_DefinedGestureBean bean = new Have_DefinedGestureBean();
				bean.setGesture_id(cs.getInt(0));
				bean.setGesture_style(cs.getString(1));
				bean.setIsmodify(cs.getInt(2));
				bean.setFunction_id(cs.getInt(3));
				bean.setFunction_name(cs.getString(4));
				bean.setPeople_name(cs.getString(5));
				bean.setPhone_number(cs.getString(6));
				list.add(bean);
			}
    	  cs.close();
//    	  this.close();
    	  return list;
      }
     
     /**
      * 获取还未定义的手势数据
      */
      public List<GestureBean> getNotDefinedData()
       {
     	 List<GestureBean> list = new ArrayList<GestureBean>();
     	  Cursor cs = db.rawQuery(DATABASE_QUERY_NOT_DEFINED, null);
     	  int mm = cs.getColumnCount();
     	  Log.i("lixianda", ""+mm);
     	  while (cs.moveToNext()) {
     		 GestureBean bean = new GestureBean();
     		 	bean.setGesdture_id(cs.getInt(0));
 				bean.setGesture_style(cs.getString(1));
 				bean.setIsmodify(cs.getInt(2));
 				list.add(bean);
 			}
     	  cs.close();
//     	  this.close();
     	  return list;
       }
      
      /**
       * 获取除了不可更改的手势后    所有功能名称
       */
       public List<FunctionBean> getFunctionData()
        {
    	   List<FunctionBean> list = new ArrayList<FunctionBean>();
    	  
      	  Cursor cs = db.rawQuery(DATABASE_QUERY_FUNCTION, null);
      	  int mm = cs.getColumnCount();
      	  Log.i("lixianda", ""+mm);
      	  while (cs.moveToNext()) {
      		FunctionBean bean = new FunctionBean();
      		 	bean.setFunction_id(cs.getInt(0));
      		 	bean.setFunction_name(cs.getString(1));
  				list.add(bean);
  			}
      	  cs.close();
//      	  this.close();
      	  return list;
        }
   //------------------------------------------------------------------------------------------------------
   
       /**
        * 插入一条手势数据  int值0代表可以修改  1代表不可以修改
        */ 
       public long insertGesture(String style, int modify)  
       {  
           ContentValues initialValues = new ContentValues();  
           initialValues.put(KEY_STYLE, style);  
           initialValues.put(KEY_MODIFY, modify);  
           return db.insert(DATABASE_G_TABLE, null, initialValues);  
       }  
       /**
        * 删除一条手势数据
        */  
       public boolean deleteGesture(int rowId)  
       {  
           return db.delete(DATABASE_G_TABLE, KEY_ROWID + "=" +rowId, null) > 0;  
       }
       
       /**
        * 删除大于某个id的手势数据
        */  
       public boolean deleteGesture(int rowId,int type)  
       {  
           return db.delete(DATABASE_G_TABLE, KEY_ROWID + ">=" +rowId, null) > 0;  
       }
       
       /**
        * 获取所有手势数据
        */  
       public Cursor getAllGesture()  
       {  
           return db.query(DATABASE_G_TABLE, new String[]{ KEY_ROWID,KEY_STYLE,KEY_MODIFY}, null, null, null, null, null);  
       }  
       /**
        * 根据id获取一条手势数据  
        */
       public Cursor getGesture(int rowId) throws SQLException  
       {  
           Cursor mCursor =   
                   db.query(true, DATABASE_G_TABLE, new String[]{ KEY_ROWID,  
                            KEY_STYLE, KEY_MODIFY}, KEY_ROWID + "=" + rowId, null, null, null, null, null);  
           if (mCursor != null)  
               mCursor.moveToFirst();  
           return mCursor;  
       }  
       /**
        * 更新手势 int值0代表可以修改  1代表不可以修改
        */
       public static boolean updateGesture(int rowId, String style, int modify)  
       {  
           ContentValues args = new ContentValues();  
           args.put(KEY_STYLE, style);  
           args.put(KEY_MODIFY, modify);  
           return db.update(DATABASE_G_TABLE, args, KEY_ROWID + "=" +rowId, null) > 0;  
       }
       //------------------------------------------------------------------------------------------------------------
      
 
       /**
        * 插入一条功能名数据
        */ 
       public long insertFunction(String functionname)  
       {  
           ContentValues initialValues = new ContentValues();  
           initialValues.put(KEY_FUNCTIONNAME, functionname);  
           return db.insert(DATABASE_F_TABLE, null, initialValues);  
       }  
       /**
        * 删除一条功能名数据
        */  
       public boolean deleteFunction(int rowId)  
       {  
           return db.delete(DATABASE_F_TABLE, KEY_F_ROWID + "=" +rowId, null) > 0;  
       }  
       /**
        * 获取所有功能名数据
        */  
       public Cursor getAllFunction()  
       {  
           return db.query(DATABASE_F_TABLE, new String[]{ KEY_F_ROWID,KEY_FUNCTIONNAME }, null, null, null, null, null);  
       }  
       /**
        * 根据id获取一条功能名数据  
        */
       public Cursor getFunction(int rowId) throws SQLException  
       {  
           Cursor mCursor =   
                   db.query(true, DATABASE_F_TABLE, new String[]{ KEY_F_ROWID,  
                            KEY_FUNCTIONNAME }, KEY_F_ROWID + "=" + rowId, null, null, null, null, null);  
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
           return db.update(DATABASE_F_TABLE, args, KEY_F_ROWID + "=" +rowId, null) > 0;  
       }
       //-------------------------------------------------------------------------------------------------------------------
        
       
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
           return db.insert(DATABASE_GF_TABLE, null, initialValues);  
       }  
       /**
        * 删除一条已定义过的手势功能关联数据
        */  
       public boolean deleteGesture_Fucntion(int rowId)  
       {  
           return db.delete(DATABASE_GF_TABLE, KEY_ROWGID + "=" +rowId, null) > 0;  
       }  
       /**
        * 获取所有已定义过的手势功能关联数据
        */  
       public Cursor getAllGesture_Fucntion()  
       {  
           return db.query(DATABASE_GF_TABLE, new String[]{ KEY_ROWGID,KEY_ROWFID,KEY_NAME,KEY_NUMBER}, null, null, null, null, null);  
       } 
       
       /**
        * 根据id获取一条已定义过的手势功能关联数据  
        */
       public Cursor getGesture_Fucntion(int rowId) throws SQLException  
       {  
           Cursor mCursor =   
                   db.query(true, DATABASE_GF_TABLE, new String[]{ KEY_ROWGID,  
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
           int i = db.update(DATABASE_GF_TABLE, args, KEY_ROWGID + "=" +gid, null);
            return  i> 0;  
       }
       
}
