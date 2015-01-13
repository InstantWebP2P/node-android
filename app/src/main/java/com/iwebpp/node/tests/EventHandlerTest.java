package com.iwebpp.node.tests;

import android.util.Log;

import com.iwebpp.EventHandler;
import com.iwebpp.node.EventEmitter;

import junit.framework.TestCase;

public final class EventHandlerTest extends TestCase {
	private static final String TAG = "EventHandlerTest";

    private EventHandler eh = new EventHandler();

    public void testEmit() {
		try {
			eh.on("ok", new EventEmitter.Listener() {
				@Override
				public void onEvent(Object data) {
					String ss = (String) data;
					
					if (ss == "ok") 
						Log.d(TAG, "pass@"+ss);
					else 
						Log.d(TAG, "fail@"+ss);
				}
			});

            eh.on("no", new EventEmitter.Listener() {
				@Override
				public void onEvent(Object data) {
					String ss = (String)data;
					
					if (ss == "no") 
						Log.d(TAG, "pass@"+ss);
					else 
						Log.d(TAG, "fail@"+ss);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
            eh.emit("ok");
            eh.emit("ok", "ok");
            eh.emit("ok", "no");

            eh.emit("no");
            eh.emit("no", "no");
            eh.emit("no", "ok");

            eh.emit("unknown");
            eh.emit("unknown", "ok");
            eh.emit("unknown", "no");
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
}
