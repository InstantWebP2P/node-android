package com.iwebpp.wspp.tests;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;

import android.util.Log;

import com.iwebpp.crypto.NaclCert;
import com.iwebpp.crypto.TweetNaclFast;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.IntervalListener;
import com.iwebpp.wspp.SecureWebSocket;
import com.iwebpp.wspp.WebSocket;
import com.iwebpp.wspp.SecureWebSocketServer;
import com.iwebpp.wspp.WebSocket.ErrorEvent;
import com.iwebpp.wspp.WebSocket.MessageEvent;
import com.iwebpp.wspp.WebSocket.OpenEvent;
import com.iwebpp.wspp.WebSocket.onmessageListener;
import com.iwebpp.wspp.WebSocket.onopenListener;
import com.iwebpp.wspp.WebSocketServer;

public final class SecureWebSocketServerTest {
	private static final String TAG = "SecureWebSocketServerTest";
	
	private NodeContext ctx;

	private boolean testConnectPair() throws Exception {

		WebSocketServer.Options wssopt = new WebSocketServer.Options();
		wssopt.port = 6668;
		wssopt.path = "/wspp";

		TweetNaclFast.Box.KeyPair kp = TweetNaclFast.Box.keyPair();
		SecureWebSocket.SecInfo sec = new SecureWebSocket.SecInfo(kp.getPublicKey(), kp.getSecretKey());
		
		SecureWebSocketServer wss = new SecureWebSocketServer(ctx, wssopt, new WebSocketServer.ListeningCallback() {

			@Override
			public void onListening() throws Exception {
				Log.d(TAG, "websocket server listening ...");		
				
				TweetNaclFast.Box.KeyPair kp = TweetNaclFast.Box.keyPair();
				SecureWebSocket.SecInfo sec = new SecureWebSocket.SecInfo(kp.getPublicKey(), kp.getSecretKey());

				final SecureWebSocket ws = new SecureWebSocket(ctx, "ws://localhost:6668/wspp", new WebSocket.Options(), sec);

				ws.onmessage(new onmessageListener(){

					@Override
					public void onMessage(MessageEvent event) throws Exception {
						Log.d(TAG, "\n\tmessage: "+event.toString());		

						if (event.isBinary()) {
							Log.d(TAG, "binary message: "+event.getData().toString());
							
							ByteBuffer mb = (ByteBuffer)event.getData();
							String ms = new String(mb.array(), "utf-8");
							Log.d(TAG, "binary message parsed: "+ms);
						} else {
							Log.d(TAG, "text message: "+(String)(event.getData()));
						}
					}

				});
				
				ws.onerror(new WebSocket.onerrorListener() {
					
					@Override
					public void onError(ErrorEvent event) throws Exception {
                        Log.d(TAG, "ws error:"+event.getCode()+",message:"+event.getError());						
					}
					
				});

				ws.onopen(new onopenListener(){

					@Override
					public void onOpen(OpenEvent event) throws Exception {
                        ws.send("Hello, tom zhou".getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);	
                        
                        ctx.setInterval(new IntervalListener(){

							@Override
							public void onInterval() throws Exception {
		                        ws.send(("Hello, tom zhou @"+new Date()).getBytes("utf-8"), new WebSocket.SendOptions(true, true), null);									
							}
                        	
                        }, 3000);
					}
					
				});
				
			}

		}, sec);

		wss.onconnection(new SecureWebSocketServer.onconnectionListener() {

			@Override
			public void onConnection(final SecureWebSocket socket) throws Exception {
				Log.d(TAG, "new ws client:"+socket);	
				
				socket.onmessage(new WebSocket.onmessageListener() {
					
					@Override
					public void onMessage(MessageEvent event) throws Exception {
						Log.d(TAG, "\n\tclient message: "+event.toString());		

						if (event.isBinary()) {
							Log.d(TAG, "binary message: "+event.getData().toString());

							ByteBuffer mb = (ByteBuffer)event.getData();
							String ms = new String(mb.array(), "utf-8");
							Log.d(TAG, "binary message parsed: "+ms);
							
							socket.send(event.getData(), new WebSocket.SendOptions(true, true), null);
						} else {
							Log.d(TAG, "text message: "+(String)(event.getData()));
							
							socket.send((event.getData().toString()+"@srv").getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);
						}
					}
					
				});
				
				socket.send("Hello@srv".getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);
			}

		});
		
		return true;
	}

