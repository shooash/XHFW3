package com.zst.xposed.halo.floatingwindow3.overlays;
import java.util.*;
import com.zst.xposed.halo.floatingwindow3.*;

import android.annotation.SuppressLint;
import android.os.Build;
import android.support.annotation.StyleRes;
import android.widget.*;
import android.content.*;
import android.text.*;
import android.view.*;
import android.util.*;
import android.graphics.*;
import android.view.View.*;
import android.app.*;
import android.graphics.drawable.*;
import de.robv.android.xposed.*;
import android.content.res.*;

public class TitleBarViewHelpers
{
	/* so the buttons are:
	m for more
	t for title
	- for minimize
	+ for maximize
	x for close
	s for constant move
	*/
	public static String buttons=null;
	private static final char NO_ANCHOR = 0;
	private static Map<Character, TitleBarButton> buttonsStack = new HashMap<>();
	private static View.OnClickListener clickListener = null;
	public static ImageButton more_button = null;
	private static int mTextColor = Color.WHITE;
	public static boolean USE_LOCAL = false;
	
	public static void setOnClickListener(final View.OnClickListener click)
	{
		clickListener = click;
	}
	static {
		buttonsStack.put('m', new TitleBarButton('m', R.id.movable_titlebar_more, "movable_titlebar_more"));
		buttonsStack.put('t', new TitleBarButton('t', R.id.movable_titlebar_appname, "movable_titlebar_appname"));
		buttonsStack.put('-', new TitleBarButton('-', R.id.movable_titlebar_min, "movable_titlebar_min"));
		buttonsStack.put('+', new TitleBarButton('+', R.id.movable_titlebar_max, "movable_titlebar_max"));
		buttonsStack.put('x', new TitleBarButton('x', R.id.movable_titlebar_close, "movable_titlebar_close"));
		buttonsStack.put('s', new TitleBarButton('s', R.id.movable_titlebar_separate, "movable_titlebar_separate"));
	}
	
//	private static Map<Character, Integer> buttonsThemeOriginal = new HashMap<>();
//	static {
//		buttonsThemeOriginal.put('m', R.drawable.movable_title_more_old);
//		buttonsThemeOriginal.put('-', R.drawable.movable_title_min_old);
//		buttonsThemeOriginal.put('+', R.drawable.movable_title_max_old);
//		buttonsThemeOriginal.put('x', R.drawable.movable_title_close_old);
//		buttonsThemeOriginal.put('s', R.drawable.movable_title_more_old);
//	}
	public static boolean loadTitleBarButtons() {
		buttons = ActivityHooks.mOverlayTheme.getButtonsList();
		return true;
	}
	
	public static ImageButton makeImageButton(final Character code, final boolean left, final Character anchor_code, final Context appContext) {
		final ImageButton result = new ImageButton(appContext, null, android.R.style.Widget_Holo_Button_Borderless);
		final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
		final TitleBarButton atb = buttonsStack.get(anchor_code);
		final TitleBarButton tb = buttonsStack.get(code);
		if(tb == null)
			return null;
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);	
		if(atb == null)
			lp.addRule(!left?RelativeLayout.ALIGN_PARENT_LEFT:RelativeLayout.ALIGN_PARENT_RIGHT);
		else
			lp.addRule(left?RelativeLayout.LEFT_OF:RelativeLayout.RIGHT_OF, atb.id);	
		result.setLayoutParams(lp);
		result.setAdjustViewBounds(true);
		result.setPadding(0, 0, 0, 0);
		result.setScaleType(ImageView.ScaleType.FIT_CENTER);
		//close_button.setBackgroundColor(Color.WHITE);
		result.setId(tb.id);
		result.setTag(tb.tag);
		setTheme(result, code);
		setAction(result);
		if(code == 'm')
			more_button = result;
		return result;
		
