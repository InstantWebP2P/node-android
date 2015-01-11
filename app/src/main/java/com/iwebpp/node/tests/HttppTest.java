package com.iwebpp.node.tests;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.NodeContext.TimeoutListener;
import com.iwebpp.node.http.ClientRequest;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.HttppServer;
import com.iwebpp.node.http.HttpServer;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ReqOptions;
import com.iwebpp.node.http.ServerResponse;
import com.iwebpp.node.http.httpp;
import com.iwebpp.node.net.AbstractServer.ListeningCallback;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.stream.Writable.WriteCB;

public final class HttppTest {
	private static final String TAG = "HttppTest";
	private NodeContext ctx;

	private boolean testListening() {
		HttppServer srv;
		final int port = 6188;
		try {
			srv = new HttppServer(ctx);

			srv.listen(port, "0.0.0.0", 10, new HttppServer.ListeningCallback() {
				
				@Override
				public void onListening() throws Exception {
                    Log.d(TAG, "httpp server listening on "+port);					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}
	
	private boolean testConnection() {
		HttppServer srv;
		final int port = 6288;
		try {
			srv = new HttppServer(ctx, new HttpServer.requestListener(){

				@Override
				public void onRequest(IncomingMessage req, ServerResponse res)
						throws Exception {
					Log.d(TAG, "got reqeust, headers: "+req.headers());

					Map<String, List<String>> headers = new Hashtable<String, List<String>>();
					headers.put("content-type", new ArrayList<String>());
					headers.get("content-type").add("text/plain");
					///headers.put("te", new LinkedList<String>());
					///headers.get("te").add("chunk");
					
					res.writeHead(200, headers);
					///for (int i = 0; i < 10; i ++)
						res.write("Hello Tom", "utf-8", new WriteCB(){

						@Override
						public void writeDone(String error) throws Exception {
							Log.d(TAG, "httpp res.write done");							
						}

					});

					res.end(null, null, null);
				}

			});
			
			srv.onClientError(new HttpServer.clientErrorListener(){

				@Override
				public void onClientError(String exception, AbstractSocket socket) throws Exception {
					// TODO Auto-generated method stub
					Log.e(TAG, "client error: "+exception + "@"+socket);
				}
				
			});

			srv.listen(port, "0.0.0.0", 10, new HttppServer.ListeningCallback() {

				@Override
				public void onListening() throws Exception {
					Log.d(TAG, "httpp server listening on "+port);					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}

	private boolean testConnect() {
		final String host = "192.188.1.100";
		final int port = 51680;

		try {

			// client
			ReqOptions ropt = new ReqOptions();
			ropt.hostname = host;
			ropt.port = port;
			ropt.method = "PUT";
			ropt.path = "/";
			///ropt.keepAlive = true;
			///ropt.keepAliveMsecs = 10000;

			ClientRequest req = httpp.request(ctx, ropt, new ClientRequest.responseListener() {

				@Override
				public void onResponse(IncomingMessage res) throws Exception {
					Log.d(TAG, "STATUS: " + res.statusCode());
					Log.d(TAG, "HEADERS: " + res.getHeaders());

					res.setEncoding("utf-8");
					res.on("data", new Listener(){

						@Override
						public void onEvent(Object chunk) throws Exception {
							Log.d(TAG, "BODY: " + chunk);

						}

					});
				}

			});
			
			req.on("error", new Listener(){

				@Override
				public void onEvent(Object e) throws Exception {
					Log.d(TAG, "problem with request: " + e);					
				}

			});

			// write data to request body
			for (int i = 0; i < 8; i ++)
			    req.write("data"+i+"\n", "utf-8", null);
			
			req.end(null, null, null);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}
	
	private boolean testConnectPair() {
		final int port = 6688;

		try {
			final HttppServer srv = httpp.createServer(ctx, new requestListener(){

				@Override
				public void onRequest(IncomingMessage req, ServerResponse res)
						throws Exception {
					Log.d(TAG, "got reqeust, headers: "+req.headers());

					Map<String, List<String>> headers = new Hashtable<String, List<String>>();
					headers.put("content-type", new ArrayList<String>());
					headers.get("content-type").add("text/plain");
					///headers.put("te", new ArrayList<String>());
					///headers.get("te").add("chunk");

					res.writeHead(200, headers);
					res.write("Hello Tom", "utf-8", new WriteCB(){

						@Override
						public void writeDone(String error) throws Exception {
							Log.d(TAG, "httpp res.write done");							
						}

					});

					res.end(null, null, null);
					
				}

			});

			srv.listen(port, "0.0.0.0", 1, new ListeningCallback() {

				@Override
				public void onListening() throws Exception {
					Log.d(TAG, "httpp server listening on "+port);		
				}
				
			});
			
			// client
			final ReqOptions ropt = new ReqOptions();
			ropt.hostname = "localhost"; // IP address instead localhost
			ropt.port = port;
			ropt.method = "GET";
			ropt.path = "/";

			// defer 2s to connect
			ctx.setTimeout(new TimeoutListener(){

				@Override
				public void onTimeout() throws Exception {
					
					ClientRequest req = httpp.request(ctx, ropt, new ClientRequest.responseListener() {

						@Override
						public void onResponse(IncomingMessage res) throws Exception {
							Log.d(TAG, "STATUS: " + res.statusCode());
							Log.d(TAG, "HEADERS: " + res.getHeaders());

							res.setEncoding("utf-8");

							res.on("data", new Listener(){

								@Override
								public void onEvent(Object chunk) throws Exception {
									Log.d(TAG, "BODY: " + chunk);

								}

							});
						}

					});

					req.on("error", new Listener(){

						@Override
						public void onEvent(Object e) throws Exception {
							Log.d(TAG, "problem with request: " + e);					
						}

					});

					// write data to request body
					///req.write("data\n", "utf-8", null);
					///req.write("data\n", "utf-8", null);
					req.end(null, null, null);
				}

			}, 2000);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}
	
	public HttppTest(){
		this.ctx = new NodeContext(); 
	}

	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				testListening();
				testConnection();
				///testConnect();
				testConnectPair();
				
				// run loop
				try {
					ctx.getLoop().run();
					
					Log.d(TAG, "exit test");
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		})).start();
	}
	
}
