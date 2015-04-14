package com.infocomiot.watch.launcher.speechsound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.iflytek.cloud.util.ResourceUtil;
import com.iflytek.cloud.util.ContactManager.ContactListener;
import com.iflytek.cloud.util.ResourceUtil.RESOURCE_TYPE;
import com.infocomiot.watch.launcher.launcher.MyApplication;

import android.R;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Audio.Media;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class SpeechsoudService extends Service {
    private boolean isUpdateLexionOver = false;	//是否更新词典完成了   如果是    就后面不需要更新
	private static String TAG = "AbnfDemo";
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
	// 云端语法文件
	private String mCloudGrammar = null;
	// 本地语法构建路径
	private String grmPath = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/msc/test";
	// 返回结果格式，支持：xml,json
	private String mResultType = "json";

	private final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
	private final String GRAMMAR_TYPE_ABNF = "abnf";
	private final String GRAMMAR_TYPE_BNF = "bnf";

	private String mEngineType = "local";

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return mybind;
	}
	private MyBinder mybind=new MyBinder();
	
	public class MyBinder extends Binder {
		public SpeechsoudService getservice()
		{
			return SpeechsoudService.this;
			
		}
	}
	/** {@inheritDoc} */

	@Override
	public void onCreate() {
		super.onCreate();
		// TODO Auto-generated method stub
		// 初始化识别对象
		mAsr = SpeechRecognizer.createRecognizer(this, mInitListener);
//
//		// 初始化语法、命令词
//		mLocalLexicon = "";
//
//		mLocalGrammar = FucUtil.readFile(this, "xtml.bnf", "utf-8");
//
//		mCloudGrammar = FucUtil.readFile(this, "grammar_sample.abnf", "utf-8");
//		
//		mToast = Toast.makeText(this, "", 300);
//
//		grammar();
		
		
//		ContactManager mgr = ContactManager.createManager(SpeechsoudService.this, mContactListener);
//		mgr.asyncQueryAllContactsName();
//		mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
		
		
		
//		Runnable r = new Runnable() {
//
//			@Override
//			public void run() {
//				grammar();
//			}
//		};
//		Thread t = new Thread(r);
//		t.start();
		
//		updateContact();


	}
	//修饰符为public   方便有联系的Activity调用
	public void startListen()
	{
		

		// 初始化语法、命令词
		mLocalLexicon = "";

		mLocalGrammar = FucUtil.readFile(this, "xtml.bnf", "utf-8");

		mCloudGrammar = FucUtil.readFile(this, "grammar_sample.abnf", "utf-8");

		// ContactManager mgr =
		// ContactManager.createManager(SpeechsoudService.this,
		// mContactListener);
		// mgr.asyncQueryAllContactsName();
		// mSharedPreferences = getSharedPreferences(getPackageName(),
		// MODE_PRIVATE);
		mToast = Toast.makeText(this, "", 300);

		grammar();
		
	}
	//修饰符为public   方便有联系的Activity调用
	public void stopListen()
	{
		mAsr.cancel();
		mAsr.destroy();
		mAsr.stopListening();
	}
	
