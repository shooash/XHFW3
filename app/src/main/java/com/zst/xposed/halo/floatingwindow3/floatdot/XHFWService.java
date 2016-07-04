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
import android.graphics.*;

public class XHFWService extends Service {
	Context mContext;
	static Class<?> classSvcMgr;
	public FloatDot fd = null;
	private FloatDot ld = null;
	private int cachedRotation = 0;
	private FloatLauncher mFloatLauncher;
	private SharedPreferences mPref;
	//Colors: snapdragger_dot_outer, snapdragger_dot_inner, launher_dot_outer, launcher_dot_inner
	private int[] mColors = new int[4];
	private int mDotsSize = Common.DEFAULT_FLOATDOT_SIZE;
	private boolean isLauncherDotEnabled;
	private int lastTaskId = 0;
	private int previousTaskId = 0;

	@Override
	public void onCreate()
	{
		// TODO: Implement this method
		//fd = new FloatingDot(getApplicationContext());
		mContext = this;
		mPref = getSharedPreferences(Common.PREFERENCE_MAIN_FILE, MODE_MULTI_PROCESS);
		isLauncherDotEnabled = mPref.getBoolean(Common.KEY_FLOATDOT_LAUNCHER_ENABLED, Common.DEFAULT_FLOATDOT_LAUNCHER_ENABLED);

		mFloatLauncher = new FloatLauncher(mContext, mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
		cachedRotation = Util.getDisplayRotation(mContext.getApplicationContext());
		
		loadColors();
		if(isLauncherDotEnabled) setupLauncherDot();
		registerBroadcast();
		super.onCreate();
	}
	
	private void setupLauncherDot(){
		Point mScreenSize = Util.getScreenSize(mContext.getApplicationContext());
		int x = mScreenSize.x/100*mPref.getInt(Common.KEY_FLOATDOT_LAUNCHER_X, Common.DEFAULT_FLOATDOT_LAUNCHER_X);
		int y = mScreenSize.y/100*mPref.getInt(Common.KEY_FLOATDOT_LAUNCHER_Y, Common.DEFAULT_FLOATDOT_LAUNCHER_Y);
		if(ld!=null)
			ld.removeDot();
		ld = new FloatDot(mContext, mFloatLauncher, mDotsSize, x, y, mColors[2], mColors[3]);
		ld.setAlpha(mPref.getFloat(Common.KEY_FLOATDOT_ALPHA, Common.DEFAULT_FLOATDOT_ALPHA));
		ld.putDragger();
		ld.registerContextMenuOnFloatDot();
		ld.showDragger(true);
	}
	
	private void saveLauncherPosition(int x, int y){
		if(x==0||y==0) return;
		final Point mScreenSize = Util.getScreenSize(mContext);
		SharedPreferences.Editor editor = mPref.edit();
		editor.putInt(Common.KEY_FLOATDOT_LAUNCHER_X, 100*x/mScreenSize.x);
		editor.putInt(Common.KEY_FLOATDOT_LAUNCHER_Y, 100*y/mScreenSize.y);
		editor.apply();
	}

	@Override
	public void onDestroy()
	{
		if(ld!=null) {
			saveLauncherPosition(ld.mCoordinates.x, ld.mCoordinates.y);
			ld.removeDot();
			}
		if(fd!=null) fd.removeDot();
		super.onDestroy();
	}
	
	
	
	@Override
	public IBinder onBind(Intent intent){
		loadColors();
		Point mScreenSize = Util.getScreenSize(mContext);
		if(fd==null) 
		{
			fd = new FloatDot(mContext, mFloatLauncher, mDotsSize, mScreenSize.x/2, mScreenSize.y/2, mColors[0], mColors[1]);
			fd.enableCommunication();
			fd.setAlpha(mPref.getFloat(Common.KEY_FLOATDOT_ALPHA, Common.DEFAULT_FLOATDOT_ALPHA));
			fd.putDragger();
			}
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent)
	{
		if(fd!=null) fd.removeDot();
		if(ld!=null){
			saveLauncherPosition(ld.mCoordinates.x, ld.mCoordinates.y);
		}
		unregisterReceiver(br);
		return super.onUnbind(intent);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		//int curRotation = Util.getDisplayRotation(this);
		//fd.rotatePosition(curRotation-cachedRotation);
		if(fd!=null)
			fd.rotatePositionByOrientation();
		if(ld!=null)
			ld.rotatePositionByOrientation();
		super.onConfigurationChanged(newConfig);
	}
	
	private void loadColors(){
		mDotsSize = mPref.getInt(Common.KEY_FLOATDOT_SIZE, Common.DEFAULT_FLOATDOT_SIZE);
		mColors[0] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_OUTER1, Common.DEFAULT_FLOATDOT_COLOR_OUTER1));
		if(mPref.getBoolean(Common.KEY_FLOATDOT_SINGLE_COLOR_SNAP, Common.DEFAULT_FLOATDOT_SINGLE_COLOR_SNAP))
			mColors[1] = mColors[0];
		else
			mColors[1] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_INNER1, Common.DEFAULT_FLOATDOT_COLOR_INNER1));
		mColors[2] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_OUTER2, Common.DEFAULT_FLOATDOT_COLOR_OUTER2));
		if(mPref.getBoolean(Common.KEY_FLOATDOT_SINGLE_COLOR_LAUNCHER, Common.DEFAULT_FLOATDOT_SINGLE_COLOR_LAUNCHER))
			mColors[3] = mColors[2];
		else
			mColors[3] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_INNER2, Common.DEFAULT_FLOATDOT_COLOR_INNER2));
	}
	
	BroadcastReceiver br = new BroadcastReceiver(){
		@Override
		public void onReceive(Context sContext, Intent sIntent)
		{
			if(sIntent.getAction().equals(Common.UPDATE_FLOATDOT_PARAMS)){
				if(sIntent.hasExtra(Common.KEY_FLOATDOT_LAUNCHER_ENABLED)){
					isLauncherDotEnabled = sIntent.getBooleanExtra(Common.KEY_FLOATDOT_LAUNCHER_ENABLED, Common.DEFAULT_FLOATDOT_LAUNCHER_ENABLED);
					if(ld==null&&isLauncherDotEnabled)
						setupLauncherDot();
					if(ld!=null&&!isLauncherDotEnabled){
						ld.removeDot();
						ld = null;
					}
					return;
				}			
				String mColor = sIntent.getStringExtra("color");
				int item = sIntent.getIntExtra("item", 0);
				int size = sIntent.getIntExtra("size", 0);
				float alpha = sIntent.getFloatExtra("alpha", 0);
				if(size!=0){
					if(fd!=null){
						fd.setSize(size);
						fd.redrawView();
					}
					if(ld!=null){
						ld.setSize(size);
						ld.redrawView();
					}
					return;
				}
				if(alpha!=0){
					if(fd!=null){
						fd.setAlpha(alpha);
						fd.redrawView();
					}
					if(ld!=null){
						ld.setAlpha(alpha);
						ld.redrawView();
					}
					return;
				}
				if(mColor!=null)
					mColors[item]=Color.parseColor("#" + mColor);
				switch(item){
					case 0:
					case 1:
						if(fd!=null){
							fd.setColor(mColors[0], mColors[1]);
							fd.redrawView();
						}
						break;
					case 2:
					case 3:
						if(ld!=null){
							ld.setColor(mColors[2], mColors[3]);
							ld.redrawView();
						}
						break;
				}
			}
				
			if(!sIntent.getAction().equals(Common.SHOW_MULTIWINDOW_DRAGGER)) return;
			boolean show = sIntent.getBooleanExtra(Common.INTENT_FLOAT_DOT_BOOL, false);
			if(fd!=null) fd.showDragger(show);
			if(ld!=null) ld.showDragger(!show);
		}
	};
	
	private void registerBroadcast(){
		IntentFilter i = new IntentFilter();
		i.addAction(Common.SHOW_MULTIWINDOW_DRAGGER);
		i.addAction(Common.UPDATE_FLOATDOT_PARAMS);
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
			return lastTaskId;
		}
		
		@Override
		public void toggleDragger(boolean show) throws RemoteException {
			fd.showDragger(show);
		}
		
		@Override
		public void bringToFront(int taskId) throws RemoteException
		{
			previousTaskId = lastTaskId;
			lastTaskId = taskId;
			mContext=getApplicationContext();
			ActivityManager mActivityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
			try {
				mActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
			} catch (Exception e) {
				Log.e("Xposed XHFWService", "Cannot move task to front", e);
				return;
			}
			
		}
		
		
	};
	}

