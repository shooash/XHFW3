package com.zst.xposed.halo.floatingwindow3.overlays;
import android.annotation.*;
import android.widget.*;
import android.app.*;
import android.content.*;
import android.view.*;
import android.content.res.*;
import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.MainXposed;
import com.zst.xposed.halo.floatingwindow3.R;
import com.zst.xposed.halo.floatingwindow3.Util;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff.Mode;
import com.zst.xposed.halo.floatingwindow3.*;
import android.util.*;
import android.view.inputmethod.*;
import android.os.*;
import com.zst.xposed.halo.floatingwindow3.debug.*;

@SuppressLint("ViewConstructor")
public class OverlayView extends RelativeLayout
{
	public static final int ID_OVERLAY_VIEW = 1000000;

	/* Corner Button Actions Constants */
	private static final int ACTION_CLICK_TRIANGLE = 0x0;
	private static final int ACTION_LONGPRESS_TRIANGLE = 0x1;
	private static final int ACTION_CLICK_QUADRANT = 0x2;
	private static final int ACTION_LONGPRESS_QUADRANT = 0x3;
	
	
	//final Activity activity;
	final Context appContext;
	Window mWindow;

	// Views
	public final View mDragToMoveBar;
	public final View mQuadrant;
	public final View mTriangle;
	public final ImageView mBorderOutline;
	public final RelativeLayout mTitleBar;
	
	private boolean mLiveResizing;
	private boolean mTintedCorners;
	private String color_triangle = new String();
	private String color_quadrant = new String();
	private int triangle_size;
	private int quadrant_size;
	private boolean triangle_enabled;
	private boolean quadrant_enabled;
	private int triangle_action;
	private int quadrant_action;
	private boolean border_enabled;
	private boolean mTintedTitlebarBorder;
	private int border_color;
	private int border_thickness;
	private int border_focus_color;
	private int border_focus_thickness;
	
//	/* Title Bar */
	private boolean mTitleBarEnabled;
	private int mTitleBarHeight;
	private int mTitleBarDivider;
	private int mTitleBarIconType;
	private int mTitleBarColor = Color.BLACK;
	private boolean mTintedTitlebar;
	private float triangle_alpha;
	private float quadrant_alpha;
	public boolean titleBarVisible = true;
	
	
	public OverlayView (final Context mContext, final Window sWindow) {
		super(mContext);
		appContext = mContext;
		mWindow = sWindow;
		loadPrefs();
		try {
			Context module_context = appContext.createPackageContext(Common.THIS_MOD_PACKAGE_NAME,
																   Context.CONTEXT_IGNORE_SECURITY);
			LayoutInflater.from(module_context).inflate(R.layout.movable_window, this);
		} catch (Exception e) {
			XmlResourceParser parser = MainXposed.sModRes.getLayout(R.layout.movable_window);
			mWindow.getLayoutInflater().inflate(parser, this);
		}
		setId(ID_OVERLAY_VIEW);
		Util.setRootNamespace(this, false);
		mDragToMoveBar = Util.findViewByIdHelper(this, R.id.movable_action_bar, "movable_action_bar");
		mTriangle = Util.findViewByIdHelper(this, R.id.movable_corner, "movable_corner");
		mQuadrant = Util.findViewByIdHelper(this, R.id.movable_quadrant, "movable_quadrant");
		mBorderOutline = (ImageView) Util.findViewByIdHelper(this, R.id.movable_background, "movable_background");
		mTitleBar = (RelativeLayout) Util.findViewByIdHelper(this,
										 R.id.movable_titlebar, "movable_titlebar");
		if(mBorderOutline!=null)
			mBorderOutline.bringToFront();
		setCornersView();
		
		setDragActionBarVisibility(false, true);
//		initDragToMoveBar();
//
		if (border_enabled)
			setWindowBorder(border_color, border_thickness);
		if (mTitleBarEnabled && mTitleBar!=null)
			setTitleBarViewLegacy();
		// After initializing everything, set this to tell findViewById to skip
		// our layout. We do this to prevent id's conflicting with the current app.
		Util.setRootNamespace(this, true);
	}
	
