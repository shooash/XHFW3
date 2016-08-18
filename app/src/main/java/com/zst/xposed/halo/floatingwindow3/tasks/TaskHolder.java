package com.zst.xposed.halo.floatingwindow3.tasks;
import android.app.*;
import java.util.*;
import com.zst.xposed.halo.floatingwindow3.Util;
import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.Compatibility;
import de.robv.android.xposed.*;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.debug.*;
import android.widget.*;
import com.zst.xposed.halo.floatingwindow3.overlays.*;
import android.content.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.graphics.*;

public class TaskHolder
{
	//final Activity mActivity;
	final Context appContext;
	Map<String, ArrayList<WindowHolder>> activitiesStack = new HashMap<>();
	//ArrayList<WindowHolder> windowsStack = new ArrayList<>();
	int taskId;
	WindowHolder defaultLayout = new WindowHolder();
	WindowHolder cachedLayout = new WindowHolder();
	int cachedOrientation;
	String packageName;
	//MovableOverlayView mMovableOverlay = null;
	OverlayView mOverlay = null;
	boolean isSnapped;
	boolean isMaximized;
	
	public TaskHolder(final Activity sActivity, final WindowHolder mDefaultLayout, final WindowHolder mCachedLayout) {
	
		MainXposed.mPref.reload();
		cachedOrientation=Util.getScreenOrientation(sActivity.getApplicationContext());	
		packageName = sActivity.getCallingPackage();
		appContext = sActivity.getApplicationContext();
		//mActivity = sActivity;
		if(packageName==null)
			packageName = sActivity.getPackageName();
//		int flags = mActivity.getIntent().getFlags();
//		isHiddenFromRecents = Util.isFlag(flags, Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//			|| Util.isFlag(flags, Intent.FLAG_ACTIVITY_NO_HISTORY);
		if(mDefaultLayout==null)
			getDefaults(MainXposed.mPref, sActivity);
		else
			defaultLayout.copy(mDefaultLayout);
		//setOverlayView(sActivity.getWindow());
		isSnapped = defaultLayout.isSnapped;
		isMaximized = defaultLayout.isMaximized;
		if(mCachedLayout!=null)
			cachedLayout.copy(mCachedLayout);
		//mActivity.getComponentName().getClassName();
	}

