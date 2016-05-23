package com.zst.xposed.halo.floatingwindow3.floatdot;
import android.view.*;
import android.widget.*;
import android.content.*;
import android.util.*;
import android.graphics.drawable.*;
import android.graphics.*;
import android.widget.TableRow.*;
import com.zst.xposed.halo.floatingwindow3.*;
import java.util.*;
import de.robv.android.xposed.*;
import android.content.pm.*;
import android.content.pm.PackageManager.*;
import android.app.*;
import android.os.*;

public class FloatLauncher
{
	Context mContext;
	PackageManager mPackageManager;
	int mScreenWidth;
	int mScreenHeight;
	int MINIMAL_WIDTH;
	int MINIMAL_HEIGHT;
	ArrayList<PackageItem> itemsList = new ArrayList<PackageItem>();
	ArrayList<String> itemsIndex = new ArrayList<String>();
	ArrayList<String> savedPackages = new ArrayList<String>();
	public PopupWindow popupWin = new PopupWindow();
	public long dismissedTime;
	SharedPreferences SavedPackages;
	
	public FloatLauncher(Context sContext){
		mContext = sContext;
		mPackageManager = mContext.getPackageManager();
		regBroadcastReceiver();
		SavedPackages = sContext.getSharedPreferences(Common.PREFERENCE_PACKAGES_FILE, Context.MODE_MULTI_PROCESS);
		loadSavedPackages();
		//fillMenu(itemsList);
	}
	
	public void showMenu(View anchor, WindowManager.LayoutParams paramsF, int offset){
		if(popupWin.isShowing()) {
			popupWin.dismiss();
			return;
		}
		refreshScreenSize();
		refreshMinimalSize();
		ListView lv = new ListView(mContext);
		LauncherListAdapter adapter = new LauncherListAdapter(mContext, itemsList, popupWin);
		lv.setAdapter(adapter);
		loadSavedPackages();
		
		popupWin.setContentView(lv);
		popupWin.setWidth(WindowManager.LayoutParams.WRAP_CONTENT);
		popupWin.setHeight(MINIMAL_HEIGHT);
		int width = View.MeasureSpec.getSize(popupWin.getWidth());
		int height = View.MeasureSpec.getSize(popupWin.getHeight());
		boolean putLeft = false;
		if(width>mScreenWidth-paramsF.x-offset || width == 0)
			width = mScreenWidth-paramsF.x-offset;
		if(height > mScreenHeight/3 || height == 0)
			height = mScreenHeight/3;
		if(width<MINIMAL_WIDTH){
			width=MINIMAL_WIDTH;
			putLeft=true;
		}
//		if(height<MINIMAL_HEIGHT)
//			height=MINIMAL_HEIGHT;
		popupWin.setWidth(MINIMAL_WIDTH);
		popupWin.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		popupWin.setOutsideTouchable(true);
		popupWin.setClippingEnabled(true);
		ColorDrawable cd = new ColorDrawable(Color.parseColor("#AA333333"));
		popupWin.setBackgroundDrawable(cd);
		popupWin.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
		popupWin.setOnDismissListener(new PopupWindow.OnDismissListener(){
				@Override
				public void onDismiss()
				{
					dismissedTime = SystemClock.uptimeMillis();
				}
		});
		int x = putLeft? paramsF.x-width: paramsF.x+offset;
		int y = paramsF.y/*+offset-Util.getStatusBarHeight(mContext)*/-mScreenHeight/2; //-height/2;
		addSavedPackages();
		popupWin.showAtLocation(anchor, Gravity.CENTER_VERTICAL | Gravity.LEFT, x, y);
	}
	
	private void loadSavedPackages(){
		//TODO add pinned apps
		Map<String, ?> mItems = SavedPackages.getAll();
		if(!mItems.containsValue(Common.PACKAGE_LAUNCHER_SAVED))
			return;
		for(Map.Entry<String,?> item : mItems.entrySet()){
//			if(!(item.getValue() instanceof int))
//				continue;
			if(Util.isFlag(item.getValue(), Common.PACKAGE_LAUNCHER_SAVED)
				&&!savedPackages.contains(item.getKey()))
				savedPackages.add(item.getKey());
		}
	}
	
	private void addSavedPackages(){
		for(String pkg : savedPackages)
		{
			if(!(itemsIndex.contains(pkg)))
				addItem(pkg, 0, 0);
			}
	}
	
	private void refreshScreenSize(){
		final WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		final DisplayMetrics metrics = new DisplayMetrics();
		wm.getDefaultDisplay().getMetrics(metrics);

		mScreenHeight = metrics.heightPixels;
		mScreenWidth = metrics.widthPixels;
	}
	
	private void refreshMinimalSize(){
		MINIMAL_WIDTH=mScreenWidth/4;
		MINIMAL_HEIGHT=0;
		int minimorum_width = Util.realDp(150, mContext);
		if(MINIMAL_WIDTH<minimorum_width)
			MINIMAL_WIDTH=minimorum_width;
	}
	
	private void addItem(String pkgName, int taskId, int sGravity){
		if(itemsIndex.contains(pkgName)){
			//if(itemsList.get(itemsIndex.indexOf(pkgName)).taskId==0)
			updateItem(pkgName, taskId);
			return;
		}
		itemsList.add(0, new PackageItem(mContext.getPackageManager(), pkgName, taskId, sGravity, savedPackages.contains(pkgName)));
		itemsIndex.add(0, pkgName);
	}
	
	private void removeItem(String pkgName){
		if(!itemsIndex.contains(pkgName))
			return;
		itemsList.remove(itemsIndex.indexOf(pkgName));
		itemsIndex.remove(pkgName);
	}
	
