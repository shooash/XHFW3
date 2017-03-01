package com.zst.xposed.halo.floatingwindow3.prefs;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.R;
import com.zst.xposed.halo.floatingwindow3.Util;
import android.preference.PreferenceFragment;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.util.*;
import com.zst.xposed.halo.floatingwindow3.overlays.*;
import com.zst.xposed.halo.floatingwindow3.*;
import com.zst.xposed.halo.floatingwindow3.themable.*;
import android.widget.*;

@SuppressWarnings("deprecation")
@SuppressLint("WorldReadableFiles")
public class TitleBarSettingsActivity extends Activity implements OnSharedPreferenceChangeListener {
	

	SharedPreferences mPref;
	Resources mResource;
	int mIconType;
	int mLayout;
	RelativeLayout mTitleBar = null;
	

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mResource = getResources();
		setContentView(R.layout.dialog_titlebar_icon_theme_chooser);
		init();
		ListView lv = (ListView) findViewById(R.id.titlebar_themes);
		final ThemeItemAdapter adapter = new ThemeItemAdapter(getContext());

		adapter.add(new ThemeItem(mResource,
								  R.string.tbic_theme_none_t,
								  R.string.tbic_theme_none_s, Common.TITLEBAR_ICON_NONE));
		adapter.add(new ThemeItem(mResource,
								  R.string.tbic_theme_original_t,
								  R.string.tbic_theme_original_s, Common.TITLEBAR_ICON_ORIGINAL));
		adapter.add(new ThemeItem(mResource,
								  R.string.tbic_theme_clearer_t,
								  R.string.tbic_theme_clearer_s, Common.TITLEBAR_ICON_BachMinuetInG));
		adapter.add(new ThemeItem(mResource,
								  R.string.tbic_theme_ssnjr_t,
								  R.string.tbic_theme_ssnjr_s, Common.TITLEBAR_ICON_SSNJR2002));
		adapter.mSelectedId = mIconType;

		lv.setAdapter(adapter);
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
					adapter.mSelectedId = pos;
					adapter.notifyDataSetChanged();
					Log.d("Xposed", "Titlebar selected " + pos);
					mPref.edit().putInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE, pos).commit();
				}
			});
		ListView sv = (ListView) findViewById(R.id.titlebar_layouts);
		final ThemeItemAdapter sadapter = populateButtonsSpinnerAdapter();
		sadapter.mSelectedId = mLayout;
		sv.setAdapter(sadapter);
		sv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int pos, long id)
				{
					sadapter.mSelectedId = pos;
					sadapter.notifyDataSetChanged();
					Log.d("Xposed", "Titlebar layout selected " + pos);
					mPref.edit().putString(Common.KEY_BUTTONS_LIST, buttonsLayoutIdToString(pos)).commit();
				}
		});
	}

	private Context getContext() {
		return this;
	}

	// Foreground Titlebar Views
	View tbDivider;
	// Foreground Actionbar Views
	ImageView abAppIcon;
	TextView abAppTitle;
	ImageView abOverflowButton;
	// Background
	View abBackground;