//	@Override
//	public int onStartCommand(Intent intent, int flags, int startId) {
//		
////		return START_NOT_STICKY;
//		return s
//	}

	/**
	 * 初始化监听器。
	 */
	private InitListener mInitListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "SpeechRecognizer  () code = " + code);
			if (code != ErrorCode.SUCCESS) {
				showTip("Init-ErrorCode：" + code);
			}
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
			if(!isUpdateLexionOver){
				updateContact();
			}
		}
	};

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

	/**
	 * 识别监听器。
	 */
	private RecognizerListener mRecognizerListener = new RecognizerListener() {

		@Override
		public void onVolumeChanged(int volume) {
			showTip("Is-talking：" + volume);
		}

		@Override
		public void onResult(final RecognizerResult result, boolean isLast) {

			if (null != result && !TextUtils.isEmpty(result.getResultString())) {
				Log.d(TAG, "recognizer result：" + result.getResultString());

				Map<String, List<String>> map = null;
				

				if (mResultType.equals("json")) {
					map = JsonParser.parseGrammarResultIntent(result.getResultString(), mEngineType);
					if (map == null) {
						showTip("Don't understand");
					}
					Set<String> set = map.keySet();
					stopListen();
					
					for (String keyset : set) {
						if (keyset.equals("callPhone")) {
							if (map.get("callPhone").size() >= 1) {
								 String number = FindPhoneNumber(map.get("callPhone").get(0));
								Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+ number));
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							}

							
						} else if (keyset.equals("sendmsgPhone")) {
							if (map.get("sendmsgPhone").size() >= 1) {
								String number = FindPhoneNumber(map.get("sendmsgPhone").get(0));

								Intent intent = new Intent();
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								// 系统默认的action，用来打开默认的短信界面
								intent.setAction(Intent.ACTION_SENDTO);
								// 需要发短息的号码
								intent.setData(Uri.parse("smsto:" + number));
								startActivity(intent);
							}
						}
						// 这个else 的是 keyset为openApp
						else {
							if ("音乐".equals(map.get("openApp").get(0))) {

								Intent intent = new Intent(Intent.ACTION_MAIN);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.addCategory(Intent.CATEGORY_APP_MUSIC);
								startActivity(intent);
							} else if ("设置".equals(map.get("openApp").get(0))) {
								Intent intent = new Intent();
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								intent.setAction(Settings.ACTION_SETTINGS);
								startActivity(intent);
							} else if ("相机".equals(map.get("openApp").get(0))) {
								Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							} else if ("图片".equals(map.get("openApp").get(0))) {
								Uri uri = Images.Media.INTERNAL_CONTENT_URI;
								Intent intent = new Intent(Intent.ACTION_VIEW, uri);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							} else if ("拨号".equals(map.get("openApp").get(0))) {
								Intent intent = new Intent(Intent.ACTION_DIAL);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							} else if ("录音机".equals(map.get("openApp").get(0))) {
								Intent intent = new Intent(Media.RECORD_SOUND_ACTION);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							}
						}
					}

				}

			}
			//这个else 是没有匹配到结果的时候
			else {
				Log.d(TAG, "recognizer result : null");
				showTip("result is null");
				mAsr.stopListening();
				mAsr.cancel();
				
				InitsetParameter();
				int ret = mAsr.startListening(this);

				if (ret != ErrorCode.SUCCESS) {

					showTip("back_Distinguish_ErrorCode：" + ret);

				}
			}

		}

		// 根据名字查找手机号码 add by lixd

		private String FindPhoneNumber(String name) {
			ContentResolver contentResolver = SpeechsoudService.this.getContentResolver();
			Cursor cursor = contentResolver.query(
					ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
					new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
					ContactsContract.PhoneLookup.DISPLAY_NAME + "='" + name + "'", null, null);

			if (cursor.moveToFirst() != false) {
				String number = cursor.getString(0);
				cursor.close();
				return number;
			}
			cursor.close();
			return "";
			
		}

		@Override
		public void onEndOfSpeech() {
			showTip("end talk");
		}

		@Override
		public void onBeginOfSpeech() {
			showTip("start talk");
		}

		@Override
		public void onError(SpeechError error) {
			if(error.getErrorCode()==20005)
//			showTip("onError Code：" + error.getErrorCode());
				{
				showTip("no result");
				}
			mAsr.stopListening();
			mAsr.cancel();
			
			InitsetParameter();
			int ret = mAsr.startListening(mRecognizerListener);

			if (ret != ErrorCode.SUCCESS) {

				showTip("back_Distinguish_ErrorCode：" + ret);

			}
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

		}
	};
	
	private void InitsetParameter() {
		mAsr.setParameter(SpeechConstant.PARAMS, null);

		// 设置识别引擎
		mAsr.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);

		mAsr.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath()); // 设置语法构建路径
		mAsr.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath); // 设置返回结果格式
		mAsr.setParameter(SpeechConstant.RESULT_TYPE, mResultType); // 设置本地识别使用语法id
		mAsr.setParameter(SpeechConstant.LOCAL_GRAMMAR, "xtml"); // 设置识别的门限值
		mAsr.setParameter(SpeechConstant.MIXED_THRESHOLD, "30"); // 使用8k音频的时候请解开注释
																	// //
//		mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
//		mAsr.setParameter(SpeechConstant.VAD_BOS, "5000");
	}
	
	/**
	 * 构建语法监听器。
	 */
	private GrammarListener grammarListener = new GrammarListener() {
		@Override
		public void onBuildFinish(String grammarId, SpeechError error) {
			if (error == null) {
				if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
					Editor editor = mSharedPreferences.edit();
					if (!TextUtils.isEmpty(grammarId))
						editor.putString(KEY_GRAMMAR_ABNF_ID, grammarId);
					editor.commit();
				}
				showTip("grammar ok：" + grammarId);
				
				ContactManager mgr = ContactManager.createManager(SpeechsoudService.this, mContactListener);
				mgr.asyncQueryAllContactsName();
				mSharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
				
			} else {
				showTip("grammar false：" + error.getErrorCode());
			}
		}
	};

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

	private boolean grammar() {
		if (mEngineType.equals(SpeechConstant.TYPE_LOCAL)) {
			String mContent = new String(mLocalGrammar);

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
			int ret = mAsr.buildGrammar(GRAMMAR_TYPE_BNF, mContent, grammarListener);

			if (ret != ErrorCode.SUCCESS) {
				showTip("grammar false：" + ret);
				return false;
			}
		}

		return true;
	}

	private boolean updateContact() {
		
// 		 mContent = new String(mLocalLexicon);
		// 清空参数
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
		// }
		mAsr.setParameter(SpeechConstant.GRAMMAR_LIST, "xtml");
		
		int ret = mAsr.updateLexicon("contact", mLocalLexicon, lexiconListener);

		if (ret != ErrorCode.SUCCESS) {
			showTip("updateLexicon false：" + ret);
			return false;
		}else
		{
			showTip("updataLexicon ok");
		}

		return true;
	}

	/**
	 * 更新词典监听器。
	 */
	private LexiconListener lexiconListener = new LexiconListener() {
		@Override
		public void onLexiconUpdated(String lexiconId, SpeechError error) {
			if (error == null) {
				
				showTip("updateLexicon ok");
//				isUpdateLexionOver = true;
				isUpdateLexionOver = true;
				
				mAsr.stopListening();
				mAsr.cancel();
				
				InitsetParameter();
				int ret = mAsr.startListening(mRecognizerListener);
				if (ret != ErrorCode.SUCCESS) {
						showTip(""+ ret);
					}
				} 
			else {
				showTip("updateLexicon false：" + error.getErrorCode());
			}
		}
	};
	

	/** {@inheritDoc} */

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		// 退出时释放连接
		stopListen();
	}

	
}
