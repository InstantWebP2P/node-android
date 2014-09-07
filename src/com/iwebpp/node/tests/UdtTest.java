package com.iwebpp.node.tests;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.UDT;
import com.iwebpp.node.Util;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.NodeContext.IntervalCallback;
import com.iwebpp.node.UDT.Server;
import com.iwebpp.node.UDT.Socket;
import com.iwebpp.node.UDT.Server.ListenCallback;
import com.iwebpp.node.Writable.WriteCB;

public final class UdtTest {
	private static final String TAG = "UdtTest";
	private NodeContext ctx;

	private boolean testListening() {
		Server srv;
		try {
			srv = new UDT.Server(ctx, new Server.Options(false), null);

			srv.listen("0.0.0.0", 51688, 4, 18, -1, new ListenCallback(){

				@Override
				public void onListen() {
					Log.d(TAG, "UDT server listening on 0.0.0.0:51688");
				}

			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;   
	}

	private boolean testConnect() {
		Server srv;
		final Socket cln;
		int port = 51686;
		
		try {
			srv = new UDT.Server(ctx, new Server.Options(false), null);

			srv.on("connection", new Listener(){

				@Override
				public void invoke(Object data) throws Exception {
					Socket peer = (Socket)data;
					
					peer.pipe(peer, true);
					
					Log.d(TAG, "got connection then echo it");
				}
				
			});
			srv.listen("0.0.0.0", port, 4, 18, -1, null);
			
			cln = new UDT.Socket(ctx, new Socket.Options(false, null));
			
			cln.connect(port, new Listener(){

				@Override
				public void invoke(Object data) throws Exception {
					Log.d(TAG, "got connected");
					
					cln.on("readable", new Listener(){

						@Override
						public void invoke(Object data) throws Exception {
		    				Object chunk;

		    				while (null != (chunk = cln.read(68))) {
		    					Log.d(TAG, "client read: "+Util.chunkToString(chunk, "utf8"));
		    				}

		    			}
						
					});
					
					cln.write("hello word", "utf-8", new WriteCB(){

						@Override
						public void invoke(String error) throws Exception {
							Log.d(TAG, "client write done");							
						}
						
					});
					
					// write after 2s
					ctx.setInterval(new IntervalCallback(){

						@Override
						public void onInterval() throws Exception {
							cln.write("hello word: "+System.currentTimeMillis() , "utf-8", null);							
						}
						
					}, 2000);
					
				}
				
			});		
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	public UdtTest(){
		this.ctx = new NodeContext(); 
	}

	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				///testListening();
				testConnect();

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
