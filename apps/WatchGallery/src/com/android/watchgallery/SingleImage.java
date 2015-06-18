package com.android.watchgallery;

import java.io.File;

import com.android.watchgallery.R;



import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class SingleImage extends Activity{
    ImageView delete;
    ImageView single;
    String y;
    int position;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.singleimage);
		delete=(ImageView) this.findViewById(R.id.delete);
		single=(ImageView) this.findViewById(R.id.single);
		Bundle exBundle=getIntent().getExtras();
		y=exBundle.getString("singleImage");
		position=exBundle.getInt("position");
		single.setImageURI(Uri.parse(y));
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(MainActivity.flag==0){
					MainActivity.imgList.remove(MainActivity.imgList.get(position));
					File f=new File(MainActivity.listImgPath.get(position));
//					MainActivity.listImgPath.remove(position);
					boolean h=f.delete();
					Intent intent = new Intent();
					intent.putExtra("position", position);
					SingleImage.this.setResult(RESULT_OK, intent);
					SingleImage.this.finish();
				}
				if(MainActivity.flag==1){
					File f=new File(MainActivity.lsmap.get(position));
					f.delete();
					MainActivity.pictureList.remove(position);
					Intent intent = new Intent();
					SingleImage.this.setResult(RESULT_OK, intent);
					SingleImage.this.finish();
				}
			}
		});
	}
  
}
