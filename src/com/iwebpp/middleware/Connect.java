package com.iwebpp.middleware;

import java.util.LinkedList;
import java.util.List;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;

public class Connect 
extends EventEmitter2 
implements requestListener{

	private static final String TAG = "Connect";

	private String parent;
	
	private class stack_b {
		private String path;
		private requestListener cb;
		
		protected stack_b(String path, requestListener cb) {
			this.path = path;
			this.cb = cb;
		}
	}
	
	private List<stack_b> stack;

	public Connect() {
		parent = null;
		stack = new LinkedList<stack_b>();
	}
	
	public Connect(String path) {
		parent = path;
		stack = new LinkedList<stack_b>();
	}
	
	public Connect setParent(String path) {
		parent = path;
		return this;
	}
	public String getParent() {
		return parent!=null? parent : "";
	}
	
	/*
	 * @description 
	 *   append callback on default path /
	 * */
	public Connect use(requestListener cb) throws Exception {
		return use("/", cb);
	}
	
	/*
	 * @description 
	 *   append callback on path
	 * */
	public Connect use(String path, requestListener cb) throws Exception {
		debug(TAG, "added request cb:"+cb+" on "+path);
		
		// normalize
		if (path == null)
			path = "/";
		else if (path.charAt(0) != '/')
			path = "/" + path;
		
		// queue request handler
		stack.add(new stack_b(path, cb));
		
		this.emit("add:"+path, cb);
		
		return this;
	}

	/*
	 * @description 
	 *   take out callback on default path /
	 * */
	public Connect unuse(requestListener cb) throws Exception {
		return unuse("/", cb);
	}
	
	/*
	 * @description 
	 *   take out callback on path
	 * */
	public Connect unuse(String path, requestListener cb) throws Exception {
		debug(TAG, "removed request cb:"+cb+" on "+path);

		// normalize
		if (path == null)
			path = "/";
		else if (path.charAt(0) != '/')
			path = "/" + path;

		// cost operation
		for (stack_b b : stack) 
			if (b.path.equalsIgnoreCase(path) && b.cb == cb)
				stack.remove(b);

		this.emit("del:"+path, cb);

		return this;
	}
	
	/*
	 * @description 
	 *   take out all callback on path
	 * */
	public Connect unuse(String path) throws Exception {
		debug(TAG, "removed request on "+path);

		// normalize
		if (path == null)
			path = "/";
		else if (path.charAt(0) != '/')
			path = "/" + path;

		// cost operation
		for (stack_b b : stack) 
			if (b.path.equalsIgnoreCase(path))
				stack.remove(b);

		this.emit("del:"+path+":all");

		return this;
	}
	
	/*
	 * @description 
	 *   take out all callback on all path
	 * */
	public Connect unuse() throws Exception {
		debug(TAG, "removed all requests");
		
		stack.clear();
		
		this.emit("del:/any:all");

		return this;
	}
	
	@Override
	public void onRequest(IncomingMessage req, ServerResponse res)
			throws Exception {
		String path = req.url();

		debug(TAG, "request on "+path);

		// check if embedded stack
		if (parent != null && !parent.equals("/")) {
			path = path.substring(parent.length());
			debug(TAG, "child path: "+path);
		}

		// run stack until response header sent out
		for (stack_b b : stack) {
			// check absolute path
			if (b.path.equalsIgnoreCase(path)) {
				debug(TAG, "absolute path handle");
				b.cb.onRequest(req, res);
			}
			
			// check if res.sent
			if (res.headersSent()) {
				debug(TAG, "absolute response sent done, stop stack");
				break;
			}
			
			
			// check embedded path
			if (b.cb instanceof Connect) {
				debug(TAG, "embedded path handle");
				
				Connect embedded = (Connect)b.cb;
				// set parent path
				String ppath = null;
				if (!b.path.equals("/")) ppath = b.path;
				if (parent!=null && !parent.equals("/")) ppath = ppath!=null ? parent+ppath : parent;
				embedded.setParent(ppath);
				
				embedded.onRequest(req, res);
			}

			// check if res.sent
			if (res.headersSent()) {
				debug(TAG, "embedded response sent done, stop stack");
				break;
			}
		}
	}

}
