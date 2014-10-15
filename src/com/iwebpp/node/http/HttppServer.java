package com.iwebpp.node.http;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.HttpParser.http_parser_type;
import com.iwebpp.node.http.HttpServer.checkContinueListener;
import com.iwebpp.node.http.HttpServer.clientErrorListener;
import com.iwebpp.node.http.HttpServer.closeListener;
import com.iwebpp.node.http.HttpServer.connectListener;
import com.iwebpp.node.http.HttpServer.connectionListener;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.HttpServer.upgradeListener;
import com.iwebpp.node.http.http.exception_socket_b;
import com.iwebpp.node.http.http.request_response_b;
import com.iwebpp.node.http.http.request_socket_head_b;
import com.iwebpp.node.net.AbstractServer;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.others.TripleState;
import com.iwebpp.node.net.UDT;

public final class HttppServer 
extends UDT.Server {

	private boolean httpAllowHalfOpen;
	private NodeContext context;
	
	private static class connectionListenerImpl 
	implements connectionListener {
		private final static String TAG = "connectionListenerImpl";

		private HttppServer self;

		private NodeContext context;

		private int maxHeadersCount = 4000;

		public connectionListenerImpl(NodeContext ctx, HttppServer srv) {
			this.context = ctx;
			this.self = srv;
		}
		@SuppressWarnings("unused")
		private connectionListenerImpl(){}

		@Override
		public void onConnection(final AbstractSocket socket) throws Exception {
			
			Log.d(TAG, "SERVER new HTTPP connection");

			http.httpSocketSetup(socket);

			// If the user has added a listener to the server,
			// request, or response, then it's their responsibility.
			// otherwise, destroy on timeout by default
			// TBD...
			/*if (self.timeout > 0)
				socket.setTimeout(self.timeout);
			
			socket.on('timeout', function() {
				var req = socket.parser && socket.parser.incoming;
				var reqTimeout = req && !req.complete && req.emit('timeout', socket);
				var res = socket._httpMessage;
				var resTimeout = res && res.emit('timeout', socket);
				var serverTimeout = self.emit('timeout', socket);

				if (!reqTimeout && !resTimeout && !serverTimeout)
					socket.destroy();
			});*/

			///var parser = parsers.alloc();
			final parserOnIncoming parser = new parserOnIncoming(context, self, socket);
					
			parser.Reinitialize(http_parser_type.HTTP_REQUEST);
			parser.socket = socket;
			socket.setParser(parser);
			parser.incoming = null;

			// Propagate headers limit from server instance to parser
			///if (util.isNumber(this.maxHeadersCount)) {
			if (this.maxHeadersCount  > 0) {
				parser.maxHeaderPairs = this.maxHeadersCount << 1;
			} else {
				// Set default value because parser may be reused from FreeList
				parser.maxHeaderPairs = 2000;
			}

			final Listener serverSocketCloseListener = new Listener() {

				@Override
				public void onEvent(Object data) throws Exception {
					Log.d(TAG, "server socket close");
					// mark this parser as reusable
					if (socket.getParser() != null)
						IncomingParser.freeParser(socket.getParser(), null);

					parser.abortIncoming();
				}
				
			};
			
			// TODO(isaacs): Move all these functions out of here
			Listener socketOnError = new Listener() {

				@Override
				public void onEvent(Object e) throws Exception {
					// TODO Auto-generated method stub
					self.emit("clientError", new exception_socket_b(e!=null? e.toString() : null, socket));
				}
				
			};

			final Listener socketOnEnd = new Listener(){
				public void onEvent(final Object data) throws Exception {
					///var socket = this;
					int ret = parser.Finish();

					if (ret != 0/*instanceof Error*/) {
						Log.d(TAG, "parse error");
						socket.destroy("parse error");
						return;
					}

					if (!self.httpAllowHalfOpen) {
						parser.abortIncoming();
						if (socket.writable()) socket.end(null, null, null);
					} else if (parser.outgoings.size() > 0) {
						///outgoing[outgoing.length - 1]._last = true;
						parser.outgoings.get(parser.outgoings.size()-1).set_last(true);
					} else if (socket.get_httpMessage() != null) {
						ServerResponse srvres = (ServerResponse)(socket.get_httpMessage());
						srvres._last = true;
					} else {
						if (socket.writable()) socket.end(null, null, null);
					}
				}
			};
			
			final Listener socketOnData = new Listener() {

				@Override
				public void onEvent(Object raw) throws Exception {
					if (!Util.isBuffer(raw)) throw new Exception("onData Not ByteBuffer");
					
                    ByteBuffer d = (ByteBuffer)raw;
					
					assert(!socket.is_paused());
					
					Log.d(TAG, "SERVER socketOnData " + Util.chunkLength(d));
					Log.d(TAG, "\t\t\t"+Util.chunkToString(d, "utf-8"));
					
					int ret = parser.Execute(d);
					
					if (ret < 0 /*instanceof Error*/) {
						Log.d(TAG, "parse error");
						socket.destroy("parse error");
					} else if (parser.incoming!=null && parser.incoming.isUpgrade()) {
						// Upgrade or CONNECT
						int bytesParsed = ret;
						IncomingMessage req = parser.incoming;
						Log.d(TAG, "SERVER upgrade or connect " + req.method());

						socket.removeListener("data", this);
						socket.removeListener("end", socketOnEnd);
						socket.removeListener("close", serverSocketCloseListener);
						parser.Finish();
						///freeParser(parser, req);
						IncomingParser.freeParser(parser, req);

						String eventName = req.method() == "CONNECT" ? "connect" : "upgrade";
						if (self.listenerCount(eventName) > 0) {
							Log.d(TAG, "SERVER have listener for " + eventName);
							///var bodyHead = d.slice(bytesParsed, d.length);
							ByteBuffer bodyHead = (ByteBuffer) Util.chunkSlice(d, bytesParsed, d.capacity());

							// TODO(isaacs): Need a way to reset a stream to fresh state
							// IE, not flowing, and not explicitly paused.
							socket.get_readableState().setFlowing(TripleState.MAYBE);
							self.emit(eventName, new request_socket_head_b(req, socket, bodyHead));
						} else {
							// Got upgrade header or CONNECT method, but have no handler.
							socket.destroy(null);
						}
					}

					if (socket.is_paused()) {
						// onIncoming paused the socket, we should pause the parser as well
						Log.d(TAG, "pause parser");
						///socket.parser.pause();
						socket.getParser().Pause(false);
					}					
				}
				
			};
				
			// The following callback is issued after the headers have been read on a
			// new message. In this callback we setup the response object and pass it
			// to the user.

			socket.set_paused(false);
			
			Listener socketOnDrain = new Listener() {
				public void onEvent(final Object data) throws Exception {
					// If we previously paused, then start reading again.
					if (socket.is_paused()) {
						socket.set_paused(false);
						// TDB...
						///socket.parser.resume();
						socket.getParser().Pause(false);
						socket.resume();
					}
				}
			};
			socket.on("drain", socketOnDrain);
			
			socket.addListener("error", socketOnError);
			socket.addListener("close", serverSocketCloseListener);
			///parser.onIncoming = parserOnIncoming;
			socket.on("end", socketOnEnd);
			
			// set flowing ??? TBD...
			socket.get_readableState().setFlowing(TripleState.TRUE);
			socket.on("data", socketOnData);
		}

	}
	
	public HttppServer(NodeContext ctx) throws Exception {
		super(ctx, new AbstractServer.Options(false), null);
		
		this.context = ctx;

		// Similar option to this. Too lazy to write my own docs.
		// http://www.squid-cache.org/Doc/config/half_closed_clients/
		// http://wiki.squid-cache.org/SquidFaq/InnerWorkings#What_is_a_half-closed_filedescriptor.3F
		this.httpAllowHalfOpen = false;

		///this.addListener('connection', connectionListener);
		this.onConnection(new connectionListenerImpl(context, this));

		/*this.addListener('clientError', function(err, conn) {
			conn.destroy(err);
		});*/
		this.onClientError(new clientErrorListener(){

			@Override
			public void onClientError(String err, AbstractSocket conn)
					throws Exception {
				// TODO Auto-generated method stub
				conn.destroy(err);
			}
			
		});
	}
	
	public HttppServer(NodeContext ctx, requestListener onreq) throws Exception {
		super(ctx, new AbstractServer.Options(false), null);

		this.context = ctx;

		// Similar option to this. Too lazy to write my own docs.
		// http://www.squid-cache.org/Doc/config/half_closed_clients/
		// http://wiki.squid-cache.org/SquidFaq/InnerWorkings#What_is_a_half-closed_filedescriptor.3F
		this.httpAllowHalfOpen = false;

		///this.addListener('connection', connectionListener);
		this.onConnection(new connectionListenerImpl(context, this));

		/*this.addListener('clientError', function(err, conn) {
			conn.destroy(err);
		});*/
		this.onClientError(new clientErrorListener(){

			@Override
			public void onClientError(String err, AbstractSocket conn)
					throws Exception {
				// TODO Auto-generated method stub
				conn.destroy(err);
			}
			
		});

		if (onreq != null) this.onRequest(onreq);
	}
	
	///server.listen(port, [hostname], [backlog], [callback])
	public HttppServer listen(
			int port, 
			String hostname, 
			int backlog, 
			ListeningCallback cb) throws Exception {
		if (cb != null) onListening(cb);
          
		super.listen(hostname, port, Util.ipFamily(hostname), backlog, -1, null);
		
		return this;
	}
	
	public HttppServer listen(
			int port, 
			String hostname,
			ListeningCallback cb) throws Exception {
		return listen(port, hostname, 256, cb);
	}
	
	public HttppServer listen(
			int port, 
			String hostname) throws Exception {
		return listen(port, hostname, 256, null);
	}
	
	/*public void close(final closeListener cb) throws Exception {
		if (cb != null) onClose(cb);

		super.close(null);
	}*/

	public int maxHeadersCount(int max) {
		return 2000;
	}

	///server.setTimeout(msecs, callback)
	///server.timeout

	// Event listeners
	public void onListening(final ListeningCallback cb) throws Exception {
		this.on("listening", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				cb.onListening();
			}

		});
	}
	public void onRequest(final requestListener cb) throws Exception {
		this.on("request", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				request_response_b data = (request_response_b)raw;

				cb.onRequest(data.getRequest(), data.getResponse());
			}

		});
	}
	public void onConnection(final connectionListener cb) throws Exception {
		this.on("connection", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				AbstractSocket data = (AbstractSocket)raw;

				cb.onConnection(data);
			}

		});
	}
	public void onClose(final closeListener cb) throws Exception {
		this.on("close", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {                   
				cb.onClose();
			}

		});
	}
	public void onCheckContinue(final checkContinueListener cb) throws Exception {
		this.on("checkContinue", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				request_response_b data = (request_response_b)raw;

				cb.onCheckContinue(data.getRequest(), data.getResponse());
			}

		});
	}
	public void onConnect(final connectListener cb) throws Exception {
		this.on("connect", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				request_socket_head_b data = (request_socket_head_b)raw;

				cb.onConnect(data.getRequest(), data.getSocket(), data.getHead());
			}

		});
	}
	public void onUpgrade(final upgradeListener cb) throws Exception {
		this.on("upgrade", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				request_socket_head_b data = (request_socket_head_b)raw;

				cb.onUpgrade(data.getRequest(), data.getSocket(), data.getHead());
			}

		});
	}
	public void onClientError(final clientErrorListener cb) throws Exception {
		this.on("clientError", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				exception_socket_b data = (exception_socket_b)raw;

				cb.onClientError(data.getException(), data.getSocket());
			}

		});
	}
	// Parser on request
	private static class parserOnIncoming 
	extends IncomingParser {
		private static final String TAG = "parserOnIncoming";
		
		private NodeContext context;

		private List<IncomingMessage> incomings;	
		private List<ServerResponse> outgoings;

		private HttppServer self;
	
	
		public parserOnIncoming(NodeContext ctx, HttppServer srv, AbstractSocket socket) {
			super(ctx, http_parser_type.HTTP_REQUEST, socket);
			this.context = ctx;
			this.self    = srv;
	
			incomings = new LinkedList<IncomingMessage>();
			outgoings = new LinkedList<ServerResponse>();

		}
		private parserOnIncoming() {super(null, null, null);}
	
		@Override
		protected boolean onIncoming(final IncomingMessage req,
				boolean shouldKeepAlive) throws Exception {
			final IncomingParser ips = this;
			
			incomings.add(req);
	
			// If the writable end isn't consuming, then stop reading
			// so that we don't become overwhelmed by a flood of
			// pipelined requests that may never be resolved.
			if (!socket.is_paused()) {
				boolean needPause = socket.get_writableState().isNeedDrain();
				if (needPause) {
				    socket.set_paused(true);
					// We also need to pause the parser, but don't do that until after
					// the call to execute, because we may still be processing the last
					// chunk.
					socket.pause();
				}
			}
	
			// TBD...
			final ServerResponse res = new ServerResponse(context, req);
	
			res.setShouldKeepAlive(shouldKeepAlive);
			//DTRACE_HTTP_SERVER_REQUEST(req, socket);
			//COUNTER_HTTP_SERVER_REQUEST();
	
			if (socket.get_httpMessage() != null) {
				// There are already pending outgoing res, append.
				outgoings.add(res);
				
				Log.d(TAG, "outgoings.add(res)");
			} else {
				res.assignSocket(socket);
				
				Log.d(TAG, "res.assignSocket(socket)");
			}
	
			// When we're finished writing the response, check if this is the last
			// response, if so destroy the socket.
			Listener resOnFinish = new Listener() {
	
				@Override
				public void onEvent(Object data) throws Exception {
					// Usually the first incoming element should be our request.  it may
					// be that in the case abortIncoming() was called that the incoming
					// array will be empty.
					assert(incomings.size() == 0 || incomings.get(0) == req);
	
					if (incomings.size() > 0) 
						incomings.remove(0);
	
					// if the user never called req.read(), and didn't pipe() or
					// .resume() or .on('data'), then we call req._dump() so that the
					// bytes will be pulled off the wire.
					if (!req.is_consuming() && !req.get_readableState().isResumeScheduled())
						req._dump();
	
					res.detachSocket(socket);
					Log.d(TAG, "res.detachSocket(socket)");

					// Reset Parser state 
					ips.Reinitialize(ips.getType());
					
					if (res.is_last()) {
						Log.d(TAG, "res.is_last()");

						socket.destroySoon();
					} else {
						// start sending the next message
						ServerResponse m = outgoings.remove(0);
						if (m != null) {
							m.assignSocket(socket);
						}
					}
				}
	
			};			
			res.on("prefinish", resOnFinish);
	
			if ( req.getHeaders().containsKey("expect") && 
				!req.getHeaders().get("expect").isEmpty() &&
				(req.getHttpVersionMajor() == 1 && req.getHttpVersionMinor() == 1) &&
				///http.continueExpression == req.headers.get("expect").get(0)) {
				Pattern.matches(http.continueExpression, req.getHeaders().get("expect").get(0))) {
				res.set_expect_continue(true);

				if (self.listenerCount("checkContinue") > 0) {
					self.emit("checkContinue", new request_response_b(req, res));
				} else {
					res.writeContinue(null);
					self.emit("request", new request_response_b(req, res));
				}
			} else {
				self.emit("request",  new request_response_b(req, res));
			}
	
			return false; // Not a HEAD response. (Not even a response!)
		}
		
		public void abortIncoming() throws Exception {
			while (incomings.size() > 0) {
				IncomingMessage req = incomings.remove(0);
				req.emit("aborted");
				req.emit("close");
			}
			// abort socket._httpMessage ?
		}
		
	}

}
