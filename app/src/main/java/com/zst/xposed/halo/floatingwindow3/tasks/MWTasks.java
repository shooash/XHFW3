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
	private Map<Integer, WindowHolder> taskStack = new HashMap<>();
	private Context appContext;
	private float[] movableViewCoordinates = new float[2];
	private float[] movableScreenCoordinates = new float[2];
	private boolean mChangedPreviousRange;
	private float[] mPreviousRange = new float[2];
	/*Preferences*/
	public boolean mRetainStartPosition;
    public boolean mConstantMovePosition;
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
		for(Map.Entry entry : taskStack.entrySet()) {
			((WindowHolder)entry.getValue()).updateByFloatDot(x,y,screenSize.x, screenSize.y);
		}
	}
	
	
	
//	public void addWindow(final Window mWindow, final int task){
//		if(taskStack.containsKey(task))
//			addWindowToExistingReg(mWindow, task);
//		else
//			Debugger.DEBUG_E("MWTask addWindow(Window, int): taskId " + task + " is not registered");
//	}

	private void createNewTaskFromActivity(final Activity mActivity)
	{
		WindowHolder mWindowHolder = new WindowHolder(mActivity, defaultLayout);
		Integer task = (Integer) mActivity.getTaskId();
		taskStack.put(task, mWindowHolder);
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
		MOVE_MAX_RANGE = Util.realDp(MainXposed.mPref.getInt(Common.KEY_MOVE_MAX_RANGE, Common.DEFAULT_MOVE_MAX_RANGE), mActivity.getApplicationContext());
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
		defaultLayout = new WindowHolder(mActivity);
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
		Integer task = (Integer) mActivity.getTaskId();
		if(taskStack.containsKey(task))
			onNewWindow(mActivity.getWindow(), task);
		else
			createNewTaskFromActivity(mActivity);
	}
	
	public void onNewWindow(final Window mWindow, final int task){
		WindowHolder mWindowHolder = taskStack.get(task);
		if(mWindowHolder==null) {
			Debugger.DEBUG_E("onNewWindow failed with taskId=" + task);
			return;
		}	
		mWindowHolder.setWindow(mWindow);
		mWindowHolder.pushToWindow();
	}

	public void onTaskPause(){
		
	}

	public void onTaskResume(final Window sWindow, final int task){
		onNewWindow(sWindow, task);
	}

	public void onRemoveActivity(){}

	public void onClearAll(){}

	public void onUserAction(final Activity mActivity, final MotionEvent mEvent){
		switch (mEvent.getAction()) {
			case MotionEvent.ACTION_DOWN:
				movableViewCoordinates[0] = mEvent.getX();
				movableViewCoordinates[1] = mEvent.getY();
				if (!mChangedPreviousRange) {
					mPreviousRange[0] = mEvent.getRawX();
					mPreviousRange[1] = mEvent.getRawY();
					mChangedPreviousRange = true;
				}
				InterActivity.changeFocusApp(mActivity);
				break;
			case MotionEvent.ACTION_MOVE:
				if (mActionBarDraggable) {
					ActionBar ab = mActivity.getActionBar();
					int height = (ab != null) ? ab.getHeight() : Util.dp(48, mActivity.getApplicationContext());

					if (movableViewCoordinates[1] < height) {
						movableScreenCoordinates[0] = mEvent.getRawX();
						movableScreenCoordinates[1] = mEvent.getRawY();
//						if((mWindowHolder.isSnapped || mWindowHolder.isMaximized) && !moveRangeAboveLimit(event))
//							break;
//						unsnap();
						Float leftFromScreen = (movableScreenCoordinates[0] - movableViewCoordinates[0]);
						Float topFromScreen = (movableScreenCoordinates[1] - movableViewCoordinates[1]);
						move(leftFromScreen.intValue(), topFromScreen.intValue(), mActivity.getTaskId());								
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				mChangedPreviousRange=false;
				break;
		}
//		ActionBar ab = a.getActionBar();
//		int height = (ab != null) ? ab.getHeight() : Util.dp(48, a.getApplicationContext());
//		if (viewY < height && mAeroSnap != null 
//			&& mActionBarDraggable && !mWindowHolder.isSnapped 
//			&& !mWindowHolder.isMaximized) {
//			mAeroSnap.dispatchTouchEvent(event);
//		}
	}
	
	public void move(int x, int y, int taskId) {
		if(!taskStack.containsKey(taskId))
			return;
		final WindowHolder mWindowHolder = taskStack.get(taskId);
		mWindowHolder.position(x, y);
		mWindowHolder.syncLayout();
	}
	
}
