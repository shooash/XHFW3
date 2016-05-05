package com.zst.xposed.halo.floatingwindow3;

import com.zst.xposed.halo.floatingwindow3.prefs.MainFragment;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import com.zst.xposed.halo.floatingwindow3.prefs.*;
import android.app.*;
import android.graphics.drawable.*;

public class MainPreference extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_viewpager);
		
		FragmentPagerAdapter adapter = new FragmentPagerAdapter(getFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				switch (position) {
				case 0:
					return MainFragment.getInstance();
				case 1:
					return MovingFragment.getInstance();
				case 2:
					return BehaviorFragment.getInstance();
				case 3:
					return OverlayFragment.getInstance();
//				case 2:
//					return TestingActivity.getInstance();
				}
				return new Fragment();
			}
			
			@Override
			public String getPageTitle(int pos) {
				switch (pos) {
				case 0:
					return getResources().getString(R.string.pref_main_top_title);
				case 1:
					return getResources().getString(R.string.pref_movable_top_title);
				case 2:
					return getResources().getString(R.string.pref_behavior_title);
				case 3:
					return getResources().getString(R.string.pref_overlay_title);
//				case 2:
//					return getResources().getString(R.string.pref_testing_top_title);
				}
				return "";
			}
			
			@Override
			public int getCount() {
				return 4;
			}
		};
		ViewPager viewPager = (ViewPager) findViewById(R.id.view_pager);
		viewPager.setAdapter(adapter);
		
		PagerTabStrip pts = (PagerTabStrip) findViewById(R.id.pager_title_strip);
		pts.setTabIndicatorColor(0xFF333333);
		pts.setTextColor(0xFF111111);
		pts.setBackgroundColor(Color.TRANSPARENT);
		
		ActionBar bar = getActionBar();
		bar.setBackgroundDrawable(new ColorDrawable(0xFFAA0000)); 
		
	}
}
