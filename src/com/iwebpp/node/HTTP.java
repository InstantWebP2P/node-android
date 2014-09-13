package com.iwebpp.node;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.iwebpp.node.TCP.Socket;

public final class HTTP {

	public static final Map<Integer, String> STATUS_CODES;

	static {
		// status codes 
		STATUS_CODES = new Hashtable<Integer, String>();

		STATUS_CODES.put(100, "Continue");
		STATUS_CODES.put(101, "Switching Protocols");
		STATUS_CODES.put(102, "Processing");                      

		STATUS_CODES.put(200, "OK");
		STATUS_CODES.put(201, "Created");
		STATUS_CODES.put(202, "Accepted");
		STATUS_CODES.put(203, "Non-Authoritative Information");
		STATUS_CODES.put(204, "No Content");
		STATUS_CODES.put(205, "Reset Content");
		STATUS_CODES.put(206, "Partial Content");
		STATUS_CODES.put(207, "Multi-Status");

		STATUS_CODES.put(300, "Multiple Choices");
		STATUS_CODES.put(301, "Moved Permanently");
		STATUS_CODES.put(302, "Moved Temporarily");
		STATUS_CODES.put(303, "See Other");
		STATUS_CODES.put(304, "Not Modified");
		STATUS_CODES.put(305, "Use Proxy");
		STATUS_CODES.put(307, "Temporary Redirect");
		STATUS_CODES.put(308, "Permanent Redirect");             

		STATUS_CODES.put(400, "Bad Request");
		STATUS_CODES.put(401, "Unauthorized");
		STATUS_CODES.put(402, "Payment Required");
		STATUS_CODES.put(403, "Forbidden");
		STATUS_CODES.put(404, "Not Found");
		STATUS_CODES.put(405, "Method Not Allowed");
		STATUS_CODES.put(406, "Not Acceptable");
		STATUS_CODES.put(407, "Proxy Authentication Required");
		STATUS_CODES.put(408, "Request Time-out");
		STATUS_CODES.put(409, "Conflict");
		STATUS_CODES.put(410, "Gone");
		STATUS_CODES.put(411, "Length Required");
		STATUS_CODES.put(412, "Precondition Failed");
		STATUS_CODES.put(413, "Request Entity Too Large");
		STATUS_CODES.put(414, "Request-URI Too Large");
		STATUS_CODES.put(415, "Unsupported Media Type");
		STATUS_CODES.put(416, "Requested Range Not Satisfiable");
		STATUS_CODES.put(417, "Expectation Failed");
		STATUS_CODES.put(418, "I\'m a teapot");
		STATUS_CODES.put(422, "Unprocessable Entity");
		STATUS_CODES.put(423, "Locked");
		STATUS_CODES.put(424, "Failed Dependency");
		STATUS_CODES.put(425, "Unordered Collection");
		STATUS_CODES.put(426, "Upgrade Required");
		STATUS_CODES.put(428, "Precondition Required");
		STATUS_CODES.put(429, "Too Many Requests");
		STATUS_CODES.put(431, "Request Header Fields Too Large");

		STATUS_CODES.put(500, "Internal Server Error");
		STATUS_CODES.put(501, "Not Implemented");
		STATUS_CODES.put(502, "Bad Gateway");
		STATUS_CODES.put(503, "Service Unavailable");
		STATUS_CODES.put(504, "Gateway Time-out");
		STATUS_CODES.put(505, "HTTP Version Not Supported");
		STATUS_CODES.put(506, "Variant Also Negotiates");
		STATUS_CODES.put(507, "Insufficient Storage");
		STATUS_CODES.put(509, "Bandwidth Limit Exceeded");
		STATUS_CODES.put(510, "Not Extended");
		STATUS_CODES.put(511, "Network Authentication Required");

		// globalAgent
		globalAgent = new Agent();


	}

