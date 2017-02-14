package com.qucii.systemui.statusbar.phone;

import com.android.systemui.statusbar.phone.NotificationPanelView;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * Created by ansen on 9/14/16.
 */
public class NotificationsViewPager extends ViewPager {
	private boolean mIsAnti;
	private NotificationPanelView mPanelView;
	private NotificationStackScrollLayout mNotificationStackScroller;
	private boolean mIsDisable;
	private float mLastX, mLastY;
	private boolean mPagerWantsIntercept = false;
	private float mSwipTheshold;
	private boolean isOnTouchNotification = false;

	public NotificationsViewPager(Context context) {
		this(context, null);
		mSwipTheshold = ViewConfiguration.get(getContext())
				.getScaledPagingTouchSlop();
	}

	public NotificationsViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setOrder(boolean isAnti) {
		this.mIsAnti = isAnti;
	}

	public boolean getOrder() {
		return mIsAnti;
	}

	public void setDisable(boolean isDisable) {
		this.mIsDisable = isDisable;
	}

	public void setPanelView(NotificationPanelView view) {
		this.mPanelView = view;
		this.mNotificationStackScroller = (NotificationStackScrollLayout) ((NotificationPagerAdapter) (getAdapter()))
				.getViews().get(0);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (mIsDisable) {
			mPagerWantsIntercept = false;
			return false;
		}
		boolean isNotificationView = mPanelView.isNotificationView();
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			resetState();
			mLastX = ev.getX();
			mLastY = ev.getY();
			isOnTouchNotification = !(mNotificationStackScroller
					.isBelowLastNotification(mLastX, mLastY)); // .....
			break;
		case MotionEvent.ACTION_MOVE:
			float tmpX = ev.getX();
			float tmpY = ev.getY();
			int deltaX = (int) (tmpX - mLastX);
			int deltaY = (int) (tmpY - mLastY);
			mLastX = tmpX;
			if (deltaY <= -mSwipTheshold  && Math.abs(deltaY) > Math.abs(deltaX) && !isNotificationView) { // force closepanel when up flip 
				mPanelView.fling(0, false);
				return false;
			}
			if (deltaX >= mSwipTheshold) { // right flip
				if (isNotificationView) {
					if (isOnTouchNotification) { //don`t intercept when on some notiification
						mPagerWantsIntercept = false;
					}
				} else {
					mPagerWantsIntercept = super.onInterceptTouchEvent(ev);//force intercept when 1 --> 0
				}
			} else if (deltaX <= -mSwipTheshold) {// left flip
				if (isNotificationView) {
					mPagerWantsIntercept = super.onInterceptTouchEvent(ev);//force intercept when 0 --> 1
				} else {
					mPagerWantsIntercept = false;//don`t intercept when 0 --> 1
				}
			} else {// don`t intercept when not beyond slop 
				mPagerWantsIntercept = false;
			}
			return mPagerWantsIntercept;

		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			resetState();
			break;

		default:
			break;
		}
		return super.onInterceptTouchEvent(ev);
	}

	public void resetState() {
		mPagerWantsIntercept = false;
		isOnTouchNotification = false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		for (int i = 0; i < getChildCount(); i++) {
			getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
		}
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mIsDisable) {
			return false;
		}
		return super.onTouchEvent(ev);
	}

	@Override
	public void setAdapter(PagerAdapter adapter) {
		super.setAdapter(adapter);
		// if (null != getAdapter()) {
		// getAdapter().unregisterDataSetObserver(mDataSetObserver);
		// }
		adapter.registerDataSetObserver(mDataSetObserver);
	}

	private DataSetObserver mDataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			super.onChanged();
			notifyDataChanged();
		}

		@Override
		public void onInvalidated() {
			super.onInvalidated();
			notifyDataChanged();
		}
	};

	private void notifyDataChanged() {
		getRefreshedList();
		invalidate();
		requestLayout();
	}

	private void getRefreshedList() {
		getAdapter();
	}

	@Override
	public void setCurrentItem(int item, boolean smoothScroll) {
		super.setCurrentItem(item, smoothScroll);
	}

	@Override
	public void setCurrentItem(int item) {
		this.setCurrentItem(item, false);// forbidden scroll animtion
	}

}
