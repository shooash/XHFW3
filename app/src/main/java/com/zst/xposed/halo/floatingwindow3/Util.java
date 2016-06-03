package com.zst.xposed.halo.floatingwindow3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import de.robv.android.xposed.XposedHelpers;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.app.*;
import android.view.*;
import android.content.res.*;
import de.robv.android.xposed.*;
import android.util.*;
import android.content.*;
import android.preference.*;
import java.io.*;
import java.util.*;
import android.app.usage.*;
import android.os.Handler;
import android.provider.*;


public class Util
{
	/* Get System DPI from build.prop 
	 * Some ROMs have Per-App DPI and it might make our views inconsistent 
	 * Fallback to app dpi if it fails*/
	public static int realDp(int dp, Context c) {
		String dpi = "";
		try {
			Process p = new ProcessBuilder("/system/bin/getprop", "ro.sf.lcd_density")
				.redirectErrorStream(true).start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				dpi = line;
			}
			p.destroy();
		} catch (Exception e) {
			dpi = "0";
			//failed, set to zero.
		} catch(Throwable t){
			if(dpi.equals("")) dpi="0";
		}
		float scale = Integer.parseInt(dpi);
		if (scale == 0) {
			// zero means it failed in getting dpi, fallback to app dpi 
			scale = c.getResources().getDisplayMetrics().density;
		} else {
			scale = (scale / 160);
		}
		return (int) (dp * scale + 0.5f);
	}

	/* Get App DPI */
	public static int dp(int dp, Context c) {
		float scale = c.getResources().getDisplayMetrics().density;
		return (int) (dp * scale + 0.5f);
	}

	/* Create a Border */
	public static ShapeDrawable makeOutline(int color, int thickness) {
		ShapeDrawable rectShapeDrawable = new ShapeDrawable(new RectShape());
		Paint paint = rectShapeDrawable.getPaint();
		paint.setColor(color);
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(thickness);
		return rectShapeDrawable;
	}

	public static ShapeDrawable makeCircle(int color, int diameter) {
		ShapeDrawable shape = new ShapeDrawable(new OvalShape());
		Paint paint = shape.getPaint();
		paint.setColor(color);
		paint.setStyle(Style.FILL);
		paint.setAntiAlias(true);
		shape.setIntrinsicHeight(diameter);
		shape.setIntrinsicWidth(diameter);
		return shape;
	}

	public static LayerDrawable makeDoubleCircle(int colorouter, int colorinner, int diameterouter, int diameterinner)
	{
		//ShapeDrawable shape = new ShapeDrawable(new OvalShape());
		LayerDrawable result = new LayerDrawable(new Drawable[]{makeCircle(colorouter,diameterouter), makeCircle(colorinner,diameterinner)});
		//(makeCircle(colorouter, diameterouter).g);
		if(Build.VERSION.SDK_INT>=23)
			result.setLayerGravity(result.getNumberOfLayers()-1, Gravity.CENTER);
		else
			result.setLayerInset(result.getNumberOfLayers()-1, (diameterouter-diameterinner)/2,  (diameterouter-diameterinner)/2, (diameterouter-diameterinner)/2, (diameterouter-diameterinner)/2);
		return result;
	}
	
	/* Set background drawable based on the API */
	@SuppressWarnings("deprecation")
	public static void setBackgroundDrawable(View view, Drawable drawable) {
		if (Build.VERSION.SDK_INT >= 16) {
			view.setBackground(drawable);
		} else {
			view.setBackgroundDrawable(drawable);
		}
	}
	
	public static int getDisplayRotation(Context mActivity) {
        Display display = ((WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        return display.getRotation();
    }
	
	
	public static int getScreenOrientation(Context mActivity)
	{
		Point screenSize = new Point();
		((WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(screenSize);
		int orientation;
		if(screenSize.x < screenSize.y){
			orientation = Configuration.ORIENTATION_PORTRAIT;
		}else {
			orientation = Configuration.ORIENTATION_LANDSCAPE;
		}
		return orientation;
	}

	public static boolean isFlag(int flagHolder, int flag){
		return ((flagHolder & flag) == flag);
	}
	
	public static void addPrivateFlagNoMoveAnimationToLayoutParam(WindowManager.LayoutParams params) {
		if (Build.VERSION.SDK_INT <= 15) return;

		try {
			Field fieldPrivateFlag = XposedHelpers.findField(WindowManager.LayoutParams.class, "privateFlags");
			fieldPrivateFlag.setInt(params, (fieldPrivateFlag.getInt(params) | 0x00000040));
		} catch (Exception e) {
			//Just pass
		}
	}
	/* this private flag is only in JB and above to turn off move animation.
	 * we need this to speed up our resizing */
	// params.privateFlags |= 0x00000040; //PRIVATE_FLAG_NO_MOVE_ANIMATION
	
	public static String getFailsafeStringFromObject(String result, Object mObject, String mItem){
		if(mObject==null) return null;
		try{
			result = (String) XposedHelpers.getObjectField(mObject, mItem);
		} catch (Throwable t){
			XposedBridge.log(t);
			return null;
		}
		return result;
	}
	
	public static Object getFailsafeObjectFromObject(Object mObject, String mItem){
		if(mObject==null) return null;
		Object result = null;
		try{
			result = XposedHelpers.getObjectField(mObject, mItem);
		} catch (Throwable t){
			XposedBridge.log(t);
			return null;
		}
		return result;
	}
	
	public static Integer getFailsafeIntFromObject(Object mObject, String mItem){
		if(mObject==null) return null;
		Integer result = null;
		try{
			result = (Integer) XposedHelpers.getIntField(mObject, mItem);
		} catch (Throwable t){
			XposedBridge.log(t);
			return null;
		}
		return result;
	}
	
	
	public static boolean getFailsafeObjectFromMethod(Object result, Object mObject, String mItem){
		if(mObject==null) return false;
		try{
			result = XposedHelpers.callMethod(mObject, mItem);
		} catch (Throwable t){
			XposedBridge.log(t);
			return false;
		}
		return (result!=null);
		}
	
	public static int getStatusBarHeight(Context mContext){
		int result = 0;
		int resourceId = mContext.getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0)
			result = mContext.getResources().getDimensionPixelSize(resourceId);
		return result;
	}
	
	public static boolean moveToFront(Context mContext, int taskId){
		ActivityManager mActivityManager = (ActivityManager) mContext
			.getSystemService(Context.ACTIVITY_SERVICE);
		try {
			mActivityManager.moveTaskToFront(taskId, ActivityManager.MOVE_TASK_NO_USER_ACTION);
		} catch (Exception e) {
			Log.e("Xposed", "Cannot move task to front", e);
			return false;
		}
		return true;
	}
	
	public static void startApp(Context mContext, String mPackageName){
		if(mPackageName==null||mContext==null)
			return;
		final Intent intent;
		try{
		intent = new Intent(mContext.getApplicationContext().getPackageManager()
										  .getLaunchIntentForPackage(mPackageName));
			} catch (Throwable t){
				Log.e("Xposed", "startApp failed for package: " + mPackageName);
//				intent = new Intent(Intent.ACTION_MAIN);
//				//intent.setComponent(new ComponentName("com.package.address","com.package.address.MainActivity"));
//				intent.setPackage(mPackageName);
//				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				return;
			}
		if(intent==null) return;
		SharedPreferences mSPrefs = mContext.getSharedPreferences(Common.PREFERENCE_MAIN_FILE, mContext.MODE_WORLD_READABLE);
		int floatFlag = mSPrefs.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		intent.addFlags(floatFlag | Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}
	
	
	public static void startAppNormal(Context mContext, String mPackageName){
		if(mPackageName==null||mContext==null)
			return;
		final Intent intent;
		try{
			intent = new Intent(mContext.getApplicationContext().getPackageManager()
								.getLaunchIntentForPackage(mPackageName));
		} catch (Throwable t){
			Log.e("Xposed", "startApp failed for package: " + mPackageName);
//				intent = new Intent(Intent.ACTION_MAIN);
//				//intent.setComponent(new ComponentName("com.package.address","com.package.address.MainActivity"));
//				intent.setPackage(mPackageName);
//				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			return;
		}
		if(intent==null) return;
		//SharedPreferences mSPrefs = mContext.getSharedPreferences(Common.PREFERENCE_MAIN_FILE, mContext.MODE_WORLD_READABLE);
		//int floatFlag = mSPrefs.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(intent);
	}
	public static Point getScreenSize(Context mContext){
		final WindowManager mWindowManager = (WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(metrics);
		return new Point(metrics.widthPixels, metrics.heightPixels);
	}

	public static void finishApp(String packageName)
	{
		Process suProcess;
		try
		{
			suProcess = Runtime.getRuntime().exec("su");
		} catch (IOException e)
		{return;}
		DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());

		try
		{
			os.writeBytes("adb shell" + "\n");
			os.flush();

			os.writeBytes("am force-stop " + packageName + "\n");
			os.flush();
		} catch (IOException e)
		{}

		
//		am.killBackgroundProcesses(packageName);
//		am.get
	}
	
	public static String getTopAppPackageName(Context mContext){
		if(Build.VERSION.SDK_INT>20&&!isUsageAccessGranted(mContext))
		{
			Intent mIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
			mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.getApplicationContext().startActivity(mIntent);
			return null;
		}
		/* returns null if fails */
		String packageName = null;
		try{
			if(Build.VERSION.SDK_INT<21){
				ActivityManager am = (ActivityManager) mContext.getApplicationContext().getSystemService(Activity.ACTIVITY_SERVICE);
				packageName = am.getRunningTasks(1).get(0).topActivity.getPackageName();
			}
			else {
				UsageStatsManager usm = (UsageStatsManager) mContext.getApplicationContext().getSystemService(Context.USAGE_STATS_SERVICE);
				long endTime = System.currentTimeMillis();
				long beginTime = endTime - 1000 * 60 * 5;
				List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime);
				if(stats!=null){
					SortedMap<Long, UsageStats> sMap = new TreeMap<Long, UsageStats>();
					for(UsageStats stat : stats) {
						sMap.put(stat.getLastTimeUsed(), stat);
					}
					if(sMap != null && !sMap.isEmpty()){
						packageName = sMap.get(sMap.lastKey()).getPackageName();
						//packageName1 = sMap.get(sMap.lastKey()-1).getPackageName();
					}
				}
			}
		} catch (Throwable t){
			Log.e("Xposed", "XHFW unable to get top app packageName", t);
		}
		
		return packageName;
		}
	
	public static void  restartTopAppAsFloating(Context mContext, int FloatFlag){
			String packageName = getTopAppPackageName(mContext);
			restartAppAsFloating(mContext, FloatFlag, packageName);
	}
	
	public static void restartAppAsFloating(final Context mContext, final int FloatFlag, final String packageName){
		if(packageName==null) return;
		try{
			Intent intent = mContext.getApplicationContext().getPackageManager().getLaunchIntentForPackage(packageName);
			intent.addFlags(FloatFlag);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			//intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory("restarted");
			mContext.getApplicationContext().startActivity(intent);
		} catch (Throwable t){
			Log.e("Xposed", "restartTopAppAsFloating failed", t);
		}
	}
	
	public static void  restartTopAppAsFullScreen(Context mContext, int FloatFlag){
		String packageName = getTopAppPackageName(mContext);
		restartAppAsFullScreen(mContext, FloatFlag, packageName);
		}
		
	public static void restartAppAsFullScreen(final Context mContext, final int FloatFlag, final String packageName){
		if(packageName==null) return;
		try{
			
//			Intent startMain = new Intent(Intent.ACTION_MAIN);
//			startMain.addCategory(Intent.CATEGORY_HOME);
//			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
//			mContext.getApplicationContext().startActivity(startMain);
//			
			Intent mIntent = new Intent(Common.RESTART_ACTIVITY);
			mIntent.setPackage(packageName);
			mContext.getApplicationContext().sendBroadcast(mIntent);
			
			final Intent intent = mContext.getApplicationContext().getPackageManager().getLaunchIntentForPackage(packageName);
			
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		//	intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);
//			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
//			intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
//			intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.addCategory("restarted");
			mContext.getApplicationContext().startActivity(intent);
			
			/* need to repeat start for Hangouts and similar */
			//mContext.getApplicationContext().startActivity(intent);
			//startAppNormal(mContext, packageName);
				
			
			
		} catch (Throwable t){
			Log.e("Xposed", "restartTopAppAsFloating failed", t);
		}
	}
	
	public static boolean isUsageAccessGranted(Context mContext){
		AppOpsManager appOps = (AppOpsManager) mContext.getSystemService(Context.APP_OPS_SERVICE);
		int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), Common.THIS_MOD_PACKAGE_NAME);
		return mode == AppOpsManager.MODE_ALLOWED;
	}
}
