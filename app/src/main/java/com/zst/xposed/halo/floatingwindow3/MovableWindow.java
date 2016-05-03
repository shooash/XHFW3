package com.zst.xposed.halo.floatingwindow3;

import android.app.*;
import android.content.*;
import android.content.res.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import com.zst.xposed.halo.floatingwindow3.*;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.*;
import android.content.pm.*;
import java.util.*;
import com.zst.xposed.halo.floatingwindow3.floatdot.*;
import android.os.*;
import android.annotation.*;

/**
 * Created by andrey on 21.04.16.
 */

public class MovableWindow
{

    public static void DEBUG(String tag){
//        XposedBridge.log(tag + " Package:[" + (mWindowHolder==null?"null":mWindowHolder.packageName + "] isSnapped: [" + mWindowHolder.isSnapped 
//						 + "] isMaximized: [" + mWindowHolder.isMaximized + "]")
//							+ "] isMovable:[" + isMovable );
//		if(mWindowHolder!=null) 
//			XposedBridge.log("      window:[" + mWindowHolder.width + ":" + mWindowHolder.height
//				+ "] at [" + mWindowHolder.x + ":" + mWindowHolder.y + "]");
//		if(mWindowHolderCached!=null) 
//			XposedBridge.log("      window:[" + mWindowHolderCached.width + ":" + mWindowHolderCached.height
//							 + "] at [" + mWindowHolderCached.x + ":" + mWindowHolderCached.y + "]");
		
    }

    public static boolean isMovable = false;
    public static WindowHolder mWindowHolder=null;
	public static WindowHolder mWindowHolderCached=null;

    static int mScreenHeight;
    static int mScreenWidth;

	/* App ActionBar Moving Values */
	private static Float screenX;
	private static Float screenY;
	private static Float viewX;
	private static Float viewY;
	private static Float leftFromScreen;
	private static Float topFromScreen;
	private static boolean mChangedPreviousRange;
	private static float[] mPreviousRange = new float[2];
	private static int MOVE_MAX_RANGE = Common.DEFAULT_MOVE_MAX_RANGE;
	
	public static MovableOverlayView mOverlayView = null;

    public static boolean mRetainStartPosition;
    public static boolean mConstantMovePosition;
    //public static int mPreviousOrientation;
    public static int mPreviousRotation;
    public static boolean mMaximizeChangeTitleBarVisibility;
    public static boolean mActionBarDraggable;
    public static boolean mMinimizeToStatusbar;

    public static AeroSnap mAeroSnap = null;
    public static boolean mAeroSnapChangeTitleBarVisibility;
    public static boolean mAeroSnapEnabled;
    static int mAeroSnapDelay;
    static boolean mAeroSnapSwipeApp;
    static int mPreviousForceAeroSnap;
	//Service
	static XHFWInterface XHFWInterfaceLink = null;

	//Float dot
	public static int[] mFloatDotCoordinates = new int[2];
	