//	View tbBackground;

	private void init() {
		mPref = getSharedPreferences(Common.PREFERENCE_MAIN_FILE, MODE_WORLD_READABLE);
		TitleBarViewHelpers.USE_LOCAL = true;
		mTitleBar = (RelativeLayout) findViewById(R.id.movable_titlebar);
		
		tbDivider = findViewById(R.id.movable_titlebar_line);

		abAppIcon = (ImageView) findViewById(android.R.id.button1);
		abAppTitle = (TextView) findViewById(android.R.id.candidatesArea);
		abOverflowButton = (ImageView) findViewById(android.R.id.button2);

		abBackground = findViewById(android.R.id.background);

		updatePref();
	}

	private void updatePref() {
		int tbHeight = Util.realDp(mPref.getInt(Common.KEY_WINDOW_TITLEBAR_SIZE,
												Common.DEFAULT_WINDOW_TITLEBAR_SIZE), getContext());
		((LinearLayout.LayoutParams) mTitleBar.getLayoutParams()).height = tbHeight;
		
		mIconType = mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
								 Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE);
		mLayout = buttonsLayoutStringToId(mPref.getString(Common.KEY_BUTTONS_LIST, Common.DEFAULT_BUTTONS_LIST));
		
		ActivityHooks.mOverlayTheme = new OverlayTheme(mPref, mResource, mIconType);
		mTitleBar.removeAllViews();
		TitleBarViewHelpers.loadTitleBarButtons();
		TitleBarViewHelpers.addButtons(mTitleBar, this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mPref.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPref.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		updatePref();
		InterActivity.sendUpdatePrefsBroadcast(this);
	}
	
	private ThemeItemAdapter populateButtonsSpinnerAdapter(){
		final ThemeItemAdapter adapter = new ThemeItemAdapter(getContext());

		adapter.add(new ThemeItem("Original",
								  "The original buttons layout: menu - title - minimize - maximize - close", 
								  Common.TITLEBAR_LAYOUT_ORIGINAL));
		adapter.add(new ThemeItem("Original w/Separate",
								  "The original buttons layout + separate windows: menu - title - separate - minimize - maximize - close", 
								  Common.TITLEBAR_LAYOUT_ORIGINAL_SEPARATE));
		adapter.add(new ThemeItem("Leftside",
								  "Leftside buttons layout like Ubuntu Unity: close - maximize - minimize - title - menu", 
								  Common.TITLEBAR_LAYOUT_LEFTSIDE));
		adapter.add(new ThemeItem("Leftside w/Separate",
								  "Leftside buttons + separate windows: close - maximize - minimize - separate - title - menu", 
								  Common.TITLEBAR_LAYOUT_LEFTSIDE_SEPARATE));
		adapter.add(new ThemeItem("Minimal Left",
								  "Minimal buttons layout: close - title - menu", 
								  Common.TITLEBAR_LAYOUT_MINIMAL_LEFT));
		adapter.add(new ThemeItem("Minimal Right",
								  "Another minimal buttons layout: menu - title - close", 
								  Common.TITLEBAR_LAYOUT_MINIMAL_RIGHT));
		adapter.mSelectedId = mIconType;
		return adapter;
	}
	
	private String buttonsLayoutIdToString(int id) {
		switch(id) {
			case Common.TITLEBAR_LAYOUT_ORIGINAL_SEPARATE:
				return Common.TITLEBAR_LAYOUT_ORIGINAL_SEPARATE_BUTTONS;
			case Common.TITLEBAR_LAYOUT_LEFTSIDE:
				return Common.TITLEBAR_LAYOUT_LEFTSIDE_BUTTONS;
			case Common.TITLEBAR_LAYOUT_LEFTSIDE_SEPARATE:
				return Common.TITLEBAR_LAYOUT_LEFTSIDE_SEPARATE_BUTTONS;
			case Common.TITLEBAR_LAYOUT_MINIMAL_LEFT:
				return Common.TITLEBAR_LAYOUT_MINIMAL_LEFT_BUTTONS;
			case Common.TITLEBAR_LAYOUT_MINIMAL_RIGHT:
				return Common.TITLEBAR_LAYOUT_MINIMAL_RIGHT_BUTTONS;
			case Common.TITLEBAR_LAYOUT_ORIGINAL:
			default:
				return Common.TITLEBAR_LAYOUT_ORIGINAL_BUTTONS;
		}
	}
	
	private int buttonsLayoutStringToId(String layout) {
		if(layout.equals(Common.TITLEBAR_LAYOUT_ORIGINAL_SEPARATE_BUTTONS))
			return Common.TITLEBAR_LAYOUT_ORIGINAL_SEPARATE;
		if(layout.equals(Common.TITLEBAR_LAYOUT_LEFTSIDE_BUTTONS))
			return Common.TITLEBAR_LAYOUT_LEFTSIDE;
		if(layout.equals(Common.TITLEBAR_LAYOUT_LEFTSIDE_SEPARATE_BUTTONS))
			return Common.TITLEBAR_LAYOUT_LEFTSIDE_SEPARATE;
		if(layout.equals(Common.TITLEBAR_LAYOUT_MINIMAL_LEFT_BUTTONS))
			return Common.TITLEBAR_LAYOUT_MINIMAL_LEFT;
		if(layout.equals(Common.TITLEBAR_LAYOUT_MINIMAL_RIGHT_BUTTONS))
			return Common.TITLEBAR_LAYOUT_MINIMAL_RIGHT;
		return Common.TITLEBAR_LAYOUT_ORIGINAL;
	}

	class ThemeItem {
		public final String title;
		public final String msg;
		public final int id;
		public ThemeItem(String _title, String _msg, int _id) {
			title = _title;
			msg = _msg;
			id = _id;
		}
		public ThemeItem(Resources r, int _title, int _msg, int _id) {
			title = r.getString(_title);
			msg = r.getString(_msg);
			id = _id;
		}
	}
	
	class ThemeItemAdapter extends ArrayAdapter<ThemeItem> {
		class ItemView extends LinearLayout {
			public TextView title;
			public TextView msg;
			public ItemView(Context context, LayoutInflater inflator) {
				super(context);
				inflator.inflate(R.layout.view_app_list, this);
				title = (TextView) findViewById(android.R.id.title);
				msg = (TextView) findViewById(android.R.id.message);
				findViewById(android.R.id.icon).setVisibility(View.GONE);

				int padding = Util.dp(8, context);
				setPadding(padding, padding, padding, padding);
			}
		}
		LayoutInflater mInflator;
		int mSelectedId;
		public ThemeItemAdapter(Context context) {
			super(context, 0);
			mInflator = (LayoutInflater) getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		}
		public View getView(int position, View v, ViewGroup parent) {
			if (v == null) {
				v = new ItemView(getContext(), mInflator);
			}
			ItemView convertView = (ItemView) v;

			final ThemeItem item = getItem(position);
			if (item != null) {
				final String title = item.title;
				final boolean isSelected = item.id == mSelectedId;
				convertView.title.setText(!isSelected ? title :
										  Html.fromHtml("<b><u>" + title + "</u></b>"));
				convertView.msg.setText(item.msg);
			}
			return convertView;
		}
	}
