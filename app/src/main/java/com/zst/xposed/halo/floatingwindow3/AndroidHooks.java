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
import com.zst.xposed.halo.floatingwindow3.debug.*;

public class AndroidHooks
{
	//static boolean isMovable = false;
//	static ArrayList<String> mListMovablePackages = new ArrayList<String>();
//	static Map<String, ArrayList<Integer>> mPackagesTasksList = new HashMap<String,ArrayList<Integer>>();

	/*****HOOKS******/

	public static void hookActivityRecord(Class<?> classActivityRecord) throws Throwable {

		XposedBridge.hookAllConstructors(classActivityRecord, 
			new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					boolean isMovable = false;
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "packageName");
					if(packageName==null)
						return;
					Debugger.DEBUG("new ActivityRecord", packageName);
					if (packageName.startsWith("com.android.systemui")|| packageName.equals("android")) return;
					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(param.thisObject, "intent");
					if(mIntent==null)
						return;
					if(mIntent.hasCategory("restarted")){
						MWRegister.removePackage(packageName);
						mIntent.removeCategory("restarted");
//						removePackage(packageName);
					}
					/* stop if need to force non movable */
					if(MWRegister.isRegistered(packageName)&&!MWRegister.isMovable(packageName)){
						removeFloatingFlag(mIntent);
						return;
					}
//					if(mPackagesTasksList.containsKey(packageName) && !isPackageMovable(packageName)){
//						removeFloatingFlag(mIntent);
//						return;
//					}

//					Object mStackSupervisor = new Object();
//					Object mActivityStack = new Object();
//					if(MainXposed.mCompatibility.ActivityRecord_ActivityStack!=-1)
//						mActivityStack=param.args[MainXposed.mCompatibility.ActivityRecord_ActivityStack];
//					else if (MainXposed.mCompatibility.ActivityRecord_StackSupervisor!=-1){
//						mStackSupervisor = param.args[MainXposed.mCompatibility.ActivityRecord_StackSupervisor];
//						mActivityStack = MainXposed.mCompatibility.getActivityRecord_ActivityStack(mStackSupervisor);
//						}
//					else if(Util.getFailsafeObjectFromObject(mStackSupervisor, param.thisObject, "mStackSupervisor"))

					Object mActivityStack = MainXposed.mCompatibility.getActivityRecord_ActivityStack(Util.getFailsafeObjectFromObject(param.thisObject, "mStackSupervisor"));

					isMovable = checkInheritFloatingFlag(packageName, mActivityStack, mIntent);

					isMovable = isMovable || Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
//					isMovable = isMovable || isPackageMovable(packageName);
					isMovable = checkBlackWhiteList(isMovable, packageName);
					MWRegister.addPackage(packageName, isMovable);
					if(!isMovable) {
						removeFloatingFlag(mIntent);
						return;
					}
					
//					addMovablePackage(packageName);
					XposedHelpers.setBooleanField(param.thisObject, "fullscreen", false);
					setIntentFlags(mIntent);
				}
			});//XposedBridge.hookAllConstructors(classActivityRecord, XC_MethodHook);

		XposedBridge.hookAllMethods(classActivityRecord, "takeFromHistory", new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					
					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(param.thisObject, "intent");
					if(mIntent == null)
						return;
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "packageName");
					Debugger.DEBUG("takeFromHistory", packageName);
