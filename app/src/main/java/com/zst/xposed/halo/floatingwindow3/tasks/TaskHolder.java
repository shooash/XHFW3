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
import com.zst.xposed.halo.floatingwindow3.MovableOverlayView;
import com.zst.xposed.halo.floatingwindow3.overlays.*;
import android.content.*;
import com.zst.xposed.halo.floatingwindow3.*;

public class TaskHolder
{
	final Activity mActivity;
	//Map<String, ArrayList<WindowHolder>> activitiesStack = new HashMap<>();
	ArrayList<WindowHolder> windowsStack = new ArrayList<>();
	int taskId;
	WindowHolder defaultLayout = new WindowHolder();
	WindowHolder cachedLayout = new WindowHolder();
	int cachedOrientation;
	String packageName;
	MovableOverlayView mMovableOverlay = null;
	OverlayView mOverlay = null;
	boolean isSnapped;
	boolean isMaximized;
	
	public TaskHolder(final Activity sActivity, final WindowHolder mDefaultLayout, final WindowHolder mCachedLayout) {
	
		MainXposed.mPref.reload();
		cachedOrientation=Util.getScreenOrientation(sActivity.getApplicationContext());	
		packageName = sActivity.getCallingPackage();
		mActivity = sActivity;
		if(packageName==null)
			packageName = sActivity.getPackageName();
//		int flags = mActivity.getIntent().getFlags();
//		isHiddenFromRecents = Util.isFlag(flags, Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
//			|| Util.isFlag(flags, Intent.FLAG_ACTIVITY_NO_HISTORY);
		if(mDefaultLayout==null)
			getDefaults(MainXposed.mPref);
		else
			defaultLayout.copy(mDefaultLayout);
		//setOverlayView(sActivity.getWindow());
		isSnapped = defaultLayout.isSnapped;
		isMaximized = defaultLayout.isMaximized;
		if(mCachedLayout!=null)
			cachedLayout.copy(mCachedLayout);
		mActivity.getComponentName().getClassName();
	}