	static final int ID_NOTIFICATION_RESTORE = 22222222;
	static final String INTENT_APP_PKG = "pkg";
	
	
    public static void hookActivity(final XC_LoadPackage.LoadPackageParam lpparam){
		
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				
                DEBUG("onCreate start ");
                Activity mActivity = (Activity) param.thisObject;
                isMovable = /* isMovable || */
					(Util.isFlag(mActivity.getIntent().getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)));
                if(!isMovable) return;
				if(mWindowHolder==null) initWindow(mActivity);
                else mWindowHolder.setWindow(mActivity);
				/* reconnect XHFWService if needed */
                connectService();
					/* we need to snap from very begining */
                //if((mAeroSnap!=null)&&(mWindowHolder.isSnapped)) mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
					/* add overlay */
                //setOverlayView();
                //showTitleBar();
                DEBUG("onCreate");
            }
        });

        XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                DEBUG("onStartSTART");
                if(!isMovable || mWindowHolder==null) return;
                mWindowHolder.setWindow((Activity) param.thisObject);
                mWindowHolder.syncLayout();
                /*  We don't touch floating dialogs  */
                //if (mActivity.getWindow().isFloating()) return;
				/* reconnect XHFWService if needed */
                //connectService();
                // Add our overlay view
              //  if(!mWindowHolder.mWindow.isFloating()&&!mWindowHolder.mWindow.isActive())
				setOverlayView();
                showTitleBar();
                DEBUG("onStartEND");
            }
        });
		
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					DEBUG("onResumeSTART");
					if(!isMovable || mWindowHolder == null) {
						toggleDragger((Activity) param.thisObject, false);
						return;
						}
					//putOverlayView();
					//showTitleBar();
					//mActivity = (Activity) param.thisObject;
					/*  We don't touch floating dialogs  */
					//if (mActivity.getWindow().isFloating()) return;
					/** update current window **/
					mWindowHolder.setWindow((Activity) param.thisObject);
					putOverlayView();
					showTitleBar();
					/* check if we need to show or hide floatdot */
					//toggleDragger();
					/* FIX FORCED ORIENTATION ON MOVABLE WINDOWS */
					mWindowHolder.mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
					/* reconnect XHFWService if needed */
					//connectService();
					//restore snap layout if needed
					//if(mWindowHolder.isSnapped&&mAeroSnap!=null) mAeroSnap.updateSnap(mWindowHolder.SnapGravity);
					//else /* restore layout */
					/* fix evernote layout */
					if(mWindowHolder.packageName.equals("com.evernote")) 
						mWindowHolder.syncLayoutForce();
					 else
						 pushLayout();
					
					toggleDragger();
					DEBUG("onResumeEND");
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					DEBUG("onDestroy mWindows.size:" + mWindowHolder.mWindows.size());
					if(!isMovable || mWindowHolder==null) return;		
					/* remove from window stack */
					mWindowHolder.mWindows.remove(((Activity)param.thisObject).getWindow());
					if(mWindowHolder.mWindows.size()<1) {
						//mMainXposed.mMovablePackages.remove(mWindowHolder.packageName);
						mWindowHolder = null;
						isMovable=false;
					}
					//else showTitleBar();
					/* disable dragger */
					//toggleDragger(false);
					// hide the resizing outline
					((Activity)param.thisObject).getApplicationContext().sendBroadcast(new Intent(Common.SHOW_OUTLINE));
					//TODO TEST
					//showTitleBar();
					return;
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onConfigurationChanged", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if(!isMovable||mWindowHolder==null) return;
					//mActivity = (Activity) param.thisObject;
					DEBUG("ACTION_CONFIGURATION_CHANGED");
					int curOrientation = Util.getScreenOrientation(mWindowHolder.mActivity.getApplicationContext());
					if(curOrientation!=mWindowHolder.cachedOrientation) {
						mWindowHolder.toggleXY();
						mWindowHolder.syncLayout();
						toggleDragger();
						}
					mWindowHolder.cachedOrientation=curOrientation;
					
					return;
				}
			});
		
			
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(!isMovable||mWindowHolder==null) return;

					Activity a = (Activity) param.thisObject;
					mWindowHolder.setWindow(a);
					MotionEvent event = (MotionEvent) param.args[0];
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							viewX = event.getX();
							viewY = event.getY();
							if (!mChangedPreviousRange) {
								mPreviousRange[0] = event.getRawX();
								mPreviousRange[1] = event.getRawY();
								mChangedPreviousRange = true;
							}
							changeFocusApp(a);
