package com.zst.xposed.halo.floatingwindow3.floatdot;

interface XHFWInterface {
	// Window management
	void bringToFront(int taskId);
	void toggleDragger(boolean show);
	void removeAppTask(int taskId, int flags);
	int getLastTaskId();
	int[] getCurrentFloatdotCoordinates();
}
