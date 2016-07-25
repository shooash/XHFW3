package com.zst.xposed.halo.floatingwindow3.tasks;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.widget.RelativeLayout.*;
import android.graphics.*;
import android.content.*;

public class WindowHolder implements Comparable<WindowHolder>
{
	/* This is a class to keep cache for
	each window's data and to get/set layout
	*/
	public final Window window;
	public int x = 0;
	public int y = 0;
	public int width = -1;
	public int height = -1;
	public int taskId = -1;
	public int SnapGravity = 0; //Gravity flag, eg TOP | LEFT for TopLeft window
    public float dim;
    public float alpha = 1;
	public boolean isMaximized = false;
	public boolean isSnapped = false;
	
	
	public WindowHolder() {
		this(null, 0, 0, 500, 500);
	}
	
	public WindowHolder(final Window mWindow) {
		window = mWindow;
		pullFromWindow(mWindow);
		restoreSnap();
	}
	
	public WindowHolder(final Window mWindow, 
		int mX, int mY, int mWidth, int mHeight) {
			window = setWindow(mWindow);
			width = mWidth;
			height = mHeight;
			isMaximized = (width==-1)&&(height==-1);
			x = isMaximized?0:mX;
			y = isMaximized?0:mY;
		}
		
	public WindowHolder (final Window mWindow, 
		int mX, int mY, int mWidth, int mHeight, int mTaskId) {
		this(mWindow, mX, mY, mWidth, mHeight);
		taskId = mTaskId;
		}
	
	public WindowHolder (final Window mWindow, final WindowHolder mWindowHolder) {
		window = setWindow(mWindow);
		copy(mWindowHolder);
	}
	
	public WindowHolder (final Window mWindow, final WindowHolder mWindowHolder, int mTaskId) {
		this(mWindow, mWindowHolder);
		taskId = mTaskId;
	}
		
	private Window setWindow(final Window mWindow) {
		if(mWindow==null)
			return null;
		/* Fix Chrome dim */
        if(!mWindow.getContext().getPackageName().startsWith("com.android.chrome"))
            mWindow.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
		return mWindow;
	}
	
	public boolean hasSameWindow(final WindowHolder mWindowHolder) {
		if(mWindowHolder==null)
			return false;
		return window == mWindowHolder.window;
	}
	
	public boolean hasSamePosition(final WindowHolder mWindowHolder) {
		if(mWindowHolder==null)
			return false;
		return (x == mWindowHolder.x)&&(y == mWindowHolder.y);
	}
	
	public boolean hasSameSize(final WindowHolder mWindowHolder) {
		if(mWindowHolder==null)
			return false;
		return (width == mWindowHolder.width)&&(height == mWindowHolder.height);
	}
	
	public boolean hasSameLayout(final WindowHolder mWindowHolder) {
		if(mWindowHolder==null)
			return false;
		return (x == mWindowHolder.x)&&(y == mWindowHolder.y)
			&&(width == mWindowHolder.width)&&(height == mWindowHolder.height);
	}
	
	public void copy (final WindowHolder sWindowHolder){
		alpha = sWindowHolder.alpha;
        width = sWindowHolder.width;
        height = sWindowHolder.height;
        x = sWindowHolder.x;
        y = sWindowHolder.y;
		isSnapped = sWindowHolder.isSnapped;
		isMaximized = sWindowHolder.isMaximized;
		SnapGravity = sWindowHolder.SnapGravity;
	}
	
	public void position(int mX, int mY) {
		x = mX;
		y = mY;
	}
	
	public void size(int mWidth, int mHeight) {
		width = mWidth;
		height = mHeight;
	}
	
	public void toggleXY(){
		position(y,x);
		size(height, width);
		//restoreSnap();
	}
	
	public void resize(int deltax, int deltay, final Context mContext) {
		final Point screenSize = Util.getScreenSize(mContext);
		if(width == WindowManager.LayoutParams.MATCH_PARENT && deltax != 0)
			width = screenSize.x;
		if(height == WindowManager.LayoutParams.MATCH_PARENT && deltay != 0)
			height = screenSize.y;
		int mWidth = width + deltax;
		int mHeight = height + deltay;
		size(mWidth, mHeight);
	}
	
