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
import android.widget.AdapterView.*;


public class FloatLauncher
{
	Context mContext;
	PackageManager mPackageManager;
	int mScreenWidth;
	int mScreenHeight;
	int MINIMAL_WIDTH;
	int MINIMAL_HEIGHT;
	ListView lv = null;
	boolean updateMenu;
	boolean mPopUpSet;
	ArrayList<PackageItem> itemsList = new ArrayList<PackageItem>();
	//ArrayList<String> itemsIndex = new ArrayList<String>();
	Map<String, PackageItem> itemsIndex = new HashMap<>();
	ArrayList<String> savedPackages = new ArrayList<String>();
	public PopupWindow popupWin = new PopupWindow();
	PopupWindow subPopupMenu = new PopupWindow();
	ListView subListView = null;
	public long dismissedTime;
	SharedPreferences SavedPackages;
	PackageManager pm;
	LauncherListAdapter adapter;
	WindowManager.LayoutParams mParams;
	Handler mHandler = new Handler();
	View mAnchor;
	int[] position = new int[3];
	int mFloatFlag = Common.FLAG_FLOATING_WINDOW;
	boolean subMenuVisible;
	
	final int ACTION_CLOSE = 1;
	final int ACTION_HALOFY = 2;
	final int ACTION_UNHALOFY = 3;
	final int ACTION_HALOFY_TOP = 4;
	final int ACTION_UNHALOFY_TOP = 5;
	final int ACTION_ADD_TO_FAVORITES = 6;
	final int ACTION_REMOVE_FROM_FAVORITES = 7;
	
	
	public FloatLauncher(Context sContext, int flag){
		mContext = sContext;
		mPackageManager = mContext.getPackageManager();
		regBroadcastReceiver();
		SavedPackages = sContext.getSharedPreferences(Common.PREFERENCE_PACKAGES_FILE, Context.MODE_MULTI_PROCESS);
		pm = mContext.getPackageManager();
		mFloatFlag = flag;
		refreshScreenSize();
		refreshMinimalSize();
		setupMenu();
	}
	
	public void setAnchor(View anchor){
		mAnchor = anchor;
	}
	