	public void setWindow(final Window sWindow) {
		mWindow = sWindow;
	}
	
	private void loadPrefs(){
		// set preferences values
		mTitleBarIconType = MainXposed.mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
										 Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE);
		mTitleBarEnabled = mTitleBarIconType != 0;
		boolean titlebar_separator_enabled = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_ENABLED,
															  Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_ENABLED);
		mTitleBarHeight = !mTitleBarEnabled ? 0 : 
			Util.realDp(MainXposed.mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SIZE, 
					Common.DEFAULT_WINDOW_TITLEBAR_SIZE), appContext);
		mTitleBarDivider = !titlebar_separator_enabled ? 0 : Util.realDp(
			MainXposed.mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_SIZE,
						 Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_SIZE), appContext);
		mLiveResizing = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_RESIZING_LIVE_UPDATE,
										 Common.DEFAULT_WINDOW_RESIZING_LIVE_UPDATE);
		mTintedTitlebar = MainXposed.mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_ENABLED, Common.DEFAULT_TINTED_TITLEBAR_ENABLED);
		if(mTintedTitlebar) mTitleBarColor = getPrimaryDarkColor();
		mTintedCorners = mTintedTitlebar && MainXposed.mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_CORNER_TINT, 
										Common.DEFAULT_TINTED_TITLEBAR_CORNER_TINT);
		if(!mTintedCorners) {
			color_triangle = MainXposed.mPref.getString(Common.KEY_WINDOW_TRIANGLE_COLOR,
					Common.DEFAULT_WINDOW_TRIANGLE_COLOR);
			color_quadrant = MainXposed.mPref.getString(Common.KEY_WINDOW_QUADRANT_COLOR,
					Common.DEFAULT_WINDOW_QUADRANT_COLOR);
		}
		triangle_enabled = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_ENABLE,
					Common.DEFAULT_WINDOW_TRIANGLE_ENABLE);
		quadrant_enabled = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_ENABLE,
											Common.DEFAULT_WINDOW_QUADRANT_ENABLE);
					
		triangle_size = triangle_enabled?Util.dp(MainXposed.mPref.getInt(Common.KEY_WINDOW_TRIANGLE_SIZE,
					Common.DEFAULT_WINDOW_TRIANGLE_SIZE), appContext):0;
		quadrant_size = quadrant_enabled?Util.dp(MainXposed.mPref.getInt(Common.KEY_WINDOW_QUADRANT_SIZE,
					Common.DEFAULT_WINDOW_QUADRANT_SIZE), appContext):0;
		
		if(MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_DRAGGING_ENABLED,
					Common.DEFAULT_WINDOW_TRIANGLE_DRAGGING_ENABLED))
			triangle_action = Common.ACTION_DRAG;
		else if(MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_RESIZE_ENABLED,
					Common.DEFAULT_WINDOW_TRIANGLE_RESIZE_ENABLED))
			triangle_action = Common.ACTION_RESIZE_LEFT;
		if(MainXposed.mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_DRAGGING_ENABLED,
									   Common.DEFAULT_WINDOW_QUADRANT_DRAGGING_ENABLED))
			quadrant_action = Common.ACTION_DRAG;
		else if(MainXposed.mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_RESIZE_ENABLED,
											Common.DEFAULT_WINDOW_QUADRANT_RESIZE_ENABLED))
			quadrant_action = Common.ACTION_RESIZE_LEFT;
		
		triangle_alpha = MainXposed.mPref.getFloat(Common.KEY_WINDOW_TRIANGLE_ALPHA,
											  Common.DEFAULT_WINDOW_TRIANGLE_ALPHA);
		

		quadrant_alpha = MainXposed.mPref.getFloat(Common.KEY_WINDOW_QUADRANT_ALPHA,
											  Common.DEFAULT_WINDOW_QUADRANT_ALPHA);
		
		border_enabled = MainXposed.mPref.getBoolean(Common.KEY_WINDOW_BORDER_ENABLED,
													 Common.DEFAULT_WINDOW_BORDER_ENABLED);
		mTintedTitlebarBorder = mTintedTitlebar && MainXposed.mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_BORDER_TINT,
					Common.DEFAULT_TINTED_TITLEBAR_BORDER_TINT);
		if(mTintedTitlebarBorder)
			border_color = mTitleBarColor;
		else
			border_color = Color.parseColor("#" + MainXposed.mPref.getString(Common.KEY_WINDOW_BORDER_COLOR,
																 Common.DEFAULT_WINDOW_BORDER_COLOR));
		border_thickness = MainXposed.mPref.getInt(Common.KEY_WINDOW_BORDER_THICKNESS,
										   Common.DEFAULT_WINDOW_BORDER_THICKNESS);
		border_focus_thickness = Util.realDp(4, appContext);
		border_focus_color = Color.parseColor("#" + MainXposed.mPref.getString(Common.KEY_AERO_FOCUS_COLOR, Common.DEFAULT_AERO_FOCUS_COLOR));
	}
	
	private void setCornersView() {
		Drawable triangle_background = MainXposed.sModRes.getDrawable(R.drawable.movable_corner);
		Drawable quadrant_background = MainXposed.sModRes.getDrawable(R.drawable.movable_quadrant);
		if(mTintedCorners) {
			triangle_background.setColorFilter(mTitleBarColor, Mode.MULTIPLY);
			quadrant_background.setColorFilter(mTitleBarColor, Mode.MULTIPLY);
		}
		else {
			triangle_background.setColorFilter(Color.parseColor("#" + color_triangle), Mode.MULTIPLY);
			quadrant_background.setColorFilter(Color.parseColor("#" + color_quadrant), Mode.MULTIPLY);
		}
		quadrant_background.setAlpha((int) (quadrant_alpha * 255));
		triangle_background.setAlpha((int) (triangle_alpha * 255));
		Util.setBackgroundDrawable(mTriangle, triangle_background);
		Util.setBackgroundDrawable(mQuadrant, quadrant_background);
		mTriangle.getLayoutParams().width = triangle_size;
		mTriangle.getLayoutParams().height = triangle_size;
		mQuadrant.getLayoutParams().width = quadrant_size;
		mQuadrant.getLayoutParams().height = quadrant_size;
		
		mTriangle.setOnTouchListener(new View.OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent e)
				{
					ActivityHooks.taskStack.onUserAction(null, e, triangle_action, v);
					return false;
				}	
			});
			
		mQuadrant.setOnTouchListener(new View.OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent e)
				{
					ActivityHooks.taskStack.onUserAction(null, e, quadrant_action, v);
					return false;
				}	
		});
		mQuadrant.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View v)
				{
					//TODO ONCLICK ACTIONS
				}
		});
	}
	
	private void setDragActionBarVisibility(boolean visible, boolean with_corner) {
		mDragToMoveBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		if (with_corner) {
			mTriangle.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
			mQuadrant.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		}
	}
	
	public void setWindowBorder(int color, int thickness) {
		if (thickness == 0) {
			mBorderOutline.setBackgroundResource(0);
		} else {
			Util.setBackgroundDrawable(mBorderOutline, Util.makeOutline(color, thickness));
		}
	}
	
	public void setWindowBorder(){
		setWindowBorder(border_color, border_enabled?border_thickness:0);
	}
	
	public void setWindowBorderFocused() {
		setWindowBorder(border_focus_color, border_focus_thickness);
	}
	
	public void setTitleBarViewLegacy() {
		final View divider = Util.findViewByIdHelper(mTitleBar,
							R.id.movable_titlebar_line, "movable_titlebar_line");
		final TextView app_title = (TextView) Util.findViewByIdHelper(mTitleBar,
							R.id.movable_titlebar_appname, "movable_titlebar_appname");
		final ImageButton max_button = (ImageButton) Util.findViewByIdHelper(mTitleBar,
							R.id.movable_titlebar_max, "movable_titlebar_max");
		final ImageButton min_button = (ImageButton) Util.findViewByIdHelper(mTitleBar,
							R.id.movable_titlebar_min, "movable_titlebar_min");
		final ImageButton more_button = (ImageButton) Util.findViewByIdHelper(mTitleBar,
							R.id.movable_titlebar_more, "movable_titlebar_more");
		final ImageButton close_button = (ImageButton) Util.findViewByIdHelper(mTitleBar,
							R.id.movable_titlebar_close, "movable_titlebar_close");
		app_title.setText(appContext.getApplicationInfo().loadLabel(appContext.getPackageManager()));

		switch (mTitleBarIconType) {
			case Common.TITLEBAR_ICON_ORIGINAL:
				close_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_close_old));
				max_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_max_old));
				min_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_min_old));
				more_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_more_old));
				break;
			case Common.TITLEBAR_ICON_BachMinuetInG:
				close_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_close));
				max_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_max));
				min_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_min));
				more_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_more));
				break;
			case Common.TITLEBAR_ICON_SSNJR2002:
				close_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_close_ssnjr));
				max_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_max_ssnjr));
				min_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_min_ssnjr));
				more_button.setImageDrawable(MainXposed.sModRes.getDrawable(R.drawable.movable_title_more_ssnjr));
				break;
		}

		RelativeLayout.LayoutParams header_param = (LayoutParams) mTitleBar.getLayoutParams();
		header_param.height = mTitleBarHeight;
		mTitleBar.setLayoutParams(header_param);

		ViewGroup.LayoutParams divider_param = divider.getLayoutParams();
		divider_param.height = mTitleBarDivider;
		divider.setLayoutParams(divider_param);

		String color_str = MainXposed.mPref.getString(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_COLOR,
										   Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_COLOR);
		divider.setBackgroundColor(Color.parseColor("#" + color_str));

		final String item1 = MainXposed.sModRes.getString(R.string.dnm_transparency);
		final String menu_item4_sub1 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub1);
		final String menu_item4_sub2 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub2);
		final String menu_item4_sub3 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub3);
		final String menu_item4_sub4 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub4);
		//4WAYMOD
		final String menu_item4_sub5 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub5);
		final String menu_item4_sub6 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub6);
		final String menu_item4_sub7 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub7);
		final String menu_item4_sub8 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub8);

		final PopupMenu popupMenu = new PopupMenu(appContext, more_button);
		final Menu menu = popupMenu.getMenu();
		menu.add(item1);
		menu.add(menu_item4_sub1);
		menu.add(menu_item4_sub2);
		menu.add(menu_item4_sub3);
		menu.add(menu_item4_sub4);
		//4WAYMOD
		menu.add(menu_item4_sub5);
		menu.add(menu_item4_sub6);
		menu.add(menu_item4_sub7);
		menu.add(menu_item4_sub8);
