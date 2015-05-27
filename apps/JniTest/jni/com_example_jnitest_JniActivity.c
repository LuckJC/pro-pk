#define LOG_TAG "JniTest"

#include <jni.h>
#include <utils/Log.h>

#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

/*
 * Class:     com_example_jnitest_JniActivity
 * Method:    printJNI
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_example_jnitest_JniActivity_printJNI
  (JNIEnv *env, jobject obj, jstring inputStr)
{
		LOGI("Hello, From libmyjnitest.so!");
		// 从 instring 字符串取得指向字符串 UTF 编码的指针
		const char *str =
		(const char *)(*env)->GetStringUTFChars(env, inputStr, JNI_FALSE);
		LOGI("Get the String--->%s",(const char *)str);
		// 通知虚拟机本地代码不再需要通过 str 访问 Java 字符串。
		(*env)->ReleaseStringUTFChars(env, inputStr, (const char *)str );
		return (*env)->NewStringUTF(env, "Hello, I am Native interface");
}

/* This function will be call when the library first be load.
* You can do some init in the libray. return which version jni it support.
*/
jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
		void *venv;
		LOGI("Hello----->JNI_OnLoad!");
		if ((*vm)->GetEnv(vm, (void**)&venv, JNI_VERSION_1_4) != JNI_OK) {
			LOGI("Hello--->ERROR: GetEnv failed");
			return -1;
		}
		
		return JNI_VERSION_1_4;
}