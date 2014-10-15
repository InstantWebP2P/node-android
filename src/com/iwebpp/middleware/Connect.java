package com.iwebpp.middleware;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;

public class Connect 
extends EventEmitter2 
implements requestListener{

	private static final String TAG = "Connect";

	private Map<String, List<requestListener>> stack;

	public Connect() {
		stack = new Hashtable<String, List<requestListener>>();
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
		Log.d(TAG, "added request cb:"+cb+" on "+path);
		
		if (!stack.containsKey(path))
			stack.put(path, new LinkedList<requestListener>());
		
		stack.get(path).add(cb);
		
		this.emit("add/"+path, cb);
		
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
		Log.d(TAG, "removed request cb:"+cb+" on "+path);
		
		if (stack.containsKey(path) && stack.get(path).contains(cb))
			stack.get(path).remove(cb);
				
		this.emit("del/"+path, cb);
		
		return this;
	}
	
	/*
	 * @description 
	 *   take out all callback on path
	 * */
	public Connect unuse(String path) throws Exception {
		Log.d(TAG, "removed request on "+path);
		
		if (stack.containsKey(path))
			stack.get(path).clear();
		
		this.emit("del/"+path+"/all");

		return this;
	}
	
	/*
	 * @description 
	 *   take out all callback on all path
	 * */
	public Connect unuse() throws Exception {
		Log.d(TAG, "removed all requests");
		
		stack.clear();
		
		this.emit("del/any/all");

		return this;
	}
	
	@Override
	public void onRequest(IncomingMessage req, ServerResponse res)
			throws Exception {
		String path = req.url();

		Log.d(TAG, "request on "+path);

		if (stack.containsKey(path)) {
			// run stack until response header sent out
			boolean complete = false;
			for (requestListener cb : stack.get(path)) {
				cb.onRequest(req, res);
				
				// check if res.sent
				if (res.headersSent()) {
				    Log.d(TAG, "response sent done, stop stack");
				    
				    complete = true;
				    break;
				}
			}
			
			// check if completed
			if (!complete) {
				// flush out
				Log.d(TAG, "flush out");
				res.end(null, null, null);
			}
		} else {
			Log.w(TAG, "No request handler on "+path);
			
			res.writeHead(404);
			res.end("No Service", "utf-8", null);
		}
	}

}
