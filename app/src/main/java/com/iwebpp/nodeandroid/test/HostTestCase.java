package com.iwebpp.nodeandroid.test;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.SmallTest;

import com.iwebpp.node.js.rhino.Host;
import com.iwebpp.nodeandroid.MainActivity;

/**
 * Created by Jasm Sison on 1/12/15.
 */
public class HostTestCase extends ActivityInstrumentationTestCase {

    private MainActivity activity;

    @Override
    protected void helpSetUp() throws Exception {
        setActivityInitialTouchMode(true);
        activity = getActivity();
    }

    /**
     *
     * Run from the Rhino context directly
     *
     * @param js
     */
    @Override
    public void runScript(final String js) throws Exception
    {

        // run from the test thread
        Host host = new Host() {
            @Override
            public String content() {
                return js;
            }
        };
        host.execute();
    }

}
