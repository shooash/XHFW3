package com.zst.xposed.halo.floatingwindow3;

/**
 * Created by andrey on 21.04.16.
 */
import android.app.Activity;
import android.view.Gravity;
import android.view.Window;

import de.robv.android.xposed.XSharedPreferences;
import android.view.*;

import java.util.ArrayList;
import android.os.*;
import android.content.*;
import android.widget.*;

public class WindowHolder3{
    public boolean isSnapped = false;
    public boolean isMaximized = false;
    public boolean serviceConnected = false;
	public boolean isHiddenFromRecents = false;
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
    public static ArrayList<Window> mWindows = new ArrayList<Window>();
    public String packageName;
    public Activity mActivity;
    //public boolean isSet=false;
    //public boolean mReceiverRegistered = false;

    public WindowHolder3(Activity sActivity, XSharedPreferences mPref){
        mActivity = sActivity;
        mPref.reload();
        alpha = mPref.getFloat(Common.KEY_ALPHA, Common.DEFAULT_ALPHA);
        dim = mPref.getFloat(Common.KEY_DIM, Common.DEFAULT_DIM);
        cachedOrientation=Util.getScreenOrientation(mActivity.getApplicationContext());
       // cachedRotation = Util.getDisplayRotation(mActivity);
		/*TODO: Get use of EXTRA_SNAP extras to keep snap gravity*/
		/*if(mActivity.getIntent().hasExtra(Common.EXTRA_SNAP)) SnapGravity = mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP, 0);
			else */
        SnapGravity = Compatibility.snapSideToGravity(mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP_SIDE, Compatibility.AeroSnap.SNAP_NONE));
        //mActivity.getIntent().getIntExtra(Common.EXTRA_SNAP, 0);
        isSnapped=(SnapGravity != 0);
        isMaximized=(SnapGravity == Gravity.FILL);
        setWindow(mActivity);
        //updateWindow();
		packageName = mActivity.getCallingPackage();
        if(packageName==null)
			packageName = mActivity.getPackageName();
		int flags = mActivity.getIntent().getFlags();
		
		isHiddenFromRecents = Util.isFlag(flags, Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
					|| Util.isFlag(flags, Intent.FLAG_ACTIVITY_NO_HISTORY);
    }
	
	/* constructor to clone values*/
	public WindowHolder3 (final WindowHolder3 sWindowHolder){
		alpha = sWindowHolder.alpha;
        width = sWindowHolder.width;
        height = sWindowHolder.height;
        x = sWindowHolder.x;
        y = sWindowHolder.y;
	}
	
	public void setWindow (Activity sActivity){
        mActivity = sActivity;
		//if(!mWindows.contains(mWindow)) mWindows.add(mWindow);
        setWindow(sActivity.getWindow());
	}

