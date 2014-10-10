package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Base64;
import android.util.Log;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Url;
import com.iwebpp.node.Url.UrlObj;
import com.iwebpp.node.Util;
import com.iwebpp.node.http.HttpServer;
import com.iwebpp.node.http.HttppServer;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;
import com.iwebpp.node.http.http;
import com.iwebpp.node.http.httpp;
import com.iwebpp.node.net.AbstractServer;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.others.BasicBean;
import com.iwebpp.node.stream.Writable.WriteCB;

/**
 * WebSocket Server implementation
 */
public class WebSocketServer 
extends EventEmitter2 {
	private static final String TAG = "WebSocketServer";

	private AbstractServer _server = null;

	private NodeContext context;

	private Options options;

	private String path;

	private List<WebSocket> clients;

	// http/httpp server map to websocket servers with Path
	private static final Map<String, List<String>> _webSocketPaths;

	static {
		_webSocketPaths = new Hashtable<String, List<String>>();
	}

	public static class Options {
		public String host = "0.0.0.0";
		public int port = -1;
		public AbstractServer server = null;

		public String path = null;
		public boolean noServer = false;
		public boolean disableHixie = true;
		public boolean clientTracking = true;

		public boolean httpp = false;
		public boolean https = false;

		// TBD...
		public VerifyClient verifyClient = null;
		public HandleProtocol handleProtocols = null;

	}

	public interface ListeningCallback {
		public void onListening() throws Exception;
	}

	public void onconnection(final onconnectionListener cb) throws Exception {
		if (cb != null)
			this.on("connection", new Listener(){

				@Override
				public void onEvent(Object raw) throws Exception {
					WebSocket data = (WebSocket)raw;		

					cb.onConnection(data);
				}

			});
	}
	public interface onconnectionListener {
		public void onConnection(WebSocket socket) throws Exception;
	}

	public void onerror(final onerrorListener cb) throws Exception {
		if (cb != null)
			this.on("error", new Listener(){

				@Override
				public void onEvent(Object raw) throws Exception {
					String data = raw!=null ? raw.toString() : null;		

					cb.onError(data);
				}

			});
	}
	public interface onerrorListener {
		public void onError(String error) throws Exception;
	}

	public WebSocketServer(NodeContext ctx, Options options, final ListeningCallback callback) throws Exception {
		this.context = ctx;

		/*
		  options = new Options({
		    host: '0.0.0.0',
		    port: null,
		    server: null,
		    verifyClient: null,
		    handleProtocols: null,
		    path: null,
		    noServer: false,
		    disableHixie: false,
		    clientTracking: true,
		    httpp: false, // default as HTTP not HTTPP
		    https: false, // default as HTTP not HTTPS
		  }).merge(options);
		 */
		///if (!options.isDefinedAndNonNull('port') && !options.isDefinedAndNonNull('server') && !options.value.noServer) {
		if (options.port < 0 && options.server==null && !options.noServer) {
			throw new Exception("'port' or a 'server' must be provided");
		}

		final WebSocketServer self = this;

		/*if (options.isDefinedAndNonNull('port')) {
		    var httpObj;

		    if (typeof options.value.https === 'object') {
			  httpObj = options.value.httpp ? httpps : https;

			  this._server = httpObj.createServer(options.value.https, function (req, res) {
			    res.writeHead(200, {'Content-Type': 'text/plain'});
			    res.end('Not implemented');
			  });
		    } else {
			  httpObj = options.value.httpp ? httpp : http;

			  this._server = httpObj.createServer(function (req, res) {
			    res.writeHead(200, {'Content-Type': 'text/plain'});
			    res.end('Not implemented');
			  });    
		    }
		    this._server.listen(options.value.port, options.value.host, callback);
		    this._closeServer = function() { if (self._server) self._server.close(); };
		  }
		  else if (options.value.server) {
		    this._server = options.value.server;

		    if (options.value.path) {
		      // take note of the path, to avoid collisions when multiple websocket servers are
		      // listening on the same http server
		      if (this._server._webSocketPaths && options.value.server._webSocketPaths[options.value.path]) {
		        throw new Error('two instances of WebSocketServer cannot listen on the same http server path');
		      }
		      if (typeof this._server._webSocketPaths !== 'object') {
		        this._server._webSocketPaths = {};
		      }
		      this._server._webSocketPaths[options.value.path] = 1;
		    }
		  }*/
		if (options.port > 0) {

			if (options.httpp) {
				this._server = httpp.createServer(context, new HttppServer.requestListener() {

					public void onRequest(IncomingMessage req, ServerResponse res) throws Exception {
						Map<String, List<String>> headers = new Hashtable<String, List<String>>();

						headers.put("Content-Type", new ArrayList<String>());
						headers.get("Content-Type").add("text/plain");

						///res.writeHead(200, {'Content-Type': 'text/plain'});
						res.writeHead(200, headers);
						res.end("Not implemented", "utf-8", null);
					}

				}).listen(options.port, options.host, new HttppServer.ListeningCallback() {

					@Override
					public void onListening() throws Exception {
						callback.onListening();						
					}

				});    
			} else {
				this._server = http.createServer(context, new HttpServer.requestListener() {

					public void onRequest(IncomingMessage req, ServerResponse res) throws Exception {
						Map<String, List<String>> headers = new Hashtable<String, List<String>>();

						headers.put("Content-Type", new ArrayList<String>());
						headers.get("Content-Type").add("text/plain");

						///res.writeHead(200, {'Content-Type': 'text/plain'});
						res.writeHead(200, headers);
						res.end("Not implemented", "utf-8", null);
					}

				}).listen(options.port, options.host, new HttpServer.ListeningCallback() {

					@Override
					public void onListening() throws Exception {
						callback.onListening();						
					}

				});    
			}

			///this._server.listen(options.port, options.host, callback);
			///this._closeServer = function() { if (self._server) self._server.close(); };
		} else if (options.server!=null) {
			this._server = options.server;

			if (options.path != null) {
				// take note of the path, to avoid collisions when multiple websocket servers are
				// listening on the same http server
				/*if (this._server._webSocketPaths && options.value.server._webSocketPaths[options.value.path]) {
					  throw new Error('two instances of WebSocketServer cannot listen on the same http server path');
				  }*/
				if (_webSocketPaths!=null &&
						_webSocketPaths.containsKey(this._server.toString()) &&
						_webSocketPaths.get(this._server.toString()).contains(options.path)) {
					throw new Exception("two instances of WebSocketServer cannot listen on the same http server path");
				}

				/*if (typeof this._server._webSocketPaths !== 'object') {
					  this._server._webSocketPaths = {};
				  }
				  this._server._webSocketPaths[options.value.path] = 1;*/
				if (!_webSocketPaths.containsKey(this._server.toString())) {
					_webSocketPaths.put(this._server.toString(), new LinkedList<String>());
				}
				_webSocketPaths.get(this._server.toString()).add(options.path);
			}
		}

		if (this._server!=null) {
			///this._server.once('listening', function() { self.emit('listening'); });

			this._server.onceListening(new AbstractServer.ListeningCallback() {

				@Override
				public void onListening() throws Exception {
					self.emit("listening");
				}

			});
		}

		/*
		  if (typeof this._server != 'undefined') {
		    this._server.on('error', function(error) {
		      self.emit('error', error)
		    });
		    this._server.on('upgrade', function(req, socket, upgradeHead) {
		      //copy upgradeHead to avoid retention of large slab buffers used in node core
		      var head = new Buffer(upgradeHead.length);
		      upgradeHead.copy(head);

		      self.handleUpgrade(req, socket, head, function(client) {
		        self.emit('connection'+req.url, client);
		        self.emit('connection', client);
		      });
		    });
		  }
		 */
		if (this._server != null) {

			if (options.httpp) {
				HttppServer srv = (HttppServer)this._server;

				srv.onError(new AbstractServer.ErrorListener() {

					@Override
					public void onError(String error) throws Exception {
						self.emit("error", error);					
					}

				});

				srv.onUpgrade(new HttppServer.upgradeListener() {

					@Override
					public void onUpgrade(final IncomingMessage req, AbstractSocket socket,
							ByteBuffer upgradeHead) throws Exception {
						//copy upgradeHead to avoid retention of large slab buffers used in node core
						///var head = new Buffer(upgradeHead.length);
						///upgradeHead.copy(head);
						ByteBuffer head = ByteBuffer.allocate(upgradeHead.capacity());
						head.put(upgradeHead); head.flip(); upgradeHead.flip();

						Log.d(TAG, "onUpgrade, upgradeHead:"+upgradeHead+",head:"+head);

						/*self.handleUpgrade(req, socket, head, function(client) {
							self.emit("connection"+req.url, client);
							self.emit("connection", client);
						});*/
						self.handleUpgrade(req, socket, head, new UpgradeCallback(){

							@Override
							public void onUpgrade(WebSocket client)
									throws Exception {
								self.emit("connection"+req.url(), client);
								self.emit("connection", client);								
							}

						});

					}

				});

			} else {
				HttpServer srv = (HttpServer)this._server;

				srv.onError(new AbstractServer.ErrorListener() {

					@Override
					public void onError(String error) throws Exception {
						self.emit("error", error);					
					}

				});

				srv.onUpgrade(new HttpServer.upgradeListener() {

					@Override
					public void onUpgrade(final IncomingMessage req, AbstractSocket socket,
							ByteBuffer upgradeHead) throws Exception {
						//copy upgradeHead to avoid retention of large slab buffers used in node core
						///var head = new Buffer(upgradeHead.length);
						///upgradeHead.copy(head);
						ByteBuffer head = ByteBuffer.allocate(upgradeHead.capacity());
						head.put(upgradeHead); head.flip(); upgradeHead.flip();

						Log.d(TAG, "onUpgrade, upgradeHead:"+upgradeHead+",head:"+head);

						/*self.handleUpgrade(req, socket, head, function(client) {
							self.emit("connection"+req.url, client);
							self.emit("connection", client);
						});*/

						self.handleUpgrade(req, socket, head, new UpgradeCallback(){

							@Override
							public void onUpgrade(WebSocket client)
									throws Exception {
								self.emit("connection"+req.url(), client);
								self.emit("connection", client);								
							}

						});

					}

				});
			}

		}

		this.options = options;///.value;
		this.path = options.path;///value.path;
		this.clients = new LinkedList<WebSocket>();///[];

	}
	@SuppressWarnings("unused")
	private WebSocketServer() {}

	private void _closeServer() throws Exception {
		if (this._server!=null) this._server.close(null); 
	}


	/**
	 * Immediately shuts down the connection.
	 * @throws Exception 
	 *
	 * @api public
	 */

	public void close() throws Exception {
		// terminate all associated clients
		String error = null;
		try {
			for (int i = 0, l = this.clients.size(); i < l; ++i) {
				this.clients.get(i).terminate();
			}
		}
		catch (Exception e) {
			error = e.toString();
		}

		// remove path descriptor, if any
		/*
  if (this.path && this._server._webSocketPaths) {
    delete this._server._webSocketPaths[this.path];
    if (Object.keys(this._server._webSocketPaths).length == 0) {
      delete this._server._webSocketPaths;
    }
  }*/
		if (this.path!=null && _webSocketPaths.containsKey(this._server.toString())) {
			if (_webSocketPaths.get(this._server.toString()).contains(this.path)) 
				_webSocketPaths.get(this._server.toString()).remove(this.path);

			if (_webSocketPaths.get(this._server.toString()).isEmpty()) {
				_webSocketPaths.remove(this._server.toString());
			}
		}

		// close the http server if it was internally created
		/*try {
    if (typeof this._closeServer !== 'undefined') {
      this._closeServer();
    }
  }
  finally {
    delete this._server;
  }
		 */
		if (this.options.port > 0) {
			try {
				this._closeServer();
			} finally {
				this._server = null;
			}
		}

		if (error != null) throw new Exception(error);
	}

	/**
	 * Handle a HTTP Upgrade request.
	 * @throws Exception 
	 *
	 * @api public
	 */

	public void handleUpgrade(IncomingMessage req, AbstractSocket socket, ByteBuffer upgradeHead, UpgradeCallback cb) throws Exception {
		// check for wrong path
		if (this.options.path != null) {
			UrlObj u = Url.parse(req.url());

			Log.d(TAG, "req.url:"+req.url()+",options.path:"+this.options.path+",u.pathname:"+u.pathname);

			if (u!=null && !this.options.path.equalsIgnoreCase(u.pathname)) return;
		}

		///if (typeof req.headers.upgrade === 'undefined' || req.headers.upgrade.toLowerCase() !== 'websocket') {
		if (!req.headers().containsKey("upgrade") || 
				req.headers().get("upgrade").isEmpty() || 
				!req.headers().get("upgrade").get(0).equalsIgnoreCase("websocket")) {
			abortConnection(socket, 400, "Bad Request");
			return;
		}

		if (req.headers().containsKey("sec-websocket-key1")) new handleHixieUpgrade(req, socket, upgradeHead, cb);
		else new handleHybiUpgrade(req, socket, upgradeHead, cb);
	}

	/**
	 * Entirely private apis,
	 * which may or may not be bound to a specific WebSocket instance.
	 * @throws Exception 
	 */

	private class handleHybiUpgrade {		
		private final IncomingMessage req;
		private final AbstractSocket socket;
		private final ByteBuffer upgradeHead;
		private final UpgradeCallback cb;
		private final Listener errorHandler;
		private int version;
		private String protocols;

		protected handleHybiUpgrade(
				final IncomingMessage req,
				final AbstractSocket socket, 
				final ByteBuffer upgradeHead, 
				final UpgradeCallback cb
				) throws Exception {			
			this.req = req;
			this.socket = socket;
			this.upgradeHead = upgradeHead;
			this.cb = cb;


			// handle premature socket errors
			/*var errorHandler = function() {
	    try { socket.destroy(); } catch (e) {}
	  }*/
			errorHandler = new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					try { socket.destroy(null); } catch (Exception e) {}
				}

			};
			socket.on("error", errorHandler);

			// verify key presence
			///if (!req.headers['sec-websocket-key']) {
			if (!req.headers().containsKey("sec-websocket-key") ||
					req.headers().get("sec-websocket-key").isEmpty()) {
				abortConnection(socket, 400, "Bad Request");
				return;
			}

			// verify version
			///var version = parseInt(req.headers['sec-websocket-version']);
			version = req.headers().containsKey("sec-websocket-version") ? 
					Integer.parseInt(req.headers().get("sec-websocket-version").get(0), 10) : -1;

					///if ([8, 13].indexOf(version) === -1) {
					if (version!=13 && version!=8) {
						abortConnection(socket, 400, "Bad Request");
						return;
					}

					// verify protocol
					///var protocols = req.headers['sec-websocket-protocol'];
					protocols = req.headers().containsKey("sec-websocket-protocol") ? 
							req.headers().get("sec-websocket-protocol").get(0) : null;

							// verify client
							/*var origin = version < 13 ?
	    req.headers['sec-websocket-origin'] :
	    req.headers['origin'];
							 */
							String origin = version < 13 ?
									(req.headers().containsKey("sec-websocket-origin") ? req.headers().get("sec-websocket-origin").get(0) : null) :
										(req.headers().containsKey("origin") ? req.headers().get("origin").get(0) : null);


									// optionally call external client verification handler
									// TBD...
									/*
	  if (typeof this.options.verifyClient == 'function') {
	    var info = {
	      origin: origin,
	      secure: typeof req.connection.authorized !== 'undefined' || typeof req.connection.encrypted !== 'undefined',
	      req: req
	    };
	    if (this.options.verifyClient.length == 2) {
	      this.options.verifyClient(info, function(result, code, name) {
	        if (typeof code === 'undefined') code = 401;
	        if (typeof name === 'undefined') name = http.STATUS_CODES[code];

	        if (!result) abortConnection(socket, code, name);
	        else completeHybiUpgrade1();
	      });
	      return;
	    }
	    else if (!this.options.verifyClient(info)) {
	      abortConnection(socket, 401, 'Unauthorized');
	      return;
	    }
	  }*/
									if (options.verifyClient != null) {
										VerifyInfo info = new VerifyInfo();
										info.origin = origin;
										info.secure = false; // TBD...
										info.req = req;

										if (!options.verifyClient.onClient(info)) {
											abortConnection(socket, 401, "Unauthorized");
											return;
										}
									}

									completeHybiUpgrade1();

		}


		// optionally call external protocol selection handler before
		// calling completeHybiUpgrade2
		private void completeHybiUpgrade1() throws Exception {
			// choose from the sub-protocols
			///if (typeof self.options.handleProtocols == 'function') {
			if (options.handleProtocols != null) {
				///var protList = (protocols || "").split(/, */);
				// TBD...
				String[] protList = (protocols!=null ? protocols : "").split(", *");

				///boolean callbackCalled = false;
				final BasicBean<Boolean> callbackCalled = new BasicBean<Boolean>(false);

				/*var res = self.options.handleProtocols(protList, function(result, protocol) {
				callbackCalled = true;
				if (!result) abortConnection(socket, 404, "Unauthorized");
				else completeHybiUpgrade2(
						protocol, version, errorHandler, 
						req, socket, upgradeHead, cb);
			});*/
				options.handleProtocols.onProtocol(protList, new HandleProtocol.HandleProtocolCallback(){

					@Override
					public void onHandle(boolean result, String protocol) throws Exception {
						///callbackCalled = true;
						callbackCalled.set(true);

						if (!result) abortConnection(socket, 404, "Unauthorized");
						else completeHybiUpgrade2(protocol);				
					}

				});

				if (!callbackCalled.get()) {
					// the handleProtocols handler never called our callback
					abortConnection(socket, 501, "Could not process protocols");
				}

				return;
			} else {
				///if (typeof protocols !== 'undefined') {
				if (protocols != null) {
					///completeHybiUpgrade2(protocols.split(/, */)[0]);
					completeHybiUpgrade2(protocols.split(", *")[0]);
				}
				else {
					completeHybiUpgrade2(null);
				}
			}

		}

		private void completeHybiUpgrade2(String protocol) throws Exception {
			// calc key
			///var key = req.headers['sec-websocket-key'];
			String keystr = req.headers().containsKey("sec-websocket-key") ?
					req.headers().get("sec-websocket-key").get(0) : "";
					/*var shasum = crypto.createHash('sha1');
		shasum.update(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
		key = shasum.digest('base64');
					 */
					MessageDigest shasum = MessageDigest.getInstance("SHA1");
					byte[] sharet = shasum.digest((keystr.trim() + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("utf-8"));
					byte[] retbuf = Base64.encode(sharet, Base64.DEFAULT);

					String key = new String(retbuf, "utf-8").trim();

					Log.d(TAG, "keystr:"+keystr+",key:"+key);

					/*var headers = [
		               'HTTP/1.1 101 Switching Protocols'
		               , 'Upgrade: websocket'
		               , 'Connection: Upgrade'
		               , 'Sec-WebSocket-Accept: ' + key
		               ];

		if (typeof protocol != 'undefined') {
			headers.push('Sec-WebSocket-Protocol: ' + protocol);
		}*/
					List<String> headers = new ArrayList<String>();

					headers.add("HTTP/1.1 101 Switching Protocols");
					headers.add("Upgrade: websocket");
					headers.add("Connection: Upgrade");
					headers.add("Sec-WebSocket-Accept: " + key);

					if (protocol != null && protocol != "") {
						headers.add("Sec-WebSocket-Protocol: " + protocol);
					}

					// allows external modification/inspection of handshake headers
					emit("headers", headers);

					///socket.setTimeout(0);
					socket.setNoDelay(true);
					try {
						String headersStr = "";
						headersStr += "HTTP/1.1 101 Switching Protocols\r\n";
						headersStr += "Upgrade: websocket\r\n";
						headersStr += "Connection: Upgrade\r\n";
						headersStr += "Sec-WebSocket-Accept: " + key + "\r\n";

						if (protocol != null && protocol != "") 
							headersStr += "Sec-WebSocket-Protocol: " + protocol.trim() + "\r\n";

						///headersStr += "\r\n\r\n";
						headersStr += "\r\n";

						///socket.write(headers.concat('', '').join('\r\n'));
						socket.write(headersStr, "utf-8", null);
					}
					catch (Exception e) {
						// if the upgrade write fails, shut the connection down hard
						try { socket.destroy(null); } catch (Exception ee) {}
						return;
					}

					WebSocket.Options wsopt = new WebSocket.Options();
					wsopt.protocolVersion = version;
					wsopt.protocol = protocol;
					final WebSocket client = new WebSocket(context, new http.request_socket_head_b(req, socket, upgradeHead), wsopt);

					if (options.clientTracking) {
						///self.clients.push(client);
						clients.add(client);

						/*
			client.on("close", function() {
				var index = self.clients.indexOf(client);
				if (index != -1) {
					self.clients.splice(index, 1);
				}
			});*/
						client.on("close", new Listener(){

							@Override
							public void onEvent(Object data) throws Exception {
								if (clients.contains(client)) 
									clients.remove(client);
							}

						});
					}

					// signal upgrade complete
					socket.removeListener("error", errorHandler);
					cb.onUpgrade(client);

		}

	}

	private class handleHixieUpgrade {
		
		private final IncomingMessage req;
		private final AbstractSocket socket;
		private final ByteBuffer upgradeHead;
		private final UpgradeCallback cb;
		private final Listener errorHandler;
		private String location;
		private String protocol;
		private String origin;

		protected handleHixieUpgrade(
				final IncomingMessage req, 
				final AbstractSocket socket, 
				final ByteBuffer upgradeHead,
				final UpgradeCallback cb) throws Exception {
			this.req = req;
			this.socket = socket;
			this.upgradeHead = upgradeHead;
			this.cb = cb;
			
			// handle premature socket errors
			/*var errorHandler = function() {
	    try { socket.destroy(); } catch (e) {}
	  }
	  socket.on('error', errorHandler);
			 */
			errorHandler = new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					try { socket.destroy(null); } catch (Exception e) {}
				}

			};
			socket.on("error", errorHandler);


			// bail if options prevent hixie
			if (options.disableHixie) {
				abortConnection(socket, 401, "Hixie support disabled");
				return;
			}

			// verify key presence
			///if (!req.headers['sec-websocket-key2']) {
			if (!req.headers().containsKey("sec-websocket-key2")) {
				abortConnection(socket, 400, "Bad Request");
				return;
			}

			///var origin = req.headers['origin']
			///  , self = this;
			this.origin = req.headers().containsKey("origin") ? req.headers().get("origin").get(0) : null;



			// verify client
			/*if (typeof this.options.verifyClient == 'function') {
	    var info = {
	      origin: origin,
	      secure: typeof req.connection.authorized !== 'undefined' || typeof req.connection.encrypted !== 'undefined',
	      req: req
	    };
	    if (this.options.verifyClient.length == 2) {
	      var self = this;
	      this.options.verifyClient(info, function(result, code, name) {
	        if (typeof code === 'undefined') code = 401;
	        if (typeof name === 'undefined') name = http.STATUS_CODES[code];

	        if (!result) abortConnection(socket, code, name);
	        else onClientVerified.apply(self);
	      });
	      return;
	    }
	    else if (!this.options.verifyClient(info)) {
	      abortConnection(socket, 401, 'Unauthorized');
	      return;
	    }
	  }
			 */
			if (options.verifyClient != null) {
				VerifyInfo info = new VerifyInfo();
				info.origin = origin;
				info.secure = false; // TBD...
				info.req = req;

				if (!options.verifyClient.onClient(info)) {
					abortConnection(socket, 401, "Unauthorized");
					return;
				}
			}

			// no client verification required
			onClientVerified();
		}
		
		// setup handshake completion to run after client has been verified
		private void onClientVerified() throws Exception {
			/*var wshost;
			if (!req.headers['x-forwarded-host'])
				wshost = req.headers.host;
			else
				wshost = req.headers['x-forwarded-host'];
			*/
			String wshost;
			if (!req.headers().containsKey("x-forwarded-host"))
				wshost = req.headers().get("host").get(0);
			else 
				wshost = req.headers().get("x-forwarded-host").get(0);
			
			///var location = ((req.headers['x-forwarded-proto'] === 'https' || socket.encrypted) ? 'wss' : 'ws') + '://' + wshost + req.url
			///		, protocol = req.headers['sec-websocket-protocol'];
			// TBD... secure
			this.location = "ws://" + wshost + req.url();
			this.protocol = req.headers().containsKey("sec-websocket-protocol") ? req.headers().get("sec-websocket-protocol").get(0) : null;

			// retrieve nonce
			final int nonceLength = 8;
			if (upgradeHead!=null && upgradeHead.capacity() >= nonceLength) {
				/*
				var nonce = upgradeHead.slice(0, nonceLength);
				var rest = upgradeHead.length > nonceLength ? upgradeHead.slice(nonceLength) : null;
				completeHandshake.call(self, nonce, rest);*/
				ByteBuffer nonce = (ByteBuffer) Util.chunkSlice(upgradeHead, 0, nonceLength);
				ByteBuffer rest = (ByteBuffer) (upgradeHead.capacity() > nonceLength ? Util.chunkSlice(upgradeHead, nonceLength, upgradeHead.capacity()) : null);
				completeHandshake(nonce, rest);
			}
			else {
				// nonce not present in upgradeHead, so we must wait for enough data
				// data to arrive before continuing
				/*var nonce = new Buffer(nonceLength);
				upgradeHead.copy(nonce, 0);
				var received = upgradeHead.length;
				var rest = null;*/
				final ByteBuffer nonce = ByteBuffer.allocate(nonceLength);
				BufferUtil.fastCopy(upgradeHead.capacity(), upgradeHead, nonce, 0);
				///int received = upgradeHead.capacity();
				final BasicBean<Integer> received = new BasicBean<Integer>(upgradeHead.capacity());

				/*var handler = function (data) {
					var toRead = Math.min(data.length, nonceLength - received);
					if (toRead === 0) return;
					data.copy(nonce, received, 0, toRead);
					received += toRead;
					if (received == nonceLength) {
						socket.removeListener("data", handler);
						if (toRead < data.length) rest = data.slice(toRead);
						completeHandshake.call(self, nonce, rest);
					}
				}
				socket.on("data", handler);*/
				socket.on("data", new Listener(){
					public void onEvent(final Object raw) throws Exception {
						ByteBuffer data = (ByteBuffer)raw;
						ByteBuffer rest = null;
						
						int toRead = Math.min(data.capacity(), nonceLength - received.get());
						if (toRead == 0) return;
						///data.copy(nonce, received, 0, toRead);
						BufferUtil.fastCopy(toRead, data, nonce, received.get());

						received.set(received.get() + toRead);
						if (received.get() == nonceLength) {
							socket.removeListener("data", this);
							///if (toRead < data.length) rest = data.slice(toRead);
							if (toRead < data.capacity()) rest = (ByteBuffer) Util.chunkSlice(data, toRead, data.capacity());
							completeHandshake(nonce, rest);
						}
					}
				});
			}
		}
		
		// handshake completion code to run once nonce has been successfully retrieved
		private void completeHandshake(ByteBuffer nonce, final ByteBuffer rest) throws Exception {
			// calculate key
			/*var k1 = req.headers['sec-websocket-key1']
			  , k2 = req.headers['sec-websocket-key2']
			  , md5 = crypto.createHash('md5');

			[k1, k2].forEach(function (k) {
				var n = parseInt(k.replace(/[^\d]/g, ''))
						, spaces = k.replace(/[^ ]/g, '').length;
				if (spaces === 0 || n % spaces !== 0){
					abortConnection(socket, 400, 'Bad Request');
					return;
				}
				n /= spaces;
				md5.update(String.fromCharCode(
						n >> 24 & 0xFF,
						n >> 16 & 0xFF,
						n >> 8  & 0xFF,
						n       & 0xFF));
			});
			md5.update(nonce.toString('binary'));
*/
			String k1 = req.headers().containsKey("sec-websocket-key1") ? req.headers().get("sec-websocket-key1").get(0): null;
			String k2 = req.headers().containsKey("sec-websocket-key2") ? req.headers().get("sec-websocket-key2").get(0): null;
		
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			if (k1 != null) {
				int n = Integer.parseInt(k1.replaceAll("[^[0-9]]", ""));
				int spaces = k1.replaceAll("[^ ]", "").length();
				if (spaces == 0 || n % spaces != 0){
					abortConnection(socket, 400, "Bad Request");
					return;
				}
				n /= spaces;
				String kstr = new String(new int[]{
					n >> 24 & 0xFF,
					n >> 16 & 0xFF,
					n >> 8  & 0xFF,
					n       & 0xFF
					}, 0, 4);
				/*md5.update(String.fromCharCode(
						n >> 24 & 0xFF,
						n >> 16 & 0xFF,
						n >> 8  & 0xFF,
						n       & 0xFF));*/
				md5.update(kstr.getBytes("utf-8"));
			}
			if (k2 != null) {
				int n = Integer.parseInt(k2.replaceAll("[^[0-9]]", ""));
				int spaces = k2.replaceAll("[^ ]", "").length();
				if (spaces == 0 || n % spaces != 0){
					abortConnection(socket, 400, "Bad Request");
					return;
				}
				n /= spaces;
				String kstr = new String(new int[]{
					n >> 24 & 0xFF,
					n >> 16 & 0xFF,
					n >> 8  & 0xFF,
					n       & 0xFF
					}, 0, 4);
				/*md5.update(String.fromCharCode(
						n >> 24 & 0xFF,
						n >> 16 & 0xFF,
						n >> 8  & 0xFF,
						n       & 0xFF));*/
				md5.update(kstr.getBytes("utf-8"));
			}
			
			/*
			var headers = [
			               'HTTP/1.1 101 Switching Protocols'
			               , 'Upgrade: WebSocket'
			               , 'Connection: Upgrade'
			               , 'Sec-WebSocket-Location: ' + location
			               ];
			if (typeof protocol != 'undefined') headers.push('Sec-WebSocket-Protocol: ' + protocol);
			if (typeof origin != 'undefined') headers.push('Sec-WebSocket-Origin: ' + origin);
*/
			String headers = "";
			headers += "HTTP/1.1 101 Switching Protocols\r\n";
			headers += "Upgrade: WebSocket\r\n";
			headers += "Connection: Upgrade\r\n";
			headers += "Sec-WebSocket-Location: " + location + "\r\n";
			
			if (protocol != null)
				headers += "Sec-WebSocket-Protocol: " + protocol + "\r\n";
			
			if (origin != null)
				headers += "Sec-WebSocket-Origin: " + origin + "\r\n";

			headers += "\r\n";
					
			///socket.setTimeout(0);
			socket.setNoDelay(true);
			try {
				// merge header and hash buffer
				///var headerBuffer = new Buffer(headers.concat('', '').join('\r\n'));
				///var hashBuffer = new Buffer(md5.digest('binary'), 'binary');
				///var handshakeBuffer = new Buffer(headerBuffer.length + hashBuffer.length);
				ByteBuffer headerBuffer = ByteBuffer.wrap(headers.getBytes("utf-8"));
				ByteBuffer hashBuffer = ByteBuffer.wrap(md5.digest());
				ByteBuffer handshakeBuffer = ByteBuffer.allocate(headerBuffer.capacity() + hashBuffer.capacity());

				///headerBuffer.copy(handshakeBuffer, 0);
				///hashBuffer.copy(handshakeBuffer, headerBuffer.length);
				BufferUtil.fastCopy(headerBuffer.capacity(), headerBuffer, handshakeBuffer, 0);
				BufferUtil.fastCopy(hashBuffer.capacity(), hashBuffer, handshakeBuffer, headerBuffer.capacity());

				// do a single write, which - upon success - causes a new client websocket to be setup
				/*socket.write(handshakeBuffer, 'binary', function(err) {
					if (err) return; // do not create client if an error happens
					var client = new WebSocket([req, socket, rest], {
						protocolVersion: 'hixie-76',
						protocol: protocol
					});
					if (self.options.clientTracking) {
						self.clients.push(client);
						client.on('close', function() {
							var index = self.clients.indexOf(client);
							if (index != -1) {
								self.clients.splice(index, 1);
							}
						});
					}

					// signal upgrade complete
					socket.removeListener("error", errorHandler);
					cb(client);
				});*/
				socket.write(handshakeBuffer, null, new WriteCB(){
					public void writeDone(final String err) throws Exception {
						if (err!=null) return; // do not create client if an error happens

						WebSocket.Options wsopt = new WebSocket.Options();
						wsopt.protocolVersionHixie = "hixie-76";
						wsopt.protocol = protocol;
						final WebSocket client = new WebSocket(context, new http.request_socket_head_b(req, socket, rest), wsopt);

						if (options.clientTracking) {
							clients.add(client);
							
							client.on("close", new Listener(){

								@Override
								public void onEvent(Object data) throws Exception {
									if (clients.contains(client)) 
										clients.remove(client);
								}

							});
						}

						// signal upgrade complete
						socket.removeListener("error", errorHandler);
						cb.onUpgrade(client);
					
					}
				});
			}
			catch (Exception e) {
				try { socket.destroy(null); } catch (Exception ee) {}
				return;
			}
		}

	}

	public static class VerifyInfo {
		public String origin = null;
		public boolean secure = false;
		public IncomingMessage req = null;
	}

	public interface VerifyClient {
		public boolean onClient(VerifyInfo info) throws Exception;
	}

	public interface HandleProtocol {
		public interface HandleProtocolCallback {
			public void onHandle(boolean result, String protocol) throws Exception;
		}

		public void onProtocol(String[] protList, HandleProtocolCallback cb) throws Exception;
	}

	private interface UpgradeCallback {
		void onUpgrade(WebSocket client) throws Exception;	
	}

	private static void abortConnection(AbstractSocket socket, int code, String name) {
		try {
			/*
			var response = [
			                'HTTP/1.1 ' + code + ' ' + name,
			                'Content-type: text/html'
			                ];
			socket.write(response.concat('', '').join('\r\n'));*/
			String response = "";
			response += "HTTP/1.1 " + code + " " + name + "\r\n";
			response += "Content-type: text/html\r\n";
			///response += "\r\n\r\n";
			response += "\r\n";

			socket.write(response, "utf-8", null);
		}
		catch (Exception e) { /* ignore errors - we've aborted this connection */ }
		finally {
			// ensure that an early aborted connection is shut down completely
			try { socket.destroy(null); } catch (Exception e) {}
		}
	}

}