	public void setupMenu(){
		lv = new ListView(mContext);
		lv.setOnItemClickListener(new OnItemClickListener(){

				@Override
				public void onItemClick(AdapterView<?> p1, View v, int pos, long p4)
				{
					LauncherListAdapter.ViewHolder holder = (LauncherListAdapter.ViewHolder) v.getTag();
					if(holder.taskId==0 || !Util.moveToFront(mContext, holder.taskId))
						Util.startApp(mContext, holder.packageName);
					popupWin.dismiss();
				}
				
		
			});
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){

				@Override
				public boolean onItemLongClick(AdapterView<?> p1, View v, int p3, long p4)
				{
					int y = View.MeasureSpec.getSize(popupWin.getHeight())/2 - View.MeasureSpec.getSize(v.getHeight())*3/2 - (int) v.getY();
					LauncherListAdapter.ViewHolder holder = (LauncherListAdapter.ViewHolder) v.getTag();
					showSubMenu(mAnchor, mContext, position[0], position[1], Util.realDp(50, mContext),- y, holder.packageName, 
								new String[]{"Close", "Restart as movable", "Restart as fullscreen", (savedPackages.contains(holder.packageName)?"Remove from favs":"Add to favs") }, 
								new int[]{ACTION_CLOSE, ACTION_HALOFY, ACTION_UNHALOFY,(savedPackages.contains(holder.packageName)?ACTION_REMOVE_FROM_FAVORITES:ACTION_ADD_TO_FAVORITES)});
					return true;
				}

			
		});

		adapter = new LauncherListAdapter(mContext, itemsList, popupWin);
		adapter.setNotifyOnChange(true);
		
		lv.setAdapter(adapter);
		loadSavedPackages();
		addSavedPackages();
		
		
		updateMenu = false;
	}
	
	public void setupPopup(){
		//final ColorDrawable cd = new ColorDrawable(Color.parseColor("#AA333333"));
		popupWin.setBackgroundDrawable(mContext.getResources().getDrawable( R.drawable.round_rect ));
		popupWin.setOutsideTouchable(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			popupWin.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
		}
		popupWin.setAnimationStyle(android.R.style.Animation);
		popupWin.setOnDismissListener(new PopupWindow.OnDismissListener(){
				@Override
				public void onDismiss()
				{
					dismissedTime = SystemClock.uptimeMillis();
				}
			});
		
		mPopUpSet = true;
	}
	
	public void showMenu(View anchor, WindowManager.LayoutParams paramsF, int offset){
		mAnchor = anchor;
		if(subMenuVisible)
			return;
		mParams = paramsF;
		if(popupWin.isShowing()) {
			popupWin.dismiss();
			return;
		}
		refreshScreenSize();
		refreshMinimalSize();
		if(lv==null)
			setupMenu();
		if(!mPopUpSet)
			setupPopup();
		
		popupWin.setContentView(lv);
		popupWin.setWidth(MINIMAL_WIDTH);
		//popupWin.setHeight(MeasureSpec.makeMeasureSpec(mScreenHeight/3,MeasureSpec.AT_MOST));
		//popupWin.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		lv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		int rows = adapter.getCount();
		int height = lv.getMeasuredHeight() * rows;
		if(height>mScreenHeight/3)
			height = mScreenHeight/3;
		popupWin.setHeight(height);
		int width =  MINIMAL_WIDTH;
		boolean putLeft = false;
		if(width>mScreenWidth-paramsF.x-offset){
			putLeft=true;
		}
		position[0] = putLeft? paramsF.x-width: paramsF.x+offset;
		position[1] = paramsF.y-mScreenHeight/2; //-height/2;
		position[2] = offset;
		popupWin.showAtLocation(mAnchor, Gravity.CENTER_VERTICAL | Gravity.LEFT, position[0], position[1]);

	}
	
	private void loadSavedPackages(){
		final Set<String> mItems = new HashSet<String>(SavedPackages.getStringSet("launcher", new HashSet<String>()));
		savedPackages = new ArrayList<String>(mItems);
	}
	
	private void addSavedPackages(){
		for(String pkg : savedPackages)
		{
			//if(!(itemsIndex.contains(pkg)))
			if(!itemsIndex.containsKey(pkg))
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
		if(pkgName==null||pkgName.equals(""))
			return;
		if(itemsIndex.containsKey(pkgName)){
			updateItem(pkgName, taskId);
			return;
		}
		PackageItem pi = new PackageItem(pm, pkgName, taskId, sGravity, savedPackages.contains(pkgName));
		adapter.insert(pi, 0);
		//itemsList.add(0, new PackageItem(pm, pkgName, taskId, sGravity, savedPackages.contains(pkgName)));
		itemsIndex.put(pkgName, pi);
	}
	
	private void removeItem(String pkgName){
		if(!itemsIndex.containsKey(pkgName))
			return;
		adapter.remove(itemsIndex.get(pkgName));
		itemsIndex.remove(pkgName);
	}
	
	private void updateItem(String pkgName, int mTaskId){
		
		if(!itemsIndex.containsKey(pkgName))
			return;
		PackageItem pi = itemsIndex.get(pkgName);
		adapter.remove(pi);
		itemsIndex.remove(pi);
		pi.taskId = mTaskId;
		pi.isFavorite = savedPackages.contains(pkgName);
		itemsIndex.put(pkgName, pi);
		adapter.insert(pi, 0);
	}
	
	private void refreshAll(){
		SavedPackages = mContext.getSharedPreferences(Common.PREFERENCE_PACKAGES_FILE, Context.MODE_MULTI_PROCESS);
		popupWin = new PopupWindow();
		itemsIndex = new HashMap<>();
		itemsList = new ArrayList<PackageItem>();
		savedPackages = new ArrayList<String>();
		lv = null;
		adapter = null;
		refreshScreenSize();
		refreshMinimalSize();
		mPopUpSet = false;
	}
	
	private void refreshFavorites(){
		popupWin.dismiss();
		savedPackages.clear();
		loadSavedPackages();
		addSavedPackages();
	}
	
	public void removeAppFromFavs(final String pkg) {
		Set<String> pkgs = new HashSet<String>(SavedPackages.getStringSet("launcher", new HashSet<String>()));
		pkgs.remove(pkg);
		SavedPackages.edit().putStringSet("launcher", pkgs).apply();
//		mPref.edit().remove(pkg).commit();
//		mPkgAdapter.update(getSetStrings());
		refreshFavorites();
	}

	public void addAppToFavs(final String pkg) {
		Set<String> pkgs = new HashSet<String>(SavedPackages.getStringSet("launcher", new HashSet<String>()));
		if(pkgs.contains(pkg))
			return;
		pkgs.add(pkg);
		SavedPackages.edit().putStringSet("launcher", pkgs).apply();
		refreshFavorites();
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
				setupMenu();
				return;
			}
			else if(sIntent.getAction().equals(Common.ORIGINAL_PACKAGE_NAME + ".APP_LAUNCHED")){
				String pkgName = sIntent.getStringExtra("packageName");
				//Log.d("Xposed", "FloatingLauncher broadcast package " + (pkgName==null?"null":pkgName));
				if(pkgName==null) return;

				int sGravity = sIntent.getIntExtra("float-gravity", 0);
				int taskId = sIntent.getIntExtra("float-taskid", 0);
				if(taskId==0)
					return;
				addItem(pkgName, taskId, sGravity);
				setupMenu();
//				if(adapter!=null)
//					adapter.notifyDataSetChanged();
				
			}
			else if(sIntent.getAction().equals(Common.UPDATE_FLOATLAUNCHER_PARAMS)){
				popupWin.dismiss();
				refreshAll();
			}
			
		}
	};
	
	private void regBroadcastReceiver(){
		IntentFilter mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(Common.ORIGINAL_PACKAGE_NAME + ".APP_LAUNCHED");
		mIntentFilter.addAction(Common.ORIGINAL_PACKAGE_NAME + ".APP_REMOVED");
		mIntentFilter.addAction(Common.UPDATE_FLOATLAUNCHER_PARAMS);
		mContext.getApplicationContext().registerReceiver(br, mIntentFilter);
	}
	
	
	public void showSubMenu(final View anchor, final Context sContext, final int x, final int y, final int x_offset, final int y_offset, final String packageName, final String[] labels, final int[] actions){
		setAnchor(anchor);
		subListView = new ListView(mContext);
		ArrayList<String> list = new ArrayList<>(Arrays.asList(labels));	

		subListView.setAdapter(new SubMenuListAdapter(mContext, list));	
		subListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
				@Override
				public void onItemClick(AdapterView<?> adapterview, View p2, final int pos, long p4)
				{
					//Util.finishApp(packageName);
					if(pos>=actions.length)
						return;
					mHandler.post(new Runnable(){

							@Override
							public void run()
							{
								subPopupMenu.dismiss();
								popupWin.dismiss();
								switch(actions[pos]){
									case ACTION_CLOSE:
										removeItem(packageName);
										Util.finishApp(mContext, packageName);
										break;
									case ACTION_HALOFY:
										Util.restartAppAsFloating(mContext, mFloatFlag, packageName);
										break;
									case ACTION_UNHALOFY:
										removeItem(packageName);
										Util.restartAppAsFullScreen(mContext, mFloatFlag, packageName);
										break;
									case ACTION_HALOFY_TOP:
										Util.restartTopAppAsFloating(mContext, mFloatFlag);
										break;
									case ACTION_UNHALOFY_TOP:
										removeItem(packageName);
										Util.restartTopAppAsFullScreen(mContext, mFloatFlag);
										break;
									case ACTION_ADD_TO_FAVORITES:
										addAppToFavs(packageName);
										break;
									case ACTION_REMOVE_FROM_FAVORITES:
										removeAppFromFavs(packageName);
										break;
								}
								
							}
					});
					
//						Util.restartTopAppAsFullScreen(mContext, mFloatFlag);
					
				}
			});
		subPopupMenu.setOnDismissListener(new PopupWindow.OnDismissListener(){

				@Override
				public void onDismiss()
				{
					subMenuVisible = false;
				}
				
			
		});
		subPopupMenu.setContentView(subListView);
		subListView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		int width = subListView.getMeasuredWidth();
		if(width>MINIMAL_WIDTH)
			width = MINIMAL_WIDTH;
		subPopupMenu.setWidth(width);
		//subPopupMenu.setWidth(MeasureSpec.makeMeasureSpec(MINIMAL_WIDTH,MeasureSpec.AT_MOST));
		subPopupMenu.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
		subPopupMenu.setBackgroundDrawable(mContext.getResources().getDrawable( R.drawable.round_rect ));
		subPopupMenu.setOutsideTouchable(true);
		subPopupMenu.setWindowLayoutType(WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
		subPopupMenu.setAnimationStyle(android.R.style.Animation);
		subPopupMenu.showAtLocation(anchor, Gravity.LEFT, x + x_offset, y + y_offset);
		subMenuVisible = true;
	}
	
	class PackageItem implements Comparable<PackageItem>{
		public Drawable packageIcon;
		public String packageName;
		public CharSequence title;
		public int snapGravity;
		public int taskId;
		public boolean isFavorite;
		
		public PackageItem(final PackageManager mPackageManager, final String mPackageName, int mTaskId, int sGravity, boolean isThisFavorite){

			getIconLabel(mPackageManager, mPackageName);
			taskId = mTaskId;
			snapGravity = sGravity;
			packageName = mPackageName;
			isFavorite = isThisFavorite;
		
		}

		public PackageItem(final String mPackageName, final String mTitle, int mTaskId, final Drawable icon, int sGravity, boolean isThisFavorite){
			packageName = mPackageName;
			title = mTitle;
			taskId = mTaskId;
			packageIcon = icon;
			snapGravity = sGravity;
			isFavorite = isThisFavorite;
		}

		public PackageItem(final String mPackageName){
			packageName = mPackageName;
			packageIcon = new ColorDrawable(Color.BLACK);
		}

		void getIconLabel(final PackageManager mPackageManager, final String mPackageName){
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
		}

		@Override
		public int compareTo(PackageItem another) {
			return this.packageName.toString().compareTo(another.packageName.toString());
		}
	}

	class SubMenuListAdapter extends ArrayAdapter<String>{
		Context mContext;
		PopupWindow popupWin;
		public SubMenuListAdapter(final Context sContext, final ArrayList<String> itemsList){
			super(sContext, 0, itemsList);
			mContext=sContext;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final String item = getItem(position);
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.floatdot_submenu_item, parent, false);
			TextView mTitle = (TextView) convertView.findViewById(android.R.id.text1);
			mTitle.setText(item);
			return convertView;
			}
		}
		
	class subMenuItem {
		public int action;
		public String label;
		public subMenuItem(String mLabel, int mAction){
			action = mAction;
			label = mLabel;
		}
	}
	class LauncherListAdapter extends ArrayAdapter<PackageItem>{
		Context mContext;
		PopupWindow popupWin;
		ArrayList<PackageItem> items;
		public LauncherListAdapter(final Context sContext, final ArrayList<PackageItem> itemsList, final PopupWindow mPopupWin){
			super(sContext, 0, itemsList);
			mContext=sContext;
			popupWin=mPopupWin;
			items = itemsList;
		}

		@Override
		public View getView(int pos, View convertView, ViewGroup parent) {
		
			if (convertView == null) //{
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.floatdot_launcher_menuitem, parent, false);
				
				
				final PackageItem item = getItem(pos);
				final ImageView mIcon = (ImageView) convertView.findViewById(android.R.id.icon);
				final TextView mTitle = (TextView) convertView.findViewById(android.R.id.text1);
				final ImageView mPoint = (ImageView) convertView.findViewById(android.R.id.button1);
				
				mIcon.setImageDrawable(item.packageIcon);
				int mColor = item.isFavorite&&item.taskId==0?Color.WHITE:Color.GREEN;
				mPoint.setImageDrawable(Util.makeCircle(mColor, Util.realDp(5, mContext)));
				mTitle.setText(item.title);
				final ViewHolder holder = new ViewHolder();
				
				holder.packageName = item.packageName;
				holder.taskId = item.taskId;
				holder.position = pos;
				holder.title = mTitle;
//				holder.point = mPoint;
//				holder.icon = mIcon;
				convertView.setTag(holder);
			//}
			//else {
				//ViewHolder vHolder = (ViewHolder) convertView.getTag();
		//		ImageView mIcon = (ImageView) convertView.findViewById(android.R.id.icon);
//				TextView mTitle = (TextView) convertView.findViewById(android.R.id.text1);
//				ImageView mPoint = (ImageView) convertView.findViewById(android.R.id.button1);
		//		pos = vHolder.position;
	//			mIcon = vHolder.icon;
				//int mColor = itemsList.get(pos).isFavorite&&itemsList.get(pos).taskId==0?Color.WHITE:Color.GREEN;
				//vHolder.point.setImageDrawable(Util.makeCircle(mColor, Util.realDp(5, mContext)));
				//mTitle.setText(item.title);
			//}
			return convertView;
		}
		
		class ViewHolder{
			ImageView icon;
			TextView title;
			ImageView point;
			int position;
			String packageName;
			int taskId;
		}
	}
	
}


