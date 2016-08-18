package com.zst.xposed.halo.floatingwindow3.tasks;
import java.util.*;
import android.app.*;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.debug.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.content.*;
import android.graphics.*;
import android.content.res.*;
import android.view.inputmethod.*;
import android.os.*;
import android.annotation.*;
import android.content.pm.*;
import com.zst.xposed.halo.floatingwindow3.themable.*;
import com.zst.xposed.halo.floatingwindow3.overlays.*;
import android.widget.*;

public class MWTasks
{
	public String packageName = new String();
	private Map<Integer, TaskHolder> taskStack = new HashMap<>();
	//private Map<Integer, Integer> tasksIndex = new HashMap<>();
	public final Context appContext;
	//public Activity startActivity;
	private float[] movableViewCoordinates = new float[2];
	private float[] movableScreenCoordinates = new float[2];
	private boolean mChangedPreviousRangeResize;
	private boolean mChangedPreviousRangeDrag;
	private float[] mPreviousRangeResize = new float[2];
	private float[] mPreviousRangeDrag = new float[2];
	private boolean mSnappable;
	private boolean mStartMaximized;
	/*Preferences*/
	public boolean mRetainStartPosition;
    public boolean mConstantMovePosition;
	public boolean mSeparateWindows;
    public int mPreviousRotation;
    public boolean mMaximizeChangeTitleBarVisibility;
    public boolean mActionBarDraggable;
    public boolean mMinimizeToStatusbar;
    public boolean mAeroSnapChangeTitleBarVisibility;
    public boolean mAeroSnapEnabled;
	public boolean mAeroFocusWindow;
	static int mAeroSnapDelay;
    static boolean mAeroSnapSwipeApp;
	public boolean mTitleBarSingleWindow;
	public int MOVE_MAX_RANGE;
	public int mInitGravity;
	public WindowHolder defaultLayout = null;
	
	public MWTasks(final String pkg, final Activity mActivity){
		/* stuff that is common for all activities */
		 packageName = pkg;
		 appContext = mActivity.getApplicationContext();
		// startActivity = mActivity;
		 onAppStart(mActivity);
	}
	
	public void onAppStart(final Activity mActivity){
		Point screenSize = Util.getScreenSize(appContext);
		int x;
		int y;
		int snapGravity = Compatibility.snapSideToGravity(mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE, 0));
		loadPrefs(mActivity);
		/* setup listeners */
		getConnected(mActivity.getApplicationContext());
		/* load theme */
		ActivityHooks.mOverlayTheme = new OverlayTheme();
		
		/* set initial snap */
		if(snapGravity!=0) {
			defaultLayout = SnapHelpers.getSnapLayout(snapGravity, screenSize.x, screenSize.y);
			return;
		}
		/* setup default layout */
		defaultLayout = new WindowHolder();
		switch(Util.getScreenOrientation(mActivity)){
            case Configuration.ORIENTATION_LANDSCAPE:
                defaultLayout.size((int) (screenSize.x * MainXposed.mPref.getFloat(Common.KEY_LANDSCAPE_WIDTH, Common.DEFAULT_LANDSCAPE_WIDTH)), 
								   (int) (screenSize.y * MainXposed.mPref.getFloat(Common.KEY_LANDSCAPE_HEIGHT, Common.DEFAULT_LANDSCAPE_HEIGHT)));
                break;
            case Configuration.ORIENTATION_PORTRAIT:
            default:
                defaultLayout.size((int) (screenSize.x * MainXposed.mPref.getFloat(Common.KEY_PORTRAIT_WIDTH, Common.DEFAULT_PORTRAIT_WIDTH)),
								   (int) (screenSize.y * MainXposed.mPref.getFloat(Common.KEY_PORTRAIT_HEIGHT, Common.DEFAULT_PORTRAIT_HEIGHT)));
                break;
        }

		if(Util.isFlag(mInitGravity, Gravity.LEFT))
			x = 0;
		else if(Util.isFlag(mInitGravity, Gravity.RIGHT))
			x = screenSize.x-defaultLayout.width;
		else
			x = (screenSize.x-defaultLayout.width)/2;

