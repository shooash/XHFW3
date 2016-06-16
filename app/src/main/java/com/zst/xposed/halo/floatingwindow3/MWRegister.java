package com.zst.xposed.halo.floatingwindow3;
import java.util.*;
import android.content.*;
import de.robv.android.xposed.*;

public class MWRegister
{
	private static Map<String, Boolean> registered = new HashMap<>();
	private static Map<String, ArrayList<Integer>> tasksStack = new HashMap<>();
	
	public static boolean addPackage(final String packageName, final boolean movable){
		if(registered.containsKey(packageName))
			return registered.get(packageName);
		return forceAddPackage(packageName, movable);
	}
	public static boolean forceAddPackage(final String packageName, final boolean movable){
		registered.put(packageName, movable);
		//XposedBridge.log("MWRegister added package " + packageName + " stack size is " + registered.size());
		return movable;
	}
	
	public static boolean addPackageAutoAssign(final String packageName, final Intent mIntent){
		if(registered.containsKey(packageName))
			return registered.get(packageName);
		return forceAddPackageAutoAssign(packageName, mIntent);
	}
	
	public static boolean forceAddPackageAutoAssign(final String packageName, final Intent mIntent){
		boolean movable = Util.isFlag(mIntent.getFlags(), MainXposed.mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW));
		registered.put(packageName, movable);
		return movable;
	}
	
	public static boolean removePackage(final String packageName){
		if(!registered.containsKey(packageName))
			return false;
		boolean movable = registered.get(packageName);
		registered.remove(packageName);
		//tasksStack.remove(packageName);
		return movable;
	}
	
	public static boolean isRegistered(final String packageName){
		return registered.containsKey(packageName);
	}
	
	public static boolean isMovable(final String packageName){
		if(!isRegistered(packageName)) {
			XposedBridge.log("ERROR MWRegister.isMovable - package " + packageName + " is not registered");
			return false;
			}
		return registered.get(packageName);
	}
	
	/* returns: package is known? */
	public static boolean addTask(final String packagename, final Integer taskId){
		ArrayList<Integer> tasks = tasksStack.get(packagename);
		if(tasks!=null){
			tasks.add(taskId);
			tasksStack.put(packagename, tasks);
		} else
			createTaskStack(packagename, taskId);
		return isRegistered(packagename);
	}
	
	/* returns: stack stack still exists? */
	public static boolean removeTask(final String packageName, final Integer taskId){
		ArrayList<Integer> tasks = tasksStack.get(packageName);
		int index;
		if(tasks==null)
			return false;
		index = tasks.indexOf(taskId);
		if(index != -1)
			tasks.remove(index);
		if(tasks.isEmpty()){
			tasksStack.remove(packageName);
			return false;
		}
		tasksStack.put(packageName, tasks);
		return true;
	}
	
	public static void createTaskStack(final String packageName, final Integer... taskId){
		ArrayList<Integer> tasks = new ArrayList<Integer>(Arrays.asList(taskId));
		tasksStack.put(packageName, tasks);
	}
}