//					if(packageName!=null && mIntent.hasCategory("restarted")){
//						removePackage(packageName);
//						//mIntent.removeCategory("restarted");
//					}
					if(mIntent.hasCategory("restarted")){
						MWRegister.removePackage(packageName);
						mIntent.removeCategory("restarted");
//						removePackage(packageName);
						MWRegister.addPackage(packageName, Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)));
					}
					if(!MWRegister.isMovable(packageName))
						removeFloatingFlag(mIntent);
					else
						setIntentFlags(mIntent);
					//isMovable = Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
				}
			});
	}

	public static void hookTaskRecord(Class<?> classTaskRecord) throws Throwable {
		XposedBridge.hookAllConstructors(classTaskRecord, 
			new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "affinity");
					if(packageName == null)
						return;
					Debugger.DEBUG("new TaskRecord", packageName);
//					isMovable = false;
					
					if ((packageName.startsWith("com.android.systemui"))||(packageName.equals("android"))) return;
//					if(param.args==null || param.args.length<MainXposed.mCompatibility.TaskRecord_Intent+1 || param.args[MainXposed.mCompatibility.TaskRecord_Intent]==null || !(param.args[MainXposed.mCompatibility.TaskRecord_Intent] instanceof Intent)) return;
					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(param.thisObject, "intent");
					//(Intent) param.args[MainXposed.mCompatibility.TaskRecord_Intent];
					if(mIntent==null)
						return;
//					isMovable = Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
//					if(mIntent.hasCategory("restarted")){
//						removePackage(packageName);
//						//mIntent.removeCategory("restarted");
//					}
//					isMovable = isMovable || isPackageMovable(packageName);
					Integer taskID = Util.getFailsafeIntFromObject(param.thisObject, "taskId");
					if(taskID==null)
						return;
					MWRegister.addTask(packageName, taskID);
					if(MWRegister.isMovable(packageName)){
						setIntentFlags(mIntent);
					}
					else
						removeFloatingFlag(mIntent);
					//addTask(packageName, taskID.intValue(), isMovable);
				}
			});

		XposedBridge.hookAllMethods(classTaskRecord, "removedFromRecents", 
			new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					String packageName = Util.getFailsafeStringFromObject(null, param.thisObject, "affinity");
					if(packageName == null)
						return;
					Debugger.DEBUG("removedFromRecents", packageName);
//					isMovable = false;
//					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(param.thisObject, "intent");
					//(Intent) param.args[MainXposed.mCompatibility.TaskRecord_Intent];

//					if(mIntent!=null && mIntent.hasCategory("restarted")){
//						removePackage(packageName);
//						mIntent.removeCategory("restarted");
//					}
					if ((packageName.startsWith("com.android.systemui"))||(packageName.equals("android"))) return;
					Integer taskID = Util.getFailsafeIntFromObject(param.thisObject, "taskId");
					if(taskID==null)
						return;
					if(MWRegister.removeTask(packageName, taskID))
						return;
						
					//removeTask(packageName, taskID.intValue());
					Object mActivityStack = Util.getFailsafeObjectFromObject(param.thisObject, "stack");
					if(mActivityStack==null)
						return;
					Object mWindowManagerService = Util.getFailsafeObjectFromObject(mActivityStack, "mWindowManager");
					if(mWindowManagerService==null)
						return;
					Context mContext = (Context) Util.getFailsafeObjectFromObject(mWindowManagerService, "mContext");
					if(mContext==null)
						return;
					InterActivity.sendRemovedPackageInfo(packageName, mContext, true);
				}
			});
	}

