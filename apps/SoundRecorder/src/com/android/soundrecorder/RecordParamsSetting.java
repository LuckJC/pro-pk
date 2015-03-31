package com.android.soundrecorder;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaRecorder;
import android.os.SystemProperties;

import com.mediatek.common.featureoption.FeatureOption;
import com.mediatek.media.MediaRecorderEx;
import com.mediatek.soundrecorder.ext.ExtensionHelper;
import com.mediatek.soundrecorder.ext.IQualityLevel;

/**
 * M: We use this class to do operations related with record params.
 * When recording, we can get all record params according to input params.
 */ 
public class RecordParamsSetting {
    public static final int FORMAT_HIGH = 0;
    public static final int FORMAT_STANDARD = 1;
    public static final int FORMAT_LOW = 2;
    public static final int MODE_NORMAL = 0;
    public static final int MODE_INDOOR = 1;
    public static final int MODE_OUTDOOR = 2;

    public static final int AUDIO_CHANNELS_MONO = 1;
    public static final int AUDIO_CHANNELS_STEREO = 2;

    public static final String NOT_LIMIT_TYPE = "*/*";
    public static final String AUDIO_NOT_LIMIT_TYPE = "audio/*";
    public static final String AUDIO_3GPP = "audio/3gpp";
    public static final String AUDIO_VORBIS = "audio/vorbis";
    public static final String AUDIO_AMR = "audio/amr";
    public static final String AUDIO_AWB = "audio/awb";
    public static final String AUDIO_OGG = "application/ogg";
    public static final String AUDIO_AAC = "audio/aac";
    public static final String AUDIO_WAV = "audio/wav";
    
    //For high quality saved in shared preferences
    public static final String HIGH_SAMPLE_RATE = "high_sample_rate";
    public static final String HIGH_ENCODE_BITRATE = "high_encode_bitrate";
    public static final String HIGH_ENCODER = "high_encoder";
    public static final String HIGH_AUDIO_CHANNELS = "high_audio_channels";
    public static final String HIGH_OUTPUT_FORMAT = "high_output_format";
    //For standard quality saved in shared preferences
    public static final String STANDARD_SAMPLE_RATE = "standard_sample_rate";
    public static final String STANDARD_ENCODE_BITRATE = "standard_encode_bitrate";
    public static final String STANDARD_ENCODER = "standard_encoder";
    public static final String STANDARD_AUDIO_CHANNELS = "standard_audio_channels";
    public static final String STANDARD_OUTPUT_FORMAT = "standard_output_format";
    //For low quality saved in shared preferences
    public static final String LOW_SAMPLE_RATE = "low_sample_rate";
    public static final String LOW_ENCODE_BITRATE = "low_encode_bitrate";
    public static final String LOW_ENCODER = "low_encoder";
    public static final String LOW_AUDIO_CHANNELS = "low_audio_channels";
    public static final String LOW_OUTPUT_FORMAT = "low_output_format";
    //For shared preferences
    public static final String RECORD_PARAM = "record_params";
    public static final String INIT_VALUES= "init_values";
    
    //M: add for adpcm
    public static final int ENCODE_BITRATE_AAC = 128000;
    public static final int ENCODE_BITRATE_VORBIS = 128000;
    public static final int SAMPLE_RATE_AAC = 48000;
    public static final int SAMPLE_RATE_VORBIS = 48000;
    public static final int ENCODE_BITRATE_ADPCM = 128000;
    public static final int SAMPLE_RATE_ADPCM = 48000;
    public static final String HIGH_RECORD_ENCODER = "high_record_encoder";

    public static final int EFFECT_AEC = 0;
    public static final int EFFECT_NS = 1;
    public static final int EFFECT_AGC = 2;

