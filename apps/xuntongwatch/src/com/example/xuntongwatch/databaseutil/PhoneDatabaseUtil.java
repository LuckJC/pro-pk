package com.example.xuntongwatch.databaseutil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.Contacts.People.Phones;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Data;
import android.text.TextUtils;
import android.util.Log;

import com.example.xuntongwatch.R;
import com.example.xuntongwatch.data.DbUtil;
import com.example.xuntongwatch.entity.CallRecords;
import com.example.xuntongwatch.entity.Contact;
import com.example.xuntongwatch.entity.GridViewItemImageView;

public class PhoneDatabaseUtil {

	public final static int ALL_TYPE = -11111;
	public final static int MISSED_TYPE = Calls.MISSED_TYPE;
	public final static int INCOMING_TYPE = Calls.INCOMING_TYPE;
	public final static int OUTGOING_TYPE = Calls.OUTGOING_TYPE;
	
	private final static Uri contact_uri = ContactsContract.Contacts.CONTENT_URI;
	private final static Uri raw_contact_uri = ContactsContract.RawContacts.CONTENT_URI;
	private final static Uri data_uri = ContactsContract.Data.CONTENT_URI;

	/**
	 * 查找 所有通话记录 按条件查询
	 * 
	 * @param context
	 * @param type
	 *            MISSED_TYPE 未接 INCOMING_TYPE 呼入 OUTGOING_TYPE 呼出 ALL_TYPE 全部
	 * @return
	 */
	public static ArrayList<CallRecords> readAllCallRecord(Context context) {
		DbUtil util = new DbUtil(context);
		ArrayList<String> has = new ArrayList<String>();
		ArrayList<CallRecords> list = new ArrayList<CallRecords>();
		String[] selection = new String[] { Calls.NUMBER, Calls.CACHED_NAME,
				Calls.DATE, Calls.NEW };
		// String where = " 0==0 group by " + Calls.NUMBER;
		// String where = "0=0) GROUP BY (" + Calls.NUMBER;
		Cursor cursor = context.getContentResolver().query(Calls.CONTENT_URI,
				selection, null, null, "date desc");
		int i = 0;
		long time3 = System.currentTimeMillis();
		StringBuffer sb = new StringBuffer("select * from telephone where ");
		while (cursor.moveToNext()) {
			// for (int j = 0; j < h; j++) {
			// String on = cursor.getColumnName(j);
			// String to = cursor.getString(j);
			// Log.e("~~ " + i, on + " == " + to);
			// }
			// i++;
			String phone = cursor
					.getString(cursor.getColumnIndex(Calls.NUMBER));
			if (has.contains(phone)) {
				continue;
			}
			i++;
			has.add(phone);
			String name = cursor.getString(cursor
					.getColumnIndex(Calls.CACHED_NAME));
			long date = cursor.getLong(cursor.getColumnIndex(Calls.DATE));
			String isSee = cursor.getString(cursor.getColumnIndex(Calls.NEW));
			CallRecords record = new CallRecords();

			String[] msgs = util.findByPhone(phone);
			String msg = msgs[0] + " " + msgs[1];
			record.setName(name);
			record.setPhone(phone);
			record.setDate(date);
			record.setIsSee(isSee);
			record.setMsg(msg);
			list.add(record);
		}
		long time4 = System.currentTimeMillis();
		Log.e("", time4 - time3 + "ms");
		Log.e("", (time4 - time3) / i + "ms" + "  ~~~" + i);
		// CallLog.Calls.getLastOutgoingCall(context);
		return list;
	}

//	@SuppressLint("InlinedApi")
//	public static ArrayList<Contact> readAllPhoneUser(Context context) {
//		// 字段
//		// Phone.DISPLAY_NAME 姓名
//		// Phone.DATA4 手机号
//		// Phone.CONTACT_LAST_UPDATED_TIMESTAMP 最后更新的时间
//		// Phone.SORT_KEY_PRIMARY 带拼音的 姓名 比如 姓名是：你好 -- 那么输出的就是：(n i 你h ao 好)
//		// Phone.PHOTO_URI 头像的Uri
//		// Phone._ID 联系人列表的ID
//		// Phone.CONTACT_ID 与其他表关联的字段
//
//		ArrayList<Contact> list = new ArrayList<Contact>();
//		Cursor cursor = context.getContentResolver().query(
//				Phone.CONTENT_URI,
//				new String[] { Phone.DISPLAY_NAME, Phone.DATA4,
//						Phone.PHOTO_URI, Phone._ID }, null, null, null);
//		while (cursor.moveToNext()) {
//			String name = cursor.getString(cursor
//					.getColumnIndex(Phone.DISPLAY_NAME));
//			String phone = cursor.getString(cursor.getColumnIndex(Phone.DATA4));
//			String headUri = cursor.getString(cursor
//					.getColumnIndex(Phone.PHOTO_URI));
//			String _id = cursor.getString(cursor.getColumnIndex(Phone._ID));
//			Contact contact = new Contact(headUri, name, phone, _id);
//			list.add(contact);
//		}
//		return list;
//	}

