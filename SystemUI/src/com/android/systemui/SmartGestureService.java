/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.systemui;

import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.media.RingtoneManager;
import android.view.MotionEvent;

import java.lang.Exception;
import java.lang.Override;
import java.lang.Runnable;

/**
 * hsp 2016-08-18 : Add for smart gesture and call flip mute
 *
 * This service start when phone boot complete
 */

public class SmartGestureService extends Service {
    private static final String TAG = "zolen";
    private static final String ACTION_PHONE_STATE_CHANGED = "android.intent.action.PHONE_STATE";
    private static final String ACTION_ALARM_ALERT = "com.android.deskclock.ALARM_ALERT";
    private static final String ALARM_DISMISS_ACTION = "com.android.deskclock.ALARM_DISMISS";
    private static final String ACTION_MUSIC_ISPLAYING = "action.music.is.playing";
    private static final String ACTION_GALLERY_IS_RESUME = "com.qucii.gallery.is.resume";
    private static final String ACTION_DIALPAD_RESUME = "com.qucii.dial.resume";
    private static final String ACTION_CONTACT_RESUME = "com.qucii.contact.resume";
    private static final String ACTION_MMS_RESUME = "com.qucii.mms.resume";
    private static final String ACTION_MUSIC_SETTINGS_CHANGE = "com.qucii.music.settings";
    private static final String ACTION_ACCEPT_RINGING_CALL = "com.qucii.accept.ringing.call";
    private static final String ACTION_SILENT_RINGING_CALL = "com.qucii.silent.ringing.call";

    private AudioManager mAudioMgr;
    private TelephonyManager tm;
    private Sensor mProximitySensor;
    private SensorManager mSensorMgr;
    private Sensor mGravitySensor;
    private boolean mGravitySensorStarted = false;
    private boolean isUpward = true;
    private int mRingerMode;
    public static final double GRAVITY = 9.81;
    public boolean isFirstDegreeValue = false;
    public boolean isMute = false;
    private boolean isAlarmAlert = false;
    private String mPhoneNumber;

    private boolean mProximitySensorStarted = false;
    private boolean isGalleryResume = false;
    private boolean mCanSmartDialed = false;
    private boolean mCanSmartAnswerCall= false;
    private boolean mCanSilentCall= false;
    private boolean isScreenOn = true;
    private boolean isMusicSettingOpen = false;
    private boolean isCallRinging = false;
    private boolean isPSensorBlocked = false;
    //When start call the first time, we set it true, if the call is connected successfully,
    //we set it false instead call again, until call is idle, we set it back to true,
    //if the call is not connected, we use psensor to set back to true;
    private boolean mPSensorNear = false;
    private boolean mGSensorChanged = false;
    private boolean mCanCallNow = true;
    private boolean mCanDialNow = false;
    private boolean mCall = false;
    private boolean mCanAnswerCall = false;
    private boolean isCallStateChanged = false;
	private boolean isBringUp = false;
    private static int gSensorChangedCount = 0;
    private static double[][] g_Value = { {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0},
                                            {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0},
                                            {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0}, {0.0, 0.0, 0.0} };
    private static long[] gSensorTimestamp = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static boolean oneAction = false;
    private static boolean twoAction = false;
    private static boolean threeAction = false;
    private static double oneActionTimesStep = 0;
    private static double twoActionTimesStep = 0;
    private static double threeActionTimesStep = 0;
    
