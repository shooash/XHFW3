package com.zst.xposed.halo.floatingwindow3.themable;
import android.graphics.drawable.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.content.*;
import android.content.res.*;

public class OverlayTheme
{
	final int currentThemeId;;
	final titleBarThemeHolder currentTheme;
	final String buttonsList;
	SharedPreferences mSharedPrefs = null;
	Resources mResources = null;
	
	public OverlayTheme(){
		this(-1);
	}
	
	public OverlayTheme(final SharedPreferences prefs, final Resources res) {
		this(prefs, res, prefs.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
					Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE));
	}
	
	public OverlayTheme(final SharedPreferences prefs, final Resources res, int theme) {
		mSharedPrefs = prefs;
		mResources = res;
		currentThemeId = theme;
		currentTheme = new titleBarThemeHolder();
		buttonsList = setButtonsList();
		setCurrentTheme();
	}
	
	public OverlayTheme(int theme) {
//		if(MainXposed.mPref!=null)
//			MainXposed.mPref.reload();
		if(theme==-1)
			theme = MainXposed.mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
											Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE);
		currentThemeId = theme;
		currentTheme = new titleBarThemeHolder();
		setCurrentTheme();
		buttonsList = setButtonsList();
	}
	
	public Drawable getButton(final char id){
		return currentTheme.get(id);
	}
	
	public String getButtonsList() {
		return buttonsList;
	}
	
	private String setButtonsList() {
		return mSharedPrefs==null?
			MainXposed.mPref.getString(Common.KEY_BUTTONS_LIST, Common.DEFAULT_BUTTONS_LIST)
			:mSharedPrefs.getString(Common.KEY_BUTTONS_LIST, Common.DEFAULT_BUTTONS_LIST);
	}
	
	private void setCurrentTheme() {
		if(mResources!=null) {
			setCurrentThemeLocal();
			return;
		}
		switch (currentThemeId) {
			case Common.TITLEBAR_ICON_ORIGINAL:
				currentTheme.close = MainXposed.sModRes.getDrawable(R.drawable.movable_title_close_old);
				currentTheme.maximize = MainXposed.sModRes.getDrawable(R.drawable.movable_title_max_old);
				currentTheme.minimize = MainXposed.sModRes.getDrawable(R.drawable.movable_title_min_old);
				currentTheme.menu = MainXposed.sModRes.getDrawable(R.drawable.movable_title_more_old);
				currentTheme.separate = MainXposed.sModRes.getDrawable(R.drawable.movable_title_more_old);
				break;
			case Common.TITLEBAR_ICON_BachMinuetInG:
				currentTheme.close = MainXposed.sModRes.getDrawable(R.drawable.movable_title_close);
				currentTheme.maximize = MainXposed.sModRes.getDrawable(R.drawable.movable_title_max);
				currentTheme.minimize = MainXposed.sModRes.getDrawable(R.drawable.movable_title_min);
				currentTheme.menu = MainXposed.sModRes.getDrawable(R.drawable.movable_title_more);
				currentTheme.separate = MainXposed.sModRes.getDrawable(R.drawable.movable_title_more);
				break;
			case Common.TITLEBAR_ICON_SSNJR2002:
				currentTheme.close = MainXposed.sModRes.getDrawable(R.drawable.movable_title_close_ssnjr);
				currentTheme.maximize = MainXposed.sModRes.getDrawable(R.drawable.movable_title_max_ssnjr);
				currentTheme.minimize = MainXposed.sModRes.getDrawable(R.drawable.movable_title_min_ssnjr);
				currentTheme.menu = MainXposed.sModRes.getDrawable(R.drawable.movable_title_more_ssnjr);
				currentTheme.separate = MainXposed.sModRes.getDrawable(R.drawable.movable_title_separate_ssnjr);
				break;
		}
	}
	
	private void setCurrentThemeLocal(){
		switch (currentThemeId) {
			case Common.TITLEBAR_ICON_ORIGINAL:
				currentTheme.close = mResources.getDrawable(R.drawable.movable_title_close_old);
				currentTheme.maximize = mResources.getDrawable(R.drawable.movable_title_max_old);
				currentTheme.minimize = mResources.getDrawable(R.drawable.movable_title_min_old);
				currentTheme.menu = mResources.getDrawable(R.drawable.movable_title_more_old);
				currentTheme.separate = mResources.getDrawable(R.drawable.movable_title_more_old);
				break;
			case Common.TITLEBAR_ICON_BachMinuetInG:
				currentTheme.close = mResources.getDrawable(R.drawable.movable_title_close);
				currentTheme.maximize = mResources.getDrawable(R.drawable.movable_title_max);
				currentTheme.minimize = mResources.getDrawable(R.drawable.movable_title_min);
				currentTheme.menu = mResources.getDrawable(R.drawable.movable_title_more);
				currentTheme.separate = mResources.getDrawable(R.drawable.movable_title_more);
				break;
			case Common.TITLEBAR_ICON_SSNJR2002:
				currentTheme.close = mResources.getDrawable(R.drawable.movable_title_close_ssnjr);
				currentTheme.maximize = mResources.getDrawable(R.drawable.movable_title_max_ssnjr);
				currentTheme.minimize = mResources.getDrawable(R.drawable.movable_title_min_ssnjr);
				currentTheme.menu = mResources.getDrawable(R.drawable.movable_title_more_ssnjr);
				currentTheme.separate = mResources.getDrawable(R.drawable.movable_title_separate_ssnjr);
				break;
		}
	}
	
	class titleBarThemeHolder {
		Drawable close = null;
		Drawable maximize = null;
		Drawable minimize = null;
		Drawable menu = null;
		Drawable separate = null;
		
		Drawable get(char id) {
			/* so the buttons are:
			 m for more
			 t for title
			 - for minimize
			 + for maximize
			 x for close
			 s for constant move
			 */
			switch(id) {
				case 'm':
					return menu;
				case '-':
					return minimize;
				case '+':
					return maximize;
				case 'x':
					return close;
				case 's':
					return separate;
				default:
					return null;
			}
		}
	}
	
//	
}