	/**
	 * 删除联系人
	 * 
	 * @param context
	 * @param _id
	 */
	public static void deleteContact(Context context, String name) {
		Cursor cursor = context.getContentResolver().query(
				android.provider.ContactsContract.Data.CONTENT_URI,
				new String[] { Data.RAW_CONTACT_ID },
				ContactsContract.Contacts.DISPLAY_NAME + "=?",
				new String[] { name }, null);
		long id = -1l;
		if (cursor.moveToFirst()) {
			id = cursor.getLong(cursor.getColumnIndex(Data.RAW_CONTACT_ID));
		}
		cursor.close();

		if (id == -1l) {
			return;
		}

		ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

		ops.add(ContentProviderOperation.newDelete(
				ContentUris.withAppendedId(RawContacts.CONTENT_URI, id))
				.build());
		try {
			context.getContentResolver().applyBatch(ContactsContract.AUTHORITY,
					ops);
		} catch (Exception e) {
		}

	}

	/**
	 * Data.MIMETYPE类型:
	 * 
	 * 1、Email.CONTENT_ITEM_TYPE 邮箱 --2、Im.CONTENT_ITEM_TYPE 聊天账号
	 * 3、StructuredPostal.CONTENT_ITEM_TYPE 住址--4、Photo.CONTENT_ITEM_TYPE 图片
	 * 5、Phone.CONTENT_ITEM_TYPE 电话号码--6、StructuredName.CONTENT_ITEM_TYPE 姓名
	 * 7、Organization.CONTENT_ITEM_TYPE 公司+职位--8、Nickname.CONTENT_ITEM_TYPE 昵称
	 * 9、GroupMembership.CONTENT_ITEM_TYPE 所属组--10、Note.CONTENT_ITEM_TYPE 备注
	 * 
	 * @param context
	 * @param name
	 * @param phone
	 * @param email
	 * @param qq
	 */
	public static void addContact(Context context, String name, String phone,
			String email, String qq) {
		ContentValues values = new ContentValues();
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation operation1 = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValue("_id", null)
				.build();
		operations.add(operation1);

		// 向data表插入姓名数据
		if (!TextUtils.isEmpty(name)) {
			values.clear();
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, name);
			operations.add(newInsertContentProviderOperation(values));
		}

