package com.zst.xposed.halo.floatingwindow3.themable;
import android.graphics.drawable.*;
import com.zst.xposed.halo.floatingwindow3.*;

public class OverlayTheme
{
	final int currentThemeId;;
	final titleBarThemeHolder currentTheme;
	final String buttonsList;
	
	public OverlayTheme(){
		this(MainXposed.mPref.getInt(Common.KEY_WINDOW_TITLEBAR_ICON_TYPE,
					 Common.DEFAULT_WINDOW_TITLEBAR_ICONS_TYPE));
	}
	public OverlayTheme(int theme) {
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
		return MainXposed.mPref.getString(Common.KEY_BUTTONS_LIST, Common.DEFAULT_BUTTONS_LIST);
	}
	
	private void setCurrentTheme() {
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
