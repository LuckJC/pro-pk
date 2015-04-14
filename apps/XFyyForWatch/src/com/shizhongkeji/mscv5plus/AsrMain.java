package com.shizhongkeji.mscv5plus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.Media;
import android.provider.MediaStore.Images;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;
import com.shizhongkeji.speech.util.FucUtil;
import com.shizhongkeji.speech.util.JsonParser;
import com.shizhongkeji.speech.util.ProgressDialogUtils;

public class AsrMain extends Activity {

	private static String TAG = "AbnfDemo";
	/**
	 * 是不是更新完联系人
	 */
	private boolean is_updata_lexcion_finish = false;

	private TextToSpeech mSpeech = null;
	/**
	 * 是不是从别的地方返回来
	 */
	private boolean is_other_back = false;
	// 语音识别对象
	private SpeechRecognizer mAsr;
	private Toast mToast;
	// 缓存
	private SharedPreferences mSharedPreferences;
	// 本地语法文件
	private String mLocalGrammar = null;
	// add by lixd 构建多个文件
	List<String> mLocalGeammarList = new ArrayList<String>();
	// 本地词典
	private String mLocalLexicon = null;
	// 本地语法构建路径
	private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/msc_test";
	// 返回结果格式，支持：xml,json
	private String mResultType = "json";

	private final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
	private final String GRAMMAR_TYPE_BNF = "bnf";

	private String mEngineType = "local";


	HashMap<String, String> IDmap = new HashMap<String, String>();
	String[] items;// 记录人名数组
	// List<String> items = new ArrayList<String>();

