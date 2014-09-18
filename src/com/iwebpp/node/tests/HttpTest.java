package com.iwebpp.node.tests;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Writable.WriteCB;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.Server;
import com.iwebpp.node.http.ServerResponse;

public final class HttpTest {
	private static final String TAG = "HttpTest";
	private NodeContext ctx;

	private boolean testListening() {
		Server srv;
		final int port = 6188;
		try {
			srv = new Server(ctx);

			srv.listen(port, "0.0.0.0", 4, 10, new Server.ListeningCallback() {
				
				@Override
				public void onListening() throws Exception {
                    Log.d(TAG, "http server listening on "+port);					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}
	
	private boolean testConnection() {
		Server srv;
		final int port = 6288;
		try {
			srv = new Server(ctx, new Server.requestListener(){

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
							Log.d(TAG, "http res.write done");							
						}

					});

					res.end(null, null, null);
				}

			});

			srv.listen(port, "0.0.0.0", 4, 10, new Server.ListeningCallback() {

				@Override
				public void onListening() throws Exception {
					Log.d(TAG, "http server listening on "+port);					
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}
	
	public HttpTest(){
		this.ctx = new NodeContext(); 
	}

	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				///testListening();
				testConnection();
				
				// run loop
				try {
					ctx.getLoop().run();
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		})).start();
	}
	
}
