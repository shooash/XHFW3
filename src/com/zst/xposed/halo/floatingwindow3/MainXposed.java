package com.zst.xposed.halo.floatingwindow3;

import com.zst.xposed.halo.floatingwindow3.hooks.ActionBarColorHook;
import com.zst.xposed.halo.floatingwindow3.hooks.HaloFloating;
import com.zst.xposed.halo.floatingwindow3.hooks.MovableWindow;
import com.zst.xposed.halo.floatingwindow3.hooks.StatusbarTaskbar;
import com.zst.xposed.halo.floatingwindow3.hooks.SystemMods;
import com.zst.xposed.halo.floatingwindow3.hooks.SystemUIOutliner;
import com.zst.xposed.halo.floatingwindow3.hooks.TestingSettingHook;


import android.content.res.XModuleResources;
import android.os.Build;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import com.zst.xposed.halo.floatingwindow3.helpers.*;
import com.zst.xposed.halo.floatingwindow3.hooks.*;
import android.app.*;
import android.content.*;
import java.io.*;

public class MainXposed implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	
	public static XModuleResources sModRes;
	// TODO make local
	public XSharedPreferences mPref;
	public XSharedPreferences mBlacklist;
	public XSharedPreferences mWhitelist;
	
	/* Hook References */
	public MovableWindow hookMovableWindow;
	public HaloFloating hookHaloFloating;
	//public ActionBarColorHook hookActionBarColor;
	public Compatibility.HookedMethods mHookedMethods;
	
	
	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		mPref = new XSharedPreferences(Common.THIS_MOD_PACKAGE_NAME, Common.PREFERENCE_MAIN_FILE);
		mBlacklist = new XSharedPreferences(Common.THIS_MOD_PACKAGE_NAME, Common.PREFERENCE_BLACKLIST_FILE);
		mWhitelist = new XSharedPreferences(Common.THIS_MOD_PACKAGE_NAME, Common.PREFERENCE_WHITELIST_FILE);
		sModRes = XModuleResources.createInstance(startupParam.modulePath, null);
		
//		File f = new File("/data/dalvik-cache/x86/", "data@app@com.zst.app.multiwindowsidebar-1@base.apk@classes.dex");
//		f.delete();
	}
	
	@Override
	public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
		// XHFW
		TestingSettingHook.handleLoadPackage(lpparam);
		// Compatibility settings
		mHookedMethods = Compatibility.getHookedMethods();
		
		//StatusbarTaskbar.handleLoadPackage(lpparam, mPref);
		
		// SystemUI MultiWindow
		try
		{
			SystemUIOutliner.handleLoadPackage(lpparam);
		}
		catch (Throwable e)
		{
			XposedBridge.log(e);
		}

		// Android
		/*try {
			SystemMods.handleLoadPackage(lpparam, mPref);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "(MainXposed // SystemMods)");
			XposedBridge.log(e);
		}*/
		
		// App
		try
		{
			hookMovableWindow = new MovableWindow(this, lpparam);
		}
		catch (Throwable e)
		{
			XposedBridge.log("MovableWindow failure");
			XposedBridge.log(e);
		}
		try
		{
			hookHaloFloating = new HaloFloating(this, lpparam, mPref);
		}
		catch (Throwable e)
		{
			XposedBridge.log("HaloFloating failure");
			XposedBridge.log(e);
		}
		//hookActionBarColor = new ActionBarColorHook(this, lpparam, mPref);
	
	}

	public boolean isBlacklisted(String pkg) {
		mBlacklist.reload();
		return mBlacklist.contains(pkg);
	}
	
	public boolean isWhitelisted(String pkg) {
		mWhitelist.reload();
		return mWhitelist.contains(pkg);
	}
	
	public int getBlackWhiteListOption() {
		mPref.reload();
		return Integer.parseInt(mPref.getString(Common.KEY_WHITEBLACKLIST_OPTIONS, Common.DEFAULT_WHITEBLACKLIST_OPTIONS));
	}

}
