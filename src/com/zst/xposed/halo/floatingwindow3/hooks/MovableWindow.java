package com.zst.xposed.halo.floatingwindow3.hooks;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.R;
import com.zst.xposed.halo.floatingwindow3.helpers.AeroSnap;
import com.zst.xposed.halo.floatingwindow3.helpers.MovableOverlayView;
//import com.zst.xposed.halo.floatingwindow3.helpers.MultiWindowAppManager;
//import com.zst.xposed.halo.floatingwindow3.helpers.SwipeToNextApp;
import com.zst.xposed.halo.floatingwindow3.helpers.Util;
//import com.zst.xposed.halo.floatingwindow3.hooks.ipc.XHFWService;
import com.zst.xposed.halo.floatingwindow3.floatdot.XHFWInterface;
//import com.zst.xposed.halo.floatingwindow3.hooks.ipc.XHFWInterface;
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


public class MovableWindow {
	
	static void DEBUG(String tag){
//		if(mWindowHolder!=null)
//			XposedBridge.log("Check " + tag + " mWH: " + mWindowHolder.packageName + " Snap: " + mWindowHolder.SnapGravity + " " + mWindowHolder.isSnapped + " gravity " + mWindowHolder.width + ":" + mWindowHolder.height + " at " + mWindowHolder.x + ":" + mWindowHolder.y);
//		else
//			XposedBridge.log("Check " + tag + "mWindowHolder is NULL!!!");
//		XposedBridge.log("     flags " + mWindowHolder.mWindow.getAttributes().flags + "  type: " + mWindowHolder.mWindow.getAttributes().type + " for " + mWindowHolder.packageName);
//		if(mWindowHolderCached!=null)
//		{
//			//mWindowHolderCached.pullFromWindow();
//			XposedBridge.log("Check " + tag + " mWHCached: " + mWindowHolderCached.packageName + " Snap: " + mWindowHolderCached.SnapGravity + " " + mWindowHolderCached.isSnapped + " gravity " + mWindowHolderCached.width + ":" + mWindowHolderCached.height + " at " + mWindowHolderCached.x + ":" + mWindowHolderCached.y);
//			}
	}
	
	static final int ID_NOTIFICATION_RESTORE = 22222222;
	static final String INTENT_APP_PKG = "pkg";

	final MainXposed mMainXposed;
	final Resources mModRes;
	final XSharedPreferences mPref;
	
	/* App ActionBar Moving Values */
	private Float screenX;
	private Float screenY;
	private Float viewX;
	private Float viewY;
	private Float leftFromScreen;
	private Float topFromScreen;

	Activity mActivity; // Current app activity
	boolean mActionBarDraggable;
	boolean mMinimizeToStatusbar;
	public static WindowHolder mWindowHolder = null;
	public static WindowHolder mWindowHolderCached = null;
	
	public static boolean mRetainStartPosition;
	public static boolean mConstantMovePosition;
	static int mPreviousOrientation;
	static int mPreviousRotation;
	
	public static MovableOverlayView mOverlayView;
	public static boolean mMaximizeChangeTitleBarVisibility;

	/* AeroSnap*/
	public static AeroSnap mAeroSnap = null;
	public static boolean mAeroSnapChangeTitleBarVisibility;
	boolean mAeroSnapEnabled;
	int mAeroSnapDelay;
	boolean mAeroSnapSwipeApp;
	int mPreviousForceAeroSnap;
	
	//Service
	static ServiceConnection XHFWServiceConnection = null;
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
		
		activityHook();
		inject_dispatchTouchEvent();

