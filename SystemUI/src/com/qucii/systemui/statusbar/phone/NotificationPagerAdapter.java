package com.qucii.systemui.statusbar.phone;

import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import com.android.systemui.statusbar.phone.NotificationsQuickSettingsContainer;

/**
 * Created by ansen on 9/14/16.
 */
public class NotificationPagerAdapter extends PagerAdapter {

    private final NotificationsQuickSettingsContainer mNotificationsQuickSettingsContainer;
    private final List<View> mViews;

    public NotificationPagerAdapter(NotificationsQuickSettingsContainer container, List<View> views) {
        this.mNotificationsQuickSettingsContainer = container;
        this.mViews = views;
    }
    public List<View> getViews(){
        return mViews;
    }


    @Override
    public int getCount() {
        return mViews.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object o) {
        return view==o;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        if (mViews == null) {
            getRefreshData();
        }
        Log.d("ansen"," "+mViews.size());
        View view = mViews.get(position);
        container.addView(view);
        return view;
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (object instanceof View) {
            if (mViews == null) {
                getRefreshData();
            }
            container.removeView(mViews.get(position));
        }

    }

    public List<View> getRefreshData() {
        return null;
    }
}
