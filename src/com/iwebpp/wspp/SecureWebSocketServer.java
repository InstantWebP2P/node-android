package com.iwebpp.wspp;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.wspp.SecureWebSocket.SecInfo;
import com.iwebpp.wspp.WebSocketServer.ListeningCallback;
import com.iwebpp.wspp.WebSocketServer.Options;
import com.iwebpp.wspp.WebSocketServer.onconnectionListener;
import com.iwebpp.wspp.WebSocketServer.onerrorListener;

public final class SecureWebSocketServer 
extends EventEmitter2 {
	private static final String TAG = "SecureWebSocketServer";

	private SecInfo mySecInfo;

	private WebSocketServer wss;

	public SecureWebSocketServer(
			final NodeContext ctx,
			final Options options,
			final ListeningCallback callback,
			final SecureWebSocket.SecInfo sec) throws Exception {
		// setup security info
		this.mySecInfo = sec;

		this.wss = new WebSocketServer(ctx, options, callback);

		// wrap ServerClient
		this.wss.onconnection(new WebSocketServer.onconnectionListener() {

			@Override
			public void onConnection(WebSocket ws) throws Exception {
				// Hand shake process
				final SecureWebSocket sws = new SecureWebSocket(ctx, ws, sec);
				
				sws.on("secure", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						debug(TAG, "secure ServerClient");

						SecureWebSocketServer.this.emit("connection", sws);
					}

				});
			}

		});
	}
	
	public void onconnection(final onconnectionListener cb) throws Exception {
		if (cb != null)
			this.on("connection", new Listener(){

				@Override
				public void onEvent(Object raw) throws Exception {
					SecureWebSocket data = (SecureWebSocket)raw;		

					cb.onConnection(data);
				}

			});
	}
	public interface onconnectionListener {
		public void onConnection(SecureWebSocket socket) throws Exception;
	}
	
	public void onerror(final onerrorListener cb) throws Exception {
		this.wss.onerror(cb);
	}

	public void close() throws Exception {
		this.wss.close();
	}

}
