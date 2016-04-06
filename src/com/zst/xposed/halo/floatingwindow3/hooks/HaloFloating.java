package com.zst.xposed.halo.floatingwindow3.hooks;

import static de.robv.android.xposed.XposedHelpers.findClass;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.view.Window;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.helpers.AeroSnap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XCallback;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import com.zst.xposed.halo.floatingwindow3.helpers.*;
import com.zst.xposed.halo.floatingwindow3.helpers.Compatibility;
import de.robv.android.xposed.*;
import android.util.*;
import android.view.*;
import android.content.*;

public class HaloFloating {
	MainXposed mMain;
	XSharedPreferences mPref;
	
	boolean mIsPreviousActivityHome;
	boolean mHasHaloFlag;
	 //TODO move to new class
	boolean isHoloFloat = false;
	
	public HaloFloating(MainXposed main, LoadPackageParam lpparam, XSharedPreferences pref) throws Throwable {
		mMain = main;
		mPref = pref;
		mPref.reload();
		
		if (lpparam.packageName.equals("android")) {
			hookActivityRecord(lpparam);
			removeAppStartingWindow(lpparam);
			kitkatMoveHomeStackHook(lpparam);
			try {
				injectActivityStack(lpparam);
			} catch (Throwable e) {
				XposedBridge.log(Common.LOG_TAG + "(ActivityStack)");
				XposedBridge.log(e);
			}
		}
		
		initHooks(lpparam);
	}
	
	private void initHooks(LoadPackageParam l) {
		/*********************************************/
		try {
			inject_Activity();
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(inject_Activity)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			injectPerformStop();
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(injectPerformStop)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			injectGenerateLayout(l);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(injectGenerateLayout)");
			XposedBridge.log(e);
		}
		/*********************************************/
		try {
			fixExceptionWhenResuming(l);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(fixExceptionWhenResuming)");
			XposedBridge.log(e);
		}
		/*********************************************/
	}
	
	private void hookActivityRecord(final LoadPackageParam lpparam)throws Throwable {
		/*final Class<?> hookRecentsPanelViewClass = findClass("com.android.systemui.recent.RecentsPanelView",
											 lpparam.classLoader);
		XposedBridge.hookAllMethods(hookRecentsPanelViewClass, "handleOnClick", new XC_MethodHook() {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					final View thiz = (View) param.thisObject;
					try {
						XposedHelpers.setObjectField(thiz, "hide", false);
						} catch (Throwable e){};
				}
			});*/
		
		
		
		Class<?> classActivityRecord = findClass("com.android.server.am.ActivityRecord",
				lpparam.classLoader);
		XposedBridge.hookAllConstructors(classActivityRecord,
				new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				mPref.reload();
				
				// Reset the values to accept the next app's
				mHasHaloFlag = false;
				Intent intent = null;
				Object activity_stack = null;
				ActivityInfo activity_info = null;
				boolean isCurrentActivityHome = false;
				
				if (Build.VERSION.SDK_INT >= 19) { // Android 4.4 onwards
					intent = (Intent) param.args[4];
					activity_info = (ActivityInfo) param.args[6];
					try {
						Object stackSupervisor = param.args[12]; // mStackSupervisor
						activity_stack = XposedHelpers.callMethod(stackSupervisor, "getFocusedStack");
					} catch (Exception e) {
						activity_stack = XposedHelpers.getObjectField(param.args[12], "mFocusedStack");
					}
					isCurrentActivityHome = (Boolean) XposedHelpers.callMethod(activity_stack, "isHomeStack");
					// Check if the previous activity is home
				} else if (Build.VERSION.SDK_INT == 18) { // Android 4.3
					intent = (Intent) param.args[5];
					activity_stack = param.args[1];
					activity_info = (ActivityInfo) param.args[7];
				} else if (Build.VERSION.SDK_INT <= 17) { // Android 4.2 & below
					intent = (Intent) param.args[4];
					activity_stack = param.args[1];
					activity_info = (ActivityInfo) param.args[6];
				}
				String packageName = activity_info.applicationInfo.packageName;
				if (packageName.startsWith("com.android.systemui")) return;
				if (packageName.equals("android")) return;
				if (intent == null) return;
				mHasHaloFlag = Util.isFlag(intent.getFlags(), mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
				
				switch (mMain.getBlackWhiteListOption()) {
					case 1: /* Always open apps in halo except blacklisted apps */
						mHasHaloFlag=!mMain.isBlacklisted(packageName);
						break;
					case 2: /* Never open apps in halo + force whitelisted apps in halo */
						mHasHaloFlag = mMain.isWhitelisted(packageName);
						break;
					case 3: /* Blacklist all apps & only allow whitelisted apps to be opened in halo */
						if (!mMain.isWhitelisted(packageName)) {
							mHasHaloFlag = false;
						}
						break;
					default: // no additional options
						if (mMain.isWhitelisted(packageName)) {
							mHasHaloFlag = true;
						}
						if (mMain.isBlacklisted(packageName)) {
							mHasHaloFlag = false;
						}
						break;
				}
				
				if(MovableWindow.mWindowHolder!=null) mHasHaloFlag=true;
				
				MovableWindow.DEBUG("HaloFloating actRecord " + packageName + " [" + mHasHaloFlag + "]");
				
				/* check if we should inherit the floating flag */
				boolean forceTaskHalo = mPref.getBoolean(Common.KEY_FORCE_OPEN_APP_ABOVE_HALO, Common.DEFAULT_FORCE_OPEN_APP_ABOVE_HALO);
				if(!mHasHaloFlag){
					ArrayList<?> taskHistoryList = null;
					if (Build.VERSION.SDK_INT >= 19) { // KK++
						taskHistoryList = (ArrayList<?>) /* ArrayList<TaskRecord> */
							XposedHelpers.getObjectField(activity_stack, "mTaskHistory");
					} else { // ICS & JB
						taskHistoryList = (ArrayList<?>) /* ArrayList<ActivityRecord> */
							XposedHelpers.getObjectField(activity_stack, "mHistory");
					}
					if(taskHistoryList!=null && taskHistoryList.size()>0){
						Object pvRecord = taskHistoryList.get(taskHistoryList.size() - 1);
						Intent pvIntent = (Intent) XposedHelpers.getObjectField(pvRecord, "intent");

						if((forceTaskHalo || packageName.equals(pvIntent.getPackage())) && !mIsPreviousActivityHome) 
							mHasHaloFlag =  Util.isFlag(pvIntent.getFlags(), mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
					}
				}

				if(mHasHaloFlag) intent.addFlags(mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
				else intent.setFlags(intent.getFlags() & ~mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
				if(mHasHaloFlag) 
					XposedHelpers.setBooleanField(param.thisObject, "fullscreen", false);

				mIsPreviousActivityHome = isCurrentActivityHome;
				
			}
		});
	}
	
	private void kitkatMoveHomeStackHook(final LoadPackageParam lpp) throws Throwable {
		if (Build.VERSION.SDK_INT < 19) return;
		final Class<?> hookClass = findClass("com.android.server.am.ActivityStackSupervisor", lpp.classLoader);
		XposedBridge.hookAllMethods(hookClass, "moveHomeStack", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				mIsPreviousActivityHome = (Boolean) param.args[0];
				// set out value so we can check later if home was before the next activity.
				// we do this since starting from kitkat, home and normal apps are in different
				// activity stacks and there is no way to see if the previous app was home.

				// param.args[0] is true when home is moved to the front, false if to the back
				// we set it accordingly since we only want it set to true if moved to the front.
			}
		});
	}
	
