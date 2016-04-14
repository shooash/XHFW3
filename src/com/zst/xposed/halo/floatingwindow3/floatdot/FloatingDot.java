package com.zst.xposed.halo.floatingwindow3.floatdot;
import android.view.*;
import android.widget.*;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.content.*;
import android.util.*;
import android.media.*;
import java.lang.annotation.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.widget.TableLayout.*;
import com.zst.xposed.halo.floatingwindow3.helpers.*;

public class FloatingDot implements Runnable
{

	@Override
	public void run()
	{
		putDragger();
		//Toast.makeText(mContext, "toogleDragger", Toast.LENGTH_SHORT).show();
	}
	
    private WindowManager mWindowManager;
    private ImageView image;
	private int mScreenWidth;
	private int mScreenHeight;
	private int mCircleDiameter = 42;
	private boolean secondTouch = false;
	private long checkTime;
	private long checkBroadcastTime;

	/*Float dot commons*/

	public static final String REFRESH_FLOAT_DOT_POSITION = Common.REFRESH_FLOAT_DOT_POSITION;
	public static final String INTENT_FLOAT_DOT_EXTRA = Common.INTENT_FLOAT_DOT_EXTRA;

	final WindowManager.LayoutParams paramsF = new WindowManager.LayoutParams(
		WindowManager.LayoutParams.WRAP_CONTENT,
		WindowManager.LayoutParams.WRAP_CONTENT,
		WindowManager.LayoutParams.TYPE_PHONE,
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
		PixelFormat.TRANSLUCENT);
	Context mContext;
	boolean mViewOn = false;
	int[] coordinates = new int[2];

    public FloatingDot(Context sContext) {
		mContext = sContext;
		mCircleDiameter = Util.realDp(24, mContext);
	}

	
	private void setLayout(){
		refreshScreenSize();
		int[] coordinates = new int[] {(mScreenWidth / 2) - (mCircleDiameter / 2),(mScreenHeight / 2) - (mCircleDiameter / 2)};
		//.getLocationInWindow(coordinates);
		
		paramsF.x = coordinates[0];
		paramsF.y = coordinates[1];
		paramsF.width = mCircleDiameter;
		paramsF.height = mCircleDiameter;
	}
	
	public void showDragger(boolean show){
		if(show) image.setVisibility(View.VISIBLE);
		else
			image.setVisibility(View.INVISIBLE);
		//mWindowManager.updateViewLayout(image, paramsF);
	}

	public void putDragger(){
		//if(mViewOn) return;
		image = new ImageView(mContext);
       // image.setImageResource(R.drawable.multiwindow_dragger_press_ud);
		image.setBackgroundResource(R.drawable.multiwindow_dragger_press_ud);
		image.setVisibility(View.INVISIBLE);
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);

        paramsF.gravity = Gravity.TOP | Gravity.LEFT;
		setLayout();
        /*paramsF.x=0;
		 paramsF.y=100;*/
		registerListener();
		
		mWindowManager.addView(image, paramsF);
		mViewOn = true;
		//sendPosition(image);
	}
	
	public void hideDragger(){
		hideDragger(image);
	}
	
	public void hideDragger(View v){
		//if(!mViewOn) return;
		v.setVisibility(View.GONE);
		mWindowManager.removeView(v);
		mViewOn = false;
	}

	private void registerListener(){
        try{
            image.setOnTouchListener(new View.OnTouchListener() {
					WindowManager.LayoutParams paramsT = paramsF;
					private int initialX;
					private int initialY;
					private float initialTouchX;
					private float initialTouchY;
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						switch(event.getAction()){
							case MotionEvent.ACTION_DOWN:
								initialX = paramsF.x;
								initialY = paramsF.y;
								initialTouchX = event.getRawX();
								initialTouchY = event.getRawY();
								checkTime = SystemClock.uptimeMillis();
								break;
							case MotionEvent.ACTION_UP:
								//if(!secondTouch) secondTouch = true;
								//else {
								//mWindowManager.removeView(v);
								sendPosition(v);
								if(SystemClock.uptimeMillis()-checkTime>3000){
									hideDragger(v);
									return false;
								}
								//setLayout();
								//putDragger();
								//secondTouch = false;

								//}
								break;
							case MotionEvent.ACTION_MOVE:
								paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
								paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
								
								mWindowManager.updateViewLayout(v, paramsF);
								checkTime = SystemClock.uptimeMillis();
								//sendPosition(v);
								break;
						}
						return false;
					}
				});
        } catch (Exception e){
            e.printStackTrace();
        }
    }
	
	
	public int[] getAbsoluteCoordinates(){

		return getAbsoluteCoordinates(image);
	}
	
	public int[] getAbsoluteCoordinates(View v){
		refreshScreenSize();
		//{paramsF.x + (mCircleDiameter),paramsF.y + (mCircleDiameter)};
		v.getLocationOnScreen(coordinates);
		coordinates[0]+= mCircleDiameter/2;
		coordinates[1]+= mCircleDiameter/2;
		return coordinates;
	}
	
	public void rotatePosition(int rotation){
		int widthPercent = 100*paramsF.x/mScreenWidth;
		int heightPercent = 100*paramsF.y/mScreenHeight;
		refreshScreenSize();
		paramsF.x = widthPercent*mScreenWidth/100;
		paramsF.y = heightPercent*mScreenHeight/100;
		//coordinates = new int[]{coordinates[Util.rollInt(0,1,-rotation)], coordinates[Util.rollInt(0,1,-rotation+1)]};
		mWindowManager.updateViewLayout(image, paramsF);
		sendPosition(image);
	}

	public void sendPosition(View v){
		sendPosition(getAbsoluteCoordinates(v));
	}
	
	public void sendPosition(int[] coordinates){

		//if(SystemClock.uptimeMillis() - checkBroadcastTime < 250) return;
		//checkBroadcastTime = SystemClock.uptimeMillis();
		Intent intent = new Intent(REFRESH_FLOAT_DOT_POSITION);

		intent.putExtra(INTENT_FLOAT_DOT_EXTRA, coordinates);
		// set package so this is broadcasted only to our own package
		mContext.sendBroadcast(intent);
	}

	private void refreshScreenSize(){
		//final WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(metrics);

		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}

}
