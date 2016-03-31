package com.zst.xposed.halo.floatingwindow3.floatdot;
import java.util.HashMap;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.os.*;


public class XHFWService extends Service {
	Context mContext;
	static final String SERVICE_NAME = "service.XHFWService";
	public static final String SHOW_MULTIWINDOW_DRAGGER = "com.zst.xposed.halo.floatingwindow3" + ".SHOW_MULTIWINDOW_DRAGGER";
	public static final String INTENT_FLOAT_DOT_BOOL = "float_dot_bool";
	static Class<?> classSvcMgr;
	public FloatingDot fd = null;

	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		//fd = new FloatingDot(getApplicationContext());
		super.onCreate();
	}
	
	/*public XHFWService(Context sContext){
		mContext = sContext;
		/*Intent i = new Intent(mContext,XHFWService.class);
		i.setAction("service.XHFWService");
		mContext.startService(i);*/
	/*}*/
	
	
	@Override
	public IBinder onBind(Intent intent){
		
		//fd.putDragger();
		//toggle(true);
		mContext = this;
		//Toast.makeText(mContext, "toogleDragger", Toast.LENGTH_SHORT).show();
		fd = new FloatingDot(mContext);
		fd.putDragger();
		registerBroadcast();
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		if(fd!=null) fd.hideDragger();
		return super.onUnbind(intent);
	}
	
	private void registerBroadcast(){
		BroadcastReceiver br = new BroadcastReceiver(){

			@Override
			public void onReceive(Context sContext, Intent sIntent)
			{
				if(!sIntent.getAction().equals(SHOW_MULTIWINDOW_DRAGGER)) return;
				boolean show = sIntent.getBooleanExtra(INTENT_FLOAT_DOT_BOOL, false);
				fd.showDragger(show);
			}
		};
		IntentFilter i = new IntentFilter();
		i.addAction(SHOW_MULTIWINDOW_DRAGGER);
		registerReceiver(br,i);
	}
	
	private final XHFWInterface.Stub mBinder = new XHFWInterface.Stub(){

		@Override
		public int[] getCurrentFloatdotCoordinates() throws RemoteException
		{
			if(fd==null) return null; //fd = new FloatingDot(mContext);
			Log.i("Xposed XHFWService", "getCurrentFloatdotCoordinates");
			return fd.getAbsoluteCoordinates();
		}

		//private FloatingDot fd;
		//private Handler h;
		
		
		@Override
		public void removeAppTask(int taskId, int flags) throws RemoteException
		{
			// TODO: Implement this method
		}

		@Override
		public int getLastTaskId() throws RemoteException
		{
			// TODO: Implement this method
			return 0;
		}
		
		@Override
		public void toggleDragger(boolean show) throws RemoteException {
			
			Log.i("Xposed XHFWService", "toggleDragger called");
			fd.showDragger(false);
			//h = new Handler();
			//h.post(new FloatingDot(getApplicationContext()));
			/*if(fd==null) fd = new FloatingDot(mContext);
			//fd.showDragger(true);
			Toast.makeText(mContext, "toogleDragger", Toast.LENGTH_SHORT).show();*/
			Log.i("Xposed XHFWService", "toggleDragger end");
			//if (fd!=null) return;
			//if(fd==null) fd = new FloatingDot(getApplicationContext());
			//toggle(show);
		}
		
		@Override
		public void bringToFront(int taskId) throws RemoteException
		{
			//toggleDragger(true);
			mContext=getApplicationContext();
			//Log.e("Xposed XHFWService", "SUCCESS SUCCESS SUCCESS service called with taskid="+taskId);
			ActivityManager mActivityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
			try {
				//XposedHelpers.callMethod(mActivityManager, "moveTaskToFront", mActivity.getTaskId(),ActivityManager.MOVE_TASK_NO_USER_ACTION);
				mActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
			} catch (Exception e) {
				Log.e("Xposed XHFWService", "Cannot move task to front", e);
				
				//XposedBridge.log(e);
				//Log.e("test1", Common.LOG_TAG + "Cannot move task to front", e);
			}
		}
		
		
	};
	}

