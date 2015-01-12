package com.iwebpp.node.api.tests;

import java.util.Map;

import android.util.Log;

import com.iwebpp.node.api.AsyncHttpClient;
import com.iwebpp.node.api.AsyncHttpClient.HttpClientCallback;
import com.iwebpp.node.api.AsyncHttpClient.http_content_b;
import com.iwebpp.node.api.SimpleApi;
import com.iwebpp.node.http.HttpServer;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;
import com.iwebpp.node.http.http;
import com.iwebpp.node.net.AbstractServer;

public final class SimpleApiTest {

	private static final String TAG = "SimpleApiTest";

	private boolean TestHttpClient() {

		// http get
		try {
			// iwebpp
			final String iwebpp = "http://iwebpp.com/";

			AsyncHttpClient.get(iwebpp, new HttpClientCallback(){

				@Override
				public void onResponse(String error, int statusCode,
						Map<String, String> headers, http_content_b content)
								throws Exception {
					if (error!=null) Log.d(TAG, error+", http get failed on "+iwebpp);
					Log.d(TAG, "UI get response, headers:"+headers + ",statusCode:"+statusCode + ",content:"+content.getContent());
				}

			});

			// taobao
			final String taobao = "http://www.taobao.com/";

			AsyncHttpClient.get(taobao, new HttpClientCallback(){

				@Override
				public void onResponse(String error, int statusCode,
						Map<String, String> headers, http_content_b content)
								throws Exception {
					if (error!=null) Log.d(TAG, error+", http get failed on "+iwebpp);
					Log.d(TAG, "UI get response, headers:"+headers + ",statusCode:"+statusCode + ",content:"+content.getContent());
				}

			});
			
			// local server demo :6188
			final String demo = "http://localhost:6188/";

			AsyncHttpClient.get(demo, new HttpClientCallback(){

				@Override
				public void onResponse(String error, int statusCode,
						Map<String, String> headers, http_content_b content)
								throws Exception {
					if (error!=null) Log.d(TAG, error+", http get failed on "+demo);
					Log.d(TAG, "UI get response, headers:"+headers + ",statusCode:"+statusCode + ",content:"+content.getContent());
				}

			});
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}
	
	private boolean TestHttpServer() {
		try {
			new HttpServerDemo(6188).execute();
		} catch (Exception e) {
			e.printStackTrace();
		} 
		
		return true;
	}
	private class HttpServerDemo extends SimpleApi {

		private int port;

		public HttpServerDemo(int port) {
			this.port = port;
		}

		@Override
		public void content() throws Exception {
			HttpServer srv = http.createServer(getNodeContext(), new requestListener() {

				@Override
				public void onRequest(IncomingMessage req, ServerResponse res)
						throws Exception {
					res.setHeader("Content-Type", "text/html; charset=utf-8");
					res.setHeader("Content-Encoding", "deflate");
					
					res.write(
							"<html>" +
							"" +
							"<head>" +
							"" +
							"</head>" +
							"" +
							"<body>" +
							"  Helloword ...." +
							"</body>" +
							"" +
							"</html>", "utf-8", null);
					res.end();
				}

			});

			srv.listen(port, new AbstractServer.ListeningCallback(){

				@Override
				public void onListening() throws Exception {
					Log.d(TAG, "listening on "+port);
				}

			});
		}

	}
	
	public void start() {
		TestHttpServer();
		TestHttpClient();
	}

}
