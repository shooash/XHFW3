package com.zst.xposed.halo.floatingwindow3.floatdot;
import android.view.*;
import android.widget.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.graphics.*;
import android.content.*;
import android.util.*;
import android.os.*;
import android.view.View.*;

public class FloatDot
{

	private WindowManager mWindowManager;
    private ImageView image;
	private int mScreenWidth;
	private int mScreenHeight;
	private int mCircleDiameter = 26;
	private int lastOrientation;
	private boolean mMoved;
	private float mAlpha = Common.DEFAULT_FLOATDOT_ALPHA;
	private boolean mPrefCommunication = false;
	public boolean isDrawableRes = false;
	public int mDrawableRes;
	//private boolean secondTouch = false;
	//private long checkTime;
	//private long checkBroadcastTime;

	/*Float dot commons*/

	public static final String REFRESH_FLOAT_DOT_POSITION = Common.REFRESH_FLOAT_DOT_POSITION;
	public static final String INTENT_FLOAT_DOT_EXTRA = Common.INTENT_FLOAT_DOT_EXTRA;
	private int TOUCH_SENSITIVITY;
	private final Context mContext;
	public int mColor = Color.BLACK;
	public int mColorInner = Color.BLACK;
	private boolean mViewOn = false;
	public Point mCoordinates = new Point((mScreenWidth / 2) - (mCircleDiameter / 2),(mScreenHeight / 2) - (mCircleDiameter / 2));
	//private int[] coordinates = new int[] {(mScreenWidth / 2) - (mCircleDiameter / 2),(mScreenHeight / 2) - (mCircleDiameter / 2)};
	private FloatLauncher mFloatLauncher;
	
	final WindowManager.LayoutParams paramsF = new WindowManager.LayoutParams(
		mCircleDiameter,
		mCircleDiameter,
		WindowManager.LayoutParams.TYPE_PHONE,
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
		WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
		WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
		WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
		WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
		WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
		PixelFormat.TRANSLUCENT);

	public FloatDot(Context sContext, FloatLauncher sFloatLauncher, int sCircleDiameter, int initx, int inity, int sColor, int sColorInner){
		mContext = sContext;
		mFloatLauncher = sFloatLauncher;
		mCircleDiameter = Util.realDp(sCircleDiameter, mContext);
		TOUCH_SENSITIVITY = mCircleDiameter/2;
		mColor = sColor;
		mColorInner = sColorInner;
		mCoordinates.x = initx;
		mCoordinates.y = inity;
		image = new ImageView(mContext);
	}
	
    public FloatDot(Context sContext, FloatLauncher sFloatLauncher) {
		mContext = sContext;
		mCircleDiameter = Util.realDp(mCircleDiameter, mContext);
		mFloatLauncher = sFloatLauncher;
		
		TOUCH_SENSITIVITY = mCircleDiameter/2;
		image = new ImageView(mContext);
		
	}
	
	
	public void setColor(int sColor, int sColorInner){
		mColor = sColor;
		mColorInner = sColorInner;
	}
	
	public void setColor(int sColor){
		mColor = sColor;
		mColorInner = sColor;
	}
	
	public void setColor(int newcolor, boolean inner){
		if(inner)
			mColorInner = newcolor;
		else
			mColor = newcolor;
	}
	
	public void redrawView(){
		//updateDot();
		if(!isDrawableRes) image.setImageDrawable(Util.makeDoubleCircle(mColor, mColorInner, mCircleDiameter, mCircleDiameter/4));
		paramsF.x = mCoordinates.x;
		paramsF.y = mCoordinates.y;
		paramsF.width = mCircleDiameter;
		paramsF.height = mCircleDiameter;
		paramsF.alpha = mAlpha;
		mWindowManager.updateViewLayout(image, paramsF);
		
	}
	
	public void setAlpha(float sAlpha){
		if(sAlpha!=0)
			mAlpha = sAlpha;
	}
	
	public void setPosition(int x, int y){
		mCoordinates.x = x;
		mCoordinates.y = y;
	}
	
	public void setDrawableResource(int res){
		mDrawableRes = res;
		isDrawableRes = true;
	}
	
	public void setSize(int sCircleDiameter){
		mCircleDiameter = Util.realDp(sCircleDiameter, mContext);
		TOUCH_SENSITIVITY = mCircleDiameter/2;
	}
	
	public void enableCommunication(){
		mPrefCommunication = true;
	}
	
	public void showDragger(boolean show){
		image.setVisibility(show?View.VISIBLE:View.INVISIBLE);
		if(mPrefCommunication&&show)
			sendPosition(image);
	}

	public void putDragger(){
		//if(mViewOn) return;
		
		if(isDrawableRes)
			image.setImageResource(mDrawableRes);
		else
			image.setImageDrawable(Util.makeDoubleCircle(mColor, mColorInner, mCircleDiameter, mCircleDiameter/4));
		image.setVisibility(View.INVISIBLE);
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        paramsF.gravity = Gravity.TOP | Gravity.LEFT;
		paramsF.windowAnimations = android.R.style.Animation_Translucent;
		setLayout();
		registerListener();
		mWindowManager.addView(image, paramsF);
		mFloatLauncher.setAnchor(image);
		mViewOn = true;
	}
	
