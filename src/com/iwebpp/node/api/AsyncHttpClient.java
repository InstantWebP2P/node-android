// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.api;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.libuvpp.handles.TimerHandle;
import com.iwebpp.node.Url;
import com.iwebpp.node.NodeContext.TimeoutListener;
import com.iwebpp.node.Url.UrlObj;
import com.iwebpp.node.Util;
import com.iwebpp.node.http.ClientRequest;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ReqOptions;
import com.iwebpp.node.http.http;

/*
 * @description
 *   AsyncHttpClient can be run from UI thread directly
 * */
public final class AsyncHttpClient {

	private static final String TAG = "AsyncHttpClient";

	private static class HttpClient extends SimpleApi {
		// request content
		private String method;

		private String url;

		private Map<String, String> headers;
		
		private http_content_b data;

		// response content
		private IncomingMessage response;

		private List<Object> body;

		// response timeout
		private int resp_timeout; // ms
		private TimerHandle resp_tmohdl;
		
		// content timeout
		private int cont_timeout; // ms
		private TimerHandle cont_tmohdl;
		
		protected HttpClient(
				String method,
				String url,
				Map<String, String> headers, 
				http_content_b data,
				final HttpClientCallback cb) {
			this(method, url, headers, data, cb, 2000, -1);
		}
		protected HttpClient(
				String method,
				String url,
				Map<String, String> headers, 
				http_content_b data,
				final HttpClientCallback cb,
				int resp_timeout) {
			this(method, url, headers, data, cb, resp_timeout, -1);
		}
		protected HttpClient(
				String method,
				String url,
				Map<String, String> headers, 
				http_content_b data,
				final HttpClientCallback cb,
				int resp_timeout,
				int cont_timeout) {
			this.method = method;
			this.url = url;

			// common headers
			this.headers = new Hashtable<String, String>();

			if (method.equalsIgnoreCase("get")) {
				this.headers.put("Accept", "text/plain,text/html,application/json,application/javascript");
				this.headers.put("Accept-Charset", "utf-8");
				this.headers.put("Accept-Encoding", "deflate");
				this.headers.put("Cache-Control", "no-cache");
			} else if (method.equalsIgnoreCase("put")) {
				this.headers.put("", "");
			} else if (method.equalsIgnoreCase("post")) {
				this.headers.put("", "");
			} else if (method.equalsIgnoreCase("delete")) {
				this.headers.put("", "");
			} else if (method.equalsIgnoreCase("get")) {
				this.headers.put("", "");
			}

			// user headers
			if (headers != null) this.headers.putAll(headers);

			// user data
			this.data = data;

			// default response timeout 2s
			this.resp_timeout = resp_timeout>0? resp_timeout : 2000;

			// default content timeout unlimited
			this.cont_timeout = cont_timeout>0? cont_timeout : -1;

			// response body
			this.body = new LinkedList<Object>();

			// handle response in UI thread
			this.once("completed", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					Map<String, String> headers = new Hashtable<String, String>();
					for (Map.Entry<String, List<String>> el : response.headers().entrySet())
						headers.put(el.getKey(), el.getValue().get(0));

					// parse body with contentType
					String ct = headers.get("content-type");

					// text body
					if (ct.contains("text/") || 
					    ct.equalsIgnoreCase("application/json") ||
					    ct.equalsIgnoreCase("application/javascript")) {
						String ret = "";
						for (Object str : body)
							ret += str.toString();

						cb.onResponse(null, response.statusCode(), headers, new http_content_b(ct, ret));		
					} else {
						ByteBuffer ret = Util.concatByteBuffer(body, 0);
						cb.onResponse(null, response.statusCode(), headers, new http_content_b(ct, ret));		
					}
				}
				
			});	
			this.once("resp_timeout", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					HttpClient.this.removeListener("completed");
					
					cb.onResponse("Operation resp_timeout", -1, null, null);					
				}

			});	
			this.once("cont_timeout", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					HttpClient.this.removeListener("completed");

					cb.onResponse("Operation cont_timeout", -1, null, null);					
				}

			});	
			this.once("error", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					HttpClient.this.removeListener("completed");

					cb.onResponse("Operation error:"+data!=null? data.toString() : "", -1, null, null);					
				}

			});	
		}
		// clear response timeout
		private void clearRespTimeout() {
			if (resp_timeout>0 && resp_tmohdl!=null)
				resp_tmohdl.close();
		}
		// clear content timeout
		private void clearContTimeout() {
			if (cont_timeout>0 && cont_tmohdl!=null)
				cont_tmohdl.close();
		}
				
		@Override
		public void content() throws Exception {
			ReqOptions options = new ReqOptions();

			// request method
			options.method = method;

			// parse URL
			UrlObj obj = Url.parse(url);
						
			options.protocol = obj.protocol;
			
		        options.auth = obj.auth;
		        
			options.hostname = obj.hostname;
			    options.port = obj.port;
			    options.host = obj.host;
			
			    options.path = obj.path;

			// set headers
			for (Map.Entry<String, String> el : headers.entrySet())
				options.setHeader(el.getKey(), el.getValue());

			// make http request with resp_timeout
			if (resp_timeout > 0) 
				this.resp_tmohdl = getNodeContext().setTimeout(new TimeoutListener(){

					@Override
					public void onTimeout() throws Exception {
						HttpClient.this.emit("resp_timeout");
					}

				}, resp_timeout);
			// make http request with cont_timeout
			if (cont_timeout > 0) 
				this.cont_tmohdl = getNodeContext().setTimeout(new TimeoutListener(){

					@Override
					public void onTimeout() throws Exception {
						HttpClient.this.emit("cont_timeout");
					}

				}, cont_timeout);
						
			ClientRequest req = http.request(getNodeContext(), options, new ClientRequest.responseListener(){

				@Override
				public void onResponse(IncomingMessage res) throws Exception {
					// clear response timeout
					clearRespTimeout();
					
					Log.d(TAG, "got response: "+res.statusCode()+", headers:"+res.headers());

					// response
					response = res;

					// check Content-Type: string or binary data
					if (res.headers().containsKey("content-type")) {
						String ct = res.headers().get("content-type").get(0);
						
						if (ct.contains("text/") || 
					  	    ct.equalsIgnoreCase("application/json") ||
						    ct.equalsIgnoreCase("application/javascript"))
							res.setEncoding("utf-8");
					} else {
						HttpClient.this.emit("error", "response miss content-type header");
						return;
					}

					// body
					res.on("data", new Listener(){

						@Override
						public void onEvent(Object data) throws Exception {
							Log.d(TAG, "response got data:\n"+data);

							body.add(data);
						}

					});
					res.on("end", new Listener(){

						@Override
						public void onEvent(Object data) throws Exception {
							// clear content timeout
							clearContTimeout();
							
							Log.d(TAG, "response got end\n");

							if (data!=null) body.add(data);

							HttpClient.this.emit("completed");
						}

					});
					res.on("error", new Listener(){

						@Override
						public void onEvent(Object error) throws Exception {
							// clear content timeout
							clearContTimeout();
							
							Log.d(TAG, "response got error:\n"+error);
							
							HttpClient.this.emit("error", "request error:"+error);
						}

					});
				}

			});
			
			if (data!=null) req.write(data.getContent(), data.getEncoding());
			req.end();
		}

	}
	public interface HttpClientCallback {
		void onResponse(String error, int statusCode, Map<String, String> headers, http_content_b content) throws Exception;
	}
	// java beans
	public static class http_content_b {
		private String type;
		private Object content;

		public http_content_b(String type, Object content) {
			this.type = type;
			this.content = content;
		}
		@SuppressWarnings("unused")
		private http_content_b() {}

		public String getType() {
			return this.type;
		}
		public Object getContent(){
			return this.content;
		}

		// Only support utf-8 string for now
		public boolean isString() {
			return content instanceof String;
		}
		public String getEncoding() {
			return isString() ? "utf-8" : null;
		}
	} 

	// HEAD
	public static void head(String url, HttpClientCallback cb) throws Exception {
		new HttpClient("HEAD", url, null, null, cb).execute();
	}

	// GET
	public static void get(String url, HttpClientCallback cb) throws Exception {
		new HttpClient("GET", url, null, null, cb).execute();
	}

	// PUT
	public static void put(String url, http_content_b data, HttpClientCallback cb) throws Exception {
		new HttpClient("PUT", url, null, data, cb, 2000).execute();
	}

	// POST
	public static void post(String url, http_content_b data, HttpClientCallback cb) throws Exception {
		new HttpClient("POST", url, null, data, cb, 2000).execute();
	}

	// DELETE
	public static void delete(String url, HttpClientCallback cb) throws Exception {
		new HttpClient("DELETE", url, null, null, cb, 2000).execute();
	}
	
}
