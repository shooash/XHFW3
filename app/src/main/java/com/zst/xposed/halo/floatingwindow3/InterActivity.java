package com.zst.xposed.halo.floatingwindow3;
import android.content.*;
import android.os.*;
import com.zst.xposed.halo.floatingwindow3.debug.*;
import android.app.*;
import com.zst.xposed.halo.floatingwindow3.floatdot.*;
import android.graphics.*;
import com.zst.xposed.halo.floatingwindow3.tasks.*;

public class InterActivity
{
	public static int[] FloatDotCoordinates = new int[2];
	public static Runnable FloatDotCoordinatesCallback = null;
	public static Runnable restartCallback = null;
	public static XHFWInterface XHFWInterfaceLink = null;
	static boolean showFocusOutline = false;
	public static boolean restartReceiverRegistered = false;
	public static boolean FloatDotReceiverRegistered = false;
	public static int focusedTaskId = -1;
	
	final static ServiceConnection XHFWServiceConnection = new ServiceConnection(){
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder)
		{
			XHFWInterfaceLink = XHFWInterface.Stub.asInterface(binder);				
			getFloatingDotCoordinates();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			XHFWInterfaceLink = null;
		}	
	};

	public static void connectService(final Context mContext){
		Debugger.DEBUG("connectService");
		if(XHFWInterfaceLink == null){
			Intent serviceStart = new Intent("persistent.XHFWService");
			serviceStart.setPackage(Common.FLOAT_DOT_PACKAGE);
			mContext.sendBroadcast(serviceStart);
			Intent intent = new Intent(Common.FLOAT_DOT_SERVICE_ACTION);
			intent.setPackage(Common.FLOAT_DOT_PACKAGE);
			mContext.bindService(intent, XHFWServiceConnection, Service.BIND_AUTO_CREATE);
		}
	}
	
	public static void getFloatingDotCoordinates(){
		try
		{
			int[] result = XHFWInterfaceLink.getCurrentFloatdotCoordinates();
			if(result==null) return;
			FloatDotCoordinates[0] = result[0];
			FloatDotCoordinates[1] = result[1];
			Debugger.DEBUG("getFloatingDotCoordinates [" + FloatDotCoordinates[0] +":"+FloatDotCoordinates[1]+"]");
		} catch (RemoteException e) {}
	}


	final static BroadcastReceiver FloatDotBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Common.REFRESH_FLOAT_DOT_POSITION)){
				Debugger.DEBUG( "REFRESH_FLOATDOT_POSITION");
				int[] coordinates = intent.getIntArrayExtra(Common.INTENT_FLOAT_DOT_EXTRA);
				if(coordinates == null) return;
				FloatDotCoordinates = coordinates;
				if(FloatDotCoordinatesCallback!=null)
					FloatDotCoordinatesCallback.run();
			}
		}
	};

	public static boolean registerFloatDotBroadcastReceiver(final Context mContext) {
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.REFRESH_FLOAT_DOT_POSITION);
		//	filters.addAction(Common.RESTART_ACTIVITY);
		try{
			mContext.registerReceiver(FloatDotBroadcastReceiver, filters);
		} catch(Throwable e){
			Debugger.DEBUG_E("registerLayoutBroadcastReceiver failed");
			return false;
		}
		FloatDotReceiverRegistered = true;
		return true;
	}

	public static void unregisterFloatDotBroadcastReceiver(final Context mContext) {
		try{
			mContext.unregisterReceiver(FloatDotBroadcastReceiver);
		} catch(Throwable e){
			Debugger.DEBUG_E(" failed to unregister FloatDotBroadcastReceiver");
		}
	}
	
	final static BroadcastReceiver restartBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Common.RESTART_ACTIVITY)&&restartCallback!=null){
				restartCallback.run();
			}
		}
	};

	public static boolean registerRestartBroadcastReceiver(final Context mContext) {
		IntentFilter filters = new IntentFilter();
		filters.addAction(Common.RESTART_ACTIVITY);
		try{
			mContext.registerReceiver(restartBroadcastReceiver, filters);
		} catch(Throwable e){
			Debugger.DEBUG_E("registerRestartBroadcastReceiver failed");
			return false;
		}
		restartReceiverRegistered = true;
		return true;
	}

	public static boolean changeFocusApp(final Activity a) {
		int task = a.getTaskId();
		if(task == focusedTaskId)
			return false;
		Debugger.DEBUG("changeFocusApp " + task);
		final WindowHolder mWindowHolder = new WindowHolder(a.getWindow());
		drawFocusFrame(ActivityHooks.taskStack.appContext, mWindowHolder.x, mWindowHolder.y, mWindowHolder.width, mWindowHolder.height);
		try {
			XHFWInterfaceLink.bringToFront(task);
			focusedTaskId = task;
		}
		catch (Throwable e)
		{
			Debugger.DEBUG_E("changeFocusApp failed for " + task);
		}
		return true;
	}
	
	public static boolean unfocusApp(int task) {
		if(focusedTaskId == task) {
			Debugger.DEBUG("unfocusApp " + task);
			focusedTaskId = -1;
			return true;
		}
		return false;
	}

	public static void drawFocusFrame(final Context mContext, final int x, final int y, final int width, final int height){
		if(showFocusOutline) return;
//		//hide previous outlines
//		//hideFocusFrame(mContext);
//		//send new params
//		Intent mIntent = new Intent(Common.SHOW_OUTLINE);
//		int[] array = {x, y, width, height};
//		mIntent.putExtra(Common.INTENT_APP_PARAMS, array);
//		mIntent.putExtra(Common.INTENT_APP_FOCUS, true);
//		Util.sendBroadcastSafe(mIntent, mContext);
		showFocusOutline = true;
	}

	public static void hideFocusFrame(final Context mContext){
		if(!showFocusOutline)
			return;
//		Util.sendBroadcastSafe(new Intent(Common.SHOW_OUTLINE), mContext);
		//mContext.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
		showFocusOutline = false;
	}
	
	public static void resetFocusFrameIfNeeded(final Context mContext, final int x, final int y, final int width, final int height, int taskId) {
		if(focusedTaskId == taskId) {
			hideFocusFrame(mContext);
			drawFocusFrame(mContext, x, y, width, height);
		}
	}

	public static void toggleDragger(final Context ctx, final boolean show){
		Intent intent = new Intent(Common.SHOW_MULTIWINDOW_DRAGGER);
		intent.putExtra(Common.INTENT_FLOAT_DOT_BOOL, show);
		Util.sendBroadcastSafe(intent, ctx);
		//ctx.sendBroadcast(intent);
	}

	

	public static void sendRemovedPackageInfo(final String packageName, final Context mContext, final boolean mCompletely){
		Intent mIntent = new Intent(Common.ORIGINAL_PACKAGE_NAME + ".APP_REMOVED");
		mIntent.putExtra("packageName", packageName);
		mIntent.putExtra("removeCompletely", mCompletely);
		//mContext.getApplicationContext().sendBroadcast(mIntent);
		Util.sendBroadcastSafe(mIntent, mContext);
	}

	public static void sendPackageInfo(final String packageName, final Context mContext, final int taskId, final int SnapGravity){
		Intent mIntent = new Intent(Common.ORIGINAL_PACKAGE_NAME + ".APP_LAUNCHED");
		mIntent.putExtra("packageName", packageName);
		mIntent.putExtra("float-gravity", SnapGravity);
		mIntent.putExtra("float-taskid", taskId);
		//mWindowHolder.mActivity.getApplicationContext().sendBroadcast(mIntent);
		Util.sendBroadcastSafe(mIntent, mContext);
	}
}