//		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//				@Override
//				public boolean onMenuItemClick(MenuItem item) {
//					if (item.getTitle().equals(item1)) {
//						showTransparencyDialogVisibility();
//					} else if (item.getTitle().equals(menu_item4_sub1)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP);
//					} else if (item.getTitle().equals(menu_item4_sub2)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM);
//					} else if (item.getTitle().equals(menu_item4_sub3)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.LEFT);
//					} else if (item.getTitle().equals(menu_item4_sub4)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.RIGHT);
//					}
//					//4WAYMOD
//					else if (item.getTitle().equals(menu_item4_sub5)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP | Gravity.LEFT);
//					} else if (item.getTitle().equals(menu_item4_sub6)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP | Gravity.RIGHT);
//					}else if (item.getTitle().equals(menu_item4_sub7)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM | Gravity.LEFT);
//					} else if (item.getTitle().equals(menu_item4_sub8)) {
//						MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM | Gravity.RIGHT);
//					}
//					return false;
//				}
//			});
		final View.OnClickListener click = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tag = (v.getTag() instanceof String) ? (String) v.getTag() : "";
				int id = v.getId();

				if (id == R.id.movable_titlebar_close || tag.equals("movable_titlebar_close")) {
					closeApp();
				} else if (id == R.id.movable_titlebar_max || tag.equals("movable_titlebar_max")) {
					ActivityHooks.taskStack.maximize();
				} else if (id == R.id.movable_titlebar_min || tag.equals("movable_titlebar_min")) {
//					MovableWindow.minimizeAndShowNotification(mActivity);
				} else if (id == R.id.movable_titlebar_more || tag.equals("movable_titlebar_more")) {
					popupMenu.show();
				}
			}
		};
		close_button.setOnClickListener(click);
		max_button.setOnClickListener(click);
		min_button.setOnClickListener(click);
		more_button.setOnClickListener(click);
		mTitleBar.setOnTouchListener(new View.OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent e)
				{
					ActivityHooks.taskStack.onUserAction(null, e, Common.ACTION_DRAG, v);
					return false;
				}
		});

		if(mTintedTitlebar) mTitleBar.setBackgroundColor(mTitleBarColor);

