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
import android.widget.TableLayout.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.graphics.drawable.*;

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
	private int mCircleDiameter = 26;
	private int lastOrientation;
	private boolean mMoved;
	//private boolean secondTouch = false;
	//private long checkTime;
	//private long checkBroadcastTime;

	/*Float dot commons*/

	public static final String REFRESH_FLOAT_DOT_POSITION = Common.REFRESH_FLOAT_DOT_POSITION;
	public static final String INTENT_FLOAT_DOT_EXTRA = Common.INTENT_FLOAT_DOT_EXTRA;

	final WindowManager.LayoutParams paramsF = new WindowManager.LayoutParams(
		mCircleDiameter,
		mCircleDiameter,
		WindowManager.LayoutParams.TYPE_PHONE,
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
		WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
		WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
		PixelFormat.TRANSLUCENT);
	Context mContext;
	int mColor = Color.BLACK;
	boolean mViewOn = false;
	int[] coordinates = new int[2];
	FloatLauncher mFloatLauncher;

    public FloatingDot(Context sContext) {
		mContext = sContext;
		mCircleDiameter = Util.realDp(mCircleDiameter, mContext);
		mFloatLauncher = new FloatLauncher(mContext);
	}


	private void setLayout(){
		refreshScreenSize();
		coordinates = new int[] {(mScreenWidth / 2) - (mCircleDiameter / 2),(mScreenHeight / 2) - (mCircleDiameter / 2)};
		paramsF.x = coordinates[0];
		paramsF.y = coordinates[1];
		paramsF.width = mCircleDiameter;
		paramsF.height = mCircleDiameter;
		paramsF.alpha = 0.5f;
		lastOrientation = Util.getScreenOrientation(mContext);
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
		//image.setBackgroundResource(R.drawable.multiwindow_dragger_press_ud);
		//image.setImageDrawable(Util.makeCircle(mColor, mCircleDiameter));
		image.setImageDrawable(Util.makeDoubleCircle(mColor, Color.RED, mCircleDiameter, mCircleDiameter/3));

		image.setVisibility(View.INVISIBLE);
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);

        paramsF.gravity = Gravity.TOP | Gravity.LEFT;
		paramsF.windowAnimations = android.R.style.Animation_Translucent;
		setLayout();
		registerListener();
		//image.setOnClickListener(menuLauncher);
		mWindowManager.addView(image, paramsF);
		mViewOn = true;
		//sendPosition(image);
	}

	private void menuLauncher(View anchor) {
			mFloatLauncher.showMenu(anchor, paramsF, mCircleDiameter);
		}
	
	//private void fillMenu(PopupMenu menu)

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
								
								//checkTime = SystemClock.uptimeMillis();
								break;
							case MotionEvent.ACTION_UP:
								//if(!secondTouch) secondTouch = true;
								//else {
								//mWindowManager.removeView(v);
								sendPosition(v);
								if(Math.abs(coordinates[0] - initialTouchX)<mCircleDiameter/2 && Math.abs(coordinates[1] - initialTouchY) < mCircleDiameter/2){
									menuLauncher(v);
									break;
								}
//								if(SystemClock.uptimeMillis()-checkTime>3000){
//									hideDragger(v);
//									return false;
//								}
								//setLayout();
								//putDragger();
								//secondTouch = false;

								//}
								break;
							case MotionEvent.ACTION_MOVE:
								
								paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
								paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
								
								coordinates[0] = paramsF.x;
								coordinates[1] = paramsF.y;
								mWindowManager.updateViewLayout(v, paramsF);
								//checkTime = SystemClock.uptimeMillis();
								//sendPosition(v);
								mMoved = true;
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

		//return getAbsoluteCoordinates(image);
		return new int[]{paramsF.x+mCircleDiameter/2,paramsF.y+mCircleDiameter/2};
	}

	public int[] getAbsoluteCoordinates(View v){
		refreshScreenSize();
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
	
	public void rotatePositionByOrientation(){
		int newOrientation = Util.getScreenOrientation(mContext);
		if(newOrientation==lastOrientation) return;
		lastOrientation = newOrientation;
		//if(View.GONE == image.getVisibility()) return;
//		int newx = coordinates[1]; // paramsF.y;
//		int newy = coordinates[0]; // paramsF.x;
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams)image.getLayoutParams();
		int newx = lp.y; // paramsF.y;
		int newy = lp.x; // paramsF
		lp.x = newx;
		lp.y = newy;
		coordinates[0] = newx;
		coordinates[1] = newy;
		mWindowManager.updateViewLayout(image, lp);
		sendPosition(coordinates);
	}
	
//	public void sendPosition(){
//		sendPosition(new int[]{paramsF.x+mCircleDiameter/2,paramsF.y+mCircleDiameter/2});
//	}
//
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