    private static final String TAG = "SR/RecordParamsSetting";
    private static int[] sFormatArray = null;
    private static int[] sModeArray = null;
    // Add to test AudioEffect through the SoundRecorder UI
    private static boolean sEnableTestAudioEffect = false;
    private static int[] sEffectArray = new int[] { R.string.recording_effect_AEC,
            R.string.recording_effect_NS, R.string.recording_effect_AGC };
    private static Resources sResources = null;
    private static SharedPreferences sPreferences = null;
    private static int[] sHighParams = null;
    private static int[] sStandardParams = null;
    private static int[] sLowParams = null;
    private static IQualityLevel sQualityLevel = null;

    //M: All params will be used when record
    static public class RecordParams {
        public int mAudioChannels = AUDIO_CHANNELS_MONO;
        public int mAudioEncoder = -1;
        public int mAudioEncodingBitRate = -1;
        // M: add for MMS, when launch from MMS, for more accurate timing
        // if AUDIO_AMR.equals(requestType), bit rate used for calculate remainingtime 
        // is not ENCODE_BITRATE_AMR 12200, but 12800 
        public int mRemainingTimeCalculatorBitRate = -1;
        public int mAudioSamplingRate = -1;
        public String mExtension = "";
        public String mMimeType = "";
        public int mHDRecordMode = -1;
        public int mOutputFormat = -1;
        public boolean[] mAudioEffect = null;
    }