	public void broadcastResizable(boolean show, int x, int y, boolean left)
	{
		if(!show||activitiesStack.isEmpty()) {
			broadcastResizable(false, 0,0,0,0);
			return;
		}
		if(left)
			broadcastResizable(true, defaultLayout.x+x, defaultLayout.y, defaultLayout.width-x, defaultLayout.height+y);
		else
			broadcastResizable(true, defaultLayout.x, defaultLayout.y, x, y);
	}
	
	
	private void broadcastResizable(boolean show, int x, int y, int w, int h) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		if (show) {
			int[] array = { x, y, w, h };
			i.putExtra(Common.INTENT_APP_PARAMS, array);
		}
		Util.sendBroadcastSafe(i, appContext);
	}

	public WindowHolder getLastOrDefaultWindow()
	{
		return defaultLayout;
	}

	public void updateByFloatDot(int x, int y, int p2, int p3)
	{
		boolean changed = defaultLayout.updateByFloatDot(x, y, p2, p3);
		if(changed) {
			syncAllWindowsAsWindow(defaultLayout);
			InterActivity.resetFocusFrameIfNeeded(appContext,
							defaultLayout.x,
							defaultLayout.y,
							defaultLayout.width,
							defaultLayout.height,
							taskId);
			}
	}

	public void getDefaults(final XSharedPreferences mPref, final Activity mActivity){
		defaultLayout.alpha = mPref.getFloat(Common.KEY_ALPHA, Common.DEFAULT_ALPHA);
        defaultLayout.dim = mPref.getFloat(Common.KEY_DIM, Common.DEFAULT_DIM);
		//defaultLayout.SnapGravity = Compatibility.snapSideToGravity(mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE, Compatibility.AeroSnap.SNAP_NONE));
		//defaultLayout.isSnapped=(defaultLayout.SnapGravity != 0);
        //defaultLayout.isMaximized=(defaultLayout.SnapGravity == Gravity.FILL);
	}
	
	public void resume(final Activity mActivity, final Window mWindow ) {
		syncAllWindows(mActivity.getComponentName().getClassName());
		setOverlayView(mWindow);
		if(isSnapped)
			mOverlay.setWindowBorderFocused();
//		if(mOverlay!=null)
//			mOverlay.setTitleBarVisibility(!isSnapped&!isMaximized);
	}
	
	public void focus(final Window mWindow) {
		setOverlayView(mWindow);
		if(isSnapped)
			mOverlay.setWindowBorderFocused();
	}
	
	public void unfocus(final Window mWindow) {
		setOverlayView(mWindow);
		if(isSnapped)
			mOverlay.setWindowBorder();
	}
	
	public boolean addWindow(final Window mWindow, final Activity mActivity) {
		String activityClass = mActivity==null?"":mActivity.getComponentName().getClassName();
		return addWindow(mWindow, activityClass);
		}
		
	public boolean addWindow(final Window mWindow, final String activityClass) {
		ArrayList<WindowHolder> windowsStack;
		if(mWindow==null ||mWindow.isFloating() || isWindowRegistered(mWindow, activityClass))
			return false;
		if(activitiesStack.containsKey(activityClass))
			windowsStack = activitiesStack.get(activityClass);
		else
			windowsStack = addActivity(activityClass);
		WindowHolder mWindowHolder = new WindowHolder(mWindow, defaultLayout);
		mWindowHolder.pushToWindow();
		windowsStack.add(mWindowHolder);
		setOverlayView(mWindow);
//		if(mOverlay!=null)
//			mOverlay.setTitleBarVisibility(!isSnapped&!isMaximized);
		//putOverlayView(mWindow);
		
		//TODO TEST MOVE TO NEW WINDOW
		return true;
	}
	
	public boolean removeActivity(final Activity mActivity) {
		activitiesStack.remove(mActivity.getComponentName().getClassName());
		removeOverlayView(mActivity.getWindow());
		return activitiesStack.isEmpty();
	}
	
	public ArrayList<WindowHolder> addActivity(final String mActivityClass) {
		Debugger.DEBUG("addActivity " + mActivityClass);
		final ArrayList<WindowHolder> windowsStack = new ArrayList<>();
		activitiesStack.put(mActivityClass, windowsStack);
		return windowsStack;
	}
	
	public boolean isWindowRegistered(final Window mWindow) {
		for(ArrayList<WindowHolder> mWindowHolderList : activitiesStack.values()) {
			for(WindowHolder mWindowHolder : mWindowHolderList) {
				//if(mWindowHolder.window == mWindow)
				if(mWindowHolder.window.hashCode() == mWindow.hashCode())
					return true;
			}
		}
		return false;
	}
	public boolean isWindowRegistered(final Window mWindow, final String activityClass) {
		if(!activitiesStack.containsKey(activityClass))
			return false;
		for(WindowHolder mWindowHolder : activitiesStack.get(activityClass)) {
			//TODO use hashCode()?
			//if(mWindowHolder.window == mWindow)
			if(mWindowHolder.window.hashCode() == mWindow.hashCode())
				return true;
		}
		return false;
	}
	
