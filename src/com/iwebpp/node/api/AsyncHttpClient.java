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

		private Object data;

		// response content
		private IncomingMessage response;

		private List<Object> body;

		private int timeout; // ms

		private TimerHandle tmohdl;

		protected HttpClient(
				String method,
				String url,
				Map<String, String> headers, 
				Object data,
				final HttpClientCallback cb,
				int timeout) {
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

			// default timeout 2s
			this.timeout = 2000;
			
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
			this.once("timeout", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					cb.onResponse("Operation timeout", -1, null, null);					
				}

			});	
			this.once("error", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					cb.onResponse("Operation error:"+data!=null? data.toString() : "", -1, null, null);					
				}

			});	
		}
		// clear timeout
		private void clearTimeout() {
			if (timeout>0 && tmohdl!=null)
				tmohdl.close();
		}
		
		@Override
		public void content() throws Exception {
			final HttpClient self = this;

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

			// make http request with timeout
			if (timeout > 0) 
				this.tmohdl = getNodeContext().setTimeout(new TimeoutListener(){

					@Override
					public void onTimeout() throws Exception {
						self.emit("timeout");
					}

				}, timeout);

			ClientRequest req = http.request(getNodeContext(), options, new ClientRequest.responseListener(){

				@Override
				public void onResponse(IncomingMessage res) throws Exception {
					// clear timeout
					clearTimeout();
					
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
						self.emit("error", "response miss content-type header");
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
							Log.d(TAG, "response got end\n");

							if (data!=null) body.add(data);

							self.emit("completed");
						}

					});
					res.on("error", new Listener(){

						@Override
						public void onEvent(Object error) throws Exception {
							Log.d(TAG, "response got error:\n"+error);
							
							self.emit("error", "request error:"+error);
						}

					});
				}

			});
			
			if (data!=null) req.write(data);
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
		public String getType() {
			return this.type;
		}
		public Object getContent(){
			return this.content;
		}
	} 

	// HEAD
	public static void head(String url, HttpClientCallback cb) throws Exception {
		new HttpClient("HEAD", url, null, null, cb, 2000).execute();
	}

	// GET
	public static void get(String url, HttpClientCallback cb) throws Exception {
		new HttpClient("GET", url, null, null, cb, 2000).execute();
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