//	public static void hookRecents(Class<?> classRecentsView) throws Throwable{
//		XposedBridge.hookAllMethods(classRecentsView, "onTaskViewClicked", new XC_MethodHook(XCallback.PRIORITY_LOWEST) {
//				@Override
//				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//					//XposedBridge.log("XHFW RECENTS ITEM CLICKED");
//					try{
//						//XposedHelpers.callMethod(param.thisObject, "dismissRecentsToFocusedTaskOrHome", true);
//						//XposedHelpers.callMethod(param.thisObject, "dismissRecentsToHomeWithoutTransitionAnimation");
//						XposedHelpers.callMethod(param.thisObject, "finish");
//						//((Activity) param.thisObject).sendBroadcast(new Intent("action_hide_recents_activity"));
//						}catch(Throwable t){
//							XposedBridge.log("hookRecents failed to hide recents");
//							XposedBridge.log(t);
//						}
//					}
//			});
//	}


	/*****HELPERS*****/

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

	private static boolean checkInheritFloatingFlag(String packageName, Object activityStack, Intent mIntent) throws Throwable {
		Debugger.DEBUG("checkInheritFloatingFlag", packageName);
		if(mIntent.hasCategory("restarted")){
			mIntent.removeCategory("restarted");
			return false;
		}
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
			return Util.isFlag(lastIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
		}
		Debugger.DEBUG("checkInheritFloatingFlag: nothing to inherit");
		return false;
	}

	private static boolean checkBlackWhiteList(boolean flag, String packageName){
		MainXposed.mPackagesList.reload();
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
		Debugger.DEBUG("checkBlackWhiteList result Flag=" + flag, packageName);
		return flag;
	}

	public static void removeAppStartingWindow(final Class<?> hookClass) throws Throwable {
		XposedBridge.hookAllMethods(hookClass, "setAppStartingWindow", new XC_MethodHook() {
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {			
					if (!MWRegister.isMovable((String) param.args[1])) return;
					//if (!mHasHaloFlag && (MovableWindow.mWindowHolder==null || !MovableWindow.mWindowHolder.isFloating)) return;
					//if ("android".equals((String) param.args[1])) return;
					// Change boolean "createIfNeeded" to FALSE
					if (param.args[param.args.length - 1] instanceof Boolean) {
						param.args[param.args.length - 1] = Boolean.FALSE;
						// Last param of the arguments
						// It's length has changed in almost all versions of Android.
						// Since it is always the last value, we use this to our advantage.
					}
					Debugger.DEBUG("setAppStartingWindow", (String) param.args[1]);
				}
			});
	}

	public static void hookActivityStack(Class<?> hookClass) throws Throwable {
		XposedBridge.hookAllMethods(hookClass, "resumeTopActivityLocked", new XC_MethodHook() {
				Object previous = null;
				boolean appPauseEnabled;
				boolean isMovable = false;
//				boolean isHalo;
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					// Find the first activity that is not finishing.
					final Object prevActivity = XposedHelpers.getObjectField(param.thisObject, "mResumedActivity");
					previous = prevActivity;
//					final Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(previous, "intent");
//					if(mIntent!=null && mIntent.hasCategory("restarted")){
//						String packageName = Util.getFailsafeStringFromObject(null, previous, "packageName");
//						if(packageName!=null){
//							removePackage(packageName);
//							mIntent.removeCategory("restarted");
//						}
//					}
//					isMovable = isMovable || (mIntent!=null&&Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)));
					String packageName = Util.getFailsafeStringFromObject(null, previous, "packageName");
					Debugger.DEBUG("resumeTopActivityLocked", packageName);
					isMovable = MWRegister.isMovable(packageName);
					if("com.whatsapp".equals(packageName))
						isMovable = false;
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

//					if(previous!=null)
//						XposedHelpers.setBooleanField(previous, "fullscreen", false);
					appPauseEnabled = MainXposed.mPref.getBoolean(Common.KEY_APP_PAUSE, Common.DEFAULT_APP_PAUSE);
					if (appPauseEnabled) return;
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
//					if(!isMovable)
//						throw new Exception("test");
					Object activityRecord = param.args[0];
					if(activityRecord==null) return;
//					ActivityInfo mActivityInfo = (ActivityInfo) Util.getFailsafeObjectFromObject(activityRecord, "info");
//					if(mActivityInfo!=null)
//					{
//						isMovable = isMovable || Util.isFlag(mActivityInfo.flags, MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
//						MovableWindow.DEBUG("startActivityLocked in flagtest [" + isMovable + "]");
//					}
					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(activityRecord, "intent");
					
//					if(mIntent!=null){
//						if(mIntent.hasCategory("restarted")){
//							removePackage(mIntent.getPackage());
//							//mIntent.removeCategory("restarted");
//							isMovable = false;
//						}
//						isMovable = isMovable || Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
//						MovableWindow.DEBUG("startActivityLocked in flagtest [" + isMovable + "]");
//					}
					String packageName = Util.getFailsafeStringFromObject(null, activityRecord, "packageName");
					if(packageName==null)
						packageName = mIntent.getPackage();
					Debugger.DEBUG("startActivityLocked", packageName);
					if(!MWRegister.isMovable(packageName)){
						if(mIntent!=null)
							removeFloatingFlag(mIntent);
						return;
						}
					if(mIntent!=null)
						setIntentFlags(mIntent);
//					if(packageName==null)
//						return;
//					isMovable = isMovable || MovableWindow.isMovable || isPackageMovable(packageName);
					
//					if (!isMovable) return;
					XposedHelpers.setBooleanField(activityRecord, "fullscreen", false);
				}
			});

//		XposedBridge.hookAllMethods(hookClass, "relaunchActivityLocked", new XC_MethodHook() {
//				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
//					MovableWindow.DEBUG("relaunchActivityLocked [" + isMovable + "]");
//					Object activityRecord = param.args[0];
//					if(activityRecord==null) return;
//					String packageName = Util.getFailsafeStringFromObject(null, activityRecord, "packageName");
//					if(packageName==null)
//						return;
//
//					isMovable = isMovable || MovableWindow.isMovable || isPackageMovable(packageName);
//					Intent mIntent = (Intent) Util.getFailsafeObjectFromObject(activityRecord, "intent");
//					if(mIntent!=null)
//						isMovable = isMovable || Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
//
//					if (!isMovable) return;
//					XposedHelpers.setBooleanField(activityRecord, "fullscreen", false);
//
//				}
//			});

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
