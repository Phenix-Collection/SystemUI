package com.android.systemui;

import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BatteryStateRegistar;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import java.text.NumberFormat;
/**
 * @Description:
 * @csdnblog   
 * @author  mare
 * @date 2016-11-1
 * @time  am 11:22:56
 */
public class BatteryLevelTextView extends TextView implements
        BatteryController.BatteryStateChangeCallback{

    private BatteryStateRegistar mBatteryStateRegistar;
    private boolean mBatteryPresent;

    private boolean mBatteryCharging;
    private boolean mForceShow;
    private boolean mAttached;
    private int mRequestedVisibility;
    private int mStyle;
    private int mPercentMode;
    private int mLevelColor = 0;

    public BatteryLevelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mRequestedVisibility = GONE;
        mLevelColor = getCurrentTextColor();
    }

    public void setForceShown(boolean forceShow) {
        mForceShow = forceShow;
        updateVisibility();
    }

    public void setBatteryStateRegistar(BatteryStateRegistar batteryStateRegistar) {
        mRequestedVisibility = VISIBLE;
        mBatteryStateRegistar = batteryStateRegistar;
        if (mAttached) {
            mBatteryStateRegistar.addStateChangedCallback(this);
        }
    }

    @Override
    public void setVisibility(int visibility) {
        mRequestedVisibility = visibility;
        updateVisibility();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Respect font size setting.
       //setTextSize(TypedValue.COMPLEX_UNIT_PX,
       //         getResources().getDimensionPixelSize(R.dimen.battery_level_text_size));
    }

    @Override
    public void onBatteryLevelChanged( int level, boolean pluggedIn,
            boolean charging) {
        String percentage = NumberFormat.getPercentInstance().format((double) level / 100.0);
        setText(percentage);
        if (mBatteryCharging != charging) {
            mBatteryCharging = charging;
            updateVisibility();
        }
    }

    @Override
    public void onPowerSaveChanged() {
        // Not used
    }

    @Override
    public void onBatteryStyleChanged(int style, int percentMode) {
        mStyle = style;
        mPercentMode = percentMode;
        updateVisibility();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mBatteryStateRegistar != null) {
            mBatteryStateRegistar.addStateChangedCallback(this);
        }

        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttached = false;

        if (mBatteryStateRegistar != null) {
            mBatteryStateRegistar.removeStateChangedCallback(this);
        }
    }

    private void updateVisibility() {
        boolean showNextPercent = /*mBatteryPresent && */(
                mPercentMode == BatteryController.PERCENTAGE_MODE_OUTSIDE
                || (mBatteryCharging && mPercentMode == BatteryController.PERCENTAGE_MODE_INSIDE));
        if (mStyle == BatteryController.STYLE_GONE) {
            showNextPercent = false;
        } else if (mStyle == BatteryController.STYLE_TEXT) {
            showNextPercent = true;
        }

        if (mBatteryStateRegistar != null && (showNextPercent || mForceShow)) {
            super.setVisibility(mRequestedVisibility);
        } else {
            super.setVisibility(GONE);
        }
    }
    
    
    public void setDarkIntensity(float darkIntensity) {
        mLevelColor =  (int) ArgbEvaluator.getInstance().evaluate(darkIntensity,
                getContext().getColor(R.color.light_mode_icon_color_single_tone),
                getContext().getColor(R.color.dark_mode_icon_color_single_tone));
        setTextColor(mLevelColor);
    }
}
