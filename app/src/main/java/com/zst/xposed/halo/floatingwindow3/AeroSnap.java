package com.zst.xposed.halo.floatingwindow3;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

public class AeroSnap {
	
	public final static int UNKNOWN = -10000;
	
	

	final Handler mHandler;
	final int mDelay;

	Runnable mRunnable;
	int mRange = 100;
	int mSensitivity = 50;

	SnapWindowHolder mSnapWindowHolder;
	int[] mOldParam = new int[2]; // w,h
	int mScreenHeight;
	int mScreenWidth;

	boolean mTimeoutRunning;
	boolean mTimeoutDone;
	boolean mRestorePosition;
	boolean mChangedPreviousRange;
	float[] mPreviousRange = new float[2];
	
	/**
	 * An Aero Snap Class to check if the current pointer's coordinates
	 * are in range of the snap region.
	 */

	
	public AeroSnap(int delay) {
		mSnapWindowHolder = new SnapWindowHolder();
		mHandler = new Handler();
		mDelay = delay;
		mRange = Util.dp(mRange, MovableWindow.mWindowHolder.mActivity.getApplicationContext());
	}
	
	public void dispatchTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if(!MovableWindow.mWindowHolder.isSnapped && !MovableWindow.mWindowHolder.isMaximized) {
				finishSnap(mSnapWindowHolder.isSnapped && mTimeoutDone);
			}
			discardTimeout();
			mChangedPreviousRange = false;
			break;
		case MotionEvent.ACTION_DOWN:
			
			refreshScreenSize();
			break;
		case MotionEvent.ACTION_MOVE:
			showSnap((int) event.getRawX(), (int) event.getRawY());
			
		}
	}
	
	
	private void showSnap(int x, int y) {
		refreshScreenSize();
		if(initSnappable(x, y)){
			calculateSnap();
			broadcastShowWithTimeout();
		} else {
			broadcastHide(MovableWindow.mWindowHolder.mActivity);
		}
		
	}
	
	// do the snap by setting the variables and hiding the snap preview
	private void finishSnap(boolean apply) {
		if (apply) {
			MovableWindow.snap(mSnapWindowHolder);
		} else {
			MovableWindow.unsnap();
		}
		//refreshLayout();
		broadcastHide(MovableWindow.mWindowHolder.mActivity);
		//MovableWindow.toggleDragger();
	}
	
	/**
	 * Forces the window to snap to this side programatically without user input
	 * @param side - Side of the screen to snap to.
	 */
		
	public void forceSnapGravity(int sSnapGravity){
		refreshScreenSize();
		if(sSnapGravity == 0) {
			MovableWindow.unsnap();
			return;
		}
		mSnapWindowHolder.updateSnap(sSnapGravity);
		calculateSnap();
		finishSnap(true);
	}
	
	public void updateSnap(int sSnapGravity){
		refreshScreenSize();
		if(sSnapGravity == 0) {
			MovableWindow.unsnap();
			return;
		}
		mSnapWindowHolder.updateSnap(sSnapGravity);
		calculateSnap();
		MovableWindow.resize(mSnapWindowHolder.x, mSnapWindowHolder.y, mSnapWindowHolder.width, mSnapWindowHolder.height);
	}
	
	/**
	 * Initializes the current screen size with respect to rotation.
	 */
	private void refreshScreenSize() {
		final WindowManager wm = (WindowManager) MovableWindow.mWindowHolder.mActivity.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		
		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}
	
	/**
	 * Checks the range of the touch coordinates and set the respective side.
	 */
	 
	private boolean initSnappable(int x, int y) {
		if ((Math.abs(mOldParam[0] - x) > mSensitivity) ||
			(Math.abs(mOldParam[1] - y) > mSensitivity)) {
			mOldParam[0] = x;
			mOldParam[1] = y;
			discardTimeout();
			return false;
		}
		mOldParam[0] = x;
		mOldParam[1] = y;
		int newSnapGravity = 0;
		if(x < mRange) newSnapGravity = newSnapGravity | Gravity.LEFT;
		if(x > (mScreenWidth - mRange)) newSnapGravity = newSnapGravity | Gravity.RIGHT;
		if(y < mRange) newSnapGravity = newSnapGravity | Gravity.TOP;
		if(y > (mScreenHeight - mRange)) newSnapGravity = newSnapGravity | Gravity.BOTTOM;
		mSnapWindowHolder.updateSnap(newSnapGravity);
		return mSnapWindowHolder.isSnapped;
		//return (newSnapGravity!=0);
	}
	
	// create a snap positioning based on the range of our touch coordinates
	private void calculateSnap() {
//		if(mSnapWindowHolder.SnapGravity == 0) {
//			mSnapWindowHolder.isSnapped = false;
//			return;
//			}
		mSnapWindowHolder.height = ViewGroup.LayoutParams.MATCH_PARENT;//mScreenHeight;
		mSnapWindowHolder.width = ViewGroup.LayoutParams.MATCH_PARENT;// mScreenWidth;
		
		if(Util.isFlag(mSnapWindowHolder.SnapGravity, Gravity.TOP))
			mSnapWindowHolder.height = MovableWindow.mFloatDotCoordinates[1] + 1;
		if(Util.isFlag(mSnapWindowHolder.SnapGravity, Gravity.BOTTOM))
			mSnapWindowHolder.height = mScreenHeight - MovableWindow.mFloatDotCoordinates[1] + 1;
		if(Util.isFlag(mSnapWindowHolder.SnapGravity, Gravity.LEFT))
			mSnapWindowHolder.width = MovableWindow.mFloatDotCoordinates[0] + 1;
		if(Util.isFlag(mSnapWindowHolder.SnapGravity, Gravity.RIGHT))
			mSnapWindowHolder.width = mScreenWidth - MovableWindow.mFloatDotCoordinates[0] + 1;
			
		mSnapWindowHolder.x = Util.isFlag(mSnapWindowHolder.SnapGravity, Gravity.RIGHT)? (MovableWindow.mFloatDotCoordinates[0]) : 0;
		mSnapWindowHolder.y = Util.isFlag(mSnapWindowHolder.SnapGravity, Gravity.BOTTOM)? (MovableWindow.mFloatDotCoordinates[1]) : 0;
	}
	
	// stop the handler from continuing
	private void discardTimeout() {
		mTimeoutDone = false;
		mTimeoutRunning = false;
		mHandler.removeCallbacks(mRunnable);
	}
	
	// send broadcast after the snap delay
	private void broadcastShowWithTimeout() {
		if (mTimeoutRunning) return;
		if (mRunnable == null) {
			mRunnable = new Runnable() {
				@Override
				public void run() {
					broadcastShow(MovableWindow.mWindowHolder.mActivity, mSnapWindowHolder.width, mSnapWindowHolder.height, mSnapWindowHolder.SnapGravity);
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mTimeoutRunning = false;
							mTimeoutDone = true;
							// Delay to offset the lag because broadcastShow
							// will have some delay in inflating the view.
						}
					}, 250);
				}
			};
		}
		mTimeoutRunning = true;
		mHandler.postDelayed(mRunnable, mDelay);
	}
	
	
	private void broadcastShow(Context ctx, int w, int h, int g) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		int[] array = { w, h, g };
		i.putExtra(Common.INTENT_APP_SNAP_ARR, array);
		ctx.sendBroadcast(i);
	}
	
	private void broadcastHide(Context ctx) {
		ctx.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
	}

}
