package com.zst.xposed.halo.floatingwindow3;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.*;
import android.content.*;
import android.content.pm.*;
import java.util.*;
import android.os.*;
import android.app.*;
import android.view.*;

import static de.robv.android.xposed.XposedHelpers.findClass;

public class SystemHooks
{
	static boolean isMovable = false;
	//static ArrayList<String> mListPackages = new ArrayList<String>();
	static Map<String, Integer> mTasksList = new HashMap<String, Integer>();
	static Map<String, Integer> mNoMovableTasksList = new HashMap<String, Integer>();
	
	public static void hookActivityRecord(Class<?> classActivityRecord) throws Throwable {

		XposedBridge.hookAllConstructors(classActivityRecord, 
			new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					ActivityInfo mActivityInfo = (ActivityInfo) param.args[MainXposed.mCompatibility.ActivityRecord_ActivityInfo];
					if(mActivityInfo==null) return;
					String packageName = mActivityInfo.applicationInfo.packageName;
					isMovable = false;
					if (packageName==null || packageName.startsWith("com.android.systemui")|| packageName.equals("android")) return;
					Intent mIntent = (Intent) param.args[MainXposed.mCompatibility.ActivityRecord_Intent];
					if(mIntent==null) return;
					Object mStackSupervisor = (MainXposed.mCompatibility.ActivityRecord_StackSupervisor==-1)?null : param.args[MainXposed.mCompatibility.ActivityRecord_StackSupervisor];
					if(mStackSupervisor==null)
						try{
							mStackSupervisor=XposedHelpers.getObjectField(param.thisObject, "mStackSupervisor");
						}catch(Throwable t){XposedBridge.log(t);}
						
					if(mNoMovableTasksList.containsKey(packageName)) {
						removeFloatingFlag(mIntent);
						return;
					}
					isMovable = checkInheritFloatingFlag(packageName,
												 (MainXposed.mCompatibility.ActivityRecord_ActivityStack==-1)? 
												 MainXposed.mCompatibility.getActivityRecord_ActivityStack(mStackSupervisor) 
												 : param.args[MainXposed.mCompatibility.ActivityRecord_ActivityStack], mIntent);
					
					isMovable = isMovable || Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
					isMovable = isMovable || mTasksList.containsKey(packageName);
					isMovable = checkBlackWhiteList(isMovable, packageName);
					if(!isMovable) return;
					MovableWindow.DEBUG(packageName + " hookActivityRecord.isMovable:[" + isMovable + "] is multiple tasks:[" + Util.isFlag(mIntent.getFlags(), Intent.FLAG_ACTIVITY_MULTIPLE_TASK) +"]");
					//if(!mTasksList.containsKey(packageName)) mTasksList.put(packageName, 0);
					XposedHelpers.setBooleanField(param.thisObject, "fullscreen", false);
					setIntentFlags(mIntent);
					}
			});//XposedBridge.hookAllConstructors(classActivityRecord, XC_MethodHook);
			
	}
	
	public static void hookAMS(Class<?> AMS) throws Throwable
	{
		XposedBridge.hookAllMethods(AMS, "removeTask", new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(param.args[0] == null) return;
					int taskId=param.args[0];
					Object taskRecord = XposedHelpers.callMethod(param.thisObject, "recentTaskForIdLocked", taskId);
					if(taskRecord==null) {
						XposedBridge.log("removeTask hook failed: wrong taskID:" + taskId);
						return;
					}
					String packageName = (String) XposedHelpers.getObjectField(taskRecord, "affinity");
					if(packageName==null) return;
					XposedBridge.log("ActivityManagerService REMOVETASK affinity: "+packageName);
					
					if(mNoMovableTasksList.containsKey(packageName)){
						int nmTaskNum = mNoMovableTasksList.get(packageName) - 1;
						if(nmTaskNum<1)
							mNoMovableTasksList.remove(packageName);
						else
							mNoMovableTasksList.put(packageName, nmTaskNum);
						return;
					}
					
					if(!mTasksList.containsKey(packageName))return;
					
					int tasksNum = mTasksList.get(packageName)-1;
					if(tasksNum<1){
						mTasksList.remove(packageName);
						//mListPackages.remove(packageName);
						}
					else
						mTasksList.put(packageName, tasksNum);
				}
			});
	}
	
	public static void hookTaskRecord(Class<?> classTaskRecord) throws Throwable {
		XposedBridge.hookAllConstructors(classTaskRecord, 
			new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					MovableWindow.DEBUG("TaskRecord start");
					if(!(param.args[MainXposed.mCompatibility.TaskRecord_Intent] instanceof Intent)) return;
					Intent mIntent = (Intent) param.args[MainXposed.mCompatibility.TaskRecord_Intent];
					String packageName = (String) XposedHelpers.getObjectField(param.thisObject, "affinity");
					if(packageName==null) return;
					isMovable = false;
					if ((packageName.startsWith("com.android.systemui"))||(packageName.equals("android"))) return;
					isMovable = Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW))
						|| mTasksList.containsKey(packageName);
					if(!isMovable) {
						if(mNoMovableTasksList.containsKey(packageName)) 
							mNoMovableTasksList.put(packageName, mTasksList.get(packageName)+1);
						else
							mNoMovableTasksList.put(packageName, 1);
						return;
						}
					//if(!mListPackages.contains(packageName)){
					//	mListPackages.add(packageName);
					//}
					if(mTasksList.containsKey(packageName)) 
						mTasksList.put(packageName, mTasksList.get(packageName)+1);
					else
						mTasksList.put(packageName, 1);
					MovableWindow.DEBUG("TaskRecord for movable " + packageName + " tasks in stack:" + mTasksList.get(packageName) 
						+ " isMovable:[" + isMovable + "] is multiple tasks:[" + Util.isFlag(mIntent.getFlags(), Intent.FLAG_ACTIVITY_MULTIPLE_TASK) +"]");
					}
				});
	}

	private static Intent setIntentFlags(Intent mIntent){
		int flags = mIntent.getFlags();
		flags = flags | MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		flags = flags | Intent.FLAG_ACTIVITY_NO_USER_ACTION;
		flags &= ~Intent.FLAG_ACTIVITY_TASK_ON_HOME;

		if (!MainXposed.mPref.getBoolean(Common.KEY_SHOW_APP_IN_RECENTS, Common.DEFAULT_SHOW_APP_IN_RECENTS)) {
			flags = flags | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
		} else if (MainXposed.mPref.getBoolean(Common.KEY_FORCE_APP_IN_RECENTS, Common.DEFAULT_FORCE_APP_IN_RECENTS)) {
			flags &= ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
		}

		mIntent.setFlags(flags);
		return mIntent;
	}
	
	private static void removeFloatingFlag(Intent mIntent){
		int flags = mIntent.getFlags();
		flags &= ~MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		mIntent.setFlags(flags);
	}
	
	private static void setIntentGravity(Intent mIntent, int sGravity, boolean alternative) throws Throwable{
		if(sGravity==0||mIntent==null) return;
		if(mIntent.getIntExtra(Common.EXTRA_SNAP_SIDE, 0)!=0) return;
		if(alternative) sGravity = altGravity(sGravity);
			
		//XposedHelpers.callMethod(mIntent, "putExtra", sGravity);
		//mIntent.setExtrasClassLoader(mIntent.getClass().getClassLoader());
		try{
			mIntent = mIntent.putExtra(Common.EXTRA_SNAP_SIDE, sGravity);
		}catch(Throwable t){
			Bundle extras = new Bundle();
			extras.putSerializable(Common.EXTRA_SNAP_SIDE,sGravity);
			mIntent.putExtras(extras);
		}
	}

	private static int altGravity(int sGravity)
	{
		sGravity = Compatibility.snapSideToGravity(sGravity);
		switch (sGravity){
			case Gravity.TOP:
				sGravity=Gravity.BOTTOM;
				break;
			case Gravity.BOTTOM:
				sGravity=Gravity.TOP;
				break;
			case Gravity.TOP | Gravity.LEFT:
				sGravity=Gravity.TOP | Gravity.RIGHT;
				break;
			case Gravity.TOP | Gravity.RIGHT:
				sGravity=Gravity.TOP | Gravity.LEFT;
				break;
			case Gravity.BOTTOM | Gravity.LEFT:
				sGravity=Gravity.BOTTOM | Gravity.RIGHT;
				break;
			case Gravity.BOTTOM | Gravity.RIGHT:
				sGravity=Gravity.BOTTOM | Gravity.LEFT;
				break;
			case Gravity.LEFT:
				sGravity=Gravity.RIGHT;
				break;
			case Gravity.RIGHT:
				sGravity=Gravity.LEFT;
				break;
		}
		sGravity = Compatibility.snapGravityToSide(sGravity);
		return sGravity;
	}
	
	private static boolean checkInheritFloatingFlag(String packageName, Object activityStack, final Intent mIntent) throws Throwable {
		if(activityStack==null) return false;
		ArrayList<?> taskHistory = (ArrayList<?>) XposedHelpers.getObjectField(activityStack, MainXposed.mCompatibility.ActivityRecord_TaskHistory);
		if(taskHistory==null || taskHistory.size()==0) return false;
		Object lastRecord = taskHistory.get(taskHistory.size() - 1);
		if(lastRecord==null) return false;
		Intent lastIntent = (Intent) XposedHelpers.getObjectField(lastRecord, "intent");
		int sGravity;
		if(lastIntent==null) return false;
		if((packageName.equals(lastIntent.getPackage()))){
			sGravity = lastIntent.getIntExtra(Common.EXTRA_SNAP_SIDE, 0);
			try
			{
				setIntentGravity(mIntent, sGravity, false);
			}
			catch (Throwable e)
			{
				XposedBridge.log("Unable to set snap gravity to intent " + mIntent.getPackage() + " Snapside=" + sGravity);
			}
			return Util.isFlag(lastIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
			}
		if(MainXposed.mPref.getBoolean(Common.KEY_FORCE_OPEN_APP_ABOVE_HALO, Common.DEFAULT_FORCE_OPEN_APP_ABOVE_HALO)){
			/* alternative gravity option */
			int sGravitySettings = MainXposed.mPref.getInt(Common.KEY_FORCE_OPEN_ALT_GRAVITY, Common.DEFAULT_FORCE_OPEN_ALT_GRAVITY);
			if(sGravitySettings!=0) {
				sGravity = lastIntent.getIntExtra(Common.EXTRA_SNAP_SIDE, 0);
				if(sGravitySettings==2&&sGravity==0)
					sGravity = Compatibility.AeroSnap.SNAP_BOTTOM;
				try
				{
					setIntentGravity(mIntent, sGravity, true);
				}
				catch (Throwable e)
				{}
			} //alternative gravity option
			return Util.isFlag(lastIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
			}
		return false;
	}
	
	private static boolean checkBlackWhiteList(boolean flag, String packageName){
		switch (MainXposed.getBlackWhiteListOption()) {
			case 1: /* Always open apps in halo except blacklisted apps */
				flag=!MainXposed.isBlacklisted(packageName);
				break;
			case 2: /* Never open apps in halo + force whitelisted apps in halo */
				flag = MainXposed.isWhitelisted(packageName);
				break;
			case 3: /* Blacklist all apps & only allow whitelisted apps to be opened in halo */
				if (!MainXposed.isWhitelisted(packageName)) {
					flag = false;
				}
				break;
			default: // no additional options
				if (MainXposed.isWhitelisted(packageName)) {
					flag = true;
				}
				if (MainXposed.isBlacklisted(packageName)) {
					flag = false;
				}
				break;
		}
		return flag;
	}
	
	public static void removeAppStartingWindow(final Class<?> hookClass) throws Throwable {
		XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					if (!isMovable) return;
					//if (!mHasHaloFlag && (MovableWindow.mWindowHolder==null || !MovableWindow.mWindowHolder.isFloating)) return;
					if ("android".equals((String) param.args[1])) return;
					// Change boolean "createIfNeeded" to FALSE
					if (param.args[param.args.length - 1] instanceof Boolean) {
						param.args[param.args.length - 1] = Boolean.FALSE;
						MovableWindow.DEBUG("setAppStartingWindow.isMovable:[" + isMovable + "]");
						// Last param of the arguments
						// It's length has changed in almost all versions of Android.
						// Since it is always the last value, we use this to our advantage.
					}
				}
			});
		}
	
	public static void hookActivityStack(Class<?> hookClass) throws Throwable {
//		final Class<?> classActivityRecord = findClass("com.android.server.am.ActivityRecord",
//													   lpp.classLoader);
		XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() {
				Object previous = null;
				boolean appPauseEnabled;
//				boolean isHalo;
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					// Find the first activity that is not finishing.
					if (!isMovable) return;
					//if (mIsPreviousActivityHome) return;

//					Object nextAR = XposedHelpers.callMethod(param.thisObject, "topRunningActivityLocked",
//															 new Class[] { classActivityRecord }, (Object) null);
//					Intent nextIntent = (Intent) XposedHelpers.getObjectField(nextAR, "intent");
//					// TODO Find better whatsapp workaround.
//					try {
//						isHalo = (!nextIntent.getPackage().equals("com.whatsapp")) &&
//							(nextIntent.getFlags() & Common.FLAG_FLOATING_WINDOW) == Common.FLAG_FLOATING_WINDOW;
//					} catch (NullPointerException e) {
//						// if getPackage returns null
//					}
//					if (!isHalo) return;

					appPauseEnabled = MainXposed.mPref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
					if (appPauseEnabled) return;

					final Object prevActivity = XposedHelpers.getObjectField(param.thisObject, "mResumedActivity");
					previous = prevActivity;
					XposedHelpers.setObjectField(param.thisObject, "mResumedActivity", null);
				}
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					if (!isMovable) return;
//					if (!isHalo) return;
//					if (mIsPreviousActivityHome) return;
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
					if (!isMovable&&!MovableWindow.isMovable) return;
					if (param.args[1] instanceof Intent) return;
					Object activityRecord = param.args[0];
					if(activityRecord==null) return;
					XposedHelpers.setBooleanField(activityRecord, "fullscreen", false);
					MovableWindow.DEBUG("startActivityLocked [" + isMovable + "]");
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
						if (Util.isFlag(launchFlags,Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME)) {
							if (Util.isFlag(launchFlags,MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW))) param.setResult(null);
							// if the app is a floating app, and is a new task on home.
							// then skip this method.
						} else {
							param.setResult(null);
							// This is not a new task on home. Dont allow the method to continue.
							// Since there is no point to run method which checks for the same thing
						}
					}
				});
			}//for SDK_INT 19 only
	}
}
