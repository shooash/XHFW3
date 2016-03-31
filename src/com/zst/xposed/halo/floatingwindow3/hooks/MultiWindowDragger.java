package com.zst.xposed.halo.floatingwindow3.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow3.helpers.MultiWindowRecentsManager;
import com.zst.xposed.halo.floatingwindow3.helpers.Util;
//import com.zst.xposed.halo.floatingwindow3.hooks.ipc.XHFWInterface;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.helpers.*;
import android.content.res.*;
import android.graphics.drawable.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.widget.*;
import android.util.*;
import android.graphics.*;

public class MultiWindowDragger
{
	private static Context mContext;
	
	public static Drawable mTouchDrawableTB;
	public static Drawable mTouchDrawableLR;
	public static Drawable mTouchDrawableTBWhite;
	public static Drawable mTouchDrawableLRWhite;
	public static int mCircleDiameter;
	public static int mColor = 0;
	public static int mScreenWidth;
	public static int mScreenHeight;
	
	public static WindowManager mWindowManager;
	// Main Circle Dragger
	public static WindowManager.LayoutParams mContentParamz;
	public static ImageView mViewContent;
	
	public static void handleLoadPackage(LoadPackageParam lpparam) {
		if (!lpparam.packageName.equals("com.android.systemui")) return;

		try {
			final Class<?> hookClass = findClass("com.android.systemui.SystemUIService",
												 lpparam.classLoader);
			XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
					@Override
					protected void afterHookedMethod(MethodHookParam param) throws Throwable {
						final Service thiz = (Service) param.thisObject;
						mContext = thiz.getApplicationContext();
						initDragger(MainXposed.sModRes, Util.realDp(24, mContext));
						createDraggerView();
						updateDraggerView(true);
						//mXHFWService = XHFWService.retrieveService(mContext);
						/*mViewManager = new MultiWindowViewManager(mContext, MainXposed.sModRes,
																  Util.realDp(24, mContext));
						// TODO option to change size

						mContext.registerReceiver(BROADCAST_RECEIVER,
												  new IntentFilter(Common.SHOW_MULTIWINDOW_DRAGGER));
						mContext.registerReceiver(APP_TOUCH_RECEIVER,
												  new IntentFilter(Common.SEND_MULTIWINDOW_APP_FOCUS));
						mContext.registerReceiver(new BroadcastReceiver() {
								@Override
								public void onReceive(Context context, Intent intent) {
									swipeToNextApp(intent.getStringExtra(Common.INTENT_APP_ID), // pkg name
												   //intent.getIntExtra(Common.INTENT_APP_SNAP, AeroSnap.SNAP_NONE),
												   intent.getIntExtra(Common.INTENT_APP_SNAP_GRAVITY, 0),//GRAMOD
												   intent.getBooleanExtra(Common.INTENT_APP_EXTRA, false)); // direction
								}
							}, new IntentFilter(Common.SEND_MULTIWINDOW_SWIPE));

						mRecentsManager = new MultiWindowRecentsManager(mContext) {
							@Override
							public void onRemoveApp(String pkg_name) {
								mTopList.remove(pkg_name);
								mBottomList.remove(pkg_name);
								mLeftList.remove(pkg_name);
								mRightList.remove(pkg_name);
								//4WAYMOD
								mTopLeftList.remove(pkg_name);
								mTopRightList.remove(pkg_name);
								mBottomLeftList.remove(pkg_name);
								mBottomRightList.remove(pkg_name);
								showDragger();
							}
						};*/
					}
				});
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / SystemUIMultiWindow");
			XposedBridge.log(e);
		}
	}
	
	static void initDragger(Resources res, int circle_size){
		mTouchDrawableTB = res.getDrawable(R.drawable.multiwindow_dragger_press_ud);
		mTouchDrawableLR = res.getDrawable(R.drawable.multiwindow_dragger_press_lr);
		mTouchDrawableTBWhite = res.getDrawable(R.drawable.multiwindow_dragger_press_ud_white);
		mTouchDrawableLRWhite = res.getDrawable(R.drawable.multiwindow_dragger_press_lr_white);
		mCircleDiameter = circle_size;
	}
	
	public static WindowManager getWM() {
		if (mWindowManager == null) {
			mWindowManager = (WindowManager)
				mContext.getSystemService(Context.WINDOW_SERVICE);
		}
		return mWindowManager;
	}
	
	public static View createDraggerView() {
		//removeDraggerView();
		// try removing in case it wasn't already
		// so as to prevent duplicated views

		mViewContent = new ImageView(mContext);
		mViewContent.setImageDrawable(Util.makeCircle(mColor, mCircleDiameter));

		refreshScreenSize();

		mContentParamz = new WindowManager.LayoutParams(
			mCircleDiameter,
			mCircleDiameter,
			WindowManager.LayoutParams.TYPE_PHONE,
			0 | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
			WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
			WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
			WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
			PixelFormat.TRANSLUCENT);
		mContentParamz.gravity = Gravity.TOP | Gravity.LEFT;
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(mContentParamz);
		mContentParamz.x = (mScreenWidth / 2) - (mCircleDiameter / 2);
		mContentParamz.y = (mScreenHeight / 2) - (mCircleDiameter / 2);
		mViewContent.setLayoutParams(mContentParamz);
		return mViewContent;
		//return null;
	}
	
	/*private static void showDragger() {
		
			mViewManager.setColor(mColor);
			mViewManager.createDraggerView();
			mViewManager.mViewContent.setOnTouchListener(DRAG_LISTENER);
			mViewManager.mViewContent.setOnClickListener(DRAGGER_MENU);
		}

		if (mUseOldDraggerLocation && mPixelsFromEdge != -1) {
			if (mSplits[0]&&mSplits[2]){
				mViewManager.mContentParamz.x = (int) (mPixelsFromEdge -
					(0.5f * mViewManager.mCircleDiameter));
				mViewManager.mContentParamz.y = (int) (mPixelsFromSideY -
					(0.5f * mViewManager.mCircleDiameter));
			}
			else if (mSplits[1]&&mSplits[3]) {
				mViewManager.mContentParamz.y = (int) (mPixelsFromEdge -
					(0.5f * mViewManager.mCircleDiameter));
				mViewManager.mContentParamz.x = (int) (mPixelsFromSideX -
					(0.5f * mViewManager.mCircleDiameter));
			}
			//TODO get rid of pixels from edge
			mUseOldDraggerLocation = false;
		} else {
			if (mSplits[1]&&mSplits[3]) {
				mPixelsFromEdge = mViewManager.mScreenHeight / 2;
			} else if (mSplits[0]&&mSplits[2]) {
				mPixelsFromEdge = mViewManager.mScreenWidth / 2;
			}
		}

		mViewManager.updateDraggerView(true);
	}*/
	
	public static void updateDraggerView(boolean add) {
		if (add) {
			try {
				getWM().addView(mViewContent, mContentParamz);
			} catch (Exception e) {
				// it is already added
			}
		} else {
			try {
				getWM().updateViewLayout(mViewContent, mContentParamz);
			} catch (Exception e) {
				updateDraggerView(true);
				// it is not added yet
			}
		}
	}
	
	private static void refreshScreenSize(){
		DisplayMetrics metrics = new DisplayMetrics();
		Display display = getWM().getDefaultDisplay();
		display.getMetrics(metrics);
		mScreenWidth = metrics.widthPixels;
		mScreenHeight = metrics.heightPixels;
	}
}
