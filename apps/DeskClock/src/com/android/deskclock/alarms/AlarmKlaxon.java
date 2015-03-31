/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.deskclock.alarms;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.text.TextUtils;


import com.android.deskclock.Log;
import com.android.deskclock.R;
import com.android.deskclock.provider.AlarmInstance;

import com.mediatek.deskclock.ext.ICMCCSpecialSpecExtension;
import com.mediatek.pluginmanager.Plugin;
import com.mediatek.pluginmanager.PluginManager;

import java.io.File;
import java.io.IOException;

/**
 * Manages playing ringtone and vibrating the device.
 */
public class AlarmKlaxon {
    private static final long[] VIBRATE_PATTERN = new long[] { 500, 500 };
    private static final int VIBRATE_LENGTH = 500;
    private static boolean sStarted = false;
    private static MediaPlayer sMediaPlayer = null;
    private static ICMCCSpecialSpecExtension sICMCCSpecialSpecExtension;

    public static void stop(Context context) {
        Log.v("AlarmKlaxon.stop()");

        if (sStarted) {
            sStarted = false;
            // Stop audio playing
            if (sMediaPlayer != null) {
                sMediaPlayer.stop();
                AudioManager audioManager = (AudioManager)
                        context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.abandonAudioFocus(null);
                sMediaPlayer.release();
                sMediaPlayer = null;
            }

            ((Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE)).cancel();
        }
    }

    @SuppressWarnings("PMD")
    public static void start(final Context context, AlarmInstance instance,
            boolean inTelephoneCall) {
        Log.v("AlarmKlaxon.start()");
        // Make sure we are stop before starting
        stop(context);

        /*
         * M: If in call state, just vibrate the phone, and don't start the alarm.
         * For CMCC, should not affect the call.
         * vibrate VIBRATE_LENGTH milliseconds. @{
         */
        if (inTelephoneCall) {
          ///M: easy porting @{
            if (sICMCCSpecialSpecExtension == null) {
                PluginManager<ICMCCSpecialSpecExtension> pm
                        = PluginManager.<ICMCCSpecialSpecExtension>create(context,
                                ICMCCSpecialSpecExtension.class.getName());
                for (int i = 0,count = pm.getPluginCount();i < count;i++) {
                    Plugin<ICMCCSpecialSpecExtension> plugin = pm.getPlugin(i);
                    try {
                        ICMCCSpecialSpecExtension ext = plugin.createObject();
                        if (ext != null) {
                            sICMCCSpecialSpecExtension = ext;
                            break;
                        }
                    } catch (Plugin.ObjectCreationException ex) {
                        Log.e("can not create plugin object!");
                        ex.printStackTrace();
                    }
                }
            }
            if (sICMCCSpecialSpecExtension != null &&
                    sICMCCSpecialSpecExtension.isCMCCSpecialSpec()) {
                Log.v("CMCC special spec : do not vibrate when in call state ");
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    Log.v("vibrator starts,and vibrates:" + VIBRATE_LENGTH + " ms");
                    vibrator.vibrate(VIBRATE_LENGTH);
                }
            }
            return;
        }///@}

        if (!AlarmInstance.NO_RINGTONE_URI.equals(instance.mRingtone)) {
            Uri alarmNoise = instance.mRingtone;
            // Fall back on the default alarm if the database does not have an
            // alarm stored.
            if (alarmNoise == null) {
                alarmNoise = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (Log.LOGV) {
                    Log.v("Using default alarm: " + alarmNoise.toString());
                }
            }

            // TODO: Reuse mMediaPlayer instead of creating a new one and/or use RingtoneManager.
            sMediaPlayer = new MediaPlayer();
            sMediaPlayer.setOnErrorListener(new OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.e("Error occurred while playing audio. Stopping AlarmKlaxon.");
                    AlarmKlaxon.stop(context);
                    return true;
                }
            });

            try {
                ///M: if boot from power off alarm and use external ringtone,
                //just use the backup ringtone to play @{
                if (PowerOffAlarm.bootFromPoweroffAlarm()
                        && PowerOffAlarm.getNearestAlarmWithExternalRingtone(
                                context, instance) != null) {
                    setBackupRingtoneToPlay(context);
                ///@}
                } else {
                    sMediaPlayer.setDataSource(context, alarmNoise);
                }
                startAlarm(context, sMediaPlayer);
            } catch (IOException ex1) {
                Log.e("Failed to play the user ringtone", ex1);
                // The alarmNoise may be on the sd card which could be busy right
                // now. Use the default ringtone.
                try {
                    // Must reset the media player to clear the error state.
                    sMediaPlayer.reset();
                    ///M: change the fallback ringtone to defualt
                    Uri defaultRingtone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    sMediaPlayer.setDataSource(context, defaultRingtone);
                    startAlarm(context, sMediaPlayer);
                } catch (IOException ex2) {
                    // At this point we just don't play anything.
                    Log.e("Failed to play the default ringtone", ex2);
                    try {
                        // Must reset the media player to clear the error state.
                        sMediaPlayer.reset();
                        ///M: default ringtone play error, use the fallback ringtone
                        setDataSourceFromResource(context, sMediaPlayer, R.raw.fallbackring);
                        startAlarm(context, sMediaPlayer);
                    } catch (IOException ex3) {
                        // At this point we just don't play anything.
                        Log.e("Failed to play fallback ringtone", ex3);
                    }
                }
            }
        }

        if (instance.mVibrate) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_PATTERN, 0);
        }

        sStarted = true;
    }

    // Do the common stuff when starting the alarm.
    private static void startAlarm(Context context, MediaPlayer player) throws IOException {
        Log.v("startAlarm, check StreamVolume and requestAudioFocus");
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // do not play alarms if stream volume is 0 (typically because ringer mode is silent).
        if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setLooping(true);
            player.prepare();
            audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            player.start();
            Log.d("Play successful, StreamVolume != 0");
        }
    }

    private static void setDataSourceFromResource(Context context, MediaPlayer player, int res)
            throws IOException {
        AssetFileDescriptor afd = context.getResources().openRawResourceFd(res);
        if (afd != null) {
            player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
        }
    }

    /** M: @{
     * set the backup ringtone for power off alarm to play
     */
    private static void setBackupRingtoneToPlay(Context context) throws IOException {
        String ringtonePath = null;
        java.io.File dir = context.getFilesDir();
        Log.v("base dir: " + dir.getAbsolutePath());
        File[] files = dir.listFiles();
        if (files != null && files.length > 0) {
            ringtonePath = files[0].getAbsolutePath();
        }
        Log.v("setBackupRingtoneToPlay ringtone: " + ringtonePath);
        if (!TextUtils.isEmpty(ringtonePath)) {
            File file = new File(ringtonePath);
            if (file != null && file.exists() && file.getTotalSpace() > 0) {
                java.io.FileInputStream fis = null;
                try {
                    fis = new java.io.FileInputStream(file);
                    sMediaPlayer.setDataSource(fis.getFD());
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }
        }
    }
}
