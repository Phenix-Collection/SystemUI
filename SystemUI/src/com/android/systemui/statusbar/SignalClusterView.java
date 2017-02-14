/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.NetworkController.IconState;
import com.android.systemui.statusbar.policy.NetworkControllerImpl;
import com.android.systemui.statusbar.policy.SecurityController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;

import java.util.ArrayList;
import java.util.List;

// Intimately tied to the design of res/layout/signal_cluster_view.xml
public class SignalClusterView
        extends LinearLayout
        implements NetworkControllerImpl.SignalCallbackExtended,
        SecurityController.SecurityControllerCallback, Tunable {

    static final String TAG = "SignalClusterView";
    static final boolean DEBUG = true;

    private static final String SLOT_AIRPLANE = "airplane";
    private static final String SLOT_MOBILE = "mobile";
    private static final String SLOT_WIFI = "wifi";
    private static final String SLOT_ETHERNET = "ethernet";

    NetworkControllerImpl mNC;
    SecurityController mSC;

    private boolean mNoSimsVisible = false;
    private boolean mVpnVisible = false;
    private boolean mEthernetVisible = false;
    private int mEthernetIconId = 0;
    private int mLastEthernetIconId = -1;
    private boolean mWifiVisible = false;
    private boolean mImsOverWifi = false;
    private int mWifiStrengthId = 0, mWifiActivityId = 0;
    private int mLastWifiStrengthId = -1, mLastWifiActivityId = -1;
    private boolean mIsAirplaneMode = false;
    private int mAirplaneIconId = 0;
    private int mLastAirplaneIconId = -1;
    private String mAirplaneContentDescription;
    private String mWifiDescription;
    private String mEthernetDescription;
    private ArrayList<PhoneState> mPhoneStates = new ArrayList<PhoneState>();
    private int mIconTint = Color.WHITE;
    private float mDarkIntensity;
    private int mNoSimsIcon;

    ViewGroup mEthernetGroup, mWifiGroup;
    View mNoSimsCombo;
    ImageView mVpn, mEthernet, mWifi, mAirplane,
                /**mNoSims,**/ mEthernetDark, mWifiDark,/** mNoSimsDark,**/ mImsOverWifiImageView;// modified by yangfan
    ImageView mWifiActivity;
    TextView mNoSims, mNoSimsDark;// added by yangfan
    View mWifiAirplaneSpacer;
    View mWifiSignalSpacer;
    LinearLayout mMobileSignalGroup;

    private int mWideTypeIconStartPadding;
    private int mSecondaryTelephonyPadding;
    private int mEndPadding;
    private int mEndPaddingNothingVisible;

    private boolean mBlockAirplane;
    private boolean mBlockMobile;
    private boolean mBlockWifi;
    private boolean mBlockEthernet;
    private boolean mBlockNetworkLabel;// added by yangfan

    public SignalClusterView(Context context) {
        this(context, null);
    }

    public SignalClusterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignalClusterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (!StatusBarIconController.ICON_BLACKLIST.equals(key)) {
            return;
        }
        ArraySet<String> blockList = StatusBarIconController.getIconBlacklist(newValue);
        boolean blockAirplane = blockList.contains(SLOT_AIRPLANE);
        boolean blockMobile = blockList.contains(SLOT_MOBILE);
        boolean blockWifi = blockList.contains(SLOT_WIFI);
        boolean blockEthernet = blockList.contains(SLOT_ETHERNET);

        if (blockAirplane != mBlockAirplane || blockMobile != mBlockMobile
                || blockEthernet != mBlockEthernet || blockWifi != mBlockWifi) {
            mBlockAirplane = blockAirplane;
            mBlockMobile = blockMobile;
            mBlockEthernet = blockEthernet;
            mBlockWifi = blockWifi;
            // Re-register to get new callbacks.
            mNC.removeSignalCallback(this);
            mNC.addSignalCallback(this);
        }
    }

    public void setNetworkController(NetworkControllerImpl nc) {
        if (DEBUG) Log.d(TAG, "NetworkController=" + nc);
        mNC = nc;
    }

    public void setSecurityController(SecurityController sc) {
        if (DEBUG) Log.d(TAG, "SecurityController=" + sc);
		//added by yangfan 
        if (sc == null && mSC != null) {
            mSC.removeCallback(this);
        }
		//added by yangfan 
        mSC = sc;
        if (mSC != null) {
            mSC.addCallback(this);
            mVpnVisible = mSC.isVpnEnabled();
        }// judge it != null by yangfan
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mWideTypeIconStartPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.wide_type_icon_start_padding);
        mSecondaryTelephonyPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.secondary_telephony_padding);
        mEndPadding = getContext().getResources().getDimensionPixelSize(
                R.dimen.signal_cluster_battery_padding);
        mEndPaddingNothingVisible = getContext().getResources().getDimensionPixelSize(
                R.dimen.no_signal_cluster_battery_padding);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mVpn            = (ImageView) findViewById(R.id.vpn);
        mEthernetGroup  = (ViewGroup) findViewById(R.id.ethernet_combo);
        mEthernet       = (ImageView) findViewById(R.id.ethernet);
        mEthernetDark   = (ImageView) findViewById(R.id.ethernet_dark);
        mWifiGroup      = (ViewGroup) findViewById(R.id.wifi_combo);
        mWifi           = (ImageView) findViewById(R.id.wifi_signal);
        mWifiDark       = (ImageView) findViewById(R.id.wifi_signal_dark);
        mWifiActivity   = (ImageView) findViewById(R.id.wifi_inout);
        mAirplane       = (ImageView) findViewById(R.id.airplane);
        mNoSims         = (/**ImageView**/TextView) findViewById(R.id.no_sims);// modified by yangfan
        mNoSimsDark     = (/**ImageView**/TextView) findViewById(R.id.no_sims_dark);// modified by yangfan
        mImsOverWifiImageView    = (ImageView) findViewById(R.id.ims_over_wifi);
        mNoSimsCombo    =             findViewById(R.id.no_sims_combo);
        mWifiAirplaneSpacer =         findViewById(R.id.wifi_airplane_spacer);
        mWifiSignalSpacer =           findViewById(R.id.wifi_signal_spacer);
        mMobileSignalGroup = (LinearLayout) findViewById(R.id.mobile_signal_group);
        for (PhoneState state : mPhoneStates) {
            mMobileSignalGroup.addView(state.mMobileGroup);
        }
        TunerService.get(mContext).addTunable(this, StatusBarIconController.ICON_BLACKLIST);

        apply();
        applyIconTint();
    }

    @Override
    protected void onDetachedFromWindow() {
        mVpn            = null;
        mEthernetGroup  = null;
        mEthernet       = null;
        mWifiGroup      = null;
        mWifi           = null;
        mWifiActivity   = null;
        mAirplane       = null;
        mImsOverWifiImageView    = null;
        mMobileSignalGroup.removeAllViews();
        mMobileSignalGroup = null;
// added by yangfan start
        mNoSimsCombo = null;
        mNoSims = null;
        mNoSimsDark = null;
// added by yangfan end 
        TunerService.get(mContext).removeTunable(this);

        super.onDetachedFromWindow();
    }

    // From SecurityController.
    @Override
    public void onStateChanged() {
        post(new Runnable() {
            @Override
            public void run() {
                mVpnVisible = mSC.isVpnEnabled();
                apply();
            }
        });
    }

    @Override
    public void setWifiIndicators(boolean enabled, IconState statusIcon, IconState qsIcon,
            boolean activityIn, boolean activityOut, String description) {
        mWifiVisible = statusIcon.visible && !mBlockWifi;
        mWifiStrengthId = statusIcon.icon;
        mWifiActivityId = getWifiActivityId(activityIn, activityOut);
        mWifiDescription = statusIcon.contentDescription;

        apply();
    }

    @Override
    public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon,
                                        int statusType, boolean showDataIcon, int qsType, boolean activityIn,
                                        boolean activityOut, int dataActivityId, int mobileActivityId,
                                        int stackedDataId, int stackedVoiceId,
                                        String typeContentDescription, String description, boolean isWide,
                                        int subId, String networkName,boolean showNetworkClass) {// add 'showDataIcon,networkName,showNetworkClass' by yangfan
        PhoneState state = getState(subId);
        if (state == null) {
            return;
        }
        state.mMobileVisible = statusIcon.visible && !mBlockMobile;
        state.mMobileStrengthId = statusIcon.icon;
        state.mMobileTypeId = statusType;        
        state.mMobileDescription = statusIcon.contentDescription;
        state.mMobileTypeDescription = typeContentDescription;
        state.mIsMobileTypeIconWide = statusType != 0 && isWide;
        state.mDataActivityId = dataActivityId;
        state.mMobileActivityId = mobileActivityId;
        state.mStackedDataId = stackedDataId;
        state.mStackedVoiceId = stackedVoiceId;
// added by yangfan
		state.mShowDataIcon = showDataIcon;
        state.mNetworkLabelName = networkName;
        state.mShowNetworkClass = showNetworkClass;
// added by yangfan
        apply();
    }