    public void setWindow (Window sWindow){
        mWindow = sWindow;
		if(!mWindows.contains(mWindow)) 
			mWindows.add(mWindow);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
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

    

    public void setMaximized(){
        width = ViewGroup.LayoutParams.MATCH_PARENT;
        height = ViewGroup.LayoutParams.MATCH_PARENT;
        x=0;
        y=0;
        SnapGravity=Gravity.FILL;
        isMaximized=true;
    }

    //restore/copy precached data
    public void restore(WindowHolder3 sWindowHolder){
        alpha = sWindowHolder.alpha;
        width = sWindowHolder.width;
        height = sWindowHolder.height;
		if(width==0) width=-1;
		if(height==0) height = -1;
        x = sWindowHolder.x;
        y = sWindowHolder.y;
        isMaximized = false;
        //isFloating = sWindowHolder.isFloating;
        isSnapped = false;
        SnapGravity = 0;
        //pushToWindow();
    }

    public void restore(SnapWindowHolder sSnapWindowHolder){
        x = sSnapWindowHolder.x;
        y = sSnapWindowHolder.y;
        width = sSnapWindowHolder.width;
        height = sSnapWindowHolder.height;
		if(width==0) width=-1;
		if(height==0) height = -1;
        SnapGravity = sSnapWindowHolder.SnapGravity;
        isSnapped = true;
    }

    //set current window to saved layout params
    public void pushToWindow(){
		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
        if(mWindow.isFloating()) return;
        WindowManager.LayoutParams mWParams = mWindow.getAttributes();
        mWParams.x = x;
        mWParams.y = y;
        mWParams.alpha = alpha;
        mWParams.width = width;
        mWParams.height = height;
        mWParams.dimAmount = dim;
        mWParams.gravity = Gravity.TOP | Gravity.LEFT;
		//mWindow.getCallback().onWindowAttributesChanged(mWParams);
        //Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
        mWindow.setAttributes(mWParams);
    }
	
	public void asyncPushToWindow(final Window sWindow){
		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
		if(mWindow.isFloating()) return;
		sWindow.setLayout(width, height);
		new Handler().post(new Runnable(){
				@Override
				public void run()
				{
					WindowManager.LayoutParams mWParams = sWindow.getAttributes();
					mWParams.x = x;
					mWParams.y = y;
					mWParams.alpha = alpha;
//					mWParams.width = width;
//					mWParams.height = height;
					mWParams.dimAmount = dim;
					//sWindow.getCallback().onWindowAttributesChanged(mWParams);
					Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
					sWindow.setAttributes(mWParams);
				}
		});
    }
	
	
//	public void pushToPhoneWindow(Window sWindow){
//		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
//        if(sWindow.isFloating()) return;
//		int titleHeight = MovableWindow.mOverlayView.getTitleBarHeight();
//        WindowManager.LayoutParams mWParams = sWindow.getAttributes();
//        mWParams.x = x;
//        mWParams.y = y+titleHeight;
//        mWParams.alpha = alpha;
//        mWParams.width = width;
//        mWParams.height = height-titleHeight;
//        mWParams.dimAmount = dim;
//        mWParams.gravity = Gravity.TOP | Gravity.LEFT;
//        //Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
//        sWindow.setAttributes(mWParams);
//    }

    public void pushToWindow(Window sWindow){
		/*FIX for floating dialogs that shouldn't be treated as movable or halo windows*/
        if(sWindow==null || sWindow.isFloating()) return;
		//sWindow.setLayout(width, height);
        WindowManager.LayoutParams mWParams = sWindow.getAttributes();
        mWParams.x = x;
        mWParams.y = y;
        mWParams.alpha = alpha;
        mWParams.width = width;
        mWParams.height = height;
		mWParams.dimAmount = dim;
        mWParams.gravity = Gravity.TOP | Gravity.LEFT;
		//sWindow.getCallback().onWindowAttributesChanged(mWParams);
		//Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
        sWindow.setAttributes(mWParams);
    }
	
	public void pushToWindowForce(Window sWindow){
        WindowManager.LayoutParams mWParams = sWindow.getAttributes();
        mWParams.x = x;
        mWParams.y = y;
        mWParams.alpha = alpha;
        mWParams.width = width;
        mWParams.height = height;
		mWParams.dimAmount = dim;
        mWParams.gravity = Gravity.TOP | Gravity.LEFT;
		//sWindow.getCallback().onWindowAttributesChanged(mWParams);
		//Util.addPrivateFlagNoMoveAnimationToLayoutParam(mWParams);
        sWindow.setAttributes(mWParams);
    }

    //get current window layout params
    public void pullFromWindow(){
        WindowManager.LayoutParams mWParams = mWindow.getAttributes();
        x = mWParams.x;
        y = mWParams.y;
        alpha = mWParams.alpha;
        width = mWParams.width;
        height = mWParams.height;
        //cachedOrientation = Util.getScreenOrientation(mActivity);
    }
	
	public void position(int newx, int newy){
		//Chrome layout fix
		if(packageName.startsWith("com.android.chrome")&&newx==0&&newy==0){
			if(x==0&&y==0){newx=1; newy=1;}
			else if(x==1&&y==1) {newx=0; newy=0;}
		}
		x = newx;
		y = newy;
	}
	
	public void size(int newwidth, int newheight){
		width = newwidth;
		height = newheight;
	}
	
	public void toggleXY(){
		position(y,x);
		size(height, width);
		restoreSnap();
	}
	
	public void syncLayout(){
		if(!isIncreasing()){
			for(Window w : mWindows){
				pushToWindow(w);
			}
		}
		else {
			for(int i = mWindows.size()-1;i>=0;i--)
				pushToWindow(mWindows.get(i));
		}
		
		
	}
	
	public void syncLayoutForce(){
		for(Window w : mWindows){
			pushToWindowForce(w);
		}
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
	
	private boolean isIncreasing(){
		WindowManager.LayoutParams lp = mWindow.getAttributes();
		if(height>lp.height||width>lp.width)
			return true;
		return false;
	}
	
	public void setTopMargin(Window window, int margin){
		if(window==null)
			return;
		try{
			final FrameLayout decorView = (FrameLayout) window.peekDecorView()
				.getRootView();
			final View child = decorView.getChildAt(0);
			FrameLayout.LayoutParams parammm = (FrameLayout.LayoutParams) child.getLayoutParams();
			parammm.setMargins(0, margin, 0, 0);
			child.setLayoutParams(parammm);
		} catch(Throwable t){}	
	}
	
	public void syncNewTopMargin(int margin){
		for(Window w : mWindows){
			setTopMargin(w, margin);
		}
	}

}

class SnapWindowHolder{
    public int x;
    public int y;
    public int height;
    public int width;
    public int SnapGravity;
    public boolean isSnapped = false;
    public void updateSnap(int newSnap){
        //if(SnapGravity == newSnap) return;
        SnapGravity = newSnap;
        isSnapped=(SnapGravity != 0);
        //isMaximized=(SnapGravity == Gravity.FILL);
    }
}
