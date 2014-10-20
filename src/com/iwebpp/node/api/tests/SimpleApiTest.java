package com.iwebpp.node.api.tests;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.libuvpp.handles.TimerHandle;
import com.iwebpp.node.Url;
import com.iwebpp.node.NodeContext.TimeoutListener;
import com.iwebpp.node.Url.UrlObj;
import com.iwebpp.node.api.SimpleApi;
import com.iwebpp.node.http.ClientRequest;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ReqOptions;
import com.iwebpp.node.http.http;

public final class SimpleApiTest {

	private static final String TAG = "SimpleApiTest";

	private class HttpGet extends SimpleApi {
		private Map<String, String> headers;

		private String body;

		private String url;

		private HttpGetCallback cb;

		private int timeout;

		private TimerHandle tmohdl;

		public HttpGet(String url, final HttpGetCallback cb, int timeout) {
			this.headers = new Hashtable<String, String>();
			this.body = "";
			this.url = url;
			this.cb = cb;
			this.timeout = timeout;
			
			// call on UI thread 
		    this.once("completed", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
                    cb.onResponse(null, headers, body);					
				}
		    	
		    });	
		    this.once("timeout", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
                    cb.onResponse("Operation timeout", null, null);					
				}
		    	
		    });	
		    this.once("error", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
                    cb.onResponse("Operation error:"+data!=null? data.toString() : "", null, null);					
				}
		    	
		    });	
		}

		@Override
		public void content() throws Exception {
			final HttpGet self = this;
			
			// check timeout
			if (timeout > 0) 
				this.tmohdl = getNodeContext().setTimeout(new TimeoutListener(){

					@Override
					public void onTimeout() throws Exception {
						///cb.onResponse("Operation timeout", null, null);
						self.emit("timeout");
					}
					
				}, timeout);
			
			http.get(getNodeContext(), url, new ClientRequest.responseListener(){

				@Override
				public void onResponse(IncomingMessage res) throws Exception {
					Log.d(TAG, "got response: "+res.statusCode());

					// headers
					for (String k : res.headers().keySet())
						headers.put(k, res.headers().get(k).get(0));
					
					// body
					res.setEncoding("utf-8");
					res.on("data", new Listener(){

						@Override
						public void onEvent(Object data) throws Exception {
							Log.d(TAG, "response got data:\n"+data);

							body += data.toString();
						}
						
					});
					res.on("end", new Listener(){

						@Override
						public void onEvent(Object data) throws Exception {
							Log.d(TAG, "response got end\n");
							
							// clear timeout
							if (timeout>0 && tmohdl!=null)
								tmohdl.close();
							
                            if (data!=null) body += data.toString();
                            
                            // MUST call from UI thread
                            ///cb.onResponse(null, headers, body);
                            self.emit("completed");
						}
						
					});
				}

			});
		}

	}
	private interface HttpGetCallback {
		public void onResponse(String error, Map<String, String> headers, String body);
	}

	private boolean TestHttpClient() {

		// http get
		try {
			// iwebpp
			final String iwebpp = "http://iwebpp.com/";

			new HttpGet(iwebpp, new HttpGetCallback(){

				@Override
				public void onResponse(String error, Map<String, String> headers,
						String body) {
					if (error!=null) Log.d(TAG, error+", http get failed on "+iwebpp);
					Log.d(TAG, "UI get response, headers:"+headers+",body:"+body);
				}

			}, 2000).execute();
			
			// taobao
			final String taobao = "http://www.taobao.com/";

			new HttpGet(taobao, new HttpGetCallback(){

				@Override
				public void onResponse(String error, Map<String, String> headers,
						String body) {
					if (error!=null) Log.d(TAG, error+", http get failed on "+taobao);
					Log.d(TAG, "UI get response, headers:"+headers+",body:"+body);
				}
				
			}, 2000).execute();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	public void start() {
		TestHttpClient();
	}

}
