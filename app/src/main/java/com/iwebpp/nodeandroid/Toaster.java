package com.iwebpp.nodeandroid;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Scriptable;


/**
 * Created by Jasm Sison on 1/13/15.
 *
 * [A Toaster singleton pattern](http://www.sirentuan.com) and
 * [native global function example](http://stackoverflow.com/questions/13033080/how-are-native-functions-created-in-rhino),
 *
 * combined together
 *
 * results in a Singleton global toast/alert function in a Rhino context.
 */
public class Toaster extends BaseFunction {
    private static Toaster mInstance = null;
    private Context mContext;
    private Toast currentToast;

    private Toaster(Context context) {
        this.mContext = context;
    }

    public Object call(org.mozilla.javascript.Context cx, Scriptable scope, Scriptable thisObj, final Object[] args) {
        Activity activity = (Activity) mContext;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                toast(((org.mozilla.javascript.ConsString) args[0]).toString());
            }
        });
        return null;
    }

    public int getArity()
    {
        return 1;
    }

    public static void init(Context context) {
        mInstance = new Toaster(context);
    }

    public static Toaster getInstance()
    {
        return mInstance;
    }

    public static void toast(final String message){
        if (mInstance.currentToast != null){
            mInstance.currentToast.cancel();
        }
        mInstance.currentToast = Toast.makeText(mInstance.mContext, message, Toast.LENGTH_SHORT);
        mInstance.currentToast.show();
    }
}
