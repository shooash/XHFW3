package com.zst.xposed.halo.floatingwindow3.prefs;

import java.util.Set;

import com.zst.xposed.halo.floatingwindow3.Common;
import com.zst.xposed.halo.floatingwindow3.R;
import com.zst.xposed.halo.floatingwindow3.prefs.adapters.AppAdapter;
import com.zst.xposed.halo.floatingwindow3.prefs.adapters.AppAdapter.AppItem;
import com.zst.xposed.halo.floatingwindow3.prefs.adapters.PackageNameAdapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.AdapterView.OnItemClickListener;
import java.util.*;
import com.zst.xposed.halo.floatingwindow3.*;

public class BlacklistActivity extends Activity {
	
	/* others */
	static final int ID_ADD = 1;
	static final int ID_HELP = 2;
	SharedPreferences mPref;
	
	/* main stuff */
	PackageNameAdapter mPkgAdapter;
	ListView mListView;
	
	/* app dialog stuff */
	AppAdapter dAdapter;
	Dialog dDialog;
	ProgressBar dProgressBar;
	ListView dListView;
	EditText dSearch;
	ImageButton dButton;
	
	@Override
	@SuppressLint("WorldReadableFiles")
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showFirstTimeHelper(false);
		mPref = getSharedPreferences(Common.PREFERENCE_PACKAGES_FILE, MODE_WORLD_READABLE);
		loadBlacklist();
		initAppList();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		// Avoid WindowLeaked Exception
		// http://publicstaticdroidmain.com/2012/01/avoiding-android-memory-leaks-part-1/
		if (dDialog != null && dDialog.isShowing()) {
			dDialog.dismiss();
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem help_item = menu.add(Menu.NONE, ID_HELP, 0, R.string.help);
		help_item.setIcon(R.drawable.blacklist_help);
		help_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		MenuItem setting_item = menu.add(Menu.NONE, ID_ADD, 0, R.string.add);
		setting_item.setIcon(R.drawable.blacklist_add);
		setting_item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case ID_ADD:
			dDialog.show();
			break;
		case ID_HELP:
			showFirstTimeHelper(true);
			break;
		}
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	private void showFirstTimeHelper(boolean force) {
		final SharedPreferences main = getSharedPreferences(Common.PREFERENCE_MAIN_FILE,
				MODE_WORLD_READABLE);
		if (!main.contains(Common.KEY_BLACKLIST_HELP) || force) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setMessage(R.string.pref_blacklist_dialog);
			if (!force) {
				alert.setPositiveButton(R.string.close_once, null);
				alert.setNegativeButton(R.string.close_forever, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						main.edit().putBoolean(Common.KEY_BLACKLIST_HELP, true).commit();
					}
				});
			}
			alert.show();    
		}
	}
	
	private void initAppList() {
		dDialog = new Dialog(this);
		dDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dDialog.setContentView(R.layout.dialog_blacklist);
		
		dProgressBar = (ProgressBar) dDialog.findViewById(R.id.progressBar1);
		dListView = (ListView) dDialog.findViewById(R.id.listView1);
		dSearch = (EditText) dDialog.findViewById(R.id.searchText);
		dButton = (ImageButton) dDialog.findViewById(R.id.searchButton);
		
		dAdapter = new AppAdapter(this, dProgressBar);
		dListView.setAdapter(dAdapter);
		dListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> v, View arg1, int pos, long arg3) {
				AppItem info = (AppItem) v.getItemAtPosition(pos);
				addApp(info.packageName);
				dDialog.dismiss();
			}
		});
		dButton.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				dAdapter.getFilter().filter(dSearch.getText().toString());
			}
		});
		
		dAdapter.update();
	}
	
	private void loadBlacklist() {
		mPkgAdapter = new PackageNameAdapter(this, getSetStrings()) {
			@Override
			public void onRemoveApp(String pkg) {
				removeApp(pkg);
			}
		};
		mListView = new ListView(this);
		mListView.setAdapter(mPkgAdapter);
		setContentView(mListView);
	}
	
	private Set<String> getSetStrings() {
		Map<String, ?> pkgList = mPref.getAll();
		Set<String> result = new HashSet<String>();
		for(Map.Entry<String, ?> E : pkgList.entrySet()){
			
			if(E.getValue() instanceof Integer && Util.isFlag((Integer)E.getValue(), Common.PACKAGE_BLACKLIST))
				result.add(E.getKey());
		}
		return result;
	}
	
	public void removeApp(String pkg) {
		mPref.edit().remove(pkg).commit();
		mPkgAdapter.update(getSetStrings());
		updateList();
	}
	
	public void addApp(String pkg) {
		int value = mPref.getInt(pkg, 0) | Common.PACKAGE_BLACKLIST;
		mPref.edit().putInt(pkg, value).commit();
		updateList();
	}
	
	private void updateList() {
		mPkgAdapter.update(getSetStrings());
	}
	
}