		// 向data表插入电话数据
		if (!TextUtils.isEmpty(phone)) {
			values.clear();
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.NUMBER, phone);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			operations.add(newInsertContentProviderOperation(values));
		}

		// 向data表插入Email数据
		if (!TextUtils.isEmpty(email)) {
			values.clear();
			values.put(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE);
			values.put(Email.DATA, email);
			values.put(Email.TYPE, Email.TYPE_WORK);
			operations.add(newInsertContentProviderOperation(values));
		}

		// 向data表插入QQ数据
		if (!TextUtils.isEmpty(qq)) {
			values.clear();
			values.put(Data.MIMETYPE, Im.CONTENT_ITEM_TYPE);
			values.put(Im.DATA, qq);
			values.put(Im.PROTOCOL, Im.PROTOCOL_QQ);
			operations.add(newInsertContentProviderOperation(values));
		}
		// 向data表插入头像数据
		Bitmap sourceBitmap = BitmapFactory.decodeResource(
				context.getResources(), R.drawable.message_chat_paizhao);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		// 将Bitmap压缩成PNG编码，质量为100%存储
		sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
		byte[] avatar = os.toByteArray();
		values.clear();
		values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
		values.put(Photo.PHOTO, avatar);
		operations.add(newInsertContentProviderOperation(values));

		// 在事务中对多个操作批量执行
		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, operations);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
	}
	
	public static void addContact(Context context,Contact contact) {
		ContentValues values = new ContentValues();
		ContentResolver resolver = context.getContentResolver();
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		ContentProviderOperation operation1 = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
				.withValue("_id", null)
				.build();
		operations.add(operation1);
		
		byte[] photo = contact.getContact_head();
		int contact_id = contact.getContact_id();
		String contact_name = contact.getContact_name();
		String contact_phone = contact.getContact_phone();

		// 向data表插入姓名数据
		if (!TextUtils.isEmpty(contact_name)) {
			values.clear();
			values.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
			values.put(StructuredName.GIVEN_NAME, contact_name);
			operations.add(newInsertContentProviderOperation(values));
		}

		// 向data表插入电话数据
		if (!TextUtils.isEmpty(contact_phone)) {
			values.clear();
			values.put(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
			values.put(Phone.NUMBER, contact_phone);
			values.put(Phone.TYPE, Phone.TYPE_MOBILE);
			operations.add(newInsertContentProviderOperation(values));
		}

		if(photo != null)
		{
//			// 向data表插入头像数据
//			Bitmap sourceBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.message_chat_paizhao);
//			ByteArrayOutputStream os = new ByteArrayOutputStream();
			// 将Bitmap压缩成PNG编码，质量为100%存储
//			sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
//			byte[] avatar = os.toByteArray();
			values.clear();
			values.put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE);
			values.put(Photo.PHOTO, photo);
			operations.add(newInsertContentProviderOperation(values));
		}
		

		// 在事务中对多个操作批量执行
		try {
			resolver.applyBatch(ContactsContract.AUTHORITY, operations);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 新插入一条记录
	 * 
	 * @param values
	 * @return
	 */
	private static ContentProviderOperation newInsertContentProviderOperation(
			ContentValues values) {
		ContentProviderOperation operation = ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(Data.RAW_CONTACT_ID, 0)
				.withValues(values).build();
		return operation;
	}
	
	private static ContentProviderOperation newUpdateContentProviderOperation(ContentValues values,String mimetype,int raw_contact_id) {
		
		String where = Data.MIMETYPE+"=? and "+Data.RAW_CONTACT_ID+"=?";
		String[] selectionArgs = new String[]{
				mimetype,raw_contact_id+""
		};
		
		ContentProviderOperation operation = ContentProviderOperation
				.newUpdate(ContactsContract.Data.CONTENT_URI)
				.withSelection(where, selectionArgs)
				.withValues(values).build();
		return operation;
	}

	public static void updateContact(Context context, long contract_id) {
		// ContentProviderOperation operation =
		// ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
		// .withSelection(selection, selectionArgs)
	}
	
	
//	 Data.MIMETYPE类型:
//		 * 
//		 * 1、Email.CONTENT_ITEM_TYPE 邮箱 --2、Im.CONTENT_ITEM_TYPE 聊天账号
//		 * 3、StructuredPostal.CONTENT_ITEM_TYPE 住址--4、Photo.CONTENT_ITEM_TYPE 图片
//		 * 5、Phone.CONTENT_ITEM_TYPE 电话号码--6、StructuredName.CONTENT_ITEM_TYPE 姓名
//		 * 7、Organization.CONTENT_ITEM_TYPE 公司+职位--8、Nickname.CONTENT_ITEM_TYPE 昵称
//		 * 9、GroupMembership.CONTENT_ITEM_TYPE 所属组--10、Note.CONTENT_ITEM_TYPE 备注
	
	public static ArrayList<GridViewItemImageView> allContact(Context context)
	{
		ArrayList<GridViewItemImageView> list = new ArrayList<GridViewItemImageView>();
		Cursor ids_contact = context.getContentResolver().query(raw_contact_uri, new String[]{"_id"}, null, null, null);
		int i = 0;
		while(ids_contact.moveToNext())
		{
			i++;
			int _id = ids_contact.getInt(0);
			String [] selection = new String[]{Data.MIMETYPE,Data.DATA1,Data.DATA15,Data._ID,Data.RAW_CONTACT_ID};
			String where = Data.RAW_CONTACT_ID+"=?";
			String[] args = new String[]{_id+""};
			Cursor cursor = context.getContentResolver().query(data_uri, selection, where, args, null);
			Contact contact = new Contact();
			Log.e("", "RAW_CONTACT_ID "+i+" == "+_id);
			while(cursor.moveToNext())
			{
				String minetype = cursor.getString(cursor.getColumnIndex(Data.MIMETYPE));
				int rawcontact_id = cursor.getInt(cursor.getColumnIndex(Data.RAW_CONTACT_ID));
				String data1 = cursor.getString(cursor.getColumnIndex(Data.DATA1));
				contact.setRawContact_id(rawcontact_id);
				if(minetype.equals(Nickname.CONTENT_ITEM_TYPE) || minetype.equals(StructuredName.CONTENT_ITEM_TYPE))//姓名
				{
					contact.setContact_name(data1);
				}else if(minetype.equals(Email.CONTENT_ITEM_TYPE))//邮箱
				{
					
				}else if(minetype.equals(StructuredPostal.CONTENT_ITEM_TYPE))//地址
				{
					
				}else if(minetype.equals(Photo.CONTENT_ITEM_TYPE))//头像(二进制流  blob格式 )
				{
					byte[] photo = cursor.getBlob(cursor.getColumnIndex(Data.DATA15));
					contact.setContact_head(photo);
				}else if(minetype.equals(Phone.CONTENT_ITEM_TYPE))//电话号码
				{
					data1 = data1.replace(" ", "");
					contact.setContact_phone(data1);
				}else if(minetype.equals(Organization.CONTENT_ITEM_TYPE))//公司+职位
				{
					
				}
			}
			list.add(contact);
		}
		
		return list;
	}
	
	public static ArrayList<GridViewItemImageView> allContact_(Context context)
	{
		ArrayList<GridViewItemImageView> list = new ArrayList<GridViewItemImageView>();
		String[] selection = new String[]{
				Phone._ID,Phone.DISPLAY_NAME,Phone.NUMBER,Phone.PHOTO_URI,Phone.RAW_CONTACT_ID
		};
		
		Cursor cursor = context.getContentResolver().query(Phone.CONTENT_URI, selection, null, null, null);
		while(cursor.moveToNext())
		{
			int contact_id = cursor.getInt(cursor.getColumnIndex(Phone._ID));
			String contact_phone = cursor.getString(cursor.getColumnIndex(Phone.NUMBER));
			String contact_name = cursor.getString(cursor.getColumnIndex(Phone.DISPLAY_NAME));
			
			String photo_uri = cursor.getString(cursor.getColumnIndex(Phone.PHOTO_URI));
			
			int rawContact_id = cursor.getInt(cursor.getColumnIndex(Phone.RAW_CONTACT_ID));
			Contact contact = new Contact(contact_id, contact_phone, contact_name, null, rawContact_id,photo_uri);
			list.add(contact);
		}
		cursor.close();
		return list;
	}
	
	public static void deleteContactByRawContact_id(Context context,int rawContact_id)
	{
		String where  = Data.RAW_CONTACT_ID+"=?";
		String[] selectionArgs = new String[]{rawContact_id+""};
		context.getContentResolver().delete(data_uri, where, selectionArgs);
	}
	
	public static void updateContact(Context context,Contact contact)
	{
		byte[] photo = contact.getContact_head();
		int _id = contact.getContact_id();
		String name = contact.getContact_name();
		String phone = contact.getContact_phone();
		int raw_id = contact.getRawContact_id();
		
		ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
		
		ContentValues values = new ContentValues();
		
		values.clear();
		values.put(Data.DATA1, name);
		operations.add(newUpdateContentProviderOperation(values,StructuredName.CONTENT_ITEM_TYPE,raw_id));
		
		values.clear();
		values.put(Data.DATA1, phone);
		operations.add(newUpdateContentProviderOperation(values,StructuredName.CONTENT_ITEM_TYPE,raw_id));
		
		values.clear();
		values.put(Data.DATA15, photo);
		operations.add(newUpdateContentProviderOperation(values,StructuredName.CONTENT_ITEM_TYPE,raw_id));
		
		// 在事务中对多个操作批量执行
		try {
			context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}
	}

}
