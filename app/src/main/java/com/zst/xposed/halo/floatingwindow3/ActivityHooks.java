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
import com.zst.xposed.halo.floatingwindow3.themable.*;

public class ActivityHooks
{
	public static String packageName = null;
	public static MWTasks taskStack = null;
	public static Activity mCurrentActivity = null;
	public static OverlayTheme mOverlayTheme = null;
	//private static Activity mPreviousActivity = null;
	public static boolean isMovable;
	
	public static void loadActivityHooks(final XC_LoadPackage.LoadPackageParam lpparam) {
		packageName = lpparam.packageName;
		Debugger.DEBUG_SWITCH = MainXposed.mPref.getBoolean(Common.KEY_DEBUG, Common.DEFAULT_DEBUG);
		hookActivityLoaders();
		injectGenerateLayout(lpparam);
	}
	
	private static void hookActivityLoaders(){
		XposedBridge.hookAllMethods(Activity.class, "onCreate", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					//mPreviousActivity = mCurrentActivity;
					mCurrentActivity = (Activity) param.thisObject;
					if(packageName==null)
						packageName = mCurrentActivity.getPackageName();
					Debugger.DEBUG("onCreate start ", packageName);
					registerRestartReceiver();
					isMovable =  isMovable || 
						(Util.isFlag(mCurrentActivity.getIntent().getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW)));
					if(!isMovable)
						return;
					MainXposed.mPref.reload();
					registerPrefsUpdateReceiver();
					if(taskStack==null)
						taskStack = new MWTasks(packageName, mCurrentActivity);
					taskStack.onNewTask(mCurrentActivity);
					Debugger.DEBUG("onCreate");
				}
			});
		
		XposedBridge.hookAllMethods(Activity.class, "onStart", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Debugger.DEBUG("onStart", packageName);
					mCurrentActivity = (Activity) param.thisObject;
					if(!isMovable || taskStack==null)
						return;
					taskStack.onNewWindow(mCurrentActivity.getWindow(), mCurrentActivity, mCurrentActivity.getTaskId());
				}
			});
			
		XposedBridge.hookAllMethods(Activity.class, "onResume", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Debugger.DEBUG("onResume start ", packageName);
					mCurrentActivity = (Activity) param.thisObject;
					if(!isMovable || taskStack==null)
						return;
					taskStack.onTaskResume(mCurrentActivity.getWindow(), mCurrentActivity, mCurrentActivity.getTaskId());
					Debugger.DEBUG("onResume");
				}
			});
		XposedBridge.hookAllMethods(Activity.class, "onPause", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					Debugger.DEBUG("onPause start ", packageName);
					Activity mActivity = (Activity) param.thisObject;
					if(!isMovable || taskStack==null)
						return;
					taskStack.onTaskPause(mActivity.getTaskId(), mActivity.getWindow());
					Debugger.DEBUG("onPause");
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
					if(!isMovable|| taskStack==null) return;
					boolean focused = param.args[0];
					Debugger.DEBUG("onWindowFocusChanged " + focused);
					if(!focused)
						taskStack.onTaskUnFocused(((Activity) param.thisObject).getTaskId(), ((Activity) param.thisObject).getWindow());
					else {
						mCurrentActivity =  (Activity) param.thisObject;
						taskStack.onTaskFocused(mCurrentActivity.getTaskId(), mCurrentActivity.getWindow());
					}
						
				}
			});
		
		XposedBridge.hookAllMethods(Activity.class, "onDestroy", new XC_MethodHook() {
				@Override
				protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
					if(!isMovable||taskStack==null) return;
					//mCurrentActivity =  (Activity) param.thisObject;
					if(taskStack.onRemoveActivity((Activity) param.thisObject)) {
						InterActivity.sendRemovedPackageInfo(packageName, ((Activity) param.thisObject).getApplicationContext(), true);
						taskStack = null;
						isMovable = false;
						mCurrentActivity = null;
						//packageName = null;
					}
					//mCurrentActivity = taskStack.startActivity;
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
					if (name.startsWith("com.android.systemui")||name.equals("android")) return;
					taskStack.onNewGeneratedWindow(window);
					Debugger.DEBUG("GenerateLayout end for" + name);
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
	
	
	final private static Runnable restartBroadcastCallback = new Runnable(){
		@Override
		public void run()
		{
			isMovable = false;
			taskStack = null;
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
	
	private static void registerPrefsUpdateReceiver() {
		if(InterActivity.updatePrefsReceiverRegistered)
			return;
		InterActivity.registerUpdatePrefsBroadcastReceiver(mCurrentActivity.getApplicationContext());
	}
	
}
