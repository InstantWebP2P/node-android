// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.http;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.net.AbstractSocket;

public final class ServerResponse 
extends OutgoingMessage {
	private final static String TAG = "ServerResponse";

	private boolean _sent100;

	private String statusMessage;

	private boolean _expect_continue;
	
	private Listener onServerResponseClose;
	
	public ServerResponse(NodeContext context, IncomingMessage req) {
		super(context);

		this.statusCode = 200;
		this.statusMessage = null;

		if (req.method() == "HEAD") this._hasBody = false;

		this.sendDate = true;

		if (req.getHttpVersionMajor() < 1 || req.getHttpVersionMinor() < 1) {
			// TBD...
			///this.useChunkedEncodingByDefault = http.chunkExpression.test(req.headers.te);
			this.useChunkedEncodingByDefault = (req.getHeaders().containsKey("te") && Pattern.matches(http.chunkExpression, req.getHeaders().get("te").get(0)));
			this.shouldKeepAlive = false;
		}
	}
	private ServerResponse(){super(null);}

	protected void _finish() throws Exception {
		///DTRACE_HTTP_SERVER_RESPONSE(this.connection);
		///COUNTER_HTTP_SERVER_RESPONSE();
		super._finish();
	}

	// response.setTimeout(msecs, callback)

	// Event listeners
	public void onClose(final closeListener cb) throws Exception {
		this.on("close", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {                   
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
			public void onEvent(Object data) throws Exception {                   
				cb.onFinish();
			}

		});
	}
	public static interface finishListener {
		public void onFinish() throws Exception;
	}

	public void assignSocket(final AbstractSocket socket) throws Exception {
		assert(null == socket.get_httpMessage());
		socket.set_httpMessage(this);

		onServerResponseClose = new Listener() {

			@Override
			public void onEvent(Object data) throws Exception {
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
				if (socket.get_httpMessage()!=null) ((ServerResponse)(socket.get_httpMessage())).emit("close");
			}

		};
		socket.on("close", onServerResponseClose);

		this.socket = socket;
		this.connection = socket;
		this.emit("socket", socket);
		this._flush();
	}


	public void detachSocket(AbstractSocket socket) {
		assert(socket.get_httpMessage() == this);
		socket.removeListener("close", onServerResponseClose);
		socket.set_httpMessage(null);
		this.socket = this.connection = null;
	}

	public void writeContinue(WriteCB cb) throws Exception {
		this._writeRaw("http/1.1 100 Continue" + http.CRLF + http.CRLF, "utf-8", cb);
		this._sent100 = true;
	}

	protected void _implicitHeader() throws Exception {
		this.writeHead(this.statusCode, null, null);
	}

	public void writeHead(int statusCode, String statusMessage, Map<String, List<String>> obj) throws Exception {
		Map<String, List<String>> headers;

		Log.d(TAG, "..... -1");

		if (Util.zeroString(statusMessage)) {
			this.statusMessage = http.STATUS_CODES.containsKey(statusCode) ?
					http.STATUS_CODES.get(statusCode) : "unknown";
		} else {
			this.statusMessage = statusMessage;
		}

		this.statusCode = statusCode;

		if (this._headers != null) {
			Log.d(TAG, "..... -2");

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

		String statusLine = "http/1.1 " + statusCode + " " +
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
		if (this.is_expect_continue() && !this._sent100) {
			setShouldKeepAlive(false);
		}
		
		Log.d(TAG, "..... -3");

		this._storeHeader(statusLine, headers);
	}

	public void writeHeader(int statusCode, String statusMessage, Map<String, List<String>> headers) throws Exception {
		this.writeHead(statusCode, statusMessage, headers);
	}
	
	public void writeHead(int statusCode, Map<String, List<String>> headers) throws Exception {
		this.writeHead(statusCode, null, headers);
	}
	
	public void writeHead(int statusCode) throws Exception {
		this.writeHead(statusCode, null, null);
	}

	public boolean sendDate() {
		return super.sendDate;
	}
	
	public int statusCode() {
		return super.statusCode;
	}
	
	/**
	 * @return the _expect_continue
	 */
	public boolean is_expect_continue() {
		return _expect_continue;
	}

	/**
	 * @param _expect_continue the _expect_continue to set
	 */
	public void set_expect_continue(boolean _expect_continue) {
		this._expect_continue = _expect_continue;
	}

}