	public void broadcastResizable(boolean show, int x, int y, boolean left)
	{
		if(!show||windowsStack.isEmpty()) {
			broadcastResizable(false, 0,0,0,0);
			return;
		}
		WindowHolder mWindowHolder = windowsStack.get(0);
		if(left)
			broadcastResizable(true, mWindowHolder.x+x, mWindowHolder.y, mWindowHolder.width-x, mWindowHolder.height+y);
		else
			broadcastResizable(true, mWindowHolder.x, mWindowHolder.y, x, y);
	}
	
	
	private void broadcastResizable(boolean show, int x, int y, int w, int h) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		if (show) {
			int[] array = { x, y, w, h };
			i.putExtra(Common.INTENT_APP_PARAMS, array);
		}
		mActivity.getApplicationContext().sendBroadcast(i);
	}

	public WindowHolder getLastOrDefaultWindow()
	{
		return defaultLayout;
//		if(windowsStack.isEmpty())
//			return defaultLayout;
//		else
//			return windowsStack.get(windowsStack.size()-1);
	}

	public void updateByFloatDot(int x, int y, int p2, int p3)
	{
		boolean changed = defaultLayout.updateByFloatDot(x, y, p2, p3);
//		for(WindowHolder mWindowHolder : windowsStack) {
//			if(mWindowHolder!=null) 
//				mWindowHolder.updateByFloatDot(x, y, p2, p3);
//			}
		if(changed) {
			syncAllWindowsAsWindow(defaultLayout);
			InterActivity.resetFocusFrameIfNeeded(mActivity.getApplicationContext(),
							defaultLayout.x,
							defaultLayout.y,
							defaultLayout.width,
							defaultLayout.height,
							taskId);
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
		setOverlayView(mWindow);
		mOverlay.setTitleBarVisibility();
		//putOverlayView(mWindow);
		
		//TODO TEST MOVE TO NEW WINDOW
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
		defaultLayout.copy(copyLayout);
	}
	
	public void move(int x, int y) {
		for(WindowHolder mWindowHolder : windowsStack) {
			if(mWindowHolder!=null) {
				mWindowHolder.position(x, y);
				mWindowHolder.pushToWindow();
				}
		}
		defaultLayout.position(x, y);
	}
	
	public void resize(int deltax, int deltay, int offset) {
		
		if(deltax>0&&deltay>0) {
			resize_backwards(deltax, deltay, offset);
			return;
		}
		final Context mContext = mActivity.getApplicationContext();
		for(WindowHolder mWindowHolder : windowsStack) {
			if(mWindowHolder!=null) {
				mWindowHolder.resize(deltax, deltay, mContext);
				mWindowHolder.position(mWindowHolder.x+offset, mWindowHolder.y);
				mWindowHolder.pushToWindow();
			}
		}
		if(!windowsStack.isEmpty()) 
			defaultLayout.copy(windowsStack.get(0));
	}
	
	private void resize_backwards(int deltax, int deltay, int offset) {
		final Context mContext = mActivity.getApplicationContext();
		for(int i = windowsStack.size()-1; i >= 0; i--) {
			WindowHolder mWindowHolder = windowsStack.get(i);
			if(mWindowHolder!=null) {
				mWindowHolder.resize(deltax, deltay, mContext);
				mWindowHolder.position(mWindowHolder.x+offset, mWindowHolder.y);
				mWindowHolder.pushToWindow();
			}
		}
		if(!windowsStack.isEmpty()) 
			defaultLayout.copy(windowsStack.get(0));
	}
	
	public void snap(int x, int y, int w, int h, int snapgravity) {
		WindowHolder mWindowHolder = new WindowHolder(null, x, y, w, h);
		mWindowHolder.SnapGravity = snapgravity;
		mWindowHolder.isSnapped = true;
		snap(mWindowHolder);
	}
	
	public void snap(final WindowHolder mWindowHolder) {
		if(!isMaximized&&!isSnapped)
			saveLayout();
		syncAllWindowsAsWindow(mWindowHolder);
		isSnapped = true;
		if(mOverlay!= null) mOverlay.setTitleBarVisibility(false);
	}
	
	public void maximize() {
		if(!isMaximized&&!isSnapped)
			saveLayout();
		for(WindowHolder mWindowHolder : windowsStack) {
			if(mWindowHolder!=null) {
				mWindowHolder.setMaximized();
				mWindowHolder.pushToWindow();
			}
		}
		defaultLayout.setMaximized();
		isMaximized = true;
		if(mOverlay!= null) mOverlay.setTitleBarVisibility(false);
	}
	
	public void saveLayout() {
		if(windowsStack.isEmpty())
			return;
		cachedLayout.copy(windowsStack.get(0));
	}
	
	public void restore() {
		syncAllWindowsAsWindow(cachedLayout);
		isSnapped = false;
		isMaximized = false;
		if(mOverlay!=null) mOverlay.setTitleBarVisibility(true);
	}
	
	public void setOverlayView(final Window mWindow){
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
//		mMovableOver(lay = (MovableOverlayView) decorView.getTag(Common.LAYOUT_OVERLAY_TAG);
//		
//		
//		for (int i = 0; i < decorView.getChildCount(); ++i) {
//			final View child = decorView.getChildAt(i);
//			if (child instanceof MovableOverlayView && mMovableOverlay != child) {
//				// If our tag is different or null, then the
//				// view we found should be removed by now.
//				decorView.removeView(decorView.getChildAt(i));
//				break;
//			}
//		}
//		if (mMovableOverlay == null) {
//			mMovableOverlay = new MovableOverlayView(mActivity, MainXposed.sModRes, MainXposed.mPref);
//			decorView.addView(mMovableOverlay, -1, mMovableOverlay.getParams());
//			setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG,  mMovableOverlay);
//		}
		if(mOverlay != null) {
			decorView.bringChildToFront(mOverlay);
			return;
		}
		mOverlay = (OverlayView) decorView.getTag(Common.LAYOUT_OVERLAY_TAG);
		if(mOverlay == null) {
			mOverlay = new OverlayView(mActivity);
			decorView.addView(mOverlay, -1, mOverlay.getParams());
			setTagInternalForView(decorView, Common.LAYOUT_OVERLAY_TAG,  mOverlay);
			if(isSnapped||isMaximized)
				mOverlay.setTitleBarVisibility(false);
		}
		else {
			decorView.bringChildToFront(mOverlay);
			//mOverlay.setTitleBarVisibility(true);
		}
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
}
