package com.zst.xposed.halo.floatingwindow3.helpers;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.hooks.MovableWindow;
import de.robv.android.xposed.*;

public class AeroSnap {
	
	public final static int UNKNOWN = -10000;
	final static int MOVE_MAX_RANGE = 10;
	

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
	boolean doSync;
	float[] mPreviousRange = new float[2];
	
	/**
	 * An Aero Snap Class to check if the current pointer's coordinates
	 * are in range of the snap region.
	 */

	
	public AeroSnap(int delay) {
		mSnapWindowHolder = new SnapWindowHolder();
		mHandler = new Handler();
		mDelay = delay;
		refreshScreenSize();
	}
	
	public void dispatchTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if(!MovableWindow.mWindowHolder.isSnapped) {
				finishSnap(mSnapWindowHolder.isSnapped && mTimeoutDone);
			}
			discardTimeout();
			mChangedPreviousRange = false;
			break;
		case MotionEvent.ACTION_DOWN:
			if (!mChangedPreviousRange) {
				mPreviousRange[0] = event.getRawX();
				mPreviousRange[1] = event.getRawY();
				mChangedPreviousRange = true;
			}
			refreshScreenSize();
			break;
		case MotionEvent.ACTION_MOVE:
			if (mRestorePosition && moveRangeAboveLimit(event)) {
				restoreOldPosition();
			}
			showSnap((int) event.getRawX(), (int) event.getRawY());
			
		}
	}
	
	// check if it is moved out of the snap and not just accidently moved a few px
	private boolean moveRangeAboveLimit(MotionEvent event) {
		final float x = event.getRawX();
		final float y = event.getRawY();
		
		boolean returnValue = false;
		if (Math.abs(mPreviousRange[0] - x) > MOVE_MAX_RANGE)
			returnValue = true;
		if (Math.abs(mPreviousRange[1] - y) > MOVE_MAX_RANGE)
			returnValue = true;

		return returnValue;
	}
	
	private void showSnap(int x, int y) {
		initSnappable(x, y);
		calculateSnap();
		
		if (mSnapWindowHolder.isSnapped) {
			broadcastShowWithTimeout();
		} else {
			broadcastHide(MovableWindow.mWindowHolder.mActivity.getApplicationContext());
		}
		
	}
	
	// do the snap by setting the variables and hiding the snap preview
	private void finishSnap(boolean apply) {
		if (apply) {
			if (saveOldPosition()) {
				mRestorePosition = true;
			}
			MovableWindow.mWindowHolder.restore(mSnapWindowHolder);
			//MovableWindow.mWindowHolder.pushToWindow();
			MovableWindow.syncLayoutParams();
			//MovableWindow.mWindowHolder.isSnapped = true;

			if ((MovableWindow.mAeroSnapChangeTitleBarVisibility)&&(MovableWindow.mOverlayView!=null)) {
				MovableWindow.mOverlayView.setTitleBarVisibility(false);
			}
			//MovableWindow.toggleDragger(true);
		} else {
			restoreOldPosition();
			//MovableWindow.toggleDragger(false);
		}
		//refreshLayout();
		broadcastHide(MovableWindow.mWindowHolder.mActivity);
		MovableWindow.toggleDragger();
	}
	
	/**
	 * Forces the window to snap to this side programatically without user input
	 * @param side - Side of the screen to snap to.
	 */
	
	public void forceSnapGravity() {
		calculateSnap();
		finishSnap(true);
	}

	public void forceSnapGravityThisOnly(int sSnapGravity){
		doSync=false;
		if(sSnapGravity == 0) {
			restoreOldPosition();
			MovableWindow.showTitleBar();
			MovableWindow.toggleDragger(false);
			return;
		}
		mSnapWindowHolder.SnapGravity = sSnapGravity;
		forceSnapGravity();
	}
		
	public void forceSnapGravity(int sSnapGravity){
		doSync=true;
		if(sSnapGravity == 0) {
			restoreOldPosition();
			MovableWindow.showTitleBar();
			MovableWindow.toggleDragger(false);
			return;
		}
		mSnapWindowHolder.SnapGravity = sSnapGravity;
		forceSnapGravity();
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
		return (newSnapGravity!=0);
	}
	
	// save the position so we can restore it later
	private boolean saveOldPosition() {
		if (mRestorePosition) return true;
		if(MovableWindow.mWindowHolder.isSnapped) return (MovableWindow.mWindowHolder.SnapGravity == 0) || (mTimeoutRunning);
		MovableWindow.saveLayout();
		return true;
	}
	
	// undo the snap when moving the window out of the snap region
	public boolean restoreOldPosition() {
		if (!MovableWindow.mWindowHolder.isSnapped) return false;
		restoreOldPositionWithoutRefresh();
		//refreshLayout();
		return true;
	}
	
	public void restoreOldPositionWithoutRefresh() {
		if (!MovableWindow.mWindowHolder.isSnapped) return;
		refreshScreenSize();//this was added to fix wrong layout on orientation changed
		MovableWindow.restoreLayout();
		//MovableWindow.mWindowHolder.pushToWindow();
		MovableWindow.mWindowHolder.updateSnap(0);
		//MovableWindow.mWindowHolder.isSnapped = false;
		MovableWindow.showTitleBar();
		mRestorePosition = false;
		if (MovableWindow.mAeroSnapChangeTitleBarVisibility) {
			MovableWindow.mOverlayView.setTitleBarVisibility(true);
		}
	}
	
	// create a snap positioning based on the range of our touch coordinates
	private void calculateSnap() {
		refreshScreenSize();
		if(mSnapWindowHolder.SnapGravity == 0) {
			mSnapWindowHolder.isSnapped = false;
			return;
			}
		mSnapWindowHolder.height = ViewGroup.LayoutParams.MATCH_PARENT;//*/mScreenHeight;
		mSnapWindowHolder.width = ViewGroup.LayoutParams.MATCH_PARENT;// */mScreenWidth;
		
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
		mSnapWindowHolder.isSnapped = true;
	}
	
	// send broadcast to sync the windows
	/*private void refreshLayout() {
		MovableWindow.pullLayout();
		if(MovableWindow.mRetainStartPosition) MovableWindow.syncLayoutParams();
		else MovableWindow.pushLayout();
	}*/
	
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
					int mGravity = 0;
					if(mSnapWindowHolder.width == ViewGroup.LayoutParams.MATCH_PARENT)
						mGravity = (mSnapWindowHolder.y == 0)? Gravity.TOP:Gravity.BOTTOM;
					else if(mSnapWindowHolder.height == ViewGroup.LayoutParams.MATCH_PARENT)
						mGravity = (mSnapWindowHolder.x == 0)? Gravity.LEFT:Gravity.RIGHT;
					else {
						mGravity = mGravity | ((mSnapWindowHolder.y == 0)? Gravity.TOP:Gravity.BOTTOM);
						mGravity = mGravity | ((mSnapWindowHolder.x == 0)? Gravity.LEFT:Gravity.RIGHT);
						}
					broadcastShow(MovableWindow.mWindowHolder.mActivity,mSnapWindowHolder.width,mSnapWindowHolder.height,mGravity);
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
		try{
			XposedHelpers.callMethod(ctx, "sendBroadcast", new Intent(Common.SHOW_OUTLINE));
			} catch (Throwable t){}
		//ctx.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
	}

}
