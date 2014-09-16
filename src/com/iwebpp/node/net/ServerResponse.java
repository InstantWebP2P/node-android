package com.iwebpp.node.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.TCP;
import com.iwebpp.node.TCP.Socket;
import com.iwebpp.node.Util;
import com.iwebpp.node.net.http.request_response_t; ;

public class ServerResponse 
extends OutgoingMessage {
	private final static String TAG = "ServerResponse";

	private Socket socket;
	private Socket connection;

	public ClientRequest req;

	private boolean _sent100;

	private int statusCode;

	private String statusMessage;

	private boolean _expect_continue;
	
	private Listener onServerResponseClose;
	
	protected ServerResponse(NodeContext context, Options options, IncomingMessage req) {
		super(context, options);

		this.statusCode = 200;
		this.statusMessage = null;

		if (req.method() == "HEAD") this._hasBody = false;

		this.sendDate = true;

		if (req.httpVersionMajor < 1 || req.httpVersionMinor < 1) {
			// TBD...
			///this.useChunkedEncodingByDefault = http.chunkExpression.test(req.headers.te);
			this.useChunkedEncodingByDefault = true;///Pattern.matches(http.chunkExpression, req.headers.get("te").get(0));
			this.shouldKeepAlive = false;
		}
	}

	protected void _finish() throws Exception {
		///DTRACE_HTTP_SERVER_RESPONSE(this.connection);
		///COUNTER_HTTP_SERVER_RESPONSE();
		super._finish();
	}

	@Override
	protected void _write(Object chunk, String encoding, WriteCB cb)
			throws Exception {
		// TODO Auto-generated method stub

	}

	// response.setTimeout(msecs, callback)

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
	public static class parserOnIncoming 
	extends IncomingParser {
		private List<IncomingMessage> incomings;
		private NodeContext context;

		private List<ServerResponse> outgoings;


		public parserOnIncoming(NodeContext ctx, TCP.Socket socket) {
			super(http_parser_type.HTTP_REQUEST, socket);
			this.context = ctx;

			incomings = new ArrayList<IncomingMessage>();
		}
		@SuppressWarnings("unused")
		private parserOnIncoming() {super(null, null);}

		@Override
		protected boolean onIncoming(final IncomingMessage req,
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
			final ServerResponse res = new ServerResponse(context, null, req);

			res.setShouldKeepAlive(shouldKeepAlive);
			//DTRACE_HTTP_SERVER_REQUEST(req, socket);
			//COUNTER_HTTP_SERVER_REQUEST();

			if (socket._httpMessage != null) {
				// There are already pending outgoing res, append.
				outgoings.add(res);
			} else {
				res.assignSocket(socket);
			}

			// When we're finished writing the response, check if this is the last
			// respose, if so destroy the socket.
			Listener resOnFinish = new Listener() {

				@Override
				public void onListen(Object data) throws Exception {
					// Usually the first incoming element should be our request.  it may
					// be that in the case abortIncoming() was called that the incoming
					// array will be empty.
					assert(incomings.size() == 0 || incomings.get(0) == req);

					incomings.remove(0);

					// if the user never called req.read(), and didn't pipe() or
					// .resume() or .on('data'), then we call req._dump() so that the
					// bytes will be pulled off the wire.
					if (!req.is_consuming() && !req.get_readableState().isResumeScheduled())
						req._dump();

					res.detachSocket(socket);

					if (res.is_last()) {
						socket.destroySoon();
					} else {
						// start sending the next message
						ServerResponse m = outgoings.remove(0);
						if (m != null) {
							m.assignSocket(socket);
						}
					}
				}

			};			
			res.on("prefinish", resOnFinish);

			if ( req.headers.containsKey("expect") &&
				(req.httpVersionMajor == 1 && req.httpVersionMinor == 1) &&
					///http.continueExpression == req.headers.get("expect").get(0)) {
					Pattern.matches(http.continueExpression, req.headers.get("expect").get(0))) {
				res._expect_continue = true;
				if (listenerCount("checkContinue") > 0) {
					this.emit("checkContinue", new request_response_t(req, res));
				} else {
					res.writeContinue(null);
					this.emit("request", new request_response_t(req, res));
				}
			} else {
				this.emit("request",  new request_response_t(req, res));
			}

			return false; // Not a HEAD response. (Not even a response!)
		}
	}

	public void assignSocket(final TCP.Socket socket) throws Exception {
		assert(null == socket._httpMessage);
		socket._httpMessage = this;

		onServerResponseClose = new Listener() {

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


	public void detachSocket(TCP.Socket socket) {
		assert(socket._httpMessage == this);
		socket.removeListener("close", onServerResponseClose);
		socket._httpMessage = null;
		this.socket = this.connection = null;
	}

	public void writeContinue(WriteCB cb) throws Exception {
		this._writeRaw("HTTP/1.1 100 Continue" + http.CRLF + http.CRLF, "utf-8", cb);
		this._sent100 = true;
	}

	public void _implicitHeader() throws Exception {
		this.writeHead(this.statusCode, null, null);
	}

	public void writeHead(int statusCode, String statusMessage, Map<String, List<String>> obj) throws Exception {
		Map<String, List<String>> headers;

		if (Util.zeroString(statusMessage)) {
			this.statusMessage = http.STATUS_CODES.containsKey(statusCode) ?
					http.STATUS_CODES.get(statusCode) : "unknown";
		} else {
			this.statusMessage = statusMessage;
		}

		this.statusCode = statusCode;

		if (this._headers != null) {
			// Slow-case: when progressive API and header fields are passed.
			if (obj != null) {
				for (Entry<String, List<String>> entry : obj.entrySet())
					if (!Util.zeroString(entry.getKey()))
						this.setHeader(entry.getKey(), entry.getValue());
			}
			// only progressive api is used
			headers = this._renderHeaders();
		} else {
			// only writeHead() called
			headers = obj;
		}

		String statusLine = "HTTP/1.1 " + statusCode + " " +
				this.statusMessage + http.CRLF;

		if (statusCode == 204 || statusCode == 304 ||
				(100 <= statusCode && statusCode <= 199)) {
			// RFC 2616, 10.2.5:
			// The 204 response MUST NOT include a message-body, and thus is always
			// terminated by the first empty line after the header fields.
			// RFC 2616, 10.3.5:
			// The 304 response MUST NOT contain a message-body, and thus is always
			// terminated by the first empty line after the header fields.
			// RFC 2616, 10.1 Informational 1xx:
			// This class of status code indicates a provisional response,
			// consisting only of the Status-Line and optional headers, and is
			// terminated by an empty line.
			this._hasBody = false;
		}

		// don't keep alive connections where the client expects 100 Continue
		// but we sent a final status; they may put extra bytes on the wire.
		if (this._expect_continue && !this._sent100) {
			setShouldKeepAlive(false);
		}

		this._storeHeader(statusLine, headers);
	}

	public void writeHeader(int statusCode, String statusMessage, Map<String, List<String>> headers) throws Exception {
		this.writeHead(statusCode, statusMessage, headers);
	}


}
