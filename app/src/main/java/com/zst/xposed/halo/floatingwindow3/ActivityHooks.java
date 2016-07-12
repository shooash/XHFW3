package com.zst.xposed.halo.floatingwindow3;
import de.robv.android.xposed.callbacks.*;
import com.zst.xposed.halo.floatingwindow3.tasks.*;
import de.robv.android.xposed.*;
import android.app.*;
import com.zst.xposed.halo.floatingwindow3.debug.*;
import android.system.*;
import android.os.*;
import junit.runner.*;
import android.view.*;

public class ActivityHooks
{
	public static String packageName = null;
	public static MWTasks taskStack = null;
	private static Activity mCurrentActivity;
	public static boolean isMovable;
	
	public static void loadActivityHooks(final XC_LoadPackage.LoadPackageParam lpparam) {
		packageName = lpparam.packageName;
		hookActivityLoaders();
		injectGenerateLayout(lpparam);
	}
	
	private static void hookActivityLoaders(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					mCurrentActivity = (Activity) param.thisObject;
					if(packageName==null)
						packageName = mCurrentActivity.getPackageName();
					Debugger.DEBUG("onCreate start ", packageName);
					registerRestartReceiver();
					isMovable =  isMovable || 
						(Util.isFlag(mCurrentActivity.getIntent().getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)));
					if(!isMovable)
						return;
					if(taskStack==null)
						taskStack = new MWTasks(packageName, mCurrentActivity);
					taskStack.onNewTask(mCurrentActivity);
					Debugger.DEBUG("onCreate");
				}
			});
		
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Debugger.DEBUG("onStart ", packageName);
					mCurrentActivity = (Activity) param.thisObject;
					if(!isMovable || taskStack==null)
						return;
					taskStack.onNewWindow(mCurrentActivity.getWindow(), mCurrentActivity.getTaskId());
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Debugger.DEBUG("onResume start ", packageName);
					mCurrentActivity = (Activity) param.thisObject;
					if(!isMovable || taskStack==null)
						return;
					taskStack.onTaskResume(mCurrentActivity.getWindow(), mCurrentActivity.getTaskId());
					Debugger.DEBUG("onResume");
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "dispatchTouchEvent", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(!isMovable|| taskStack==null) return;
					Debugger.DEBUG("dispatchTouchEvent", packageName);
					mCurrentActivity = (Activity) param.thisObject;
					MotionEvent event = (MotionEvent) param.args[0];
					taskStack.onUserAction(mCurrentActivity, event);

				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onWindowFocusChanged", new XC_MethodHook() {
				@Override
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					boolean focused = param.args[0];
					mCurrentActivity =  (Activity) param.thisObject;
					if(!focused)
						InterActivity.unfocusApp(mCurrentActivity.getTaskId());
				}
			});
			
		
	}
	
	private static void injectGenerateLayout(final XC_LoadPackage.LoadPackageParam lpparam) {
		Class<?> cls = null;
		try{
			cls = XposedHelpers.findClass(MainXposed.mCompatibility.Internal_PhoneWindow, lpparam.classLoader);
		} catch(Throwable t){
			XposedBridge.log(t);
			return;
		}
		if(cls==null) return;
		XposedBridge.hookAllMethods(cls, "generateLayout", new XC_MethodHook() {
				protected void afterHookedMethod(MethodHookParam param) throws Throwable {
					Debugger.DEBUG("GenerateLayout", packageName);
					if (!isMovable||taskStack==null) return;
					Window window = (Window) param.thisObject;
					String name = window.getContext().getPackageName();
					//if (name.startsWith("com.android.systemui")||name.equals("android")) return;

					Debugger.DEBUG("GenerateLayout end for" + name);
				}
			});
	}
	
	final private static Runnable restartBroadcastCallback = new Runnable(){
		@Override
		public void run()
		{
			if(android.os.Build.VERSION.SDK_INT>=21)
				mCurrentActivity.finishAndRemoveTask();
			else
				mCurrentActivity.finish();
		}
	};
	
	private static void registerRestartReceiver() {
		if(InterActivity.restartReceiverRegistered)
			return;
		InterActivity.restartCallback = restartBroadcastCallback;
		InterActivity.registerRestartBroadcastReceiver(mCurrentActivity.getApplicationContext());
	}
	
}
