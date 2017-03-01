package com.zst.xposed.halo.floatingwindow3.floatdot;

interface XHFWInterface {
	// Window management
	boolean bringToFront(int taskId, int status);
	void toggleDragger(boolean show);
	void updateStatus(int taskId, int status);
	void removeAppTask(int taskId, int flags);
	int getLastTaskId();
	int[] getCurrentFloatdotCoordinates();
	void unfocusApp(int task);
	boolean focusApp(int taskId, int status);
}