	public static final class Server 
	extends EventEmitter2 {

		///server.listen(port, [hostname], [backlog], [callback])
		public int listen(
				int port, 
				String hostname, 
				int backlog, 
				ListeningCallback cb) throws Exception {
			
			
			return 0;
		}
		public static interface ListeningCallback {
			public void onListening() throws Exception;
		}
		
		public int close(final closeListener cb) throws Exception {
			if (cb != null) onClose(cb);
			
			return 0;
		}
		
		public int maxHeadersCount(int max) {
			return max;
			
		}
		
		///server.setTimeout(msecs, callback)
		///server.timeout
		
		// Event listeners
		public void onRequest(final requestListener cb) throws Exception {
			this.on("request", new Listener(){

				@Override
				public void onListen(Object raw) throws Exception {
                    request_response_t data = (request_response_t)raw;
                    
                    cb.onRequest(data.request, data.response);
				}
				
			});
		}
		public static interface requestListener {
			public void onRequest(IncomingMessage req, ServerResponse res) throws Exception;
		}
		
		public void onConnection(final connectionListener cb) throws Exception {
			this.on("connection", new Listener(){

				@Override
				public void onListen(Object raw) throws Exception {
					TCP.Socket data = (TCP.Socket)raw;
                    
                    cb.onConnection(data);
				}
				
			});
		}
		public static interface connectionListener {
			public void onConnection(TCP.Socket socket) throws Exception;
		}
		
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
		
		public void onCheckContinue(final checkContinueListener cb) throws Exception {
			this.on("checkContinue", new Listener(){

				@Override
				public void onListen(Object raw) throws Exception {
                    request_response_t data = (request_response_t)raw;
                    
                    cb.onCheckContinue(data.request, data.response);
				}
				
			});
		}
		public static interface checkContinueListener {
			public void onCheckContinue(IncomingMessage req, ServerResponse res) throws Exception;
		}
		
		public void onCheckContinue(final connectListener cb) throws Exception {
			this.on("connect", new Listener(){

				@Override
				public void onListen(Object raw) throws Exception {
					request_socket_head_t data = (request_socket_head_t)raw;
                    
                    cb.onConnect(data.request, data.socket, data.head);
				}
				
			});
		}
		public static interface connectListener {
			public void onConnect(IncomingMessage request, Socket socket, ByteBuffer head) throws Exception;
		}
		
		public void onUpgrade(final upgradeListener cb) throws Exception {
			this.on("upgrade", new Listener(){

				@Override
				public void onListen(Object raw) throws Exception {
					request_socket_head_t data = (request_socket_head_t)raw;
                    
                    cb.onUpgrade(data.request, data.socket, data.head);
				}
				
			});
		}
		public static interface upgradeListener {
			public void onUpgrade(IncomingMessage request, Socket socket, ByteBuffer head) throws Exception;
		}
		
		public void onClientError(final clientErrorListener cb) throws Exception {
			this.on("upgrade", new Listener(){

				@Override
				public void onListen(Object raw) throws Exception {
					exception_socket_t data = (exception_socket_t)raw;
                    
                    cb.onClientError(data.exception, data.socket);
				}
				
			});
		}
		public static interface clientErrorListener {
			public void onClientError(String exception, Socket socket) throws Exception;
		}
	
		// POJO beans
		private class request_response_t {
			IncomingMessage request;
			ServerResponse  response;
		}
		private class request_socket_head_t {
			IncomingMessage request;
			TCP.Socket      socket;
			ByteBuffer      head;
		}
		private class exception_socket_t {
			String     exception;
			TCP.Socket socket;
		}
	}
	
	
	public static final class ServerResponse 
	extends Writable2 {

		protected ServerResponse(NodeContext context, Options options) {
			super(context, options);
			// TODO Auto-generated constructor stub
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
		
		public int statusCode(int statusCode) {
			
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
		
	}

	public static final class Agent {

		public int maxSockets() {
			return 0;
			
		}
		
		public List<TCP.Socket> sockets() {
			return null;
			
		}
		
		public List<ClientRequest> requests() {
			return null;
			
		}
		
		
	}
	public static final Agent globalAgent;

	public static final class ClientRequest 
	extends Writable2 {

		protected ClientRequest(NodeContext context, Options options) {
			super(context, options);
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
				public void onListen(Object raw) throws Exception {      
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
				public void onListen(Object raw) throws Exception {      
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
				public void onListen(Object raw) throws Exception {      
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
				public void onListen(Object raw) throws Exception {      
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
				public void onListen(Object raw) throws Exception {                   
                    cb.onContinue();
				}
				
			});
		}
		public static interface continueListener {
			public void onContinue() throws Exception;
		}
		
		// POJO beans
		private class response_socket_head_t {
			IncomingMessage response;
			TCP.Socket      socket;
			ByteBuffer      head;
		}

	}
	
	/* Abstract base class for ServerRequest and ClientResponse. */
	public static abstract class IncomingMessage 
	extends Readable2 {
		private final static String TAG = "IncomingMessage";
		
		private Map<String, List<String>> headers;
		private Map<String, List<String>> trailers;
		private Socket socket;
		private Socket connection;
		private String httpVersion;
		private boolean complete;
		private List<String> rawHeaders;
		private List<String> rawTrailers;
		private boolean readable;
		private List<Object> _pendings;
		private int _pendingIndex;
		private String url;
		private String method;
		private int statusCode;
		private String statusMessage;
		private Socket client;
		private boolean _consuming;
		private boolean _dumped;

		protected IncomingMessage(NodeContext context, Options options, Socket socket) {
			super(context, options);
			
			 // XXX This implementation is kind of all over the place
			  // When the parser emits body chunks, they go in this list.
			  // _read() pulls them out, and when it finds EOF, it ends.

			  this.socket = socket;
			  this.connection = socket;

			  this.httpVersion = null;
			  this.complete = false;
			  this.headers = new Hashtable<String, List<String>>();
			  this.rawHeaders = new ArrayList<String>();
			  this.trailers = new Hashtable<String, List<String>>();
			  this.rawTrailers = new ArrayList<String>();

			  this.readable = true;

			  this._pendings = new ArrayList<Object>();
			  this._pendingIndex = 0;

			  // request (server) only
			  this.url = "";
			  this.method = null;

			  // response (client) only
			  this.statusCode = -1;
			  this.statusMessage = null;
			  this.client = this.socket;

			  // flag for backwards compatibility grossness.
			  this._consuming = false;

			  // flag for when we decide that this message cannot possibly be
			  // read by the user, so there's no point continuing to handle it.
			  this._dumped = false;
			
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

		private void _addHeaderLines(List<String> headers, int n) {
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

				if (this.complete) {
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
					// https://mxr.mozilla.org/mozilla/source/netwerk/protocol/http/src/nsHttpHeaderArray.cpp
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
		private void _dump() throws Exception {
			if (!this._dumped) {
				this._dumped = true;
				this.resume();
			}
		}
		
		protected static void readStart(TCP.Socket socket) throws Exception {
			///if (socket && !socket._paused && socket.readable)
			if (socket!=null && !socket._paused && socket.readable())
				socket.resume();
		}

		protected static void readStop(TCP.Socket socket) throws Exception {
			if (socket != null)
				socket.pause();
		}
		
		public String httpVersion() {
			return this.httpVersion;
		}
		
		public Map<String, List<String>> headers() {
			return this.headers;
		}
		
		public Map<String, List<String>> trailers() {
			return this.trailers;
		}
		
		// message.setTimeout(msecs, callback)
		
		public String method() {
			return this.method;
			
		}
		
		public String url() {
			return this.url;
			
		}
		
		public int statusCode() {
			return this.statusCode;
			
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
				public void onListen(Object raw) throws Exception {                   
                    cb.onClose();
				}
				
			});
		}
		public static interface closeListener {
			public void onClose() throws Exception;
		}
		
		// POJO beans
		
	}

	// http.createServer([requestListener])
	public static final int createServer(Server.requestListener onreq) {

		return 0;
	}

	// http.request(options, [callback])
	public static final int request(ReqOptions options, ClientRequest.responseListener onres) {

		return 0;
	}
	public static final int request(String url, ClientRequest.responseListener onres) {

		return 0;
	}

	// http.get(options, [callback])
	public static final int get(ReqOptions options, ClientRequest.responseListener onres) {

		return 0;
	}
	public static final int get(String url, ClientRequest.responseListener onres) {

		return 0;
	}

	public static final class ReqOptions {

	}
	

}
