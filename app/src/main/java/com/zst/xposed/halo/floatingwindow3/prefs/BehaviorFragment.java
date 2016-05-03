package com.zst.xposed.halo.floatingwindow3.prefs;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.R;
import android.app.*;
import android.widget.*;
import android.view.*;

public class BehaviorFragment extends PreferenceFragment {

	static BehaviorFragment mInstance;
	SharedPreferences mPref;

	public static BehaviorFragment getInstance() {
		if (mInstance == null) {
			mInstance = new BehaviorFragment();
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
		addPreferencesFromResource(R.xml.pref_behavior);
		findPreference("window_whitelist").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getActivity().startActivity(new Intent(getActivity(), WhitelistActivity.class));
					return false;
				}
			});
		findPreference("window_blacklist").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getActivity().startActivity(new Intent(getActivity(), BlacklistActivity.class));
					return false;
				}
			});
		findPreference(Common.KEY_KEYBOARD_MODE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					showKeyboardDialog();
					return true;
				}
			});
		mPref = getActivity().getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
												   PreferenceActivity.MODE_WORLD_READABLE);
	}
	
	private void showKeyboardDialog() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		final ListView modeList = new ListView(getActivity());

		builder.setView(modeList);
		builder.setTitle(R.string.pref_keyboard_title);

		final AlertDialog dialog = builder.create();
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
																	  android.R.layout.simple_list_item_1, android.R.id.text1);

		adapter.add(getResources().getString(R.string.keyboard_default));
		adapter.add(getResources().getString(R.string.keyboard_pan));
		adapter.add(getResources().getString(R.string.keyboard_scale));

		modeList.setAdapter(adapter);
		modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
					String title = ((TextView) view.findViewById(android.R.id.text1))
						.getText().toString();
					if (title.equals(getResources().getString(R.string.keyboard_default))) {
						mPref.edit().putInt(Common.KEY_KEYBOARD_MODE, 1).commit();
					} else if (title.equals(getResources().getString(R.string.keyboard_pan))) {
						mPref.edit().putInt(Common.KEY_KEYBOARD_MODE, 2).commit();
					} else if (title.equals(getResources().getString(R.string.keyboard_scale))) {
						mPref.edit().putInt(Common.KEY_KEYBOARD_MODE, 3).commit();
					}
					Toast.makeText(getActivity(), title, Toast.LENGTH_SHORT).show();
					dialog.dismiss();
				}
			});
		dialog.show();
	}
}
