/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.statusbar.policy;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.util.Log;

import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.statusbar.policy.ZenModeController.Callback;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * @Description:
 * @csdnblog   http://blog.csdn.net/mare_blue
 * @author  mare
 * @date 2016-11-21
 * @time  ÏÂÎç4:59:17
 */
public class ProfilesController {
	private static final String TAG = "ProfilesController";
	private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
	public static final int RINGER_MODE_UNKNOWN = -1;
	
	private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
	private final Context mContext;
	private final GlobalSetting mModeSetting;
	

	public ProfilesController(Context context, Handler handler) {
		mContext = context;
		mModeSetting = new GlobalSetting(mContext, handler,
				Global.QS_PROFILE_MODE) {
			@Override
			protected void handleValueChanged(int value) {
				fireRingerChanged(value);
			}
		};
	}

	public void updateRingerMode(int value) {
		mModeSetting.setValue(value);
	}
	
	public int getRingerMode(){
		return mModeSetting.getValue();
	}
	
	public boolean isRingerSilent(){
		return mModeSetting.getValue() == AudioManager.RINGER_MODE_SILENT;
	}
	
	public static interface Callback{
		void ringerModeChanged(int mode);
	}
	
    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(Callback callback) {
        mCallbacks.remove(callback);
    }
    
    private void fireRingerChanged(int mode) {
        for (Callback cb : mCallbacks) {
            cb.ringerModeChanged(mode);
        }
    }
	
}
