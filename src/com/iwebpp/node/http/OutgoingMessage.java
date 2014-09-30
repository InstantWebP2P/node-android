// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.http;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import android.util.Log;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.NodeContext.nextTickListener;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.stream.Writable;

public abstract class OutgoingMessage  
extends EventEmitter2 
implements Writable {
///extends Writable2 {
	private final static String TAG = "OutgoingMessage";

	protected static final String connectionExpression = "Connection";
	protected static final String transferEncodingExpression = "Transfer-Encoding";
	protected static final String closeExpression = "close";
	protected static final String contentLengthExpression = "Content-Length";
	protected static final String dateExpression = "Date";
	protected static final String expectExpression = "Expect";

	protected static final List<String> automaticHeaders;

	static {
		automaticHeaders = new ArrayList<String>();
		
		automaticHeaders.add("connection");
		automaticHeaders.add("content-length");
		automaticHeaders.add("transfer-encoding");
		automaticHeaders.add("date");

	}

	protected List<Object> output;
	protected List<String> outputEncodings;
	protected List<WriteCB> outputCallbacks;
	protected boolean _last;
	protected boolean shouldKeepAlive;
	/**
	 * @return the shouldKeepAlive
	 */
	public boolean isShouldKeepAlive() {
		return shouldKeepAlive;
	}

	/**
	 * @param shouldKeepAlive the shouldKeepAlive to set
	 */
	public void setShouldKeepAlive(boolean shouldKeepAlive) {
		this.shouldKeepAlive = shouldKeepAlive;
	}


	protected boolean chunkedEncoding;
	protected boolean useChunkedEncodingByDefault;
	protected boolean sendDate;
	protected boolean _hasBody;
	/**
	 * @return the _hasBody
	 */
	public boolean is_hasBody() {
		return _hasBody;
	}

	/**
	 * @param _hasBody the _hasBody to set
	 */
	public void set_hasBody(boolean _hasBody) {
		this._hasBody = _hasBody;
	}


	protected String _trailer;
	protected boolean finished;
	protected boolean _hangupClose;
	protected AbstractSocket socket;
	protected AbstractSocket connection;
	protected Map<String, Boolean> _removedHeader;
	protected boolean _headerSent;
	protected String _header;
	protected NodeContext context;

	protected Agent agent;

	protected Map<String, List<String>> _headers;

	protected Map<String, String> _headerNames;

	protected int statusCode;

	protected OutgoingMessage(NodeContext ctx) {
		///super(ctx, new Options(-1, false, "utf8", false));
		super();
		this.context = ctx;

		this.output = new LinkedList<Object>();
		this.outputEncodings = new LinkedList<String>();
		this.outputCallbacks = new LinkedList<WriteCB>();

		// TBD... change default to false
		///this.writable = true;
		this.writable(true);

		this._last = false;
		this.chunkedEncoding = false;
		this.shouldKeepAlive = true;
		this.useChunkedEncodingByDefault = true;
		this.sendDate = false;
		this._removedHeader = new Hashtable<String, Boolean>();

		this._hasBody = true;
		this._trailer = "";

		this.finished = false;
		this._hangupClose = false;

		this.socket = null;
		this.connection = null;
	}
	@SuppressWarnings("unused")
	private OutgoingMessage(){}

	/*
	 * OutgoingMessage.prototype.setTimeout = function(msecs, callback) {
if (callback)
this.on('timeout', callback);
if (!this.socket) {
this.once('socket', function(socket) {
  socket.setTimeout(msecs);
});
} else
this.socket.setTimeout(msecs);
};
	 * */

	// It's possible that the socket will be destroyed, and removed from
	// any messages, before ever calling this.  In that case, just skip
	// it, since something else is destroying this connection anyway.
	///OutgoingMessage.prototype.destroy = function(error) {
	public void destroy(final String error) throws Exception {
		if (this.socket != null)
			this.socket.destroy(error);
		else
			this.once("socket", new Listener() {

				@Override
				public void onEvent(Object data) throws Exception {
					AbstractSocket socket = (AbstractSocket)data;
					socket.destroy(error);
				}

			});
	}


	// This abstract either writing directly to the socket or buffering it.
	///OutgoingMessage.prototype._send = function(data, encoding, callback) {
	public boolean _send(Object data, String encoding, WriteCB callback) throws Exception {
		// This is a shameful hack to get the headers and first body chunk onto
		// the same packet. Future versions of Node are going to take care of
		// this at a lower level and in a more general way.
		if (!this._headerSent) {
			if (Util.isString(data) &&
				encoding != "hex" &&
				encoding != "base64") {
				data = this._header + data;
			} else {
				///this.output.unshift(this._header);
				///this.outputEncodings.unshift("binary");
				///this.outputCallbacks.unshift(null);
				this.output.add(0, this._header);
				this.outputEncodings.add(0, "utf-8");///"binary");// TBD...
				this.outputCallbacks.add(0, null);
			}
			this._headerSent = true;
		}
		return this._writeRaw(data, encoding, callback);
	}


	///OutgoingMessage.prototype._writeRaw = function(data, encoding, callback) {
	public boolean _writeRaw(Object data, String encoding, final WriteCB callback) throws Exception {
		///if (data.length === 0) {
		if (data == null || Util.chunkLength(data)==0) {
			///if (util.isFunction(callback))
			if (callback != null)
				///process.nextTick(callback);
				context.nextTick(new nextTickListener(){

					@Override
					public void onNextTick() throws Exception {
						callback.writeDone(null);							
					}

				});
			return true;
		}
		
		Log.d(TAG, "this.connection: "+this.connection);
		
		// TBD...
		if (this.connection!=null &&
			this.connection.get_httpMessage() == this &&
			this.connection.writable() &&
			!this.connection.isDestroyed()) {
			// There might be pending data in the this.output buffer.
			///while (this.output.length) {
			while (this.output.size() > 0) {
				if (!this.connection.writable()) {
					this._buffer(data, encoding, callback);
					return false;
				}
				///var c = this.output.shift();
				///var e = this.outputEncodings.shift();
				///var cb = this.outputCallbacks.shift();
				Object c = this.output.remove(0);
				String e = this.outputEncodings.remove(0);
				WriteCB cb = this.outputCallbacks.remove(0);

				this.connection.write(c, e, cb);
			}
			
			Log.d(TAG, "..... 13");

			// Directly write to socket.
			return this.connection.write(data, encoding, callback);
		} else if (this.connection!=null && this.connection.isDestroyed()) {
			Log.d(TAG, "..... 15");

			// The socket was destroyed.  If we're still trying to write to it,
			// then we haven't gotten the 'close' event yet.
			return false;
		} else {
			Log.d(TAG, "..... 16");

			// buffer, as long as we're not destroyed.
			this._buffer(data, encoding, callback);
			return false;
		}
	}

	protected boolean _buffer(Object data, String encoding, WriteCB callback) {
		this.output.add(data);
		this.outputEncodings.add(encoding);
		this.outputCallbacks.add(callback);
		return false;
	}

	// POJO beans
	protected class _State {
		boolean sentConnectionHeader;
		boolean sentContentLengthHeader;
		boolean sentTransferEncodingHeader;
		boolean sentDateHeader;
		boolean sentExpect;
		String messageHeader;

		public _State(
				boolean sentConnectionHeader,
				boolean sentContentLengthHeader,
				boolean sentTransferEncodingHeader, 
				boolean sentDateHeader, 
				boolean sentExpect,
				String message) {
			this.sentConnectionHeader = sentConnectionHeader;
			this.sentContentLengthHeader = sentContentLengthHeader;
			this.sentTransferEncodingHeader = sentTransferEncodingHeader;
			this.sentDateHeader = sentDateHeader;
			this.sentExpect = sentExpect;
			this.messageHeader = message;
		}
		@SuppressWarnings("unused")
		private _State(){}
	}

	protected void _storeHeader(String firstLine, Map<String, List<String>> headers) throws Exception {
		// firstLine in the case of request is: 'GET /index.html HTTP/1.1\r\n'
		// in the case of response it is: 'HTTP/1.1 200 OK\r\n'
		/*var state = {
				sentConnectionHeader: false,
				sentContentLengthHeader: false,
				sentTransferEncodingHeader: false,
				sentDateHeader: false,
				sentExpect: false,
				messageHeader: firstLine
		};*/
		_State state = new _State(false, false, false, false, false, firstLine);

		if (headers != null && !headers.isEmpty()) {
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				String key = entry.getKey();

				for (String value : entry.getValue())
					storeHeader(state, key, value);
			}

			/*
			var keys = Object.keys(headers);
			var isArray = util.isArray(headers);
			var field, value;

			for (var i = 0, l = keys.length; i < l; i++) {
				var key = keys[i];
				if (isArray) {
					field = headers[key][0];
					value = headers[key][1];
				} else {
					field = key;
					value = headers[key];
				}

				if (util.isArray(value)) {
					for (var j = 0; j < value.length; j++) {
						storeHeader(this, state, field, value[j]);
					}
				} else {
					storeHeader(this, state, field, value);
				}
			}*/
		}
		
		Log.d(TAG, "..... -5");

		// Date header
		if (this.sendDate == true && state.sentDateHeader == false) {
			state.messageHeader += "Date: " + context.utcDate() + http.CRLF;
		}
		
		Log.d(TAG, "..... -6");

		// Force the connection to close when the response is a 204 No Content or
		// a 304 Not Modified and the user has set a "Transfer-Encoding: chunked"
		// header.
		//
		// RFC 2616 mandates that 204 and 304 responses MUST NOT have a body but
		// node.js used to send out a zero chunk anyway to accommodate clients
		// that don't have special handling for those responses.
		//
		// It was pointed out that this might confuse reverse proxies to the point
		// of creating security liabilities, so suppress the zero chunk and force
		// the connection to close.
		int statusCode = this.statusCode;
		if ((statusCode == 204 || statusCode == 304) &&
				this.chunkedEncoding == true) {
			Log.d(TAG, ""+statusCode + " response should not use chunked encoding," +
					" closing connection.");

			this.chunkedEncoding = false;
			this.shouldKeepAlive = false;
		}

		// keep-alive logic
		///if (this._removedHeader.connection) {
		if (this._removedHeader.containsKey("connection") && 
			this._removedHeader.get("connection")) {
			this._last = true;
			this.shouldKeepAlive = false;
		} else if (state.sentConnectionHeader == false) {
			boolean shouldSendKeepAlive = this.shouldKeepAlive &&
					(state.sentContentLengthHeader ||
							this.useChunkedEncodingByDefault ||
							this.agent!=null);
			if (shouldSendKeepAlive) {
				state.messageHeader += "Connection: keep-alive\r\n";
			} else {
				this._last = true;
				state.messageHeader += "Connection: close\r\n";
			}
		}
		
		Log.d(TAG, "..... -7");

		if (state.sentContentLengthHeader == false &&
				state.sentTransferEncodingHeader == false) {
			if (this._hasBody && 
				!(this._removedHeader.containsKey("transfer-encoding") && 
				  this._removedHeader.get("transfer-encoding"))) {
				if (this.useChunkedEncodingByDefault) {
					state.messageHeader += "Transfer-Encoding: chunked\r\n";
					this.chunkedEncoding = true;
				} else {
					this._last = true;
				}
			} else {
				// Make sure we don't end the 0\r\n\r\n at the end of the message.
				this.chunkedEncoding = false;
			}
		}

		this._header = state.messageHeader + http.CRLF;
		this._headerSent = false;
		
		Log.d(TAG, "..... -8");

		// wait until the first body chunk, or close(), is sent to flush,
		// UNLESS we're sending Expect: 100-continue.
		if (state.sentExpect) this._send("", "utf-8", null);
	}

	protected void storeHeader(_State state, String field, String value) {
		OutgoingMessage self = this;
		
		// Protect against response splitting. The if statement is there to
		// minimize the performance impact in the common case.
		/// TBD...
		///if (/[\r\n]/.test(value))
		///if (value!=null && Pattern.matches("[\r\n]", value))
		value = value.replaceAll("[\r\n]+[ \t]*", "");

		state.messageHeader += field + ": " + value + http.CRLF;

		///if (connectionExpression == field) {
		if (Pattern.matches(connectionExpression, field)) {
			state.sentConnectionHeader = true;
			if (Pattern.matches(closeExpression, value)) {
				self._last = true;
			} else {
				self.shouldKeepAlive = true;
			}

		} else if (Pattern.matches(transferEncodingExpression, field)) {
			state.sentTransferEncodingHeader = true;
			
			if (Pattern.matches(http.chunkExpression, value)) 
				self.chunkedEncoding = true;
		} else if (Pattern.matches(contentLengthExpression, field)) {
			state.sentContentLengthHeader = true;
		} else if (Pattern.matches(dateExpression, field)) {
			state.sentDateHeader = true;
		} else if (Pattern.matches(expectExpression, field)) {
			state.sentExpect = true;
		}
	}

	public void setHeader(String name, List<String> value) throws Exception {
		if (!Util.zeroString(this._header)) {
			///throw new Error('Can\'t set headers after they are sent.');
			throw new Exception("Can\'t set headers after they are sent.");
		}

		String key = name.toLowerCase();
		this._headers = this._headers!=null ? this._headers : new Hashtable<String, List<String>>();
		this._headerNames = this._headerNames!=null ? this._headerNames : new Hashtable<String, String>();
		this._headers.put(key, value);
		this._headerNames.put(key, name);

		if (automaticHeaders.contains(key)) {
			this._removedHeader.put(key, false);
		}
	}
	public void setHeader(String name, String value) throws Exception {
		List<String> v = new ArrayList<String>(); v.add(value);
		
		setHeader(name, v);
	}
	
	public List<String> getHeader(String name) {
		if (null==this._headers) return null;

		String key = name.toLowerCase();
		return this._headers.containsKey(key) ? this._headers.get(key) : null;
	}


	public void removeHeader(String name) throws Exception {
		if (!Util.zeroString(this._header)) {
			///throw new Error('Can\'t remove headers after they are sent.');
			throw new Exception("Can\'t remove headers after they are sent.");
		}

		String key = name.toLowerCase();

		if (key == "date")
			this.sendDate = false;
		else if (automaticHeaders.contains(key))
			this._removedHeader.put(key, true);
		
		if (this._headers != null) {
			///delete this._headers[key];
			///delete this._headerNames[key];
			this._headers.remove(key);
			this._headerNames.remove(key);
		}
	}

	protected Map<String, List<String>> _renderHeaders() throws Exception {
		if (!Util.zeroString(this._header)) {
			///throw new Error('Can\'t render headers after they are sent to the client.');
			throw new Exception("Can\'t render headers after they are sent to the client.");
		}

		if (null==this._headers || this._headers.isEmpty()) return null;

		Map<String, List<String>> headers = new Hashtable<String, List<String>>();

		for (Entry<String, List<String>> entry : this._headers.entrySet())
			headers.put(this._headerNames.get(entry.getKey()), entry.getValue());

		return headers;
	}

	public boolean headersSent() {
		return !Util.zeroString(this._header); 
	}

	protected abstract void _implicitHeader() throws Exception;

	
	public boolean write(Object chunk, String encoding, WriteCB callback) throws Exception {
		if (Util.zeroString(this._header)) {
			this._implicitHeader();
		}
		
		Log.d(TAG, ".......... 0");

		if (!this._hasBody) {
			Log.d(TAG, "This type of response MUST NOT have a body. " +
					"Ignoring write() calls.");
			return true;
		}

		if (!Util.isString(chunk) && !Util.isBuffer(chunk)) {
			///throw new TypeError('first argument must be a string or Buffer');
			throw new Exception("first argument must be a string or Buffer");
		}
		
		Log.d(TAG, ".......... -1");

		// If we get an empty string or buffer, then just do nothing, and
		// signal the user to keep writing.
		if (Util.chunkLength(chunk) == 0) return true;

		int len;
		boolean ret;
		if (this.chunkedEncoding) {
			Log.d(TAG, ".......... 1");
			
			if (Util.isString(chunk) &&
				encoding != "hex" &&
				encoding != "base64" &&
				encoding != "binary") {
				Log.d(TAG, ".......... 2");
				
				///len = Buffer.byteLength(chunk, encoding);
				len = Util.stringByteLength((String) chunk, encoding);

				///chunk = len.toString(16) + CRLF + chunk + CRLF;
				chunk = Integer.toString(len, 16) + http.CRLF + chunk + http.CRLF;

				Log.d(TAG, "write _send: "+chunk.toString());
				
				ret = this._send(chunk, encoding, callback);
			} else {
				// buffer, or a non-toString-friendly encoding
				if (Util.isString(chunk))
					///len = Buffer.byteLength(chunk, encoding);
					len = Util.stringByteLength((String) chunk, encoding);
				else
					///len = chunk.length;
					len = Util.chunkLength(chunk);

				if (this.connection!=null && this.connection.corked()==0 ) {
					this.connection.cork();
					final AbstractSocket conn = this.connection;
					///process.nextTick(function connectionCork() {
					context.nextTick(new nextTickListener(){

						@Override
						public void onNextTick() throws Exception {
							if (conn != null)
								conn.uncork();					
						}

					});
				}

				///ByteBuffer crlf_buf = ByteBuffer.wrap("\r\n".getBytes("utf-8"));

				this._send(Integer.toString(len, 16), "utf-8"/*TBD..."binary"*/, null);
				this._send(ByteBuffer.wrap("\r\n".getBytes("utf-8")), null, null);
				this._send(chunk, encoding, null);
				ret = this._send(ByteBuffer.wrap("\r\n".getBytes("utf-8")), null, callback);
			}
		} else {
			Log.d(TAG, ".......... 3");

			ret = this._send(chunk, encoding, callback);
		}

		Log.d(TAG, "write ret = " + ret);
		return ret;
	}

	public void addTrailers(Map<String, String> headers) {
		this._trailer = "";

		for (Entry<String, String> entry : headers.entrySet())
			this._trailer += entry.getKey() + ": " + entry.getValue() + http.CRLF;
	}

	public boolean end(Object data, String encoding, final WriteCB callback) throws Exception {
		/*if (util.isFunction(data)) {
    callback = data;
    data = null;
  } else if (util.isFunction(encoding)) {
    callback = encoding;
    encoding = null;
  }*/

		if (data!=null && !Util.isString(data) && !Util.isBuffer(data)) {
			///throw new TypeError('first argument must be a string or Buffer');
			throw new Exception("first argument must be a string or Buffer");
		}

		if (this.finished) {
			return false;
		}

		final OutgoingMessage self = this;

		WriteCB finish = new WriteCB() {

			@Override
			public void writeDone(String error) throws Exception {
				// TODO Auto-generated method stub
				self.emit("finish");
			}

		};

		///if (util.isFunction(callback))
		if (null!=callback)
			this.once("finish", new Listener(){
				public void onEvent(Object data) throws Exception {
					callback.writeDone(null);
				}
			});

		if (Util.zeroString(this._header)) {
			this._implicitHeader();
		}

		if (data!=null && !this._hasBody) {
			Log.d(TAG, "This type of response MUST NOT have a body. " +
					"Ignoring data passed to end().");
			data = null;
		}

		if (this.connection!=null && data!=null)
			this.connection.cork();

		boolean ret;
		if (data!=null) {
			// Normal body write.
			ret = this.write(data, encoding, null);
		}

		if (this.chunkedEncoding) {
			ret = this._send("0\r\n" + this._trailer + "\r\n", "utf-8"/*TBD..."binary"*/, finish);
		} else {
			// Force a flush, HACK.
			ret = this._send("", "utf-8"/*TBD..."binary"*/, finish);
		}

		if (this.connection!=null && data!=null)
			this.connection.uncork();

		this.finished = true;

		// There is the first message on the outgoing queue, and we've sent
		// everything to the socket.
		Log.d(TAG, "outgoing message end.");
		if (this.output.size() == 0 && this.connection.get_httpMessage() == this) {
			this._finish();
		}

		return ret;
	}

	@Override
	public boolean writable() {
		if (this.connection != null)
			return this.connection.writable();
		else
			return false;
	}

	@Override
	public void writable(boolean writable) {
		if (this.connection != null)
			this.connection.writable(writable);
	}

	protected void _finish() throws Exception {
		assert(this.connection!=null);
		this.emit("prefinish");
	}

	// This logic is probably a bit confusing. Let me explain a bit:
	//
	// In both http servers and clients it is possible to queue up several
	// outgoing messages. This is easiest to imagine in the case of a client.
	// Take the following situation:
	//
	//    req1 = client.request('GET', '/');
	//    req2 = client.request('POST', '/');
	//
	// When the user does
	//
	//   req2.write('hello world\n');
	//
	// it's possible that the first request has not been completely flushed to
	// the socket yet. Thus the outgoing messages need to be prepared to queue
	// up data internally before sending it on further to the socket's queue.
	//
	// This function, outgoingFlush(), is called by both the AbstractServer and Client
	// to attempt to flush any pending messages out to the socket.
	protected void _flush() throws Exception {
		if (this.socket!=null && this.socket.writable()) {
			boolean ret = false;
			while (this.output.size() > 0) {
				///var data = this.output.shift();
				///var encoding = this.outputEncodings.shift();
				///var cb = this.outputCallbacks.shift();

				Object data = this.output.remove(0);
				String encoding = this.outputEncodings.remove(0);
				WriteCB cb = this.outputCallbacks.remove(0);

				ret = this.socket.write(data, encoding, cb);
				
				Log.d(TAG, "_flush "+data+"@"+encoding+",ret="+ret);
			}

			if (this.finished) {
				// This is a queue to the server or client to bring in the next this.
				this._finish();
			} else if (ret) {
				// This is necessary to prevent https from breaking
				this.emit("drain");
			}
		}
	}

	public void flush() throws Exception {
		if (Util.zeroString(this._header)) {
			// Force-flush the headers.
			this._implicitHeader();
			this._send("", "utf-8", null);
		}
	}

	/**
	 * @return the _last
	 */
	public boolean is_last() {
		return _last;
	}

	/**
	 * @param _last the _last to set
	 */
	public void set_last(boolean _last) {
		this._last = _last;
	}



}
