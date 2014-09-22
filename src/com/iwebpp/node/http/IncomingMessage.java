package com.iwebpp.node.http;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.net.TCP;
import com.iwebpp.node.net.TCP.Socket;
import com.iwebpp.node.stream.Readable2;

public class IncomingMessage 
extends Readable2 {

	private final static String TAG = "IncomingMessage";

	public Map<String, List<String>> headers;
	public Map<String, List<String>> trailers;
	private Socket socket;
	private String httpVersion;
	private boolean complete;
	private List<String> rawHeaders;
	private List<String> rawTrailers;
	private List<Object> _pendings;
	private String url;
	private String method;
	private int statusCode;
	private String statusMessage;
	private boolean _consuming;
	private boolean _dumped;
	private boolean upgrade;

	public ClientRequest  req;
	public ServerResponse res;

	private int httpVersionMajor;

	private int httpVersionMinor;

	public IncomingParser parser;

	/**
	 * @return the upgrade
	 */
	public boolean isUpgrade() {
		return upgrade;
	}
	/**
	 * @param upgrade the upgrade to set
	 */
	public void setUpgrade(boolean upgrade) {
		this.upgrade = upgrade;
	}
	public IncomingMessage(NodeContext context, Socket socket) {
		super(context, new Options(-1, null, false, "utf8", false));
		
		Log.d(TAG, "start ...");

		// XXX This implementation is kind of all over the place
		// When the parser emits body chunks, they go in this list.
		// _read() pulls them out, and when it finds EOF, it ends.

		this.socket = socket;
		this.httpVersion = null;
		this.setComplete(false);
		this.headers = new Hashtable<String, List<String>>();
		this.rawHeaders = new ArrayList<String>();
		this.trailers = new Hashtable<String, List<String>>();
		this.rawTrailers = new ArrayList<String>();

		this.readable(true);

		this.set_pendings(new ArrayList<Object>());
		// request (server) only
		this.url = "";
		this.setMethod(null);

		// response (client) only
		this.setStatusCode(-1);
		this.setStatusMessage(null);
		// flag for backwards compatibility grossness.
		this._consuming = false;

		// flag for when we decide that this message cannot possibly be
		// read by the user, so there's no point continuing to handle it.
		this.set_dumped(false);

	}
	private IncomingMessage() {super(null, null);}

	@Override
	protected void _read(int size) throws Exception {
		// We actually do almost nothing here, because the parserOnBody
		// function fills up our internal buffer directly.  However, we
		// do need to unpause the underlying socket so that it flows.
		if (this.socket.readable())
			readStart(this.socket);			
	}

	// It's possible that the socket will be destroyed, and removed from
	// any messages, before ever calling this.  In that case, just skip
	// it, since something else is destroying this connection anyway.
	public void destroy(String error) throws Exception {
		if (this.socket != null)
			this.socket.destroy(error);
	}

	protected void _addHeaderLines(List<String> headers, int n) {
		/*if (headers && headers.length) {
			var raw, dest;
			if (this.complete) {
				raw = this.rawTrailers;
				dest = this.trailers;
			} else {
				raw = this.rawHeaders;
				dest = this.headers;
			}
			raw.push.apply(raw, headers);

			for (var i = 0; i < n; i += 2) {
				var k = headers[i];
				var v = headers[i + 1];
				this._addHeaderLine(k, v, dest);
			}
		}*/

		if (headers!=null && headers.size()>0) {
			///assert(headers.size() == n);
			List<String> raw;
			Map<String, List<String>> dest;

			if (this.isComplete()) {
				raw = this.rawTrailers;
				dest = this.trailers;
			} else {
				raw = this.rawHeaders;
				dest = this.headers;
			}
			raw.addAll(headers);

			for (int i = 0; i < headers.size(); i += 2) {
				String k = headers.get(i);
				String v = headers.get(i+1);

				this._addHeaderLine(k, v, dest);
			}

		}
	}

	// Add the given (field, value) pair to the message
	//
	// Per RFC2616, section 4.2 it is acceptable to join multiple instances of the
	// same header with a ', ' if the header in question supports specification of
	// multiple values this way. If not, we declare the first instance the winner
	// and drop the second. Extended header fields (those beginning with 'x-') are
	// always joined.
	///IncomingMessage.prototype._addHeaderLine = function(field, value, dest) {
	private void _addHeaderLine(String field, String value,
			Map<String, List<String>> dest) {
		field = field.toLowerCase();

		///switch (field) {
		// Array headers:
		///case "set-cookie":
		if (field == "set-cookie") {
			///if (!util.isUndefined(dest[field])) {
			if (dest.containsKey(field)) {
				///dest[field].push(value);
				dest.get(field).add(value);
			} else {
				///dest[field] = [value];
				dest.put(field, new ArrayList<String>());
				dest.get(field).add(value);
			}
			///break;
		} else if (
				// list is taken from:
				// https://mxr.mozilla.org/mozilla/source/netwerk/protocol/Http/src/nsHttpHeaderArray.cpp
				///case "content-type":
				field == "content-type" || 
				///case "content-length":
				field == "content-length" || 
				//case "user-agent":
				field == "user-agent" || 
				//case "referer":
				field == "referer" || 
				//case "host":
				field == "host" || 
				///case "authorization":
				field == "authorization" || 
				///case "proxy-authorization":
				field == "proxy-authorization" || 			
				///case "if-modified-since":
				field == "if-modified-since" || 		
				//case "if-unmodified-since":
				field == "if-unmodified-since" || 		
				///case "from":
				field == "from" || 		
				///case "location":
				field == "location" || 		
				///case "max-forwards":
				field == "max-forwards"	
				) {
			// drop duplicates
			///if (util.isUndefined(dest[field]))
			if (!dest.containsKey(field)) {
				///dest[field] = value;
				dest.put(field, new ArrayList<String>());
				dest.get(field).add(value);
			}
			///break;

			///default:
		} else {
			// make comma-separated list
			///if (!util.isUndefined(dest[field]))
			if (dest.containsKey(field)) {
				///dest[field] += ", " + value;
				String nstr = dest.get(field).get(0) + ", " + value;
				dest.get(field).set(0, nstr);
			} else {
				///dest[field] = value;
				dest.put(field, new ArrayList<String>());
				dest.get(field).add(value);
			}
		}
	}

	// Call this instead of resume() if we want to just
	// dump all the data to /dev/null
	///IncomingMessage.prototype._dump = function() {
	public void _dump() throws Exception {
		if (!this.is_dumped()) {
			this.set_dumped(true);
			this.resume();
		}
	}

	public static void readStart(TCP.Socket socket) throws Exception {
		///if (socket && !socket._paused && socket.readable)
		if (socket!=null && !socket._paused && socket.readable())
			socket.resume();
	}

	public static void readStop(TCP.Socket socket) throws Exception {
		if (socket != null)
			socket.pause();
	}

	public String httpVersion() {
		return this.httpVersion;
	}
	public void httpVersion(String httpVersion) {
		this.httpVersion = httpVersion;
	}

	public Map<String, List<String>> headers() {
		return this.headers;
	}

	public Map<String, List<String>> trailers() {
		return this.trailers;
	}

	// message.setTimeout(msecs, callback)

	public String method() {
		return this.getMethod();

	}
	public void method(String method) {
		this.setMethod(method);
	}

	public String url() {
		return this.url;
	}
	public void url(String url) {
		this.url = url;
	}

	public int statusCode() {
		return this.getStatusCode();
	}
	public void statusCode(int statusCode) {
		this.setStatusCode(statusCode);
	}

	public TCP.Socket socket() {
		return this.socket;
	}


	public Object read(int n) throws Exception {
		this._consuming = true;
		return super.read(n);
	}

	// Event listeners
	public void onClose(final closeListener cb) throws Exception {
		this.on("close", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {                   
				cb.onClose();
			}

		});
	}
	/**
	 * @return the _dumped
	 */
	public boolean is_dumped() {
		return _dumped;
	}
	/**
	 * @param _dumped the _dumped to set
	 */
	public void set_dumped(boolean _dumped) {
		this._dumped = _dumped;
	}
	/**
	 * @return the complete
	 */
	public boolean isComplete() {
		return complete;
	}
	/**
	 * @param complete the complete to set
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}
	/**
	 * @return the _pendings
	 */
	public List<Object> get_pendings() {
		return _pendings;
	}
	/**
	 * @param _pendings the _pendings to set
	 */
	public void set_pendings(List<Object> _pendings) {
		this._pendings = _pendings;
	}
	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}
	/**
	 * @param method the method to set
	 */
	public void setMethod(String method) {
		this.method = method;
	}
	/**
	 * @return the statusCode
	 */
	public int getStatusCode() {
		return statusCode;
	}
	/**
	 * @param statusCode the statusCode to set
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	/**
	 * @return the statusMessage
	 */
	public String getStatusMessage() {
		return statusMessage;
	}
	/**
	 * @param statusMessage the statusMessage to set
	 */
	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}
	/**
	 * @return the _consuming
	 */
	public boolean is_consuming() {
		return _consuming;
	}
	/**
	 * @param _consuming the _consuming to set
	 */
	public void set_consuming(boolean _consuming) {
		this._consuming = _consuming;
	}
	/**
	 * @return the httpVersionMajor
	 */
	public int getHttpVersionMajor() {
		return httpVersionMajor;
	}
	/**
	 * @param httpVersionMajor the httpVersionMajor to set
	 */
	public void setHttpVersionMajor(int httpVersionMajor) {
		this.httpVersionMajor = httpVersionMajor;
	}
	/**
	 * @return the httpVersionMinor
	 */
	public int getHttpVersionMinor() {
		return httpVersionMinor;
	}
	/**
	 * @param httpVersionMinor the httpVersionMinor to set
	 */
	public void setHttpVersionMinor(int httpVersionMinor) {
		this.httpVersionMinor = httpVersionMinor;
	}
	public static interface closeListener {
		public void onClose() throws Exception;
	}

	// POJO beans

}
