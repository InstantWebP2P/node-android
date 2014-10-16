package com.iwebpp.middleware.test;

import android.util.Log;

import com.iwebpp.middleware.Connect;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.TimeoutListener;
import com.iwebpp.node.http.ClientRequest.responseListener;
import com.iwebpp.node.http.HttpServer;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.HttppServer;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;
import com.iwebpp.node.http.http;
import com.iwebpp.node.http.httpp;

public final class ConnectTest {
	private static final String TAG = "ConnectTest";
	
	private NodeContext ctx;

	private boolean testStack() throws Exception {
		Connect stack = new Connect();

		// append timestamp header
		stack.use(new requestListener(){

			@Override
			public void onRequest(IncomingMessage req, ServerResponse res)
					throws Exception {
				res.setHeader("timestamp", ""+System.currentTimeMillis());				
			}

		});

		// set status code
		stack.use(new requestListener(){

			@Override
			public void onRequest(IncomingMessage req, ServerResponse res)
					throws Exception {
				res.writeHead(200);
				res.end(null, null, null);
			}

		});

		// set un-reachable header
		stack.use(new requestListener(){

			@Override
			public void onRequest(IncomingMessage req, ServerResponse res)
					throws Exception {
				res.setHeader("unreachable", "yes");
			}

		});
		
		// route on path
		stack.use("/route", new requestListener(){

			@Override
			public void onRequest(IncomingMessage req, ServerResponse res)
					throws Exception {
				res.setHeader("route", req.url());
				res.end("route", "utf-8", null);
			}
			
		});

		HttpServer srv = http.createServer(ctx, stack);
		HttppServer srvpp = httpp.createServer(ctx, stack);

		int port = 5188;
		srv.listen(port, "0.0.0.0"); Log.d(TAG, "http server listen on "+port);
		srvpp.listen(port, "0.0.0.0"); Log.d(TAG, "httpp server listen on "+port);

		// request on /, /route
		ctx.setTimeout(new TimeoutListener(){

			@Override
			public void onTimeout() throws Exception {
				http.get(ctx, "http://localhost:5188/", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got http response on " + res.getReq().getPath()  +", headers:"+res.headers());					
					}

				});
				httpp.get(ctx, "http://localhost:5188/", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got httpp response on " + res.getReq().getPath()  +", headers:"+res.headers());				
					}

				});
				
				http.get(ctx, "http://localhost:5188/route", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got http response on " + res.getReq().getPath()  +", headers:"+res.headers());				
					}

				});
				httpp.get(ctx, "http://localhost:5188/route", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got httpp response on " + res.getReq().getPath()  +", headers:"+res.headers());			
					}

				});
			}
			
		}, 2000);
		
		return true;
	}

	private boolean testNest() throws Exception {
		final Connect stack = new Connect();
		final Connect stack1 = new Connect();

		// append timestamp header
		stack.use(new requestListener(){

			@Override
			public void onRequest(IncomingMessage req, ServerResponse res)
					throws Exception {
				res.setHeader("timestamp1", ""+System.currentTimeMillis());	
				res.setHeader("ppath", stack.getParent());
				res.end("flat", "utf-8", null);
			}

		});

		// append timestamp header
		stack1.use("/nest", new requestListener(){

			@Override
			public void onRequest(IncomingMessage req, ServerResponse res)
					throws Exception {
				res.setHeader("timestamp2", ""+System.currentTimeMillis());		
				res.setHeader("ppath", stack1.getParent());
				res.end("nest", "utf-8", null);
			}

		});
		stack1.use("/nest", stack1);
		
		stack.use(stack1);
		stack.use("/nest", stack1);
		
		HttpServer srv = http.createServer(ctx, stack);
		HttppServer srvpp = httpp.createServer(ctx, stack);

		int port = 5189;
		srv.listen(port, "0.0.0.0"); Log.d(TAG, "http server listen on "+port);
		srvpp.listen(port, "0.0.0.0"); Log.d(TAG, "httpp server listen on "+port);

		// request on /, /route
		ctx.setTimeout(new TimeoutListener(){

			@Override
			public void onTimeout() throws Exception {
				http.get(ctx, "http://localhost:5189/", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got http response on " + res.getReq().getPath() +", headers:"+res.headers());			
					}

				});
				httpp.get(ctx, "http://localhost:5189/", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got httpp response on " + res.getReq().getPath()  +", headers:"+res.headers());		
					}

				});
				
				http.get(ctx, "http://localhost:5189/nest", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got http response on " + res.getReq().getPath()  +", headers:"+res.headers());		
					}

				});
				httpp.get(ctx, "http://localhost:5189/nest", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got httpp response on " + res.getReq().getPath()  +", headers:"+res.headers());			
					}

				});
				
				http.get(ctx, "http://localhost:5189/nest/nest", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got http response on " + res.getReq().getPath()  +", headers:"+res.headers());	
					}

				});
				httpp.get(ctx, "http://localhost:5189/nest/nest", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got httpp response on " + res.getReq().getPath()  +", headers:"+res.headers());		
					}

				});
				
				http.get(ctx, "http://localhost:5189/nest/nest/nest", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got http response on " + res.getReq().getPath()  +", headers:"+res.headers());	
					}

				});
				httpp.get(ctx, "http://localhost:5189/nest/nest/nest", new responseListener(){

					@Override
					public void onResponse(IncomingMessage res) throws Exception {
						Log.d(TAG, "got httpp response on " + res.getReq().getPath()  +", headers:"+res.headers());		
					}

				});
			}
			
		}, 2000);
		
		return true;
	}
	
	public ConnectTest(){
		this.ctx = new NodeContext(); 
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					testStack();
					testNest();
					
					// run loop
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
