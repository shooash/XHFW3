package com.zst.xposed.halo.floatingwindow3.tasks;
import android.app.*;
import java.util.*;
import com.zst.xposed.halo.floatingwindow3.Util;
import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.Compatibility;
import de.robv.android.xposed.*;
import android.view.*;

public class TaskHolder
{
	Activity mActivity;
	ArrayList<WindowHolder> windowsStack = new ArrayList<>();
	int taskId;
	WindowHolder defaultLayout = new WindowHolder();
	int cachedOrientation;
	String packageName;
	
	public TaskHolder(final Activity sActivity, final WindowHolder mDefaultLayout) {
	
		MainXposed.mPref.reload();
		cachedOrientation=Util.getScreenOrientation(sActivity.getApplicationContext());	
		packageName = sActivity.getCallingPackage();
		if(packageName==null)
			packageName = sActivity.getPackageName();
//		int flags = mActivity.getIntent().getFlags();
//		isHiddenFromRecents = Util.isFlag(flags, Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//			|| Util.isFlag(flags, Intent.FLAG_ACTIVITY_NO_HISTORY);
		if(mDefaultLayout==null)
			getDefaults(MainXposed.mPref);
		else
			defaultLayout.copy(mDefaultLayout);
	}

	public WindowHolder getLastOrDefaultWindow()
	{
		if(windowsStack.isEmpty())
			return defaultLayout;
		else
			return windowsStack.get(windowsStack.size()-1);
	}

	public void updateByFloatDot(int x, int y, int p2, int p3)
	{
		for(WindowHolder mWindowHolder : windowsStack) {
			if(mWindowHolder!=null) mWindowHolder.updateByFloatDot(x, y, p2, p3);
			}
	}

	public void getDefaults(final XSharedPreferences mPref){
		defaultLayout.alpha = mPref.getFloat(Common.KEY_ALPHA, Common.DEFAULT_ALPHA);
        defaultLayout.dim = mPref.getFloat(Common.KEY_DIM, Common.DEFAULT_DIM);
		defaultLayout.SnapGravity = Compatibility.snapSideToGravity(mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE, Compatibility.AeroSnap.SNAP_NONE));
		defaultLayout.isSnapped=(defaultLayout.SnapGravity != 0);
        defaultLayout.isMaximized=(defaultLayout.SnapGravity == Gravity.FILL);
	}
	
	public boolean addWindow(final Window mWindow) {
		if(mWindow==null ||mWindow.isFloating() || isWindowRegistered(mWindow))
			return false;
		WindowHolder mWindowHolder = new WindowHolder(mWindow, defaultLayout);
		mWindowHolder.pushToWindow();
		windowsStack.add(mWindowHolder);
		return true;
	}
	
	public boolean isWindowRegistered(final Window mWindow) {
		for(WindowHolder mWindowHolder : windowsStack) {
			//TODO use hashCode()?
			if(mWindowHolder.window == mWindow)
				return true;
		}
		return false;
	}
	
//	public int getWindowIndex(final Window mWindow) {
//		
//		return -1;
//	}
	
	public void syncAllWindows() {
		for(WindowHolder mWindowHolder : windowsStack) {
			mWindowHolder.pushToWindow();
		}
	}
	
	public void syncAllWindowsAsWindow(final Window mWindow) {
		WindowHolder copyLayout = new WindowHolder(mWindow);
		syncAllWindowsAsWindow(copyLayout);
	}
	
	public void syncAllWindowsAsWindow(final WindowHolder copyLayout) {
		for(WindowHolder mWindowHolder : windowsStack) {
			mWindowHolder.copy(copyLayout);
			mWindowHolder.pushToWindow();
		}
	}
	
	public void move(int x, int y) {
		for(WindowHolder mWindowHolder : windowsStack) {
			if(mWindowHolder!=null) {
				mWindowHolder.position(x, y);
				mWindowHolder.pushToWindow();
				}
		}
	}
}
