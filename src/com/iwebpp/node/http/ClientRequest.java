package com.iwebpp.node.http;

import java.nio.ByteBuffer;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.nextTickCallback;
import com.iwebpp.node.NodeError;
import com.iwebpp.node.TCP;
import com.iwebpp.node.TCP.Socket;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.HttpParser.http_parser_type;
import com.iwebpp.node.Writable2;
import com.iwebpp.node.http.http.response_socket_head_t;

public class ClientRequest 
extends OutgoingMessage {
	private final static String TAG = "ClientRequest";

	public IncomingParser parser;

	public IncomingMessage res;

	public String method;

	public boolean upgradeOrConnect;

	protected TCP.Socket socket;
	
	private socketCloseListener socketCloseListener;

	private int maxHeadersCount = 4000;

	protected boolean aborted;

	protected ClientRequest(NodeContext context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void _write(Object chunk, String encoding, WriteCB cb)
			throws Exception {
		// TODO Auto-generated method stub

	}

	///request.write(chunk, [encoding])
	public boolean write(Object chunk, String encoding) {

		return false;
	}

	///request.end([data], [encoding])
	public void end(Object data, String encoding) {

	}

	public void abort() {

	}

	///request.setTimeout(timeout, [callback])

	public int setNoDelay(boolean noDelay) {

		return 0;
	}

	public int setSocketKeepAlive(boolean enable, int initialDelay) {

		return 0;
	}

	// Event listeners
	public void onResponse(final responseListener cb) throws Exception {
		this.on("response", new Listener(){

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

	public void onSocket(final socketListener cb) throws Exception {
		this.on("socket", new Listener(){

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

	public void onConnect(final connectListener cb) throws Exception {
		this.on("connect", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				response_socket_head_t data = (response_socket_head_t)raw;

				cb.onConnect(data.response, data.socket, data.head);
			}

		});
	}
	public static interface connectListener {
		public void onConnect(IncomingMessage res, TCP.Socket socket, ByteBuffer head) throws Exception;
	}

	public void onUpgrade(final upgradeListener cb) throws Exception {
		this.on("upgrade", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {      
				response_socket_head_t data = (response_socket_head_t)raw;

				cb.onUpgrade(data.response, data.socket, data.head);
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
		private NodeContext context;

		public parserOnIncomingClient(NodeContext ctx, TCP.Socket socket) {
			super(ctx, http_parser_type.HTTP_RESPONSE, socket);
			this.context = ctx;
		}
        private parserOnIncomingClient(){}
		
		@Override
		protected boolean onIncoming(final IncomingMessage res,
				boolean shouldKeepAlive) throws Exception {
			Socket socket = this.socket;
			final ClientRequest req = (ClientRequest)socket._httpMessage;


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
				// Server MUST respond with Connection:keep-alive for us to enable it.
				// If we've been upgraded (via WebSockets) we also shouldn't try to
				// keep the connection open.
				req.shouldKeepAlive = false;
			}


			///DTRACE_HTTP_CLIENT_RESPONSE(socket, req);
			///COUNTER_HTTP_CLIENT_RESPONSE();
			req.res = res;
			res.req = req;
			
			// add our listener first, so that we guarantee socket cleanup
			Listener responseOnEnd = new Listener() {

				@Override
				public void onEvent(Object data) throws Exception {
					///var res = this;
					ClientRequest req = res.req;
					final Socket socket = req.socket;

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
						context.nextTick(new nextTickCallback() {

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
		req.parser = parser;

		socket.parser = parser;
		socket._httpMessage = req;

		// Setup "drain" propogation.
		http.httpSocketSetup(socket);

		// Propagate headers limit from request object to parser
		if (req.maxHeadersCount  > 0/*util.isNumber(req.maxHeadersCount)*/) {
			parser.maxHeaderPairs = req.maxHeadersCount << 1;
		} else {
			// Set default value because parser may be reused from FreeList
			parser.maxHeaderPairs = 2000;
		}

		///parser.onIncoming = parserOnIncomingClient;
		socket.on("error", socketErrorListener);
		socket.on("data", socketOnData);
		socket.on("end", socketOnEnd);
		
		this.socketCloseListener = new socketCloseListener(context, socket);
		socket.on("close", socketCloseListener);
		
		req.emit("socket", socket);
}

	public void onSocket(final TCP.Socket socket) {
		final ClientRequest req = this;

		context.nextTick(new NodeContext.nextTickCallback() {

			@Override
			public void onNextTick() throws Exception {
				if (req.aborted) {
					// If we were aborted while waiting for a socket, skip the whole thing.
					socket.emit("free");
				} else {
					tickOnSocket(req, socket);
				}
			}

		});

	}

	@Override
	protected void _implicitHeader() {
		// TODO Auto-generated method stub
		
	}

	private static NodeError createHangUpError() {
		return new NodeError("ECONNRESET", "socket hang up");
	}
	
	private class socketCloseListener 
	implements Listener {
		private NodeContext context;
		private TCP.Socket  socket;

		public socketCloseListener(NodeContext ctx, TCP.Socket socket) {
			this.context = ctx;
			this.socket  = socket;
		}
		private socketCloseListener(){}

		@Override
		public void onEvent(Object data) throws Exception {
			///var socket = this;
			final ClientRequest req = (ClientRequest)socket._httpMessage;
			Log.d(TAG, "HTTP socket close");

			// Pull through final chunk, if anything is buffered.
			// the ondata function will handle it properly, and this
			// is a no-op if no final chunk remains.
			///socket.read();
			socket.read(-1);

			// NOTE: Its important to get parser here, because it could be freed by
			// the `socketOnData`.
			parserOnIncomingClient parser = (parserOnIncomingClient) socket.parser;
			req.emit("close");
			if (req.res!=null && req.res.readable()) {
				// Socket closed before we emitted 'end' below.
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

/*
function socketErrorListener(err) {
  var socket = this;
  var parser = socket.parser;
  var req = socket._httpMessage;
  debug('SOCKET ERROR:', err.message, err.stack);

  if (req) {
    req.emit('error', err);
    // For Safety. Some additional errors might fire later on
    // and we need to make sure we don't double-fire the error event.
    req.socket._hadError = true;
  }

  if (parser) {
    parser.finish();
    freeParser(parser, req);
  }
  socket.destroy();
}

function socketOnEnd() {
  var socket = this;
  var req = this._httpMessage;
  var parser = this.parser;

  if (!req.res && !req.socket._hadError) {
    // If we don't have a response then we know that the socket
    // ended prematurely and we need to emit an error on the request.
    req.emit('error', createHangUpError());
    req.socket._hadError = true;
  }
  if (parser) {
    parser.finish();
    freeParser(parser, req);
  }
  socket.destroy();
}

function socketOnData(d) {
  var socket = this;
  var req = this._httpMessage;
  var parser = this.parser;

  assert(parser && parser.socket === socket);

  var ret = parser.execute(d);
  if (ret instanceof Error) {
    debug('parse error');
    freeParser(parser, req);
    socket.destroy();
    req.emit('error', ret);
    req.socket._hadError = true;
  } else if (parser.incoming && parser.incoming.upgrade) {
    // Upgrade or CONNECT
    var bytesParsed = ret;
    var res = parser.incoming;
    req.res = res;

    socket.removeListener('data', socketOnData);
    socket.removeListener('end', socketOnEnd);
    parser.finish();

    var bodyHead = d.slice(bytesParsed, d.length);

    var eventName = req.method === 'CONNECT' ? 'connect' : 'upgrade';
    if (EventEmitter.listenerCount(req, eventName) > 0) {
      req.upgradeOrConnect = true;

      // detach the socket
      socket.emit('agentRemove');
      socket.removeListener('close', socketCloseListener);
      socket.removeListener('error', socketErrorListener);

      // TODO(isaacs): Need a way to reset a stream to fresh state
      // IE, not flowing, and not explicitly paused.
      socket._readableState.flowing = null;

      req.emit(eventName, res, socket, bodyHead);
      req.emit('close');
    } else {
      // Got Upgrade header or CONNECT method, but have no handler.
      socket.destroy();
    }
    freeParser(parser, req);
  } else if (parser.incoming && parser.incoming.complete &&
             // When the status code is 100 (Continue), the server will
             // send a final response after this client sends a request
             // body. So, we must not free the parser.
             parser.incoming.statusCode !== 100) {
    socket.removeListener('data', socketOnData);
    socket.removeListener('end', socketOnEnd);
    freeParser(parser, req);
  }
}
*/

}
