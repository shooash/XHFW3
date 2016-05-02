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
import org.w3c.dom.*;
import android.content.res.*;
import com.zst.xposed.halo.floatingwindow3.*;

public class XHFWService extends Service {
	Context mContext;
	static final String SERVICE_NAME = Common.FLOAT_DOT_SERVICE_ACTION;
	public static final String SHOW_MULTIWINDOW_DRAGGER = Common.SHOW_MULTIWINDOW_DRAGGER;
	public static final String INTENT_FLOAT_DOT_BOOL = Common.INTENT_FLOAT_DOT_BOOL;
	static Class<?> classSvcMgr;
	public FloatingDot fd = null;
	private int cachedRotation = 0;

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
		cachedRotation = Util.getDisplayRotation(this);
		
		if(fd==null) 
		{
			fd = new FloatingDot(mContext);
			fd.putDragger();
			registerBroadcast();
			}
		//fd.sendPosition();
		//Log.i("XHFWService", "position sent");
		//Toast.makeText(mContext, "toogleDragger", Toast.LENGTH_SHORT).show();
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		if(fd!=null) fd.hideDragger();
		unregisterReceiver(br);
		return super.onUnbind(intent);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		//int curRotation = Util.getDisplayRotation(this);
		//fd.rotatePosition(curRotation-cachedRotation);
		fd.rotatePositionByOrientation();
		super.onConfigurationChanged(newConfig);
	}
	
	BroadcastReceiver br = new BroadcastReceiver(){

		@Override
		public void onReceive(Context sContext, Intent sIntent)
		{
			if(!sIntent.getAction().equals(SHOW_MULTIWINDOW_DRAGGER)) return;
			boolean show = sIntent.getBooleanExtra(INTENT_FLOAT_DOT_BOOL, false);
			fd.showDragger(show);
		}
	};
	
	private void registerBroadcast(){
		
		IntentFilter i = new IntentFilter();
		i.addAction(SHOW_MULTIWINDOW_DRAGGER);
		registerReceiver(br,i);
	}
	
	private final XHFWInterface.Stub mBinder = new XHFWInterface.Stub(){

		@Override
		public int[] getCurrentFloatdotCoordinates() throws RemoteException
		{
			if(fd==null) return null; //fd = new FloatingDot(mContext);
			int result[] = fd.getAbsoluteCoordinates();
			Log.i("Xposed XHFWService", "getCurrentFloatdotCoordinates ["+result[0]+":"+result[1]+"]");
			return result;
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
			
			//Log.i("Xposed XHFWService", "toggleDragger called");
			fd.showDragger(show);
			//h = new Handler();
			//h.post(new FloatingDot(getApplicationContext()));
			/*if(fd==null) fd = new FloatingDot(mContext);
			//fd.showDragger(true);
			Toast.makeText(mContext, "toogleDragger", Toast.LENGTH_SHORT).show();*/
			//Log.i("Xposed XHFWService", "toggleDragger end");
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

