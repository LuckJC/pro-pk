package com.example.xuntongwatch.util;

import android.app.ProgressDialog;
import android.content.Context;

public class CommonDialog {
	private static ProgressDialog progressDialog;
	public static void showDialog(Context context){
		progressDialog =new ProgressDialog(context);
		progressDialog.setMessage("请稍后...");
		progressDialog.setCancelable(false);
		progressDialog.show();
	}
	public  static void  closeDialog(){
		progressDialog.dismiss();
		
	}
}
