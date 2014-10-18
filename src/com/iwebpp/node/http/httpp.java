package com.iwebpp.node.http;


import com.iwebpp.SimpleDebug;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Url;
import com.iwebpp.node.Url.UrlObj;

public final class httpp extends SimpleDebug {
	private static final String TAG = "httpp";

	// httpp.createServer([requestListener])
	public static HttppServer createServer(
			NodeContext ctx, 
			HttpServer.requestListener onreq) throws Exception {
		  return new HttppServer(ctx, onreq);
	}

	// httpp.request(options, [callback])
	public static ClientRequest request(
			NodeContext ctx, 
			ReqOptions options, 
			ClientRequest.responseListener onres) throws Exception {
		debug(TAG, "httpp request");

		options.httpp = true;

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

	// httpp.get(options, [callback])
	public static ClientRequest get(
			NodeContext ctx, 
			ReqOptions options, 
			ClientRequest.responseListener onres) throws Exception {
		
		// GET method
		options.method = "GET";
		options.httpp  = true;
		
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