	private void updateItem(String pkgName, int mTaskId){
		if(!itemsIndex.contains(pkgName)||itemsList.size()==0)
			return;
		int index = itemsIndex.indexOf(pkgName);
		PackageItem pi = itemsList.get(index);
		pi.taskId = mTaskId;
		pi.isFavorite = false;
		//force it appear at top of the list
		itemsList.remove(index);
		itemsIndex.remove(index);
		itemsList.add(0, pi);
		itemsIndex.add(0, pkgName);
	}
	
	
	final BroadcastReceiver br = new BroadcastReceiver(){

		@Override
		public void onReceive(Context sContext, Intent sIntent)
		{
			if(sIntent.getAction().equals(Common.ORIGINAL_PACKAGE_NAME + ".APP_REMOVED")){
				boolean mCompletely = sIntent.getBooleanExtra("removeCompletely", false);
				if(mCompletely)
					removeItem(sIntent.getStringExtra("packageName"));
				else
					updateItem(sIntent.getStringExtra("packageName"), 0);
			}
			String pkgName = sIntent.getStringExtra("packageName");
			Log.d("Xposed", "FloatingLauncher broadcast package " + (pkgName==null?"null":pkgName));
			if(pkgName==null) return;
			
			int sGravity = sIntent.getIntExtra("float-gravity", 0);
			int taskId = sIntent.getIntExtra("float-taskid", 0);
			if(taskId==0)
				return;
			addItem(pkgName, taskId, sGravity);
		}
	};
	
	private void regBroadcastReceiver(){
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(Common.ORIGINAL_PACKAGE_NAME + ".APP_LAUNCHED");
		mIntentFilter.addAction(Common.ORIGINAL_PACKAGE_NAME + ".APP_REMOVED");
		mContext.getApplicationContext().registerReceiver(br, mIntentFilter);
	}
	
//	private void drawFocusFrame(){
//		if(!mAeroFocusWindow) return;
//		//hide previous outlines
//		hideFocusFrame(mContext.getApplicationContext());
//		if(!mWindowHolder.isSnapped) return;
//		//send new params
//		Intent mIntent = new Intent(Common.SHOW_OUTLINE);
//		int[] array = {mWindowHolder.x, mWindowHolder.y, mWindowHolder.height, mWindowHolder.width};
//		mIntent.putExtra(Common.INTENT_APP_PARAMS, array);
//		mIntent.putExtra(Common.INTENT_APP_FOCUS, true);
//		mWindowHolder.mActivity.getApplicationContext().sendBroadcast(mIntent);
//		showFocusOutline = true;
//	}
//	
//	private static void hideFocusFrame(Context mContext){
//		mContext.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
//		showFocusOutline = false;
//	}
}

class PackageItem implements Comparable<PackageItem>{
	public Drawable packageIcon;
	public String packageName;
	public CharSequence title;
	public int snapGravity;
	public int taskId;
	public boolean isFavorite;
	
	public PackageItem(PackageManager mPackageManager, String mPackageName, int mTaskId, int sGravity, boolean isThisFavorite){
		
		Drawable icon;
		try{
			icon = mPackageManager.getApplicationIcon(mPackageName);
		} catch (Throwable t){
			icon = new ColorDrawable(Color.BLACK);
		}
		packageIcon = icon;
		try
		{
			title = mPackageManager.getApplicationInfo(mPackageName, 0).loadLabel(mPackageManager);
		} catch (Throwable e)
		{
			title = mPackageName;
		}
		taskId = mTaskId;
		snapGravity = sGravity;
		packageName = mPackageName;
		isFavorite = isThisFavorite;
		// (packageName);
		//= mAppInfo.loadIcon(
	}
	
	public PackageItem(String mPackageName, String mTitle, int mTaskId, Drawable icon, int sGravity, boolean isThisFavorite){
		packageName = mPackageName;
		title = mTitle;
		taskId = mTaskId;
		packageIcon = icon;
		snapGravity = sGravity;
		isFavorite = isThisFavorite;
		
	}
	public PackageItem(String mPackageName){
		packageName = mPackageName;
		packageIcon = new ColorDrawable(Color.BLACK);
	}
	
	@Override
	public int compareTo(PackageItem another) {
		return this.packageName.toString().compareTo(another.packageName.toString());
	}
}

class LauncherListAdapter extends ArrayAdapter<PackageItem>{
	Context mContext;
	PopupWindow popupWin;
	public LauncherListAdapter(Context sContext, ArrayList<PackageItem> itemsList, PopupWindow mPopupWin){
		super(sContext, 0, itemsList);
		mContext=sContext;
		popupWin=mPopupWin;
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		// Get the data item for this position
		final PackageItem item = getItem(position);    
		// Check if an existing view is being reused, otherwise inflate the view
		if (convertView == null) {
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.floatdot_launcher_menuitem, parent, false);
		}
		// Lookup view for data population
		ImageView mIcon = (ImageView) convertView.findViewById(android.R.id.icon);
		TextView mTitle = (TextView) convertView.findViewById(android.R.id.text1);
		ImageView mPoint = (ImageView) convertView.findViewById(android.R.id.button1);
		// Populate the data into the template view using the data object
		mIcon.setImageDrawable(item.packageIcon);
		int mColor = item.isFavorite&&item.taskId==0?Color.WHITE:Color.GREEN;
		mPoint.setImageDrawable(Util.makeCircle(mColor, Util.realDp(5, mContext)));
		
		mTitle.setText(item.title);
		
		convertView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View p1)
				{
					if(item.taskId==0 || !Util.moveToFront(mContext, item.taskId))
						Util.startApp(mContext, item.packageName);
					popupWin.dismiss();
				}
		});
		// Return the completed view to render on screen
		return convertView;
	}
}
