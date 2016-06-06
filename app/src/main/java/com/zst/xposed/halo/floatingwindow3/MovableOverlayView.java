package com.zst.xposed.halo.floatingwindow3;

import de.robv.android.xposed.XposedHelpers;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.view.*;
import android.util.*;
import android.graphics.*;
import android.graphics.drawable.*;
import com.zst.xposed.halo.floatingwindow3.prefs.*;

@SuppressLint("ViewConstructor")
// We only create this view programatically, so the default
// constructor used by XML inflating is not needed

public class MovableOverlayView extends RelativeLayout {
	
	public static final int ID_OVERLAY_VIEW = 1000000;
	
	/* Corner Button Actions Constants */
	private static final int ACTION_CLICK_TRIANGLE = 0x0;
	private static final int ACTION_LONGPRESS_TRIANGLE = 0x1;
	private static final int ACTION_CLICK_QUADRANT = 0x2;
	private static final int ACTION_LONGPRESS_QUADRANT = 0x3;
	
	// (constants) App Objects
	//private final MainXposed mMainXposed;
	private final Activity mActivity;
	private final Resources mResource;
	//private final AeroSnap mAeroSnap;
	private final SharedPreferences mPref;
	
	// Views
	public final View mDragToMoveBar;
	public final View mQuadrant;
	public final View mTriangle;
	public final ImageView mBorderOutline;
	
	public RelativeLayout mTitleBarHeader;
	public ImageButton mTitleBarClose;
	public ImageButton mTitleBarMin;
	public ImageButton mTitleBarMax;
	public ImageButton mTitleBarMore;
	public TextView mTitleBarTitle;
	
	private View mTransparencyDialog;
	
	/* Title Bar */
	private final int mTitleBarHeight;
	private final int mTitleBarDivider;
	private final int mTitleBarIconType;
	private final boolean mLiveResizing;
	private int mTitleBarColor = Color.BLACK;
	private boolean mTintedTitlebar;
	
	/**
	 * Create the overlay view for Movable and Resizable feature
	 * @param activity - the current activity
	 * @param resources - resource from the module
	 * @param pref - preference of the module
	 * @param aerosnap - an aerosnap instance
	 */
	public MovableOverlayView(Activity activity, Resources resources,
			SharedPreferences pref) {
		super(activity);
		
		// Set the params
		//mMainXposed = main;
		mActivity = activity;
		mResource = resources;
		mPref = pref;
		//mAeroSnap = aerosnap;
		mTintedTitlebar = mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_ENABLED, Common.DEFAULT_TINTED_TITLEBAR_ENABLED);
		if(mTintedTitlebar) mTitleBarColor = getPrimaryDarkColor();
		/* get the layout from our module. we cannot just use the R reference
		 * since the layout is from the module, not the current app we are
		 * modifying. thus, we use a parser */
		try {
			Context module_context = activity.createPackageContext(Common.THIS_MOD_PACKAGE_NAME,
					Context.CONTEXT_IGNORE_SECURITY);
			LayoutInflater.from(module_context).inflate(R.layout.movable_window, this);
		} catch (Exception e) {
			XmlResourceParser parser = resources.getLayout(R.layout.movable_window);
			activity.getWindow().getLayoutInflater().inflate(parser, this);
		}
		
		// Thanks to this post for some inspiration:
		// http://sriramramani.wordpress.com/2012/07/25/infamous-viewholder-pattern/
		
		setId(ID_OVERLAY_VIEW);
		setRootNamespace(false);
		
		mDragToMoveBar = findViewByIdHelper(this, R.id.movable_action_bar, "movable_action_bar");
		mTriangle = findViewByIdHelper(this, R.id.movable_corner, "movable_corner");
		mQuadrant = findViewByIdHelper(this, R.id.movable_quadrant, "movable_quadrant");
		mBorderOutline = (ImageView) findViewByIdHelper(this, R.id.movable_background, "movable_background");
		if(mBorderOutline!=null)
			mBorderOutline.bringToFront();
		
