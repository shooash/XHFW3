package com.zst.xposed.halo.floatingwindow3;

import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import java.util.logging.*;
 
public class Movable implements View.OnTouchListener {
        final Window mWindow;
        final LayoutParams param;
        final boolean mReturn;
        
        private static Float screenX ;
    	private static Float screenY ;
    	private static Float viewX ;
    	private static Float viewY ;
    	private static Float leftFromScreen ;
    	private static Float topFromScreen ;
    	private View offsetView;
       
        public Movable(Window window, boolean return_value) {
                mWindow=window;
        		param = mWindow.getAttributes(); 
        		mReturn = return_value;
        }
        
        public Movable(Window window, View v){
        	this(window, false);
        	offsetView = v;
        }
        
        @Override
        public boolean onTouch(View v, MotionEvent event) {
        	switch (event.getAction()){
        	case MotionEvent.ACTION_DOWN:
        		viewX = event.getX();
    			viewY = event.getY();
        		if (offsetView != null) {
        			int[] location = {0,0};
        			offsetView.getLocationInWindow(location);
        			viewX = viewX + location[0];
        			viewY = viewY + location[1];
        		}
                break;
        	case MotionEvent.ACTION_MOVE:
        		screenX = event.getRawX();
        		screenY = event.getRawY();
        		leftFromScreen = (screenX - viewX);
        		topFromScreen = (screenY - viewY);
        		//mWindow.setGravity(Gravity.LEFT | Gravity.TOP);
				if(MovableWindow.mWindowHolder.isSnapped)
					MovableWindow.unsnap();
				else 
					MovableWindow.move(leftFromScreen.intValue(),topFromScreen.intValue());
        		break;
        	}
        	if (MovableWindow.mAeroSnap != null) {
        		MovableWindow.mAeroSnap.dispatchTouchEvent(event);
        	}
        	return mReturn;
        }
      
}
