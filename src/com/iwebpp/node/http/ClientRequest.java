package com.iwebpp.node.http;

import java.nio.ByteBuffer;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.nextTickCallback;
import com.iwebpp.node.TCP;
import com.iwebpp.node.TCP.Socket;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.HttpParser.http_parser_type;
import com.iwebpp.node.Writable2;
import com.iwebpp.node.http.http.response_socket_head_t;

public class ClientRequest 
extends OutgoingMessage {
	private final static String TAG = "ClientRequest";

	public IncomingMessage res;

	public String method;

	public boolean upgradeOrConnect;

	protected TCP.Socket socket;

	public IncomingParser parser;

	protected ClientRequest(NodeContext context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void _write(Object chunk, String encoding, WriteCB cb)
			throws Exception {
		// TODO Auto-generated method stub

	}

	///request.write(chunk, [encoding])
	public boolean write(Object chunk, String encoding) {

		return false;
	}

	///request.end([data], [encoding])
	public void end(Object data, String encoding) {

	}

	public void abort() {

	}

	///request.setTimeout(timeout, [callback])

	public int setNoDelay(boolean noDelay) {

		return 0;
	}

	public int setSocketKeepAlive(boolean enable, int initialDelay) {

		return 0;
	}

	// Event listeners
	public void onResponse(final responseListener cb) throws Exception {
		this.on("response", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				IncomingMessage data = (IncomingMessage)raw;

				cb.onResponse(data);
			}

		});
	}
	public static interface responseListener {
		public void onResponse(IncomingMessage res) throws Exception;
	}

	public void onSocket(final socketListener cb) throws Exception {
		this.on("socket", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				TCP.Socket data = (TCP.Socket)raw;

				cb.onSocket(data);
			}

		});
	}
	public static interface socketListener {
		public void onSocket(TCP.Socket socket) throws Exception;
	}

	public void onConnect(final connectListener cb) throws Exception {
		this.on("connect", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				response_socket_head_t data = (response_socket_head_t)raw;

				cb.onConnect(data.response, data.socket, data.head);
			}

		});
	}
	public static interface connectListener {
		public void onConnect(IncomingMessage res, TCP.Socket socket, ByteBuffer head) throws Exception;
	}

	public void onUpgrade(final upgradeListener cb) throws Exception {
		this.on("upgrade", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				response_socket_head_t data = (response_socket_head_t)raw;

				cb.onUpgrade(data.response, data.socket, data.head);
			}

		});
	}
	public static interface upgradeListener {
		public void onUpgrade(IncomingMessage res, TCP.Socket socket, ByteBuffer head) throws Exception;
	}

	public void onContinue(final continueListener cb) throws Exception {
		this.on("continue", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {                   
				cb.onContinue();
			}

		});
	}
	public static interface continueListener {
		public void onContinue() throws Exception;
	}

	// Parser on response
	private class parserOnIncomingClient 
	extends IncomingParser {
		private NodeContext context;

		public parserOnIncomingClient(NodeContext ctx, TCP.Socket socket) {
			super(ctx, http_parser_type.HTTP_RESPONSE, socket);
			this.context = ctx;
		}
        private parserOnIncomingClient(){}
		
		@Override
		protected boolean onIncoming(IncomingMessage incoming,
				boolean shouldKeepAlive) throws Exception {
			Socket socket = this.socket;
			final ClientRequest req = (ClientRequest)socket._httpMessage;


			// propogate "domain" setting...
			/*if (req.domain && !res.domain) {
				Log.d(TAG, "setting res.domain");
				res.domain = req.domain;
			}*/

			Log.d(TAG, "AGENT incoming response!");

			if (req.res != null) {
				// We already have a response object, this means the server
				// sent a double response.
				socket.destroy(null);
				///return;
				return false;
			}
			req.res = res;

			// Responses to CONNECT request is handled as Upgrade.
			if (req.method == "CONNECT") {
				res.setUpgrade(true);
				return true; // skip body
			}

			// Responses to HEAD requests are crazy.
			// HEAD responses aren't allowed to have an entity-body
			// but *can* have a content-length which actually corresponds
			// to the content-length of the entity-body had the request
			// been a GET.
			boolean isHeadResponse = req.method == "HEAD";
			Log.d(TAG, "AGENT isHeadResponse "+isHeadResponse);

			if (res.statusCode() == 100) {
				// restart the parser, as this is a continue message.
				///delete req.res; // Clear res so that we don't hit double-responses.
				req.res = null;
				req.emit("continue");
				return true;
			}

			if (req.shouldKeepAlive && !shouldKeepAlive && !req.upgradeOrConnect) {
				// Server MUST respond with Connection:keep-alive for us to enable it.
				// If we've been upgraded (via WebSockets) we also shouldn't try to
				// keep the connection open.
				req.shouldKeepAlive = false;
			}


			///DTRACE_HTTP_CLIENT_RESPONSE(socket, req);
			///COUNTER_HTTP_CLIENT_RESPONSE();
			req.res = res;
			res.req = req;
			
			// add our listener first, so that we guarantee socket cleanup
			Listener responseOnEnd = new Listener() {

				@Override
				public void onEvent(Object data) throws Exception {
					///var res = this;
					ClientRequest req = res.req;
					final Socket socket = req.socket;

					if (!req.shouldKeepAlive) {
						if (socket.writable()) {
							Log.d(TAG, "AGENT socket.destroySoon()");
							socket.destroySoon();
						}
						assert(!socket.writable());
					} else {
						Log.d(TAG, "AGENT socket keep-alive");
						// TBD...
						///if (req.timeoutCb) {
						///  socket.setTimeout(0, req.timeoutCb);
						///  req.timeoutCb = null;
						///}

						// TBD...
						///socket.removeListener("close", socketCloseListener);
						///socket.removeListener("error", socketErrorListener);
						
						// Mark this socket as available, AFTER user-added end
						// handlers have a chance to run.
						///process.nextTick(function() {
						context.nextTick(new nextTickCallback() {

							@Override
							public void onNextTick() throws Exception {
								socket.emit("free");
							}
							
						});
						
					}
				}
			};
			res.on("end", responseOnEnd);
			
			boolean handled = req.emit("response", res);

			// If the user did not listen for the 'response' event, then they
			// can't possibly read the data, so we ._dump() it into the void
			// so that the socket doesn't hang there in a paused state.
			if (!handled)
				res._dump();

			return isHeadResponse;
		}

	}

	@Override
	protected void _implicitHeader() {
		// TODO Auto-generated method stub
		
	}
}