		if(Util.isFlag(mInitGravity, Gravity.TOP))
			y = 0;
		else if(Util.isFlag(mInitGravity, Gravity.BOTTOM))
			y = screenSize.y-defaultLayout.height;
		else
			y = (screenSize.y-defaultLayout.height)/2;

		defaultLayout.position(x,y);
		//mTaskHolder = new TaskHolder(mActivity, defaultLayout);
	}
	
	private void createNewTaskFromActivity(final Activity mActivity)
	{
		WindowHolder cachedlayout = null;
		final TaskHolder mTaskHolder;
		mSeparateWindows = mSeparateWindows || (!mConstantMovePosition&&Util.isFlag(mActivity.getIntent().getFlags(), Intent.FLAG_ACTIVITY_NEW_DOCUMENT));
		if(mStartMaximized) {
			cachedlayout = new WindowHolder();
			cachedlayout.copy(defaultLayout);
			defaultLayout.setMaximized();
		}
		
		if(mSeparateWindows) {
			mTaskHolder = new TaskHolder(mActivity, defaultLayout, cachedlayout);
		}
		else if (taskStack.isEmpty()){
			mTaskHolder = new TaskHolder(mActivity, defaultLayout, cachedlayout);
		} else {
			final int anyId = getAnyRegisteredTaskId();
			final WindowHolder mDefaultLayout = taskStack.get(anyId).defaultLayout;
			final WindowHolder mCachedLayout = taskStack.get(anyId).cachedLayout;
			mTaskHolder = new TaskHolder(mActivity, mDefaultLayout, mCachedLayout);
		}

		Integer task = (Integer) mActivity.getTaskId();
		taskStack.put(task, mTaskHolder);
		//tasksIndex.put(task, taskStack.size()-1);
		InterActivity.sendPackageInfo(packageName, mActivity.getApplicationContext(), task, 0);
	}
	
	public void switchSeparateWindows(final View v)
	{
//		if(!(v instanceof ImageButton))
//			return;
		mSeparateWindows = !mSeparateWindows;
		Debugger.DEBUG("switchSeparateWindows " + mSeparateWindows);
		TitleBarViewHelpers.setTheme((ImageButton) v, 's');
		
	}

	public void updateLayoutByFloatDot()
	{
		Point screenSize = Util.getScreenSize(appContext);
		int x = InterActivity.FloatDotCoordinates[0];
		int y = InterActivity.FloatDotCoordinates[1];
//		if(mTaskHolder!=null)
//			mTaskHolder.updateByFloatDot(x,y,screenSize.x, screenSize.y);
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			if(mTh.getValue()!=null) mTh.getValue().updateByFloatDot(x,y,screenSize.x, screenSize.y);
		}
//		for(Map.Entry entry : taskStack.entrySet()) {
//			((WindowHolder)entry.getValue()).updateByFloatDot(x,y,screenSize.x, screenSize.y);
//		}
	}
	
	
	
