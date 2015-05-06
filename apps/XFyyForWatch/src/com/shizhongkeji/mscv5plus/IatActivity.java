package com.shizhongkeji.mscv5plus;

import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.shizhongkeji.speech.util.JsonParser;

public class IatActivity extends Activity implements OnClickListener{
	private static String TAG = "IatDemo";
	// 语音听写对象
	private SpeechRecognizer mIat;
	// 语音听写UI
	private RecognizerDialog iatDialog;
	// 听写结果内容
	private EditText mResultText;
	
	private Toast mToast;

	String name;
	String number;
	
	TextToSpeech mSpeech;
	private SharedPreferences mSharedPreferences;
	 
	private class TTSListener implements OnInitListener{

		@Override
		public void onInit(int status) {
			// TODO Auto-generated method stub
			if (status == TextToSpeech.SUCCESS) {
				// int result = mSpeech.setLanguage(Locale.ENGLISH);
				int result = mSpeech.setLanguage(Locale.CHINA);
				// 如果打印为-2，说明不支持这种语言
				if (result == -2) {
					Log.d("lixianda", "" + result);
					IatActivity.this.finish();
				}
				if (result == TextToSpeech.LANG_MISSING_DATA
						|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
					System.out.println("-------------not use");
				} else {
					// 初始化陈功而且支持当前语音后做什么
				}
			} else {
				Log.e("lixianda", "TTS init error");
				IatActivity.this.finish();
			}
		}
	}
	
	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.iat_activity);
		initLayout();

		// 初始化识别对象
		mIat = SpeechRecognizer.createRecognizer(this, mInitListener);
		// 初始化听写Dialog,如果只使用有UI听写功能,无需创建SpeechRecognizer
		iatDialog = new RecognizerDialog(this,mInitListener);
		
		mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME, Activity.MODE_PRIVATE);
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);	
		mResultText = ((EditText)findViewById(R.id.iat_text));
		TextView tv = ((TextView)findViewById(R.id.iat_name_phonenumber));
		
		Intent it=this.getIntent();
		name = it.getStringExtra("Name");
		number = it.getStringExtra("PhoneNumber");
		
		
		tv.setText(name+"    "+number);
		
		mSpeech = new TextToSpeech(IatActivity.this, new TTSListener(),"com.iflytek.speechcloud");
		mSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {

			@Override
			public void onStart(String utteranceId) {
			}

			@Override
			public void onError(String utteranceId) {
			}

			@Override
			public void onDone(String utteranceId) {
				if (utteranceId.equals("1001")) {
					
				}
			}
		});
	}

	/**
	 * 初始化Layout。
	 */
	private void initLayout(){
		
		Button btn = (Button)findViewById(R.id.iat_recognize);
		btn.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					mResultText.setText(mResultText.getText().toString());// 清空显示内容
					// 设置参数
					setParam();
					boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
					if (isShowDialog) {
						// 显示听写对话框
						iatDialog.setListener(recognizerDialogListener);
						iatDialog.show();
						showTip(getString(R.string.text_begin));
					} else {
						// 不显示听写对话框
						ret = mIat.startListening(recognizerListener);
						if(ret != ErrorCode.SUCCESS){
							showTip("听写失败,错误码：" + ret);
						}else {
							showTip(getString(R.string.text_begin));
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					mIat.stopListening();
					break;}
				return true;
			}
		});
		findViewById(R.id.iat_sendsms).setOnClickListener(this);
		findViewById(R.id.image_iat_set).setOnClickListener(this);
	}

	int ret = 0;// 函数调用返回值
	@Override
	public void onClick(View view) {				
		switch (view.getId()) {
		// 进入参数设置页面
		case R.id.image_iat_set:
			Intent intents = new Intent(IatActivity.this, IatSettings.class);
			startActivity(intents);
			break;
			// 开始听写
		case R.id.iat_recognize:
			mResultText.setText(mResultText.getText().toString());// 清空显示内容
			// 设置参数
			setParam();
			boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
			if (isShowDialog) {
				// 显示听写对话框
				iatDialog.setListener(recognizerDialogListener);
				iatDialog.show();
				showTip(getString(R.string.text_begin));
			} else { 
				// 不显示听写对话框
				ret = mIat.startListening(recognizerListener);
				if(ret != ErrorCode.SUCCESS){
					showTip("听写失败,错误码：" + ret);
				}else {
					showTip(getString(R.string.text_begin));
				}
			}
			break;
			// 停止听写
		case R.id.iat_sendsms: 

			SmsManager smsManager = SmsManager.getDefault();  
			smsManager.sendTextMessage(number, null,mResultText.getText().toString(), null, null);
			mSpeech.speak("短信已经发送给" + name, TextToSpeech.QUEUE_FLUSH, null);		
			break;
		
		default:
			break;
		}
	}


	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
        		showTip("初始化失败,错误码："+code);
        	}
		}
	};
	
	/**
	 * 听写监听器。
	 */
	private RecognizerListener recognizerListener=new RecognizerListener(){

		@Override
		public void onBeginOfSpeech() {	
			showTip(getResources().getString(R.string.start_talk));
		}
		
		@Override
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			showTip(getResources().getString(R.string.end_talk));
		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {		
			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());
			if(isLast) {
				//TODO 最后的结果
			}
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip(getResources().getString(R.string.volume_changed) + volume);
		}


		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			
		}
	};

	/**
	 * 听写UI监听器
	 */
	private RecognizerDialogListener recognizerDialogListener=new RecognizerDialogListener(){
		public void onResult(RecognizerResult result, boolean isLast) {
			Log.d(TAG, "recognizer result：" + result.getResultString());
			String text = JsonParser.parseIatResult(result.getResultString());
			
			if(text.equals("返回"))
			{
				IatActivity.this.finish();
			}
			
			else{
				mResultText.append(text);
				mResultText.setSelection(mResultText.length());
				
				mResultText.setText(mResultText.getText().toString());// 清空显示内容
				}
		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {
			showTip(error.getPlainDescription(true));
		}

	};

	private void showTip(final String str)
	{
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mToast.setText(str);
				mToast.show();
			}
		});
	}

	/**
	 * 参数设置
	 * @param param
	 * @return 
	 */
	public void setParam(){
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);
		String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
		// 设置引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);

		if (lag.equals("en_us")) {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
		}else {
			// 设置语言
			mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mIat.setParameter(SpeechConstant.ACCENT,lag);
		}

		// 设置语音前端点
		mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
		// 设置语音后端点
		mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
		// 设置标点符号
		mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));
		// 设置音频保存路径
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/iflytek/wavaudio.pcm");
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出时释放连接
		mIat.cancel();
		mIat.destroy();
		if(mSpeech!=null)
		{
			mSpeech.shutdown();
			}
	}
}
