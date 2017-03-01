package com.zst.xposed.halo.floatingwindow3.debug;
import android.util.*;
import java.io.*;

public class GetLogs
{
	public static boolean suRequested = false;
	
	public static boolean requestSu() {
		Log.d("XHFW3", "Tequesting SU root access");
		Process suProcess = null;
		try {
			suProcess = Runtime.getRuntime().exec("su -c ls");
			suProcess.waitFor();
			if(suProcess.exitValue()==0)
				suRequested = true;
		} catch (Throwable t) {
			suRequested = false;
		}
		return suRequested;
	}
	
	public static boolean saveLogXposed(String path) {
		String cmd = "su -c \"logcat -d -v time | grep -i xposed > '" + path + "' \"";
		try {
			runSuCommand(cmd);
				return true;
		} catch (Throwable t) {

		}
		return false;
	}
	
	public static boolean saveLogErrors(String path) {
		String cmd = "su -c \"logcat -d -v time -t 100 *:E > " + path + " \"";
		try {
			runSuCommand(cmd);
				return true;
		} catch (Throwable t) {

		}
		return false;
	}
	
	public static boolean makeDir(String path) {
		String cmd = "su -c 'mkdir " + path + "'";
		try {
			runSuCommand(cmd);
//			Process suProcess = Runtime.getRuntime().exec(cmd);
//			suProcess.waitFor();
//			if(suProcess.exitValue()==0)
				return true;
		} catch (Throwable t) {

		}
		return false;
	}
	
	public static void runSuCommand(String cmd) throws IOException, InterruptedException {
		Process p = Runtime.getRuntime().exec("su");
		DataOutputStream dos = new DataOutputStream(p.getOutputStream());
		dos.writeBytes(cmd+"\n");
		dos.writeBytes("exit\n");
		dos.flush();
		dos.close();
		p.waitFor();
	}
	
	public static boolean saveLogAll() {
//		if(!suRequested&&!requestSu())
//			return false;
		String path = "/sdcard/XHFWlogs";
		makeDir(path);
		return saveLogXposed(path+"/Xposed.log")&saveLogErrors(path + "/Errors.log");
	}
}
