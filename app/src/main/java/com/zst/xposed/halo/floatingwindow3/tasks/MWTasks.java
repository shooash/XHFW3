package com.zst.xposed.halo.floatingwindow3.tasks;
import java.util.*;
import android.app.*;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.debug.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.content.*;
import android.graphics.*;
import android.content.res.*;

public class MWTasks
{
	public String packageName = new String();
	private ArrayList<TaskHolder> taskStack = new ArrayList<>();
	private Map<Integer, Integer> tasksIndex = new HashMap<>();
	private Context appContext;
	private float[] movableViewCoordinates = new float[2];
	private float[] movableScreenCoordinates = new float[2];
	private boolean mChangedPreviousRangeResize;
	private boolean mChangedPreviousRangeDrag;
	private float[] mPreviousRangeResize = new float[2];
	private float[] mPreviousRangeDrag = new float[2];
	private boolean mSnappable;
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
	public int MOVE_MAX_RANGE;
	public int mInitGravity;
	private WindowHolder defaultLayout = null;
	
	public MWTasks(final String pkg, final Activity mActivity){
		/* stuff that is common for all activities */
		 packageName = pkg;
		 appContext = mActivity.getApplicationContext();
		 onAppStart(mActivity);
	}

	public void updateLayoutByFloatDot()
	{
		Point screenSize = Util.getScreenSize(appContext);
		int x = InterActivity.FloatDotCoordinates[0];
		int y = InterActivity.FloatDotCoordinates[1];
//		if(mTaskHolder!=null)
//			mTaskHolder.updateByFloatDot(x,y,screenSize.x, screenSize.y);
		for(TaskHolder mTh : taskStack) {
			if(mTh!=null) mTh.updateByFloatDot(x,y,screenSize.x, screenSize.y);
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

	private void createNewTaskFromActivity(final Activity mActivity)
	{
		//WindowHolder2 mWindowHolder = new WindowHolder2(mActivity, defaultLayout);
		mSeparateWindows = !mConstantMovePosition&&Util.isFlag(mActivity.getIntent().getFlags(), Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
		TaskHolder mTaskHolder;
		if(mSeparateWindows)
			mTaskHolder = new TaskHolder(mActivity, defaultLayout, null);
		else 
			mTaskHolder = new TaskHolder(mActivity,
				taskStack.isEmpty()?defaultLayout:taskStack.get(0).getLastOrDefaultWindow(),
										 taskStack.isEmpty()?null:taskStack.get(0).cachedLayout);
		
		/* DEBUG SECTION */
//		int flags = mActivity.getIntent().getFlags();
//		Debugger.DEBUG("DEBUG FLAG HAS MULTIPLE TASKS:" + Util.isFlag(flags, Intent.FLAG_ACTIVITY_MULTIPLE_TASK) + 
//					   "DEBUG FLAG HAS NEW DOCUMENT" + Util.isFlag(flags, Intent.FLAG_ACTIVITY_NEW_DOCUMENT));
		
		/* TODO: CLEAN */
		Integer task = (Integer) mActivity.getTaskId();
		taskStack.add(mTaskHolder);
		tasksIndex.put(task, taskStack.size()-1);
		InterActivity.sendPackageInfo(packageName, mActivity.getApplicationContext(), task, 0);
	}
	
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
		mAeroFocusWindow = MainXposed.mPref.getBoolean(Common.KEY_AERO_FOCUS_ENABLED, Common.DEFAULT_AERO_FOCUS_ENABLED);
		MOVE_MAX_RANGE = Util.realDp(MainXposed.mPref.getInt(Common.KEY_MOVE_MAX_RANGE, Common.DEFAULT_MOVE_MAX_RANGE), appContext);
		mInitGravity = MainXposed.mPref.getInt(Common.KEY_GRAVITY, Common.DEFAULT_GRAVITY);
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
	
	public void onAppStart(final Activity mActivity){
		Point screenSize = Util.getScreenSize(appContext);
		int x;
		int y;
		loadPrefs(mActivity);
		/* setup listeners */
		getConnected(mActivity.getApplicationContext());
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
		/* initial snap */
		//TODO
//		if(mAeroSnap!=null&&mWindowHolder.isSnapped) {
//			mWindowHolder.isSnapped=false;
//			mAeroSnap.forceSnapGravity(mWindowHolder.SnapGravity);
//		}
//		else if(MainXposed.mPref.getBoolean(Common.KEY_MAXIMIZE_ALL, Common.DEFAULT_MAXIMIZE_ALL) || MainXposed.isMaximizedlisted(mWindowHolder.packageName)){
//			maximize();
//		} else {
//			mWindowHolder.syncLayout();
//		}
	}
	
	public void onNewTask(final Activity mActivity) {
//		Integer task = (Integer) mActivity.getTaskId();
//		if(taskStack.contains(task))
//			onNewWindow(mActivity.getWindow(), task);
//		else
			createNewTaskFromActivity(mActivity);
	}
	
	public void onNewWindow(final Window mWindow, final int task){
		TaskHolder mTh = taskStack.get(tasksIndex.get(task));
		if(mTh==null) {
			Debugger.DEBUG_E("onNewWindow failed with taskId=" + task);
			return;
		}
		mTh.addWindow(mWindow);
//		mTaskHolder.addWindow(mWindow);
	}

	public void onTaskPause(){
		
	}

	public void onTaskResume(final Window sWindow, final int task){
		onNewWindow(sWindow, task);
	}

	public void onRemoveActivity(final Activity mActivity){
//		int taskId = mActivity.getTaskId();
//		int index;
//		if(!tasksIndex.containsKey(taskId))
//			return;
//		index = tasksIndex.get(taskId);
//		final TaskHolder mTaskHolder = taskStack.get(index);
//		if(mTaskHolder.remove())
//			taskStack.remove(index);
		
	}

	public void onClearAll(){}

	public void onUserAction(final Activity mActivity, final MotionEvent mEvent){
		onUserAction(mActivity, mEvent, Common.ACTION_DRAG, null);
		}
		
	public void onUserAction(final Activity mActivity, final MotionEvent mEvent, int action, final View offsetview){
		int height;
		boolean drag = false;
		int[] offsetXY = {0,0};
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
				InterActivity.changeFocusApp(mActivity);
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
					int deltax = (int) (mEvent.getRawX() - mPreviousRangeResize[0]);
					int deltay = (int) (mEvent.getRawY() - mPreviousRangeResize[1]);
					resize(-deltax, deltay, deltax, taskId);
					mChangedPreviousRangeResize = false;
					broadcastResizable(taskId, false, 0, 0, true);
				}
				else if (action == Common.ACTION_DRAG)
					mChangedPreviousRangeDrag=false;
				break;
		}
	}
	
	public void restoreIfNeeded(int taskId){
		if(!mSeparateWindows) {
			restoreAll();
			return;
		}
		if(!tasksIndex.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(tasksIndex.get(taskId));
		if(mTaskHolder.isSnapped || mTaskHolder.isMaximized ) 
			mTaskHolder.restore();
	}
	
	private void restoreAll() {
		for(TaskHolder mTaskHolder : taskStack) {
			if(mTaskHolder.isSnapped || mTaskHolder.isMaximized ) 
				mTaskHolder.restore();
		}
	}
	
	public void move(int x, int y, int taskId) {
		if(!mSeparateWindows) {
			move(x,y);
			return;
		}
		//if(!taskStack.containsKey(taskId))
		if(!tasksIndex.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(tasksIndex.get(taskId));
		mTaskHolder.move(x, y);
	}
	
	public void move (int x,  int y) {
		for(TaskHolder mTaskHolder : taskStack) {
			mTaskHolder.move(x, y);
		}
	}
	
	public void resize(int deltax, int deltay, int offset, int taskId) {
		if(!mSeparateWindows) {
			resize(deltax, deltay, offset);
			return;
		}
		if(!tasksIndex.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(tasksIndex.get(taskId));
		mTaskHolder.resize(deltax, deltay, offset);
	}
	
	public void resize(int deltax, int deltay, int offset) {
		TaskHolder mTaskHolderBase = taskStack.get(0);
		mTaskHolderBase.resize(deltax, deltay, offset);
		for(TaskHolder mTaskHolder : taskStack) {
			//mTaskHolder.resize(deltax, deltay, offset);
			mTaskHolder.syncAllWindowsAsWindow(mTaskHolderBase.defaultLayout);
		}
	}
	
	public void maximize(int taskId) {
		if(!mSeparateWindows) {
			maximize();
			return;
		}
		if(!tasksIndex.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(tasksIndex.get(taskId));
		mTaskHolder.maximize();
	}
	
	private void maximize() {
		for(TaskHolder mTaskHolder : taskStack) {
			mTaskHolder.maximize();
		}
	}
	
	public void snap(final WindowHolder snapWindowHolder, int taskId) {
		if(!mSeparateWindows) {
			snap(snapWindowHolder);
			return;
		}
		if(!tasksIndex.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(tasksIndex.get(taskId));
		mTaskHolder.snap(snapWindowHolder);
	}
	
	private void snap(final WindowHolder snapWindowHolder) {
		for(TaskHolder mTaskHolder : taskStack) {
			mTaskHolder.snap(snapWindowHolder);
		}
	}
	
	private void broadcastResizable(int taskId, boolean show, int x, int y, boolean left) {
		if(!tasksIndex.containsKey(taskId))
			return;
		final TaskHolder mTaskHolder = taskStack.get(tasksIndex.get(taskId));
		mTaskHolder.broadcastResizable(show, x, y, left);
	}

}