//		mTitleBarHeader = header;
//		mTitleBarTitle = app_title;
//		mTitleBarClose = close_button;
//		mTitleBarMin = min_button;
//		mTitleBarMax = max_button;
//		mTitleBarMore = more_button;

		setTitleBarVisibility(true);
	}
	
//	public void setTitleBarVisibility() {
//		setTitleBarVisibility(titleBarVisible);
//	}
	
	public void setTitleBarVisibility(boolean visible) {
		setTitleBarVisibility(visible, this.mWindow);
	}
	
	public void setTitleBarVisibility(boolean visible, final Window mWindow) {
		Debugger.DEBUG("setTitleBarVisibility " + visible);
		titleBarVisible = visible;
		if(mTitleBar==null)
			return;
		mTitleBar.setVisibility(visible ? View.VISIBLE : View.GONE);
			final FrameLayout decorView = (FrameLayout) mWindow.peekDecorView()
				.getRootView();
			for(int i=decorView.indexOfChild(this)-1; i>=0; i--){
				final View child = decorView.getChildAt(i);
//				if(child instanceof MovableOverlayView)
//					continue;
				//if(!child.getFitsSystemWindows())
					//continue;
				FrameLayout.LayoutParams parammm = (FrameLayout.LayoutParams) child.getLayoutParams();
				parammm.setMargins(0, visible ? mTitleBarHeight : 0, 0, 0);
				child.setLayoutParams(parammm);
			}
	}
	
	public static final RelativeLayout.LayoutParams getParams() {
		final RelativeLayout.LayoutParams paramz = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		paramz.setMargins(0, 0, 0, 0);
		return paramz;
	}
	
	private int getPrimaryDarkColor(){
		int DEFAULT_TITLEBAR_COLOR = Color.BLACK;
		TypedValue a = new TypedValue();
		if(!appContext.getTheme().resolveAttribute(android.R.attr.colorPrimaryDark, a, true))
			return DEFAULT_TITLEBAR_COLOR;	
		if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
			// it is a color
			return a.data;
		}
		return DEFAULT_TITLEBAR_COLOR;
	}
	
	public void closeApp() {
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
		if (MainXposed.mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SINGLE_WINDOW,
							 Common.DEFAULT_WINDOW_TITLEBAR_SINGLE_WINDOW)
			&& Build.VERSION.SDK_INT >= 16) {
			ActivityHooks.mCurrentActivity.finishAffinity();
		} else {
			ActivityHooks.mCurrentActivity.finish();
		}
	}
}