    static RecordParams getRecordParams(String requestType, int selectFormat, int selectMode,
            boolean[] selectEffect) {
        LogUtils.i(TAG, "<getRecordParams> start");
        RecordParams recordParams = new RecordParams();
        int[] params = null;
        if (canSelectEffect()) {
            recordParams.mAudioEffect = selectEffect;
        }
        if (AUDIO_NOT_LIMIT_TYPE.equals(requestType) || NOT_LIMIT_TYPE.equals(requestType)) {
            // For launch SoundRecorder without specify a encode type, eg: from launcher
            if (FeatureOption.HAVE_AACENCODE_FEATURE) {
                switch (selectFormat) {
                    case FORMAT_HIGH:
                        params = sHighParams.clone();
                        recordParams.mAudioEncoder = sPreferences.getInt(HIGH_ENCODER, params[0]);
                        recordParams.mAudioChannels = sPreferences.getInt(HIGH_AUDIO_CHANNELS, params[1]);
                        recordParams.mAudioEncodingBitRate = sPreferences.getInt(HIGH_ENCODE_BITRATE, params[2]);
                        recordParams.mAudioSamplingRate = sPreferences.getInt(HIGH_SAMPLE_RATE, params[3]);
                        recordParams.mOutputFormat = sPreferences.getInt(HIGH_OUTPUT_FORMAT, params[4]);
                        break;
                    case FORMAT_STANDARD:
                        params = sStandardParams.clone();
                        recordParams.mAudioEncoder = sPreferences.getInt(STANDARD_ENCODER, params[0]);
                        recordParams.mAudioChannels = sPreferences.getInt(STANDARD_AUDIO_CHANNELS, params[1]);
                        recordParams.mAudioEncodingBitRate = sPreferences.getInt(STANDARD_ENCODE_BITRATE, params[2]);
                        recordParams.mAudioSamplingRate = sPreferences.getInt(STANDARD_SAMPLE_RATE, params[3]);
                        recordParams.mOutputFormat = sPreferences.getInt(STANDARD_OUTPUT_FORMAT, params[4]);
                        break;
                    case FORMAT_LOW:
                        params = sLowParams.clone();
                        recordParams.mAudioEncoder = sPreferences.getInt(LOW_ENCODER, params[0]);
                        recordParams.mAudioChannels = sPreferences.getInt(LOW_AUDIO_CHANNELS, params[1]);
                        recordParams.mAudioEncodingBitRate = sPreferences.getInt(LOW_ENCODE_BITRATE, params[2]);
                        recordParams.mAudioSamplingRate = sPreferences.getInt(LOW_SAMPLE_RATE, params[3]);
                        recordParams.mOutputFormat = sPreferences.getInt(LOW_OUTPUT_FORMAT, params[4]);
                        break;
                    default:
                        LogUtils.e(TAG, "<getRecordParams> selectFormat is out of range");
                        break;
                }
            } else {
                // if there is no other Encoders ,will default use AMR Encoder and out put ".amr" file
                LogUtils.i(TAG, "<getRecordParams>, use default Encoder: AMR.");
                params = sResources.getIntArray(R.array.amr_params);
                recordParams.mAudioEncoder = params[0];
                recordParams.mAudioChannels = params[1];
                recordParams.mAudioEncodingBitRate = params[2];
                recordParams.mAudioSamplingRate = params[3];
                recordParams.mOutputFormat = params[4];
            }
            //use to calculator remaining time
            recordParams.mRemainingTimeCalculatorBitRate = recordParams.mAudioEncodingBitRate;
            setExtensionAndMimeTypeParams(recordParams);
        } else {
            //For launch SoundRecorder with specify a encode type, eg: from mms
            if (AUDIO_AMR.equals(requestType)) {
                params = sResources.getIntArray(R.array.amr_params);
            } else if (AUDIO_AWB.equals(requestType)) {
                params = sResources.getIntArray(R.array.awb_params);
            } else if (AUDIO_AAC.equals(requestType)) {
                params = sResources.getIntArray(R.array.aac_params);
            }
            if (null != params) {
                recordParams.mAudioEncoder = params[0];
                recordParams.mAudioChannels = params[1];
                recordParams.mAudioEncodingBitRate = params[2];
                recordParams.mAudioSamplingRate = params[3];
                recordParams.mOutputFormat = params[4];
                if (MediaRecorder.AudioEncoder.AMR_NB == recordParams.mAudioEncoder) {
                    recordParams.mRemainingTimeCalculatorBitRate = 12800;
                } else {
                    recordParams.mRemainingTimeCalculatorBitRate = recordParams.mAudioEncodingBitRate;
                }
                setExtensionAndMimeTypeParams(recordParams);
            } else {
                throw new IllegalArgumentException("Invalid output file type requested");
            }
        }
        LogUtils.i(TAG, "recordParams.mAudioEncoder is:" + recordParams.mAudioEncoder
                + "; recordParams.mAudioChannels is:" + recordParams.mAudioChannels
                + "; recordParams.mAudioEncodingBitRate is:" + recordParams.mAudioEncodingBitRate
                + "; recordParams.mAudioSamplingRate is:" + recordParams.mAudioSamplingRate
                + "; recordParams.mOutputFormat is:" + recordParams.mOutputFormat);
        //Set recording mode
        if (FeatureOption.MTK_AUDIO_HD_REC_SUPPORT) {
            switch (selectMode) {
            case MODE_NORMAL:
                recordParams.mHDRecordMode = MediaRecorderEx.HDRecordMode.NORMAL;
                LogUtils.i(TAG, "<getRecordParams> mHDRecordMode" + "is MODE_NORMAL");
                break;
            case MODE_INDOOR:
                recordParams.mHDRecordMode = MediaRecorderEx.HDRecordMode.INDOOR;
                LogUtils.i(TAG, "<getRecordParams> mHDRecordMode" + "is MODE_INDOOR");
                break;
            case MODE_OUTDOOR:
                recordParams.mHDRecordMode = MediaRecorderEx.HDRecordMode.OUTDOOR;
                LogUtils.i(TAG, "<getRecordParams> mHDRecordMode" + "is MODE_OUTDOOR");
                break;
            default:
                LogUtils.e(TAG, "<getRecordParams> selectMode is out of range");
                break;
            }
        }
        LogUtils.i(TAG, "<getRecordParams> end");
        return recordParams;
    }

    static boolean canSelectFormat() {
        // M: Modify to use AAC encoder and remove the AMR and VORBIS
        LogUtils.i(TAG, "<canSelectFormat> FeatureOption.HAVE_AACENCODE_FEATURE is:"
                + FeatureOption.HAVE_AACENCODE_FEATURE);
        return FeatureOption.HAVE_AACENCODE_FEATURE;
    }

