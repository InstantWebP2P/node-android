// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.tests;

import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.EventEmitter2;

import junit.framework.TestCase;

public final class EE2Test extends TestCase {
    private static final String TAG = "EE2TestTest";

    private EventEmitter2 ee2 = new EventEmitter2();

    /**
     *
     * TODO figure out how to test this
     *
     * @throws Exception
     */
    @LargeTest
    public void testEmit() throws Exception {
        ee2.on("ok", new EventEmitter.Listener() {
            @Override
            public void onEvent(Object data) {
                String ss = (String) data;

                if (ss == "ok")
                    Log.d(TAG, "pass@" + ss);
                else {
                    Log.d(TAG, "fail@" + ss);
                }

                assertSame("ok", ss);
            }
        });

        ee2.on("no", new EventEmitter.Listener() {
            @Override
            public void onEvent(Object data) {
                String ss = (String) data;

                if (ss == "no")
                    Log.d(TAG, "pass@" + ss);
                else
                    Log.d(TAG, "fail@" + ss);

                assertSame("no", ss);
            }
        });

        assertTrue(ee2.emit("ok"));
        assertTrue(ee2.emit("ok", "ok"));
        assertTrue(ee2.emit("ok", "no"));

        assertTrue(ee2.emit("no"));
        assertTrue(ee2.emit("no", "no"));
        assertTrue(ee2.emit("no", "ok"));

        assertTrue(ee2.emit("unknown"));
        assertTrue(ee2.emit("unknown", "ok"));
        assertTrue(ee2.emit("unknown", "no"));

        fail(); // FIXME these tests are not correct
    }
}
