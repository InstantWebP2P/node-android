package com.iwebpp.node.tests;

import android.util.Log;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.EventEmitter2;

public final class EE2Test extends EventEmitter2 {
	private static final String TAG = "EE2TestTest";

	private boolean testEmit() {
		try {
			on("ok", new EventEmitter.Listener() {
				@Override
				public void invoke(Object data) {
					String ss = (String) data;
					
					// TODO Auto-generated method stub
					if (ss == "ok") 
						Log.d(TAG, "pass@"+ss);
					else 
						Log.d(TAG, "fail@"+ss);
				}
			});
			
			on("no", new EventEmitter.Listener() {
				@Override
				public void invoke(Object data) {
					String ss = (String)data;
					
					// TODO Auto-generated method stub
					if (ss == "no") 
						Log.d(TAG, "pass@"+ss);
					else 
						Log.d(TAG, "fail@"+ss);
				}
			});
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			emit("ok");
			emit("ok", "ok"); 
			emit("ok", "no");
			
			emit("no"); 
			emit("no", "no"); 
			emit("no", "ok");
			
			emit("unknown"); 
			emit("unknown", "ok"); 
			emit("unknown", "no");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		return true;
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				final EE2Test test = new EE2Test();

				test.testEmit();			    
			}
		})).start();

	}
}