	/* 
	 * It changes the "mResumedActivity" object to null.
	 * There is a check in "resumeTopActivityLocked" that if "mResumedActivity"
	 * is not null, then pause the app. We are working around it like this.
	 */
	private  void injectActivityStack(final LoadPackageParam lpp) throws Throwable {
		final Class<?> classActivityRecord = findClass("com.android.server.am.ActivityRecord",
				lpp.classLoader);
		final Class<?> hookClass = findClass("com.android.server.am.ActivityStack", lpp.classLoader);
		
		XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() {
			Object previous = null;
			boolean appPauseEnabled;
			boolean isHalo;
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				// Find the first activity that is not finishing.
				if (!mHasHaloFlag) return;
				if (mIsPreviousActivityHome) return;
				
				Object nextAR = XposedHelpers.callMethod(param.thisObject, "topRunningActivityLocked",
						new Class[] { classActivityRecord }, (Object) null);
				Intent nextIntent = (Intent) XposedHelpers.getObjectField(nextAR, "intent");
				// TODO Find better whatsapp workaround.
				try {
					isHalo = (!nextIntent.getPackage().equals("com.whatsapp")) &&
							(nextIntent.getFlags() & mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)) == mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW); //LUCINIAMOD
				} catch (NullPointerException e) {
					// if getPackage returns null
				}
				if (!isHalo) return;
				
				mPref.reload();
				appPauseEnabled = mPref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
				if (appPauseEnabled) return;
				
