package com.szkj.szgesturesetting;

import java.util.ArrayList;
import java.util.List;

import com.szkj.szgestureDBclass.Query_Defined_Util;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

public class SettingMain_Activity extends Activity {

	private Item_Adapter adapter;
	/**
	 * 存放已经定义的手势关联数据
	 */
	private List<Have_DefinedGestureBean> list_defined;
	/**
	 * 存放未定义的手势
	 */
	private List<GestureBean> list_gesture_NOdefined;
	/**
	 * 存放所有功能名称
	 */
	private List<FunctionBean> list_function;
	/**
	 * 存放联系人列表
	 */
	private List<String> list_PeopleName;
	
	Query_Defined_Util util;
	
	private ListView lv;
	private TextView addgesture;
	
	String[] str;
	int tmp_gid;
	int tmp_fid;
	String tmp_style;
	int tmp_modify;
	String tmp_fname;
	String tmp_people_name;
	String  tmp_phone_number;
	
	boolean IsfisrtAddData ;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_setting_main);
		lv = (ListView)findViewById(R.id.list);
		
		util=new Query_Defined_Util(this);
		util.open();
		
		list_defined = util.getDefinedData();
		list_gesture_NOdefined = util.getNotDefinedData();
		list_function = util.getFunctionData();
		list_PeopleName = this.QueryPeople();
		
		addgesture = (TextView)findViewById(R.id.add_gesture);
		addgesture.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				toArry(1);
				alertDialog();
			}
		});
		
		adapter=new Item_Adapter(this, list_defined);
		
		lv.setAdapter(adapter);
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				if(position<=3) //前四项不可更改
				{
					view.setClickable(false);
				}
				else
				{
					final int i = position;
					
					Dialog alertDialog = new AlertDialog.Builder(SettingMain_Activity.this).
						    setTitle("确定要不需要这种手势？" +":\n"+list_defined.get(position).getGesture_style()).
						    setPositiveButton("确认", new DialogInterface.OnClickListener() {

						     @Override
						     public void onClick(DialogInterface dialog, int which) {
						    	 util.deleteGesture_Fucntion(list_defined.get(i).getGesture_id());//前四项不可改变
						    	 list_defined = util.getDefinedData();
						    	 adapter.setData(list_defined);
					    		 adapter.notifyDataSetChanged();
					    		 list_gesture_NOdefined = util.getNotDefinedData();
//					    		 toArry(1);
						     }
						    }).
						    setNegativeButton("取消", new DialogInterface.OnClickListener() {

						     @Override
						     public void onClick(DialogInterface dialog, int which) {
						      // TODO Auto-generated method stub
						     }
						    }).
						    create();
						  alertDialog.show();
				}
				
				return true;
			}
			
		});
		lv.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				if(position<=3) //前四项不可更改
				{
					view.setClickable(false);
				}
				else
				{
					toArry(0);
					alertDialog(position);
				}
			}
		});	
		
	}
	
	//转换成数组
	void toArry(int type)
	{
		switch(type)
		{	// 0 手势转成数组格式
			case 0:
				str = new String[list_function.size()];
				for (int i = 0; i < list_function.size(); i++) {
				str[i] = list_function.get(i).getFunction_name();
				}
				break;
			//	1功能名称转换成数组
			case 1:
				str = new String[list_gesture_NOdefined.size()];
				for(int i=0;i<list_gesture_NOdefined.size();i++)
				{
					str[i] = list_gesture_NOdefined.get(i).getGesture_style();
				}
				break;
			//联系人名字转换成数组
			case 2:
				str = new String[list_PeopleName.size()];
				for(int i=0;i<list_PeopleName.size();i++)
				{
					str[i] = list_PeopleName.get(i);
				}
				break;
			}
	}
	
	/**
	 * <br>功能简述:
	 * <br>功能详细描述: 更改功能名称
	 * <br>注意:
	 * @param position
	 */
	int s1_gid;
	int s1_fid;
	String s1_style;
	String s1_function_name;
	String s1_peoplename;
	String s1_number;
	void alertDialog(final int position)
	{
		
		 
		 Dialog alertDialog = new AlertDialog.Builder(this).
				    setTitle("更改 功能名称")
				    .setSingleChoiceItems(str, -1, new DialogInterface.OnClickListener() {
				 
				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 s1_gid = list_defined.get(position).getGesture_id();
				    	 s1_fid = list_function.get(which).getFunction_id();//因为前四条为固定不可变而且不列出来的
				    	 s1_style = list_defined.get(position).getGesture_style();
				    	 s1_function_name = list_function.get(which).getFunction_name();
				    	 //Toast.makeText(SettingMain_Activity.this, "s1_gid="+s1_gid+"  "+s1_style+" 更改为："+"s1_fid="+s1_fid+"?"+"which="+which, Toast.LENGTH_SHORT).show();
				     }
				    }).
				    setPositiveButton("完成", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 if(s1_function_name.equals("直接拨号"))
				    	 {
				    		 if(list_PeopleName.size()<1)
				    		 {
				    			 Toast.makeText(SettingMain_Activity.this, "没有联系人", Toast.LENGTH_SHORT).show();
				    		 }
				    		 else
				    		 { 
				    			 toArry(2);   //联系人
				    			 Dialog alertDialog = new AlertDialog.Builder(SettingMain_Activity.this).
				    					    setTitle("选择联系人!").
				    					    setIcon(R.drawable.ic_launcher)
				    					    .setSingleChoiceItems(str, -1, new DialogInterface.OnClickListener() {
				    					 
				    					     @Override
				    					     public void onClick(DialogInterface dialog, int which) {
//				    					    	 tempid = which+4;//加4是因为数据库头四条功能名称没有获取上来   也就是上下左右手势
				    					    	 s1_peoplename = list_PeopleName.get(which);
				    					    	
				    					    	 //Toast.makeText(SettingMain_Activity.this, "您选择了： "+s1_peoplename, Toast.LENGTH_SHORT).show();
				    					     }
				    					    })
				    					    .setPositiveButton("完成", new DialogInterface.OnClickListener() {

				    					     @Override
				    					     public void onClick(DialogInterface dialog, int which) {
				    					    	 s1_number = FindPhoneNumber(s1_peoplename);
				    				    		 //position+1是因为数据库id是从1开始   而在listview的position是从0开始
				    					    	 util.updateGesture_Fucntion(s1_gid, s1_fid, s1_peoplename, s1_number);
				    				    		 list_defined = util.getDefinedData();
				    				    		 adapter.setData(list_defined);
				    				    		 adapter.notifyDataSetChanged();

				    					     }
				    					    }).
				    					    setNegativeButton("取消", new DialogInterface.OnClickListener() {

				    					     @Override
				    					     public void onClick(DialogInterface dialog, int which) {
				    					      // TODO Auto-generated method stub
				    					     }
				    					    }).
				    					    create();
				    					  alertDialog.show();
				    		 }
				    	 }
				    	 else
				    	 {	 
					    	 util.updateGesture_Fucntion(s1_gid, s1_fid, null, null);
					   
					    	 list_defined = util.getDefinedData();
					    	 adapter.setData(list_defined);
				    		 adapter.notifyDataSetChanged();
				    	 }
				    	}
				    }).
				    setNegativeButton("取消", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				      // TODO Auto-generated method stub
				     }
				    }).
				    create();
				  alertDialog.show();
	}
	
	/**
	 * <br>功能简述:
	 * <br>功能详细描述:选择手势
	 * <br>注意:
	 */
	 int s2_gid;
	 String s2_style;
	 int s2_modify;
	
	void alertDialog()
	{
		
		
		 Dialog alertDialog = new AlertDialog.Builder(this).
				    setTitle("选择手势")
				    
				    .setSingleChoiceItems(str, -1, new DialogInterface.OnClickListener() {
				 
				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 s2_gid = list_gesture_NOdefined.get(which).getGesdture_id();
				    	 s2_style = list_gesture_NOdefined.get(which).getGesture_style();
				    	 s2_modify = list_gesture_NOdefined.get(which).getIsmodify();
				    	 //Toast.makeText(SettingMain_Activity.this, "您选择了： "+s2_style+"   "+s2_gid, Toast.LENGTH_SHORT).show();
				     }
				    })
				    .setPositiveButton("完成", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 toArry(0); //功能
				    	 alertDialog(s2_gid, s2_style, s2_modify);
				     }
				    }).
				    setNegativeButton("取消", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				      // TODO Auto-generated method stub
				     }
				    }).
				    create();
				  alertDialog.show();
	}
	
	/**
	 * <br>功能简述: 选择功能名称
	 * <br>功能详细描述:根据选中的手势得到相关数据后    再选择功能名称
	 * <br>注意:
	 */
	 int s3_fid;
	 String s3_fname;
	
	private void alertDialog(final int id,String style,int modify)
	{
		
		
		 Dialog alertDialog = new AlertDialog.Builder(this).
				    setTitle("选择功能名称")
				    .setSingleChoiceItems(str, -1, new DialogInterface.OnClickListener() {
				 
				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 s3_fid=list_function.get(which).getFunction_id();//加4是因为数据库头四条功能名称没有获取上来   也就是上下左右手势
				    	 s3_fname = list_function.get(which).getFunction_name();
				    	// Toast.makeText(SettingMain_Activity.this,"id="+id+ "您选择了： "+s3_fname+"   "+s3_fid, Toast.LENGTH_SHORT).show();
				     }
				    })
				    .setPositiveButton("完成", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 if(s3_fname.equals("直接拨号"))
				    	 {
				    		 if(list_PeopleName.isEmpty())
				    		 {
				    			 Toast.makeText(SettingMain_Activity.this, "没有联系人", Toast.LENGTH_SHORT).show();
				    		 }
				    		 else
				    		 { 
				    			 toArry(2);   //联系人
					    		 alertDialog(s2_gid,s3_fid);
				    		 }
				    	}
				    	 else{
				    		 //tmp_gid+1是因为数据库id是从1开始   而在listview的position是从0开始
				    		 util.insertGesture_Fucntion(s2_gid, s3_fid, null, null);
				    		 list_defined = util.getDefinedData();
				    		 list_gesture_NOdefined = util.getNotDefinedData();
				    		 
				    		 adapter.setData(list_defined);
				    		 adapter.notifyDataSetChanged();
				    		 
				    	 	}
				     }
				    }).
				    setNegativeButton("取消", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				      // TODO Auto-generated method stub
				     }
				    }).
				    create();
				  alertDialog.show();
	}
	
	/**
	 * <br>功能简述: 选择联系人
	 * <br>功能详细描述:选中的功能名称如果是直接拨号  就选择联系人
	 * <br>注意:
	 */

	String s4_name;
	String s4_number;
	private void alertDialog(final int gid,final int fid)
	{
		 
		
		 Dialog alertDialog = new AlertDialog.Builder(this).
				    setTitle("选择联系人")
				    .setSingleChoiceItems(str, -1, new DialogInterface.OnClickListener() {
				 
				     @Override
				     public void onClick(DialogInterface dialog, int which) {
//				    	 tempid = which+4;//加4是因为数据库头四条功能名称没有获取上来   也就是上下左右手势
				    	 s4_name = list_PeopleName.get(which);
				    	 //Toast.makeText(SettingMain_Activity.this, "您选择了： "+s4_name, Toast.LENGTH_SHORT).show();
				     }
				    })
				    .setPositiveButton("完成", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				    	 s4_number = FindPhoneNumber(s4_name);
			    		 //position+1是因为数据库id是从1开始   而在listview的position是从0开始
				    	 util.insertGesture_Fucntion(s2_gid, s3_fid, s4_name, s4_number);
			    		 list_defined = util.getDefinedData();
			    		 adapter.setData(list_defined);
			    		 adapter.notifyDataSetChanged();

				     }
				    }).
				    setNegativeButton("取消", new DialogInterface.OnClickListener() {

				     @Override
				     public void onClick(DialogInterface dialog, int which) {
				      // TODO Auto-generated method stub
				     }
				    }).
				    create();
				  alertDialog.show();
	}
	
	
	/**
	 * <br>功能简述:
	 * <br>功能详细描述:查找所有联系人
	 * <br>注意:display_name
	 * @return 联系人列表
	 */
	List<String> QueryPeople()
	{
		List<String> names= new ArrayList<String>();
		Cursor cs = this.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, 
                null, null, null, null);
		names.clear();
		while(cs.moveToNext())
		{
			String name = new String();
			 name = cs.getString(cs  
	                    .getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)); 
			 if(name!=null)
			 {names.add(name);}
		}
		if(cs != null)
		{
			cs.close();
		}
		return names;
	}
	
	/**
	 * <br>功能简述:查找号码
	 * <br>功能详细描述:根据联系人名字查找这个联系人的所有号码    默认返回第一个号码
	 * <br>注意:联系人名字不能为空     多条联系人
	 * @param name 联系人姓名
	 * @return 字符串
	 */
	String FindPhoneNumber(String name) {
		ContentResolver contentResolver = this.getContentResolver();
		Cursor cursor = contentResolver.query(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
				new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
				ContactsContract.PhoneLookup.DISPLAY_NAME + "='" + name + "'", null, null);
		
		while (cursor.moveToNext()) {
			return cursor.getString(0);
		}
		return "";
	}
	
	
}
