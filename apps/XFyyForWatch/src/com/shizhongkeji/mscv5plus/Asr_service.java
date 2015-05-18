package com.shizhongkeji.mscv5plus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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
import com.shizhongkeji.speech.util.MyDialog;
import com.shizhongkeji.speech.util.ProgressDialogUtils;

public class Asr_service extends Service {

	private static int TAG = 555;
	/**
	 * 是不是更新完联系人标志
	 */
	private boolean is_updata_lexcion_finish = false;

	private TextToSpeech mSpeech = null;
	/**
	 * 是不是从别的地方返回来标志
	 */
//	private boolean is_other_back = false;
	/**
	 * 是否有弹框标志
	 */
//	private boolean is_have_dialog = false;
	/**
	 * 控制打电话过程中的标志
	 */
//	private boolean is_contrl_call = false;
	/**
	 * 发现暂停标志
	 */
//	private boolean is_found_pause = false; 
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
	String name = null;//联系人名字 
	String number = null;//电话号码

	PhoneStateReceiver ps = new PhoneStateReceiver();
	private Handler handler = new Handler() {

		/** {@inheritDoc} */

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			if (msg.what == 1) {
				mSpeech.speak(getResources().getString(R.string.no_person),
						TextToSpeech.QUEUE_FLUSH, null);
			}
			if(msg.what == Asr_service.TAG)
			{
				IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
				mSpeech.speak(getResources().getString(R.string.help_you_dothing),
						TextToSpeech.QUEUE_ADD, IDmap);
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
				if (result == -2) {
					Log.d("lixianda", "" + result);
					stopSelf();
				}
				if (result == TextToSpeech.LANG_MISSING_DATA
						|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
					System.out.println("-------------not use");
				} else {
					// 初始化陈功而且支持当前语音后做什么
				}
			} else {
				Log.e("lixianda", "TTS init error");
				stopSelf();
			}
		}
	}

	/** {@inheritDoc} */
	 
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		
		mToast = Toast.makeText(this, "", 500);
		// 初始化识别对象
		mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
		// 初始化合成引擎  讯飞引擎
		mSpeech = new TextToSpeech(Asr_service.this, new TTSListener(), "com.iflytek.speechcloud");
			
			mSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {

			@Override
			public void onStart(String utteranceId) {
			}

			@Override
			public void onError(String utteranceId) {
			}

			@Override
			public void onDone(String utteranceId) {
				//提示音 “我能帮你做什么”
				if (utteranceId.equals("1001")) {
					ret = mAsr.startListening(mRecognizerListener);
					if (ret != ErrorCode.SUCCESS) {
						showTip("startListening error: " + ret);
					}
				}
				//提示完拨号  就打电话
				else if(utteranceId.equals("1002"))
				{
					// 拨号
					Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					mAsr.stopListening();
					stopSelf();
				}
				else if(utteranceId.equals("1003"))
				{
					// 语音编辑发短息
					Intent intent = new Intent(Asr_service.this, IatActivity.class);
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra("Name", name);
					intent.putExtra("PhoneNumber", number);
					startActivity(intent);
					stopSelf();
				}
				else
				{
					stopSelf();
				}
				
			}
		});
	
		mLocalLexicon = "";

		mLocalGrammar = FucUtil.readFile(Asr_service.this, "xtml.bnf", "utf-8");

		grammar();
		
		IntentFilter phonefilter = new IntentFilter();
		phonefilter.addAction("android.intent.action.PHONE_STATE");
		phonefilter.addAction("android.intent.action.NEW_OUTGOING_CALL");
		registerReceiver(ps, phonefilter);
		
		
	}

	
	String mContent;// 语法、词典临时变量
	int ret = 0;// 函数调用返回值

	private void grammar() {
		mContent = new String(mLocalGrammar);
		// 清空参数
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
		// grammarListener
		ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, grammarListener);
		if (ret != ErrorCode.SUCCESS) {
			showTip("grammar() error:" + ret);
		}
	}

	private void updataLexcion() {
		// mContent = new String(mLocalLexicon);
		if (mLocalLexicon == null) {
			showTip(getResources().getString(R.string.no_person));
			handler.sendEmptyMessage(1);
		}

		// 设置语法名称
		mAsr.setParameter(SpeechConstant.GRAMMAR_LIST, "xtml");

		
		
		Log.i("lixianda", sb.toString());
		ret = mAsr.updateLexicon("contact", sb.toString(), lexiconListener);
		if (ret != ErrorCode.SUCCESS) {
			if (ret == 20009) {
				showTip("updataLexcion() error:" + ret + getResources().getString(R.string.no_person));
			} else {
				showTip("updataLexcion() error:" + ret);
			}
		}
	}

	/**
	 * <br>功能简述:
	 * <br>功能详细描述:去掉特殊字符
	 * <br>注意:
	 * @param s
	 * @return
	 */
		public static String format(String s){ 
//			boolean b = Pattern.matches("[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……& amp;*（）——+|{}【】‘；：”“’。，、？|-]", s);
			String string = s.replace("[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……& amp;*（）——+|{}【】‘；：”“’。，、？|-]", "");
		   return string; 
		 }
	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d("lixianda", "SpeechRecognizer  () code = " + code);
			if (code != ErrorCode.SUCCESS) {
				// TODO 提示初始化引擎失败，退出应用
				showTip("mInitListener error:" + code);
				stopSelf();
			} else {
				
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
				showTip(getResources().getString(R.string.updatalexcion_ok));
				is_updata_lexcion_finish = true;

				IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
				mSpeech.speak(getResources().getString(R.string.help_you_dothing),
						TextToSpeech.QUEUE_ADD, IDmap);
//				ProgressDialogUtils.dismissProgressDialog();
			} else {
				if (error.getErrorCode() == 23108) {
					IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
					mSpeech.speak(getResources().getString(R.string.help_you_dothing),
							TextToSpeech.QUEUE_ADD, IDmap);
				}
				if (error.getErrorCode() == 20009) {
					showTip("lexiconListener error:" + error.getErrorCode() + getResources().getString(R.string.no_person));
					IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
					mSpeech.speak(getResources().getString(R.string.help_you_dothing),
							TextToSpeech.QUEUE_ADD, IDmap);
				}
				if (error.getErrorCode() == 23110) {
					IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
					mSpeech.speak(getResources().getString(R.string.help_you_dothing),
							TextToSpeech.QUEUE_ADD, IDmap);
				}
				Log.i("lixianda","lexiconListener error:" + error.getErrorCode());
//				ProgressDialogUtils.dismissProgressDialog();
//				AsrMain.this.finish();

			}
		}
	};

	/**
	 * 构建语法监听器。
	 */
	private GrammarListener grammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if (error == null) {
				showTip(getResources().getString(R.string.gramar_ok) + ":" + grammarId);

				ContactManager mgr = ContactManager.createManager(Asr_service.this, mContactListener);
				mgr.asyncQueryAllContactsName();
				mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);

			}
			if (error != null) {
				showTip("grammarListener：" + error.getErrorCode());
				stopSelf();
			}
		}
	};

	
	StringBuffer sb=new StringBuffer();
	StringBuffer show_sb=new StringBuffer();
	/**
	 * <br>功能简述:
	 * <br>功能详细描述:判断每个联系人名字中是否拥有非法字符
	 * <br>注意:
	 * @param lst
	 */
	void getdata(List<String> lst)
	{
	for(int i=0;i<lst.size();i++)
	{
		
		char[] mm =	lst.get(i).toCharArray();
		for(char c:mm)
		{
			boolean b = Pattern.matches("[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……& amp;*（）——+|{}【】‘；：”“’。，、？|-]", String.valueOf(c));
			if(b && !lst.isEmpty())
			{
				show_sb.append(lst.get(i)+";");
				lst.remove(i);
				getdata(lst);
				break;
			}
			else
			{
				
			}

		}
		
		
	}
	
}
	/**
	 * 获取联系人监听器。
	 */
	private ContactListener mContactListener = new ContactListener() {
		@Override
		public void onContactQueryFinish(String contactInfos, boolean changeFlag) {
			// 获取联系人
			mLocalLexicon = contactInfos;

			
			String[] tmp_str=mLocalLexicon.split("\n");
			List<String> lst_str=new ArrayList<String>(); 
			
			for(int i=0;i<tmp_str.length;i++)
			{
				lst_str.add(tmp_str[i]);
			}
		
			getdata(lst_str);
			
			
			
			if(lst_str.isEmpty())
			{
//				showTip("没有联系人或者您的联系人全是非法字符，打电话跟发短信将无效");
//				IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1001");
//				mSpeech.speak(getResources().getString(R.string.help_you_dothing),
//						TextToSpeech.QUEUE_ADD, IDmap);
			}
			else{
					for(int i=0;i<lst_str.size();i++)
					{
					sb.append(lst_str.get(i)+"\n");
					}
				}
			
			if (!is_updata_lexcion_finish && !sb.toString().equals("")) {
//				ProgressDialogUtils.showProgressDialog(Asr_service.this,
//						getResources().getString(R.string.going_updataLexcion));  
				updataLexcion();
			}

			if (sb.toString().equals("")) {
				
				//_____________add by lixd__________
				AcquireWakeLock();
				showTip(getResources().getString(R.string.dialog_system_prompt_content)+"\n存在非法字符："+show_sb);
				IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
						"1001");
				mSpeech.speak(
						getResources().getString(R.string.help_you_dothing),
						TextToSpeech.QUEUE_ADD, IDmap);
				
				//__________________________
				

			}
		}
	};

	/**
	 * 识别监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onVolumeChanged(int volume) {
			showTip(getResources().getString(R.string.volume_changed) + volume);
		}

		@Override
		public void onResult(final RecognizerResult result, boolean isLast) {
			//add by lixd
//		if(!is_found_pause)	
//		{	
			if (null != result && !TextUtils.isEmpty(result.getResultString())) {
				Log.d("lixianda", "recognizer result:" + result.getResultString());

				Map<String, List<String>> map = null;

				if (mResultType.equals("json")) {
					map = JsonParser
							.parseGrammarResultIntent(result.getResultString(), mEngineType);
					if (map == null) {
						showTip("again speak");

						mAsr.setParameter(SpeechConstant.PARAMS, null);
						// 设置识别引擎
						mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
						// 设置识别资源的路径
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
						// mAsr.setParameter(SpeechConstant.VAD_BOS, "3000");
						ret = mAsr.startListening(mRecognizerListener);

						if (ret != ErrorCode.SUCCESS) {
							showTip("startListening error:" + ret);
						}
						return;
					}
					
					Set<String> set = map.keySet();

					for (String keyset : set) {
						
						if (keyset.equals("callPhone")) {
							if (map.get("callPhone").size() >= 1) {
								name = map.get("callPhone").get(0);
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1002");
								mSpeech.speak(getResources().getString(R.string.call_phone) + name,
										TextToSpeech.QUEUE_ADD, IDmap);

								number = FindPhoneNumber(name);
							}

						} else if (keyset.equals("sendmsgPhone")) {

							if (map.get("sendmsgPhone").size() >= 1) {
								AcquireWakeLock();
								
								name = map.get("sendmsgPhone").get(0);
								number = FindPhoneNumber(name);
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1003");
								mSpeech.speak(getResources().getString(R.string.sen_msg) + name,
										TextToSpeech.QUEUE_ADD, IDmap);
								// Intent intent = new Intent();
								// // 系统默认的action，用来打开默认的短信界面
								// intent.setAction(Intent.ACTION_SENDTO);
								// // 需要发短息的号码
								// intent.setData(Uri.parse("smsto:" + number));
								// startActivity(intent);
								// mAsr.stopListening();
							}
						}
						// else 的是 keyset为openApp
						else {
							if ((getResources().getString(R.string.music)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1004");
								mSpeech.speak(getResources().getString(R.string.music_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								AcquireWakeLock();
//								Intent intent = new Intent(Intent.ACTION_MAIN);
								Intent intent = new Intent();
								intent.setClassName("com.shizhongkeji.musicplayer", "com.shizhongkeji.musicplayer.MainActivity");
							//	intent.addCategory(Intent.CATEGORY_APP_MUSIC);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
								mAsr.stopListening();
								//stopSelf();
							} else if ((getResources().getString(R.string.settings)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1005");
								mSpeech.speak(
										getResources().getString(R.string.settings_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								AcquireWakeLock();
								Intent mIntent = new Intent();
								mIntent.setAction(Settings.ACTION_SETTINGS);
								mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(mIntent);
								mAsr.stopListening();
								//stopSelf();
							} else if ((getResources().getString(R.string.camera)).equals(map.get(
									"openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1006");
								mSpeech.speak(
										getResources().getString(R.string.camera_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								AcquireWakeLock();
								Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
								camera.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(camera);
								mAsr.stopListening();
							//	stopSelf();
							} else if ((getResources().getString(R.string.picture)).equals(map.get(
									"openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1007");
								mSpeech.speak(getResources()
										.getString(R.string.picture_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								AcquireWakeLock();
								Uri uri = Images.Media.INTERNAL_CONTENT_URI;
								Intent intent = new Intent(Intent.ACTION_VIEW, uri);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
								mAsr.stopListening();
							//	stopSelf();
							} else if ((getResources().getString(R.string.call_dial)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1008");
								mSpeech.speak(
										getResources().getString(R.string.call_dial_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								AcquireWakeLock();
//								Intent intent = new Intent(Intent.ACTION_DIAL);
								Intent intent = new Intent();
								intent.setAction("com.example.xuntongwatch.main.Call_Activity");
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
								mAsr.stopListening();
							//	stopSelf();
							} else if ((getResources().getString(R.string.recorder)).equals(map
									.get("openApp").get(0))) {
								IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "1009");
								mSpeech.speak(
										getResources().getString(R.string.recorder_alert_sound),
										TextToSpeech.QUEUE_ADD, IDmap);
								AcquireWakeLock();
								Intent mi = new Intent(Media.RECORD_SOUND_ACTION);
								mi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(mi);
								mAsr.stopListening();
								//stopSelf();
							}
						}
					}

				}

			}

			else {
				mSpeech.speak(getResources().getString(R.string.no_thing),
						TextToSpeech.QUEUE_FLUSH, null);
				Log.d("lixianda", "recognizer result : null");
			}
//		}
//		//add by lixd
//		else
//		{
//			
//		}
			
		}

		// 根据名字查找手机号码 add by lixd
		private String FindPhoneNumber(String name) {
			ContentResolver contentResolver = Asr_service.this.getContentResolver();
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

			showTip(getResources().getString(R.string.end_talk));
			Log.d("lixianda", "onEndOfSpeech()");
		}

		@Override
		public void onBeginOfSpeech() {
			showTip(getResources().getString(R.string.start_talk));
			Log.d("lixianda", "onBeginOfSpeech()");
		}

		@Override
		public void onError(SpeechError error) {
//		if(!is_found_pause)	
//		{	
			if (error.getErrorCode() == 20005) {
				showTip(getResources().getString(R.string.no_result_show));
			} else if (error.getErrorCode() == 23300) {
				showTip(getResources().getString(R.string.again_grammar));
			} else if (error.getErrorCode() == 23108) {
				showTip(getResources().getString(R.string.again_updataLexcion));
			} else {
				Log.e("lixianda", "onError()");
				Toast.makeText(Asr_service.this, "" + error.getErrorCode(), Toast.LENGTH_SHORT).show();
			}
				mAsr.stopListening();
				mAsr.cancel();

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
				// mAsr.setParameter(SpeechConstant.VAD_BOS, "1000");
				ret = mAsr.startListening(mRecognizerListener);

				if (ret != ErrorCode.SUCCESS) {
					showTip("error code:" + ret);
				}
//			}
//		else
//		{
//			
//		}
		
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			Log.d("lixianda", "onEvent()" + eventType + " " + arg1 + " " + arg2 + " ");
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
			mAsr.setParameter(SpeechConstant.VAD_BOS, "2000");
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

	/**
	 * {@inheritDoc}点亮屏幕
	 */
	private void AcquireWakeLock() { 
	    PowerManager pm = (PowerManager)getApplication().getSystemService(Context.POWER_SERVICE); 
	       
	    WakeLock m_wakeObj = (WakeLock)pm.newWakeLock(PowerManager.FULL_WAKE_LOCK 
	               | PowerManager.ACQUIRE_CAUSES_WAKEUP 
	               | PowerManager.ON_AFTER_RELEASE, ""); 
	       
	    // m_wakeObj.acquire(); 
	       
	    //点亮屏幕15秒钟 
	    m_wakeObj.acquire(1000 * 5); 
	    m_wakeObj.release();//释放资源 
	       
	}
	
	
	
	
	
	
	public class PhoneStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			// 如果是拨打电话
			if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
				mAsr.stopListening();
				mAsr.cancel();
				mSpeech.stop();
			} 
			else {
				TelephonyManager tm = (TelephonyManager) context
						.getSystemService(Service.TELEPHONY_SERVICE);
				switch (tm.getCallState()) {
				case TelephonyManager.CALL_STATE_RINGING:
					mAsr.stopListening();
					mAsr.cancel();
					mAsr.destroy();
					mSpeech.stop();
					break;
				case TelephonyManager.CALL_STATE_IDLE:
//					is_other_back = false;
//					is_contrl_call = false;
					
					mAsr.stopListening();
					mAsr.cancel();
					mAsr.destroy();
					mSpeech.stop();
					break;
				case TelephonyManager.CALL_STATE_OFFHOOK:
//					is_contrl_call = true;
					
					mAsr.stopListening();
					mAsr.cancel();
					mAsr.destroy();
					mSpeech.stop();
					break;
				}

			}
		}

	}
	
	
	
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	/** {@inheritDoc} */
	 
	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
	}

	/** {@inheritDoc} */
	 
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		showTip("服务的onstartCommand()");
		
	
		
		if(is_updata_lexcion_finish)
		{
			showTip("联系人更新完成");
		}
	//	IDmap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,
	//			"1001");
	//	mSpeech.speak(
	//			getResources().getString(R.string.help_you_dothing),
	//			TextToSpeech.QUEUE_ADD, IDmap);
		
		return START_NOT_STICKY;
	}

	/** {@inheritDoc} */
	 
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		//showTip("服务已经停止");
		destoryListen();
		super.onDestroy();
	}
	
	/**
	 * <br>功能简述:
	 * <br>功能详细描述:停止录音
	 * <br>注意:
	 */
	void destoryListen()
	{
		if (mAsr != null) {
			mAsr.cancel();
			mAsr.destroy();
			mAsr.stopListening();
		}
	}
	
	
}
