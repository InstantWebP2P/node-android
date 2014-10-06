package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Base64;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Url;
import com.iwebpp.node.Url.UrlObj;
import com.iwebpp.node.http.HttpServer;
import com.iwebpp.node.http.HttppServer;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;
import com.iwebpp.node.http.http;
import com.iwebpp.node.http.httpp;
import com.iwebpp.node.net.AbstractServer;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.others.BasicBean;

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
	private static Map<String, List<String>> _webSocketPaths;

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
						upgradeHead.put(head); head.flip(); upgradeHead.flip();

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
						upgradeHead.put(head); head.flip(); upgradeHead.flip();

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
			if (u!=null && !this.options.path.equalsIgnoreCase(u.pathname)) return;
		}

		///if (typeof req.headers.upgrade === 'undefined' || req.headers.upgrade.toLowerCase() !== 'websocket') {
		if (!req.headers().containsKey("upgrade") || 
			 req.headers().get("upgrade").isEmpty() || 
			!req.headers().get("upgrade").get(0).equalsIgnoreCase("websocket")) {
			abortConnection(socket, 400, "Bad Request");
			return;
		}

		///if (req.headers['sec-websocket-key1']) handleHixieUpgrade.apply(this, arguments);
		///else handleHybiUpgrade.apply(this, arguments);
		handleHybiUpgrade(req, socket, upgradeHead, cb);
	}

/**
 * Entirely private apis,
 * which may or may not be bound to a specific WebSocket instance.
 * @throws Exception 
 */

private void handleHybiUpgrade(final IncomingMessage req, final AbstractSocket socket, final ByteBuffer upgradeHead, final UpgradeCallback cb) throws Exception {
  // handle premature socket errors
  /*var errorHandler = function() {
    try { socket.destroy(); } catch (e) {}
  }*/
  Listener errorHandler = new Listener(){

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
  int version = req.headers().containsKey("sec-websocket-version") ? Integer.parseInt(req.headers().get("sec-websocket-version").get(0), 10) : -1;

  ///if ([8, 13].indexOf(version) === -1) {
  if (version!=13 && version!=8) {
	  abortConnection(socket, 400, "Bad Request");
	  return;
  }

  // verify protocol
  ///var protocols = req.headers['sec-websocket-protocol'];
  String protocols = req.headers().containsKey("sec-websocket-protocol") ? req.headers().get("sec-websocket-protocol").get(0) : null;
 
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
  if (this.options.verifyClient != null) {
	  VerifyInfo info = new VerifyInfo();
	  info.origin = origin;
	  info.secure = false; // TBD...
	  info.req = req;

	  if (!this.options.verifyClient.onClient(info)) {
		  abortConnection(socket, 401, "Unauthorized");
		  return;
	  }
  }

  completeHybiUpgrade1(
		  protocols, version, errorHandler,
		  req, socket, upgradeHead, cb);
}

public static class VerifyInfo {
	 public String origin = null;
     public boolean secure = false;
     public IncomingMessage req = null;
}

public interface VerifyClient {
	public boolean onClient(VerifyInfo info) throws Exception;
}

private void completeHybiUpgrade2(
		String protocol, 
		int version,
		Listener errorHandler,
		final IncomingMessage req, final AbstractSocket socket, final ByteBuffer upgradeHead, final UpgradeCallback cb) throws Exception {
	final WebSocketServer self = this;
	
	// calc key
	///var key = req.headers['sec-websocket-key'];
	String key = req.headers().containsKey("sec-websocket-key") ? req.headers().get("sec-websocket-key").get(0) : null;
	/*var shasum = crypto.createHash('sha1');
	shasum.update(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11");
	key = shasum.digest('base64');
*/
	key = Base64.encodeToString((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("utf-8"), Base64.DEFAULT);
	
	/*var headers = [
	               'HTTP/1.1 101 Switching Protocols'
	               , 'Upgrade: websocket'
	               , 'Connection: Upgrade'
	               , 'Sec-WebSocket-Accept: ' + key
	               ];

	if (typeof protocol != 'undefined') {
		headers.push('Sec-WebSocket-Protocol: ' + protocol);
	}*/
    Map<String, String> headers = new Hashtable<String, String>();
    
    ///headers.put("HTTP/1.1 101 Switching Protocols", new ArrayList<String>());
    headers.put("FirstLine", "HTTP/1.1 101 Switching Protocols");
    headers.put("Upgrade", "websocket");
    headers.put("Connection", "Upgrade");
    headers.put("Sec-WebSocket-Accept", key);

	// allows external modification/inspection of handshake headers
	self.emit("headers", headers);
	    
	///socket.setTimeout(0);
	socket.setNoDelay(true);
	try {
	    String headersStr = "";
	    headersStr += "HTTP/1.1 101 Switching Protocols\r\n";
	    headersStr += "Upgrade: websocket\r\n";
	    headersStr += "Connection: Upgrade\r\n";
	    headersStr += "Sec-WebSocket-Accept: " + key + "\r\n";
	    headersStr += "\r\n\r\n";
	    		
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

	if (self.options.clientTracking) {
		///self.clients.push(client);
		self.clients.add(client);
		
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
                if (self.clients.contains(client)) 
                	self.clients.remove(client);
			}
			
		});
	}

	// signal upgrade complete
	socket.removeListener("error", errorHandler);
	cb.onUpgrade(client);
}

// optionally call external protocol selection handler before
// calling completeHybiUpgrade2
private void completeHybiUpgrade1(
		String protocols, 
		final int version,
		final Listener errorHandler,
		final IncomingMessage req, final AbstractSocket socket, final ByteBuffer upgradeHead, final UpgradeCallback cb) throws Exception {
	final WebSocketServer self = this;

	// choose from the sub-protocols
	///if (typeof self.options.handleProtocols == 'function') {
	if (self.options.handleProtocols != null) {
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
		self.options.handleProtocols.onProtocol(protList, new HandleProtocol.HandleProtocolCallback(){

			@Override
			public void onHandle(boolean result, String protocol) throws Exception {
				///callbackCalled = true;
				callbackCalled.set(true);
				
				if (!result) abortConnection(socket, 404, "Unauthorized");
				else completeHybiUpgrade2(
						protocol, version, errorHandler, 
						req, socket, upgradeHead, cb);				
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
			completeHybiUpgrade2(
					protocols.split(", *")[0], version, errorHandler,
					req, socket, upgradeHead, cb);
		}
		else {
			completeHybiUpgrade2(
					null, version, errorHandler,
					req, socket, upgradeHead, cb);
		}
	}
}

public interface HandleProtocol {
	public interface HandleProtocolCallback {
		public void onHandle(boolean result, String protocol) throws Exception;
	}

	public void onProtocol(String[] protList, HandleProtocolCallback cb) throws Exception;
}

	interface UpgradeCallback {
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
			response += "\r\n\r\n";

			socket.write(response, "utf-8", null);
		}
		catch (Exception e) { /* ignore errors - we've aborted this connection */ }
		finally {
			// ensure that an early aborted connection is shut down completely
			try { socket.destroy(null); } catch (Exception e) {}
		}
	}

	
}
