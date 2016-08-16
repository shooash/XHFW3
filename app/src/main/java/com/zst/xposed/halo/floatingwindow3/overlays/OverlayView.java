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
import android.text.*;
import android.graphics.drawable.*;
import de.robv.android.xposed.*;

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
	public RelativeLayout mDragToMoveBar = null;
	public View mQuadrant;
	public View mTriangle;
	public final ImageView mBorderOutline;
	public final RelativeLayout mTitleBar;
	private View mTransparencyDialog;
	
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
	
	/* Title Bar */
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
		final LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		setBackground(null);
		setId(ID_OVERLAY_VIEW);
		Util.setRootNamespace(this, false);
		setCorners();
		mTitleBar = getTitleBarView();
		if(mTitleBarEnabled&&mTitleBar!=null)
			addView(mTitleBar);
		
//		mTriangle = Util.findViewByIdHelper(this, R.id.movable_corner, "movable_corner");
//		mQuadrant = Util.findViewByIdHelper(this, R.id.movable_quadrant, "movable_quadrant");
		mBorderOutline = getBorderView();
		addView(mBorderOutline);
		mBorderOutline.bringToFront();
//		setCornersViewLegacy();
		
//		setDragActionBarVisibility(false, true);
////		initDragToMoveBar();
////
		if (border_enabled)
			setWindowBorder(border_color, border_thickness);
