package com.szkj.watchcamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.PictureCallback;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

public class MainActivity extends Activity implements SurfaceHolder.Callback, OnClickListener {
	private static final String TAG = "CameraDemo";
	private Camera mCamera;
	private Camera.Parameters parameters;
	private Button btn;
	private SurfaceView surface;
	SurfaceHolder holder;
	Spinner spinner_focus;
	Spinner spinner_size;

	boolean is_open_settting = false;// 打开相机设置 默认为 关

	View ve;
	View bottomView;
	String[] str_spinner_focus = { "自动", "无限远", "连续", "手动", "微距" };
	String[] str_spinner_definition = { "高质量", "中质量", "低质量" };
	private ArrayAdapter<String> focusAdapter;
	private ArrayAdapter<String> photosizeAdapter;

	DrawCaptureRect mDraw;// 画矩形框
	private FrameLayout faceView;

	int viewWidth;// faceView的宽高 也是屏幕的宽高
	int viewHeight;// faceView的宽高 也是屏幕的宽高

	boolean is_findface = false;// 是否打开人脸检测 默认为 关
	boolean is_continue_take = false;// 是否打开连拍 默认为 关
	boolean is_focus_manual = false;// 是否是手动聚焦

	int count = 0;

	String str_focusmode;// 聚焦模式
	String str_picture_definition;// 图片质量
	Handler hand = new Handler() {

		/** {@inheritDoc} */

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if (msg.what == 101) {// 拍照
				mCamera.takePicture(null, null, mPicture);

			}
			if (msg.what == 102) {// 人脸检测
				mCamera.startPreview();
				mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
				mCamera.startFaceDetection();
				try {
					mCamera.setPreviewDisplay(holder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			super.handleMessage(msg);
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_main);

		viewWidth = this.getWindowManager().getDefaultDisplay().getWidth();
		viewHeight = this.getWindowManager().getDefaultDisplay().getHeight();

		mCamera = getCameraInstance();
		if(mCamera == null)
		{
			Toast.makeText(MainActivity.this, "没有相机", Toast.LENGTH_SHORT).show();
			finish();
			return;
		}
		parameters = mCamera.getParameters();

		faceView = (FrameLayout) findViewById(R.id.face_rect);

		surface = (SurfaceView) findViewById(R.id.surface);
		surface.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub

				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					mDraw = new DrawCaptureRect(MainActivity.this, new Rect((int) event.getX() - 30, (int) event.getY() - 30, (int) event.getX() + 30, (int) event.getY() + 30), Color.GREEN);
					if (faceView.getChildCount() >= 1) {
						faceView.removeAllViews();
						faceView.invalidate();
					}
					faceView.addView(mDraw);
					return true;
					}
				if (event.getAction() == MotionEvent.ACTION_UP) {
					if (is_open_settting) {
						return false;
					} else if (!is_focus_manual) {
						return false;
					} else {
						if (mCamera != null) {

							if (parameters.getMaxNumFocusAreas() > 0) {

								List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

								int x_left = Float.valueOf((event.getX() / viewWidth) * 2000 - 1000).intValue();
								int x_top = Float.valueOf((event.getY() / viewWidth) * 2000 - 1000).intValue();

								Rect areaRect1 = new Rect(x_left - 30, x_top - 30, x_left + 30,x_top + 30);
								meteringAreas.add(new Camera.Area(areaRect1, 700));
								parameters.setMeteringAreas(meteringAreas);
								try {
									mCamera.setParameters(parameters);

									mCamera.setPreviewDisplay(holder);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								mCamera.startPreview();

							}

						}
						mCamera.autoFocus(new AutoFocusCallback() {
							@Override
							public void onAutoFocus(boolean success, Camera camera) {
								faceView.removeAllViews();
							}
						});
					}
					return true;
				}else
				{return false;}
				
			}
		});

		findViewById(R.id.common_setting).setOnClickListener(this);
		findViewById(R.id.to_picture).setOnClickListener(this);
		findViewById(R.id.change_mode).setOnClickListener(this);
		findViewById(com.szkj.watchcamera.R.id.btn).setOnClickListener(this);
		ve = (View) MainActivity.this.findViewById(R.id.setting_view);
		bottomView = (View) MainActivity.this.findViewById(R.id.bottom_layout);

		spinner_focus = (Spinner) ve.findViewById(R.id.focus_spinner);
		spinner_size = (Spinner) ve.findViewById(R.id.photosize_spinner);

		focusAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.item_spinner_focus,
				str_spinner_focus);
		photosizeAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.item_spinner_focus,
				str_spinner_definition);

		spinner_focus.setAdapter(focusAdapter);
		spinner_focus.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				mCamera.stopPreview();
				parameters = mCamera.getParameters();
				// 对焦模式
				str_focusmode = parent.getItemAtPosition(position).toString();
				if (str_focusmode.equals(str_spinner_focus[0])) {
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					is_focus_manual = false;
				} else if (str_focusmode.equals(str_spinner_focus[1])) {
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
					is_focus_manual = false;
				} else if (str_focusmode.equals(str_spinner_focus[2])) {
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
					mCamera.cancelAutoFocus();
					is_focus_manual = false;
				} else if (str_focusmode.equals(str_spinner_focus[3])) {
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
					is_focus_manual = true;
				} else {
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
					is_focus_manual = false;
				}

				parameters.getSupportedFocusModes();
				mCamera.setParameters(parameters);

				try {
					mCamera.setPreviewDisplay(holder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mCamera.startPreview();

				if (is_findface) {
					mCamera.stopPreview();
					hand.sendEmptyMessage(102);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}
		});

		spinner_size.setAdapter(photosizeAdapter);
		spinner_size.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				mCamera.stopPreview();
				parameters = mCamera.getParameters();
				// 设置图片质量
				str_picture_definition = parent.getItemAtPosition(position).toString();

				if (str_picture_definition.equals(str_spinner_definition[0])) {
					parameters.setJpegQuality(100);
				} else if (str_picture_definition.equals(str_spinner_definition[1])) {
					parameters.setJpegQuality(50);
				} else {
					parameters.setJpegQuality(1);
				}

				mCamera.setParameters(parameters);

				try {
					mCamera.setPreviewDisplay(holder);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mCamera.startPreview();

				if (is_findface) {
					mCamera.stopPreview();
					hand.sendEmptyMessage(102);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO Auto-generated method stub

			}
		});

		((Switch) ve.findViewById(R.id.find_face))
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if (isChecked) {
							is_findface = true;
							mCamera.stopPreview();
							hand.sendEmptyMessage(102);// 人脸检测
						} else {
							is_findface = false;
							mCamera.setFaceDetectionListener(null);
							faceView.removeAllViews();
							faceView.invalidate();
						}

						mCamera.setParameters(parameters);

						try {
							mCamera.setPreviewDisplay(holder);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						mCamera.startPreview();

					}
				});

		((Switch) ve.findViewById(R.id.continu_take))
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						// TODO Auto-generated method stub
						if (isChecked) {
							is_continue_take = true;
						} else {
							is_continue_take = false;
						}
					}
				});

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.btn:

			if (is_continue_take) {
				hand.sendEmptyMessage(101);
			} else {
				mCamera.takePicture(null, null, mPicture);
			}

			break;
		case R.id.common_setting:
			if (!is_open_settting) {
				ve.setVisibility(View.VISIBLE);
				bottomView.setVisibility(View.GONE);
				is_open_settting = true;
			} else {
				ve.setVisibility(View.GONE);
				bottomView.setVisibility(View.VISIBLE);
				is_open_settting = false;
			}
			break;
		case R.id.to_picture:
			
			Intent intent_one = new Intent();
			intent_one.setClassName("com.android.watchgallery","com.android.watchgallery.MainActivity");
			intent_one.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try{startActivity(intent_one);}
			catch(Exception e)
			{Toast.makeText(MainActivity.this, "没找到应用", Toast.LENGTH_SHORT).show();}
			
			mCamera.setFaceDetectionListener(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			break;
		case R.id.change_mode:
			
			Intent intent_two = new Intent();
			intent_two.setClassName("com.shizhongkeji.videoplayer","com.shizhongkeji.videoplayer.VideoChooseActivity");
			intent_two.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			try{startActivity(intent_two);}
			catch(Exception e)
			{Toast.makeText(MainActivity.this, "没找到应用", Toast.LENGTH_SHORT).show();}
			
			mCamera.setFaceDetectionListener(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			break;

		default:
			break;
		}
	}

	// 获取相机Camera
	public Camera getCameraInstance() {
		Camera c = null;
		c = Camera.open();
		if(c == null)
		{
			Toast.makeText(MainActivity.this, "没有相机", Toast.LENGTH_SHORT).show();
			finish();
		}
		return c;
	}

	private PictureCallback mPicture = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			new SavePictureTask().execute(data);
			mCamera.startPreview();

			if (is_continue_take) {
				hand.sendEmptyMessage(101);
				count++;
				if (count >= 3) {
					hand.removeMessages(101);
				}
			}

			if (is_findface) {
				mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
				mCamera.startFaceDetection();
			}
		}

	};

	// 异步保存拍照的图片
	public class SavePictureTask extends AsyncTask<byte[], String, String> {
		MediaScannerConnection msc;
		SimpleDateFormat date = new SimpleDateFormat("yyyyMMdd_HHmmss");
		String s = date.format(System.currentTimeMillis());

		@Override
		protected String doInBackground(byte[]... params) {
			File pictureFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
					+ "/DCIM/MyCamera/img_" + s + ".jpg");
			if (!pictureFile.getParentFile().exists()) {
				pictureFile.getParentFile().mkdir();
			}
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(params[0]);
				fos.flush();
				fos.close();

				Toast.makeText(MainActivity.this, "图片保存OK", Toast.LENGTH_SHORT).show();
				Uri uri = Uri.parse("file://" + pictureFile.getAbsolutePath());
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));//通知系统来扫描文件

			} catch (Exception e) {
				Log.d(TAG, "File not found: " + e.getMessage());
			}
			return null;
		}

	}

	@Override
	protected void onDestroy() {

		if (mCamera != null) {
			mCamera.setFaceDetectionListener(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}

		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		holder = surface.getHolder();
		holder.addCallback(this);

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {

		try {
			
			if (mCamera == null) {
				mCamera = getCameraInstance();
			}
			mCamera.stopFaceDetection();
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
			mCamera.startFaceDetection();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		try {
			
			if (mCamera == null) {
				mCamera = getCameraInstance();
			}
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
			setDisplayOrientation(mCamera, 90);
			mCamera.setParameters(parameters);
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();

			mDraw = new DrawCaptureRect(MainActivity.this, new Rect(viewWidth / 2 - 30,
					viewHeight / 2 - 30, viewWidth / 2 + 30, viewHeight / 2 + 30), Color.GREEN);

			mCamera.autoFocus(new AutoFocusCallback() {

				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					// TODO Auto-generated method stub
					faceView.removeAllViews();

				}
			});
			if(is_findface)
			{
				mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
				mCamera.startFaceDetection();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

		return;
	}

	// 相机硬件调整角度
	protected void setDisplayOrientation(Camera camera, int angle) {
		Method downPolymorphic;
		try {
			downPolymorphic = camera.getClass().getMethod("setDisplayOrientation",
					new Class[] { int.class });
			if (downPolymorphic != null)
				downPolymorphic.invoke(camera, new Object[] { angle });
		} catch (Exception e1) {
		}
	}

	/** {@inheritDoc} */

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub

		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (is_open_settting) {
				ve.setVisibility(View.GONE);
				bottomView.setVisibility(View.VISIBLE);
				is_open_settting = false;
				return true;
			}

		}

		return super.onKeyDown(keyCode, event);

	}

	/**
	 * 
	 * <br>
	 * 类描述: <br>
	 * 功能详细描述:聚焦框 以及人脸框
	 * 
	 * @author lenovo
	 * @date [2015-5-28]
	 */
	class DrawCaptureRect extends View {
		private int mcolorfill;
		private int mleft, mtop, mwidth, mheight;

		public DrawCaptureRect(Context context, Rect rt, int colorfill) {
			super(context);
			// TODO Auto-generated constructor stub
			this.mcolorfill = colorfill;
			this.mleft = rt.left;
			this.mtop = rt.top;
			this.mwidth = rt.right;
			this.mheight = rt.bottom;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			// TODO Auto-generated method stub
			Paint mpaint = new Paint();
			mpaint.setColor(mcolorfill);
			mpaint.setStyle(Paint.Style.STROKE);
			mpaint.setStrokeWidth(1.0f);

			canvas.drawRect(new Rect(mleft, mtop, mwidth, mheight), mpaint);
			super.onDraw(canvas);
		}

	}

	/**
	 * 人脸检测
	 */
	class MyFaceDetectionListener implements Camera.FaceDetectionListener {

		@Override
		public void onFaceDetection(Face[] faces, Camera camera) {
//			parameters = mCamera.getParameters();
			if (faces.length > 0) {
				Matrix mMatrix = new Matrix();
				mMatrix.setScale(1, 1); // 只有后置摄像头如果有前置摄像头
										// mMatrix.setScale(isMirror?-1:1, 1);
										// isMirror判断是前置还是后置摄像头
				// This is the value for
				// android.hardware.Camera.setDisplayOrientation.
				mMatrix.postRotate(90);
				// Camera driver coordinates range from (-1000, -1000) to (1000,
				// 1000).
				// UI coordinates range from (0, 0) to (width, height).
				mMatrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
				mMatrix.postTranslate(viewWidth / 2f, viewHeight / 2f);

				RectF re = new RectF();
				if (faceView.getChildCount() <= 1) {
					for (int i = 0; i < faces.length; i++) {

						re.set(faces[i].rect);
						mMatrix.mapRect(re);

						mDraw = new DrawCaptureRect(MainActivity.this, new Rect(
								Math.round(re.left), Math.round(re.top), Math.round(re.right),
								Math.round(re.bottom)), Color.GREEN);

						faceView.addView(mDraw);

//						List<Camera.Area> focusArea = new ArrayList<Camera.Area>();
//						Rect focus_rect = new Rect(Math.round(re.left), Math.round(re.top),
//								Math.round(re.right), Math.round(re.bottom));
//						focusArea.add(new Camera.Area(focus_rect, 800));
//						parameters.setFocusAreas(focusArea);
//
//						mCamera.setParameters(parameters);
//
//						try {
//							mCamera.setPreviewDisplay(holder);
//						} catch (IOException e) {
//							// TODO Auto-generated catch block
//							e.printStackTrace();
//						}
//						mCamera.startPreview();
						
						 Log.d("lixianda", faces[i].score+";");
					}
				} else {
					faceView.removeAllViews();

				}
				faceView.invalidate();

			} else {
				faceView.removeAllViews();
			}

		}

	}

	
	
}
