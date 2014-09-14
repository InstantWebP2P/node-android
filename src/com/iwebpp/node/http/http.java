package com.iwebpp.node.http;

import java.util.Hashtable;
import java.util.Map;


public final class http {

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

	public static final Agent globalAgent;

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

}
