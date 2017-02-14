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

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.qs.QSTileView;
import com.android.systemui.qs.SignalTileView;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController;
import com.android.systemui.statusbar.policy.NetworkController.MobileDataController.DataUsageInfo;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.SignalCallbackAdapter;

/** Quick settings tile: Cellular **/
public class CellularTile extends QSTile<QSTile.BooleanState> {
// modified by yangfan 
    private static final Intent MOBILE_NETWORK_SETTINGS = new Intent(
            Intent.ACTION_MAIN).setComponent(new ComponentName(
            "com.android.phone", "com.android.phone.MobileNetworkSettings"));
    private static final Intent MOBILE_NETWORK_SETTINGS_MSIM = new Intent(
            "com.android.settings.sim.SIM_SUB_INFO_SETTINGS");
// modified by yangfan
    private static final String ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED = "org.codeaurora.intent.action.ACTION_UICC_MANUAL_PROVISION_STATUS_CHANGED";

    private final TelephonyManager mTelephonyManager;
    private final NetworkController mController;
    private final MobileDataController mDataController;
    private final CellularDetailAdapter mDetailAdapter;
    private boolean mIsDataOn = false;
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private PhoneStateListener mPhoneStateListener;
    private int mIconDataConnected = R.drawable.qucii_ic_qs_mobiledata_connected;
    private int mIconDataUnConnected = R.drawable.qucii_ic_qs_mobiledata_unconnected;
	
		// added by yangfan
    private final CellSignalCallback mSignalCallback = new CellSignalCallback();
    private SubscriptionInfo mSubscriptionInfo;
    private SubscriptionManager mSubscriptionManager;
		// added by yangfan
		
    public CellularTile(Host host) {
        super(host);
        mController = host.getNetworkController();
        mDataController = mController.getMobileDataController();
        mDetailAdapter = new CellularDetailAdapter();
		// added by yangfan
        mTelephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = SubscriptionManager.from(mContext);
		// added by yangfan
    }

