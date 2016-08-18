package com.zst.xposed.halo.floatingwindow3.debug;
import com.zst.xposed.halo.floatingwindow3.*;
import java.util.*;
import android.os.*;
import de.robv.android.xposed.*;
import android.util.*;

public class Debugger
{
	public static boolean DEBUG_SWITCH = false;
	public static String packageName = new String();
	public static String packageId = new String();
	public static String currentInstance = new String();
	public static String currentInstanceId = new String();
	private static boolean initialized = false;
	private static boolean systemContext = true;
	public static String TAG = "Xposed";
	private static Map<String, String> packageNamesMap = new HashMap<>();
	
	public static void DEBUG_E(final String sTag) {
		ArrayList<String> msg = new ArrayList<String>();
		String header;
		if(!initialized)
			setupDebug();
		header = "XHFW:" + currentInstanceId + ":" + packageName;
		msg.add(header + " " + sTag);
		if(ActivityHooks.taskStack!=null){
			msg.add(header + "        " + DEBUG_MovableWindow_1());
			msg.add(header + "        " + DEBUG_MovableWindow_2());
		}
		for(String s : msg){
			log(s);
		}
	}
	public static void DEBUG(final String sTag){
		if(!DEBUG_SWITCH) return;
		ArrayList<String> msg = new ArrayList<String>();
		String header;
		if(!initialized)
			setupDebug();
		header = "XHFW:" + currentInstanceId + ":" + packageName;
		msg.add(header + " " + sTag);
		if(ActivityHooks.taskStack!=null){
			msg.add(header + "        " + DEBUG_MovableWindow_1());
			msg.add(header + "        " + DEBUG_MovableWindow_2());
		}
		for(String s : msg){
			log(s);
		}
	}
	
	public static void DEBUG(final String sTag, final String pkg){
		if(!DEBUG_SWITCH) return;
		setPackageName(pkg);
		DEBUG(sTag);
	}
	
	private static String DEBUG_MovableWindow(){
		return "MovableWindow: " + (ActivityHooks.isMovable?"isMovable, ":"") + 
			(ActivityHooks.taskStack.defaultLayout!=null?
			"x:y [" + ActivityHooks.taskStack.defaultLayout.x + ":" + ActivityHooks.taskStack.defaultLayout.y + "] " +
		"size [" + ActivityHooks.taskStack.defaultLayout.width + ":" + ActivityHooks.taskStack.defaultLayout.height + "] ":"no layout");
	}
	
	private static String DEBUG_MovableWindow_1(){
		return "MovableWindow: " + (ActivityHooks.isMovable?"isMovable, ":"") + 
			(ActivityHooks.taskStack.defaultLayout!=null?
			"x:y [" + ActivityHooks.taskStack.defaultLayout.x + ":" + ActivityHooks.taskStack.defaultLayout.y + "] " +
			"size [" + ActivityHooks.taskStack.defaultLayout.width + ":" + ActivityHooks.taskStack.defaultLayout.height + "] ":"no layout");
	}
	
	private static String DEBUG_MovableWindow_2(){
		return "";
	}
	
	private static void log(String text){
		if(systemContext)
			XposedBridge.log(text);
		else
			Log.d(TAG, text);
	}
	
	private static void logSystemContext(String text){
		XposedBridge.log(text);
	}
	
	private static void logUserContext(String text){
		Log.d(TAG, text);
	}
	
	public static void setupDebug(){
		if(!DEBUG_SWITCH) return;
		currentInstance = MainXposed.mPackageName;
		if(currentInstance.equals("user")){
			systemContext = false;
			return;
		}	
		currentInstanceId = getNewInstanceId();
		log("XHFW debug instance " + currentInstance + ":" + currentInstanceId);
		if(!(currentInstance.equals("android")||currentInstance.startsWith("com.android.systemui")))
			packageName = currentInstance;
			packageId = currentInstanceId;
		initialized = true;
	}
	
	private static void setPackageName(String pkg){
		packageName = pkg;
		if(!packageNamesMap.containsKey(packageName)){
			packageId=getNewPackageId();
			packageNamesMap.put(packageName, packageId);
			log("XHFW debug package " + packageName + " " + currentInstanceId + ":" + packageId);
		}
		else
			packageId = packageNamesMap.get(packageName);
		//log("XHFW debug package " + packageName + " " + currentInstanceId + ":" + packageId);
	}
	
	private static String getNewInstanceId(){
		int pid = android.system.Os.getpid();
		return "PID" + pid;
	}
	
	private static String getNewPackageId(){
		long t = SystemClock.uptimeMillis();
		return Long.toHexString(t);
	}
}
