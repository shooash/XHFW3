package com.zst.xposed.halo.floatingwindow3;

import static de.robv.android.xposed.XposedHelpers.findClass;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import android.graphics.*;

public class SystemUIOutliner {
	
	private static Context mContext;
	private static View mOutline;
	private static View mOutlineFocus;
	private static WindowManager mWm;
	
	static final int HIDE = -10000;
	
	public static void handleLoadPackage(LoadPackageParam lpp) throws Throwable {
		if (!lpp.packageName.equals("com.android.systemui")) return;
		
		try {
			focusChangeContextFinder(lpp);
		} catch (Throwable e) {
			XposedBridge.log(Common.LOG_TAG + "Movable / SystemUIOutliner");
			XposedBridge.log(e);
		}
	}
	
	private static void focusChangeContextFinder(LoadPackageParam l) throws Throwable {
		Class<?> hookClass = null;
		try{
			hookClass = findClass("com.android.systemui.SystemUIService", l.classLoader);
			} catch(Throwable t){
				XposedBridge.log(t);
				return;
			}
		if(hookClass==null) return;
		XposedBridge.hookAllMethods(hookClass, "onCreate", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Service thiz = (Service) param.thisObject;
				mContext = thiz.getApplicationContext();
				mContext.registerReceiver(mIntentReceiver, new IntentFilter(Common.SHOW_OUTLINE));
			}
		});
	}
	
	final static BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context ctx, Intent intent) {
			boolean isFocusOnly = intent.getBooleanExtra(Common.INTENT_APP_FOCUS, false);
			int[] array = intent.getIntArrayExtra(Common.INTENT_APP_PARAMS);
			int[] array2 = intent.getIntArrayExtra(Common.INTENT_APP_SNAP_ARR);
			if (array != null) {
				if(isFocusOnly) 
					refreshOutlineViewFocus(ctx,array[0], array[1], array[2], array[3]);
				else
					refreshOutlineView(ctx, array[0], array[1], array[2], array[3]);
			} else if (array2 != null) {
				refreshOutlineView(ctx, array2[0], array2[1], array2[2]);
			} else {
				refreshOutlineView(ctx, HIDE, HIDE, HIDE, HIDE);
				refreshOutlineViewFocus(ctx, HIDE, HIDE, HIDE, HIDE);
			}
		}
	};
	
	// Create a view in SystemUI window manager
	private static void createOutlineView(Context ctx) {
		if (mWm == null) {
			mWm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
		}
		WindowManager.LayoutParams layOutParams = new WindowManager.LayoutParams(
			WindowManager.LayoutParams.TYPE_PHONE,
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
			WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM |
			WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
			WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
			PixelFormat.TRANSLUCENT);
		layOutParams.gravity = Gravity.TOP | Gravity.LEFT;
		String outlineColor = MainXposed.mPref.getString(Common.KEY_AERO_FOCUS_COLOR, Common.DEFAULT_AERO_FOCUS_COLOR);
		Util.addPrivateFlagNoMoveAnimationToLayoutParam(layOutParams);
		mOutline = getOutlineView(ctx, Color.parseColor("#" + outlineColor));
		mOutline.setFocusable(false);
		mOutline.setClickable(false);
		mOutline.setVisibility(View.GONE);
		
		mOutlineFocus = getOutlineViewFocus(ctx, Color.parseColor("#" + outlineColor));
		mOutlineFocus.setFocusable(false);
		mOutlineFocus.setClickable(false);
		mOutlineFocus.setVisibility(View.GONE);
		
		mWm.addView(mOutline, layOutParams);
		mWm.addView(mOutlineFocus,layOutParams);
	}
	
	// show the outline with positioning (x,y)
	private static void refreshOutlineView(Context ctx, int x, int y, int height, int width) {
		if (mOutline == null) {
			createOutlineView(ctx);
		}
		if (x == HIDE || y == HIDE || height == HIDE || width == HIDE) {
			mOutline.setVisibility(View.GONE);
			return;
		}
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) mOutline.getLayoutParams();
		param.x = x;
		param.y = y;
		param.height = height;
		param.width = width;		
		param.gravity = Gravity.TOP | Gravity.LEFT;
		mWm.updateViewLayout(mOutline, param);
		mOutline.setVisibility(View.VISIBLE);
	}
	
	// show the outline with positioning (x,y)
	private static void refreshOutlineViewFocus(Context ctx, int x, int y, int height, int width) {
		if (mOutlineFocus == null) {
			createOutlineView(ctx);
		}
		if (x == HIDE || y == HIDE || height == HIDE || width == HIDE) {
			mOutlineFocus.setVisibility(View.GONE);
			return;
		}
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) mOutlineFocus.getLayoutParams();
		param.x = x;
		param.y = y;
		param.height = height;
		param.width = width;		
		param.gravity = Gravity.TOP | Gravity.LEFT;
		mWm.updateViewLayout(mOutlineFocus, param);
		mOutlineFocus.setVisibility(View.VISIBLE);
	}
	
	// show the outline with gravity
	private static void refreshOutlineView(Context ctx, int w, int h, int g) {
		if (mOutline == null) {
			createOutlineView(ctx);
		}
		if (h == HIDE || w == HIDE || g == HIDE) {
			mOutline.setVisibility(View.GONE);
			return;
		}
		WindowManager.LayoutParams param = (WindowManager.LayoutParams) mOutline.getLayoutParams();
		param.x = 0;
		param.y = 0;
		param.height = h;
		param.width = w;		
		param.gravity = g;
		mWm.updateViewLayout(mOutline, param);
		mOutline.setVisibility(View.VISIBLE);
	}
	
	// create outline view with translucent filling
	private static View getOutlineView(Context ctx, int color) {
		FrameLayout outline = new FrameLayout(ctx);
		Util.setBackgroundDrawable(outline, Util.makeOutline(color, Util.realDp(4, ctx)));
		
		View filling = new View(ctx);
		filling.setBackgroundColor(color);
		filling.setAlpha(0.5f);
		outline.addView(filling);
		
		return outline;
	}
	
	// create outline with no filling
	private static View getOutlineViewFocus(Context ctx, int color) {
		FrameLayout outline = new FrameLayout(ctx);
		Util.setBackgroundDrawable(outline, Util.makeOutline(color, Util.realDp(4, ctx)));

		return outline;
	}
}