				final Object prevActivity = XposedHelpers.getObjectField(param.thisObject, "mResumedActivity");
				previous = prevActivity;
				XposedHelpers.setObjectField(param.thisObject, "mResumedActivity", null);
			}
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!mHasHaloFlag) return;
				if (!isHalo) return;
				if (mIsPreviousActivityHome) return;
				if (appPauseEnabled) return;
				if (previous != null) {
					XposedHelpers.setObjectField(param.thisObject, "mResumedActivity", previous);
					previous = null;
				}
			}
		});
		
		/* This is a Kitkat work-around to make sure the background is transparent */
		XposedBridge.hookAllMethods(hookClass, "startActivityLocked", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				MovableWindow.DEBUG("HaloFloating startActivityLocked [" + mHasHaloFlag + "]");
				if (!mHasHaloFlag) return;
				if (param.args[1] instanceof Intent) return;
				Object activityRecord = param.args[0];
				XposedHelpers.setBooleanField(activityRecord, "fullscreen", false);
			}
		});
		
		if (Build.VERSION.SDK_INT < 19) {
			/*
			 * Prevents the App from bringing the home to the front.
			 * Doesn't exists on Kitkat so it is not needed
			 */
			XposedBridge.hookAllMethods(hookClass, "moveHomeToFrontFromLaunchLocked", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					int launchFlags = (Integer) param.args[0];
					if ((launchFlags & (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME))
							== (Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME)) {
						boolean floating = (launchFlags & mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)) == mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW); //TLUCINIAMOD
						if (floating) param.setResult(null);
						// if the app is a floating app, and is a new task on home.
						// then skip this method.
					} else {
						param.setResult(null);
						// This is not a new task on home. Dont allow the method to continue.
						// Since there is no point to run method which checks for the same thing
					}
				}
			});
		}
	}
	
	/*
	 * Removes the app starting placeholder screen before the app contents is shown.
	 * Does this by making 'createIfNeeded' to false
	 */
	private void removeAppStartingWindow(final LoadPackageParam lpp) throws Throwable {
		Class<?> hookClass = findClass("com.android.server.wm.WindowManagerService", lpp.classLoader);
		XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				
				if (!mHasHaloFlag) return;
				MovableWindow.DEBUG("HaloFloating setAppStartingWindow");
				//if (!mHasHaloFlag && (MovableWindow.mWindowHolder==null || !MovableWindow.mWindowHolder.isFloating)) return;
				if ("android".equals((String) param.args[1])) return;
				// Change boolean "createIfNeeded" to FALSE
				if (param.args[param.args.length - 1] instanceof Boolean) {
					param.args[param.args.length - 1] = Boolean.FALSE;
					// Last param of the arguments
					// It's length has changed in almost all versions of Android.
					// Since it is always the last value, we use this to our advantage.
				}
			}
		});
	}
	
	/*
	 * If the window is not movable (normal halo window), we scale the window every onResume.
	 * onResume is called after every rotation so we do not need to bother with it. 
	 * 
	 * If the window is movable, then scale only on every onStart.
	 * 
	 * This is done onStart because Samsung's multiwindow codes run between onCreate & onStart
	 * and the codes undo my layout scaling.
	 */
	private  void inject_Activity() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
			protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
				Activity thiz = (Activity) param.thisObject;
				Intent intent = thiz.getIntent();
				isHoloFloat = (intent.getFlags() & mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW))
						== mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);//LUCINIAMOD
			}
		});
	}
	
	/*
	 * This is the default Halo window behavior by Paranoid Android to close windows
	 * after the screen is turned off. These are their comments from the sources:
	 * 
	 *  	Floating Window activities should be kept volatile to prevent
	 *  	new activities taking up front in a minimized space. Every
	 *  	stop call, for instance when pressing home, will terminate
	 *  	the activity. If the activity is already finishing we might
	 *  	just as well let it go.
	 *  
	 *  I added the option to allow the user to disable it.
	 */
	@SuppressLint("NewApi")
	private  void injectPerformStop() throws Throwable {
		XposedBridge.hookAllMethods(Activity.class, "performStop", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {		
				Activity thiz = (Activity) param.thisObject;
				mPref.reload();
				if (mPref.getBoolean(Common.KEY_DISABLE_AUTO_CLOSE, Common.DEFAULT_DISABLE_AUTO_CLOSE)) return;
				if (!thiz.isChangingConfigurations() && (thiz.getWindow() != null) && isHoloFloat
						&& !thiz.isFinishing()) {
					thiz.finishAffinity();
				}
				
			}
		});
	}
	
	private void injectGenerateLayout(final LoadPackageParam lpp)
			throws Throwable {
				
	Class<?> cls = findClass("com.android.internal.policy.impl.PhoneWindow", lpp.classLoader);
		XposedBridge.hookAllMethods(cls, "generateLayout", new XC_MethodHook() {
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				if (!isHoloFloat) return;
				Window window = (Window) param.thisObject;
				String name = window.getContext().getPackageName();
				if (name.startsWith("com.android.systemui")) return;
				if (name.equals("android")) return;
				//if(window.isFloating()) return; //MODAL fix
				if(MovableWindow.mWindowHolder==null) return;
				MovableWindow.mWindowHolder.setWindow(window);
				MovableWindow.pushLayout();
				//MovableWindow.connectService();
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
	boolean mExceptionHook = false;
	private void fixExceptionWhenResuming(final LoadPackageParam lpp) throws Throwable {
		Class<?> cls = findClass("android.app.ActivityThread", lpp.classLoader);
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
	
}
