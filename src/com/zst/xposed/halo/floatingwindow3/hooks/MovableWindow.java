package com.zst.xposed.halo.floatingwindow3.hooks;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.R;
import com.zst.xposed.halo.floatingwindow3.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow3.helpers.MovableOverlayView;
import com.zst.xposed.halo.floatingwindow3.helpers.Util;
import com.zst.xposed.halo.floatingwindow3.floatdot.XHFWInterface;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import com.zst.xposed.halo.floatingwindow3.helpers.*;
import android.content.pm.*;
import de.robv.android.xposed.*;
import android.app.*;
import android.content.*;
import android.util.*;
import com.zst.xposed.halo.floatingwindow3.floatdot.*; 


public class MovableWindow {
	
	public static void DEBUG(String tag){
//		XposedBridge.log("CHECK " + tag + " mWindows size: " + mWindows.size() + " at " + (mWindowHolder==null? " NULL " : mWindowHolder.packageName));
//		if(mWindowHolder!=null){
//			XposedBridge.log("Check " + tag + " mWH: " + mWindowHolder.packageName + " Snap: " + mWindowHolder.SnapGravity + " " + mWindowHolder.isSnapped + " gravity " + mWindowHolder.width + ":" + mWindowHolder.height + " at " + mWindowHolder.x + ":" + mWindowHolder.y);
//			XposedBridge.log("     flags " + mWindowHolder.mWindow.getAttributes().flags + "  type: " + mWindowHolder.mWindow.getAttributes().type + " for " + mWindowHolder.packageName);
//			}
//		else
//		{XposedBridge.log("Check " + tag + "mWindowHolder is NULL!!!");
//		
//		}
//		if(mWindowHolderCached!=null)
//		{
//			//mWindowHolderCached.pullFromWindow();
//			XposedBridge.log("Check " + tag + " mWHCached: " + mWindowHolderCached.packageName + " Snap: " + mWindowHolderCached.SnapGravity + " " + mWindowHolderCached.isSnapped + " gravity " + mWindowHolderCached.width + ":" + mWindowHolderCached.height + " at " + mWindowHolderCached.x + ":" + mWindowHolderCached.y);
//			}
	}
	
	static final int ID_NOTIFICATION_RESTORE = 22222222;
	static final String INTENT_APP_PKG = "pkg";

	static MainXposed mMainXposed;
	static Resources mModRes;
	static XSharedPreferences mPref;
	
	/* App ActionBar Moving Values */
	private Float screenX;
	private Float screenY;
	private Float viewX;
	private Float viewY;
	private Float leftFromScreen;
	private Float topFromScreen;

	static Activity mActivity; // Current app activity
	static boolean mActionBarDraggable;
	static boolean mMinimizeToStatusbar;
	public static WindowHolder mWindowHolder = null;
	public static WindowHolder mWindowHolderCached = null;
	private static ArrayList<Window> mWindows = new ArrayList<Window>();
	
	
	public static boolean mRetainStartPosition;
	public static boolean mConstantMovePosition;
	static int mPreviousOrientation;
	static int mPreviousRotation;
	
	public static MovableOverlayView mOverlayView;
	public static boolean mMaximizeChangeTitleBarVisibility;

	/* AeroSnap*/
	public static AeroSnap mAeroSnap = null;
	public static boolean mAeroSnapChangeTitleBarVisibility;
	public static boolean mAeroSnapEnabled;
	static int mAeroSnapDelay;
	static boolean mAeroSnapSwipeApp;
	static int mPreviousForceAeroSnap;
	
	//Service
	static XHFWInterface XHFWInterfaceLink = null;
	
	//Float dot
	static int mScreenHeight;
	static int mScreenWidth;
	public static int[] mFloatDotCoordinates = new int[2];


	@SuppressWarnings("static-access")
	public MovableWindow(MainXposed main, LoadPackageParam lpparam) throws Throwable {
		mMainXposed = main;
		mModRes = main.sModRes;
		mPref = main.mPref;
		
		hook_activity();
		inject_dispatchTouchEvent();

	}
	/***********************************************************/
	/*********** ACTIVITY HOOKS ********************************/
	/***********************************************************/
	
