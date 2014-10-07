// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.http;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.Url;
import com.iwebpp.node.Url.UrlObj;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.NodeContext;


public final class http {

	private static final String TAG = "http";
	
	public static final Map<Integer, String> STATUS_CODES;

	static {
		// STATUS CODES
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

		STATUS_CODES.put(500, "Internal AbstractServer Error");
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
		///globalAgent = new Agent();

	}

	///public static final Agent globalAgent;
	public static final String CRLF = "\r\n";

	public static final String chunkExpression = "chunk";
	public static final String continueExpression = "100-continue";


	public static void httpSocketSetup(final AbstractSocket socket) throws Exception {
		Listener ondrain = new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				if (socket.get_httpMessage()!=null) socket.get_httpMessage().emit("drain");

			}

		};
		
		socket.removeListener("drain"/*, ondrain*/);
		socket.on("drain", ondrain);
	}

	// POJO beans
	public static class request_response_b {
		/**
		 * @return the request
		 */
		public IncomingMessage getRequest() {
			return request;
		}
		/**
		 * @return the response
		 */
		public ServerResponse getResponse() {
			return response;
		}
		private IncomingMessage request;
		private ServerResponse  response;
		
		public request_response_b(IncomingMessage request, ServerResponse response) {
			this.request  = request;
			this.response = response;
		}
		@SuppressWarnings("unused")
		private request_response_b(){}
	}
	
	public static class request_socket_head_b {
		/**
		 * @return the request
		 */
		public IncomingMessage getRequest() {
			return request;
		}
		/**
		 * @return the socket
		 */
		public AbstractSocket getSocket() {
			return socket;
		}
		/**
		 * @return the head
		 */
		public ByteBuffer getHead() {
			return head;
		}
		private IncomingMessage request;
		private AbstractSocket  socket;
		private ByteBuffer      head;
		
		public request_socket_head_b(IncomingMessage request, AbstractSocket socket, ByteBuffer head) {
			this.request = request;
			this.socket  = socket;
			this.head    = head;
		}
		@SuppressWarnings("unused")
		private request_socket_head_b(){}
	}
	
	public static class exception_socket_b {
		/**
		 * @return the exception
		 */
		public String getException() {
			return exception;
		}
		/**
		 * @return the socket
		 */
		public AbstractSocket getSocket() {
			return socket;
		}
		private String         exception;
		private AbstractSocket socket;
		
		public exception_socket_b(String exception, AbstractSocket socket) {
			this.exception = exception;
			this.socket    = socket;
		}
		@SuppressWarnings("unused")
		private exception_socket_b(){}
	}
	
	public static class response_socket_head_b {
		/**
		 * @return the response
		 */
		public IncomingMessage getResponse() {
			return response;
		}
		/**
		 * @return the socket
		 */
		public AbstractSocket getSocket() {
			return socket;
		}
		/**
		 * @return the head
		 */
		public ByteBuffer getHead() {
			return head;
		}
		private IncomingMessage response;
		private AbstractSocket  socket;
		private ByteBuffer      head;
		
		public response_socket_head_b(IncomingMessage response, AbstractSocket socket, ByteBuffer head) {
			this.response = response;
			this.socket   = socket;
			this.head     = head;
		}
		@SuppressWarnings("unused")
		private response_socket_head_b(){}
	}
	
	// http.createServer([requestListener])
	public static HttpServer createServer(
			NodeContext ctx, 
			HttpServer.requestListener onreq) throws Exception {
		  return new HttpServer(ctx, onreq);
	}

	// http.request(options, [callback])
	public static ClientRequest request(
			NodeContext ctx, 
			ReqOptions options, 
			ClientRequest.responseListener onres) throws Exception {
		Log.d(TAG, "http request");
		
		options.httpp = false;

		return new ClientRequest(ctx, options, onres);
	}
	// TBD... parser ReqOptions from URL
	public static ClientRequest request(
			NodeContext ctx, 
			String url,
			ClientRequest.responseListener onres) throws Exception {
		UrlObj obj = Url.parse(url);
		
		ReqOptions options = new ReqOptions();
		
		options.protocol = obj.protocol;
		
	        options.auth = obj.auth;
		
		options.hostname = obj.hostname;
		    options.port = obj.port;
		    options.host = obj.host;
		
		    options.path = obj.path;
		    
		return request(ctx, options, onres);
	}

	// http.get(options, [callback])
	public static ClientRequest get(
			NodeContext ctx, 
			ReqOptions options, 
			ClientRequest.responseListener onres) throws Exception {
		
		// GET method
		options.method = "GET";
		options.httpp  = false;
		
		ClientRequest req = request(ctx, options, onres);
		req.end(null, null, null);
		return req;
	}
	public static ClientRequest get(
			NodeContext ctx, 
			String url,
			ClientRequest.responseListener onres) throws Exception {

		UrlObj obj = Url.parse(url);
		
		ReqOptions options = new ReqOptions();
		
		options.protocol = obj.protocol;
		
	        options.auth = obj.auth;
		
		options.hostname = obj.hostname;
		    options.port = obj.port;
		    options.host = obj.host;
		
		    options.path = obj.path;
		    
		return get(ctx, options, onres);
	}

}
