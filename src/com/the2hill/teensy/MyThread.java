package com.the2hill.teensy;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

public class MyThread extends Thread {
	public Handler myThreadHandler;
	Activity mainActivity;
	Handler mainHandler;
	int what = -1;

	public MyThread(TeensyRemoteActivity anActivity) {
		this.mainActivity = anActivity;
		this.mainHandler = anActivity.handler;
	}

	@Override
	public void run() {
		Looper.prepare();
		myThreadHandler = new Handler() {
			public void handleMessage(Message msg) {
				what = msg.what;

				// Writing back to main UI thread
				mainActivity.runOnUiThread(new Runnable() {

					public void run() {
						Toast.makeText(
								mainActivity,
								"This is MyThread. What="
										+ Integer.toString(what),
								Toast.LENGTH_SHORT).show();

					}
				});

				Message m = Message.obtain();
				m.what = 10;
				mainHandler.obtainMessage(12).sendToTarget();
			}
		};
		Looper.loop();
	}
}