//	public int getWindowIndex(final Window mWindow) {
//		
//		return -1;
//	}
	
	public void syncAllWindows() {
		for(String activityClass : activitiesStack.keySet()) {
			syncAllWindows(activityClass);
		}
	}
	
	public void syncAllWindows(final String activityClass) {
		for(WindowHolder mWindowHolder : activitiesStack.get(activityClass)) {
			mWindowHolder.pushToWindow();
		}
	}
	
	public void syncAllWindowsAsWindow(final Window mWindow) {
		WindowHolder copyLayout = new WindowHolder(mWindow);
		syncAllWindowsAsWindow(copyLayout);
	}
	
	public void syncAllWindowsAsWindow(final WindowHolder copyLayout) {
		for(String activityClass : activitiesStack.keySet()) {
			syncAllWindowsAsWindow(copyLayout, activityClass);
		}
		defaultLayout.copy(copyLayout);
	}
	
	public void syncAllWindowsAsWindow(final WindowHolder copyLayout, final String activityClass) {
		for(WindowHolder mWindowHolder : activitiesStack.get(activityClass)) {
			mWindowHolder.copy(copyLayout);
			mWindowHolder.pushToWindow();
			if(mOverlay!=null)
				mOverlay.setTitleBarVisibility(!isSnapped&&!isMaximized, mWindowHolder.window);
		}
	}
	
	public void move(int x, int y) {
		if(isSnapped || isMaximized)
			return;
		for(String activityClass : activitiesStack.keySet()) {
			move(x, y, activityClass);
		}
		defaultLayout.position(x, y);
	}
	
	public void move(int x, int y, final String activityClass) {
		for(WindowHolder mWindowHolder : activitiesStack.get(activityClass)) {
			if(mWindowHolder!=null) {
				mWindowHolder.position(x, y);
				mWindowHolder.pushToWindow();
			}
		}
	}
	
	public void resize(int deltax, int deltay, int offset) {
		if(isSnapped || isMaximized)
			return;
		defaultLayout.resize(deltax, deltay, appContext);
		defaultLayout.position(defaultLayout.x+offset, defaultLayout.y);
		syncAllWindowsAsWindow(defaultLayout);
	}
	