    @Override
    public void onCreate() {
        super.onCreate();
        tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mSensorMgr = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mGravitySensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mProximitySensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        //Init broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PHONE_STATE_CHANGED);
        filter.addAction(ACTION_ALARM_ALERT);
        filter.addAction(ACTION_GALLERY_IS_RESUME);
        filter.addAction(ACTION_MUSIC_ISPLAYING);
        filter.addAction(ACTION_MUSIC_SETTINGS_CHANGE);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(ACTION_DIALPAD_RESUME);
        filter.addAction(ACTION_CONTACT_RESUME);
        filter.addAction(ACTION_MMS_RESUME);
        this.registerReceiver(mReceiver, filter);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            String action = i.getAction();
            if(action.equals(ACTION_PHONE_STATE_CHANGED)) {
                isFirstDegreeValue = false;
                isCallStateChanged = true;
                Bundle bundle = i.getExtras();
                if (bundle == null) {
                    return;
                }
                if(gSensorChangedCount >= 15)
                {
                    gSensorChangedCount = 0;
                }
                String state = bundle.getString(TelephonyManager.EXTRA_STATE);
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    isCallRinging = true;
                    boolean silentCall = (Settings.System.getInt(getContentResolver(),
                            "call_flip_mute", 0) == 1);
                    mRingerMode = mAudioMgr.getRingerMode();
                    if (mRingerMode == AudioManager.RINGER_MODE_NORMAL && silentCall) {
                        mCanSilentCall = true;
                    } else {
                        mCanSilentCall = false;
                    }

                    boolean answer = (Settings.System.getInt(getContentResolver(),
                            "smart_answer", 0) == 1);
                    if (answer) {
                        mCanSmartAnswerCall = true;
                    } else {
                        mCanSmartAnswerCall = false;
                    }
                    
                } else {
                    isCallRinging = false;
                }
                if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    mCanCallNow = true;
                    isCallStateChanged = false;
                    mCanSmartAnswerCall = false;
                    mCanSilentCall = false;
                }                
                updateGravitySensor();
                updateProximitySensor();          
            }
            else if (action.equals(ACTION_ALARM_ALERT)) {
                isFirstDegreeValue = false;
                boolean alarmMute = (Settings.System.getInt(getContentResolver(),
                        "alarm_flip_mute", 0) == 1);
                if (alarmMute) {
                    isAlarmAlert = true;
                    updateGravitySensor();
                }
            }
            else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
                updateProximitySensor();
                updateGravitySensor();
            }
            else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                //When screen off, close all sensor listener
                isScreenOn = false;
                updateProximitySensor();
                updateGravitySensor();
            }
            else if (action.equals(ACTION_GALLERY_IS_RESUME)) {
                boolean switchPhoto = (Settings.System.getInt(getContentResolver(),
                        "air_switch_photo", 0) == 1);
                if (switchPhoto) {
                    boolean started = i.getBooleanExtra("started", false);
                    if (started) {
                        isGalleryResume = true;
                    } else {
                        isGalleryResume = false;
                    }
                    updateProximitySensor();
                }
            }
            else if (action.equals(ACTION_MUSIC_ISPLAYING)) {
                boolean switchMusic = (Settings.System.getInt(getContentResolver(),
                        "air_switch_music", 0) == 1);
                Log.d(TAG, "Receive from AudioManager, music is playing");
                if (switchMusic) {
                    isMusicSettingOpen = true;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            updateProximitySensor();
                        }
                    }, 2000);

                }
            }
            else if (action.equals(ACTION_MUSIC_SETTINGS_CHANGE)) {
                boolean switchMusic = (Settings.System.getInt(getContentResolver(),
                        "air_switch_music", 0) == 1);
                if (switchMusic) {
                    isMusicSettingOpen = true;
                } else {
                    isMusicSettingOpen = false;
                }
                updateProximitySensor();
            }
            //When we are in call log page, detail of contacts and mms, we can use smart call
            else if (action.equals(ACTION_DIALPAD_RESUME)
                    || action.equals(ACTION_CONTACT_RESUME)
                    || action.equals(ACTION_MMS_RESUME)) {
                boolean dial = (Settings.System.getInt(getContentResolver(), "smart_dial", 0) == 1);
                if (dial) {
                    boolean started = i.getBooleanExtra("started", false);
                    String num = i.getStringExtra("number");
                    if (num != null) {
                        mPhoneNumber = num.trim();
                    } else {
                        mPhoneNumber = null;
                    }

                    if (started) {
                        mCanSmartDialed = true;
                    } else {
                        mCanSmartDialed = false;
                    }
                    updateProximitySensor();
                    updateGravitySensor();
                }
            }

            
        }
    };

    private void updateProximitySensor() {
        boolean startSensor = !mProximitySensorStarted
                && isScreenOn
                && (isGalleryResume
                    || (isMusicPlaying() && isMusicSettingOpen)
                    || mCanSmartDialed
                    || mCanSmartAnswerCall);
        if (startSensor) {
            Log.d(TAG, "Start PSensor");
            mSensorMgr.registerListener(mSensorEventListener,
                    mProximitySensor,
                    SensorManager.SENSOR_DELAY_UI);
            mProximitySensorStarted = true;
        }
        boolean stopSensor = mProximitySensorStarted
                && !isGalleryResume
                && (!isMusicPlaying() || !isMusicSettingOpen)
                && !mCanSmartDialed
                && !mCanSmartAnswerCall;
        if (mProximitySensorStarted && !isScreenOn) {
            stopSensor = true;
        }
        if (stopSensor) {
            Log.d(TAG, "Stop PSensor");
            mSensorMgr.unregisterListener(mSensorEventListener, mProximitySensor);
            mProximitySensorStarted = false;
        }
    }

    private boolean isMusicPlaying() {
        return mAudioMgr.isMusicActive();
    }

    private void updateGravitySensor() {
        boolean start = !mGravitySensorStarted
                && isScreenOn
                && (mCanSilentCall || mCanSmartDialed || isAlarmAlert || mCanSmartAnswerCall);
        if (start) {
            Log.d(TAG, "Start Gravity Sensor");
            
            gSensorChangedCount = 0;
            isBringUp = false;
            threeActionTimesStep = 0;
            twoActionTimesStep = 0;
            oneActionTimesStep = 0;
            oneAction = false;
            twoAction = false;
            threeAction = false;
            mSensorMgr.registerListener(mSensorEventListener,
                    mGravitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mGravitySensorStarted = true;
        }

        boolean stop = mGravitySensorStarted
                && !mCanSilentCall
                && !mCanSmartAnswerCall
                && !mCanSmartDialed
                && !isAlarmAlert;
        if (mGravitySensorStarted && !isScreenOn) {
            stop = true;
        }
        if(stop) {
            Log.d(TAG, "Stop Gravity Sensor");
            mSensorMgr.unregisterListener(mSensorEventListener, mGravitySensor);
            mGravitySensorStarted = false;
        }
    }


    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                gravitySensorChanged(event);
            }
            else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float thisVal = event.values[0];
                isPSensorBlocked = false;
                if (thisVal == 0) { // Has blocked the PSensor
                    isPSensorBlocked = true;
                    mPSensorNear = true;
                    if (isGalleryResume) {
                        movePhoto();
                    } else if (isMusicPlaying() && isMusicSettingOpen) {
                        controlMusic();
                    } else {
                        updateProximitySensor();
                    }
                }
                else
                {
                    mPSensorNear = false;
                 }   
                //If the call is not connected and phone state not change at all(we can't receive broadcast),
                //we should reset mCanCallNow to true here for ready to make another call
                if (!isCallStateChanged && !mCanCallNow) {
                    mCanCallNow = true;
                }
            }
            
            if(mPSensorNear &&  !isAlarmAlert && mCanCallNow && !mCanSmartAnswerCall && isBringUp &&mGSensorChanged)
            {
                mCanCallNow = false;
                mPSensorNear = false;
                isBringUp = false;
                threeActionTimesStep = 0;
                twoActionTimesStep = 0;
                oneActionTimesStep = 0;
                oneAction = false;
                twoAction = false;
                threeAction = false;
                for(int k = 0; k <= 14; k++ ){
                    g_Value[k][0] = 0.0;
                    g_Value[k][1] = 0.0;
                    g_Value[k][2] = 0.0;
                } 
                gSensorChangedCount = 0;
                dial();
            }
            else if(mPSensorNear && !isAlarmAlert && mCanSmartAnswerCall && isBringUp && mGSensorChanged)
            {
                mCanSmartAnswerCall = false;
                mPSensorNear = false;
                isBringUp = false;
                threeActionTimesStep = 0;
                twoActionTimesStep = 0;
                oneActionTimesStep = 0;
                oneAction = false;
                twoAction = false;
                threeAction = false;
                for(int l = 0; l <= 14; l++ )
                {
                    g_Value[l][0] = 0.0;
                    g_Value[l][1] = 0.0;
                    g_Value[l][2] = 0.0;
                } 
                gSensorChangedCount = 0;
                sendBroadcastToAcceptCall();
            }
            
        }
    };
 

    private void movePhoto() {
        Log.d(TAG, "Switch to next photo");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Instrumentation inst = new Instrumentation();
                inst.sendPointerSync(MotionEvent.obtain(
                        SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_DOWN, 500, 500, 0));
                inst.sendPointerSync(MotionEvent.obtain(
                        SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_MOVE, 450, 500, 0));
                inst.sendPointerSync(MotionEvent.obtain(
                        SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                        MotionEvent.ACTION_UP, 450, 500, 0));
            }
        }).start();
    }

    private void controlMusic() {
        Log.d(TAG, "Switch to next music");
        try {
            Runtime.getRuntime().exec("input keyevent KEYCODE_MEDIA_NEXT");
        } catch (Exception e) {

        }
    }

    private void gravitySensorChanged(SensorEvent event) {
	    double x = event.values[SensorManager.DATA_X];
        double y = event.values[SensorManager.DATA_Y];
        double z = event.values[SensorManager.DATA_Z];

        if (!isFirstDegreeValue) {
            if (z > 0) {
                isUpward = true;
            } else {
                isUpward = false;
            }
            isFirstDegreeValue = true;
        }
        boolean canUptodown = (z > -10 && z < -8) && (x > -3 && x < 3) && (y > -3 && y < 3);
        boolean canDowntoup = (z > 8 && z < 10) && (x > -3 && x < 3) && (y > -3 && y < 3);
        if (isUpward && canUptodown) { //up to down
            isMute = true;
            isUpward = false;
        }
        else if (!isUpward && canDowntoup) { //down to up
            isMute = true;
            isUpward = true;
        }

        //When we flip the device, mute the call or close the alarm
        if (isMute) {
            muteCallAndAlarm();
        }

        //Put the phone across the ear, we can make a call or answer the new call.
        mCall = isPSensorBlocked &&
                (/*left hand*/(x > 5 && x < 8.5) || /*right hand*/(x < -5 && x > -8)) &&
                (y > 5 && y < 8.5) &&
                (z < 2 && z > -2);
        if(mCall)
        {
            mGSensorChanged = true;
        }else{
            mGSensorChanged = false;
        }
		
        if(gSensorChangedCount == 15){
            long timesstap = gSensorTimestamp[14] - gSensorTimestamp[0];
        }
            
        if((gSensorChangedCount == 15) && ((gSensorTimestamp[14] - gSensorTimestamp[0] >= 550000000) && (gSensorTimestamp[14] - gSensorTimestamp[0] <= 1000000000))){
            for(int ll = 0; ll <= 14; ll++){
                if(g_Value[ll][1] > 7.0)
                {
                    threeActionTimesStep = gSensorTimestamp[ll];
                    threeAction = true;
                }else if( (g_Value[ll][1] > 3.0 && g_Value[ll][1] < 7.0) && (g_Value[ll][2] > 3.0 && g_Value[ll][2] < 9.0) )
                {
                    twoActionTimesStep = gSensorTimestamp[ll];
                    twoAction = true;
                }else if( ((g_Value[ll][0] < -4.0 && g_Value[ll][0] > -8.0) || (g_Value[ll][0] > 3.5 && g_Value[ll][0] < 8.0)) && (g_Value[ll][1] > 3.0 && g_Value[ll][1] < 7.0))
                {
                    oneActionTimesStep = gSensorTimestamp[ll];
                    oneAction = true;
                }
            }

            if(threeAction && (twoAction || oneAction) && (threeActionTimesStep > oneActionTimesStep) && (threeActionTimesStep > oneActionTimesStep))
            {
                isBringUp = true;
            }else{
                isBringUp = false;
            }
            gSensorChangedCount = 0;
        }else if((gSensorChangedCount >= 0) && (gSensorChangedCount <= 14)){ 
            g_Value[gSensorChangedCount][0] = event.values[SensorManager.DATA_X];
            g_Value[gSensorChangedCount][1] = event.values[SensorManager.DATA_Y];
            g_Value[gSensorChangedCount][2] = event.values[SensorManager.DATA_Z];
            gSensorTimestamp[gSensorChangedCount] = event.timestamp;
            gSensorChangedCount++;
        }else if(gSensorChangedCount == 15)
        {
            gSensorChangedCount = 0;
        }
        
    }
        
    private void dial() {
        if (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE){
            return;
        }
        Log.d(TAG, "Smart call begin, phone number is : " + mPhoneNumber);
        Intent i;
        if(PhoneNumberUtils.isEmergencyNumber(mPhoneNumber)) {
            i = new Intent(Intent.ACTION_CALL_EMERGENCY, Uri.parse("tel:" + mPhoneNumber));
        } else {
            i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + mPhoneNumber));
        }
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void muteCallAndAlarm() {
        if (isAlarmAlert) {
            sendBroadcastToCloseAlarm();
            isAlarmAlert = false;
            updateGravitySensor();
        } else if (mCanSilentCall) {
            //Mute the call ringing
            sendBroadcastToSilentCall();
            mCanSilentCall = false;
            updateGravitySensor();
        }
        isMute = false;
    }

    private void sendBroadcastToCloseAlarm() {
        Intent i = new Intent();
        i.setAction(ALARM_DISMISS_ACTION);
        this.sendBroadcast(i);
    }

    private void sendBroadcastToAcceptCall() {
       if (android.provider.Settings.System.getInt(this.getContentResolver(), "smart_answer", 0)!=1){
          return;
       }
        Intent i = new Intent();
        i.setAction(ACTION_ACCEPT_RINGING_CALL);
        this.sendBroadcast(i);
    }

    private void sendBroadcastToSilentCall() {
        Intent i = new Intent();
        i.setAction(ACTION_SILENT_RINGING_CALL);
        this.sendBroadcast(i);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