    @Override
    protected QSTile.BooleanState newTileState() {
        return new BooleanState();
    }		// modified by yangfan

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mController.addSignalCallback(mSignalCallback);
        } else {
            mController.removeSignalCallback(mSignalCallback);
        }
    }

    // @Override
    // public QSTileView createTileView(Context context) {
    //     return new SignalTileView(context);
    // }// modified by yangfan

    @Override
    protected void handleClick() {
        if (!mState.enableClick) return ;
        mSubscriptionInfo  = mSubscriptionManager.getDefaultDataSubscriptionInfo();
//        final boolean ecbMode = SystemProperties.getBoolean(
//                TelephonyProperties.PROPERTY_INECM_MODE, false);
        int slot = mSubscriptionInfo.getSimSlotIndex();
        int subId = mSubscriptionInfo.getSubscriptionId();
        boolean wasEnabled = mDataController.isMobileDataEnabled();
        boolean isEnable = mTelephonyManager.getDataEnabled(subId);
        Log.i(TAG,"CellularTile >> wasEnabled : "+ wasEnabled +",isEnable : " + isEnable);
        MetricsLogger.action(mContext, getMetricsCategory(), !isEnable);
//        mDataController.setMobileDataEnabled(!wasEnabled);
        mTelephonyManager.setDataEnabled(subId, !isEnable);
        mState.value = !isEnable;
        refreshState();
    }// modified by yangfan

    @Override
    protected void handleUpdateState(QSTile.BooleanState state, Object arg) {
        Log.i(TAG,"state : " + state.toString());
         boolean visible = mController.hasMobileDataFeature();
        mSubscriptionInfo = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        state.enableClick = visible && mSubscriptionInfo != null;
        final Resources r = mContext.getResources();
        state.label = r.getString(R.string.quick_settings_cellular_detail_title);
        if (!visible || mSubscriptionInfo == null) {
            state.icon = ResourceIcon.get(mIconDataUnConnected);
            return ;
        }
        int subId = mSubscriptionInfo.getSubscriptionId();
        boolean wasEnabled = mDataController.isMobileDataEnabled();
        boolean isEnable = mTelephonyManager.getDataEnabled(subId);
        state.visible = true;
        CallbackInfo cb = (CallbackInfo) arg;
        if (cb == null) {
            cb = mSignalCallback.mInfo;
        }
        int iconId = 0;
//        if (cb.noSim) {
//            iconId = mIconDataUnConnected;
//        }else if (!cb.enabled || cb.airplaneModeEnabled) {
//            iconId = mIconDataUnConnected;
//        }else {
//            iconId = mIconDataConnected;
//        }
        Log.i(TAG,"enableClick : " + state.enableClick);
        iconId = isEnable && state.enableClick && cb.enabled && !cb.airplaneModeEnabled /*&& state.visible*/ ? mIconDataConnected : mIconDataUnConnected;
        state.icon = ResourceIcon.get(iconId);
        state.autoMirrorDrawable = !cb.noSim;
        // state.label = cb.enabled
        // ? removeTrailingPeriod(cb.enabledDesc)
        // : r.getString(R.string.quick_settings_rssi_emergency_only);

        final String signalContentDesc = cb.enabled
                && (cb.mobileSignalIconId > 0) ? cb.signalContentDescription
                : r.getString(R.string.accessibility_no_signal);
        final String dataContentDesc = cb.enabled && (cb.dataTypeIconId > 0)
                && !cb.wifiEnabled ? cb.dataContentDescription : r
                .getString(R.string.accessibility_no_data);
        state.contentDescription = r.getString(
                R.string.accessibility_quick_settings_mobile,
                signalContentDesc, dataContentDesc, state.label);

    }// modified by yangfan

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_CELLULAR;
    }

    // Remove the period from the network name
    public static String removeTrailingPeriod(String string) {
        if (string == null) return null;
        final int length = string.length();
        if (string.endsWith(".")) {
            return string.substring(0, length - 1);
        }
        return string;
    }

    private static final class CallbackInfo {
        boolean enabled;
        boolean wifiEnabled;
        boolean airplaneModeEnabled;
        int mobileSignalIconId;
        String signalContentDescription;
        int dataTypeIconId;
        String dataContentDescription;
        boolean activityIn;
        boolean activityOut;
        String enabledDesc;
        boolean noSim;
        boolean isDataTypeIconWide;
        String networkName;// added by yangfan
        boolean isForbidden;// added by yangfan
    }

    private final class CellSignalCallback extends SignalCallbackAdapter {
        private final CallbackInfo mInfo = new CallbackInfo();
        @Override
        public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
                boolean activityIn, boolean activityOut, String description) {
            mInfo.wifiEnabled = enabled;
            refreshState(mInfo);
        }

        @Override
        public void setMobileDataIndicators(IconState statusIcon,
                                            IconState qsIcon, int statusType,boolean showDataIcon, int qsType,
                                            boolean activityIn, boolean activityOut, int dataActivityId,
                                            int mobileActivityId, int stackedDataIcon,
                                            int stackedVoiceIcon, String typeContentDescription,
                                            String description, boolean isWide, int subId,String networkName,boolean showNetworkClass) {// added by yangfan
            if (qsIcon == null) {
                // Not data sim, don't display.
                return;
            }
            mInfo.enabled = qsIcon.visible;
            mInfo.mobileSignalIconId = qsIcon.icon;
            mInfo.signalContentDescription = qsIcon.contentDescription;
            mInfo.dataTypeIconId = qsType;
            mInfo.dataContentDescription = typeContentDescription;
            mInfo.activityIn = activityIn;
            mInfo.activityOut = activityOut;
            mInfo.enabledDesc = description;
            mInfo.isDataTypeIconWide = qsType != 0 && isWide;
            mInfo.networkName = networkName;// added by yangfan
            mInfo.isForbidden = showNetworkClass;// added by yangfan
            refreshState(mInfo);
        }

        @Override
        public void setNoSims(boolean show) {
            mInfo.noSim = show;
            if (mInfo.noSim) {
                // Make sure signal gets cleared out when no sims.
                mInfo.mobileSignalIconId = 0;
                mInfo.dataTypeIconId = 0;
                // Show a No SIMs description to avoid emergency calls message.
                mInfo.enabled = true;
                mInfo.enabledDesc = mContext.getString(
                        R.string.keyguard_missing_sim_message_short);
                mInfo.signalContentDescription = mInfo.enabledDesc;
            }
            refreshState(mInfo);
        }

        @Override
        public void setIsAirplaneMode(IconState icon) {
            mInfo.airplaneModeEnabled = icon.visible;
            refreshState(mInfo);
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            mDetailAdapter.setMobileDataEnabled(enabled);
        }
    };

    private final class CellularDetailAdapter implements DetailAdapter {

        @Override
        public int getTitle() {
            return R.string.quick_settings_cellular_detail_title;
        }

        @Override
        public Boolean getToggleState() {
            return mDataController.isMobileDataSupported()
                    ? mDataController.isMobileDataEnabled()
                    : null;
        }

        @Override
        public Intent getSettingsIntent() {
            return /**CELLULAR_SETTINGS**/MOBILE_NETWORK_SETTINGS;// modified by yangfan 
        }

        @Override
        public void setToggleState(boolean state) {
            MetricsLogger.action(mContext, MetricsLogger.QS_CELLULAR_TOGGLE, state);
            mDataController.setMobileDataEnabled(state);
        }

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.QS_DATAUSAGEDETAIL;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final DataUsageDetailView v = (DataUsageDetailView) (convertView != null
                    ? convertView
                    : LayoutInflater.from(mContext).inflate(R.layout.data_usage, parent, false));
            final DataUsageInfo info = mDataController.getDataUsageInfo();
            if (info == null) return v;
            v.bind(info);
            return v;
        }

        public void setMobileDataEnabled(boolean enabled) {
            fireToggleStateChanged(enabled);
        }
    }
	
// added by yangfan 
    @Override
    protected void handleLongClick() {
        mHost.startActivityDismissingKeyguard(MOBILE_NETWORK_SETTINGS);
    }
// added by yangfan 
}
