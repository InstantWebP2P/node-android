// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.http;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.nextTickListener;
import com.iwebpp.node.NodeError;
import com.iwebpp.node.HttpParser.http_parser_type;
import com.iwebpp.node.Util;
import com.iwebpp.node.http.Http.response_socket_head_b;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.net.TCP;
import com.iwebpp.node.others.TripleState;

public final class ClientRequest 
extends OutgoingMessage {
	private final static String TAG = "ClientRequest";

	private IncomingParser parser;

	private IncomingMessage res;

	private String method;

	private boolean upgradeOrConnect;

	// AbstractSocket event listeners
	private socketCloseListener socketCloseListener;
	private socketErrorListener socketErrorListener;
	private socketOnData        socketOnData;
	private socketOnEnd         socketOnEnd;

	private int maxHeadersCount = 4000;

	private long aborted = -1;

	private String path;

	public ClientRequest(NodeContext context, ReqOptions options, responseListener cb) throws Exception {
		super(context);
		final ClientRequest self = this;

		/*if (util.isString(options)) {
		    options = url.parse(options);
		  } else {
		    options = util._extend({}, options);
		  }*/

		// No global agent in node.android
		Agent agent = options.agent;
		///var defaultAgent = options._defaultAgent || Agent.globalAgent;
		/*if (agent === false) {
		    agent = new defaultAgent.constructor();
		  } else */if (Util.isNullOrUndefined(agent) && null==options.createConnection) {
			  // TBD...
			  agent = new Agent(context, options);///defaultAgent;
		  }
		  self.agent = agent;
		  ///self.agent = null;

		  String protocol = !Util.zeroString(options.protocol) ?  options.protocol : "http:";///defaultAgent.protocol;
		  String expectedProtocol = "http:";///defaultAgent.protocol;
		  if (self.agent!=null && self.agent.protocol()!=null)
			  expectedProtocol = self.agent.protocol();

		  if (options.path!=null && options.path.contains(" ") /*/ /.test(options.path)*/) {
			  // The actual regex is more like /[^A-Za-z0-9\-._~!$&'()*+,;=/:@]/
			  // with an additional rule for ignoring percentage-escaped characters
			  // but that's a) hard to capture in a regular expression that performs
			  // well, and b) possibly too restrictive for real-world usage. That's
			  // why it only scans for spaces because those are guaranteed to create
			  // an invalid request.
			  throw new Exception("Request path contains unescaped characters.");
		  } else if (!protocol.equalsIgnoreCase(expectedProtocol)) {
			  throw new Error("Protocol " + protocol + " not supported. " +
					  "Expected " + expectedProtocol + ".");
		  }

		  int defaultPort = options.defaultPort > 0 ? options.defaultPort :
			  self.agent!=null ? self.agent.defaultPort() : 80;

			  int port = options.port = options.port > 0 ? options.port : defaultPort;/// || 80;
			  String host = options.host = options.hostname!=null ? options.hostname :
				  options.host!=null ?  options.host : "localhost";

			  ///if (util.isUndefined(options.setHost)) {
			  boolean setHost = options.setHost;
			  ///}

			  String method = self.method = (options.method!=null? options.method : "GET").toUpperCase();
			  self.path = options.path!=null? options.path : "/";
			  if (cb!=null) {
				  ///self.once("response", cb);
				  self.onceResponse(cb);
			  }

			  /*
		  if (!util.isArray(options.headers)) {
		    if (options.headers) {
		      var keys = Object.keys(options.headers);
		      for (var i = 0, l = keys.length; i < l; i++) {
		        var key = keys[i];
		        self.setHeader(key, options.headers[key]);
		      }
		    }
		    if (host!=null && !this.getHeader("host") && setHost) {
		      var hostHeader = host;
		      if (port && +port !== defaultPort) {
		        hostHeader += ':' + port;
		      }
		      this.setHeader("Host", hostHeader);
		    }
		  }
			   */
			  {
				  if (options.headers != null)
					  for (Map.Entry<String, List<String>> entry : options.headers.entrySet())
						  self.setHeader(entry.getKey(), entry.getValue());

				  if (host!=null && null==this.getHeader("host") && setHost) {
					  String hostHeader = host;
					  if (port>0 && port != defaultPort) {
						  hostHeader += ':' + port;
					  }
					  this.setHeader("Host", hostHeader);
				  }
			  }


			  if (options.auth!=null && null==this.getHeader("Authorization")) {
				  //basic auth
				  this.setHeader("Authorization", "Basic " +
						  ///new ByteBuffer(options.auth).toString("base64"));
						  options.auth);
			  }

			  if (method == "GET" ||
				  method == "HEAD" ||
				  method == "DELETE" ||
				  method == "OPTIONS" ||
				  method == "CONNECT") {
				  self.useChunkedEncodingByDefault = false;
			  } else {
				  self.useChunkedEncodingByDefault = true;
			  }

			  if (null!=options.headers/*util.isArray(options.headers)*/) {
				  self._storeHeader(self.method + " " + self.path + " HTTP/1.1\r\n",
						  options.headers);
			  } else if (self.getHeader("expect")!=null) {
				  self._storeHeader(self.method + " " + self.path + " HTTP/1.1\r\n",
						  self._renderHeaders());
			  }

			  /*if (self.socketPath) {
		    self._last = true;
		    self.shouldKeepAlive = false;
		    var conn = self.agent.createConnection({ path: self.socketPath });
		    self.onSocket(conn);
		  } else*/ if (self.agent!=null) {
			  // If there is an agent we should default to Connection:keep-alive,
			  // but only if the Agent will actually reuse the connection!
			  // If it's not a keepAlive agent, and the maxSockets==Infinity, then
			  // there's never a case where this socket will actually be reused
			  ///if (!self.agent.isKeepAlive() && !Number.isFinite(self.agent.getMaxSockets())) {
			  if (!self.agent.keepAlive() && self.agent.maxSockets()==Agent.defaultMaxSockets) {
				  self._last = true;
				  self.shouldKeepAlive = false;
			  } else {
				  self._last = false;
				  self.shouldKeepAlive = true;
			  }
			  self.agent.addRequest(self, options);
		  } else {
			  // No agent, default to Connection:close.
			  self._last = true;
			  self.shouldKeepAlive = false;
			  TCP.Socket conn;
			  if (options.createConnection != null) {
				  conn = options.
						  createConnection.
						  createConnection(
								  context, 
								  options.host, options.port,
								  options.localAddress,
								  options.localPort,
								  null);
			  } else {
				  Log.d(TAG, "CLIENT use TCP.createConnection " + options);
				  conn = TCP.createConnection(
						  context, 
						  options.host, options.port,
						  options.localAddress,
						  options.localPort,
						  null);
			  }
			  self.onSocket(conn);
		  }

		  self._deferToConnect(null, null, new Listener(){

			  @Override
			  public void onEvent(Object data) throws Exception {
				  self._flush();
				  ///self = null;
				  Log.d(TAG, "_flush done");
			  }

		  });

	}

	protected void _finish() throws Exception {
		///DTRACE_HTTP_CLIENT_REQUEST(this, this.connection);
		///COUNTER_HTTP_CLIENT_REQUEST();
		super._finish();
	}

	public void abort() throws Exception {
		// Mark as aborting so we can avoid sending queued request data
		// This is used as a truthy flag elsewhere. The use of Date.now is for
		// debugging purposes only.
		this.aborted = System.currentTimeMillis();

		// If we're aborting, we don't care about any more response data.
		if (this.res != null)
			this.res._dump();
		else
			this.onceResponse(new responseListener(){

				@Override
				public void onResponse(IncomingMessage res) throws Exception {
					res._dump();
				}

			});

		// In the event that we don't have a socket, we will pop out of
		// the request queue through handling in onSocket.
		if (this.socket != null) {
			// in-progress
			this.socket.destroy(null);
		}
	}

	private void _deferToConnect(
			final String method, 
			final Object arguments_, 
			final Listener cb) throws Exception {
		// This function is for calls that need to happen once the socket is
		// connected and writable. It's an important promisy thing for all the socket
		// calls that happen either now (when a socket is assigned) or
		// in the future (when a socket gets assigned out of the pool and is
		// eventually writable).
		final ClientRequest self = this;

		Listener onSocket = new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				///final TCP.Socket socket = (TCP.Socket)data;
				
				Log.d(TAG, "onSocket: "+self.socket);
				Log.d(TAG, "this.connection: "+self.connection);
				
				if (self.socket.writable()) {
					Log.d(TAG, "_deferToConnect: "+self.socket.writable());

					/*
					if (method) {
						self.socket[method].apply(self.socket, arguments_);
					}
					if (cb) { cb(); }*/
					if (method!=null && method == "setNoDelay") {
						Boolean args = (Boolean)arguments_;

						self.socket.setNoDelay(args);
					} else if (method!=null && method == "setSocketKeepAlive") {
						enable_initialDelay_b args = (enable_initialDelay_b)arguments_;

						self.socket.setKeepAlive(args.enable, args.initialDelay);
					}
					if (cb != null) cb.onEvent(null);
				} else {
					/*self.socket.once("connect", function() {
						if (method) {
							self.socket[method].apply(self.socket, arguments_);
						}
						if (cb) { cb();}
					});*/
					// TBD...
					self.socket.once("connect", new Listener(){
						
						@Override
						public void onEvent(Object data) throws Exception {
							Log.d(TAG, "onSocket connected: "+socket);

							if (method!=null && method == "setNoDelay") {
								Boolean args = (Boolean)arguments_;

								self.socket.setNoDelay(args);
							} else if (method!=null && method == "setSocketKeepAlive") {
								enable_initialDelay_b args = (enable_initialDelay_b)arguments_;

								self.socket.setKeepAlive(args.enable, args.initialDelay);
							}
							if (cb != null) cb.onEvent(null);
						}

					});
				}
			}

		};

		if (null==self.socket) {
			self.once("socket", onSocket);
		} else {
			onSocket.onEvent(self.socket);
		}
	}

	///request.setTimeout(timeout, [callback])

	public void setNoDelay(boolean noDelay) throws Exception {
		this._deferToConnect("setNoDelay", new Boolean(noDelay), null);
	}

	public void setSocketKeepAlive(boolean enable, int initialDelay) throws Exception {
		this._deferToConnect("setKeepAlive", new enable_initialDelay_b(enable, initialDelay), null);
	}

	/*ClientRequest.prototype.clearTimeout = function(cb) {
  this.setTimeout(0, cb);
};*/

	// POJO beans
	private class enable_initialDelay_b {
		private boolean enable;
		private int     initialDelay;

		private enable_initialDelay_b(boolean enable, int initialDelay) {
			this.enable = enable;
			this.initialDelay = initialDelay;
		}
	}

	// Event listeners
	public void onceResponse(final responseListener cb) throws Exception {
		this.once("response", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				IncomingMessage data = (IncomingMessage)raw;

				cb.onResponse(data);
			}

		});
	}
	public static interface responseListener {
		public void onResponse(IncomingMessage res) throws Exception;
	}

	public void onceSocket(final socketListener cb) throws Exception {
		this.once("socket", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				TCP.Socket data = (TCP.Socket)raw;

				cb.onSocket(data);
			}

		});
	}
	public static interface socketListener {
		public void onSocket(TCP.Socket socket) throws Exception;
	}

	public void onceConnect(final connectListener cb) throws Exception {
		this.once("connect", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				response_socket_head_b data = (response_socket_head_b)raw;

				cb.onConnect(data.getResponse(), data.getSocket(), data.getHead());
			}

		});
	}
	public static interface connectListener {
		public void onConnect(IncomingMessage res, TCP.Socket socket, ByteBuffer head) throws Exception;
	}

	public void onceUpgrade(final upgradeListener cb) throws Exception {
		this.once("upgrade", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				response_socket_head_b data = (response_socket_head_b)raw;

				cb.onUpgrade(data.getResponse(), data.getSocket(), data.getHead());
			}

		});
	}
	public static interface upgradeListener {
		public void onUpgrade(IncomingMessage res, TCP.Socket socket, ByteBuffer head) throws Exception;
	}

	public void onContinue(final continueListener cb) throws Exception {
		this.on("continue", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {                   
				cb.onContinue();
			}

		});
	}
	public static interface continueListener {
		public void onContinue() throws Exception;
	}

	// Parser on response
	private class parserOnIncomingClient 
	extends IncomingParser {
		private static final String TAG = "parserOnIncoming";

		private NodeContext context;

		public parserOnIncomingClient(NodeContext ctx, TCP.Socket socket) {
			super(ctx, http_parser_type.HTTP_RESPONSE, socket);
			this.context = ctx;
		}
		private parserOnIncomingClient(){super(null, null, null);}

		@Override
		protected boolean onIncoming(final IncomingMessage res,
				boolean shouldKeepAlive) throws Exception {
			AbstractSocket socket = this.socket;
			final ClientRequest req = (ClientRequest)socket.get_httpMessage();


			// propogate "domain" setting...
			/*if (req.domain && !res.domain) {
				Log.d(TAG, "setting res.domain");
				res.domain = req.domain;
			}*/

			Log.d(TAG, "AGENT incoming response!");

			if (req.res != null) {
				// We already have a response object, this means the server
				// sent a double response.
				socket.destroy(null);
				///return;
				return false;
			}
			req.res = res;

			// Responses to CONNECT request is handled as Upgrade.
			if (req.method == "CONNECT") {
				res.setUpgrade(true);
				return true; // skip body
			}

			// Responses to HEAD requests are crazy.
			// HEAD responses aren't allowed to have an entity-body
			// but *can* have a content-length which actually corresponds
			// to the content-length of the entity-body had the request
			// been a GET.
			boolean isHeadResponse = req.method == "HEAD";
			Log.d(TAG, "AGENT isHeadResponse "+isHeadResponse);

			if (res.statusCode() == 100) {
				// restart the parser, as this is a continue message.
				///delete req.res; // Clear res so that we don't hit double-responses.
				req.res = null;
				req.emit("continue");
				return true;
			}

			if (req.shouldKeepAlive && !shouldKeepAlive && !req.upgradeOrConnect) {
				// AbstractServer MUST respond with Connection:keep-alive for us to enable it.
				// If we've been upgraded (via WebSockets) we also shouldn't try to
				// keep the connection open.
				req.shouldKeepAlive = false;
			}


			///DTRACE_HTTP_CLIENT_RESPONSE(socket, req);
			///COUNTER_HTTP_CLIENT_RESPONSE();
			req.res = res;
			res.setReq(req);

			// add our listener first, so that we guarantee socket cleanup
			Listener responseOnEnd = new Listener() {

				@Override
				public void onEvent(Object data) throws Exception {
					///var res = this;
					ClientRequest req = res.getReq();
					final AbstractSocket socket = req.socket;

					if (!req.shouldKeepAlive) {
						if (socket.writable()) {
							Log.d(TAG, "AGENT socket.destroySoon()");
							socket.destroySoon();
						}
						assert(!socket.writable());
					} else {
						Log.d(TAG, "AGENT socket keep-alive");
						// TBD...
						///if (req.timeoutCb) {
						///  socket.setTimeout(0, req.timeoutCb);
						///  req.timeoutCb = null;
						///}

						// TBD...
						socket.removeListener("close", socketCloseListener);
						socket.removeListener("error", socketErrorListener);

						// Mark this socket as available, AFTER user-added end
						// handlers have a chance to run.
						///process.nextTick(function() {
						context.nextTick(new nextTickListener() {

							@Override
							public void onNextTick() throws Exception {
								socket.emit("free");
							}

						});

					}
				}
			};
			res.on("end", responseOnEnd);

			boolean handled = req.emit("response", res);

			// If the user did not listen for the 'response' event, then they
			// can't possibly read the data, so we ._dump() it into the void
			// so that the socket doesn't hang there in a paused state.
			if (!handled)
				res._dump();

			return isHeadResponse;
		}

	}

	private void tickOnSocket(ClientRequest req, TCP.Socket socket) throws Exception {
		///var parser = parsers.alloc();
		parserOnIncomingClient parser = new parserOnIncomingClient(context, socket);

		req.socket = socket;
		req.connection = socket;
		parser.Reinitialize(http_parser_type.HTTP_RESPONSE);
		parser.socket = socket;
		parser.incoming = null;
		req.setParser(parser);

		socket.setParser(parser);
		socket.set_httpMessage(req);

		Log.d(TAG, "req.connection: "+req.connection);

		// Setup "drain" propogation.
		Http.httpSocketSetup(socket);

		// Propagate headers limit from request object to parser
		if (req.maxHeadersCount  > 0/*util.isNumber(req.maxHeadersCount)*/) {
			parser.maxHeaderPairs = req.maxHeadersCount << 1;
		} else {
			// Set default value because parser may be reused from FreeList
			parser.maxHeaderPairs = 2000;
		}

		///parser.onIncoming = parserOnIncomingClient;
		this.socketErrorListener = new socketErrorListener(context, socket);
		socket.on("error", socketErrorListener);
		
		// set flowing ??? TBD...
		socket.get_readableState().setFlowing(TripleState.TRUE);
		this.socketOnData = new socketOnData(context, socket);
		socket.on("data", socketOnData);

		this.socketOnEnd = new socketOnEnd(context, socket);
		socket.on("end", socketOnEnd);

		this.socketCloseListener = new socketCloseListener(context, socket);
		socket.on("close", socketCloseListener);
		
		req.emit("socket", socket);
		
		Log.d(TAG, "emit socket: "+socket);
	}

	public void onSocket(final TCP.Socket socket) {
		final ClientRequest req = this;
		
		Log.d(TAG, "onSocket");

		context.nextTick(new NodeContext.nextTickListener() {

			@Override
			public void onNextTick() throws Exception {
				Log.d(TAG, "onNextTick ");

				if (req.aborted > 0) {
					// If we were aborted while waiting for a socket, skip the whole thing.
					socket.emit("free");
				} else {
					Log.d(TAG, "tickOnSocket");

					tickOnSocket(req, socket);
				}
			}

		});

	}

	@Override
	protected void _implicitHeader() throws Exception {
		this._storeHeader(this.method + " " + this.path + " HTTP/1.1\r\n",
				this._renderHeaders());
	}

	private static NodeError createHangUpError() {
		return new NodeError("ECONNRESET", "socket hang up");
	}

	/**
	 * @return the parser
	 */
	public IncomingParser getParser() {
		return parser;
	}

	/**
	 * @param parser the parser to set
	 */
	public void setParser(IncomingParser parser) {
		this.parser = parser;
	}

	private class socketCloseListener 
	implements Listener {
		private TCP.Socket  socket;

		public socketCloseListener(NodeContext ctx, TCP.Socket socket) {
			this.socket  = socket;
		}
		@SuppressWarnings("unused")
		private socketCloseListener(){}

		@Override
		public void onEvent(Object data) throws Exception {
			///var socket = this;
			final ClientRequest req = (ClientRequest)socket.get_httpMessage();
			Log.d(TAG, "HTTP socket close");

			// Pull through final chunk, if anything is buffered.
			// the ondata function will handle it properly, and this
			// is a no-op if no final chunk remains.
			///socket.read();
			socket.read(-1);

			// NOTE: Its important to get parser here, because it could be freed by
			// the `socketOnData`.
			parserOnIncomingClient parser = (parserOnIncomingClient) socket.getParser();
			req.emit("close");
			if (req.res!=null && req.res.readable()) {
				// AbstractSocket closed before we emitted 'end' below.
				req.res.emit("aborted");
				final IncomingMessage res = req.res;
				res.on("end", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						res.emit("close");
					}

				});
				res.push(null, null);
			} else if (req.res==null && !req.socket.is_hadError()) {
				// This socket error fired before we started to
				// receive a response. The error needs to
				// fire on the request.
				req.emit("error", createHangUpError());
				req.socket.set_hadError(true);
			}

			// Too bad.  That output wasn't getting written.
			// This is pretty terrible that it doesn't raise an error.
			// Fixed better in v0.10
			if (req.output != null)
				///req.output.length = 0;
				req.output.clear();

			if (req.outputEncodings != null)
				///req.outputEncodings.length = 0;
				req.outputEncodings.clear();

			if (parser != null) {
				parser.Finish();
				IncomingParser.freeParser(parser, req);
			}
		}

	}
	private class socketErrorListener 
	implements Listener {
		private TCP.Socket  socket;

		public socketErrorListener(NodeContext ctx, TCP.Socket socket) {
			this.socket  = socket;
		}
		@SuppressWarnings("unused")
		private socketErrorListener(){}

		@Override
		public void onEvent(Object err) throws Exception {
			///var socket = this;
			parserOnIncomingClient parser = (parserOnIncomingClient) socket.getParser();
			ClientRequest req = (ClientRequest) socket.get_httpMessage();
			Log.d(TAG, "SOCKET ERROR: " + err);/// err.message, err.stack);

			if (req != null) {
				req.emit("error", err);
				// For Safety. Some additional errors might fire later on
				// and we need to make sure we don't double-fire the error event.
				req.socket.set_hadError(true);
			}

			if (parser != null) {
				parser.Finish();
				IncomingParser.freeParser(parser, req);
			}
			socket.destroy(null);
		}

	}


	private class socketOnEnd
	implements Listener {
		private TCP.Socket  socket;

		public socketOnEnd(NodeContext ctx, TCP.Socket socket) {
			this.socket  = socket;
		}
		@SuppressWarnings("unused")
		private socketOnEnd(){}

		@Override
		public void onEvent(Object data) throws Exception {
			///var socket = this;
			ClientRequest req = (ClientRequest) socket.get_httpMessage();
			parserOnIncomingClient parser = (parserOnIncomingClient) socket.getParser();

			if (req.res==null && !req.socket.is_hadError()) {
				// If we don't have a response then we know that the socket
				// ended prematurely and we need to emit an error on the request.
				req.emit("error", createHangUpError());
				req.socket.set_hadError(true);
			}
			if (parser != null) {
				parser.Finish();
				IncomingParser.freeParser(parser, req);
			}
			socket.destroy(null);
		}
	}


	private class socketOnData
	implements Listener {
		private TCP.Socket  socket;

		public socketOnData(NodeContext ctx, TCP.Socket socket) {
			this.socket  = socket;
		}
		@SuppressWarnings("unused")
		private socketOnData(){}

		@Override
		public void onEvent(Object raw) throws Exception {
			ByteBuffer d = (ByteBuffer)raw;
			///var socket = this;
			ClientRequest req = (ClientRequest) socket.get_httpMessage();
			parserOnIncomingClient parser = (parserOnIncomingClient) socket.getParser();

			assert(parser!=null && parser.socket == socket);

			int ret = parser.Execute(d);
			if (ret < 0/*ret instanceof Error*/) {
				Log.d(TAG, "parse error");
				IncomingParser.freeParser(parser, req);
				socket.destroy(null);
				req.emit("error", "parse error");
				req.socket.set_hadError(true);
			} else if (parser.incoming!=null && parser.incoming.isUpgrade()) {
				// Upgrade or CONNECT
				int bytesParsed = ret;
				IncomingMessage res = parser.incoming;
				req.res = res;

				socket.removeListener("data", socketOnData);
				socket.removeListener("end", socketOnEnd);
				parser.Finish();

				ByteBuffer bodyHead = (ByteBuffer)Util.chunkSlice(d, bytesParsed);// d.slice(bytesParsed, d.length);

				String eventName = req.method == "CONNECT" ? "connect" : "upgrade";
				if (req.listenerCount(eventName) > 0) {
					req.upgradeOrConnect = true;

					// detach the socket
					socket.emit("agentRemove");
					socket.removeListener("close", socketCloseListener);
					socket.removeListener("error", socketErrorListener);

					// TODO(isaacs): Need a way to reset a stream to fresh state
					// IE, not flowing, and not explicitly paused.
					socket.get_readableState().setFlowing(TripleState.MAYBE);

					req.emit(eventName, new Http.response_socket_head_b(res, socket, bodyHead));
					req.emit("close");
				} else {
					// Got Upgrade header or CONNECT method, but have no handler.
					socket.destroy(null);
				}
				IncomingParser.freeParser(parser, req);
			} else if (parser.incoming!=null && parser.incoming.isComplete() &&
					// When the status code is 100 (Continue), the server will
					// send a final response after this client sends a request
					// body. So, we must not free the parser.
					parser.incoming.statusCode() != 100) {
				socket.removeListener("data", socketOnData);
				socket.removeListener("end", socketOnEnd);
				IncomingParser.freeParser(parser, req);
			}
		}
	}

}