		/*
		 <ImageButton
		 android:id="@+id/movable_titlebar_max"
		 style="@android:style/Widget.Holo.Button.Borderless"
		 android:layout_width="wrap_content"
		 android:layout_height="match_parent"
		 android:layout_alignParentBottom="true"
		 android:layout_alignParentTop="true"
		 android:layout_toLeftOf="@+id/movable_titlebar_close"
		 android:adjustViewBounds="true"
		 android:paddingLeft="0dp"
		 android:paddingRight="0dp"
		 android:scaleType="fitCenter"
		 android:src="#FFF"
		 android:tag="movable_titlebar_max" />
		*/
	}
	
	public static TextView makeTextView(final Character code, final Character anchor_right, final Character anchor_left, final Context appContext) {
		if(code!='t')
			return null;
		final TextView app_title = new TextView(appContext);
		final RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		final TitleBarButton atbl = buttonsStack.get(anchor_left);
		final TitleBarButton atbr = buttonsStack.get(anchor_right);
		final TitleBarButton tb = buttonsStack.get(code);
		if(tb == null)
			return null;
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		//if(anchor_left == NO_ANCHOR&&anchor_right == NO_ANCHOR)
		//	lp.addRule(left?RelativeLayout.ALIGN_PARENT_LEFT:RelativeLayout.ALIGN_PARENT_RIGHT);
		if(atbl!=null)
			lp.addRule(RelativeLayout.LEFT_OF, atbl.id);
		else
			lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		if(atbr!=null)
			lp.addRule(RelativeLayout.RIGHT_OF, atbr.id);
		else
			lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
//		lp.addRule(RelativeLayout.LEFT_OF, R.id.movable_titlebar_close);
//		lp.addRule(RelativeLayout.RIGHT_OF, R.id.movable_titlebar_more);
		app_title.setLayoutParams(lp);
		app_title.setEllipsize(TextUtils.TruncateAt.END);
		app_title.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL);
		app_title.setMaxLines(1);
		if(Build.VERSION.SDK_INT<23)
			//noinspection deprecation
			app_title.setTextAppearance(appContext, android.R.style.TextAppearance_Medium);
		else
			app_title.setTextAppearance(android.R.style.TextAppearance_Medium);
		app_title.setTextColor(mTextColor);
		app_title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		app_title.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
		app_title.setId(tb.id);
		app_title.setTag(tb.tag);
		app_title.setText(appContext.getApplicationInfo().loadLabel(appContext.getPackageManager()));
		return app_title;
	}
	
	public static void setTheme(ImageButton mButton, final Character code) {
		if(code == null)
			return;
		if(code=='s'&&!USE_LOCAL&&!ActivityHooks.taskStack.mSeparateWindows)
			setThemeInActive(mButton, code);
		else {
			mButton.setImageDrawable(tintIcon(ActivityHooks.mOverlayTheme.getButton(code), mTextColor));
		}
			
	}
	
	public static void setThemeInActive(ImageButton mButton, final Character code) {
		if(code == null)
			return;
		mButton.setImageDrawable(tintIcon(ActivityHooks.mOverlayTheme.getButton(code), Color.GRAY));
		}
	
	public static void setAction(final ImageButton mButton) {
		if(clickListener==null)
			return;
		mButton.setOnClickListener(clickListener);
	}
	
	public static boolean addButtons(RelativeLayout mTitleBar, final Context appContext) {
		if(buttons==null&&!loadTitleBarButtons())
			return false;
		addButtons(mTitleBar, appContext, buttons);
		return true;
	}
	
	public static void addButtons(RelativeLayout mTitleBar, final Context appContext, final String btnslist) {
		char last_code = NO_ANCHOR;
		int i = 0;
		int length = btnslist.length();
		for(i = 0; i<length&&btnslist.charAt(i)!='t'; i++) {
			char code = btnslist.charAt(i);
			ImageButton ib = makeImageButton(code, false, last_code, appContext);
			if(ib==null)
				continue;
			last_code = code;
			mTitleBar.addView(ib);
		}
		if(i>=length) 
			return;
		char tcode = btnslist.charAt(i);
		if(tcode!='t')
			return;
		char next_code;
		if(i+1<length)
			next_code = btnslist.charAt(i+1);
		else
			next_code = NO_ANCHOR;
		TextView tv = makeTextView(tcode, last_code, next_code, appContext);
		if(tv!=null) {
			mTitleBar.addView(tv);
		}
		last_code = NO_ANCHOR;
		for(int n = length-1; n>i; n--) {
			char code = btnslist.charAt(n);
			ImageButton ib = makeImageButton(code, true, last_code, appContext);
			if(ib==null)
				continue;
			last_code = code;
			mTitleBar.addView(ib);
		}
		
	}
	
	public static int getContrastTextColor(int bgColor) {
		if(Util.isColorDark(bgColor))
			return Color.WHITE;
		else
			return Color.BLACK;
	}
	
	public static void setContrastTextColor(int bgColor) {
		mTextColor = getContrastTextColor(bgColor);
	}
	
	@SuppressLint("NewApi")
	public static int getPrimaryDarkColor(final Window mWindow){
		int DEFAULT_TITLEBAR_COLOR = Color.BLACK;
		//mWindow.get
		//DEFAULT_TITLEBAR_COLOR = appContext.getResources().getColor(android.R.color., appContext.getTheme());
		//final int actionBarId = mWindow.getContext().getResources().getIdentifier("action_bar", "id", "android");
		//final View actionBar = mWindow.findViewById(actionBarId);
		//final Drawable actionBarBackground = actionBar.getBackground();

		final ActionBar actionBar = ActivityHooks.mCurrentActivity.getActionBar();
		Drawable actionBarBackground = null;
		if(actionBar!=null/*&&actionBar.isShowing()*/) {
			final Object actionBarContainer = XposedHelpers.getObjectField(actionBar, "mContainerView");
			actionBarBackground = (Drawable) XposedHelpers.getObjectField(actionBarContainer, "mBackground");
		}
		else {
			final int actionBarId = mWindow.getContext().getResources().getIdentifier("action_bar", "id", "android");
			final View actionBarV = mWindow.findViewById(actionBarId);
			if(actionBarV!=null/*&&actionBarV.getVisibility()==VISIBLE*/) {
				actionBarBackground = actionBarV.getBackground();
			}
		}
		if(actionBarBackground!=null) {
			DEFAULT_TITLEBAR_COLOR = getMainColorFromActionBarDrawable(actionBarBackground);
		}
		else {
			DEFAULT_TITLEBAR_COLOR = getMainColorFromTheme(mWindow, mWindow.getContext().getTheme(), (Build.VERSION.SDK_INT >= 21) ? mWindow.getStatusBarColor() : Color.BLACK);
		}

		return DEFAULT_TITLEBAR_COLOR;
	}

	private static int getMainColorFromTheme(final Window mWindow, final Resources.Theme theme, int fallback) {
		Integer colorPrimaryDark = null;
		Integer colorPrimary = null;
		TypedValue a = new TypedValue();
		if(mWindow.getContext().getTheme().resolveAttribute(android.R.attr.colorPrimaryDark, a, true)&&
		   (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT))
			colorPrimaryDark = a.data;
		if(mWindow.getContext().getTheme().resolveAttribute(android.R.attr.colorPrimary, a, true)&&
		   (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT))
			colorPrimary = a.data;
		else if (colorPrimaryDark!=null)
			return colorPrimaryDark;
		else
			return fallback;
		if(colorPrimaryDark == null||Util.getColorDarkness(colorPrimaryDark)>Util.getColorDarkness(colorPrimary))
			return colorPrimary;
		else
			return colorPrimaryDark;

	}

	private static int getMainColorFromActionBarDrawable(Drawable drawable) {
		if (drawable == null) {
			return Color.BLACK;
		}
		/*
		 * This should fix the bug where a huge part of the ActionBar background
		 * is drawn white.
		 */
		Drawable copyDrawable = drawable.getConstantState().newDrawable();
		if (copyDrawable instanceof ColorDrawable) {
			return ((ColorDrawable) drawable).getColor();
		}
		Bitmap bitmap = drawableToBitmap(copyDrawable);
		if (bitmap == null) return Color.BLACK;
		int pixel;
		try {
			if (bitmap.getHeight() <= 5) {
				pixel = bitmap.getPixel(0, 0);
			} else {
				pixel = bitmap.getPixel(0, 5);
			}
		} catch (IllegalArgumentException e) {
			pixel = Color.BLACK;
		}
		int red = Color.red(pixel);
		int blue = Color.blue(pixel);
		int green = Color.green(pixel);
		int alpha = Color.alpha(pixel);
		return Color.argb(alpha, red, green, blue);
	}

	public static RelativeLayout getTitleBarView(final Context appContext, int mTitleBarHeight, boolean mTintedTitlebar, int mTitleBarColor, int mTitleBarDivider, final Runnable transparencyRunnable) {
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
		if(TitleBarViewHelpers.more_button!=null&&!USE_LOCAL) {
			final PopupMenu popupMenu = getTitleBarPopupMenu(appContext, TitleBarViewHelpers.more_button, transparencyRunnable);
			TitleBarViewHelpers.more_button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View p1)
					{
						popupMenu.show();
					}
				});
		}

		RelativeLayout.LayoutParams header_param = (RelativeLayout.LayoutParams) result.getLayoutParams();
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
	
	public static PopupMenu getTitleBarPopupMenu(final Context appContext, final View anchor, final Runnable transparencyAction) {
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
					ActivityHooks.taskStack.snap(Gravity.TOP);
				} else if (item.getTitle().equals(menu_item4_sub2)) {
					ActivityHooks.taskStack.snap(Gravity.BOTTOM);
				} else if (item.getTitle().equals(menu_item4_sub3)) {
					ActivityHooks.taskStack.snap(Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub4)) {
					ActivityHooks.taskStack.snap(Gravity.RIGHT);
				}
				else if (item.getTitle().equals(menu_item4_sub5)) {
					ActivityHooks.taskStack.snap(Gravity.TOP | Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub6)) {
					ActivityHooks.taskStack.snap(Gravity.TOP | Gravity.RIGHT);
				}else if (item.getTitle().equals(menu_item4_sub7)) {
					ActivityHooks.taskStack.snap(Gravity.BOTTOM | Gravity.LEFT);
				} else if (item.getTitle().equals(menu_item4_sub8)) {
					ActivityHooks.taskStack.snap(Gravity.BOTTOM | Gravity.RIGHT);
				} else if (item.getTitle().equals(item5)) {
					if(transparencyAction!=null)
						transparencyAction.run();
				}
				return false;
			}
		};

		result.setOnMenuItemClickListener(onClick);
		return result;
	}
	
	private static Bitmap drawableToBitmap(Drawable drawable) throws IllegalArgumentException {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}
		Bitmap bitmap;
		try {
			bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
										 drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
		} catch (IllegalArgumentException e) {
			return null;
		}
		return bitmap;
	}
	
	public static Drawable tintIcon(final Drawable icon, int icon_color) {
		Drawable result;
		if(icon==null)
			return null;
		icon.clearColorFilter();
		result = icon.getConstantState().newDrawable();
		result.setColorFilter(icon_color, PorterDuff.Mode.SRC_ATOP);
		return result;
	}
	
	static class TitleBarButton {
		final int code;
		final int id;
		final String tag;
		public TitleBarButton(final int c, final int i, final String t) {
			code = c;
			id = i;
			tag = t;
		}
	}
}
