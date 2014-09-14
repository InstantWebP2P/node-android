package com.iwebpp.node.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.iwebpp.node.IncomingMessage;
import com.iwebpp.node.IncomingParser;
import com.iwebpp.node.Listener;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Options;
import com.iwebpp.node.TCP;
import com.iwebpp.node.http_parser_type;
import com.iwebpp.node.var;
import com.iwebpp.node.HTTP.ServerResponse;
import com.iwebpp.node.HTTP.ServerResponse.closeListener;
import com.iwebpp.node.HTTP.ServerResponse.finishListener;
import com.iwebpp.node.TCP.Socket;
import com.iwebpp.node.Writable.WriteCB;

public class ServerResponse 
extends OutgoingMessage {
	private final static String TAG = "ServerResponse";

	private Socket socket;
	private Socket connection;

	public ClientRequest req;

	protected ServerResponse(NodeContext context, Options options, IncomingMessage req) {
		super(context, options);
		
	}

	@Override
	protected void _write(Object chunk, String encoding, WriteCB cb)
			throws Exception {
		// TODO Auto-generated method stub

	}

	public int writeContinue() {
		return 0;

	}

	public int writeHead(int statusCode, String reasonPhrase, Map<String, String> headers) {

		return 0;
	}

	public int writeHead(int statusCode) {

		return 0;
	}

	public int writeHead(int statusCode, Map<String, String> headers) {

		return 0;
	}
	
	// response.setTimeout(msecs, callback)

	public int setHeader(String name, String value) {

		return 0;
	}

	public int setHeader(String name, List<String> values) {

		for (String value : values) 
			setHeader(name, value);

		return 0;
	}

	public boolean headersSent() {
		return false;

	}

	public void sendDate(boolean send) {

	}
	public boolean sendDate() {
		return false;
	}

	public String getHeader(String name) {
		return null;
	}

	public boolean removeHeader(String name) {

		return false;
	}

	///response.write(chunk, [encoding])
	public boolean write(Object chunk, String encoding) {

		return false;
	}

	public int addTrailers(Map<String, String> headers) {

		return 0;
	}

	///response.end([data], [encoding])
	public void end(Object chunk, String encoding) {

	}

	// Event listeners
	public void onClose(final closeListener cb) throws Exception {
		this.on("close", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {                   
				cb.onClose();
			}

		});
	}
	public static interface closeListener {
		public void onClose() throws Exception;
	}

	public void onFinish(final finishListener cb) throws Exception {
		this.on("finish", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {                   
				cb.onFinish();
			}

		});
	}
	public static interface finishListener {
		public void onFinish() throws Exception;
	}

	// Parser on request
	private class parserOnIncoming 
	extends IncomingParser {
		private List<IncomingMessage> incomings;
		private NodeContext context;
		
		private List<ServerResponse> outgoing;
		
		
		public parserOnIncoming(NodeContext ctx, http_parser_type type, TCP.Socket socket) {
			super(type, socket);
			this.context = ctx;
			
			incomings = new ArrayList<IncomingMessage>();
		}
		private parserOnIncoming() {super(null, null);}

		@Override
		protected boolean onIncoming(IncomingMessage req,
				boolean shouldKeepAlive) throws Exception {
			incomings.add(req);

			// If the writable end isn't consuming, then stop reading
			// so that we don't become overwhelmed by a flood of
			// pipelined requests that may never be resolved.
			if (!socket._paused) {
				boolean needPause = socket.get_writableState().isNeedDrain();
				if (needPause) {
					socket._paused = true;
					// We also need to pause the parser, but don't do that until after
					// the call to execute, because we may still be processing the last
					// chunk.
					socket.pause();
				}
			}

			// TBD...
			ServerResponse res = new ServerResponse(context, null, req);

			res.setShouldKeepAlive(shouldKeepAlive);
			//DTRACE_HTTP_SERVER_REQUEST(req, socket);
			//COUNTER_HTTP_SERVER_REQUEST();

			if (socket._httpMessage != null) {
				// There are already pending outgoing res, append.
				outgoing.add(res);
			} else {
				res.assignSocket(socket);
			}

			// When we're finished writing the response, check if this is the last
			// respose, if so destroy the socket.
			res.on("prefinish", resOnFinish);

			Listener resOnFinish = new Listener() {

				@Override
				public void onListen(Object data) throws Exception {
					// Usually the first incoming element should be our request.  it may
					// be that in the case abortIncoming() was called that the incoming
					// array will be empty.
					assert(incoming.length === 0 || incoming[0] === req);

					incoming.shift();

					// if the user never called req.read(), and didn't pipe() or
					// .resume() or .on('data'), then we call req._dump() so that the
					// bytes will be pulled off the wire.
					if (!req._consuming && !req._readableState.resumeScheduled)
						req._dump();

					res.detachSocket(socket);

					if (res._last) {
						socket.destroySoon();
					} else {
						// start sending the next message
						var m = outgoing.shift();
						if (m) {
							m.assignSocket(socket);
						}
					}
				}
				
			};
			
			if (!util.isUndefined(req.headers.expect) &&
					(req.httpVersionMajor == 1 && req.httpVersionMinor == 1) &&
					continueExpression.test(req.headers['expect'])) {
				res._expect_continue = true;
				if (EventEmitter.listenerCount(self, 'checkContinue') > 0) {
					self.emit('checkContinue', req, res);
				} else {
					res.writeContinue();
					self.emit('request', req, res);
				}
			} else {
				self.emit('request', req, res);
			}
			return false; // Not a HEAD response. (Not even a response!)
		}
	}

	public void assignSocket(final TCP.Socket socket) {
		assert(null == socket._httpMessage);
		socket._httpMessage = this;

		Listener onServerResponseClose = new Listener() {

			@Override
			public void onListen(Object data) throws Exception {
				// EventEmitter.emit makes a copy of the 'close' listeners array before
				// calling the listeners. detachSocket() unregisters onServerResponseClose
				// but if detachSocket() is called, directly or indirectly, by a 'close'
				// listener, onServerResponseClose is still in that copy of the listeners
				// array. That is, in the example below, b still gets called even though
				// it's been removed by a:
				//
				//   var obj = new events.EventEmitter;
				//   obj.on('event', a);
				//   obj.on('event', b);
				//   function a() { obj.removeListener('event', b) }
				//   function b() { throw "BAM!" }
				//   obj.emit('event');  // throws
				//
				// Ergo, we need to deal with stale 'close' events and handle the case
				// where the ServerResponse object has already been deconstructed.
				// Fortunately, that requires only a single if check. :-)
				if (socket._httpMessage!=null) ((ServerResponse)(socket._httpMessage)).emit("close");
			}

		};
		socket.on("close", onServerResponseClose);


		this.socket = socket;
		this.connection = socket;
		this.emit("socket", socket);
		this._flush();
	}

}
