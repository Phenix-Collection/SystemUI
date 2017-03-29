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
 * limitations under the License
 */

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.ProfilesController;

/** Quick settings tile: Control Audio Mode **/
public class ProfileTile extends QSTile<QSTile.BooleanState> {

    private AudioManager mAudioManager;
    private ProfilesController mController;
    private boolean mListening;

    public ProfileTile(Host host) {
        super(host);
        mController = mHost.getProfilesController();
        mAudioManager = (AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        IntentFilter filter = new IntentFilter();
        // 添加监听震动 静音的改变 by yangfan 
        filter.addAction(AudioManager.RINGER_MODE_CHANGED_ACTION);
        filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
        filter.addAction("action_dnd_update");
     // 添加监听震动 静音的改变 by yangfan  end
        mContext.registerReceiver(mReceiver, filter);
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshState();

                }
            }, 0);

        }
    };

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    protected void handleClick() {
        int ringMode = mAudioManager.getRingerMode();
        int nextMode;
        if (ringMode == AudioManager.RINGER_MODE_NORMAL) {
            nextMode = AudioManager.RINGER_MODE_VIBRATE;
        } else if (ringMode == AudioManager.RINGER_MODE_VIBRATE) {
            nextMode = AudioManager.RINGER_MODE_SILENT;
        } else if (ringMode == AudioManager.RINGER_MODE_SILENT) {
            nextMode = AudioManager.RINGER_MODE_NORMAL;
            //weiliji add begin    
            Intent i = new Intent("com.qucci.switch_slient");
            i.putExtra("qucci_switch_slient", true);
            mContext.sendBroadcast(i);
            //weiliji add end
        } else {    
            nextMode = AudioManager.RINGER_MODE_NORMAL;
        }
        updateRingerMode(nextMode);
        refreshState();
    }

    void updateRingerMode(int mode) {
        mAudioManager.setRingerMode(mode , true);
    }

    @Override
    public int getMetricsCategory() {
        return 240;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
        int ringMode = mAudioManager.getRingerMode();
        if (ringMode == AudioManager.RINGER_MODE_NORMAL) {
            state.label = mHost.getContext()
                    .getString(R.string.qs_tile_ringing);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_profile_ringing);
        } else if (ringMode == AudioManager.RINGER_MODE_VIBRATE) {
            state.label = mHost.getContext()
                    .getString(R.string.qs_tile_vibrate);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_profile_vibrate);
        } else if (ringMode == AudioManager.RINGER_MODE_SILENT) {
            state.label = mHost.getContext().getString(R.string.qs_tile_silent);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_profile_silent);
        } else {
            state.label = mHost.getContext()
                    .getString(R.string.qs_tile_ringing);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_profile_ringing);
        }
        mController.updateRingerMode(ringMode);
        Log.e(TAG, "ringMode : "+ ringMode);
        int onOrOffId = state.value ? R.string.qs_tile_ringing
                : R.string.qs_tile_ringing;
        state.contentDescription = mContext.getString(onOrOffId);
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext
                    .getString(R.string.accessibility_quick_settings_flashlight_changed_on);
        } else {
            return mContext
                    .getString(R.string.accessibility_quick_settings_flashlight_changed_off);
        }
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening)
            return;
        mListening = listening;
        if (mListening) {
            mController.addCallback(mRingerCallback);
        } else {
            mController.removeCallback(mRingerCallback);
        }
    }

    private final ProfilesController.Callback mRingerCallback = new ProfilesController.Callback() {
        @Override
        public void ringerModeChanged(int mode) {
            refreshState(mode);
        }
    };

}
