package com.example.xuntongwatch.data;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import com.example.xuntongwatch.entity.TelephoneAdress;

public class DbUtil {
	private MySQLiteDataBase mdb;
	private SQLiteDatabase sdb;

	public DbUtil(Context context) {
		this.mdb = new MySQLiteDataBase(context);
	}

	public void open() {
		sdb = mdb.getWritableDatabase();
	}

	public void close(Cursor cursor)

	{
		if (cursor != null) {
			cursor.close();
		}
		sdb.close();
	}

	public void insertInto(TelephoneAdress entity) {
		open();
		String sql = "insert into telephone (telephone,areaCode,city,cardType) values (?,?,?,?)";
		Object[] obj = new Object[] { entity.getTelephone(),
				entity.getAreaCode(), entity.getCity(), entity.getCardType() };
		sdb.execSQL(sql, obj);
		close(null);
	}

	public void insetIntoTwo(ArrayList<TelephoneAdress> entitys) {
		open();
		String sql = "insert into telephone (telephone,areaCode,city,cardType) values (?,?,?,?)";
		SQLiteStatement stat = sdb.compileStatement(sql);
		sdb.beginTransaction();
		for (TelephoneAdress telephoneAdress : entitys) {
			stat.bindString(1, telephoneAdress.getTelephone());
			stat.bindString(2, telephoneAdress.getAreaCode());
			stat.bindString(3, telephoneAdress.getCity());
			stat.bindString(4, telephoneAdress.getCardType());
			stat.executeInsert();
		}
		sdb.setTransactionSuccessful();
		sdb.endTransaction();
		sdb.close();
	}

	public void findAll() {
		open();
		String sql = "select * from telephone";
		Cursor cursor = sdb.rawQuery(sql, null);
		while (cursor.moveToNext()) {
			String telephone = cursor.getString(1);
			String areaCode = cursor.getString(2);
			String city = cursor.getString(3);
			String cardType = cursor.getString(4);
		}
		close(cursor);
	}

	public String[] findByPhone(String phone) {
		String[] str = new String[] { "", "" };
		String where = "";
		String phone1 = null;
		if (phone.startsWith("1")) {// 手机号码
			if (phone.length() < 7)
				return str;
			where = "telephone";
			phone = phone.substring(0, 7);
		} else if (phone.startsWith("0")) {// 电话号码
			where = "areaCode";
			phone1 = phone.substring(0, 4);
			phone = phone.substring(0, 3);
		}
		if (TextUtils.isEmpty(where)) {
			return str;
		}
		open();
		String sql = "select * from telephone where " + where + "='" + phone
				+ "'";
		if (!TextUtils.isEmpty(phone1)) {
			sql = "select * from telephone where " + where + " in (" + phone
					+ "," + phone1 + ")";
		}
		Cursor cursor = sdb.rawQuery(sql, null);
		if (cursor.moveToNext()) {
			String city = cursor.getString(3);
			String cardtype = cursor.getString(4);
			city = city.replace("省", "");
			city = city.replace("市", "");
			String type = "";
			if (cardtype.contains("电信")) {
				type = "电信";
			} else if (cardtype.contains("联通")) {
				type = "联通";
			} else if (cardtype.contains("移动")) {
				type = "移动";
			}
			str[0] = city;
			str[1] = type;
		}
		close(cursor);
		return str;
	}

	public StringBuffer addSql(String where, String phone) {
		StringBuffer sb = new StringBuffer("select * from telephone where ");
		return sb;
	}

}