	PhoneStateReceiver ps = new PhoneStateReceiver();
	private Handler handler = new Handler() {

		/** {@inheritDoc} */

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if (msg.what == 1) {
				// IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
				// "1100");
				mSpeech.speak(getResources().getString(R.string.no_person),
						TextToSpeech.QUEUE_FLUSH, null);
			}
		}

	};

	private class TTSListener implements OnInitListener {

		@Override
		public void onInit(int status) {
			// TODO Auto-generated method stub
			if (status == TextToSpeech.SUCCESS) {
				// int result = mSpeech.setLanguage(Locale.ENGLISH);
				int result = mSpeech.setLanguage(Locale.CHINA);
				// 如果打印为-2，说明不支持这种语言
				Toast.makeText(AsrMain.this, "-------------result = " + result, Toast.LENGTH_LONG)
						.show();
				if (result == TextToSpeech.LANG_MISSING_DATA
						|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
					System.out.println("-------------not use");
				} else {

				}
			}
		}

	}

	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// getActionBar().setDisplayHomeAsUpEnabled(true);
		Log.e("XF", "onCreate()");
		setContentView(R.layout.asrdemo);
		mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		// 初始化识别对象
		mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
		// 初始化语法、命令词

		mSpeech = new TextToSpeech(AsrMain.this, new TTSListener());
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
					ret = mAsr.startListening(mRecognizerListener);
					if (ret != ErrorCode.SUCCESS) {
						showTip("startListening error: " + ret);
					}
				}
			}
		});

		IntentFilter phonefilter = new IntentFilter();
		phonefilter.addAction("android.intent.action.PHONE_STATE");
		phonefilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
		registerReceiver(ps, phonefilter);
	}

	String mContent;// 语法、词典临时变量
	int ret = 0;// 函数调用返回值

	private void grammar() {
		mContent = new String(mLocalGrammar);

		mAsr.setParameter(SpeechConstant.PARAMS, null);
		// 设置文本编码格式
		mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
		// 设置引擎类型
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		// 设置语法构建路径
		mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
		// 使用8k音频的时候请解开注释
		// mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
		// 设置资源路径
		mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
		// 单个文件命令构建 add by lixd
		ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, grammarListener);
		if (ret != ErrorCode.SUCCESS) {
			showTip("grammar() error:" + ret);
		}
	}

	private void updataLexcion() {
		// mContent = new String(mLocalLexicon);
		if (mLocalLexicon == null) {
			showTip("no person");
			handler.sendEmptyMessage(1);
		}
		mAsr.setParameter(SpeechConstant.PARAMS, null);
		// 设置引擎类型
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
		// 设置资源路径
		mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
		// 使用8k音频的时候请解开注释
		// mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
		// 设置语法构建路径
		mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
		// 设置文本编码格式
		mAsr.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
		// 设置语法名称
		mAsr.setParameter(SpeechConstant.GRAMMAR_LIST, "xtml");

		ret = mAsr.updateLexicon("contact", mLocalLexicon, lexiconListener);
		if (ret != ErrorCode.SUCCESS) {
			showTip("updataLexcion() error:" + ret);
		}
	}

	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer  () code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("mInitListener error:" + code);
			}
			mLocalLexicon = "";

			mLocalGrammar = FucUtil.readFile(AsrMain.this, "xtml.bnf", "utf-8");
			if (!is_other_back) {
				grammar();
			}
		}
	};
	
	/**
	 * 更新词典监听器。
	 */
	private LexiconListener lexiconListener = new LexiconListener() {
		@Override
		public void onLexiconUpdated(String lexiconId, SpeechError error) {
			if (error == null) {
				showTip("LexiconUpdated ok");
			} else {
				showTip("LexiconUpdated error:" + error.getErrorCode());
			}

			ProgressDialogUtils.dismissProgressDialog();

			IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
			mSpeech.speak(getResources().getString(R.string.help_you_dothing),
					TextToSpeech.QUEUE_ADD, IDmap);
		}
	};

	/**
	 * 构建语法监听器。
	 */
	private GrammarListener grammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if (error == null) {
				showTip("grammarListener：" + grammarId);
			} else {
				showTip("grammarListener error:" + error.getErrorCode());
			}
			ProgressDialogUtils.showProgressDialog(AsrMain.this, "正在更新联系人");

			// 获取联系人，本地更新词典时使用
			ContactManager mgr = ContactManager.createManager(AsrMain.this, mContactListener);
			mgr.asyncQueryAllContactsName();
			mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		}
	};
	
	/**
	 * 获取联系人监听器。
	 */
	private ContactListener mContactListener = new ContactListener() {
		@Override
		public void onContactQueryFinish(String contactInfos, boolean changeFlag) {
			// 获取联系人
			mLocalLexicon = contactInfos;
			if (!is_updata_lexcion_finish) {
				updataLexcion();
				is_updata_lexcion_finish = true;
			}
		}
	};
	
	/**
	 * 识别监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onVolumeChanged(int volume) {
			showTip("VolumeChanged:" + volume);
		}
		@Override
		public void onResult(final RecognizerResult result, boolean isLast) {

			if (null != result && !TextUtils.isEmpty(result.getResultString())) {
				Log.d("XF", "recognizer result:" + result.getResultString());

				Map<String, List<String>> map = null;

				if (mResultType.equals("json")) {
					map = JsonParser
							.parseGrammarResultIntent(result.getResultString(), mEngineType);
					if (map == null) {
						showTip("again speak");
						mAsr.setParameter(SpeechConstant.PARAMS, null);
						// 设置识别引擎
						mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);

						mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
						// 设置语法构建路径
						mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
						// 设置返回结果格式
						mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
						// 设置本地识别使用语法id
						mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "xtml");
						// 设置识别的门限值
						mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
						// 使用8k音频的时候请解开注释
						// mAsr.setParameter(SpeechConstant.SAMPLE_RATE,
						// "8000");
						mAsr.setParameter(SpeechConstant.VAD_BOS, "3000");
						ret = mAsr.startListening(mRecognizerListener);

						if (ret != ErrorCode.SUCCESS) {
							showTip("startListening error:" + ret);
						}
						return;
					}
					Set<String> set = map.keySet();

					String number = null;

					for (String keyset : set) {
						if (keyset.equals("callPhone")) {
							if (map.get("callPhone").size() >= 1) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1002");
								mSpeech.speak(getResources().getString(R.string.call_phone)
										+ map.get("callPhone").get(0), TextToSpeech.QUEUE_ADD,
										IDmap);
								number = FindPhoneNumber(map.get("callPhone").get(0));
								// 拨号
								Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
										+ number));

								startActivity(intent);
								mAsr.stopListening();
								// isother_back=true;
							}

						} else if (keyset.equals("sendmsgPhone")) {

							if (map.get("sendmsgPhone").size() >= 1) {

								number = FindPhoneNumber(map.get("sendmsgPhone").get(0));
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1003");
								mSpeech.speak(
										getResources().getString(R.string.sen_msg)
												+ map.get("sendmsgPhone").get(0),
										TextToSpeech.QUEUE_ADD, IDmap);
								Intent intent = new Intent();
								// 系统默认的action，用来打开默认的短信界面
								intent.setAction(Intent.ACTION_SENDTO);
								// 需要发短息的号码
								intent.setData(Uri.parse("smsto:" + number));
								startActivity(intent);
								mAsr.stopListening();
								is_other_back = true;
							}
						}
						// else 的是 keyset为openApp
						else {
							if ((getResources().getString(R.string.music)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1004");
								mSpeech.speak(getResources().getString(R.string.music_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								Intent intent = new Intent(Intent.ACTION_MAIN);
								intent.addCategory(Intent.CATEGORY_APP_MUSIC);
								startActivity(intent);
								mAsr.stopListening();
								is_other_back = true;
							} else if ((getResources().getString(R.string.settings)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1005");
								mSpeech.speak(
										getResources().getString(R.string.settings_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								Intent mIntent = new Intent();
								mIntent.setAction(Settings.ACTION_SETTINGS);
								startActivity(mIntent);
								mAsr.stopListening();
								is_other_back = true;

							} else if ((getResources().getString(R.string.camera)).equals(map.get(
									"openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1006");
								mSpeech.speak(
										getResources().getString(R.string.camera_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
								startActivity(camera);
								mAsr.stopListening();
								is_other_back = true;
							} else if ((getResources().getString(R.string.picture)).equals(map.get(
									"openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1007");
								mSpeech.speak(getResources()
										.getString(R.string.picture_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								Uri uri = Images.Media.INTERNAL_CONTENT_URI;
								Intent intent = new Intent(Intent.ACTION_VIEW, uri);
								startActivity(intent);
								mAsr.stopListening();
								is_other_back = true;
							} else if ((getResources().getString(R.string.call_dial)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1008");
								mSpeech.speak(
										getResources().getString(R.string.call_dial_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								Intent intent = new Intent(Intent.ACTION_DIAL);
								startActivity(intent);
								mAsr.stopListening();
								is_other_back = true;
							} else if ((getResources().getString(R.string.recorder)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1009");
								mSpeech.speak(
										getResources().getString(R.string.recorder_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);

								Intent mi = new Intent(Media.RECORD_SOUND_ACTION);
								startActivity(mi);
								mAsr.stopListening();
								is_other_back = true;
							}
						}
					}

				}

			}

			else {
				mSpeech.speak(getResources().getString(R.string.no_thing),
						TextToSpeech.QUEUE_FLUSH, null);
				Log.d(TAG, "recognizer result : null");
			}

		}
		
		// 根据名字查找手机号码 add by lixd
		private String FindPhoneNumber(String name) {
			ContentResolver contentResolver = AsrMain.this.getContentResolver();
			Cursor cursor = contentResolver.query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
					ContactsContract.PhoneLookup.DISPLAY_NAME + "='" + name + "'", null, null);

			while (cursor.moveToNext()) {
				return cursor.getString(0);
			}
			return "";
		}

		@Override
		public void onEndOfSpeech() {

			showTip("end talk");
			Log.e("XF", "onEndOfSpeech()");
		}

		@Override
		public void onBeginOfSpeech() {
			showTip("start talk");
			Log.e("XF", "onBeginOfSpeech()");
		}

		@Override
		public void onError(SpeechError error) {
			mAsr.stopListening();
			if(error.getErrorCode()==20005)
			{
				showTip(getResources().getString(R.string.no_result_show));				
			}
			else if(error.getErrorCode()==23300)
			{
				showTip(getResources().getString(R.string.again_grammar));
			}
			else if(error.getErrorCode()==23108)
			{
				showTip(getResources().getString(R.string.again_updataLexcion));
			}
			else
			{
				showTip("onError Code:" + error.getErrorCode());
			}
			mAsr.setParameter(SpeechConstant.PARAMS, null);
			// 设置识别引擎
			mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);

			mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
			// 设置语法构建路径
			mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
			// 设置返回结果格式
			mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
			// 设置本地识别使用语法id
			mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "xtml");
			// 设置识别的门限值
			mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
			// 使用8k音频的时候请解开注释
			// mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
			mAsr.setParameter(SpeechConstant.VAD_BOS, "3000");
			ret = mAsr.startListening(mRecognizerListener);

			if (ret != ErrorCode.SUCCESS) {
				showTip("error code:" + ret);
			}

		}
		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			Log.e("XF", "onEvent()");
		}
	};

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public boolean setParam() {
		boolean result = false;
		// 清空参数
		mAsr.setParameter(SpeechConstant.PARAMS, null);
		// 设置识别引擎
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
		if ("cloud".equalsIgnoreCase(mEngineType)) {
			String grammarId = mSharedPreferences.getString(KEY_GRAMMAR_ABNF_ID, null);
			if (TextUtils.isEmpty(grammarId)) {
				result = false;
			} else {
				// 设置返回结果格式
				mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
				// 设置云端识别使用的语法id
				mAsr.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarId);
				result = true;
			}
		} else {
			// 设置本地识别资源
			mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
			// 设置语法构建路径
			mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
			// 设置返回结果格式
			mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
			// 设置本地识别使用语法id
			mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "xtml");
			// 设置识别的门限值
			mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
			// 使用8k音频的时候请解开注释
			// mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
			result = true;
		}
		return result;
	}

	// 获取识别资源路径
	private String getResourcePath() {
		StringBuffer tempBuffer = new StringBuffer();
		// 识别通用资源
		tempBuffer.append(ResourceUtil.generateResourcePath(this, RESOURCE_TYPE.assets,
				"asr/common.jet"));
		// 识别8k资源-使用8k的时候请解开注释
		// tempBuffer.append(";");
		// tempBuffer.append(ResourceUtil.generateResourcePath(this,
		// RESOURCE_TYPE.assets, "asr/common_8k.jet"));
		return tempBuffer.toString();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出时释放连接
		mAsr.cancel();
		mAsr.destroy();
		mAsr.stopListening();

		mSpeech.stop();
		mSpeech.shutdown();
		mSpeech = null;
		
		unregisterReceiver(ps);
		
	}

	/** {@inheritDoc} */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (is_other_back) {

			IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
			mSpeech.speak(getResources().getString(R.string.help_you_dothing),
					TextToSpeech.QUEUE_ADD, IDmap);
		}

	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mAsr.cancel();
		mAsr.destroy();
		mAsr.stopListening();
		
	}

	public class PhoneStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			
			// 如果是拨打电话
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				mAsr.cancel();
				mAsr.stopListening();
				mSpeech.stop();
			} else {
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				switch (tm.getCallState()) {
				case TelephonyManager.CALL_STATE_RINGING:
					mAsr.cancel();
					mAsr.stopListening();
					mAsr.destroy();
					mSpeech.stop();
					break;
				case TelephonyManager.CALL_STATE_IDLE:
					is_other_back = true;

					mAsr.cancel();
					mAsr.stopListening();
					mAsr.destroy();
					mSpeech.stop();
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
					mAsr.cancel();
					mAsr.stopListening();
					mAsr.destroy();
					mSpeech.stop();
					break;
				}

			}
		}

	}
}
