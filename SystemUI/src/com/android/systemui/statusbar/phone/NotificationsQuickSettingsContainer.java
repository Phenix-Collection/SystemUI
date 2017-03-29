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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import com.qucii.systemui.statusbar.phone.NotificationPagerAdapter;
import com.qucii.systemui.statusbar.phone.NotificationsViewPager;

import java.util.ArrayList;
import java.util.List;

/**
 * The container with notification stack scroller and quick settings inside.
 */
public class NotificationsQuickSettingsContainer extends FrameLayout
        implements ViewStub.OnInflateListener {

    private View mScrollView;
    private View mUserSwitcher;
    private View mStackScroller;
    private View mKeyguardStatusBar;
    private boolean mInflated;
    private boolean mQsExpanded;
    private NotificationsViewPager mNotificationsPager;
    private NotificationPagerAdapter mNotificationPagerAdapter;
    private List<View> mViews=new ArrayList<View>();

    public NotificationsQuickSettingsContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mNotificationsPager=(NotificationsViewPager)findViewById(R.id.notification_viewpager);
        mNotificationPagerAdapter = new NotificationPagerAdapter(this, getViews());
//        mScrollView = findViewById(R.id.scroll_view);
//        mStackScroller = findViewById(R.id.notification_stack_scroller);
        mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        mNotificationsPager.setAdapter(mNotificationPagerAdapter);
        ViewStub userSwitcher = (ViewStub) findViewById(R.id.keyguard_user_switcher);
        userSwitcher.setOnInflateListener(this);
        mUserSwitcher = userSwitcher;
    }

    private List<View> getViews() {
        if(mViews.size()==0){
            mScrollView= (ObservableScrollView)LayoutInflater.from(mNotificationsPager.getContext()).inflate(R.layout.qucii_notification_observablescrollview,null);
            mStackScroller=(NotificationStackScrollLayout)LayoutInflater.from(mNotificationsPager.getContext()).inflate(R.layout.qucii_notification_stackscrolllayout,null);
            mViews.add(mStackScroller);
            mViews.add(mScrollView);
        }
        return mViews;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        setPadding(0, 0, 0, insets.getSystemWindowInsetBottom());
        return insets;
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean userSwitcherVisible = mInflated && mUserSwitcher.getVisibility() == View.VISIBLE;
        boolean statusBarVisible = mKeyguardStatusBar.getVisibility() == View.VISIBLE;

        View stackQsTop = mQsExpanded ? mStackScroller : mScrollView;
        View stackQsBottom = !mQsExpanded ? mStackScroller : mScrollView;
        // Invert the order of the scroll view and user switcher such that the notifications receive
        // touches first but the panel gets drawn above.
       /* if (child == mScrollView) {
            return super.drawChild(canvas, userSwitcherVisible && statusBarVisible ? mUserSwitcher
                    : statusBarVisible ? mKeyguardStatusBar
                    : userSwitcherVisible ? mUserSwitcher
                    : stackQsBottom, drawingTime);
        } else if (child == mStackScroller) {
            return super.drawChild(canvas,
                    userSwitcherVisible && statusBarVisible ? mKeyguardStatusBar
                    : statusBarVisible || userSwitcherVisible ? stackQsBottom
                    : stackQsTop,
                    drawingTime);
        } else if (child == mUserSwitcher) {
            return super.drawChild(canvas,
                    userSwitcherVisible && statusBarVisible ? stackQsBottom
                    : stackQsTop,
                    drawingTime);
        } else if (child == mKeyguardStatusBar) {
            return super.drawChild(canvas,
                    stackQsTop,
                    drawingTime);
        }else {
            return super.drawChild(canvas, child, drawingTime);
        }*/
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void onInflate(ViewStub stub, View inflated) {
        if (stub == mUserSwitcher) {
            mUserSwitcher = inflated;
            mInflated = true;
        }
    }

    public void setQsExpanded(boolean expanded) {
        if (mQsExpanded != expanded) {
            mQsExpanded = expanded;
            invalidate();
        }
    }
}
