package com.zst.xposed.halo.floatingwindow3;

import com.zst.xposed.halo.floatingwindow3.Common;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class TestingSettingHook {
	
	public static void handleLoadPackage(LoadPackageParam lpp) {
		if (lpp.packageName.equals(Common.THIS_MOD_PACKAGE_NAME)) {
			Class<?> hookClass = XposedHelpers.findClass("com.zst.xposed.halo.floatingwindow3.MainPreference",
					lpp.classLoader);
			XposedBridge.hookAllMethods(hookClass, "testSettings", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					param.args[0] = false;
				}
			});
		}
	}
}
