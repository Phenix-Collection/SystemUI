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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.Clock;
import com.qucii.systemui.statusbar.phone.ClockController;

public class IconMerger extends LinearLayout {
    private static final String TAG = "IconMerger";
    private static final boolean DEBUG = false;

    private int mIconSize;
    private View mMoreView;
    private int mClockLocation;

    public IconMerger(Context context, AttributeSet attrs) {
        super(context, attrs);

        mIconSize = context.getResources().getDimensionPixelSize(
                R.dimen.status_bar_icon_size);

        if (DEBUG) {
            setBackgroundColor(0x800099FF);
        }
    }

    public void setOverflowIndicator(View v) {
        mMoreView = v;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // we need to constrain this to an integral multiple of our children
        int width = getMeasuredWidth();
		//modified  by yangfan
        if (mClockLocation == ClockController.STYLE_CLOCK_CENTER) {
            int totalWidth = getResources().getDisplayMetrics().widthPixels;
            width = totalWidth / 2 - mIconSize * 2;
        }
        int iconMegerSize = width - (width % mIconSize);
        Log.d(TAG, "iconMegerSize : " + iconMegerSize + "mIconSize : " + mIconSize);
        setMeasuredDimension(iconMegerSize, getMeasuredHeight());
		//modified  by yangfan
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        checkOverflow(r - l);
    }

    private void checkOverflow(int width) {
        if (mMoreView == null) return;

        final int N = getChildCount();
        int visibleChildren = 0;
        for (int i=0; i<N; i++) {
            if (getChildAt(i).getVisibility() != GONE) visibleChildren++;
        }
        final boolean overflowShown = (mMoreView.getVisibility() == View.VISIBLE);
        // let's assume we have one more slot if the more icon is already showing
		//modified  by yangfan
        final boolean configOnlyShowUSB = getContext().getResources().getBoolean(R.bool.config_only_show_usb_adb);
        if (overflowShown) {
            int totalWidth = getResources().getDisplayMetrics().widthPixels;
            if ((mClockLocation != ClockController.STYLE_CLOCK_CENTER &&
                    mClockLocation != ClockController.STYLE_CLOCK_LEFT) ||
                    (visibleChildren > (totalWidth / mIconSize / 2 + 1))) {
                visibleChildren--;
            }
        }
        final boolean moreRequired = visibleChildren * mIconSize > width;
        setMoreVisibility(moreRequired || overflowShown ? VISIBLE : GONE);
		//modified  by yangfan
    }
	
	//added by yangfan 
    public void setClockAndDateStatus(int mode) {
        mClockLocation = mode;
    }

    public void setMoreVisibility(int vis) {
        if (mMoreView.getVisibility() != vis) {
            mMoreView.setVisibility(vis);
        }
    }
	//added by yangfan 
}
