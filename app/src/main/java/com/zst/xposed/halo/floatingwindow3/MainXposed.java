package com.zst.xposed.halo.floatingwindow3;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import static de.robv.android.xposed.XposedHelpers.*;
import android.content.res.*;
import java.util.*;

public class MainXposed implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	
	public static XModuleResources sModRes = null;
	public static XSharedPreferences mPref = null;
	public static XSharedPreferences mPackagesList = null;
	public static Compatibility.Hooks mCompatibility =  new Compatibility.Hooks();
	

	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		try{
		if(sModRes==null)
		 sModRes = XModuleResources.createInstance(startupParam.modulePath, null);
			}catch(Throwable t){
				XposedBridge.log("ModuleResources init failed");
				XposedBridge.log(t);
			}
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
	if(mPref==null) 
			mPref = new XSharedPreferences(Common.THIS_MOD_PACKAGE_NAME, Common.PREFERENCE_MAIN_FILE);
	if(mPackagesList==null)
			mPackagesList = new XSharedPreferences(Common.THIS_MOD_PACKAGE_NAME, Common.PREFERENCE_PACKAGES_FILE);
	mPref.reload();
	mPackagesList.reload();
	if(!mPref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW)) return;
	
	if(lpparam.packageName==null) return;
	XposedBridge.log("XHFW3 load package " + lpparam.packageName);
	if(lpparam.packageName.equals("android")){
		try {
			Class<?> classActivityRecord = findClass("com.android.server.am.ActivityRecord", lpparam.classLoader);
			if (classActivityRecord != null)
				SystemHooks.hookActivityRecord(classActivityRecord);
			Class<?> classTaskRecord = findClass("com.android.server.am.TaskRecord", lpparam.classLoader);
			if (classTaskRecord != null)
				SystemHooks.hookTaskRecord(classTaskRecord);
				else
					XposedBridge.log("Failed to get class TaskRecord");
//			Class<?> AMS = findClass("com.android.server.am.ActivityManagerService", lpparam.classLoader);
//			if(AMS!=null)
//				SystemHooks.hookAMS(AMS);
		}
		catch (Throwable e){
			XposedBridge.log("hookActivityRecord failed - Exception");
			XposedBridge.log(e);
		}
		try {
			Class<?> classWMS = findClass("com.android.server.wm.WindowManagerService", lpparam.classLoader);
			if(classWMS!=null)
				SystemHooks.removeAppStartingWindow(classWMS);
		} catch(ClassNotFoundError e){
			XposedBridge.log("Class com.android.server.wm.WindowManagerService not found in MainXposed");
		}catch (Throwable e){
			XposedBridge.log("removeAppStartingWindow failed - Exception");
			XposedBridge.log(e);
		}
		try{
			Class<?> classActivityStack = findClass("com.android.server.am.ActivityStack", lpparam.classLoader);
			if(classActivityStack!=null)
				SystemHooks.hookActivityStack(classActivityStack);
		} catch(ClassNotFoundError e){
			XposedBridge.log("Class com.android.server.am.ActivityStack not found in MainXposed");
		}catch (Throwable e){
			XposedBridge.log("hookActivityStack failed - Exception");
			XposedBridge.log(e);
		}
		
	} else if(!lpparam.packageName.startsWith("com.android.systemui")){
		try{
			MovableWindow.hookActivity(lpparam);
		} catch (Throwable t){
			XposedBridge.log("MovableWindow hook failed");
			XposedBridge.log(t);
			return;
		}
		try{
			Class<?> clsAT = findClass("android.app.ActivityThread", lpparam.classLoader);
			if(clsAT!=null){
				MovableWindow.fixExceptionWhenResuming(clsAT);
				} 
			}catch(Throwable t){XposedBridge.log(t);}
		
		// XHFW
		TestingSettingHook.handleLoadPackage(lpparam);
		}
	else if(lpparam.packageName.equals("com.android.systemui")) {//TODO SHOULDN'T HOOK SYSTEMUI
		try{
			SystemUIOutliner.handleLoadPackage(lpparam);
			} catch(Exception e){
				XposedBridge.log("SystemUIOutliner exception in MainXposed");
				XposedBridge.log(e);
			} catch(Throwable t){
				XposedBridge.log(t);
			}
//		try{
//			Class<?> classRecentsApps = findClass("com.android.systemui.recents.RecentsActivity", lpparam.classLoader);
//			if(classRecentsApps!=null){
//				SystemHooks.hookRecents(classRecentsApps);
//			} 
//			else {
//				XposedBridge.log("class Recents not found");
//			}
//		} catch (Throwable t){
//			XposedBridge.log("hookRecents failed");
//			XposedBridge.log(t);
//			}
		} //elseif
	}//handleLoadPackage

	public static boolean isBlacklisted(String pkg) {
		return Util.isFlag(mPackagesList.getInt(pkg, 0), Common.PACKAGE_BLACKLIST);
	}
	
	public static boolean isWhitelisted(String pkg) {
		return Util.isFlag(mPackagesList.getInt(pkg, 0), Common.PACKAGE_WHITELIST);
	}
	
	public static int getBlackWhiteListOption() {
		mPref.reload();
		return Integer.parseInt(mPref.getString(Common.KEY_WHITEBLACKLIST_OPTIONS, Common.DEFAULT_WHITEBLACKLIST_OPTIONS));
	}
	
	public static boolean isMaximizedlisted(String pkg) {
		return Util.isFlag(mPackagesList.getInt(pkg, 0), Common.PACKAGE_MAXIMIZE);
	}

}