	public void removeDot(){
		try{
			mWindowManager.removeView(image);
			} catch(Throwable t){}
	}
	
	public void registerContextMenuOnFloatDot(){
		image.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View p1)
				{
					String mPackageName = Util.getTopAppPackageName(mContext);
					mFloatLauncher.showSubMenu(image, mContext, mCoordinates.x, mCoordinates.y-mScreenHeight/2, mCircleDiameter, 0, mPackageName,
						new String[]{mContext.getString(R.string.floatdot_action_movable),
							mContext.getString(R.string.floatdot_action_fullscreen),
								(mFloatLauncher.isInFavorites(mPackageName)?mContext.getString(R.string.floatdot_action_unfav):mContext.getString(R.string.floatdot_action_fav))},
						new int[] {mFloatLauncher.ACTION_HALOFY, mFloatLauncher.ACTION_UNHALOFY, (mFloatLauncher.isInFavorites(mPackageName)?mFloatLauncher.ACTION_REMOVE_FROM_FAVORITES:mFloatLauncher.ACTION_ADD_TO_FAVORITES)});
					return true;
				}


			});
		}
	
	public void updateDot(){
		boolean show = image.getVisibility()==View.VISIBLE;
		removeDot();
		setLayout();
		putDragger();
		showDragger(show);
	}
	
	private void setLayout(){
		refreshScreenSize();
		paramsF.x = mCoordinates.x;
		paramsF.y = mCoordinates.y;
		paramsF.width = mCircleDiameter;
		paramsF.height = mCircleDiameter;
		paramsF.alpha = mAlpha;
		lastOrientation = Util.getScreenOrientation(mContext);
	}

	private void menuLauncher(View anchor) {
		if(mFloatLauncher.dismissedTime+500>SystemClock.uptimeMillis())
			return;
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
//								int x = (int) event.getRawX();
//								int y = (int) event.getRawY();
//								if(x<paramsF.x || x>paramsF.x+mCircleDiameter 
//									|| y < paramsF.y || y > paramsF.y+mCircleDiameter)
//									mFloatLauncher.popupWin.dismiss();
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
								//moveAnim(v, 100, 100);
								//getAbsoluteCoordinates(v);
								if(mPrefCommunication)
									sendPosition(v);
								if(Math.abs(mCoordinates.x + mCircleDiameter/2 - initialTouchX)<mCircleDiameter/2 && Math.abs(mCoordinates.y + mCircleDiameter/2 - initialTouchY) < TOUCH_SENSITIVITY)
									menuLauncher(v);
								break;
							case MotionEvent.ACTION_MOVE:
								paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
								paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);

								mCoordinates.x = paramsF.x;
								mCoordinates.y = paramsF.y;
								mWindowManager.updateViewLayout(v, paramsF);
								//checkTime = SystemClock.uptimeMillis();
							
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
		int[] coordinates = new int[2];
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
	}

	public void rotatePositionByOrientation(){
		int newOrientation = Util.getScreenOrientation(mContext);
		if(newOrientation==lastOrientation) return;
		lastOrientation = newOrientation;
		WindowManager.LayoutParams lp = (WindowManager.LayoutParams)image.getLayoutParams();
		int newx = lp.y; // paramsF.y;
		int newy = lp.x; // paramsF
		lp.x = newx;
		lp.y = newy;
		mCoordinates.x = newx;
		mCoordinates.y = newy;
		mWindowManager.updateViewLayout(image, lp);
	}

	public void sendPosition(View v){
		sendPosition(getAbsoluteCoordinates(v));
	}

	public void sendPosition(int[] coordinates){
		Intent intent = new Intent(REFRESH_FLOAT_DOT_POSITION);
		intent.putExtra(INTENT_FLOAT_DOT_EXTRA, coordinates);
		mContext.sendBroadcast(intent);
	}

	private void refreshScreenSize(){
		//final WindowManager wm = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(metrics);

		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}

	private void moveAnim(final View view, final int amountToMoveRight, final int amountToMoveDown){
		view.animate().x(50).y(100);
//		TranslateAnimation anim = new TranslateAnimation(0, amountToMoveRight, 0, amountToMoveDown);
//		anim.setDuration(1000);
//
//		anim.setAnimationListener(new TranslateAnimation.AnimationListener() {
//
//				@Override
//				public void onAnimationStart(Animation animation) { }
//
//				@Override
//				public void onAnimationRepeat(Animation animation) { }
//
//				@Override
//				public void onAnimationEnd(Animation animation) 
//				{
//					FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)view.getLayoutParams();
//					params.topMargin += amountToMoveDown;
//					params.leftMargin += amountToMoveRight;
//					view.setLayoutParams(params);
//				}
//			});
//
//		view.startAnimation(anim);
	}

}
