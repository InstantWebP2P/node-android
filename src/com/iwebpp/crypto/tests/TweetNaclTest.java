package com.iwebpp.crypto.tests;

import android.util.Log;

public final class TweetNaclTest {
	private static final String TAG = "TweetNaclTest";

	private boolean testEmit() {
		return true;
	}

	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				testEmit();			    
			}
		})).start();

	}


}