// add 'showDataIcon,networkName,showNetworkClass' by yangfan begin
    @Override
    public void setMobileDataIndicators(IconState statusIcon, IconState qsIcon,
                                        int statusType,boolean showDataIcon,int qsType, boolean activityIn,
                                        boolean activityOut, int dataActivityId, int mobileActivityId,
                                        int stackedDataId, int stackedVoiceId,
                                        String typeContentDescription, String description, boolean isWide,
                                        int subId, int imsIconId, boolean isImsOverWifi,
                                        int dataNetworkTypeInRoamingId, int embmsIconId, String networkName,boolean showNetworkClass) {
        PhoneState state = getState(subId);
        if (state == null) {
            return;
        }
        state.mMobileImsId = imsIconId;
        state.mDataNetworkTypeInRoamingId = dataNetworkTypeInRoamingId;
        state.mMobileEmbmsId = embmsIconId;
        mImsOverWifi = isImsOverWifi;

        this.setMobileDataIndicators(statusIcon, qsIcon, statusType,showDataIcon, qsType,
                activityIn, activityOut, dataActivityId, mobileActivityId,
                stackedDataId, stackedVoiceId, typeContentDescription,
                description, isWide, subId, networkName,showNetworkClass);
    }
// add 'showDataIcon,networkName,showNetworkClass' by yangfan end

    @Override
    public void setEthernetIndicators(IconState state) {
        mEthernetVisible = state.visible && !mBlockEthernet;
        mEthernetIconId = state.icon;
        mEthernetDescription = state.contentDescription;

        apply();
    }

    @Override
    public void setNoSims(boolean show) {
        mNoSimsVisible = show && !mBlockMobile;
        apply();
    }

    @Override
    public void setSubs(List<SubscriptionInfo> subs) {
        if (hasCorrectSubs(subs)) {
            return;
        }
        // Clear out all old subIds.
        mPhoneStates.clear();
        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.removeAllViews();
        }
        final int n = subs.size();
        for (int i = 0; i < n; i++) {
            inflatePhoneState(subs.get(i).getSubscriptionId());
        }
        if (isAttachedToWindow()) {
            applyIconTint();
        }
    }

    private boolean hasCorrectSubs(List<SubscriptionInfo> subs) {
        final int N = subs.size();
        if (N != mPhoneStates.size()) {
            return false;
        }
        for (int i = 0; i < N; i++) {
            if (mPhoneStates.get(i).mSubId != subs.get(i).getSubscriptionId()) {
                return false;
            }
        }
        return true;
    }

    private PhoneState getState(int subId) {
        for (PhoneState state : mPhoneStates) {
            if (state.mSubId == subId) {
                return state;
            }
        }
        Log.e(TAG, "Unexpected subscription " + subId);
        return null;
    }

    private int getWifiActivityId(boolean activityIn, boolean activityOut) {
        if (!getContext().getResources().getBoolean(R.bool.config_showWifiActivity)) {
            return 0;
        }
        int activityId = 0;
        if (activityIn && activityOut) {
            activityId = R.drawable.stat_sys_wifi_inout;
        } else if (activityIn) {
            activityId = R.drawable.stat_sys_wifi_in;
        } else if (activityOut) {
            activityId = R.drawable.stat_sys_wifi_out;
        }
        return activityId;
    }

    private int getNoSimIcon() {
        int resId = 0;
        final String[] noSimArray;
        Resources res = getContext().getResources();

        if (!res.getBoolean(R.bool.config_read_icons_from_xml)) return resId;

        try {
            noSimArray = res.getStringArray(R.array.multi_no_sim);
        } catch (android.content.res.Resources.NotFoundException e) {
            return resId;
        }

        if (noSimArray == null) return resId;

        String resName = noSimArray[0];
        resId = res.getIdentifier(resName, null, getContext().getPackageName());
        if (DEBUG) Log.d(TAG, "getNoSimIcon resId = " + resId + " resName = " + resName);
        return resId;
    }

    private PhoneState inflatePhoneState(int subId) {
        PhoneState state = new PhoneState(subId, mContext);
        if (mMobileSignalGroup != null) {
            mMobileSignalGroup.addView(state.mMobileGroup);
        }
        mPhoneStates.add(state);
        return state;
    }

    @Override
    public void setIsAirplaneMode(IconState icon) {
        mIsAirplaneMode = icon.visible && !mBlockAirplane;
        mAirplaneIconId = icon.icon;
        mAirplaneContentDescription = icon.contentDescription;

        apply();
    }

    @Override
    public void setMobileDataEnabled(boolean enabled) {
        // Don't care.
        for (PhoneState mPhoneState : mPhoneStates) {
            mPhoneState.updateNetworkClassVisibility();
        }// added by yangfan
    }

    @Override
    public boolean dispatchPopulateAccessibilityEventInternal(AccessibilityEvent event) {
        // Standard group layout onPopulateAccessibilityEvent() implementations
        // ignore content description, so populate manually
        if (mEthernetVisible && mEthernetGroup != null &&
                mEthernetGroup.getContentDescription() != null)
            event.getText().add(mEthernetGroup.getContentDescription());
        if (mWifiVisible && mWifiGroup != null && mWifiGroup.getContentDescription() != null)
            event.getText().add(mWifiGroup.getContentDescription());
        for (PhoneState state : mPhoneStates) {
            state.populateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEventInternal(event);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        if (mEthernet != null) {
            mEthernet.setImageDrawable(null);
            mEthernetDark.setImageDrawable(null);
            mLastEthernetIconId = -1;
        }

        if (mWifi != null) {
            mWifi.setImageDrawable(null);
            mWifiDark.setImageDrawable(null);
            mLastWifiStrengthId = -1;
        }

        if (mWifiActivity != null) {
            mWifiActivity.setImageDrawable(null);
            mLastWifiActivityId = -1;
        }

        for (PhoneState state : mPhoneStates) {
            if (state.mMobile != null) {
                state.mMobile.setImageDrawable(null);
            }
            if (state.mMobileType != null) {
                state.mMobileType.setImageDrawable(null);
                state.mMobileDataType.setImageDrawable(null);// added by yangfan
            }
        }

        if (mAirplane != null) {
            mAirplane.setImageDrawable(null);
            mLastAirplaneIconId = -1;
        }

        apply();
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    // Run after each indicator change.
    private void apply() {
        if (mWifiGroup == null) return;

        mVpn.setVisibility(mVpnVisible ? View.VISIBLE : View.GONE);
        if (DEBUG) Log.d(TAG, String.format("vpn: %s", mVpnVisible ? "VISIBLE" : "GONE"));

        if (mEthernetVisible) {
            if (mLastEthernetIconId != mEthernetIconId) {
                mEthernet.setImageResource(mEthernetIconId);
                mEthernetDark.setImageResource(mEthernetIconId);
                mLastEthernetIconId = mEthernetIconId;
            }
            mEthernetGroup.setContentDescription(mEthernetDescription);
            mEthernetGroup.setVisibility(View.VISIBLE);
        } else {
            mEthernetGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("ethernet: %s",
                    (mEthernetVisible ? "VISIBLE" : "GONE")));

        if (mWifiVisible) {
            if (mWifiStrengthId != mLastWifiStrengthId) {
                mWifi.setImageResource(mWifiStrengthId);
                mWifiDark.setImageResource(mWifiStrengthId);
                mLastWifiStrengthId = mWifiStrengthId;
            }
            if (mWifiActivityId != mLastWifiActivityId) {
                mWifiActivity.setImageResource(mWifiActivityId);
                mLastWifiActivityId = mWifiActivityId;
            }
            mWifiGroup.setContentDescription(mWifiDescription);
            mWifiGroup.setVisibility(View.VISIBLE);
        } else {
            mWifiGroup.setVisibility(View.GONE);
        }

        if (DEBUG) Log.d(TAG,
                String.format("wifi: %s sig=%d act=%d",
                    (mWifiVisible ? "VISIBLE" : "GONE"),
                    mWifiStrengthId, mWifiActivityId));

        boolean anyMobileVisible = false;
        int firstMobileTypeId = 0;
        for (PhoneState state : mPhoneStates) {
            if (state.apply(anyMobileVisible)) {
                if (!anyMobileVisible) {
                    firstMobileTypeId = state.mMobileTypeId;
                    anyMobileVisible = true;
                }
            }
        }

        if (mIsAirplaneMode) {
            if (mLastAirplaneIconId != mAirplaneIconId) {
                mAirplane.setImageResource(mAirplaneIconId);
                mLastAirplaneIconId = mAirplaneIconId;
            }
            mAirplane.setContentDescription(mAirplaneContentDescription);
            mAirplane.setVisibility(View.VISIBLE);
        } else {
            mAirplane.setVisibility(View.GONE);
        }

        if (mImsOverWifi){
            mImsOverWifiImageView.setVisibility(View.VISIBLE);
        } else {
            mImsOverWifiImageView.setVisibility(View.GONE);
        }

        if (mIsAirplaneMode && mWifiVisible) {
            mWifiAirplaneSpacer.setVisibility(View.VISIBLE);
        } else {
            mWifiAirplaneSpacer.setVisibility(View.GONE);
        }

        if (((anyMobileVisible && firstMobileTypeId != 0) || mNoSimsVisible) && mWifiVisible) {
            mWifiSignalSpacer.setVisibility(View.VISIBLE);
        } else {
            mWifiSignalSpacer.setVisibility(View.GONE);
        }

        if (mNoSimsVisible && mNoSims != null && mNoSimsDark != null) {
            if (mNoSimsIcon == 0) mNoSimsIcon = getNoSimIcon();
            if (mNoSimsIcon != 0) {
                mNoSims.setText(getContext().getResources().getString(
                        mNoSimsIcon));
                mNoSimsDark.setText(getContext().getResources().getString(
                        mNoSimsIcon));// modified by yangfan
            }
        }
        if (null != mNoSimsCombo) {
            mNoSimsCombo.setVisibility(mNoSimsVisible ? View.VISIBLE : View.GONE);
        }// modified by yangfan

        boolean anythingVisible = mNoSimsVisible || mWifiVisible || mIsAirplaneMode
                || anyMobileVisible || mVpnVisible || mEthernetVisible;
        //setPaddingRelative(0, 0, anythingVisible ? mEndPadding : mEndPaddingNothingVisible, 0);// remove padding by yangfan
    }

    public void setIconTint(int tint, float darkIntensity) {
        boolean changed = tint != mIconTint || darkIntensity != mDarkIntensity;
        mIconTint = tint;
        mDarkIntensity = darkIntensity;
        if (changed && isAttachedToWindow()) {
            applyIconTint();
            applyTextTint();// added by yangfan
        }
    }

// added by yangfan
    private void applyTextTint() {
        mNoSimsDark.setTextColor(mIconTint);
        mNoSims.setTextColor(mIconTint);
    }
// added by yangfan

    private void applyIconTint() {
        setTint(mVpn, mIconTint);
        setTint(mAirplane, mIconTint);
        applyDarkIntensity(mDarkIntensity, mNoSims, mNoSimsDark);
        applyDarkIntensity(mDarkIntensity, mWifi, mWifiDark);
        applyDarkIntensity(mDarkIntensity, mEthernet, mEthernetDark);
        for (int i = 0; i < mPhoneStates.size(); i++) {
            mPhoneStates.get(i).setIconTint(mIconTint, mDarkIntensity);
        }
    }

    private void applyDarkIntensity(float darkIntensity, View lightIcon, View darkIcon) {
        lightIcon.setAlpha(1 - darkIntensity);
        darkIcon.setAlpha(darkIntensity);
    }

    private void setTint(ImageView v, int tint) {
        v.setImageTintList(ColorStateList.valueOf(tint));
    }

    private class PhoneState {
        private final int mSubId;
        private boolean mMobileVisible = false/**;**/ , mShowDataIcon = false;// added by yangfan
        private int mMobileStrengthId = 0, mMobileTypeId = 0;
        private boolean mIsMobileTypeIconWide;
        private String mMobileDescription, mMobileTypeDescription;

        private ViewGroup mMobileGroup;
        private ImageView mMobile, mMobileDark, mMobileType,mMobileDataType, mRoaming,
                mMobileIms, mDataNetworkTypeInRoaming, mMobileEmbms;// declare variable 'mobileDataType'  by yangfan

        private int mDataActivityId = 0, mMobileActivityId = 0, mMobileImsId = 0,
                 mDataNetworkTypeInRoamingId =0, mMobileEmbmsId = 0;
        private int mStackedDataId = 0, mStackedVoiceId = 0;
        private ImageView mDataActivity, mMobileActivity, mStackedData, mStackedVoice;
        private ViewGroup mMobileSingleGroup, mMobileStackedGroup;
//declare variable by yangfan begin
        private View mMobileNetworkLaberGroup;
        private TextView mNetWorkNameLabelView;
        private boolean mIsNetworkLabelEnable;
        private String mNetworkLabelName;
        private boolean mHasSimNoService;
        private boolean mNoserviceEnable;
        private SubscriptionManager mSubscriptionManager;
        private boolean mShowNetworkClass;
//declare variable by yangfan end

        public PhoneState(int subId, Context context) {
            ViewGroup root = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.mobile_signal_group, null);
            setViews(root);
            mSubId = subId;

            mSubscriptionManager = SubscriptionManager.from(mContext);//init sm by yangfan
        }

//add labelVisibility by yangfan 
        public void setLabelVisibility(boolean enable, boolean noServiceEnable) {
            mIsNetworkLabelEnable = enable;
            updateNetworkClassVisibility();
//            apply(false);
        }

        public void updateNetworkClassVisibility() {
            TelephonyManager tm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
            mNetWorkNameLabelView.setText(getNetworkLabelName(tm,mShowDataIcon));
            mNetWorkNameLabelView.setVisibility(mIsNetworkLabelEnable && !TextUtils.isEmpty(mNetworkLabelName) ? VISIBLE : GONE);
        }
//add labelVisibility by yangfan end

//add method of 'getNetworkLabelName' by yangfan 
        public String getNetworkLabelName(TelephonyManager tm,boolean showData) {
            String result = mNetworkLabelName;
            String[] names = null;
            Log.i(TAG, "mNetworkLabelName ; " + result);
            String filter = mContext.getResources().getString(R.string.mobile_network_namae_filter);
            if (TextUtils.isEmpty(result)) {
                return result;
            }
            result = result.replaceAll(" ", "");
            final int[] subIds = mSubscriptionManager.getActiveSubscriptionIdList();
            boolean china = result.contains(filter) && subIds.length > 1;
            if (china) {
                result = result.replace(filter, "");
            }
            Log.i(TAG, "filterChina : " + china + ", result : " + result + " ,filter : " + filter);
            names = result.split("\\|");
            result = "";
            for (String label : names) {
                Log.i(TAG, "label : " + label + "names.length : " + names.length);
                if (label.equals(mContext.getString(com.android.internal.R.string.lockscreen_carrier_default))
                        || label.equals(mContext.getString(com.android.internal.R.string.emergency_calls_only))) {
                    result = "";
                    break;
//                    continue;
                } else {
                    result = label;
                }
            }
            mNetworkLabelName = result;
            mShowNetworkClass = mShowDataIcon;
            Log.i(TAG, "mShowNetworkClass : " + mShowNetworkClass);
            result = mShowNetworkClass ? result : result.replaceAll("\\dG","");
            return result.trim();
        }
//add method of 'getNetworkLabelName' by yangfan end

        public void setViews(ViewGroup root) {
            mMobileGroup    = root;
            mMobile         = (ImageView) root.findViewById(R.id.mobile_signal);
            mMobileDark     = (ImageView) root.findViewById(R.id.mobile_signal_dark);
            mMobileType     = (ImageView) root.findViewById(R.id.mobile_type);
            mMobileDataType = (ImageView) root.findViewById(R.id.mobile_data_type);// add 4G by yangfan
            mMobileActivity = (ImageView) root.findViewById(R.id.mobile_inout);
            mMobileIms      = (ImageView) root.findViewById(R.id.ims_hd);
            mDataNetworkTypeInRoaming = (ImageView) root
                    .findViewById(R.id.dataNetwork_type_in_roaming);
            mMobileEmbms    = (ImageView) root.findViewById(R.id.embms);
            mDataActivity   = (ImageView) root.findViewById(R.id.data_inout);
            mStackedData    = (ImageView) root.findViewById(R.id.mobile_signal_data);
            mStackedVoice   = (ImageView) root.findViewById(R.id.mobile_signal_voice);

            mMobileSingleGroup = (ViewGroup) root.findViewById(R.id.mobile_signal_single);
            mMobileStackedGroup = (ViewGroup) root.findViewById(R.id.mobile_signal_stacked);
            mRoaming        = (ImageView) root.findViewById(R.id.mobile_roaming);
//add 'label and label container' by yangfan 
            mMobileNetworkLaberGroup = root
                    .findViewById(R.id.mobile_network_label);
            mNetWorkNameLabelView = (TextView) root
                    .findViewById(R.id.network_label);
//add 'label and label container' by yangfan  end
        }

        public boolean apply(boolean isSecondaryIcon) {
//add 'tm' by yangfan 
            TelephonyManager tm = (TelephonyManager) mContext
                    .getSystemService(Context.TELEPHONY_SERVICE);
//add 'tm' by yangfan end

//update labelName by yangfan
            String networkLabelName = getNetworkLabelName(tm,mShowDataIcon);
            mNetWorkNameLabelView.setText(networkLabelName);
            mNetWorkNameLabelView.setVisibility(mIsNetworkLabelEnable && !TextUtils.isEmpty(networkLabelName) ? VISIBLE : GONE);
//                mMobileSingleGroup.setVisibility(mNoserviceEnable ? GONE : VISIBLE);

            int mobileStrengthId =  TextUtils.isEmpty(networkLabelName) ? R.drawable.qucii_stat_sys_no_service : mMobileStrengthId;
//update labelName by yangfan end
            if (mMobileVisible && !mIsAirplaneMode) {
                mMobile.setImageResource(mMobileStrengthId);
                Drawable mobileDrawable = mMobile.getDrawable();
                if (mobileDrawable instanceof Animatable) {
                    Animatable ad = (Animatable) mobileDrawable;
                    if (!ad.isRunning()) {
                        ad.start();
                    }
                }
//added by yangfan 
                mMobileDark.setImageResource(mMobileStrengthId);
//added by yangfan end
                Drawable mobileDarkDrawable = mMobileDark.getDrawable();
                if (mobileDarkDrawable instanceof Animatable) {
                    Animatable ad = (Animatable) mobileDarkDrawable;
                    if (!ad.isRunning()) {
                        ad.start();
                    }
                }
                mMobileType.setImageResource(mMobileTypeId);
				
                mMobileDataType.setImageResource(0);

                mDataActivity.setImageResource(mDataActivityId);
                Drawable dataActivityDrawable = mDataActivity.getDrawable();
                if (dataActivityDrawable instanceof Animatable) {
                    Animatable ad = (Animatable) dataActivityDrawable;
                    if (!ad.isRunning()) {
                        ad.start();
                    }
                }

                mMobileActivity.setImageResource(mMobileActivityId);
                Drawable mobileActivityDrawable = mMobileActivity.getDrawable();
                if (mobileActivityDrawable instanceof Animatable) {
                    Animatable ad = (Animatable) mobileActivityDrawable;
                    if (!ad.isRunning()) {
                        ad.start();
                    }
                }

                mMobileIms.setImageResource(mMobileImsId);
                mMobileEmbms.setImageResource(mMobileEmbmsId);

                mDataNetworkTypeInRoaming.setImageResource(mDataNetworkTypeInRoamingId);

                if (mStackedDataId != 0 && mStackedVoiceId != 0) {
                    mStackedData.setImageResource(mStackedDataId);
                    mStackedVoice.setImageResource(mStackedVoiceId);
                    mMobileSingleGroup.setVisibility(View.GONE);
// added by yangfan
                    mMobileDataType.setVisibility(GONE);
// added by yangfan end
                    mMobileStackedGroup.setVisibility(View.VISIBLE);
                } else {
                    mStackedData.setImageResource(0);
                    mStackedVoice.setImageResource(0);
                    mMobileSingleGroup.setVisibility(View.VISIBLE);
                    mMobileStackedGroup.setVisibility(View.GONE);
                }

                if (tm != null && tm.isNetworkRoaming(mSubId) &&
                        (mContext.getResources().getBoolean(R.bool.show_roaming_and_network_icons))) {
                    mRoaming.setImageDrawable(getContext().getResources().getDrawable(
                            R.drawable.stat_sys_data_fully_connected_roam));
                } else {
                    mRoaming.setImageDrawable(null);
                }
                mMobileGroup.setContentDescription(mMobileTypeDescription
                        + " " + mMobileDescription);
                mMobileGroup.setVisibility(View.VISIBLE);
            } else {
                mMobileGroup.setVisibility(View.GONE);
            }

            // When this isn't next to wifi, give it some extra padding between the signals.
            mMobileGroup.setPaddingRelative(isSecondaryIcon ? mSecondaryTelephonyPadding : 0,
                    0, 0, 0);
            mMobile.setPaddingRelative(mIsMobileTypeIconWide ? mWideTypeIconStartPadding : 0,
                    0, 0, 0);
            mMobileDark.setPaddingRelative(mIsMobileTypeIconWide ? mWideTypeIconStartPadding : 0,
                    0, 0, 0);

            if (DEBUG) Log.d(TAG, String.format("mobile: %s sig=%d typ=%d",
                        (mMobileVisible ? "VISIBLE" : "GONE"), mMobileStrengthId, mMobileTypeId));

            //  force gone by yangfan 
            mMobileType.setVisibility(mMobileTypeId != 0 ? /*View.VISIBLE*/GONE
                    : View.GONE);
            mMobileDataType.setVisibility(mShowDataIcon ? GONE
                    : GONE);
            mDataActivity.setVisibility(mDataActivityId != 0 ? View.VISIBLE : View.GONE);
            int dataIcon = mMobileActivityId != 0 && !TextUtils.isEmpty(mNetworkLabelName) ? /*VISIBLE*/GONE : GONE;
            mMobileActivity.setVisibility(dataIcon);//  force gone by yangfan
            mMobileIms.setVisibility(mMobileImsId != 0 ? View.VISIBLE : View.GONE);
            mDataNetworkTypeInRoaming.setVisibility(mDataNetworkTypeInRoamingId != 0 ? View.VISIBLE
                    : View.GONE);
            mMobileEmbms.setVisibility(mMobileEmbmsId != 0 ? View.VISIBLE : View.GONE);
            return mMobileVisible;
        }

        public void populateAccessibilityEvent(AccessibilityEvent event) {
            if (mMobileVisible && mMobileGroup != null
                    && mMobileGroup.getContentDescription() != null) {
                event.getText().add(mMobileGroup.getContentDescription());
            }
        }

        public void setIconTint(int tint, float darkIntensity) {
            applyDarkIntensity(darkIntensity, mMobile, mMobileDark);
            mNetWorkNameLabelView.setTextColor(tint);//added by yangfan
            setTint(mMobileType, tint);
            setTint(mMobileDataType, tint);//added by yangfan
        }
    }

//added by yangfan start 
    @Override
    public void setNetworkLabelEnable(boolean enable, boolean noServiceEnable) {
        for (PhoneState state : mPhoneStates) {
            state.setLabelVisibility(enable, noServiceEnable);
        }
    }
//added by yangfan end 
}