	private boolean isIncreasing(){
		WindowManager.LayoutParams lp = window.getAttributes();
		if(height>lp.height||width>lp.width)
			return true;
		return false;
	}
	
	public void pushToWindow() {
		pushToWindow(window);
	}
	
	public void pushToWindow(final Window sWindow){
		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
        if(sWindow==null) return;
		//sWindow.setLayout(width, height);
        WindowManager.LayoutParams mWParams = sWindow.getAttributes();
        mWParams.x = x;
        mWParams.y = y;
        mWParams.alpha = alpha;
        mWParams.width = width==0?ViewGroup.LayoutParams.MATCH_PARENT:width;
        mWParams.height = height==0?ViewGroup.LayoutParams.MATCH_PARENT:height;
		mWParams.dimAmount = dim;
        mWParams.gravity = Gravity.TOP | Gravity.LEFT;
		//sWindow.getCallback().onWindowAttributesChanged(mWParams);
		//Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
        sWindow.setAttributes(mWParams);
    }
	
	//get current window layout params
    public void pullFromWindow(Window mWindow){
        WindowManager.LayoutParams mWParams = mWindow.getAttributes();
        x = mWParams.x;
        y = mWParams.y;
        alpha = mWParams.alpha;
        width = mWParams.width;
        height = mWParams.height;
        //cachedOrientation = Util.getScreenOrientation(mActivity);
    }

    public void setMaximized(){
        width = ViewGroup.LayoutParams.MATCH_PARENT;
        height = ViewGroup.LayoutParams.MATCH_PARENT;
        x=0;
        y=0;
        SnapGravity=Gravity.FILL;
        isMaximized=true;
    }
	
	public void updateSnap(int newSnap){
        SnapGravity = newSnap;
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
	
    //restore/copy precached data

    public void restore(final WindowHolder sWindowHolder){
        x = sWindowHolder.x;
        y = sWindowHolder.y;
        width = sWindowHolder.width;
        height = sWindowHolder.height;
		if(width==0) width=-1;
		if(height==0) height = -1;
        SnapGravity = sWindowHolder.SnapGravity;
        isSnapped = sWindowHolder.isSnapped;
		isMaximized = sWindowHolder.isMaximized;
    }
	
	public boolean updateLayoutIfNeeded(final int newx, final int newy, final int newwidth, final int newheight) {
		boolean change = false;
		if((newx != x)||(newy != y)) {
			change = true;
			position(newx, newy);
		}
		if((newwidth != width)||(newheight != height)) {
			change = true;
			size(newwidth, newheight);
		}
		return change;
	}
	
//	public void syncLayout(){
//		if(!isIncreasing()){
//			for(Window w : mWindows){
//				pushToWindow(w);
//			}
//		}
//		else {
//			for(int i = mWindows.size()-1;i>=0;i--)
//				pushToWindow(mWindows.get(i));
//		}
//
//
//	}
	
	public boolean updateByFloatDot(final int fdx, final int fdy, 
								 final int screenWidth, final int screenHeight){
		if(!isSnapped)
			return false;
		int newx = 0;
		int newy = 0;
		int newwidth = -1;
		int newheight = -1;
		if(Util.isFlag(SnapGravity, Gravity.RIGHT)) {
			newx = fdx;
			newwidth = screenWidth - fdx + 1;
		}
		else if(Util.isFlag(SnapGravity, Gravity.LEFT)) {
			newwidth = fdx + 1;
		}
		if(Util.isFlag(SnapGravity, Gravity.BOTTOM)) {
			newy = fdy;
			newheight = screenHeight - fdy + 1;
		}
		else if(Util.isFlag(SnapGravity, Gravity.TOP)) {
			newheight = fdy + 1;
		}
		return updateLayoutIfNeeded(newx, newy, newwidth, newheight);
	}

	@Override
	public int compareTo(WindowHolder p1)
	{
		return (p1!=null&&window==p1.window)?0:-1;
	}
	
}