    static boolean canSelectMode() {
        return FeatureOption.MTK_AUDIO_HD_REC_SUPPORT;
    }

    static boolean canSelectEffect() {
        /**
         * M: Modified for enable test Audio Effect through the SoundRecorder UI
         * and disable it when release to customer. @{
         */
        return FeatureOption.NATIVE_AUDIO_PREPROCESS_ENABLE && sEnableTestAudioEffect;
        /** @}*/
    }

    static boolean isAvailableRequestType(String requestType) {
        return (AUDIO_AMR.equals(requestType) || AUDIO_3GPP.equals(requestType)
                || AUDIO_NOT_LIMIT_TYPE.equals(requestType) || NOT_LIMIT_TYPE.equals(requestType));
    }

    static int[] getModeStringIDArray() {
        int[] modeIDArray = new int[3];
        modeIDArray[MODE_NORMAL] = R.string.recording_mode_nomal;
        modeIDArray[MODE_INDOOR] = R.string.recording_mode_meeting;
        modeIDArray[MODE_OUTDOOR] = R.string.recording_mode_lecture;
        sModeArray = new int[3];
        sModeArray[0] = MODE_NORMAL;
        sModeArray[1] = MODE_INDOOR;
        sModeArray[2] = MODE_OUTDOOR;
        return modeIDArray;
    }

    static int[] getFormatStringIDArray(Context context) {
        int[] formartStringIDArray = null;
        if (FeatureOption.HAVE_AACENCODE_FEATURE) {
            int levelNumber = sQualityLevel.getLevelNumber();
            if (SoundRecorder.TWO_LEVELS == levelNumber) {
                formartStringIDArray = new int[2];
                formartStringIDArray[0] = R.string.recording_format_high;
                formartStringIDArray[1] = R.string.recording_format_standard;
                sFormatArray = new int[2];
                sFormatArray[0] = FORMAT_HIGH;
                sFormatArray[1] = FORMAT_STANDARD;
            } else if (SoundRecorder.THREE_LEVELS == levelNumber) {
                formartStringIDArray = new int[3];
                formartStringIDArray[0] = R.string.recording_format_high;
                formartStringIDArray[1] = R.string.recording_format_mid;
                formartStringIDArray[2] = R.string.recording_format_low;
                sFormatArray = new int[3];
                sFormatArray[0] = FORMAT_HIGH;
                sFormatArray[1] = FORMAT_STANDARD;
                sFormatArray[2] = FORMAT_LOW;
            }
        } else {
            LogUtils.e(TAG, "<dlgChooseChannel> No featureOption enable");
        }
        return formartStringIDArray;
    }

    static int[] getEffectStringIDArray() {
        return sEffectArray;
    }

    static int getSelectFormat(int which) {
        return sFormatArray[which];
    }

    static int getSelectMode(int which) {
        return sModeArray[which];
    }

