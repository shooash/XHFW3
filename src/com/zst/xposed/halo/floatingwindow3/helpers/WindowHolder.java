package com.zst.xposed.halo.floatingwindow3.helpers;

import android.app.Activity;
import android.view.Gravity;
import android.view.Window;

import com.zst.xposed.halo.floatingwindow3.Common;

import de.robv.android.xposed.XSharedPreferences;
import android.view.*;
import android.content.*;

public class WindowHolder{
    public boolean isFloating = false;
	public boolean isMovable = false;
    public boolean isSnapped = false;
    public boolean isMaximized = false;
	public boolean serviceConnected = false;
	public int SnapGravity = 0; //Gravity flag, eg TOP | LEFT for TopLeft window
	public float dim;
    public float alpha;
    public int width = -1;
    public int height = -1;
	public int x = 0;
	public int y = 0;
    public int cachedOrientation;
    public int cachedRotation;
    public Window mWindow;
    public String packageName;
	public Activity mActivity;
	public boolean isSet=false;
	public boolean mReceiverRegistered = false;

    public WindowHolder(Activity sActivity, XSharedPreferences mPref){
		mActivity = sActivity;
        mPref.reload();
        isFloating=(mActivity.getIntent().getFlags() & mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW))
                == mPref.getInt(Common.KEY_FLOATING_FLAG, Common.FLAG_FLOATING_WINDOW);
		alpha = mPref.getFloat(Common.KEY_ALPHA, Common.DEFAULT_ALPHA);
		dim = mPref.getFloat(Common.KEY_DIM, Common.DEFAULT_DIM);
		isMovable = (isFloating&&(mPref.getBoolean(Common.KEY_MOVABLE_WINDOW, Common.DEFAULT_MOVABLE_WINDOW)));
        cachedOrientation=mActivity.getResources().getConfiguration().orientation;
        cachedRotation = Util.getDisplayRotation(mActivity);
		/*TODO: Get use of EXTRA_SNAP extras to keep snap gravity*/
		/*if(mActivity.getIntent().hasExtra(Common.EXTRA_SNAP)) SnapGravity = mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP, 0);
			else */
		SnapGravity = Compatibility.snapSideToGravity(mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE, Compatibility.AeroSnap.SNAP_NONE));
		//mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP, 0);
        isSnapped=(SnapGravity != 0);
        isMaximized=(SnapGravity == Gravity.FILL);
        mWindow = mActivity.getWindow();
        updateWindow();
		packageName = mActivity.getPackageName();
	}
	
	public void updateSnap(int newSnap){
		SnapGravity = newSnap;
	}
	
	public boolean updateSnap(Activity sActivity){
		int newSnap = sActivity.getIntent().getIntExtra(Common.EXTRA_SNAP, 0);
		if(newSnap == 0) return false;
		if(SnapGravity == newSnap) return false;
		SnapGravity = newSnap;
		isSnapped=(SnapGravity != 0);
        isMaximized=(SnapGravity == Gravity.FILL);
		return true;
	}
	
	public void updateWindow(Window sWindow){
		mWindow = sWindow;
		updateWindow();
	}
	
	public void updateWindow(){
		alpha = mWindow.getAttributes().alpha;
        width = mWindow.getAttributes().width;
        height = mWindow.getAttributes().height;
		x = mWindow.getAttributes().x;
		y = mWindow.getAttributes().y;
		packageName = mWindow.getAttributes().packageName;
	}
	
	public void setWindow (Activity sActivity){
		mActivity = sActivity;
		mWindow = sActivity.getWindow();
		/*FIX focus other windows not working on some apps*/
		mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	}
	
	public void setWindow (Window sWindow){
		mWindow = sWindow;
		//mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
	}

	public void setMaximized(){
		width = ViewGroup.LayoutParams.MATCH_PARENT;
		height = ViewGroup.LayoutParams.MATCH_PARENT;
		x=0;
		y=0;
		SnapGravity=Gravity.FILL;
		isMaximized=true;
	}
	
	//restore/copy precached data
	public void restore(WindowHolder sWindowHolder){
		alpha = sWindowHolder.alpha;
		width = sWindowHolder.width;
		height = sWindowHolder.height;
		x = sWindowHolder.x;
		y = sWindowHolder.y;
		isMaximized = sWindowHolder.isMaximized;
		isFloating = sWindowHolder.isFloating;
		isSnapped = sWindowHolder.isSnapped;
		SnapGravity = sWindowHolder.SnapGravity;
		//pushToWindow();
	}
	
	public void restore(SnapWindowHolder sSnapWindowHolder){
		x = sSnapWindowHolder.x;
		y = sSnapWindowHolder.y;
		width = sSnapWindowHolder.width;
		height = sSnapWindowHolder.height;
		SnapGravity = sSnapWindowHolder.SnapGravity;
		isSnapped = sSnapWindowHolder.isSnapped;
	}
	
	//set current window to saved layout params
	public void pushToWindow(){
		WindowManager.LayoutParams mWParams = mWindow.getAttributes();
		mWParams.x = x;
		mWParams.y = y;
		mWParams.alpha = alpha;
		mWParams.width = width;
		mWParams.height = height;
		mWParams.dimAmount = dim;
		//mWParams.gravity = Gravity.TOP | Gravity.LEFT;
		//Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
		mWindow.setAttributes(mWParams);
	}
	
	public void pushToWindow(Window sWindow){
		WindowManager.LayoutParams mWParams = sWindow.getAttributes();
		mWParams.x = x;
		mWParams.y = y;
		mWParams.alpha = alpha;
		mWParams.width = width;
		mWParams.height = height;
		mWParams.gravity = Gravity.TOP | Gravity.LEFT;
		sWindow.setAttributes(mWParams);
	}
	
	//set current window to saved layout params
	public void pullFromWindow(){
		WindowManager.LayoutParams mWParams = mWindow.getAttributes();
		x = mWParams.x;
		y = mWParams.y;
		alpha = mWParams.alpha;
		width = mWParams.width;
		height = mWParams.height;
		cachedOrientation = Util.getScreenOrientation(mActivity);
	}
	
	public int restoreSnap(){
		if(!isSnapped) {
			SnapGravity = 0;
			return 0;
		}
		int newFlag = 0;
		if(width!=-1){
			newFlag |= (x==0)?Gravity.LEFT : Gravity.RIGHT;
		}
		if(height!=-1){
			newFlag |= (y==0)?Gravity.TOP : Gravity.BOTTOM;
		}
		SnapGravity = newFlag;
		return newFlag;
	}
	
}

class SnapWindowHolder{
	int x;
	int y;
	int height;
	int width;
	int SnapGravity;
	boolean isSnapped = false;
	public void updateSnap(int newSnap){
		//if(SnapGravity == newSnap) return;
		SnapGravity = newSnap;
		//isSnapped=(SnapGravity != 0);
        //isMaximized=(SnapGravity == Gravity.FILL);
	}
}
