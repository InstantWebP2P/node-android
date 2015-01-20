package com.iwebpp.nodeandroid.test;

import com.iwebpp.node.js.rhino.Host;
import com.iwebpp.nodeandroid.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by Jasm Sison on 1/12/15.
 */
public class ActivityTestCase extends ActivityInstrumentationTestCase {

    private MainActivity activity;

    @Override
    protected void helpSetUp() throws Exception {
        setActivityInitialTouchMode(true);
        activity = getActivity();
    }

    /**
     *
     * Reflection hack to call the MainActivity#runScript method
     *
     * @param js
     */
    @Override
    public void runScript(final String js) throws Exception
    {
        // run as if from a helper thread in the Application
        Class<?> c = MainActivity.class;
        Method method = null;
        try {
            method = c.getDeclaredMethod ("runScript", new Class[] { String.class });
            method.setAccessible(true);
            method.invoke(activity, js);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