	private boolean testConnectPairOverUDP() throws Exception {

		WebSocketServer.Options wssopt = new WebSocketServer.Options();
		wssopt.port = 6668;
		wssopt.path = "/wspp";
		wssopt.httpp = true;

		TweetNaclFast.Box.KeyPair kp = TweetNaclFast.Box.keyPair();
		SecureWebSocket.SecInfo sec = new SecureWebSocket.SecInfo(kp.getPublicKey(), kp.getSecretKey());
		
		SecureWebSocketServer wss = new SecureWebSocketServer(ctx, wssopt, new WebSocketServer.ListeningCallback() {

			@Override
			public void onListening() throws Exception {
				Log.d(TAG, "httpp websocket server listening ...");		
				
				WebSocket.Options wsopt = new WebSocket.Options();
				wsopt.httpp = true;
				
				final WebSocket ws = new WebSocket(ctx, "ws://localhost:6668/wspp", null, wsopt);

				ws.onmessage(new onmessageListener(){

					@Override
					public void onMessage(MessageEvent event) throws Exception {
						Log.d(TAG, "\n\thttpp message: "+event.toString());		

						if (event.isBinary()) {
							Log.d(TAG, "httpp binary message: "+event.getData().toString());

							ByteBuffer mb = (ByteBuffer)event.getData();
							String ms = new String(mb.array(), "utf-8");
							Log.d(TAG, "binary message parsed: "+ms);
							
						} else {
							Log.d(TAG, "httpp text message: "+(String)(event.getData()));
						}
					}

				});
				
				ws.onerror(new WebSocket.onerrorListener() {
					
					@Override
					public void onError(ErrorEvent event) throws Exception {
                        Log.d(TAG, "httpp ws error:"+event.getCode()+",message:"+event.getError());						
					}
					
				});

				ws.onopen(new onopenListener(){

					@Override
					public void onOpen(OpenEvent event) throws Exception {
                        ws.send("Hello, tom zhou".getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);	
                        
                        ctx.setInterval(new IntervalListener(){

							@Override
							public void onInterval() throws Exception {
		                        ws.send(("Hello, tom zhou @"+new Date()).getBytes("utf-8"), new WebSocket.SendOptions(true, true), null);									
							}
                        	
                        }, 3000);
					}
					
				});
				
			}

		}, sec);

		wss.onconnection(new SecureWebSocketServer.onconnectionListener() {

			@Override
			public void onConnection(final SecureWebSocket socket) throws Exception {
				Log.d(TAG, "httpp new ws client:"+socket);	
				
				socket.onmessage(new WebSocket.onmessageListener() {
					
					@Override
					public void onMessage(MessageEvent event) throws Exception {
						Log.d(TAG, "\n\thttpp client message: "+event.toString());		

						if (event.isBinary()) {
							Log.d(TAG, "httpp binary message: "+event.getData().toString());

							ByteBuffer mb = (ByteBuffer)event.getData();
							String ms = new String(mb.array(), "utf-8");
							Log.d(TAG, "binary message parsed: "+ms);
							
							socket.send(event.getData(), new WebSocket.SendOptions(true, true), null);
						} else {
							Log.d(TAG, "httpp text message: "+(String)(event.getData()));
							
							socket.send(event.getData().toString()+"@srv httpp", new WebSocket.SendOptions(false, false), null);
						}
					}
					
				});
				
				socket.send("httpp Hello@srv", new WebSocket.SendOptions(false, false), null);
			}

		});
		
		return true;
	}
	