		// set preferences values
		mTitleBarIconType = mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
				Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE);
		boolean titlebar_enabled = true;// mTitleBarIconType != 0;
		boolean titlebar_separator_enabled = mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_ENABLED,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_ENABLED);
		mTitleBarHeight = !titlebar_enabled ? 0 : Util.realDp(
				mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SIZE, Common.DEFAULT_WINDOW_TITLEBAR_SIZE),
				activity);
		mTitleBarDivider = !titlebar_separator_enabled ? 0 : Util.realDp(
				mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_SIZE,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_SIZE), activity);
		mLiveResizing = mPref.getBoolean(Common.KEY_WINDOW_RESIZING_LIVE_UPDATE,
				Common.DEFAULT_WINDOW_RESIZING_LIVE_UPDATE);
		
		// init stuff
		initCornersViews();
		
		setRootNamespace(true);
		// After initializing everything, set this to tell findViewById to skip
		// our layout. We do this to prevent id's conflicting with the current app.
	}
	
	private View findViewByIdHelper(View view, int id, String tag) {
		View v = view.findViewById(id);
		if (v == null) {
			v = findViewWithTag(view, tag);
		}
		return v;
    }
	
	private View findViewWithTag(View view, String text) {
		if (view.getTag() instanceof String) {
			if (((String) view.getTag()).equals(text))
				return view;
		}
		if (view instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); ++i) {
				final View child = group.getChildAt(i);
				final View found = findViewWithTag(child, text);
				if (found != null)
					return found;
			}
		}
        return null;
    }
	
	/**
	 * Initializes the triangle and quadrant's transparency, color, size etc.
	 * @since When inflating, the system will find the drawables in the CURRENT
	 *        app, which will FAIL since the drawables are in the MODULE. So we
	 *        have no choice but to do this programatically
	 */
	private void initCornersViews() {
		Drawable triangle_background = mResource.getDrawable(R.drawable.movable_corner);
		Drawable quadrant_background = mResource.getDrawable(R.drawable.movable_quadrant);
		
		if(mTintedTitlebar && mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_CORNER_TINT, Common.DEFAULT_TINTED_TITLEBAR_CORNER_TINT)){
			triangle_background.setColorFilter(mTitleBarColor, Mode.MULTIPLY);
			quadrant_background.setColorFilter(mTitleBarColor, Mode.MULTIPLY);
		}
		else{
			String color_triangle = mPref.getString(Common.KEY_WINDOW_TRIANGLE_COLOR,
					Common.DEFAULT_WINDOW_TRIANGLE_COLOR);
			if (!color_triangle.equals(Common.DEFAULT_WINDOW_TRIANGLE_COLOR)) { 
				triangle_background.setColorFilter(Color.parseColor("#" + color_triangle),
						Mode.MULTIPLY);
			}
			
			String color_quadrant = mPref.getString(Common.KEY_WINDOW_QUADRANT_COLOR,
					Common.DEFAULT_WINDOW_QUADRANT_COLOR);
			if (!color_quadrant.equals(Common.DEFAULT_WINDOW_QUADRANT_COLOR)) {
				quadrant_background.setColorFilter(Color.parseColor("#" + color_quadrant),
						Mode.MULTIPLY);
			}
		}
		
		float triangle_alpha = mPref.getFloat(Common.KEY_WINDOW_TRIANGLE_ALPHA,
				Common.DEFAULT_WINDOW_TRIANGLE_ALPHA);
		triangle_background.setAlpha((int) (triangle_alpha * 255));
		
		float quadrant_alpha = mPref.getFloat(Common.KEY_WINDOW_QUADRANT_ALPHA,
				Common.DEFAULT_WINDOW_QUADRANT_ALPHA);
		quadrant_background.setAlpha((int) (quadrant_alpha * 255));
		
		Util.setBackgroundDrawable(mTriangle, triangle_background);
		Util.setBackgroundDrawable(mQuadrant, quadrant_background);
		
		int triangle_size = Util.dp(mPref.getInt(Common.KEY_WINDOW_TRIANGLE_SIZE,
				Common.DEFAULT_WINDOW_TRIANGLE_SIZE), mActivity.getApplicationContext());
		mTriangle.getLayoutParams().width = triangle_size;
		mTriangle.getLayoutParams().height = triangle_size;
		
		int quadrant_size = Util.dp(mPref.getInt(Common.KEY_WINDOW_QUADRANT_SIZE,
				Common.DEFAULT_WINDOW_QUADRANT_SIZE), mActivity.getApplicationContext());
		mQuadrant.getLayoutParams().width = quadrant_size;
		mQuadrant.getLayoutParams().height = quadrant_size;
		
		final boolean triangle_enabled = mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_ENABLE,
				Common.DEFAULT_WINDOW_TRIANGLE_ENABLE);
		if (triangle_enabled) {
			if (mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_RESIZE_ENABLED,
					Common.DEFAULT_WINDOW_TRIANGLE_RESIZE_ENABLED)) {
				if (mLiveResizing) {
					mTriangle.setOnTouchListener(new Resizable(mActivity, mActivity.getWindow()));
				} else {
					mTriangle.setOnTouchListener(new OutlineLeftResizable(mActivity, mActivity
							.getWindow()));
				}
			}
			
			if (mPref.getBoolean(Common.KEY_WINDOW_TRIANGLE_DRAGGING_ENABLED,
					Common.DEFAULT_WINDOW_TRIANGLE_DRAGGING_ENABLED)) {
				mTriangle.setOnTouchListener(new Movable(mActivity.getWindow(), mTriangle));
			}
			
			mTriangle.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					cornerButtonClickAction(ACTION_CLICK_TRIANGLE);
				}
			});
			
			mTriangle.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					cornerButtonClickAction(ACTION_LONGPRESS_TRIANGLE);
					return true;
				}
			});
		} else {
			mTriangle.getLayoutParams().width = 0;
			mTriangle.getLayoutParams().height = 0;
		}
		
		boolean quadrant_enabled = mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_ENABLE,
				Common.DEFAULT_WINDOW_QUADRANT_ENABLE);
		if (quadrant_enabled) {
			if (mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_RESIZE_ENABLED,
					Common.DEFAULT_WINDOW_QUADRANT_RESIZE_ENABLED)) {
				if (mLiveResizing) {
					mQuadrant.setOnTouchListener(new RightResizable(mActivity.getWindow()));
				} else {
					mQuadrant.setOnTouchListener(new OutlineRightResizable(mActivity.getWindow()));
				}
			}
			
			if (mPref.getBoolean(Common.KEY_WINDOW_QUADRANT_DRAGGING_ENABLED,
					Common.DEFAULT_WINDOW_QUADRANT_DRAGGING_ENABLED)) {
				mQuadrant.setOnTouchListener(new Movable(mActivity.getWindow(), mQuadrant));
			}
			
			mQuadrant.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					cornerButtonClickAction(ACTION_CLICK_QUADRANT);
				}
			});
			
			mQuadrant.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View v) {
					cornerButtonClickAction(ACTION_LONGPRESS_QUADRANT);
					return true;
				}
			});
		} else {
			mQuadrant.getLayoutParams().width = 0;
			mQuadrant.getLayoutParams().height = 0;
		}
		
		setDragActionBarVisibility(false, true);
		initDragToMoveBar();
		
		if (mPref.getBoolean(Common.KEY_WINDOW_BORDER_ENABLED,
				Common.DEFAULT_WINDOW_BORDER_ENABLED)) {
			final int color = Color.parseColor("#" + mPref.getString(Common.KEY_WINDOW_BORDER_COLOR,
							Common.DEFAULT_WINDOW_BORDER_COLOR));
			final int thickness = mPref.getInt(Common.KEY_WINDOW_BORDER_THICKNESS,
					Common.DEFAULT_WINDOW_BORDER_THICKNESS);
			setWindowBorder(color, thickness);
			//mMainXposed.hookActionBarColor.setBorderThickness(thickness);
		}
		
		initTitleBar();
	}
	
	// Corner Buttons (Triangle, Quadrant) Actions.
	private void cornerButtonClickAction(int type_of_action) {
		String index = "0";
		switch (type_of_action) {
		case ACTION_CLICK_TRIANGLE:
			index = mPref.getString(Common.KEY_WINDOW_TRIANGLE_CLICK_ACTION,
					Common.DEFAULT_WINDOW_TRIANGLE_CLICK_ACTION);
			break;
		case ACTION_LONGPRESS_TRIANGLE:
			index = mPref.getString(Common.KEY_WINDOW_TRIANGLE_LONGPRESS_ACTION,
					Common.DEFAULT_WINDOW_TRIANGLE_LONGPRESS_ACTION);
			break;
		case ACTION_CLICK_QUADRANT:
			index = mPref.getString(Common.KEY_WINDOW_QUADRANT_CLICK_ACTION,
					Common.DEFAULT_WINDOW_QUADRANT_CLICK_ACTION);
			break;
		case ACTION_LONGPRESS_QUADRANT:
			index = mPref.getString(Common.KEY_WINDOW_QUADRANT_LONGPRESS_ACTION,
					Common.DEFAULT_WINDOW_QUADRANT_LONGPRESS_ACTION);
			break;
		}
		switch (Integer.parseInt(index)) {
		case 0: // Do Nothing
			break;
		case 1: // Drag & Move Bar
			setDragActionBarVisibility(true, true);
			break;
		case 2:
			closeApp();
			break;
		case 3: // Transparency Dialog
			showTransparencyDialogVisibility();
			break;
		case 4: // Minimize / Hide Entire App
			MovableWindow.minimizeAndShowNotification(mActivity);
			break;
		case 5: // Drag & Move Bar w/o hiding corner
			setDragActionBarVisibility(true, false);
			break;
		case 6: // Maximize App
			MovableWindow.maximize();
			break;
		}
	}
	
	// Create the Titlebar
	private void initTitleBar() {
		final RelativeLayout header = (RelativeLayout) findViewByIdHelper(this,
				R.id.movable_titlebar, "movable_titlebar");
		
		if (mTitleBarHeight == 0) {
			removeView(header);
			return;
		}
		
		final View divider = findViewByIdHelper(header,
				R.id.movable_titlebar_line, "movable_titlebar_line");
		final TextView app_title = (TextView) findViewByIdHelper(header,
				R.id.movable_titlebar_appname, "movable_titlebar_appname");
		final ImageButton max_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_max, "movable_titlebar_max");
		final ImageButton min_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_min, "movable_titlebar_min");
		final ImageButton more_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_more, "movable_titlebar_more");
		final ImageButton close_button = (ImageButton) findViewByIdHelper(header,
				R.id.movable_titlebar_close, "movable_titlebar_close");
	
		app_title.setText(mActivity.getApplicationInfo().loadLabel(mActivity.getPackageManager()));
		
		switch (mTitleBarIconType) {
		case Common.TITLEBAR_ICON_ORIGINAL:
			close_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close_old));
			max_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max_old));
			min_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min_old));
			more_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more_old));
			break;
		case Common.TITLEBAR_ICON_BachMinuetInG:
			close_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close));
			max_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max));
			min_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min));
			more_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more));
			break;
		case Common.TITLEBAR_ICON_SSNJR2002:
			close_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_close_ssnjr));
			max_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_max_ssnjr));
			min_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_min_ssnjr));
			more_button.setImageDrawable(mResource.getDrawable(R.drawable.movable_title_more_ssnjr));
			break;
		}
		
		RelativeLayout.LayoutParams header_param = (LayoutParams) header.getLayoutParams();
		header_param.height = mTitleBarHeight;
		header.setLayoutParams(header_param);
		
		ViewGroup.LayoutParams divider_param = divider.getLayoutParams();
		divider_param.height = mTitleBarDivider;
		divider.setLayoutParams(divider_param);
		
		String color_str = mPref.getString(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_COLOR,
				Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_COLOR);
		divider.setBackgroundColor(Color.parseColor("#" + color_str));
		
		final String item1 = mResource.getString(R.string.dnm_transparency);
		final String menu_item4_sub1 = mResource.getString(R.string.dnm_snap_window_sub1);
		final String menu_item4_sub2 = mResource.getString(R.string.dnm_snap_window_sub2);
		final String menu_item4_sub3 = mResource.getString(R.string.dnm_snap_window_sub3);
		final String menu_item4_sub4 = mResource.getString(R.string.dnm_snap_window_sub4);
		//4WAYMOD
		final String menu_item4_sub5 = mResource.getString(R.string.dnm_snap_window_sub5);
		final String menu_item4_sub6 = mResource.getString(R.string.dnm_snap_window_sub6);
		final String menu_item4_sub7 = mResource.getString(R.string.dnm_snap_window_sub7);
		final String menu_item4_sub8 = mResource.getString(R.string.dnm_snap_window_sub8);

		final PopupMenu popupMenu = new PopupMenu(mActivity, more_button);
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
		popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				if (item.getTitle().equals(item1)) {
					showTransparencyDialogVisibility();
				} else if (item.getTitle().equals(menu_item4_sub1)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP);
				} else if (item.getTitle().equals(menu_item4_sub2)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM);
				} else if (item.getTitle().equals(menu_item4_sub3)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub4)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.RIGHT);
				}
				//4WAYMOD
				else if (item.getTitle().equals(menu_item4_sub5)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP | Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub6)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP | Gravity.RIGHT);
				}else if (item.getTitle().equals(menu_item4_sub7)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM | Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub8)) {
					MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM | Gravity.RIGHT);
				}
				return false;
			}
		});
		
		final View.OnClickListener click = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tag = (v.getTag() instanceof String) ? (String) v.getTag() : "";
				int id = v.getId();
				
				if (id == R.id.movable_titlebar_close || tag.equals("movable_titlebar_close")) {
					closeApp();
				} else if (id == R.id.movable_titlebar_max || tag.equals("movable_titlebar_max")) {
					MovableWindow.maximize();
				} else if (id == R.id.movable_titlebar_min || tag.equals("movable_titlebar_min")) {
					MovableWindow.minimizeAndShowNotification(mActivity);
				} else if (id == R.id.movable_titlebar_more || tag.equals("movable_titlebar_more")) {
					popupMenu.show();
				}
			}
		};
		close_button.setOnClickListener(click);
		max_button.setOnClickListener(click);
		min_button.setOnClickListener(click);
		more_button.setOnClickListener(click);
		header.setOnTouchListener(new Movable(mActivity.getWindow(), true));
		
		if(mTintedTitlebar) header.setBackgroundColor(mTitleBarColor);
		
		mTitleBarHeader = header;
		mTitleBarTitle = app_title;
		mTitleBarClose = close_button;
		mTitleBarMin = min_button;
		mTitleBarMax = max_button;
		mTitleBarMore = more_button;
		
		setTitleBarVisibility(true);
	}
	
	//use Primary dark color for
	private int getPrimaryDarkColor(){
		int DEFAULT_TITLEBAR_COLOR = Color.BLACK;
		
		TypedValue a = new TypedValue();
		if(!mActivity.getTheme().resolveAttribute(android.R.attr.colorPrimaryDark, a, true))
			return DEFAULT_TITLEBAR_COLOR;	
		if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
		// it is a color
		return a.data;
		}
		return DEFAULT_TITLEBAR_COLOR;
	}
	
	/*private int getActionBarColor(){
		View v = mActivity.getActionBar().getCustomView();
		if(v!=null) return getDrawableColor(v.getBackground());
		return Color.RED;
	}
	
	private int getDrawableColor(Drawable mDrawable){
		if(mDrawable==null) return Color.BLUE;
		Bitmap mBitmap = drawableToBitmap(mDrawable);
		if(mBitmap==null) return Color.BLUE;
		int pixel;
		try{
			if (mBitmap.getHeight() <= 5) {
				pixel = mBitmap.getPixel(0, 0);
			} else {
				pixel = mBitmap.getPixel(0, 5);
			}
			} catch(Throwable t){
				pixel = 10;
			}
		return Color.argb(Color.alpha(pixel), Color.red(pixel), Color.green(pixel), Color.blue(pixel));
	}
	
	public static Bitmap drawableToBitmap (Drawable drawable) {
		Bitmap bitmap = null;

		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			if(bitmapDrawable.getBitmap() != null) {
				return bitmapDrawable.getBitmap();
			}
		}

		if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
			bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
		} else {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
		}

		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);
		return bitmap;
	}*/
	
	// Create the drag-to-move bar
	private void initDragToMoveBar() {
		mDragToMoveBar.setOnTouchListener(new Movable(mActivity.getWindow(), true));
		
		TextView dtm_title = (TextView) findViewByIdHelper(mDragToMoveBar,
				R.id.movable_dtm_title, "movable_dtm_title");
		dtm_title.setText(mResource.getString(R.string.dnm_title));
		
		final ImageButton done = (ImageButton) findViewByIdHelper(mDragToMoveBar,
				R.id.movable_done, "movable_done");
		done.setImageDrawable(mResource.getDrawable(R.drawable.movable_done));
		done.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDragActionBarVisibility(false, true);
			}
		});
		
		final String menu_item1 = mResource.getString(R.string.dnm_transparency);
		final String menu_item3 = mResource.getString(R.string.dnm_minimize);
		final String menu_item2 = mResource.getString(R.string.dnm_close_app);
		final String menu_item4 = mResource.getString(R.string.dnm_snap_window);
		final String menu_item4_sub1 = mResource.getString(R.string.dnm_snap_window_sub1);
		final String menu_item4_sub2 = mResource.getString(R.string.dnm_snap_window_sub2);
		final String menu_item4_sub3 = mResource.getString(R.string.dnm_snap_window_sub3);
		final String menu_item4_sub4 = mResource.getString(R.string.dnm_snap_window_sub4);
		//4WAYMOD
		final String menu_item4_sub5 = mResource.getString(R.string.dnm_snap_window_sub5);
		final String menu_item4_sub6 = mResource.getString(R.string.dnm_snap_window_sub6);
		final String menu_item4_sub7 = mResource.getString(R.string.dnm_snap_window_sub7);
		final String menu_item4_sub8 = mResource.getString(R.string.dnm_snap_window_sub8);
		
		final ImageButton overflow = (ImageButton) findViewByIdHelper(mDragToMoveBar,
				R.id.movable_overflow, "movable_overflow");
		overflow.setImageDrawable(mResource.getDrawable(R.drawable.movable_overflow));
		
		final PopupMenu popupMenu = new PopupMenu(overflow.getContext(), overflow);
		Menu menu = popupMenu.getMenu();
		menu.add(menu_item1);
		menu.add(menu_item3);
		menu.add(menu_item2);

		
		SubMenu submenu_item4 = menu.addSubMenu(menu_item4);
		submenu_item4.add(menu_item4_sub1);
		submenu_item4.add(menu_item4_sub2);
		submenu_item4.add(menu_item4_sub3);
		submenu_item4.add(menu_item4_sub4);
		//4WAYMOD
		submenu_item4.add(menu_item4_sub5);
		submenu_item4.add(menu_item4_sub6);
		submenu_item4.add(menu_item4_sub7);
		submenu_item4.add(menu_item4_sub8);
		overflow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (item.getTitle().equals(menu_item1)) {
							showTransparencyDialogVisibility();
						} else if (item.getTitle().equals(menu_item2)) {
							closeApp();
						} else if (item.getTitle().equals(menu_item3)) {
							MovableWindow.minimizeAndShowNotification(mActivity);
						} else if (item.getTitle().equals(menu_item4_sub1)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP);
						} else if (item.getTitle().equals(menu_item4_sub2)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM);
						} else if (item.getTitle().equals(menu_item4_sub3)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.LEFT);
						} else if (item.getTitle().equals(menu_item4_sub4)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.RIGHT);
						}
						//4WAYMOD
						else if (item.getTitle().equals(menu_item4_sub5)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP | Gravity.LEFT);
						} else if (item.getTitle().equals(menu_item4_sub6)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.TOP | Gravity.RIGHT);;
						}else if (item.getTitle().equals(menu_item4_sub7)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM | Gravity.LEFT);;
						} else if (item.getTitle().equals(menu_item4_sub8)) {
							MovableWindow.mAeroSnap.forceSnapGravity(Gravity.BOTTOM | Gravity.RIGHT);;
						}
						return false;
					}
				});
				popupMenu.show();
			}
		});
	}
	
	private void showTransparencyDialogVisibility() {
		final RelativeLayout bg = (RelativeLayout) findViewByIdHelper(this, 
				R.id.movable_transparency_holder, "movable_transparency_holder");
		if (mTransparencyDialog == null) {
			XmlResourceParser parser = mResource.getLayout(R.layout.movable_dialog_transparency);
			mTransparencyDialog = mActivity.getWindow().getLayoutInflater().inflate(parser, bg);
			
			final TextView title = (TextView) mTransparencyDialog.findViewById(android.R.id.text1);
			final TextView numb = (TextView) mTransparencyDialog.findViewById(android.R.id.text2);
			final SeekBar bar = (SeekBar) mTransparencyDialog.findViewById(android.R.id.progress);

			title.setText(mResource.getString(R.string.dnm_transparency));
			
			final float current_alpha = mActivity.getWindow().getAttributes().alpha;
			numb.setText((int) (current_alpha * 100) + "%");
			bar.setProgress((int) (current_alpha * 100) - 10);
			bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					final int newProgress = (progress + 10);
					numb.setText(newProgress + "%");
					
					WindowManager.LayoutParams params = mActivity.getWindow().getAttributes();
					params.alpha = newProgress * 0.01f;
					mActivity.getWindow().setAttributes(params);
				}
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}
			});
			
			bg.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					if (event.getAction() == MotionEvent.ACTION_UP) {
						bg.setVisibility(View.GONE);
					}
					return true;
				}
			});
		}
		bg.setVisibility(View.VISIBLE);
	}
	
	private void setDragActionBarVisibility(boolean visible, boolean with_corner) {
		mDragToMoveBar.setVisibility(visible ? View.VISIBLE : View.GONE);
		if (with_corner) {
			mTriangle.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
			mQuadrant.setVisibility(visible ? View.INVISIBLE : View.VISIBLE);
		}
	}
	
	public void setTitleBarVisibility(boolean visible) {
		if (mTitleBarHeader != null) {
			mTitleBarHeader.setVisibility(visible ? View.VISIBLE : View.GONE);
			
			final FrameLayout decorView = (FrameLayout) mActivity.getWindow().peekDecorView()
					.getRootView();
			final View child = decorView.getChildAt(0);
			FrameLayout.LayoutParams parammm = (FrameLayout.LayoutParams) child.getLayoutParams();
			parammm.setMargins(0, visible ? mTitleBarHeight : 0, 0, 0);
			child.setLayoutParams(parammm);
		}
	}
	
	public void setWindowBorder(int color, int thickness) {
		if(mTintedTitlebar && mPref.getBoolean(Common.KEY_TINTED_TITLEBAR_BORDER_TINT, Common.DEFAULT_TINTED_TITLEBAR_BORDER_TINT))
			color = mTitleBarColor;
		if (thickness == 0) {
			mBorderOutline.setBackgroundResource(0);
		} else {
			Util.setBackgroundDrawable(mBorderOutline, Util.makeOutline(color, thickness));
		}
	}
	
	public static final RelativeLayout.LayoutParams getParams() {
		final RelativeLayout.LayoutParams paramz = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		paramz.setMargins(0, 0, 0, 0);
		return paramz;
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
					mActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(mActivity.getCurrentFocus().getWindowToken(), 0);
		} catch (Exception e) {
			//ignore
		}
		if (mPref.getBoolean(Common.KEY_WINDOW_TITLEBAR_SINGLE_WINDOW,
				Common.DEFAULT_WINDOW_TITLEBAR_SINGLE_WINDOW)
				&& Build.VERSION.SDK_INT >= 16) {
				if(MovableWindow.mWindowHolder!=null && MovableWindow.mWindowHolder.mActivity!=null)
					MovableWindow.mWindowHolder.mActivity.finishAffinity();
				else
					mActivity.finishAffinity();
		} else {
			if(MovableWindow.mWindowHolder!=null && MovableWindow.mWindowHolder.mActivity!=null)
				MovableWindow.mWindowHolder.mActivity.finish();
			else
				mActivity.finish();
		}
	}
	
	public void setRootNamespace(boolean isRoot) {
		XposedHelpers.callMethod(this, "setIsRootNamespace", isRoot);
	}
	
	public int getTitleBarHeight(){
		return mTitleBarHeight+mTitleBarDivider;
	}
}
