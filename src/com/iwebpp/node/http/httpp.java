package com.iwebpp.node.http;

import com.iwebpp.node.NodeContext;

public final class httpp {

	// httpp.createServer([requestListener])
	public static HttppServer createServer(
			NodeContext ctx, 
			HttppServer.requestListener onreq) throws Exception {
		  return new HttppServer(ctx, onreq);
	}

	// httpp.request(options, [callback])
	public static ClientRequest request(
			NodeContext ctx, 
			ReqOptions options, 
			ClientRequest.responseListener onres) throws Exception {
		options.httpp = true;

		return new ClientRequest(ctx, options, onres);
	}
	// TBD... parser ReqOptions from URL
	public static ClientRequest request(
			NodeContext ctx, 
			String url,
			ClientRequest.responseListener onres) throws Exception {

		return null;
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

		return null;
	}

}