//		if (mTitleBarEnabled && mTitleBar!=null)
//			setTitleBarView();
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
		if(mTintedTitlebar) {
			mTitleBarColor = TitleBarViewHelpers.getPrimaryDarkColor(mWindow);
			TitleBarViewHelpers.setContrastTextColor(mTitleBarColor);
		}
		
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
	
	private View getCornerView(final boolean left, final int touch_action, final View.OnClickListener onClick, final View.OnLongClickListener onLongClick) {
		final View result = new View(appContext);
		final int size = left?triangle_size:quadrant_size;
		final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(size, size);
		Drawable background = MainXposed.sModRes.getDrawable(left?R.drawable.movable_corner:R.drawable.movable_quadrant);
		if(mTintedCorners) 
			background.setColorFilter(mTitleBarColor, Mode.MULTIPLY);
		else
			background.setColorFilter(Color.parseColor("#" + (left?color_triangle:color_quadrant)), Mode.MULTIPLY);
		background.setAlpha((int) ((left?triangle_alpha:quadrant_alpha) * 255));
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		lp.addRule(left?RelativeLayout.ALIGN_PARENT_LEFT:RelativeLayout.ALIGN_PARENT_RIGHT);
		result.setLayoutParams(lp);
		Util.setBackgroundDrawable(result, background);
		result.setClickable(true);
		result.setLongClickable(true);
		if(onClick!=null)
			result.setOnClickListener(onClick);
		if(onLongClick!=null)
			result.setOnLongClickListener(onLongClick);
		result.setOnTouchListener(new View.OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent e)
				{
					ActivityHooks.taskStack.onUserAction(null, e, touch_action, v);
					return false;
				}	
			});
		result.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View p1)
				{
					cornerButtonClickAction(left?ACTION_CLICK_TRIANGLE:ACTION_CLICK_QUADRANT);
				}
		});
		result.setOnLongClickListener(new View.OnLongClickListener(){
				@Override
				public boolean onLongClick(View p1)
				{
					cornerButtonClickAction(left?ACTION_LONGPRESS_TRIANGLE:ACTION_LONGPRESS_QUADRANT);
					return false;
				}	
		});
		
		result.setId(left?R.id.movable_corner:R.id.movable_quadrant);
		result.setTag(left?"movable_corner":"movable_quadrant");
		return result;
	}
	private void setCorners() {
		if(triangle_enabled)
			addView(mTriangle = getCornerView(true, triangle_action, null, null));
		//TODO: add onclick actions
		if(quadrant_enabled)
			addView(mQuadrant = getCornerView(false, quadrant_action, null, null));
	}
	
	
	private ImageView getBorderView() {
		final ImageView result = new ImageView(appContext);
		final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(ALIGN_PARENT_LEFT);
		lp.addRule(ALIGN_PARENT_TOP);
		lp.addRule(ALIGN_PARENT_RIGHT);
		lp.addRule(ALIGN_PARENT_BOTTOM);
		result.setLayoutParams(lp);
		result.setClickable(false);
		result.setFocusable(false);
		result.setScaleType(ImageView.ScaleType.FIT_XY);
		result.setFocusableInTouchMode(false);
		result.setId(R.id.movable_background);
		result.setTag("movable_background");
		/*
		 <ImageView
		 android:id="@+id/movable_background"
		 android:layout_width="wrap_content"
		 android:layout_height="wrap_content"
		 android:layout_alignParentBottom="true"
		 android:layout_alignParentLeft="true"
		 android:layout_alignParentRight="true"
		 android:layout_alignParentTop="true"
		 android:tag="movable_background"
		 android:clickable="false"
		 android:focusable="false"
		 android:scaleType="fitXY"
		 android:focusableInTouchMode="false" />
		*/
		return result;
	}
	
	private void setDragActionBarVisibility(boolean visible, boolean with_corner) {
		if(mDragToMoveBar==null) {
			mDragToMoveBar = getDragToMoveBar();
			addView(mDragToMoveBar);
		}
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
	
	public RelativeLayout getTitleBarView() {
		RelativeLayout result = new RelativeLayout(appContext);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 0);
		result.setLayoutParams(lp);
		result.setBackgroundColor(0);
		result.setTag("movable_titlebar");
		result.setClickable(true);
		result.setId(R.id.movable_titlebar);
		
		final View divider = new View(appContext);
		lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, 2);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);	
		divider.setLayoutParams(lp);
		divider.setMinimumHeight(5);
		divider.setBackgroundColor(Color.WHITE);
		divider.setTag("movable_titlebar_line");
		
		final View.OnClickListener click = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String tag = (v.getTag() instanceof String) ? (String) v.getTag() : "";
				int id = v.getId();

				if (id == R.id.movable_titlebar_close || tag.equals("movable_titlebar_close")) {
					ActivityHooks.taskStack.closeCurrentApp();
				} else if (id == R.id.movable_titlebar_max || tag.equals("movable_titlebar_max")) {
					ActivityHooks.taskStack.maximize();
				} else if (id == R.id.movable_titlebar_min || tag.equals("movable_titlebar_min")) {
					ActivityHooks.taskStack.minimizeCurrentApp();
				} else if (id == R.id.movable_titlebar_separate || tag.equals("movable_titlebar_separate")) {
					ActivityHooks.taskStack.switchSeparateWindows(v);
				}
			}
		};
		
		TitleBarViewHelpers.setOnClickListener(click);
		TitleBarViewHelpers.addButtons(result, appContext);
		result.addView(divider);
		if(TitleBarViewHelpers.more_button!=null) {
			final PopupMenu popupMenu = getTitleBarPopupMenu(appContext, TitleBarViewHelpers.more_button);
			TitleBarViewHelpers.more_button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View p1)
					{
						popupMenu.show();
					}
			});
		}

		RelativeLayout.LayoutParams header_param = (LayoutParams) result.getLayoutParams();
		header_param.height = mTitleBarHeight;
		result.setLayoutParams(header_param);

		ViewGroup.LayoutParams divider_param = divider.getLayoutParams();
		divider_param.height = mTitleBarDivider;
		divider.setLayoutParams(divider_param);

		String color_str = MainXposed.mPref.getString(Common.KEY_WINDOW_TITLEBAR_SEPARATOR_COLOR,
													  Common.DEFAULT_WINDOW_TITLEBAR_SEPARATOR_COLOR);
		divider.setBackgroundColor(Color.parseColor("#" + color_str));

		result.setOnTouchListener(new View.OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent e)
				{
					if(ActivityHooks.taskStack!=null)
						ActivityHooks.taskStack.onUserAction(null, e, Common.ACTION_DRAG, v);
					return false;
				}
			});

		if(mTintedTitlebar) result.setBackgroundColor(mTitleBarColor);

		return result;
	}
	
	public PopupMenu getTitleBarPopupMenu(final Context appContext, final View anchor) {
		final PopupMenu result = new PopupMenu(appContext, anchor);
		final Menu menu = result.getMenu();
		final String item1 = MainXposed.sModRes.getString(R.string.dnm_maximize);
		final String item2 = MainXposed.sModRes.getString(R.string.dnm_minimize);
		final String item3 = MainXposed.sModRes.getString(R.string.dnm_close_app);
		final String item4 = MainXposed.sModRes.getString(R.string.dnm_snap_window);
		final String menu_item4_sub1 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub1);
		final String menu_item4_sub2 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub2);
		final String menu_item4_sub3 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub3);
		final String menu_item4_sub4 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub4);
		//4WAYMOD
		final String menu_item4_sub5 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub5);
		final String menu_item4_sub6 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub6);
		final String menu_item4_sub7 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub7);
		final String menu_item4_sub8 = MainXposed.sModRes.getString(R.string.dnm_snap_window_sub8);
		final String item5 = MainXposed.sModRes.getString(R.string.dnm_transparency);

		menu.add(item1);
		menu.add(item2);
		menu.add(item3);
		final SubMenu snaps = menu.addSubMenu(item4);
		snaps.add(menu_item4_sub1);
		snaps.add(menu_item4_sub2);
		snaps.add(menu_item4_sub3);
		snaps.add(menu_item4_sub4);
		snaps.add(menu_item4_sub5);
		snaps.add(menu_item4_sub6);
		snaps.add(menu_item4_sub7);
		snaps.add(menu_item4_sub8);
		menu.add(item5);
		final PopupMenu.OnMenuItemClickListener onClick = new PopupMenu.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				if (item.getTitle().equals(item1)) {
					ActivityHooks.taskStack.maximize();
				} else if (item.getTitle().equals(item2)) {
					ActivityHooks.taskStack.minimizeCurrentApp();
				} else if (item.getTitle().equals(item3)) {
					ActivityHooks.taskStack.closeCurrentApp();
				} else if (item.getTitle().equals(menu_item4_sub1)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.TOP);
				} else if (item.getTitle().equals(menu_item4_sub2)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.BOTTOM);
				} else if (item.getTitle().equals(menu_item4_sub3)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub4)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.RIGHT);
				}
				else if (item.getTitle().equals(menu_item4_sub5)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.TOP | Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub6)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.TOP | Gravity.RIGHT);
				}else if (item.getTitle().equals(menu_item4_sub7)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.BOTTOM | Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub8)) {
					ActivityHooks.taskStack.snapCurrentWindow(Gravity.BOTTOM | Gravity.RIGHT);
				} else if (item.getTitle().equals(item5)) {
					showTransparencyDialogVisibility();
					}
				return false;
			}
		};

		result.setOnMenuItemClickListener(onClick);
		return result;
	}
	
	private RelativeLayout getDragToMoveBar() {
		final RelativeLayout result = new RelativeLayout(appContext);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, 48);
		lp.addRule(ALIGN_PARENT_LEFT);
		lp.addRule(ALIGN_PARENT_RIGHT);
		lp.removeRule(ALIGN_PARENT_TOP);
		lp.addRule(BELOW, R.id.movable_titlebar);
		result.setLayoutParams(lp);
		result.setBackgroundColor(Color.parseColor("#b0333333"));
		result.setId(R.id.movable_action_bar);
		result.setTag("movable_action_bar");
		
		final TextView tv = new TextView(appContext);
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(ALIGN_PARENT_BOTTOM);
		lp.addRule(ALIGN_PARENT_TOP);
		lp.addRule(ALIGN_PARENT_LEFT);
		lp.addRule(ALIGN_PARENT_RIGHT);
		tv.setLayoutParams(lp);
		tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
		tv.setText(MainXposed.sModRes.getString(R.string.dnm_title)); 
		tv.setTextAppearance(appContext, android.R.attr.textAppearanceMedium);
		tv.setTextColor(Color.WHITE);
		tv.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		tv.setTag("movable_dtm_title");
		tv.setId(R.id.movable_dtm_title);
		
		result.addView(tv);
		
		final ImageButton ibDone = new ImageButton(appContext);
		lp = new LayoutParams(48, 48);
		lp.addRule(ALIGN_PARENT_BOTTOM);
		lp.addRule(ALIGN_PARENT_LEFT);
		lp.addRule(ALIGN_PARENT_TOP);
		ibDone.setLayoutParams(lp);
		ibDone.setBackground(MainXposed.sModRes.getDrawable(R.drawable.movable_done));
		ibDone.setScaleType(ImageView.ScaleType.FIT_XY);
		ibDone.setId(R.id.movable_done);
		ibDone.setTag("movable_done");
		ibDone.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					setDragActionBarVisibility(false, true);
				}
			});
		
		result.addView(ibDone);
		
		final ImageButton ibOver = new ImageButton(appContext);
		lp = new LayoutParams(48, 48);
		lp.addRule(ALIGN_PARENT_BOTTOM);
		lp.addRule(ALIGN_PARENT_RIGHT);
		lp.addRule(ALIGN_PARENT_TOP);
		ibOver.setLayoutParams(lp);
		ibOver.setBackground(MainXposed.sModRes.getDrawable(R.drawable.movable_overflow));
		ibOver.setScaleType(ImageView.ScaleType.FIT_XY);
		ibOver.setId(R.id.movable_overflow);
		ibOver.setTag("movable_overflow");
		final PopupMenu popupMenu = getTitleBarPopupMenu(appContext, ibOver);
		ibOver.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View p1)
				{
					popupMenu.show();
				}
			});
		result.addView(ibOver);
		
		result.setOnTouchListener(new View.OnTouchListener(){
				@Override
				public boolean onTouch(View v, MotionEvent e)
				{
					ActivityHooks.taskStack.onUserAction(null, e, Common.ACTION_DRAG, v);
					return false;
				}
			});
		return result;
	}
	
	private RelativeLayout getTransparencyDialog() {
		RelativeLayout result = (RelativeLayout) Util.findViewByIdHelper(this, 
						 R.id.movable_transparency_holder, "movable_transparency_holder");
		if(result!=null)
			return result;
		result = new RelativeLayout(appContext);
		final LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		lp.addRule(RelativeLayout.BELOW, R.id.movable_titlebar);
		result.setLayoutParams(lp);
		result.setClickable(true);
		result.setFocusable(true);
		result.setFocusableInTouchMode(true);
		result.setVisibility(View.GONE);
		result.setBackgroundColor(Color.parseColor("#10000000"));
		result.setId(R.id.movable_transparency_holder);
		result.setTag("movable_transparency_holder");
		addView(result);
		
		return result;
	}
	
	private void showTransparencyDialogVisibility() {
		final RelativeLayout bg = getTransparencyDialog();
		
		if (mTransparencyDialog == null) {
			XmlResourceParser parser = MainXposed.sModRes.getLayout(R.layout.movable_dialog_transparency);
			mTransparencyDialog = mWindow.getLayoutInflater().inflate(parser, bg);

			final TextView title = (TextView) mTransparencyDialog.findViewById(android.R.id.text1);
			final TextView numb = (TextView) mTransparencyDialog.findViewById(android.R.id.text2);
			final SeekBar bar = (SeekBar) mTransparencyDialog.findViewById(android.R.id.progress);

			title.setText(MainXposed.sModRes.getString(R.string.dnm_transparency));

			final float current_alpha = mWindow.getAttributes().alpha;
			numb.setText((int) (current_alpha * 100) + "%");
			bar.setProgress((int) (current_alpha * 100) - 10);
			bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						final int newProgress = (progress + 10);
						numb.setText(newProgress + "%");

						WindowManager.LayoutParams params = mWindow.getAttributes();
						params.alpha = newProgress * 0.01f;
						mWindow.setAttributes(params);
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
	
	public void setTitleBarVisibility(boolean visible) {
		setTitleBarVisibility(visible, this.mWindow);
	}
	
	public void setTitleBarVisibility(boolean visible, final Window mWindow) {
		if(mWindow==null)
			return;
		Debugger.DEBUG("setTitleBarVisibility " + visible);
		titleBarVisible = visible;
		if(mTitleBar==null)
			return;
		mTitleBar.setVisibility(visible ? View.VISIBLE : View.GONE);
			final View pdv = mWindow.peekDecorView();
			if(pdv==null)
				return;
			final FrameLayout decorView = (FrameLayout) pdv.getRootView();
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
	
	
	
//	private int getTaskBarColor() {
//		int result = Color.CYAN;
//		//int result = appContext.getResources().getColor(android.R.attr.actionBarItemBackground, appContext.getTheme());
//		
//		return result;
//	}
	
	// Corner Buttons (Triangle, Quadrant) Actions.
	private void cornerButtonClickAction(int type_of_action) {
		String index = "0";
		switch (type_of_action) {
			case ACTION_CLICK_TRIANGLE:
				index = MainXposed.mPref.getString(Common.KEY_WINDOW_TRIANGLE_CLICK_ACTION,
										Common.DEFAULT_WINDOW_TRIANGLE_CLICK_ACTION);
				break;
			case ACTION_LONGPRESS_TRIANGLE:
				index = MainXposed.mPref.getString(Common.KEY_WINDOW_TRIANGLE_LONGPRESS_ACTION,
										Common.DEFAULT_WINDOW_TRIANGLE_LONGPRESS_ACTION);
				break;
			case ACTION_CLICK_QUADRANT:
				index = MainXposed.mPref.getString(Common.KEY_WINDOW_QUADRANT_CLICK_ACTION,
										Common.DEFAULT_WINDOW_QUADRANT_CLICK_ACTION);
				break;
			case ACTION_LONGPRESS_QUADRANT:
				index = MainXposed.mPref.getString(Common.KEY_WINDOW_QUADRANT_LONGPRESS_ACTION,
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
				ActivityHooks.taskStack.closeCurrentApp();
				break;
			case 3: // Transparency Dialog
				showTransparencyDialogVisibility();
				break;
			case 4: // Minimize / Hide Entire App
				ActivityHooks.taskStack.minimizeCurrentApp();
				break;
			case 5: // Drag & Move Bar w/o hiding corner
				setDragActionBarVisibility(true, false);
				break;
			case 6: // Maximize App
				MovableWindow.maximize();
				break;
		}
	}
	
}
