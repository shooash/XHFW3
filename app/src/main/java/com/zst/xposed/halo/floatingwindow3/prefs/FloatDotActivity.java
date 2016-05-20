package com.zst.xposed.halo.floatingwindow3.prefs;
import android.app.*;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.*;
import android.os.*;
import com.zst.xposed.halo.floatingwindow3.*;
import java.util.prefs.*;
import android.widget.*;
import android.graphics.*;

public class FloatDotActivity extends Activity implements OnSharedPreferenceChangeListener
{
	SharedPreferences mPref;
	//Context mContext;
	int[] mColors = new int[4];
	int mCircleDiameter;
	ImageView snapDragger;
	ImageView floatLauncher;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_floatdot);
		snapDragger = (ImageView) findViewById(R.id.snapDragger);
		floatLauncher = (ImageView) findViewById(R.id.floatingLauncher);
		mPref = getSharedPreferences(Common.PREFERENCE_MAIN_FILE, MODE_WORLD_READABLE);
		updatePrefs();
//		getFragmentManager().beginTransaction().replace(R.id.floatDotPrefs,
//														new FloatDotFragment()).commit();
		
	}
	
	private void updatePrefs(){
		loadColors();
		mCircleDiameter=mPref.getInt(Common.KEY_FLOATDOT_SIZE, Common.DEFAULT_FLOATDOT_SIZE);
		snapDragger.setImageDrawable(Util.makeDoubleCircle(mColors[0], mColors[1], Util.realDp(mCircleDiameter, getApplicationContext()), Util.realDp(mCircleDiameter/4, getApplicationContext())));
		floatLauncher.setImageDrawable(Util.makeDoubleCircle(mColors[2], mColors[3], Util.realDp(mCircleDiameter, getApplicationContext()), Util.realDp(mCircleDiameter/4, getApplicationContext())));
		}
	
	private void loadColors(){
		mColors[0] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_OUTER1, Common.DEFAULT_FLOATDOT_COLOR_OUTER1));
		if(mPref.getBoolean(Common.KEY_FLOATDOT_SINGLE_COLOR_SNAP, Common.DEFAULT_FLOATDOT_SINGLE_COLOR_SNAP))
			mColors[1] = mColors[0];
		else
			mColors[1] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_INNER1, Common.DEFAULT_FLOATDOT_COLOR_INNER1));
		mColors[2] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_OUTER2, Common.DEFAULT_FLOATDOT_COLOR_OUTER2));
		if(mPref.getBoolean(Common.KEY_FLOATDOT_SINGLE_COLOR_LAUNCHER, Common.DEFAULT_FLOATDOT_SINGLE_COLOR_LAUNCHER))
			mColors[3] = mColors[2];
		else
			mColors[3] = Color.parseColor("#" + mPref.getString(Common.KEY_FLOATDOT_COLOR_INNER2, Common.DEFAULT_FLOATDOT_COLOR_INNER2));
	}
	
	private void sendRefreshCommand(String key){
		int item = 0;
		int size = 0;
		float alpha = 0;
		String mColor = null;
		if(key.equals(Common.KEY_FLOATDOT_COLOR_OUTER1))
			item=0;
		else if(key.equals(Common.KEY_FLOATDOT_COLOR_INNER1))
			item=1;
		else if(key.equals(Common.KEY_FLOATDOT_COLOR_OUTER2))
			item=2;
		else if(key.equals(Common.KEY_FLOATDOT_COLOR_INNER2))
			item=3;
		else if(key.equals(Common.KEY_FLOATDOT_SINGLE_COLOR_SNAP)){
			item = 1;
			if(mPref.getBoolean(key, Common.DEFAULT_FLOATDOT_SINGLE_COLOR_SNAP))
				mColor = mPref.getString(Common.KEY_FLOATDOT_COLOR_OUTER1, Common.DEFAULT_FLOATDOT_COLOR_OUTER1);
			else
				mColor = mPref.getString(Common.KEY_FLOATDOT_COLOR_INNER1, Common.KEY_FLOATDOT_COLOR_INNER1);		
		}
		else if(key.equals(Common.KEY_FLOATDOT_SINGLE_COLOR_LAUNCHER)){
			item = 3;
			if(mPref.getBoolean(key, Common.DEFAULT_FLOATDOT_SINGLE_COLOR_LAUNCHER))
				mColor = mPref.getString(Common.KEY_FLOATDOT_COLOR_OUTER2, Common.DEFAULT_FLOATDOT_COLOR_OUTER2);
			else
				mColor = mPref.getString(Common.KEY_FLOATDOT_COLOR_INNER2, Common.KEY_FLOATDOT_COLOR_INNER2);		
		}
		else if(key.equals(Common.KEY_FLOATDOT_SIZE)){
			size = mPref.getInt(key, Common.DEFAULT_FLOATDOT_SIZE);
		}
		else if(key.equals(Common.KEY_FLOATDOT_ALPHA)){
			alpha = mPref.getFloat(key, Common.DEFAULT_FLOATDOT_ALPHA);
		}
		else return;
		
		if(mColor==null && size==0 && alpha == 0) mColor = mPref.getString(key, null);
		Intent mIntent = new Intent(Common.UPDATE_FLOATDOT_PARAMS);
		mIntent.putExtra("color", mColor);
		mIntent.putExtra("size", size);
		mIntent.putExtra("alpha", alpha);
		mIntent.putExtra("item", item);
		mIntent.setPackage(Common.THIS_MOD_PACKAGE_NAME);
		getApplicationContext().sendBroadcast(mIntent);
	}
	
	private void enableFloatDotLauncher(boolean enable){
		Intent mIntent = new Intent(Common.UPDATE_FLOATDOT_PARAMS);
		mIntent.putExtra(Common.KEY_FLOATDOT_LAUNCHER_ENABLED, enable);
		mIntent.setPackage(Common.THIS_MOD_PACKAGE_NAME);
		getApplicationContext().sendBroadcast(mIntent);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences p1, String key)
	{
		updatePrefs();
		if(key.equals(Common.KEY_FLOATDOT_LAUNCHER_ENABLED)){
			enableFloatDotLauncher(mPref.getBoolean(key, Common.DEFAULT_FLOATDOT_LAUNCHER_ENABLED));
			return;
		}
		sendRefreshCommand(key);
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
}