//							if (a.getWindow().getAttributes().gravity != (Gravity.LEFT | Gravity.TOP)) {
//								// Fix First Resize moving into corner
//								screenX = event.getRawX();
//								screenY = event.getRawY();
//								leftFromScreen = (screenX - viewX);
//								topFromScreen = (screenY - viewY);
//								a.getWindow().setGravity(Gravity.LEFT | Gravity.TOP);
//								updateView(a.getWindow(), leftFromScreen, topFromScreen);
//							}
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
									if(moveRangeAboveLimit(event)){
										if(mWindowHolder.isSnapped) unsnap();
										move(leftFromScreen.intValue(), topFromScreen.intValue());
										if (mAeroSnap != null) {
											mAeroSnap.dispatchTouchEvent(event);
										}
										mChangedPreviousRange=false;
										}
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
			
		try
		{
			injectGenerateLayout(lpparam);
		}
		catch (Throwable e)
		{}
    }
	
	private static void injectGenerateLayout(final XC_LoadPackage.LoadPackageParam lpparam)
	throws Throwable {
		Class<?> cls = XposedHelpers.findClass(MainXposed.mCompatibility.Internal_PhoneWindow, lpparam.classLoader);
		if(cls==null) return;
		XposedBridge.hookAllMethods(cls, "generateLayout", new XC_MethodHook() {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					MovableWindow.DEBUG("GenerateLayout");
					if (!isMovable) return;
					Window window = (Window) param.thisObject;
					String name = window.getContext().getPackageName();
					if (name.startsWith("com.android.systemui")||name.equals("android")) return;
					if(window.isFloating()) return; //MODAL fix
					if(mWindowHolder==null) return;
					//mWindowHolder.setWindow(window);
					//TODO add to settings an option to force titlebar to overlay windows 
					//(but that will overlap actionbar)
					//setOverlayView();
					//pushLayout();
					mWindowHolder.pushToWindow(window);
				}
			});
	}
	
	/*
	 * This is to fix "resuming" apps that have not been paused.
	 * Some apps (eg. BoatBrowser) will throw exceptions and we
	 * fix it using this hook.
	 * 
	 * According to the AOSP sources for Instrumentation.java:
	 * 
	 * 		To allow normal system exception process to occur, return false.
	 *		If true is returned, the system will proceed as if the exception
	 *		didn't happen.
	 *
	 * Therefore, to remove the exception, we return true if the resume activity
	 * is in process and false when we are not resuming to let normal system behavior
	 * continue as normal.
	 */
	static boolean mExceptionHook = false;
	public static void fixExceptionWhenResuming(final Class<?> cls) throws Throwable {
		
		XposedBridge.hookAllMethods(cls, "performResumeActivity",
			new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					mExceptionHook = true;
				}
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					mExceptionHook = false;
				}
			});
		XposedBridge.hookAllMethods(android.app.Instrumentation.class, "onException",
			new XC_MethodReplacement() {
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					return mExceptionHook;
				}
			});
	}

    /***********************************************************/
    /********************* INIT ********************************/
    /***********************************************************/

    private static boolean initWindow(Activity mActivity){ //return: continue?

        /*********** this is what we need to do once **********/
		/* load and set prefs */
        loadPrefs();
		/* setup windowholder - to keep and manipulate window layouts */
        mWindowHolder = new WindowHolder(mActivity, MainXposed.mPref);
		/* setup aerosnap class - to manage snap-to-side windows */
        mAeroSnap = mAeroSnapEnabled ? new AeroSnap(mAeroSnapDelay) : null;
		MOVE_MAX_RANGE=Util.realDp(MainXposed.mPref.getInt(Common.KEY_MOVE_MAX_RANGE, Common.DEFAULT_MOVE_MAX_RANGE), mActivity.getApplicationContext());
       	/* FIX focus other windows not working on some apps */
        mWindowHolder.mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		/* Fix Chrome dim */
        if(!mWindowHolder.packageName.startsWith("com.android.chrome"))
            mWindowHolder.mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		refreshScreenSize();
		mFloatDotCoordinates[0]=mScreenWidth/2;
		mFloatDotCoordinates[1]=mScreenHeight/2;
		/* we need to have XHFWService connected so that we have correct floatingdot posotion*/
		registerLayoutBroadcastReceiver();
		connectService();
		/* set initial layout */
		setInitLayout();
		/* connect XHFWService if needed */
        //connectService();
        return true;
    }

	private static void loadPrefs(){
        mActionBarDraggable = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED, Common.DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED);
		 mRetainStartPosition = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION, Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION);
		 mConstantMovePosition = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION, Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION);

		 //mPreviousOrientation = mActivity.getResources().getConfiguration().orientation;
		 mMinimizeToStatusbar = MainXposed.mPref.getBoolean(Common.KEY_MINIMIZE_APP_TO_STATUSBAR, Common.DEFAULT_MINIMIZE_APP_TO_STATUSBAR);

		 mAeroSnapEnabled = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED);
		 mAeroSnapDelay = MainXposed.mPref.getInt(Common.KEY_WINDOW_RESIZING_AERO_SNAP_DELAY, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY);
		 mAeroSnapSwipeApp = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP);
		 mAeroSnapChangeTitleBarVisibility = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE);
		 mMaximizeChangeTitleBarVisibility = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_MAXIMIZE_HIDE, Common.DEFAULT_WINDOW_TITLEBAR_MAXIMIZE_HIDE);
		 
    }
	
    private static void setInitLayout(){
        //if(mWindowHolder.mWindow.isFloating()) return;
        //mWindowHolder.mWindow.setGravity(MainXposed.mPref.getInt(Common.KEY_GRAVITY, Common.DEFAULT_GRAVITY));
        //WindowManager.LayoutParams params = mWindowHolder.mWindow.getAttributes();
        //Util.addPrivateFlagNoMoveAnimationToLayoutParam(params);
        //mWindowHolder.mWindow.setAttributes(params);
        mWindowHolder.mWindow.addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        //mWindowHolder.mWindow.setWindowAnimations(android.R.style.Animation_Dialog);
        mWindowHolder.mWindow.clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        switch(MainXposed.mPref.getInt(Common.KEY_KEYBOARD_MODE, Common.DEFAULT_KEYBOARD_MODE)){
            case 2:
                mWindowHolder.mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
                break;
            case 3:
                mWindowHolder.mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                break;
        }
        switch(Util.getScreenOrientation(mWindowHolder.mActivity)){
            case Configuration.ORIENTATION_LANDSCAPE:
                mWindowHolder.width = (int) (mScreenWidth * MainXposed.mPref.getFloat(Common.KEY_LANDSCAPE_WIDTH, Common.DEFAULT_LANDSCAPE_WIDTH));
                mWindowHolder.height = (int) (mScreenHeight * MainXposed.mPref.getFloat(Common.KEY_LANDSCAPE_HEIGHT, Common.DEFAULT_LANDSCAPE_HEIGHT));
                break;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                mWindowHolder.width = (int) (mScreenWidth * MainXposed.mPref.getFloat(Common.KEY_PORTRAIT_WIDTH, Common.DEFAULT_PORTRAIT_WIDTH));
                mWindowHolder.height = (int) (mScreenHeight * MainXposed.mPref.getFloat(Common.KEY_PORTRAIT_HEIGHT, Common.DEFAULT_PORTRAIT_HEIGHT));
                break;
        }
		mWindowHolder.position((mScreenWidth-mWindowHolder.width)/2, (mScreenHeight-mWindowHolder.height)/2);
		if(mAeroSnap!=null&&mWindowHolder.isSnapped) {
			mWindowHolder.isSnapped=false;
			mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
			}
		else {
			mWindowHolder.syncLayout();
			}
    }
	

	/***********************************************************/
	/**************** Titlebar and corners *********************/
	/***********************************************************/
	
	public static void showTitleBar(){
		DEBUG("showTitleBar");
		if(mOverlayView == null || mWindowHolder==null) return;
		//toggleDragger(mWindowHolder.isSnapped);

		if(mWindowHolder.mWindow.isFloating()) {
			//mOverlayView.setTitleBarVisibility(false);
			return;
			}
		if(mWindowHolder.isSnapped||mWindowHolder.isMaximized) showTitleBar(false);
		else showTitleBar(true);
	}
	
	public static void showTitleBar(boolean show){
		DEBUG("showTitleBar " + show);
		if(show)
			mOverlayView.setTitleBarVisibility(true);
		else
			mOverlayView.setTitleBarVisibility(false);
		}

	public static void setOverlayView(){
		DEBUG("setOverlayView");
		/*  We don't touch floating dialogs  */
		if (mWindowHolder==null || mWindowHolder.mWindow.isFloating()) return;	
		FrameLayout decorView;
		try{
			decorView = (FrameLayout) mWindowHolder.mWindow.peekDecorView().getRootView();
		} catch(Throwable t){
			decorView=null;
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
			mOverlayView = new MovableOverlayView(mWindowHolder.mActivity, MainXposed.sModRes, MainXposed.mPref);
			decorView.addView(mOverlayView, -1, MovableOverlayView.getParams());
			setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG, mOverlayView);
		}
	}

	public static void putOverlayView(){
		/*  We don't touch floating dialogs  */
		if (mWindowHolder==null || mWindowHolder.mWindow.isFloating()) return;	
		FrameLayout decor_view = (FrameLayout) mWindowHolder.mWindow.peekDecorView().getRootView();
		mOverlayView = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
		decor_view.bringChildToFront(mOverlayView);
		//mMainXposed.hookActionBarColor.setTitleBar(mOverlayView);
	}
	
	private static void setTagInternalForView(View view, int key, Object object) {
		Class<?>[] classes = { Integer.class, Object.class };
		XposedHelpers.callMethod(view, "setTagInternal", classes, key, object);
		// view.setTagInternal(key, object);
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
			mWindowHolder.mActivity.getApplicationContext().bindService(intent, XHFWServiceConnection, Service.BIND_AUTO_CREATE);
		}
	}


	final static BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Common.REFRESH_FLOAT_DOT_POSITION)){
				DEBUG( "REFRESH_FLOATDOT_POSITION");
				int[] coordinates = intent.getIntArrayExtra(Common.INTENT_FLOAT_DOT_EXTRA);
				if(coordinates == null) return;
				mFloatDotCoordinates=coordinates;
				if(mAeroSnap==null||mWindowHolder==null||!mWindowHolder.isSnapped) return;
				mAeroSnap.updateSnap(mWindowHolder.SnapGravity);
				//if(mWindowHolder.SnapGravity==0) mWindowHolder.restoreSnap();
				//if(mAeroSnap!=null){
				//	mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
				//}
				//syncLayoutParams();
				//toggleDragger(true);
			}