    /**
     * M: If is the first time to use the "record_params" preference, we will
     * set the default values to initialize the preference.
     * 
     * @param context
     */
    static void initRecordParamsSharedPreference(Context context) {
        LogUtils.i(TAG, "<InitSharedPreference>");
        sResources = context.getResources();
        sPreferences = context.getSharedPreferences(RECORD_PARAM, 0);
        sQualityLevel = ExtensionHelper.getExtension(context);
        getParamsArrayFromResource();
        boolean isFirstSet = sPreferences.getBoolean(INIT_VALUES, true);
        LogUtils.i(TAG, "isFirstSet is:" + isFirstSet);
        if (isFirstSet) {
            LogUtils.i(TAG, "It is the first time to set default values.");
            Editor editor = sPreferences.edit();
            editor.putBoolean(INIT_VALUES, false);
            //For high quality 
            editor.putInt(HIGH_ENCODER, sHighParams[0]);
            editor.putInt(HIGH_AUDIO_CHANNELS, sHighParams[1]);
            editor.putInt(HIGH_ENCODE_BITRATE, sHighParams[2]);
            editor.putInt(HIGH_SAMPLE_RATE, sHighParams[3]);
            editor.putInt(HIGH_OUTPUT_FORMAT, sHighParams[4]);
            //For standard quality
            editor.putInt(STANDARD_ENCODER, sStandardParams[0]);
            editor.putInt(STANDARD_AUDIO_CHANNELS, sStandardParams[1]);
            editor.putInt(STANDARD_ENCODE_BITRATE, sStandardParams[2]);
            editor.putInt(STANDARD_SAMPLE_RATE, sStandardParams[3]);
            editor.putInt(STANDARD_OUTPUT_FORMAT, sStandardParams[4]);
            if(null != sLowParams){
                //For low quality
                editor.putInt(LOW_ENCODER, sLowParams[0]);
                editor.putInt(LOW_AUDIO_CHANNELS, sLowParams[1]);
                editor.putInt(LOW_ENCODE_BITRATE, sLowParams[2]);
                editor.putInt(LOW_SAMPLE_RATE, sLowParams[3]);
                editor.putInt(LOW_OUTPUT_FORMAT, sLowParams[4]);
            }
            editor.commit();
        }
    }

    /**
     * M: Set the file extension and MimeType depends on the given outputFormat.
     * 
     * @param recordParams
     */
    static void setExtensionAndMimeTypeParams(RecordParams recordParams) {
        LogUtils.i(TAG, "<setExtensionAndMimeTypeParams>");
        switch (recordParams.mOutputFormat) {
            case MediaRecorder.OutputFormat.AMR_NB:
                recordParams.mExtension= ".amr";
                recordParams.mMimeType = RecordParamsSetting.AUDIO_AMR;
                break;
            case MediaRecorder.OutputFormat.AMR_WB:
                recordParams.mExtension= ".awb";
                recordParams.mMimeType = RecordParamsSetting.AUDIO_AWB;
                break;
            case MediaRecorder.OutputFormat.THREE_GPP:
                recordParams.mExtension= ".3gpp";
                recordParams.mMimeType = RecordParamsSetting.AUDIO_3GPP;
                break;
            case MediaRecorder.OutputFormat.OUTPUT_FORMAT_OGG:
                recordParams.mExtension= ".ogg";
                recordParams.mMimeType = RecordParamsSetting.AUDIO_OGG;
                break;
            case MediaRecorder.OutputFormat.OUTPUT_FORMAT_WAV:
                recordParams.mExtension= ".wav";
                recordParams.mMimeType = RecordParamsSetting.AUDIO_WAV;
                break;
            case MediaRecorder.OutputFormat.AAC_ADTS:
            case MediaRecorder.OutputFormat.AAC_ADIF:
                 recordParams.mExtension = ".aac";
                 recordParams.mMimeType = RecordParamsSetting.AUDIO_AAC;
                break;
            default:
                break;
        }
        LogUtils.i(TAG, "recordParams.mExtension is:" + recordParams.mExtension
                + "; recordParams.mMimeType is:" + recordParams.mMimeType);
    }

    /**
     * M: Get the parameters array from resource file.
     */
    static void getParamsArrayFromResource(){
        int levelNumber = sQualityLevel.getLevelNumber();
        if (SoundRecorder.THREE_LEVELS == levelNumber) {
            LogUtils.i(TAG, "There will show three levels at voice qulity choose dialog.");
            sHighParams = sResources.getIntArray(R.array.operator_high_params);
            sStandardParams = sResources.getIntArray(R.array.operator_standard_params);
            sLowParams = sResources.getIntArray(R.array.operator_low_params);
        } else {
            LogUtils.i(TAG, "There will show two levels at voice qulity choose dialog.");
            sHighParams = sResources.getIntArray(R.array.high_params);
            sStandardParams = sResources.getIntArray(R.array.standard_params);
        }
    }

}
