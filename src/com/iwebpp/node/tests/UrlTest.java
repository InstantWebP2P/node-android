package com.iwebpp.node.tests;

import com.iwebpp.node.Url;

import android.util.Log;


public final class UrlTest {
	private static final String TAG = "UrlTest";

	private boolean testParse() throws Exception {		
		// parse without query
		String link0 = "http://user:pass@host.com:8080/p/a/t/h?query=string#hash";

		Url.UrlObj obj0 = Url.parse(link0, false, true);
		Log.d(TAG, "link:"+link0+", parsed:"+obj0);
		if (!link0.equalsIgnoreCase(obj0.toString()))
			return false;

		// parse with query
		String link1 = "https://user:pass@host.com:8888/p/a/t/h?query=string&i=love&love=u#hash";

		Url.UrlObj obj1 = Url.parse(link1, true, true);
		Log.d(TAG, "link:"+link1+", parsed:"+obj1);
		if (!link1.equalsIgnoreCase(obj1.toString()))
			return false;
		Log.d(TAG, "queryParams:"+obj1.queryParams);
				
		return true;
	}

	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				try {
					testParse();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			    
			}
		})).start();

	}
}