//	public void addWindow(final Window mWindow, final int task){
//		if(taskStack.containsKey(task))
//			addWindowToExistingReg(mWindow, task);
//		else
//			Debugger.DEBUG_E("MWTask addWindow(Window, int): taskId " + task + " is not registered");
//	}

	
	
	private void loadPrefs(Activity mActivity){
		mActionBarDraggable = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_ACTIONBAR_DRAGGING_ENABLED, Common.DEFAULT_WINDOW_ACTIONBAR_DRAGGING_ENABLED);
		mRetainStartPosition = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_MOVING_RETAIN_START_POSITION, Common.DEFAULT_WINDOW_MOVING_RETAIN_START_POSITION);
		mConstantMovePosition = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_MOVING_CONSTANT_POSITION, Common.DEFAULT_WINDOW_MOVING_CONSTANT_POSITION);
		mMinimizeToStatusbar = MainXposed.mPref.getBoolean(Common.KEY_MINIMIZE_APP_TO_STATUSBAR, Common.DEFAULT_MINIMIZE_APP_TO_STATUSBAR);
		mAeroSnapEnabled = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_ENABLED, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_ENABLED);
		mAeroSnapDelay = MainXposed.mPref.getInt(Common.KEY_WINDOW_RESIZING_AERO_SNAP_DELAY, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_DELAY);
		mAeroSnapSwipeApp = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_SWIPE_APP);
		mAeroSnapChangeTitleBarVisibility = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE, Common.DEFAULT_WINDOW_RESIZING_AERO_SNAP_TITLEBAR_HIDE);
		mMaximizeChangeTitleBarVisibility = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_MAXIMIZE_HIDE, Common.DEFAULT_WINDOW_TITLEBAR_MAXIMIZE_HIDE);
		mTitleBarSingleWindow = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SINGLE_WINDOW, Common.DEFAULT_WINDOW_TITLEBAR_SINGLE_WINDOW);
		mAeroFocusWindow = MainXposed.mPref.getBoolean(Common.KEY_AERO_FOCUS_ENABLED, Common.DEFAULT_AERO_FOCUS_ENABLED);
		MOVE_MAX_RANGE = Util.realDp(MainXposed.mPref.getInt(Common.KEY_MOVE_MAX_RANGE, Common.DEFAULT_MOVE_MAX_RANGE), appContext);
		mInitGravity = MainXposed.mPref.getInt(Common.KEY_GRAVITY, Common.DEFAULT_GRAVITY);
		mStartMaximized = MainXposed.mPref.getBoolean(Common.KEY_MAXIMIZE_ALL, Common.DEFAULT_MAXIMIZE_ALL)||MainXposed.isMaximizedlisted(packageName);
    }
	
	final private Runnable FloatDotCallback = new Runnable() {
		@Override
		public void run()
		{
			updateLayoutByFloatDot();
		}
	};

	private void getConnected(Context mContext){
		if(InterActivity.XHFWInterfaceLink==null)
			InterActivity.connectService(mContext);
		if(!InterActivity.FloatDotReceiverRegistered) {
			InterActivity.FloatDotCoordinatesCallback = FloatDotCallback;
			InterActivity.registerFloatDotBroadcastReceiver(mContext);
		}
	}
	
	public void onNewTask(final Activity mActivity) {
		Integer task = (Integer) mActivity.getTaskId();
		if(taskStack.containsKey(task)) {
			onNewWindow(mActivity.getWindow(), mActivity, task);
		}
		else
			createNewTaskFromActivity(mActivity);
	}
	
	public void onNewWindow(final Window mWindow, Activity mActivity, int task){
		//if(startActivity == null && mWindow!=null)
		//	startActivity = mActivity;
		if(mActivity==null) {
			mActivity=ActivityHooks.mCurrentActivity;
			task = mActivity.getTaskId();
		}
		TaskHolder mTh = taskStack.get(task);
		if(mTh==null) {
			Debugger.DEBUG_E("onNewWindow failed with taskId=" + task);
			return;
		}
		if(!mTh.addWindow(mWindow, mActivity))
			onTaskResume(mWindow, mActivity, task);
//		mTaskHolder.addWindow(mWindow);
	}
	
	public void onNewGeneratedWindow(final Window mWindow) {
		if(mWindow==null || mWindow.isFloating() || isWindowRegisteredAnywhere(mWindow))
			return;
		Debugger.DEBUG("onNewGeneratedWindow");
		taskStack.get(ActivityHooks.mCurrentActivity.getTaskId()).defaultLayout.pushToWindow(mWindow);
		//onNewWindow(mWindow, ActivityHooks.mCurrentActivity, ActivityHooks.mCurrentActivity.getTaskId());
	}

	public void onTaskPause(final int task, final Window sWindow){
		final TaskHolder mTaskHolder = taskStack.get(task);
		//if(
		if(mTaskHolder.isSnapped) {
			//InterActivity.drawFocusFrame(appContext, mWindowHolder.x, mWindowHolder.y, mWindowHolder.width, mWindowHolder.height);
			InterActivity.toggleDragger(appContext, false);
		}
		mTaskHolder.unfocus(sWindow);
	}

	public void onTaskResume(final Window sWindow, final Activity mActivity, final int task){
		//onNewWindow(sWindow, mActivity, task);
//		if(!taskStack.containsKey(task)) {
//			onNewWindow(sWindow, mActivity, task);
//			return;
//		}
		final TaskHolder mTaskHolder = taskStack.get(task);
		if(mTaskHolder==null)
			return;
		if(mTaskHolder.isSnapped) {
			//InterActivity.drawFocusFrame(appContext, mWindowHolder.x, mWindowHolder.y, mWindowHolder.width, mWindowHolder.height);
			InterActivity.toggleDragger(appContext, true);
		}
		mTaskHolder.resume(mActivity, sWindow);
		//onTaskFocused(task);
	}

	public boolean onRemoveActivity(final Activity mActivity){
		int taskId = mActivity.getTaskId();
		if(!taskStack.containsKey(taskId))
			return false;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		if(mTaskHolder.removeActivity(mActivity))
			taskStack.remove(taskId);
		return taskStack.isEmpty();
	}

	public void onClearAll(){}

	public void onUserAction(final Activity mActivity, final MotionEvent mEvent){
		onUserAction(mActivity, mEvent, Common.ACTION_DRAG, null);
		}
		
	public void onUserAction(Activity mActivity, final MotionEvent mEvent, int action, final View offsetview){
		int height;
		boolean drag = false;
		int[] offsetXY = {0,0};
		if(mActivity == null)
			mActivity = ActivityHooks.mCurrentActivity;
		int taskId = mActivity.getTaskId();
		final Point screenSize = Util.getScreenSize(appContext);
		switch (mEvent.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if(offsetview!=null) {
					offsetview.getLocationInWindow(offsetXY);
				}
				movableViewCoordinates[0] = mEvent.getX()+offsetXY[0];
				movableViewCoordinates[1] = mEvent.getY()+offsetXY[1];
				if(action == Common.ACTION_RESIZE_LEFT&&!mChangedPreviousRangeResize) {
					restoreIfNeeded(taskId);
					mPreviousRangeResize[0] = mEvent.getRawX();
					mPreviousRangeResize[1] = mEvent.getRawY();
					mChangedPreviousRangeResize = true;
				}
				else if (action == Common.ACTION_DRAG&&!mChangedPreviousRangeDrag) {
					mPreviousRangeDrag[0] = mEvent.getRawX();
					mPreviousRangeDrag[1] = mEvent.getRawY();
					mChangedPreviousRangeDrag = true;
				}
				if(InterActivity.changeFocusApp(mActivity))
					onTaskFocused(mActivity.getTaskId(), mActivity.getWindow());
				break;
			case MotionEvent.ACTION_MOVE:
				if (action == Common.ACTION_DRAG) {
					if(offsetview==null&&mActionBarDraggable) {
						ActionBar ab = mActivity.getActionBar();
						height = (ab != null) ? ab.getHeight() : Util.dp(48, appContext);
						drag = movableViewCoordinates[1] < height;
					}
					else 
						drag = (offsetview!=null);
					
					if (drag) {
						movableScreenCoordinates[0] = mEvent.getRawX();
						movableScreenCoordinates[1] = mEvent.getRawY();
						if(Math.abs(mPreviousRangeDrag[0]-movableScreenCoordinates[0]) < MOVE_MAX_RANGE &&
						   Math.abs(mPreviousRangeDrag[1]-movableScreenCoordinates[1]) < MOVE_MAX_RANGE) {
								//mPreviousRange = movableScreenCoordinates;
								return;
							}
						restoreIfNeeded(taskId);
						mSnappable = SnapHelpers.checkAndShowSnap((int)movableScreenCoordinates[0], (int) movableScreenCoordinates[1], screenSize.x, screenSize.y, appContext);
//						if((mWindowHolder.isSnapped || mWindowHolder.isMaximized) && !moveRangeAboveLimit(event))
//							break;
//						unsnap();
						Float leftFromScreen = (movableScreenCoordinates[0] - movableViewCoordinates[0]);
						Float topFromScreen = (movableScreenCoordinates[1] - movableViewCoordinates[1]);
//						mSeparateWindows = !mConstantMovePosition&&Util.isFlag(mActivity.getIntent().getFlags(), Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
						move(leftFromScreen.intValue(), topFromScreen.intValue(), mActivity.getTaskId());								
					}
				}
				
				else if (action == Common.ACTION_RESIZE_LEFT) {
//					movableScreenCoordinates[0] = mEvent.getRawX();
//					movableScreenCoordinates[1] = mEvent.getRawY();
					broadcastResizable(taskId, true, (int) mEvent.getX(), (int) mEvent.getY(), true);
				}
				break;
			case MotionEvent.ACTION_UP:
				if (mSnappable) {
					snap(SnapHelpers.checkAndGetFinalSnap((int) mEvent.getRawX(), (int) mEvent.getRawY(), screenSize.x, screenSize.y, appContext), 
						taskId);
					mSnappable = false;
				}
				else if(action == Common.ACTION_RESIZE_LEFT&&mChangedPreviousRangeResize) {
					broadcastResizable(taskId, false, 0, 0, true);
					mChangedPreviousRangeResize = false;
					int deltax = (int) (mEvent.getRawX() - mPreviousRangeResize[0]);
					int deltay = (int) (mEvent.getRawY() - mPreviousRangeResize[1]);
					resize(-deltax, deltay, deltax, taskId);
				}
				else if (action == Common.ACTION_DRAG)
					mChangedPreviousRangeDrag=false;
				break;
		}
	}
	
	public void onTaskFocused(int taskId, final Window mWindow) {
		if(!taskStack.containsKey(taskId))
			return;
		for(TaskHolder mTh : taskStack.values()) {
			if(mTh.mOverlay!=null)
				mTh.mOverlay.setWindowBorder();
		}
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		if(mTaskHolder.activitiesStack.isEmpty())
			return;
		mTaskHolder.focus(mWindow);
		final WindowHolder mWindowHolder = mTaskHolder.defaultLayout;
		
		if(mTaskHolder.isSnapped) {
			InterActivity.drawFocusFrame(appContext, mWindowHolder.x, mWindowHolder.y, mWindowHolder.width, mWindowHolder.height);
			InterActivity.toggleDragger(appContext, true);
		}
	}

	public void onTaskUnFocused(int taskId, final Window mWindow) {
		InterActivity.unfocusApp(taskId);
			InterActivity.hideFocusFrame(appContext);
			InterActivity.toggleDragger(appContext, false);
//		for(TaskHolder mTh : taskStack.values()) {
//			if(mTh.mOverlay!=null)
//				mTh.mOverlay.setWindowBorder();
//		}
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		if(mTaskHolder!=null) {
			mTaskHolder.unfocus(mWindow);
//			if(mTaskHolder.mOverlay!=null)
//				mTaskHolder.mOverlay.setWindowBorder();
		}
			
	}
	
	public void restoreIfNeeded(int taskId){
		if(!mSeparateWindows) {
			restoreAll();
			return;
		}
		if(!taskStack.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		if(mTaskHolder.isSnapped || mTaskHolder.isMaximized ) {
			mTaskHolder.restore();
			//InterActivity.hideFocusFrame(appContext);
			InterActivity.toggleDragger(appContext, false);
		}
			
	}
	
	private void restoreAll() {
		boolean restored = false;
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			if(mTh.getValue()!=null) {
				TaskHolder mTaskHolder = mTh.getValue();
				if(mTaskHolder.isSnapped || mTaskHolder.isMaximized ) 
					mTaskHolder.restore();
					restored = true;
				}
		}
		if(restored) {
			//InterActivity.hideFocusFrame(appContext);
			InterActivity.toggleDragger(appContext, false);
		}
			
	}
	
	public void move(int x, int y, int taskId) {
		if(!mSeparateWindows) {
			move(x,y);
			return;
		}
		//if(!taskStack.containsKey(taskId))
		if(!taskStack.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		mTaskHolder.move(x, y);
	}
	
	public void move (int x,  int y) {
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			if(mTh.getValue()!=null) mTh.getValue().move(x, y);
		}
	}
	
	public void resize(int deltax, int deltay, int offset, int taskId) {
		if(!mSeparateWindows) {
			resizeall(deltax, deltay, offset, taskId);
			return;
		}
		if(!taskStack.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		mTaskHolder.resize(deltax, deltay, offset);
	}
	
	public void resizeall(int deltax, int deltay, int offset, int taskId) {
		TaskHolder mTaskHolderBase = taskStack.get(taskId);
		mTaskHolderBase.resize(deltax, deltay, offset);
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			if(mTh.getValue()!=null) mTh.getValue().syncAllWindowsAsWindow(mTaskHolderBase.defaultLayout);
		}
	}
	
	public void maximize() {
		maximize(InterActivity.focusedTaskId);
	}
	public void maximize(int taskId) {
		InterActivity.toggleDragger(appContext, false);
		InterActivity.hideFocusFrame(appContext);
		if(!mSeparateWindows) {
			maximizeAll();
			return;
		}
		if(!taskStack.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		mTaskHolder.maximize();
	}
	
	private void maximizeAll() {
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			if(mTh.getValue()!=null) mTh.getValue().maximize();
		}
	}
	
	public void snapCurrentWindow(int snapGravity) {
		int taskId = InterActivity.focusedTaskId;
		if(taskId==-1||!taskStack.containsKey(taskId))
			taskId = ActivityHooks.mCurrentActivity.getTaskId();
		snap(SnapHelpers.getSnapLayout(snapGravity, appContext), taskId);
	}
	
	public void snap(final WindowHolder snapWindowHolder, int taskId) {
		
		if(!mSeparateWindows) {
			snapAll(snapWindowHolder);
			return;
		}
		if(!taskStack.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		mTaskHolder.snap(snapWindowHolder);
		InterActivity.drawFocusFrame(appContext, snapWindowHolder.x, snapWindowHolder.y, snapWindowHolder.width, snapWindowHolder.height);
		InterActivity.toggleDragger(appContext, true);
	}
	
	private void snapAll(final WindowHolder snapWindowHolder) {
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			if(mTh.getValue()!=null) 
				mTh.getValue().snap(snapWindowHolder);
		}
		InterActivity.drawFocusFrame(appContext, snapWindowHolder.x, snapWindowHolder.y, snapWindowHolder.width, snapWindowHolder.height);
		InterActivity.toggleDragger(appContext, true);
	}
	
	private void broadcastResizable(int taskId, boolean show, int x, int y, boolean left) {
		if(!taskStack.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(taskId);
		mTaskHolder.broadcastResizable(show, x, y, left);
	}
	
	private int getAnyRegisteredTaskId() {
		//taskStack.keySet().toArray();
		//return taskStack.keySet().toArray()[0];
		return taskStack.keySet().iterator().next();
	}
	
	public boolean isWindowRegisteredAnywhere(final Window mWindow) {
		for(TaskHolder mTaskHolder : taskStack.values()) {
			if(mTaskHolder.isWindowRegistered(mWindow))
				return true;
		}
		return false;
	}
	public WindowTaskHolderFinderResult findWindowTaskHolder(final Window mWindow) {
		for(Map.Entry<Integer, TaskHolder> mTh : taskStack.entrySet()) {
			String result = mTh.getValue().findWindowActivityId(mWindow);
			if(result!=null)
				return new WindowTaskHolderFinderResult(mTh.getKey(), result);
		}
		return null;
	}
	
	public class WindowTaskHolderFinderResult {
		final Integer taskId;
		final String activityId;
		public WindowTaskHolderFinderResult(final Integer t, final String s) {
			taskId = t;
			activityId = s;
		}
	}
	
	public void closeCurrentApp() {
		try {
			/* Work-around for bug:
			 * When closing a floating window using the titlebar
			 * while the keyboard is open, the floating window
			 * closes but the keyboard remains open on top of
			 * another fullscreen app.
			 */
			InputMethodManager imm = (InputMethodManager)
				appContext.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(ActivityHooks.mCurrentActivity.getCurrentFocus().getWindowToken(), 0);
		} catch (Exception e) {
			//ignore
		}
		//final TaskHolder mTaskHolder = taskStack.get(InterActivity.focusedTaskId);
		final Activity sActivity;
		//if(mTaskHolder==null)
			sActivity = ActivityHooks.mCurrentActivity;
		//else
		//	sActivity = mTaskHolder.mActivity;
		if (mTitleBarSingleWindow && Build.VERSION.SDK_INT >= 16) {
			sActivity.finishAffinity();
		} else {
			sActivity.finish();
		}
	}
	
	public void minimizeCurrentApp(){
		//final TaskHolder mTaskHolder = taskStack.get(InterActivity.focusedTaskId);
		final Activity sActivity;
		//if(mTaskHolder==null)
			sActivity = ActivityHooks.mCurrentActivity;
		//else
		//	sActivity = mTaskHolder.mActivity;
		InterActivity.minimizeAndShowNotification(sActivity);
	}
	
}
