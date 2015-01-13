// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.tests;

import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.EventEmitter2;

import junit.framework.TestCase;

public final class EE2Test extends TestCase  {
	private static final String TAG = "EE2TestTest";

    private EventEmitter2 ee2 = new EventEmitter2();

    @LargeTest
	public void testEmit() {
		try {
			ee2.on("ok", new EventEmitter.Listener() {
                @Override
                public void onEvent(Object data) {
                    String ss = (String) data;

                    if (ss == "ok")
                        Log.d(TAG, "pass@" + ss);
                    else {
                        Log.d(TAG, "fail@" + ss);
                    }
                }
            });

            ee2.on("no", new EventEmitter.Listener() {
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
			e.printStackTrace(); // FIXME, this prevents the test from breaking
		}
		
		try {
            ee2.emit("ok");
            ee2.emit("ok", "ok");
            ee2.emit("ok", "no");

            ee2.emit("no");
            ee2.emit("no", "no");
            ee2.emit("no", "ok");

            ee2.emit("unknown");
            ee2.emit("unknown", "ok");
            ee2.emit("unknown", "no");
		} catch (Exception e) {
			e.printStackTrace(); // FIXME, this also gives a false sense of security
		} 
	}
}