//			if(intent.getAction().equals(Intent.ACTION_SCREEN_ON)){
//				mWindowHolder.syncLayoutForce();
//				pushLayout();
//				DEBUG("Screen on");
//			}
		}
	};
	
	

	public static boolean registerLayoutBroadcastReceiver() {
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_FLOAT_DOT_POSITION);
//		if(mWindowHolder.packageName.equals("com.evernote"))
//			filters.addAction(Intent.ACTION_SCREEN_ON);
		try{
			mWindowHolder.mActivity.getApplicationContext().registerReceiver(mBroadcastReceiver, filters);
		} catch(Throwable e){
			DEBUG("Check registerLayoutBroadcastReceiver error");
			return false;
		}
		//setTagInternalForView(window.getDecorView(), Common.LAYOUT_RECEIVER_TAG, br);
		return true;
	}
	
	

	private static void unregisterLayoutBroadcastReceiver() {
		if(mWindowHolder==null) return;
		try{
			mWindowHolder.mActivity.getApplicationContext().unregisterReceiver(mBroadcastReceiver);
		} catch(Throwable e){
			XposedBridge.log(mWindowHolder.packageName + " failed to unregister receiver.");
		}
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
		if(mWindowHolder==null) return;
		if(mWindowHolder.isSnapped) toggleDragger(mWindowHolder.mActivity.getApplicationContext(), true);
		else toggleDragger(mWindowHolder.mActivity.getApplicationContext(), false);
	}

	public static void toggleDragger(boolean show){
		if(mWindowHolder==null) return;
		toggleDragger(mWindowHolder.mActivity.getApplicationContext(), show);
	}
	
	public static void toggleDragger(Context ctx, boolean show){
		Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
		intent.putExtra(Common.INTENT_FLOAT_DOT_BOOL, show);
		ctx.sendBroadcast(intent);
	}
	
	public static void getFloatingDotCoordinates(){
		if(mWindowHolder==null) return;
		refreshScreenSize();
		try
		{
			mFloatDotCoordinates[0]=mScreenWidth/2;
			mFloatDotCoordinates[1]=mScreenHeight/2;
			int[] result = XHFWInterfaceLink.getCurrentFloatdotCoordinates();
			if(result==null) return;
			
			mFloatDotCoordinates[0] = result[0];
			mFloatDotCoordinates[1] = result[1];
			DEBUG("getFloatingDotCoordinates [" + mFloatDotCoordinates[0] +":"+mFloatDotCoordinates[1]+"]");
			if(mWindowHolder.isSnapped)
				mAeroSnap.updateSnap(mWindowHolder.SnapGravity);
		//	if(mWindowHolder.isSnapped) toggleDragger(true);
			/*We need to forceSnap once on create because some apps are started snapped with wrong params - so we just fix the layout*/
//			if((!mWindowHolder.isSet)&&(mWindowHolder.isSnapped)&&(mAeroSnap!=null)){
//				mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
//
//				mWindowHolder.isSet=true;
//			}
		} catch (RemoteException e) {}
	}

	/***********************************************************/
	/********************* LAYOUT ******************************/
	/***********************************************************/
	
	
	

    private static void refreshScreenSize() {
        final WindowManager wm = (WindowManager) MovableWindow.mWindowHolder.mActivity.getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        mScreenHeight = metrics.heightPixels;
        mScreenWidth = metrics.widthPixels;
    }
	
	public static void move(int mLeftFromScreen, int mTopFromScreen)
	{
		DEBUG("move");
		mWindowHolder.position(mLeftFromScreen, mTopFromScreen);
		mWindowHolder.syncLayout();
	}
	
	public static void resize(int x, int y, int width, int height){
		DEBUG("resize");
		mWindowHolder.position(x,y);
		resize(width, height);
	}
	
	public static void resize(int width, int height){
		mWindowHolder.size(width, height);
		mWindowHolder.syncLayout();
	}
	
	public static void pushLayout(){
		mWindowHolder.pushToWindow();
	}

	public static void saveLayout(){
		mWindowHolderCached = new WindowHolder(mWindowHolder);
		DEBUG("savedLayout");
	}

	public static void restoreLayout(){
		if(mWindowHolderCached==null) return;
		mWindowHolder.restore(mWindowHolderCached);
		if(mWindowHolder.width == 0) mWindowHolder.width = -1;
		if(mWindowHolder.height == 0) mWindowHolder.height = -1;
		DEBUG("restoredLayout");
	}

	public static void maximize()
	{
		DEBUG("maximize");
		/*mWindowHolder.mWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
				  ViewGroup.LayoutParams.MATCH_PARENT);//*/
		if(!mWindowHolder.isSnapped) saveLayout();
		mWindowHolder.setMaximized();
		mWindowHolder.syncLayout();
		showTitleBar();
	}
	
	public static void snap(SnapWindowHolder mSnapWindowHolder){
		DEBUG("snap");
		if(!mWindowHolder.isMaximized&&!mWindowHolder.isSnapped) saveLayout();
		mWindowHolder.restore(mSnapWindowHolder);
		mWindowHolder.syncLayout();
		showTitleBar();
		toggleDragger(true);
	}
	
	public static void unsnap()
	{
		DEBUG("unsnap");
		if(!mWindowHolder.isSnapped&&!mWindowHolder.isMaximized) return;
		restoreLayout();
		mWindowHolder.syncLayout();
		showTitleBar(true);
		toggleDragger(false);
	}
	
	// check if it is moved out of the snap and not just accidently moved a few px
	private static boolean moveRangeAboveLimit(MotionEvent event) {
		return ((Math.abs(mPreviousRange[0] -  event.getRawX()) > MOVE_MAX_RANGE)||
			(Math.abs(mPreviousRange[1] -  event.getRawY()) > MOVE_MAX_RANGE));
	}
	
	/***********************************************************/
	/*********************** Minimize **************************/
	/***********************************************************/
	// Send the app to the back, and show a notification to restore
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static void minimizeAndShowNotification(final Activity ac) {
		if (!mMinimizeToStatusbar) {
			ac.moveTaskToBack(true);
			return;
		}

		Intent i = new Intent(Common.REMOVE_NOTIFICATION_RESTORE + ac.getPackageName());
		ApplicationInfo app_info = ac.getApplication().getApplicationInfo();
		PendingIntent intent = PendingIntent.getBroadcast(ac, 0, i,
														  PendingIntent.FLAG_UPDATE_CURRENT);
		String title = String.format(MainXposed.sModRes.getString(R.string.dnm_minimize_notif_title),
									 app_info.loadLabel(ac.getPackageManager()));

		Notification.Builder nb = new Notification.Builder(ac)
			.setContentTitle(title)
			.setContentText(MainXposed.sModRes.getString(R.string.dnm_minimize_notif_summary))
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
	
}