		try {
			injectTriangle(lpparam);
		} catch (Exception e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / inject_DecorView_generateLayout");
			XposedBridge.log(e);
		}
	}
	
	private void loadPrefs(){
		mPref.reload();
		mActionBarDraggable = mPref.getBoolean(Common.KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED, Common.DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED);
		mRetainStartPosition = mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION, Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION);
		mConstantMovePosition = mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION, Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION);

		mPreviousOrientation = mActivity.getResources().getConfiguration().orientation;
		mMinimizeToStatusbar = mPref.getBoolean(Common.KEY_MINIMIZE_APP_TO_STATUSBAR, Common.DEFAULT_MINIMIZE_APP_TO_STATUSBAR);
		//4WAYMOD
		Display display = ((WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		mPreviousRotation = display.getRotation();


		mAeroSnapEnabled = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED);
		mAeroSnapDelay = mPref.getInt(Common.KEY_WINDOW_RESIZING_AERO_SNAP_DELAY, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY);
		mAeroSnapSwipeApp = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP);
		mAeroSnapChangeTitleBarVisibility = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE);
		mMaximizeChangeTitleBarVisibility = mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_MAXIMIZE_HIDE, Common.DEFAULT_WINDOW_TITLEBAR_MAXIMIZE_HIDE);
	}
	
	static private void connectService(){
		DEBUG("connectService");
		if(mWindowHolder.serviceConnected)
			{
				getFloatingDotCoordinates();
				return;
				}
		if(XHFWServiceConnection==null)
		XHFWServiceConnection = new ServiceConnection(){
			@Override
			public void onServiceConnected(ComponentName name, IBinder binder)
			{
				XHFWInterfaceLink = XHFWInterface.Stub.asInterface((IBinder) binder);				
				getFloatingDotCoordinates();
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{
				XHFWInterfaceLink = null;
				mWindowHolder.serviceConnected=false;
			}	
		};
		
		if(XHFWInterfaceLink == null){
			Intent intent = new Intent(Common.FLOAT_DOT_SERVICE_ACTION);
			intent.setPackage(Common.FLOAT_DOT_PACKAGE);
			mWindowHolder.mActivity.bindService(intent, XHFWServiceConnection, Service.BIND_AUTO_CREATE);
		}
		
	}

	private void setDebutHaloLayout(){
		refreshScreenSize();
		/*FIX focus other windows not working on some apps*/
		mWindowHolder.mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		LayoutScaling.appleFloating(mPref, mWindowHolder.mWindow);
		pullLayout();
		mWindowHolder.x = (mScreenWidth-mWindowHolder.width)/2;
		mWindowHolder.y = (mScreenHeight-mWindowHolder.height)/2;
		pushLayout();
		connectService();
	}
	
	public static void getFloatingDotCoordinates(){
		try
		{
			mFloatDotCoordinates[0]=mScreenWidth/2;
			mFloatDotCoordinates[1]=mScreenHeight/2;
			int[] result = XHFWInterfaceLink.getCurrentFloatdotCoordinates();
			if(result==null) return;
			mFloatDotCoordinates = result;
			/*We need to forceSnap once on create because some apps are started snapped with wrong params - so we just fix the layout*/
			if((!mWindowHolder.isSet)&&(mWindowHolder.isSnapped)&&(mAeroSnap!=null)){
				mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
				mWindowHolder.isSet=true;
			}
		} catch (RemoteException e) {}
	}

	private boolean recheckFloating(Activity sActivity){
		/*this is to fix wrong size and black screen when movable app is closed and reopened via launcher*/
		return (sActivity.getIntent().getFlags() & mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW))
				== mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
	}

	private void activityHook(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				mActivity = (Activity) param.thisObject;
				if (mWindowHolder == null) {
					mWindowHolder = new WindowHolder(mActivity, mPref);
					/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
					if (mWindowHolder.mWindow.isFloating()) return;
					if (!mWindowHolder.isMovable) return;
					refreshScreenSize();
					setDebutHaloLayout();
				} else mWindowHolder.setWindow(mActivity);
				/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
				if (mWindowHolder.mWindow.isFloating()) return;//MODAL FIX
				if (!mWindowHolder.isMovable) return;
				/*TODO fix wrong size and black screen when movable app is closed and reopened via launcher*/
				/*if(recheckFloating(mWindowHolder.mActivity)) {
					mWindowHolder.isFloating=false;	mWindowHolder.isMovable=false; return;
				}*/
				loadPrefs();
				if (mAeroSnap == null)
					mAeroSnap = mAeroSnapEnabled ? new AeroSnap(mAeroSnapDelay) : null;
				DEBUG("onCreate");
				checkIfInitialSnapNeeded();

			}
		});

		// re-initialize the variables when resuming as they may get replaced by another activity.
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				DEBUG("onResumeSTART");
				mActivity = (Activity) param.thisObject;
				mWindowHolder.setWindow(mActivity);
				/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
				if(mWindowHolder.mWindow.isFloating()) return;
				if(!mWindowHolder.isFloating){
					toggleDragger(false);
					return;
				}
				if(!mWindowHolder.isMovable) return;
				//restore layout
				pushLayout();
				//FIX FORCED ORIENTATION ON MOVABLE WINDOWS
				mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
				//Get mOverlayView
				FrameLayout decor_view = (FrameLayout) mActivity.getWindow().peekDecorView().getRootView();
				mOverlayView = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
				decor_view.bringChildToFront(mOverlayView);
				//reconnect XHFWService if needed
				connectService();
				//restore snap layout if needed
				if(mWindowHolder.isSnapped&&mAeroSnap!=null) mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
				else toggleDragger(false);
				//make all windows of the activity to keep layout
				syncLayoutParams();
				DEBUG("onResumeEND");
			}
		});

		// unregister the receiver for syncing window position
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
			@Override
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				DEBUG("onDestroy");
				//if (!isHoloFloat) return;
				if(!mWindowHolder.isFloating) return;
				//syncLayoutParams();
				unregisterLayoutBroadcastReceiver(((Activity) param.thisObject).getWindow());
				//MultiWindowAppManager.appsRegisterListener((Activity) param.thisObject, false);
				// hide the resizing outline
				((Activity)param.thisObject).sendBroadcast(new Intent(Common.SHOW_OUTLINE));
			}
		});
	}
	
	public static void showTitleBar(){
		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
		if(mWindowHolder.mWindow.isFloating()) return;
		if(mOverlayView == null) return;
		boolean is_maximized = 
			mWindowHolder.mWindow.getAttributes().width  == ViewGroup.LayoutParams.MATCH_PARENT &&
			mWindowHolder.mWindow.getAttributes().height == ViewGroup.LayoutParams.MATCH_PARENT;
		if ((mMaximizeChangeTitleBarVisibility && is_maximized) ||
			(mAeroSnapChangeTitleBarVisibility && mWindowHolder.isSnapped)) {
			mOverlayView.setTitleBarVisibility(false);
		}
		else mOverlayView.setTitleBarVisibility(true);
		DEBUG("showTitleBar");
	}
	
	public void setOverlayView(){
		FrameLayout decorView = (FrameLayout) mWindowHolder.mWindow.peekDecorView().getRootView();
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

	private void injectTriangle(final LoadPackageParam lpparam)
			throws Throwable {
		/*TODO: MOVE ALL HOOKS TO ONE METHOD*/
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				DEBUG("onStartSTART");
				if(!mWindowHolder.isMovable) return;
				mActivity = (Activity) param.thisObject;
				mWindowHolder.setWindow(mActivity);
				Window window = (Window) mActivity.getWindow();
				if(mWindowHolder.mWindow.isFloating()) return;//MODAL FIX
				// register the receiver for syncing window position
				registerLayoutBroadcastReceiver(mActivity, window);
				connectService();
				// Add our overlay view
				setOverlayView();
				showTitleBar();
				checkIfInitialSnapNeeded();
				DEBUG("onStartEND");
			}
		});
	}

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
	
	private void checkIfInitialSnapNeeded() {
		if((mAeroSnap==null)||(!mWindowHolder.isSnapped)||(mWindowHolder.SnapGravity==mPreviousForceAeroSnap)) return;
		if(mWindowHolder.SnapGravity==0) {mPreviousForceAeroSnap = 0; return;}
		layout_moved = false;
		DEBUG("checkInitSnap before forceSnap");
		mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
		mPreviousForceAeroSnap = mWindowHolder.SnapGravity;
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

	/* (Start) Layout Position Method Helpers */
	static boolean layout_moved;
	
	public static void pullLayout(){
		mWindowHolder.pullFromWindow();
		DEBUG("pullLayout");
		//saveLayout();
		layout_moved = true;
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

	public static void registerLayoutBroadcastReceiver(final Activity activity,
			final Window window) {
		if (!(mRetainStartPosition || mConstantMovePosition)) return;
		
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
					
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
				}
				
				if (intent.getAction().equals(Common.REFRESH_FLOAT_DOT_POSITION)){
						int[] coordinates = intent.getIntArrayExtra(Common.INTENT_FLOAT_DOT_EXTRA);
						if(coordinates == null) return;
						mFloatDotCoordinates=coordinates;
						if(!mWindowHolder.isSnapped) return;
						refreshScreenSize();
						if(Util.isFlag(mWindowHolder.SnapGravity, Gravity.TOP)){
							mWindowHolder.height = coordinates[1]+1;
							mWindowHolder.y=0;
						}
						if(Util.isFlag(mWindowHolder.SnapGravity, Gravity.BOTTOM)){
							mWindowHolder.y = coordinates[1];
							mWindowHolder.height = mScreenHeight - coordinates[1]+1;
						}
						if(Util.isFlag(mWindowHolder.SnapGravity, Gravity.LEFT)){
							mWindowHolder.width = coordinates[0]+1;
							mWindowHolder.x = 0;
						}
						if(Util.isFlag(mWindowHolder.SnapGravity, Gravity.RIGHT)){
							mWindowHolder.x = coordinates[0];
							mWindowHolder.width = mScreenWidth - coordinates[0]+1;
						}
						mWindowHolder.setWindow(window);
						//pushLayout();
						syncLayoutParams();
						return;
				}
				
				if (intent.getStringExtra(INTENT_APP_PKG).equals(
					window.getContext().getApplicationInfo().packageName)) {
						//get new coordinates
					DEBUG("REFRESH_APP_LAYOUT");
					mWindowHolder.setWindow(window);
					if (layout_moved&&mRetainStartPosition) pushLayout();
					showTitleBar();
				}
				
			}
		};
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_APP_LAYOUT);
		filters.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		filters.addAction(Common.REFRESH_FLOAT_DOT_POSITION);

		//if((br!=null)&&(activity!=null)) XposedHelpers.callMethod(activity, "registerReceiver", br, filters);
		window.getContext().registerReceiver(br, filters);
		setTagInternalForView(window.getDecorView(), Common.LAYOUT_RECEIVER_TAG, br);
	}

	private static void unregisterLayoutBroadcastReceiver(Window window) {
		if (!(mRetainStartPosition || mConstantMovePosition)) return;
		BroadcastReceiver br = null;
		try {
			br = (BroadcastReceiver) window.getDecorView().getTag(Common.LAYOUT_RECEIVER_TAG);
		} catch(Exception e){}
		try{
			if(br!=null) window.getContext().unregisterReceiver(br);
		} catch (Exception e) {}
	}

	public static boolean pullAndSyncLayoutParams() {
		if (!mRetainStartPosition) return false;
		pullLayout();
		syncLayoutParams();
		return true;
	}
	
	public static void syncLayoutParams() {
		if (!mRetainStartPosition) return;
		Intent intent = new Intent(Common.REFRESH_APP_LAYOUT);
		intent.putExtra(INTENT_APP_PKG, mWindowHolder.packageName);
		// set package so this is broadcasted only to our own package
		intent.setPackage(mWindowHolder.packageName);
		mWindowHolder.mActivity.sendBroadcast(intent);
	}
	/* (End) Layout Position Method Helpers */

	private static void changeFocusApp(Activity a) {
		if(XHFWInterfaceLink==null) {mWindowHolder.serviceConnected=false; connectService();}
		try		{
			XHFWInterfaceLink.bringToFront(a.getTaskId());
		}
		catch (RemoteException e)
		{
			XposedBridge.log("changeFocusApp failed");
		}
	}

	private void updateView(Window mWindow, float x, float y) {
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
	
	public static void toggleDragger(boolean show){
		Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
		intent.putExtra(Common.INTENT_FLOAT_DOT_BOOL, show);
		mWindowHolder.mActivity.sendBroadcast(intent);
	}
	
	private static void refreshScreenSize() {
		final WindowManager wm = (WindowManager) MovableWindow.mWindowHolder.mActivity.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);
		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}
}