//	class ButtonsItem {
//		public final String title;
//		public final String msg;
//		public final String buttons;
//		public ThemeItem(Resources r, int _title, int _msg, int _id) {
//			title = r.getString(_title);
//			msg = r.getString(_msg);
//			id = _id;
//		}
//	}
//	class ButtonsItemAdapter extends ArrayAdapter<ButtonsItem> {
//		class ItemView extends LinearLayout {
//			public TextView title;
//			public TextView msg;
//			public ItemView(Context context, LayoutInflater inflator) {
//				super(context);
//				inflator.inflate(R.layout.view_app_list, this);
//				title = (TextView) findViewById(android.R.id.title);
//				msg = (TextView) findViewById(android.R.id.message);
//				findViewById(android.R.id.icon).setVisibility(View.GONE);
//
//				int padding = Util.dp(8, context);
//				setPadding(padding, padding, padding, padding);
//			}
//		}
//		LayoutInflater mInflator;
//		int mSelectedId;
//		public ButtonsItemAdapter(Context mContext) {
//			super(mContext, 0);
//			mInflator = (LayoutInflater) getContext().getSystemService(
//				Context.LAYOUT_INFLATER_SERVICE);
//		}
//		public View getView(int position, View v, ViewGroup parent) {
//			if (v == null) {
//				v = new ItemView(getContext(), mInflator);
//			}
//			ItemView convertView = (ItemView) v;
//
//			final ButtonsItem item = getItem(position);
//			if (item != null) {
//				final String title = item.title;
//				final boolean isSelected = item.id == mSelectedId;
//				convertView.title.setText(!isSelected ? title :
//										  Html.fromHtml("<b><u>" + title + "</u></b>"));
//				convertView.msg.setText(item.msg);
//			}
//			return convertView;
//		}
//	}
}
