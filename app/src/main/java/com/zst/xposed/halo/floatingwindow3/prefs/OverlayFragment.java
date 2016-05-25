package com.zst.xposed.halo.floatingwindow3.prefs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.R;

public class OverlayFragment extends PreferenceFragment {

	static OverlayFragment mInstance;
	SharedPreferences mPref;

	public static OverlayFragment getInstance() {
		if (mInstance == null) {
			mInstance = new OverlayFragment();
		}
		return mInstance;
	}
	@Override
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
		getPreferenceManager().setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
		addPreferencesFromResource(R.xml.pref_overlay);
		findPreference("titlebar_theme").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getActivity().startActivity(new Intent(getActivity(), TitleBarSettingsActivity.class));
					return false;
				}
			});
		findPreference("floatdot_theme").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					Intent intent = new Intent();
					intent.setClass(getActivity(), FloatDotActivity.class);
					startActivityForResult(intent, 0); 
					
					return false;
				}
			});
		findPreference("launcher_list").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getActivity().startActivity(new Intent(getActivity(), LauncherListActivity.class));
					return false;
				}
			});
		mPref = getActivity().getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
												   PreferenceActivity.MODE_WORLD_READABLE);
	}
}