	// V2 NACL CERT test
	private boolean testConnectPairV2() throws Exception {

		WebSocketServer.Options wssopt = new WebSocketServer.Options();
		wssopt.port = 6688;
		wssopt.path = "/wspp";

		// generate rootCA, Cert
		NaclCert.CAInfo info = new NaclCert.CAInfo();

		info.ca = "iwebpp.com";
		info.tte = System.currentTimeMillis() + 3600000L*24*365000; // 1000 years
		final NaclCert.CAInfo rootCA = NaclCert.generateCA(info);
		
		TweetNaclFast.Box.KeyPair kp = TweetNaclFast.Box.keyPair();
		
		// prepare reqdesc
		NaclCert.ReqDescSignByCa desc = new NaclCert.ReqDescSignByCa();
		desc.version = "1.0";
		desc.type = "ca";
		desc.tte = System.currentTimeMillis() + 3600000L*24*365;
		desc.publickey = kp.getPublicKey();

		// domains 
		desc.names = new ArrayList<String>();
		desc.names.add("51dese.com");
		desc.names.add("ruyier.com");
		desc.names.add("localhost");

		// ips
		desc.ips = new ArrayList<String>();
		desc.ips.add("127.0.0.1");
		desc.ips.add("10.1.1.1");

		NaclCert.Cert cert = NaclCert.generate(desc, rootCA);
		
		SecureWebSocket.SecInfo secinfo = new SecureWebSocket.SecInfo(
				kp.getPublicKey(), kp.getSecretKey(),
				cert, rootCA.cert, true);
		
		SecureWebSocketServer wss = new SecureWebSocketServer(ctx, wssopt, new WebSocketServer.ListeningCallback() {

			@Override
			public void onListening() throws Exception {
				Log.d(TAG, "V2 websocket server listening ...");		
				
				TweetNaclFast.Box.KeyPair kp = TweetNaclFast.Box.keyPair();
				
				// prepare reqdesc
				NaclCert.ReqDescSignByCa desc = new NaclCert.ReqDescSignByCa();
				desc.version = "1.0";
				desc.type = "ca";
				desc.tte = System.currentTimeMillis() + 3600000L*24*365;
				desc.publickey = kp.getPublicKey();

				// domains 
				desc.names = new ArrayList<String>();
				desc.names.add("51dese.com");
				desc.names.add("ruyier.com");
				desc.names.add("localhost");

				// ips
				desc.ips = new ArrayList<String>();
				desc.ips.add("127.0.0.1");
				desc.ips.add("10.1.1.1");

				NaclCert.Cert cert = NaclCert.generate(desc, rootCA);
				
				SecureWebSocket.SecInfo secinfo = new SecureWebSocket.SecInfo(
						kp.getPublicKey(), kp.getSecretKey(),
						cert, rootCA.cert, true);
				
				final SecureWebSocket ws = new SecureWebSocket(ctx, "ws://localhost:6688/wspp", new WebSocket.Options(), secinfo);

				ws.onmessage(new onmessageListener(){

					@Override
					public void onMessage(MessageEvent event) throws Exception {
						Log.d(TAG, "\n\tV2 message: "+event.toString());		

						if (event.isBinary()) {
							Log.d(TAG, "V2 binary message: "+event.getData().toString());
							
							ByteBuffer mb = (ByteBuffer)event.getData();
							String ms = new String(mb.array(), "utf-8");
							Log.d(TAG, "V2 binary message parsed: "+ms);
						} else {
							Log.d(TAG, "V2 text message: "+(String)(event.getData()));
						}
					}

				});
				
				ws.onerror(new WebSocket.onerrorListener() {
					
					@Override
					public void onError(ErrorEvent event) throws Exception {
                        Log.d(TAG, "V2 ws error:"+event.getCode()+",message:"+event.getError());						
					}
					
				});

				ws.onopen(new onopenListener(){

					@Override
					public void onOpen(OpenEvent event) throws Exception {
                        ws.send("V2 Hello, tom zhou".getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);	
                        
                        ctx.setInterval(new IntervalListener(){

							@Override
							public void onInterval() throws Exception {
		                        ws.send(("V2 Hello, tom zhou @"+new Date()).getBytes("utf-8"), new WebSocket.SendOptions(true, true), null);									
							}
                        	
                        }, 3000);
					}
					
				});
				
			}

		}, secinfo);

		wss.onconnection(new SecureWebSocketServer.onconnectionListener() {

			@Override
			public void onConnection(final SecureWebSocket socket) throws Exception {
				Log.d(TAG, "V2 new ws client:"+socket);	
				
				socket.onmessage(new WebSocket.onmessageListener() {
					
					@Override
					public void onMessage(MessageEvent event) throws Exception {
						Log.d(TAG, "\n\tV2 client message: "+event.toString());		

						if (event.isBinary()) {
							Log.d(TAG, "V2 binary message: "+event.getData().toString());

							ByteBuffer mb = (ByteBuffer)event.getData();
							String ms = new String(mb.array(), "utf-8");
							Log.d(TAG, "V2 binary message parsed: "+ms);
							
							socket.send(event.getData(), new WebSocket.SendOptions(true, true), null);
						} else {
							Log.d(TAG, "V2 text message: "+(String)(event.getData()));
							
							socket.send((event.getData().toString()+"@srv").getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);
						}
					}
					
				});
				
				socket.send("V2 Hello@srv".getBytes("utf-8"), new WebSocket.SendOptions(true, false), null);
			}

		});
		
		return true;
	}
	
	public SecureWebSocketServerTest(){
		this.ctx = new NodeContext(); 
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					///testConnectPair();
					///testConnectPairOverUDP();
					
					testConnectPairV2();
					///testConnectPairOverUDPV2();
					
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
