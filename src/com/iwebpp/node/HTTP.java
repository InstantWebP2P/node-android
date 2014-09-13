package com.iwebpp.node;

import java.nio.ByteBuffer;
import java.util.Hashtable;
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
			this.on("socket", new Listener(){

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
			this.on("socket", new Listener(){

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
			this.on("close", new Listener(){

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

	public static final class IncomingMessage 
	extends Readable2 {

		protected IncomingMessage(NodeContext context, Options options) {
			super(context, options);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void _read(int size) throws Exception {
			// TODO Auto-generated method stub
			
		}

		// Event listeners
		
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
