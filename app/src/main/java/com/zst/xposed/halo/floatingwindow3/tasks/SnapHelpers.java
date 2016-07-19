package com.zst.xposed.halo.floatingwindow3.tasks;
import android.view.*;
import com.zst.xposed.halo.floatingwindow3.*;
import android.graphics.*;
import android.content.*;

public class SnapHelpers
{
	public static final int mRange = 100;
	
	public static boolean checkAndShowSnap(int x, int y, int screenW, int screenH, final Context mContext) {
		int snapGravity = getSnapGravityByHotspot(x, y, screenW, screenH);
		if(snapGravity == 0) {
			broadcastHideSnap(mContext);
			return false;
		}
		broadcastShowSnap(mContext, getSnapLayout(snapGravity, screenW, screenH));
		return true;
	}
	
	public static WindowHolder checkAndGetFinalSnap(int x, int y, int screenW, int screenH, final Context mContext) {
		int snapGravity = getSnapGravityByHotspot(x, y, screenW, screenH);
		broadcastHideSnap(mContext);
		WindowHolder mWindowHolder = getSnapLayout(snapGravity, screenW, screenH);
		mWindowHolder.isSnapped = true;
		return mWindowHolder;
	}
	
	public static int getSnapGravityByHotspot(int x, int y, int screenW, int screenH) {
		int newSnapGravity = 0;
		if(x < mRange) newSnapGravity = newSnapGravity | Gravity.LEFT;
		if(x > (screenW - mRange)) newSnapGravity = newSnapGravity | Gravity.RIGHT;
		if(y < mRange) newSnapGravity = newSnapGravity | Gravity.TOP;
		if(y > (screenH - mRange)) newSnapGravity = newSnapGravity | Gravity.BOTTOM;
		return newSnapGravity;
	}
	
	public static WindowHolder getSnapLayout(int snapGravity, final Context mContext) {
		Point screenSize = Util.getScreenSize(mContext);
		return getSnapLayout(snapGravity, screenSize.x, screenSize.y);
	}
	
	public static WindowHolder getSnapLayout(int snapGravity, int screenW, int screenH) {
		int x = 0;
		int y = 0;
		int w = WindowManager.LayoutParams.MATCH_PARENT;
		int h = WindowManager.LayoutParams.MATCH_PARENT;
		
		if (Util.isFlag(snapGravity, Gravity.RIGHT)) {
			x = InterActivity.FloatDotCoordinates[0];
			w = screenW - InterActivity.FloatDotCoordinates[0];
		} else if (Util.isFlag(snapGravity, Gravity.LEFT)) {
			w = InterActivity.FloatDotCoordinates[0];
		}
		if (Util.isFlag(snapGravity, Gravity.BOTTOM)) {
			y = InterActivity.FloatDotCoordinates[1];
			h = screenH - InterActivity.FloatDotCoordinates[1];
		} else if (Util.isFlag(snapGravity, Gravity.TOP)) {
			h = InterActivity.FloatDotCoordinates[1];
		}
		return new WindowHolder(null, x, y, w, h);
	}
	
	public static void broadcastShowSnap(final Context ctx, final WindowHolder mWindowHolder) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		int[] array = { mWindowHolder.x, mWindowHolder.y, mWindowHolder.width, mWindowHolder.height };
		i.putExtra(Common.INTENT_APP_PARAMS, array);
		ctx.sendBroadcast(i);
	}
	
	public static void broadcastShowLegacy(final Context ctx, int w, int h, int g) {
		Intent i = new Intent(Common.SHOW_OUTLINE);
		int[] array = { w, h, g };
		i.putExtra(Common.INTENT_APP_SNAP_ARR, array);
		ctx.sendBroadcast(i);
	}

	public static void broadcastHideSnap(final Context ctx) {
		ctx.sendBroadcast(new Intent(Common.SHOW_OUTLINE));
	}
}