//	public void resize(int deltax, int deltay, int offset, final String activityClass) {
//		if(!activitiesStack.containsKey(activityClass))
//			return;
////		if(deltax>0&&deltay>0) {
////			resize_backwards(deltax, deltay, offset, activityClass);
////			return;
////		}
//		
//		for(WindowHolder mWindowHolder : activitiesStack.get(activityClass)) {
//			if(mWindowHolder!=null) {
//				mWindowHolder.resize(deltax, deltay, appContext);
//				mWindowHolder.position(mWindowHolder.x+offset, mWindowHolder.y);
//				mWindowHolder.pushToWindow();
//			}
//		}
//		if(!activitiesStack.get(activityClass).isEmpty()) 
//			defaultLayout.copy(activitiesStack.get(activityClass).get(0));
//	}
//	
//	private void resize_backwards(int deltax, int deltay, int offset, final String activityClass) {
//		if(!activitiesStack.containsKey(activityClass))
//			return;
//		ArrayList<WindowHolder> windowsStack = activitiesStack.get(activityClass);
//		for(int i = windowsStack.size()-1; i >= 0; i--) {
//			WindowHolder mWindowHolder = windowsStack.get(i);
//			if(mWindowHolder!=null) {
//				mWindowHolder.resize(deltax, deltay, appContext);
//				mWindowHolder.position(mWindowHolder.x+offset, mWindowHolder.y);
//				mWindowHolder.pushToWindow();
//			}
//		}
//		if(!windowsStack.isEmpty()) 
//			defaultLayout.copy(windowsStack.get(0));
//	}
	
	public void snap(int x, int y, int w, int h, int snapgravity) {
		WindowHolder mWindowHolder = new WindowHolder(null, x, y, w, h);
		mWindowHolder.SnapGravity = snapgravity;
		mWindowHolder.isSnapped = true;
		snap(mWindowHolder);
	}
	
	public void snap(final WindowHolder mWindowHolder) {
		saveLayout();
		syncAllWindowsAsWindow(mWindowHolder);
		isSnapped = true;
		if(mOverlay!= null && mWindowHolder.window!=null) {
			mOverlay.setWindow(mWindowHolder.window);
			mOverlay.setTitleBarVisibility(false);
		}
		else
			setOverlayView(ActivityHooks.mCurrentActivity.getWindow());
		mOverlay.setWindowBorderFocused();
	}
	
	public void maximize() {
		for(String activityClass : activitiesStack.keySet()) {
			maximize(activityClass);
		}
		defaultLayout.setMaximized();
		isMaximized = true;
		if(mOverlay!= null) 
			mOverlay.setTitleBarVisibility(false);
		else
			setOverlayView(ActivityHooks.mCurrentActivity.getWindow());
		mOverlay.setWindowBorder();
	}
	
	public void maximize(final String activityClass) {
		saveLayout();
		for(WindowHolder mWindowHolder : activitiesStack.get(activityClass)) {
			if(mWindowHolder!=null) {
				mWindowHolder.setMaximized();
				mWindowHolder.pushToWindow();
			}
		}
	}
	
	public void saveLayout() {
		if(defaultLayout.isSnapped || defaultLayout.isMaximized)
			return;
		cachedLayout.copy(defaultLayout);
	}
	
	public void restore() {
		syncAllWindowsAsWindow(cachedLayout);
		isSnapped = false;
		isMaximized = false;
		if(mOverlay!=null) {
			mOverlay.setWindow(ActivityHooks.mCurrentActivity.getWindow());
			mOverlay.setTitleBarVisibility(true);
		}
		else
			{
				setOverlayView(ActivityHooks.mCurrentActivity.getWindow());
			}
		mOverlay.setWindowBorder();
	}
	
	public void setOverlayView(final Window mWindow){
		if(mWindow==null)
			return;
		FrameLayout decorView;
		try{
			decorView = (FrameLayout) mWindow.peekDecorView().getRootView();
		} catch(Throwable t){
			decorView=null;
		}
		if (decorView == null) return;
		// make sure the titlebar/drag-to-move-bar is not behind the statusbar
		decorView.setFitsSystemWindows(true);
		try {
			// disable resizing animation to speed up scaling (doesn't work on all roms)
			XposedHelpers.callMethod(decorView, "hackTurnOffWindowResizeAnim", true);
		} catch (Throwable e) {
		}
		Debugger.DEBUG("setOverlayView");

		//if(mOverlay==null)
		mOverlay = (OverlayView) decorView.getTag(Common.LAYOUT_OVERLAY_TAG);
//		for (int i = 0; i < decorView.getChildCount(); ++i) {
//			final View child = decorView.getChildAt(i);
//			if (child instanceof OverlayView && mOverlay != child) {
//				// If our tag is different or null, then the
//				// view we found should be removed by now.
//				decorView.removeView(decorView.getChildAt(i));
//				break;
//			}
//		}
		if(mOverlay == null) {
			mOverlay = new OverlayView(appContext, mWindow);
			decorView.addView(mOverlay, -1, mOverlay.getParams());
			setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG,  mOverlay);
//			if(isSnapped||isMaximized)
//				mOverlay.setTitleBarVisibility(false);
		}
		decorView.bringChildToFront(mOverlay);
		mOverlay.setWindow(mWindow);
		if(isSnapped||isMaximized)
			mOverlay.setTitleBarVisibility(false);
		else
			mOverlay.setTitleBarVisibility(true);
	}
	
	public void removeOverlayView(final Window mWindow) {
		mOverlay = null;
		if(mWindow==null)
			return;
		FrameLayout decorView;
		try{
			decorView = (FrameLayout) mWindow.peekDecorView().getRootView();
		} catch(Throwable t){
			decorView=null;
		}
		if (decorView == null) return;
		for (int i = 0; i < decorView.getChildCount(); ++i) {
			final View child = decorView.getChildAt(i);
			if (child instanceof OverlayView) {
				// If our tag is different or null, then the
				// view we found should be removed by now.
				decorView.removeView(decorView.getChildAt(i));
				break;
			}
		}
		setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG,  null);
	}
//	public void putOverlayView(final Window mWindow){
//		Debugger.DEBUG("putOverlayView");
//		FrameLayout decor_view;
//		try{
//			decor_view = (FrameLayout) mWindow.peekDecorView().getRootView();
//		} catch(Throwable t){
//			decor_view=null;
//		}
//		if (decor_view == null) return;
//		if(mMovableOverlay==null)
//			mMovableOverlay = (MovableOverlayView) decor_view.getTag(Common.LAYOUT_OVERLAY_TAG);
//		if (mMovableOverlay != null)  decor_view.bringChildToFront(mMovableOverlay);
//		else
//			setOverlayView(mWindow);
//	}
	
	private static void setTagInternalForView(View view, int key, Object object) {
		Class<?>[] classes = { Integer.class, Object.class };
		XposedHelpers.callMethod(view, "setTagInternal", classes, key, object);
	}
	
	public String findWindowActivityId(final Window mWindow) {
		for(String id : activitiesStack.keySet()) {
			if(isWindowRegistered(mWindow, id))
				return id;
		}
		return null;
	}
}
