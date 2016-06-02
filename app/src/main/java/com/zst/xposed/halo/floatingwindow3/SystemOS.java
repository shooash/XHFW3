package com.zst.xposed.halo.floatingwindow3;

import android.content.*;
import android.os.*;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.*;
import de.robv.android.xposed.*;
import java.util.*;
import de.robv.android.xposed.callbacks.*;

public class SystemOS
{
	/*       FIRST MENTIONS:           */
	/* init ActivityRecord -> init TaskRecord -> startActivityLocked*/
	/* restore: ActivityRecord */
	/* create new Activity: init ActivityRecord -> startActivityLocked */
	
	public static void hookActivityRecord(Class<?> classActivityRecord) throws Throwable {

		XposedBridge.hookAllConstructors(classActivityRecord, 
			new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(param.thisObject, "intent");
					boolean movable;
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "packageName");
					if(packageName==null || mIntent == null)
						return;
					if (packageName.startsWith("com.android.systemui")|| packageName.equals("android")) return;
					
					if(mIntent.hasCategory("restarted"))
						movable = forceUpdatePackageData(packageName, mIntent);
					else if(MWRegister.isRegistered(packageName)){
						knownPackageCallBack(packageName);
						movable = MWRegister.isMovable(packageName);
					}
					/* it's a new package, try to get it's credentials and register */
					else {
						movable = registerPackage(packageName, mIntent, 
										MainXposed.mCompatibility.getActivityRecord_ActivityStack(Util.getFailsafeObjectFromObject(param.thisObject, "mStackSupervisor")));
					}
					if(!movable)
						return;
					XposedHelpers.setBooleanField(param.thisObject, "fullscreen", !movable);
					setFloatingFlag(mIntent, movable);
					setIntentFlags(mIntent);
					MovableWindow.DEBUG("ActivityRecord " + packageName + (movable? " is movable":"is fullscreen"));
				}
			});//XposedBridge.hookAllConstructors(classActivityRecord, XC_MethodHook);
	}
	
	public static void hookTaskRecord(Class<?> classTaskRecord) throws Throwable {
		XposedBridge.hookAllConstructors(classTaskRecord, 
			new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "affinity");
					boolean movable = false;
					Intent mIntent = null;
					if(packageName == null)
						return;
					mIntent = (Intent) Util.getFailsafeObjectFromObject(param.thisObject, "intent");
					MovableWindow.DEBUG("TaskRecord package: " + packageName);
//					if(!MWRegister.isRegistered(packageName)){
//						//Task is created from non activity context, eg as Evernote does
//						movable = registerPackage(packageName, mIntent, Util.getFailsafeObjectFromObject(param.thisObject, "stack"));
//					}
					Integer taskID = Util.getFailsafeIntFromObject(param.thisObject, "taskId");
					if(taskID==null)
						return;
					MWRegister.addTask(packageName, taskID);
					if(movable){
						setFloatingFlag(mIntent, movable);
						setIntentFlags(mIntent);
					}
					MovableWindow.DEBUG("TaskRecord " + packageName + (movable? " is movable":" is fullscreen"));
				}
			});

		XposedBridge.hookAllMethods(classTaskRecord, "removedFromRecents", 
			new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "affinity");
					if(packageName == null)
						return;
					MovableWindow.DEBUG("TaskRecord package: " + packageName);
					if(!MWRegister.isRegistered(packageName))
						return;
					Integer taskID = Util.getFailsafeIntFromObject(param.thisObject, "taskId");
					if(taskID==null)
						return;
					/* TODO: do we really need to forget packages? */
					if(MWRegister.removeTask(packageName, taskID))
						return; // there're still some tasks left
					//MWRegister.removePackage(packageName);
					/* tell FloatDot Launcher the package is gone */
					Object mActivityStack = Util.getFailsafeObjectFromObject(param.thisObject, "stack");
					if(mActivityStack==null)
						return;
					Object mWindowManagerService = Util.getFailsafeObjectFromObject(mActivityStack, "mWindowManager");
					if(mWindowManagerService==null)
						return;
					Context mContext = (Context) Util.getFailsafeObjectFromObject(mWindowManagerService, "mContext");
					if(mContext==null)
						return;
					MovableWindow.sendRemovedPackageInfo(packageName, mContext, true);
				}
			});
	}
	
	
	/* ADDITIONAL HOOKS */
	public static void removeAppStartingWindow(final Class<?> hookClass) throws Throwable {
		XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {			
					if (!MWRegister.isMovable((String) param.args[1])) return;
					// Change boolean "createIfNeeded" to FALSE
					if (param.args[param.args.length - 1] instanceof Boolean) {
						param.args[param.args.length - 1] = Boolean.FALSE;
						MovableWindow.DEBUG("setAppStartingWindow end");
						// Last param of the arguments
						// It's length has changed in almost all versions of Android.
						// Since it is always the last value, we use this to our advantage.
					}
				}
			});
	}
	
	public static void hookActivityStack(Class<?> hookClass) throws Throwable {
		XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() {
				Object previous = null;
				boolean isMovable;
				boolean appPauseEnabled;
//				boolean isHalo;
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					// Find the first activity that is not finishing.
					final Object prevActivity = XposedHelpers.getObjectField(param.thisObject, "mResumedActivity");
					String packageName = Util.getFailsafeStringFromObject(null, previous, "packageName");
					isMovable = MWRegister.isMovable(packageName);
					if(!isMovable)
						return;
					previous = prevActivity;
					
					appPauseEnabled = MainXposed.mPref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
					if (appPauseEnabled) 
						return;
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
					MovableWindow.DEBUG("startActivityLocked ");
//					if(!isMovable)
//						throw new Exception("test");
					Object activityRecord = param.args[0];
					String packageName = Util.getFailsafeStringFromObject(null, activityRecord, "packageName");
					if(!MWRegister.isRegistered(packageName)){
						//Activity not created via activity stack and record
//						final Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(activityRecord, "intent");
//						boolean movable;
//						if(mIntent.hasCategory("restarted"))
//							movable = forceUpdatePackageData(packageName, mIntent);
//						else
//							movable = registerPackage(packageName, mIntent, param.thisObject);
//						setFloatingFlag(mIntent, movable);
//						setIntentFlags(mIntent);
					}
					MovableWindow.DEBUG("startActivityLocked for package " + packageName + " which is " + (MWRegister.isMovable(packageName)?"movable":"fullscreen"));
//					XposedHelpers.setBooleanField(activityRecord, "fullscreen", !MWRegister.isMovable(packageName));
				}
			});
			
	}
	/* REGISTER ETC */
	
	/* welcome newcomers*/
	 private static boolean registerPackage(final String packageName, final Intent mIntent, final Object mActivityStack){
		 boolean movable = hasFloatingFlag(mIntent)
		 	|| checkInheritFloatingFlag(packageName, mActivityStack, mIntent);
		 movable = checkBlackWhiteList(movable, packageName);
		 MWRegister.addPackage(packageName, movable);
		 
		 return movable;
	 }
	 
	 /* not the first launch */
	 private static void knownPackageCallBack(final String packageName){
		 
	 }
	 
	 /* launched by FloatLaunchers to make movable or fullscreen */
	private static boolean forceUpdatePackageData(final String packageName, final Intent mIntent){
		boolean movable = MWRegister.forceAddPackageAutoAssign(packageName, mIntent);
		return movable;
	}
	
	 
	/*****HELPERS*****/
	private static Intent setIntentFlags(Intent mIntent){
		int flags = mIntent.getFlags();
		flags = flags | Intent.FLAG_ACTIVITY_NO_USER_ACTION;
		flags &= ~Intent.FLAG_ACTIVITY_TASK_ON_HOME;

		if (!MainXposed.mPref.getBoolean(Common.KEY_SHOW_APP_IN_RECENTS, Common.DEFAULT_SHOW_APP_IN_RECENTS)) {
			flags = flags | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
		} else if (MainXposed.mPref.getBoolean(Common.KEY_FORCE_APP_IN_RECENTS, Common.DEFAULT_FORCE_APP_IN_RECENTS)) {
			flags &= ~Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
		}

		mIntent.setFlags(flags);
		mIntent.removeCategory("restarted");
		return mIntent;
	}
	
	private static boolean hasFloatingFlag(Intent mIntent){
		if(mIntent == null)
			return false;
		return Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
	}

	private static void removeFloatingFlag(Intent mIntent){
		int flags = mIntent.getFlags();
		flags &= ~MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		mIntent.setFlags(flags);
	}
	
	private static void addFloatingFlag(Intent mIntent){
		int flags = mIntent.getFlags();
		flags &= MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		mIntent.setFlags(flags);
	}
	
	private static void setFloatingFlag(Intent mIntent, boolean movable){
		if(movable)
			addFloatingFlag(mIntent);
		else
			removeFloatingFlag(mIntent);
	}

	private static void setIntentGravity(Intent mIntent, int sGravity, boolean alternative) throws Throwable{
		if(sGravity==0||mIntent==null) return;
		if(mIntent.getIntExtra(Common.EXTRA_SNAP_SIDE, 0)!=0) return;
		if(alternative) sGravity = altGravity(sGravity);
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

	private static boolean checkInheritFloatingFlag(final String packageName, final Object activityStack, final Intent mIntent) {
		//if(activityStack==null) return false;
		ArrayList<?> taskHistory = (ArrayList<?>) Util.getFailsafeObjectFromObject(activityStack, MainXposed.mCompatibility.ActivityRecord_TaskHistory);
		if(taskHistory==null || taskHistory.size()==0)
			return false;
		Object lastRecord = taskHistory.get(taskHistory.size() - 1);
		if(lastRecord==null) return false;
		Intent lastIntent = (Intent) Util.getFailsafeObjectFromObject(lastRecord, "intent");
		if(lastIntent==null)
			return false;
		int sGravity;
//		if((packageName.equals(lastIntent.getPackage()))){
//			sGravity = lastIntent.getIntExtra(Common.EXTRA_SNAP_SIDE, 0);
//			try
//			{
//				setIntentGravity(mIntent, sGravity, false);
//			}
//			catch (Throwable e)
//			{
//				XposedBridge.log("Unable to set snap gravity to intent " + mIntent.getPackage() + " Snapside=" + sGravity);
//			}
//			return Util.isFlag(lastIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
//		}
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
			return hasFloatingFlag(lastIntent); //Util.isFlag(lastIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
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
}