	private void hook_activity(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					mActivity = (Activity) param.thisObject;
					/*  We don't touch floating dialogs  */
					if (mActivity.getWindow().isFloating()) return;
					/* Setup window holder */
					if(!initWindow()) return;
					/* if it is movable - continue and add it to windows array */
					mWindows.add(mWindowHolder.mWindow);
					/* we need to snap from very begining */
					if((mAeroSnap!=null)&&(mWindowHolder.isSnapped)) mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
					/* add overlay */
					setOverlayView();
					showTitleBar();
					DEBUG("onCreate");
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					DEBUG("onStartSTART");
					mActivity = (Activity) param.thisObject;
					/*  We don't touch floating dialogs  */
					if (mActivity.getWindow().isFloating()) return;
					/* no need to act if it's not movable */
					if(!mWindowHolder.isMovable) return;
					// Add our overlay view
					setOverlayView();
					showTitleBar();
					DEBUG("onStartEND");
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					DEBUG("onResumeSTART");
					mActivity = (Activity) param.thisObject;
					/*  We don't touch floating dialogs  */
					if (mActivity.getWindow().isFloating()) return;
					/* no need to act if it's not movable */
					if(!mWindowHolder.isMovable) return;		
					/** update current window **/
					if(mWindowHolder!=null) //
						mWindowHolder.setWindow(mActivity);
					/* check if we need to show or hide floatdot */
					toggleDragger();
					/* FIX FORCED ORIENTATION ON MOVABLE WINDOWS */
					mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);		
					/* bring overlay to front */
					putOverlayView();
					showTitleBar();
					/* reconnect XHFWService if needed */
					connectService();
					//restore snap layout if needed
					if(mWindowHolder.isSnapped&&mAeroSnap!=null) mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
					else /* restore layout */
						pushLayout();
					/* make all windows of the activity to keep layout - they loose it constantly! */
					syncLayoutParams();
					DEBUG("onResumeEND");
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onConfigurationChanged", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mActivity = (Activity) param.thisObject;
					DEBUG("ACTION_CONFIGURATION_CHANGED");
					/*  We don't touch floating dialogs  */
					if (mActivity.getWindow().isFloating()) return;
					/* no need to act if it's not movable */
					if(!mWindowHolder.isMovable) return;
					int curRotation = Util.getDisplayRotation(mActivity);
//					mFloatDotCoordinates = new int[]{mFloatDotCoordinates[Util.rollInt(0,1,mWindowHolder.cachedRotation-curRotation)], mFloatDotCoordinates[Util.rollInt(0,1,mWindowHolder.cachedRotation-curRotation+1)]};
//					
//					mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
					mWindowHolder.cachedRotation = curRotation;
					connectService();
					return;
				}
			});
		
		XposedBridge.hookAllMethods(Activity.class, "onPause", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mActivity = (Activity) param.thisObject;
					DEBUG("onPause");
					/*  We don't touch floating dialogs  */
					if (mActivity.getWindow().isFloating()) return;
					/* no need to act if it's not movable */
					if(!mWindowHolder.isMovable) return;		
					/* disable dragger */
					toggleDragger(false);
					return;
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					DEBUG("onDestroy");
					/*  We don't touch floating dialogs  */
					if (((Activity)param.thisObject).getWindow().isFloating()) return;
					/* no need to act if it's not movable */
					if(mWindowHolder==null||!mWindowHolder.isMovable) return;			
					/* remove from window stack */
					mWindows.remove(((Activity)param.thisObject).getWindow());
					if(mWindows.size()<1) {
						mWindowHolder = null;
					}
					// hide the resizing outline
					((Activity)param.thisObject).sendBroadcast(new Intent(Common.SHOW_OUTLINE));
					return;
				}
			});
	}
	
	/***********************************************************/
	/********************* INIT ********************************/
	/***********************************************************/
	
	private static boolean initWindow(){ //return: continue?
		/** if it is not the first call just update current window **/
		if(mWindowHolder!=null){ //
			mWindowHolder.setWindow(mActivity);
			return (mWindowHolder.isMovable);
		}	
		/*********** this is what we need to do once **********/
		/* load and set prefs */
		loadPrefs();
		/* setup windowholder - to keep and manipulate window layouts */
		mWindowHolder = new WindowHolder(mActivity, mPref);
		/* setup aerosnap class - to manage snap-to-side windows */
		mAeroSnap = mAeroSnapEnabled ? new AeroSnap(mAeroSnapDelay) : null;
		/* set current window */
		mWindowHolder.setWindow(mActivity);
		/* check white and blacklists */
		//mWindowHolder.isMovable = blackWhiteListed();
		/* no need to go on if is not movable */
		if(!mWindowHolder.isMovable) return false;
		mPref.reload();
		/* we should set proper halo flags */
		setIntentFlags();
		/* FIX focus other windows not working on some apps */
		mWindowHolder.mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		/* Fix Chrome dim */
		if(!mWindowHolder.mWindow.getContext().getPackageName().startsWith("com.android.chrome"))
			mWindowHolder.mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		/* set initial layout */
		refreshScreenSize();
		setInitLayout();
		pushLayout();
		return true;
	}
	
	private static void loadPrefs(){
		mPref.reload();
		mActionBarDraggable = mPref.getBoolean(Common.KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED, Common.DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED);
		mRetainStartPosition = mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION, Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION);
		mConstantMovePosition = mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION, Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION);

		mPreviousOrientation = mActivity.getResources().getConfiguration().orientation;
		mMinimizeToStatusbar = mPref.getBoolean(Common.KEY_MINIMIZE_APP_TO_STATUSBAR, Common.DEFAULT_MINIMIZE_APP_TO_STATUSBAR);
		
		mAeroSnapEnabled = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED);
		mAeroSnapDelay = mPref.getInt(Common.KEY_WINDOW_RESIZING_AERO_SNAP_DELAY, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY);
		mAeroSnapSwipeApp = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP);
		mAeroSnapChangeTitleBarVisibility = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE);
		mMaximizeChangeTitleBarVisibility = mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_MAXIMIZE_HIDE, Common.DEFAULT_WINDOW_TITLEBAR_MAXIMIZE_HIDE);
	}
	
	private static void setIntentFlags(){
		Intent mIntent = mWindowHolder.mActivity.getIntent();
		int flags = mIntent.getFlags();
		flags = flags | mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		flags = flags | Intent.FLAG_ACTIVITY_NO_USER_ACTION;
		flags &= ~Intent.FLAG_ACTIVITY_TASK_ON_HOME;

		if (!mPref.getBoolean(Common.KEY_SHOW_APP_IN_RECENTS, Common.DEFAULT_SHOW_APP_IN_RECENTS)) {
			flags = flags | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
		} else if (mPref.getBoolean(Common.KEY_FORCE_APP_IN_RECENTS, Common.DEFAULT_FORCE_APP_IN_RECENTS)) {
			flags &= ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
		}
		mIntent.setFlags(flags);
		mWindowHolder.mActivity.setIntent(mIntent);
	}
	
	private static void setInitLayout(){
		mWindowHolder.mWindow.setGravity(mPref.getInt(Common.KEY_GRAVITY, Common.DEFAULT_GRAVITY));
		WindowManager.LayoutParams params = mWindowHolder.mWindow.getAttributes();
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(params);
		mWindowHolder.mWindow.setAttributes(params);
		mWindowHolder.mWindow.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
		mWindowHolder.mWindow.setWindowAnimations(android.R.style.Animation_Dialog);
		mWindowHolder.mWindow.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
		switch(mPref.getInt(Common.KEY_KEYBOARD_MODE, Common.DEFAULT_KEYBOARD_MODE)){
			case 2:
				mWindowHolder.mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
				break;
			case 3:
				mWindowHolder.mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
				break;
		}
		switch(Util.getScreenOrientation(mWindowHolder.mActivity)){
			case Configuration.ORIENTATION_LANDSCAPE:
				mWindowHolder.width = (int) (mScreenWidth * mPref.getFloat(Common.KEY_LANDSCAPE_WIDTH, Common.DEFAULT_LANDSCAPE_WIDTH));
				mWindowHolder.height = (int) (mScreenHeight * mPref.getFloat(Common.KEY_LANDSCAPE_HEIGHT, Common.DEFAULT_LANDSCAPE_HEIGHT));
				break;
			case Configuration.ORIENTATION_PORTRAIT:
			default:
				mWindowHolder.width = (int) (mScreenWidth * mPref.getFloat(Common.KEY_PORTRAIT_WIDTH, Common.DEFAULT_PORTRAIT_WIDTH));
				mWindowHolder.height = (int) (mScreenHeight * mPref.getFloat(Common.KEY_PORTRAIT_HEIGHT, Common.DEFAULT_PORTRAIT_HEIGHT));
				break;
		}
		mWindowHolder.x = (mScreenWidth-mWindowHolder.width)/2;
		mWindowHolder.y = (mScreenHeight-mWindowHolder.height)/2;
	}
	
	
	/***********************************************************/
	/************ FLOATING DOT COMMUNICATION *******************/
	/***********************************************************/
	final static ServiceConnection XHFWServiceConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder)
		{
			XHFWInterfaceLink = XHFWInterface.Stub.asInterface(binder);				
			getFloatingDotCoordinates();
			registerLayoutBroadcastReceiver();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			XHFWInterfaceLink = null;
			unregisterLayoutBroadcastReceiver();
		}	
	};

	public static void connectService(){
		DEBUG("connectService");
		if(XHFWInterfaceLink == null){
			Intent intent = new Intent(Common.FLOAT_DOT_SERVICE_ACTION);
			intent.setPackage(Common.FLOAT_DOT_PACKAGE);
			mWindowHolder.mActivity.bindService(intent, XHFWServiceConnection, Service.BIND_AUTO_CREATE);
		}

	}
	
	
	final static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			/*if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {

			 DEBUG("ACTION_CONFIGURATION_CHANGED");
			 if(activity!=null) mWindowHolder.setWindow(activity);
			 else mWindowHolder.setWindow(window);
			 int curRotation = Util.getDisplayRotation(activity);
			 if((curRotation != mWindowHolder.cachedRotation)&&(mWindowHolder.isSnapped)){
			 if(mWindowHolder.SnapGravity == 0) mWindowHolder.restoreSnap();
			 ArrayList<Integer> RotationSnaps = new ArrayList<Integer>(Arrays.asList(Gravity.TOP,Gravity.RIGHT,Gravity.BOTTOM,Gravity.LEFT));
			 int newFlag = 0;
			 for(int i : RotationSnaps){
			 if((mWindowHolder.SnapGravity & i)==i){
			 newFlag=newFlag | RotationSnaps.get(Util.rollInt(RotationSnaps.indexOf(i),RotationSnaps.size()-1,mWindowHolder.cachedRotation-curRotation));
			 }
			 }
			 mAeroSnap.forceSnapGravity(newFlag);
			 mWindowHolder.cachedRotation = curRotation;
			 }
			 return;
			 }*/

			if (intent.getAction().equals(Common.REFRESH_FLOAT_DOT_POSITION)){
				DEBUG( "REFRESH_FLOATDOT_POSITION");
				int[] coordinates = intent.getIntArrayExtra(Common.INTENT_FLOAT_DOT_EXTRA);
				if(coordinates == null) return;
				mFloatDotCoordinates=coordinates;
				if(!mWindowHolder.isSnapped) return;
				if(mWindowHolder.SnapGravity==0) mWindowHolder.restoreSnap();
				if(mAeroSnap!=null){
					mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
				}
				//syncLayoutParams();
				//toggleDragger(true);
			}
		}
	};
	
	public static void registerLayoutBroadcastReceiver() {
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_FLOAT_DOT_POSITION);
		mWindowHolder.mActivity.registerReceiver(mBroadcastReceiver, filters);
		//setTagInternalForView(window.getDecorView(), Common.LAYOUT_RECEIVER_TAG, br);
	}
	
	private static void unregisterLayoutBroadcastReceiver() {
		mWindowHolder.mActivity.unregisterReceiver(mBroadcastReceiver);
	}

	private static void changeFocusApp(Activity a) {
		if(XHFWInterfaceLink==null) {connectService();}
		try		{
			XHFWInterfaceLink.bringToFront(a.getTaskId());
		}
		catch (RemoteException e)
		{
			XposedBridge.log("changeFocusApp failed");
		}
		toggleDragger();
	}
	
	public static void toggleDragger(){
		if(mWindowHolder.isSnapped) toggleDragger(true);
		else toggleDragger(false);
	}
	
	public static void toggleDragger(boolean show){
		Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
		intent.putExtra(Common.INTENT_FLOAT_DOT_BOOL, show);
		mWindowHolder.mActivity.sendBroadcast(intent);
	}
	
	/***********************************************************/
	/**************** Titlebar and corners *********************/
	/***********************************************************/
	
	
	public static void showTitleBar(){
		DEBUG("showTitleBar");
		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
		if(mWindowHolder.mWindow.isFloating()) return;
		if(mOverlayView == null) return;
		toggleDragger(mWindowHolder.isSnapped);

		boolean is_maximized = 
			mWindowHolder.mWindow.getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT &&
			mWindowHolder.mWindow.getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT;
		if ((mMaximizeChangeTitleBarVisibility && is_maximized) ||
			(mAeroSnapChangeTitleBarVisibility && mWindowHolder.isSnapped)) {
			mOverlayView.setTitleBarVisibility(false);
		}
		else mOverlayView.setTitleBarVisibility(true);
	}

	public static void setOverlayView(){
		FrameLayout decorView = null;
		try{
		decorView = (FrameLayout) mWindowHolder.mActivity.getWindow().peekDecorView().getRootView();
		
		} catch (NullPointerException e){
			//decorView = (FrameLayout) mWindowHolder.mWindow.peekDecorView().getRootView();
		}
		if (decorView == null) return;
		// make sure the titlebar/drag-to-move-bar is not behind the statusbar
		decorView.setFitsSystemWindows(true);
		try {
			// disable resizing animation to speed up scaling (doesn't work on all roms)
			XposedHelpers.callMethod(decorView, "hackTurnOffWindowResizeAnim", true);
		} catch (Throwable e) {
		}

		mOverlayView = (MovableOverlayView) decorView.getTag(Common.LAYOUT_OVERLAY_TAG);
		for (int i = 0; i < decorView.getChildCount(); ++i) {
			final View child = decorView.getChildAt(i);
			if (child instanceof MovableOverlayView && mOverlayView != child) {
				// If our tag is different or null, then the
				// view we found should be removed by now.
				decorView.removeView(decorView.getChildAt(i));
				break;
			}
		}

		if (mOverlayView == null) {
			mOverlayView = new MovableOverlayView(mMainXposed, mActivity, mModRes, mPref, mAeroSnap);
			decorView.addView(mOverlayView, -1, MovableOverlayView.getParams());
			setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG, mOverlayView);
		}
	}
	
	private static void putOverlayView(){
		FrameLayout decor_view = (FrameLayout) mActivity.getWindow().peekDecorView().getRootView();
		//mOverlayView = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
		decor_view.bringChildToFront(mOverlayView);
	}
	
	public static void getFloatingDotCoordinates(){
		try
		{
			mFloatDotCoordinates[0]=mScreenWidth/2;
			mFloatDotCoordinates[1]=mScreenHeight/2;
			int[] result = XHFWInterfaceLink.getCurrentFloatdotCoordinates();
			if(result==null) return;
			mFloatDotCoordinates = result;
			if(mWindowHolder.isSnapped) toggleDragger(true);
			/*We need to forceSnap once on create because some apps are started snapped with wrong params - so we just fix the layout*/
			if((!mWindowHolder.isSet)&&(mWindowHolder.isSnapped)&&(mAeroSnap!=null)){
				mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);

				mWindowHolder.isSet=true;
			}
		} catch (RemoteException e) {}
	}
	
	/***********************************************************/
	/********************* LAYOUT ******************************/
	/***********************************************************/
	
	public static void pullLayout(){
		mWindowHolder.pullFromWindow();
		DEBUG("pullLayout");
		//saveLayout();
	}

	public static void pushLayout(){
		DEBUG("pushLayout");
		mWindowHolder.pushToWindow();
	}

	public static void saveLayout(){
		mWindowHolderCached = mWindowHolder;
		DEBUG("savedLayout");
	}

	public static void restoreLayout(){
		if(mWindowHolderCached==null) return;
		mWindowHolder.restore(mWindowHolderCached);
		if(mWindowHolder.width == 0) mWindowHolder.width = -1;
		if(mWindowHolder.height == 0) mWindowHolder.height = -1;
		DEBUG("restoredLayout");
	}
	
	public static boolean pullAndSyncLayoutParams() {
		if (!mRetainStartPosition) return false;
		//pushLayout();
		pullLayout();
		syncLayoutParams();
		return true;
	}

	public static void syncLayoutParams() {

		if (!mRetainStartPosition) return;
		for(Window w : mWindows){
			mWindowHolder.pushToWindow(w);
		}
	}
	
	/***********************************************************/
	/********************* ALIA ********************************/
	/***********************************************************/
	
	
	// maximize and restore the window.
	public void maximizeApp(Activity activity) {
		/*TODO make maximized just another snap mode (Gravity.FILL)*/
		if ((activity.getWindow().getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT) ||
			(activity.getWindow().getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT)) {
			if (mWindowHolder.isSnapped) {
				// we need to maximize instead of restoring since it is snapped to the edge
				mAeroSnap.restoreOldPositionWithoutRefresh();
				// dont refresh since we need to maximize it again
				mWindowHolder.setWindow(activity);
				pullLayout();
				// save our unsnapped position, then maximize
				activity.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
						ViewGroup.LayoutParams.MATCH_PARENT);
				return;
			}
			pushLayout();
			if (mMaximizeChangeTitleBarVisibility) {
				mOverlayView.setTitleBarVisibility(true);
			}
		} else {
			mWindowHolder.setWindow(activity);
			pullLayout();
			activity.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.MATCH_PARENT);
			if (mMaximizeChangeTitleBarVisibility) {
				mOverlayView.setTitleBarVisibility(false);
			}
		}
		// after that, send a broadcast to sync the position of the window
		pullAndSyncLayoutParams();
	}

	// Send the app to the back, and show a notification to restore
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public void minimizeAndShowNotification(final Activity ac) {
		if (!mMinimizeToStatusbar) {
			ac.moveTaskToBack(true);
			return;
		}

		Intent i = new Intent(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName());
		ApplicationInfo app_info = ac.getApplication().getApplicationInfo();
		PendingIntent intent = PendingIntent.getBroadcast(ac, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT);
		String title = String.format(mModRes.getString(R.string.dnm_minimize_notif_title),
        		app_info.loadLabel(ac.getPackageManager()));

		Notification.Builder nb = new Notification.Builder(ac)
		        .setContentTitle(title)
		        .setContentText(mModRes.getString(R.string.dnm_minimize_notif_summary))
		        .setSmallIcon(app_info.icon)
		        .setAutoCancel(true)
		        .setContentIntent(intent)
		        .setOngoing(true);
		
		Notification n;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			n = nb.build();
		} else {
			n = nb.getNotification();
		}
		
		final NotificationManager notificationManager =
		  (NotificationManager) ac.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ID_NOTIFICATION_RESTORE, n);

		ac.moveTaskToBack(true);

		ac.registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				notificationManager.cancel(ID_NOTIFICATION_RESTORE);
				changeFocusApp(ac);
				context.unregisterReceiver(this);
			}
		}, new IntentFilter(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName()));
	}

	// hook the touch events to move the window and have aero snap.
	private void inject_dispatchTouchEvent() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				if(!mWindowHolder.isMovable) return;

				Activity a = (Activity) param.thisObject;
				mWindowHolder.setWindow(a);
				MotionEvent event = (MotionEvent) param.args[0];
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					viewX = event.getX();
					viewY = event.getY();
					changeFocusApp(a);
					if (a.getWindow().getAttributes().gravity != (Gravity.LEFT | Gravity.TOP)) {
						// Fix First Resize moving into corner
						screenX = event.getRawX();
						screenY = event.getRawY();
						leftFromScreen = (screenX - viewX);
						topFromScreen = (screenY - viewY);
						a.getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
						updateView(a.getWindow(), leftFromScreen, topFromScreen);
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (mActionBarDraggable) {
					ActionBar ab = a.getActionBar();
					int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());

					if (viewY < height) {
						screenX = event.getRawX();
						screenY = event.getRawY();
						leftFromScreen = (screenX - viewX);
						topFromScreen = (screenY - viewY);
						Window mWindow = a.getWindow();
						mWindow.setGravity(Gravity.LEFT | Gravity.TOP);
						updateView(mWindow, leftFromScreen, topFromScreen);
						if (mAeroSnap != null) {
							mAeroSnap.dispatchTouchEvent(event);
						}
						pullAndSyncLayoutParams();
					}
					}
					break;
				}
				ActionBar ab = a.getActionBar();
				int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());
				if (viewY < height && mAeroSnap != null && mActionBarDraggable) {
					mAeroSnap.dispatchTouchEvent(event);
				}
				
			}
		});
	}

	private static void updateView(Window mWindow, float x, float y) {
		WindowManager.LayoutParams params = mWindow.getAttributes();
		params.x = (int) x;
		params.y = (int) y;
		mWindow.setAttributes(params);
	}
	
	private static void setTagInternalForView(View view, int key, Object object) {
		Class<?>[] classes = { Integer.class, Object.class };
		XposedHelpers.callMethod(view, "setTagInternal", classes, key, object);
		// view.setTagInternal(key, object);
	}
	
	private static void refreshScreenSize() {
		final WindowManager wm = (WindowManager) MovableWindow.mWindowHolder.mActivity.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}
}
