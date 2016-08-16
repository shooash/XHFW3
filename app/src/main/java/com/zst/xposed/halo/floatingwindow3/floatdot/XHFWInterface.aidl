package com.zst.xposed.halo.floatingwindow3.floatdot;

interface XHFWInterface {
	// Window management
	boolean bringToFront(int taskId);
	void toggleDragger(boolean show);
	void removeAppTask(int taskId, int flags);
	int getLastTaskId();
	int[] getCurrentFloatdotCoordinates();
	void unfocusApp(int task);
}
