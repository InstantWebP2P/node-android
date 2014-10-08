// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>

package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.util.Base64;
import android.util.Log;

import com.iwebpp.libuvpp.handles.TimerHandle;
import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.TimeoutListener;
import com.iwebpp.node.NodeContext.nextTickListener;
import com.iwebpp.node.Url;
import com.iwebpp.node.http.Agent;
import com.iwebpp.node.http.ClientRequest;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ReqOptions;
import com.iwebpp.node.http.http;
import com.iwebpp.node.http.httpp;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.stream.Readable2;
import com.iwebpp.node.stream.Writable.WriteCB;
import com.iwebpp.wspp.Receiver.opcOptions;
import com.iwebpp.wspp.Sender.SendOptions;

/**
 * WebSocket implementation
 */
public class WebSocket 
extends EventEmitter2 {

	private static final String TAG = "WebSocket";

	/**
	 * Constants
	 */

	// Default protocol version

	public static final int ProtocolVersion = 13;

	// Close timeout

	public static final int CloseTimeout = 30000; // Allow 5 seconds to terminate the connection cleanly


	/**
	 * Ready States
	 */

	/*["CONNECTING", "OPEN", "CLOSING", "CLOSED"].forEach(function (state, index) {
    WebSocket.prototype[state] = WebSocket[state] = index;
});*/

	public static final int CONNECTING = 0;
	public static final int       OPEN = 1;
	public static final int    CLOSING = 2;
	public static final int     CLOSED = 3;

	public static class Options {

		public String origin = null;
		public int protocolVersion = WebSocket.ProtocolVersion;
		public String host = null;
		public Map<String, String> headers = null;
		public String protocol = null;
		public Agent agent = null;

		// ssl-related options
		public String pfx = null;
		public String key = null;
		public String passphrase = null;
		public String cert = null;
		public String ca = null;
		public String ciphers = null;
		public boolean rejectUnauthorized = false;

		// httpp-related options
		///hole: {port: -1},
		///httpp: false // default as HTTP not HTTPP
		public boolean httpp = false;
		public int localPort = -1;
		public String localAddress = null;
		
		public Options() {
			this.headers = new Hashtable<String, String>();
		}

	}

	private AbstractSocket _socket;

	private NodeContext context;

	private int bytesReceived;

	private int readyState;

	private Map<String, Boolean> supports;

	private int _closeCode;

	private String _closeMessage;

	private boolean _isServer;

	private Sender _sender;

	private Receiver _receiver;

	private Listener cleanupWebsocketResources;

	private List<Sendor> _queue;

	private IncomingMessage upgradeReq;

	private String protocol;

	private int protocolVersion;

	private String url;

	private TimerHandle _closeTimer;

	public WebSocket(NodeContext ctx, String address, List<String> protocols, Options options) throws Exception {
		this.context = ctx;
		final WebSocket self = this;

		/*
    	  if (protocols && !Array.isArray(protocols) && 'object' == typeof protocols) {
    	    // accept the "options" Object as the 2nd argument
    	    options = protocols;
    	    protocols = null;
    	  }
    	  if ('string' == typeof protocols) {
    	    protocols = [ protocols ];
    	  }
    	  if (!Array.isArray(protocols)) {
    	    protocols = [];
    	  }
    	  // TODO: actually handle the `Sub-Protocols` part of the WebSocket client

    	  this._socket = null;
    	  this.bytesReceived = 0;
    	  this.readyState = null;
    	  this.supports = {};

    	  if (Array.isArray(address)) {
    	    initAsServerClient.apply(this, address.concat(options));
    	  } else {
    	    initAsClient.apply(this, [address, protocols, options]);
    	  }
		 */
		this._socket = null;
		this.bytesReceived = 0;
		this.readyState = -1;/// null;
		this.supports = new Hashtable<String, Boolean>();///{};

		initAsClient(address, protocols, options);

		this.cleanupWebsocketResources = new Listener(){

			@Override
			public void onEvent(Object error) throws Exception {
				Log.d(TAG, "cleanupWebsocketResources:"+error!=null ? error.toString() : "");
				
				
				if (self.readyState == WebSocket.CLOSED) return;
				boolean emitClose = self.readyState != WebSocket.CONNECTING;
				self.readyState = WebSocket.CLOSED;

				// TBD...
				///clearTimeout(this._closeTimer);
				///self._closeTimer = null;

				if (emitClose) self.emit("close", new close_code_b(
						self._closeMessage!=null ?  self._closeMessage : "",
						self._closeCode>0 ?     self._closeCode : 1000)); ///self.emit("close", self._closeCode || 1000, self._closeMessage || "");

				if (self._socket!=null) {
					self._socket.removeAllListeners();
					// catch all socket error after removing all standard handlers
					final AbstractSocket socket = self._socket;

					/*self._socket.on("error", function() {
	    		      try { socket.destroy(); } catch (e) {}
	    		    });*/
					self._socket.on("error", new Listener(){
						public void onEvent(final Object data) throws Exception {
							try { socket.destroy(null); } catch (Exception e) {}
						}
					});

					try {
						if (null==error) self._socket.end(null, null, null);
						else self._socket.destroy(null);
					}
					catch (Exception e) { /* Ignore termination errors */ }
					self._socket = null;
				}

				if (self._sender!=null) {
					self._sender.removeAllListeners();
					self._sender = null;
				}

				if (self._receiver!=null) {
					self._receiver.cleanup();
					self._receiver = null;
				}

				self.removeAllListeners();
				self.on("error", new Listener(){
					public void onEvent(final Object data) throws Exception {

					}
				});///function() {}); // catch all errors after this
				/// TBD...
				///delete this._queue;
			}

		};

	}

	protected WebSocket(NodeContext ctx, http.request_socket_head_b rsh, Options options) throws Exception {
		this.context = ctx;
		final WebSocket self = this;

		/*
    	  if (protocols && !Array.isArray(protocols) && 'object' == typeof protocols) {
    	    // accept the "options" Object as the 2nd argument
    	    options = protocols;
    	    protocols = null;
    	  }
    	  if ('string' == typeof protocols) {
    	    protocols = [ protocols ];
    	  }
    	  if (!Array.isArray(protocols)) {
    	    protocols = [];
    	  }
    	  // TODO: actually handle the `Sub-Protocols` part of the WebSocket client

    	  this._socket = null;
    	  this.bytesReceived = 0;
    	  this.readyState = null;
    	  this.supports = {};

    	  if (Array.isArray(address)) {
    	    initAsServerClient.apply(this, address.concat(options));
    	  } else {
    	    initAsClient.apply(this, [address, protocols, options]);
    	  }
		 */
		this._socket = null;
		this.bytesReceived = 0;
		this.readyState = -1;/// null;
		this.supports = new Hashtable<String, Boolean>();///{};

		initAsServerClient(rsh.getRequest(), rsh.getSocket(), rsh.getHead(), options);

		this.cleanupWebsocketResources = new Listener(){

			@Override
			public void onEvent(Object error) throws Exception {
				if (self.readyState == WebSocket.CLOSED) return;
				boolean emitClose = self.readyState != WebSocket.CONNECTING;
				self.readyState = WebSocket.CLOSED;

				// TBD...
				///clearTimeout(this._closeTimer);
				///self._closeTimer = null;

				if (emitClose) self.emit("close", new close_code_b(
						self._closeMessage!=null ?  self._closeMessage : "",
								self._closeCode>0 ?     self._closeCode : 1000)); ///self.emit("close", self._closeCode || 1000, self._closeMessage || "");

				if (self._socket!=null) {
					self._socket.removeAllListeners();
					// catch all socket error after removing all standard handlers
					final AbstractSocket socket = self._socket;

					/*self._socket.on("error", function() {
	    		      try { socket.destroy(); } catch (e) {}
	    		    });*/
					self._socket.on("error", new Listener(){
						public void onEvent(final Object data) throws Exception {
							try { socket.destroy(null); } catch (Exception e) {}
						}
					});

					try {
						if (null==error) self._socket.end(null, null, null);
						else self._socket.destroy(null);
					}
					catch (Exception e) { /* Ignore termination errors */ }
					self._socket = null;
				}

				if (self._sender!=null) {
					self._sender.removeAllListeners();
					self._sender = null;
				}

				if (self._receiver!=null) {
					self._receiver.cleanup();
					self._receiver = null;
				}

				self.removeAllListeners();
				self.on("error", new Listener(){
					public void onEvent(final Object data) throws Exception {

					}
				});///function() {}); // catch all errors after this
				/// TBD...
				///delete this._queue;
			}

		};

	}

	@SuppressWarnings("unused")
	private WebSocket(){}

	/**
	 * Gracefully closes the connection, after sending a description message to the server
	 *
	 * @param {Object} data to be sent to the server
	 * @throws Exception 
	 * @api public
	 */

	public void close(int code, Object data) throws Exception {
		if (this.readyState == WebSocket.CLOSING || this.readyState == WebSocket.CLOSED) return;
		if (this.readyState == WebSocket.CONNECTING) {
			this.readyState = WebSocket.CLOSED;
			return;
		}
		try {
			this.readyState = WebSocket.CLOSING;
			this._closeCode = code;
			this._closeMessage = data.toString();
			boolean mask = !this._isServer;
			this._sender.close(code, data, mask);
		}
		catch (Exception e) {
			this.emit("error", e.toString());
		}
		finally {
			this.terminate();
		}
	}

	/**
	 * Pause the client stream
	 * @throws Exception 
	 *
	 * @api public
	 */

	public void pause() throws Exception {
		if (this.readyState != WebSocket.OPEN) throw new Exception("not opened");
		this._socket.pause();
	}

	/**
	 * Sends a ping
	 *
	 * @param {Object} data to be sent to the server
	 * @param {Object} Members - mask: boolean, binary: boolean
	 * @param {boolean} dontFailWhenClosed indicates whether or not to throw if the connection isnt open
	 * @throws Exception 
	 * @api public
	 */

	public void ping(Object data, SendOptions options, boolean dontFailWhenClosed) throws Exception {
		if (this.readyState != WebSocket.OPEN) {
			if (dontFailWhenClosed == true) return;
			throw new Exception("not opened");
		}
		///options = options || {};
		///if (typeof options.mask == 'undefined') options.mask = !this._isServer;
		this._sender.ping(data, options);
	}

	/**
	 * Sends a pong
	 *
	 * @param {Object} data to be sent to the server
	 * @param {Object} Members - mask: boolean, binary: boolean
	 * @param {boolean} dontFailWhenClosed indicates whether or not to throw if the connection isnt open
	 * @throws Exception 
	 * @api public
	 */

	public void pong(Object data, SendOptions options, boolean dontFailWhenClosed) throws Exception {
		if (this.readyState != WebSocket.OPEN) {
			if (dontFailWhenClosed == true) return;
			throw new Exception("not opened");
		}
		///options = options!=null ? options : new SendOptions();
		///if (typeof options.mask == 'undefined') options.mask = !this._isServer;
		this._sender.pong(data, options);
	}

	/**
	 * Resume the client stream
	 * @throws Exception 
	 *
	 * @api public
	 */

	public void resume() throws Exception {
		if (this.readyState != WebSocket.OPEN) throw new Exception("not opened");
		this._socket.resume();
	}

	/**
	 * Sends a piece of data
	 *
	 * @param {Object} data to be sent to the server
	 * @param {Object} Members - mask: boolean, binary: boolean
	 * @param {function} Optional callback which is executed after the send completes
	 * @throws Exception 
	 * @api public
	 */

	public boolean send(final Object raw, final SendOptions options, final WriteCB cb) throws Exception {
		/*if (typeof options == 'function') {
    cb = options;
    options = {};
  }*/

		if (this.readyState != WebSocket.OPEN) {
			/*if (typeof cb == 'function') cb(new Error('not opened'));
    else throw new Error('not opened');*/
			if (cb != null) cb.writeDone("not opened");
			else throw new Exception("not opened");
			return false;
		}

		final Object data;
		if (raw==null) data = "";
		else data = raw;

		if (this._queue != null && !this._queue.isEmpty()) {
			final WebSocket self = this;
			///this._queue.push(function() { self.send(data, options, cb); });
			this._queue.add(new Sendor(){

				@Override
				public void execute() throws Exception {
					self.send(data, options, cb); 			
				}

			});
			return false;
		}

		///options = options!=null ? options : new SendOptions();
		options.fin = true;

		/*
  if (typeof options.binary == 'undefined') {
    options.binary = (data instanceof ArrayBuffer || data instanceof Buffer ||
      data instanceof Uint8Array ||
      data instanceof Uint16Array ||
      data instanceof Uint32Array ||
      data instanceof Int8Array ||
      data instanceof Int16Array ||
      data instanceof Int32Array ||
      data instanceof Float32Array ||
      data instanceof Float64Array);
  }*/
		options.binary = data!=null ? data instanceof ByteBuffer : options.binary;


		///if (typeof options.mask == 'undefined') options.mask = !this._isServer;

		///var readable = typeof stream.Readable == 'function' ? stream.Readable : stream.Stream;

		// TBD...
		///if (data instanceof readable) {
		if (data instanceof Readable2) {
			startQueue();

			final WebSocket self = this;

			sendStream(data, options, new WriteCB(){

				public void writeDone(String error) throws Exception {
					///process.nextTick(function() { executeQueueSends(self); });
					///if (typeof cb == 'function') cb(error);

					context.nextTick(new nextTickListener(){

						@Override
						public void onNextTick() throws Exception {
							executeQueueSends();					
						}

					});

					if (cb != null) cb.writeDone(error);
				}

			});

			return true;
		}
		else return this._sender.send(data, options, cb);
	}

	private interface Sendor {
		void execute() throws Exception;
	}

	private void startQueue() {
		final WebSocket instance = this;

		instance._queue = instance._queue!=null ? instance._queue : new LinkedList<Sendor>();
	}

	private void executeQueueSends() throws Exception {
		final WebSocket instance = this;

		List<Sendor> queue = instance._queue;
		///if (typeof queue == 'undefined') return;
		if (queue==null || queue.size()==0) return;
		/*delete instance._queue;
  for (var i = 0, l = queue.length; i < l; ++i) {
    queue[i]();
  }*/
		for (Sendor l : queue)
			l.execute();
		instance._queue.clear();
	}

	private void sendStream(Object data, final SendOptions options, final WriteCB cb) throws Exception {
		final WebSocket instance = this;

		/*stream.on('data', function(data) {
    if (instance.readyState != WebSocket.OPEN) {
      if (typeof cb == 'function') cb(new Error('not opened'));
      else {
        delete instance._queue;
        instance.emit('error', new Error('not opened'));
      }
      return;
    }
    options.fin = false;
    instance._sender.send(data, options);
  });
  stream.on('end', function() {
    if (instance.readyState != WebSocket.OPEN) {
      if (typeof cb == 'function') cb(new Error('not opened'));
      else {
        delete instance._queue;
        instance.emit('error', new Error('not opened'));
      }
      return;
    }
    options.fin = true;
    instance._sender.send(null, options);
    if (typeof cb == 'function') cb(null);
  });*/

		Readable2 stream = (Readable2)data;

		stream.on("data", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				if (instance.readyState != WebSocket.OPEN) {
					///if (typeof cb == 'function') cb(new Error('not opened'));
					if (cb != null) cb.writeDone("not opened");
					else {
						///delete instance._queue;
						instance._queue.clear();
						instance.emit("error", "not opened"/*new Error('not opened')*/);
					}
					return;
				}
				options.fin = false;
				instance._sender.send(data, options, null);			
			}

		});

		stream.on("end", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				if (instance.readyState != WebSocket.OPEN) {
					///if (typeof cb == 'function') cb(new Error('not opened'));
					if (cb != null) cb.writeDone("not opened");
					else {
						///delete instance._queue;
						// TBD...
						instance._queue.clear();
						instance.emit("error", "not opened"/*new Error('not opened')*/);
					}
					return;
				}
				options.fin = true;
				instance._sender.send(null, options, null);
				///if (typeof cb == 'function') cb(null);	
				if (cb != null) cb.writeDone(null);
			}

		});

	}


	/**
	 * Streams data through calls to a user supplied function
	 *
	 * @param {Object} Members - mask: boolean, binary: boolean
	 * @param {function} 'function (error, send)' which is executed on successive ticks of which send is 'function (data, final)'.
	 * @api public
	 */

	public interface StreamCallback {
		public interface SendDone {
			public void done(Object data, boolean finl) throws Exception;
		}
		public void onStream(String error, SendDone send);
		public void setSendDone(SendDone send);
		public boolean isPrepand();

	}

	// TBD...
	private void stream(final SendOptions options, final StreamCallback cb) throws Exception {
		/*if (typeof options == 'function') {
    cb = options;
    options = {};
  }
		 */

		final WebSocket self = this;
		if (cb == null /*typeof cb != 'function'*/) throw new Exception("callback must be provided");
		if (this.readyState != WebSocket.OPEN) {
			if (cb != null/*typeof cb == 'function'*/) cb.onStream("not opened", null);/// (new Error('not opened'));
			else throw new Exception("not opened");
			return;
		}
		if (this._queue != null && !this._queue.isEmpty()) {
			///this._queue.push(function() { self.stream(options, cb); });
			this._queue.add(new Sendor(){

				@Override
				public void execute() throws Exception {
					self.stream(options, cb); 			
				}

			});
			return;
		}
		// TBD...
		///options = options || {};
		///if (typeof options.mask == 'undefined') options.mask = !this._isServer;
		startQueue();
		/*var send = function(data, final) {
    try {
      if (self.readyState != WebSocket.OPEN) throw new Error('not opened');
      options.fin = final === true;
      self._sender.send(data, options);
      if (!final) process.nextTick(cb.bind(null, null, send));
      else executeQueueSends(self);
    }
    catch (e) {
      if (typeof cb == 'function') cb(e);
      else {
        delete self._queue;
        self.emit('error', e);
      }
    }
  }*/					
		// TBD...
		///process.nextTick(cb.bind(null, null, send));
		/*
		final StreamCallback.SendDone sendone = new StreamCallback.SendDone() {

			@Override
			public void done(Object data, boolean finl) throws Exception {
				final StreamCallback.SendDone sself = this;

				try {
					if (self.readyState != WebSocket.OPEN) throw new Exception("not opened");
					options.fin = finl == true;
					self._sender.send(data, options, null);
					///if (!finl) process.nextTick(cb.bind(null, null, send));
					if (!finl) context.nextTick(new nextTickListener(){

						@Override
						public void onNextTick() throws Exception {
							cb.onStream(null, sself);						
						}

					});
					else executeQueueSends();
				}
				catch (Exception e) {
					///if (typeof cb == 'function') cb(e);
					if (cb != null) cb.onStream(e.toString(), null);
					else {
						///delete self._queue;
						self._queue.clear();
						self.emit("error", e.toString());
					}
				}				
			}

		};

		context.nextTick(new nextTickListener(){

			@Override
			public void onNextTick() throws Exception {
				cb.onStream(null, sendone);
			}

		});
*/
	}

	/**
	 * Immediately shuts down the connection
	 * @throws Exception 
	 *
	 * @api public
	 */

	public void terminate() throws Exception {
		if (this.readyState == WebSocket.CLOSED) return;
		if (this._socket != null) {
			try {
				// End the connection
				this._socket.end(null, null, null);
			}
			catch (Exception e) {
				// Socket error during end() call, so just destroy it right now
				///cleanupWebsocketResources.call(this, true);
				cleanupWebsocketResources.onEvent(null);
				return;
			}

			// Add a timeout to ensure that the connection is completely
			// cleaned up within 30 seconds, even if the clean close procedure
			// fails for whatever reason
			if (this._closeTimer != null) {
				context.clearTimeout(this._closeTimer);
			}
			///this._closeTimer = setTimeout(cleanupWebsocketResources.bind(this, true), CloseTimeout);
			this._closeTimer = context.setTimeout(new TimeoutListener(){

				@Override
				public void onTimeout() throws Exception {
					cleanupWebsocketResources.onEvent(null);				
				}

			}, CloseTimeout);
		}
		else if (this.readyState == WebSocket.CONNECTING) {
			///cleanupWebsocketResources.call(this, true);
			cleanupWebsocketResources.onEvent(null);
		}
	}

	/**
	 * Expose bufferedAmount
	 *
	 * @api public
	 */

	public int bufferedAmount() {
		int amount = 0;
		if (this._socket!=null) {
			amount = this._socket.bufferSize() > 0 ?  this._socket.bufferSize() : 0;
		}
		return amount;
	}

	/**
	 * Emulates the W3C Browser based WebSocket interface using function members.
	 * @throws Exception 
	 *
	 * @see http://dev.w3.org/html5/websockets/#the-websocket-interface
	 * @api public
	 */

	public void onopen(final onopenListener cb) throws Exception {
		final WebSocket self = this;
		
        this.removeListener("open");
        
        this.on("open", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
                cb.onOpen(new OpenEvent(self));				
			}
        	
        });
	}
	public interface onopenListener {
		public void onOpen(OpenEvent event) throws Exception;
	}

	public void onerror(final onerrorListener cb) throws Exception {
		final WebSocket self = this;
		
        this.removeListener("error");
        
        this.on("error", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				error_code_b data = (error_code_b)raw;
				
                cb.onError(new ErrorEvent(data.errorCode, data.reason, self));				
			}
        	
        });
	}
	public interface onerrorListener {
		public void onError(ErrorEvent event) throws Exception;
	}

	public void onclose(final oncloseListener cb) throws Exception {
		final WebSocket self = this;
		
        this.removeListener("close");
        
        this.on("close", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				close_code_b data = (close_code_b)raw;
				
                cb.onClose(new CloseEvent(data.closeCode, data.message, self));				
			}
        	
        });
	}
	public interface oncloseListener {
		public void onClose(CloseEvent event) throws Exception;
	}

	public void onmessage(final onmessageListener cb) throws Exception {
		final WebSocket self = this;
		
        this.removeListener("message");
        
        this.on("message", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				message_data_b data = (message_data_b)raw;
				
                cb.onMessage(new MessageEvent(data.data, data.flags.binary ? "Binary" : "Text", self));				
			}
        	
        });
	}
	public interface onmessageListener {
		public void onMessage(MessageEvent event) throws Exception;
	}


	/**
	 * Emulates the W3C Browser based WebSocket interface using addEventListener.
	 *
	 * @see https://developer.mozilla.org/en/DOM/element.addEventListener
	 * @see http://dev.w3.org/html5/websockets/#the-websocket-interface
	 * @api public
	 */
	/*
WebSocket.prototype.addEventListener = function(method, listener) {
  var target = this;
  if (typeof listener === 'function') {
    if (method === 'message') {
      function onMessage (data, flags) {
        listener.call(this, new MessageEvent(data, flags.binary ? 'Binary' : 'Text', target));
      }
      // store a reference so we can return the original function from the addEventListener hook
      onMessage._listener = listener;
      this.on(method, onMessage);
    } else if (method === 'close') {
      function onClose (code, message) {
        listener.call(this, new CloseEvent(code, message, target));
      }
      // store a reference so we can return the original function from the addEventListener hook
      onClose._listener = listener;
      this.on(method, onClose);
    } else if (method === 'error') {
      function onError (event) {
        event.target = target;
        listener.call(this, event);
      }
      // store a reference so we can return the original function from the addEventListener hook
      onError._listener = listener;
      this.on(method, onError);
    } else if (method === 'open') {
      function onOpen () {
        listener.call(this, new OpenEvent(target));
      }
      // store a reference so we can return the original function from the addEventListener hook
      onOpen._listener = listener;
      this.on(method, onOpen);
    } else {
      this.on(method, listener);
    }
  }
}
	 */


	/**
	 * W3C MessageEvent
	 *
	 * @see http://www.w3.org/TR/html5/comms.html
	 * @api private
	 */

	/*function MessageEvent(dataArg, typeArg, target) {
  this.data = dataArg;
  this.type = typeArg;
  this.target = target;
}*/

	public static final class MessageEvent {
		/**
		 * @return the data
		 */
		public Object getData() {
			return data;
		}
		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		/**
		 * @return the type
		 */
		public boolean isBinary() {
			return binary;
		}
		/**
		 * @return the target
		 */
		public WebSocket getTarget() {
			return target;
		}
		private Object    data;
		private String    type;
		private boolean   binary;
		private WebSocket target;

		public MessageEvent(Object data, String type, WebSocket target) {
			this.data   = data;
			this.type   = type;
			this.target = target;
			
			this.binary = "binary".equalsIgnoreCase(type);
		}
		@SuppressWarnings("unused")
		private MessageEvent(){}
	}

	/**
	 * W3C CloseEvent
	 *
	 * @see http://www.w3.org/TR/html5/comms.html
	 * @api private
	 */

	/*function CloseEvent(code, reason, target) {
  this.wasClean = (typeof code == 'undefined' || code == 1000);
  this.code = code;
  this.reason = reason;
  this.target = target;
}*/

	public static final class CloseEvent {
		/**
		 * @return the code
		 */
		public int getCode() {
			return code;
		}
		/**
		 * @return the reason
		 */
		public String getReason() {
			return reason;
		}
		/**
		 * @return the target
		 */
		public WebSocket getTarget() {
			return target;
		}
		/**
		 * @return the wasClean
		 */
		public boolean isWasClean() {
			return wasClean;
		}
		private int       code;
		private String    reason;
		private WebSocket target;
		private boolean   wasClean;

		public CloseEvent(int code, String reason, WebSocket target) {
			this.code   = code;
			this.reason = reason;
			this.target = target;

			this.wasClean = (code < 0 || code == 1000);
		}
		@SuppressWarnings("unused")
		private CloseEvent(){}
	}

	/**
	 * W3C OpenEvent
	 *
	 * @see http://www.w3.org/TR/html5/comms.html
	 * @api private
	 */

	/*function OpenEvent(target) {
  this.target = target;
}*/

	public static final class OpenEvent {
		/**
		 * @return the target
		 */
		public WebSocket getTarget() {
			return target;
		}
		private WebSocket target;

		public OpenEvent(WebSocket target) {
			this.target = target;
		}
		@SuppressWarnings("unused")
		private OpenEvent(){}
	}

	public static final class ErrorEvent {
		/**
		 * @return the error
		 */
		public int getCode() {
			return code;
		}
		/**
		 * @return the error
		 */
		public String getError() {
			return error;
		}
		/**
		 * @return the target
		 */
		public WebSocket getTarget() {
			return target;
		}
		private int       code;
		private String    error;
		private WebSocket target;

		public ErrorEvent(int code, String error, WebSocket target) {
			this.code   = code;
			this.error  = error;
			this.target = target;
		}
		@SuppressWarnings("unused")
		private ErrorEvent(){}
	}


	/**
	 * Entirely private apis,
	 * which may or may not be bound to a specific WebSocket instance.
	 * @throws Exception 
	 */

	private void initAsServerClient(IncomingMessage req, AbstractSocket socket, 
			ByteBuffer upgradeHead, Options options) throws Exception {
		/*options = new Options({
    protocolVersion: protocolVersion,
    protocol: null
  }).merge(options);*/
		///options.protocolVersion = WebSocket.ProtocolVersion;
		///options.protocol = null;

		// expose state properties
		this.protocol = options.protocol;
		this.protocolVersion = options.protocolVersion;
		///this.supports.binary = (this.protocolVersion != 'hixie-76');
		this.supports.put("binary", true); // always support binary 
		this.upgradeReq = req;
		this.readyState = WebSocket.CONNECTING;
		this._isServer = true;

		// establish connection
		///if (options.value.protocolVersion == 'hixie-76') establishConnection.call(this, ReceiverHixie, SenderHixie, socket, upgradeHead);
		///else 
		///establishConnection.call(this, Receiver, Sender, socket, upgradeHead);
		establishConnection(socket, upgradeHead);
	}

	private void initAsClient(String address, List<String> protocols, final Options options) throws Exception {
		/*options = new Options({
    origin: null,
    protocolVersion: protocolVersion,
    host: null,
    headers: null,
    protocol: null,
    agent: null,
    // ssl-related options
    pfx: null,
    key: null,
    passphrase: null,
    cert: null,
    ca: null,
    ciphers: null,
    rejectUnauthorized: null,

    // httpp-related options
    hole: {port: -1},
    httpp: false // default as HTTP not HTTPP
  }).merge(options);*/
		if (options.protocolVersion != 8 && options.protocolVersion != 13) {
			throw new Exception("unsupported protocol version");
		}

		Log.d(TAG, "as client, options:"+options);
		
		// verify url and establish http class
		Url.UrlObj serverUrl = Url.parse(address);
		boolean isUnixSocket = serverUrl.protocol == "ws+unix:";
		if (null==serverUrl.host && !isUnixSocket) throw new Exception("invalid url");
		boolean isSecure = serverUrl.protocol == "wss:" || serverUrl.protocol == "https:";
		/// TBD...
		///var httpObj = options.value.httpp ? (isSecure ? httpps : httpp) : (isSecure ? https : http);
		int port = serverUrl.port>0 ? serverUrl.port : (isSecure ? 443 : 80);
		String auth = serverUrl.auth;

		// expose state properties
		this._isServer = false;
		this.url = address;
		this.protocolVersion = options.protocolVersion;
		///this.supports.binary = (this.protocolVersion != 'hixie-76');
		this.supports.put("binary", true);

		// TBD...
		// begin handshake
		/*var key = new Buffer(options.value.protocolVersion + '-' + Date.now()).toString('base64');
  var shasum = crypto.createHash('sha1');
  shasum.update(key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11');
  var expectedServerKey = shasum.digest('base64');
		 */
		String str = ""+options.protocolVersion+"-"+(new Date());
		String key = Base64.encodeToString(str.getBytes(), Base64.DEFAULT);

		MessageDigest shasum = MessageDigest.getInstance("SHA1");
		shasum.update((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("utf-8"));
		byte[] sharet = shasum.digest();
		
		final String expectedServerKey = Base64.encodeToString(sharet, Base64.DEFAULT);
		
		Log.d(TAG, "str:"+str+",srv key:"+key+",exp:"+expectedServerKey);

		Agent agent = options.agent;

		String headerHost = serverUrl.hostname;
		// Append port number to Host and Origin header, only if specified in the url and non-default
		if(serverUrl.port > 0) {
			if((isSecure && (port != 443)) || (!isSecure && (port != 80))){
				headerHost = headerHost + ':' + port;
			}
		}

		/*
  var requestOptions = {
    port: port,
    host: serverUrl.hostname,
    hole: options.value.hole || {port: -1},
    headers: {
      'Connection': 'Upgrade',
      'Upgrade': 'websocket',
      'Host': headerHost,
      'Origin': (isSecure ? 'https://' : 'http://') + headerHost,
      'Sec-WebSocket-Version': options.value.protocolVersion,
      'Sec-WebSocket-Key': key
    }
  };*/
		ReqOptions requestOptions = new ReqOptions();
		
		requestOptions.method = "GET";
		
		requestOptions.port = port;
		requestOptions.hostname = serverUrl.hostname;
		
		requestOptions.localPort = options.localPort;
		requestOptions.localAddress = options.localAddress;

		requestOptions.headers.put("Connection", new ArrayList<String>()); 
		requestOptions.headers.get("Connection").add("Upgrade");

		requestOptions.headers.put("Upgrade", new ArrayList<String>()); 
		requestOptions.headers.get("Upgrade").add("websocket");

		requestOptions.headers.put("Host", new ArrayList<String>()); 
		requestOptions.headers.get("Host").add(headerHost);

		requestOptions.headers.put("Origin", new ArrayList<String>()); 
		requestOptions.headers.get("Origin").add((isSecure ? "https://" : "http://") + headerHost);

		requestOptions.headers.put("Sec-WebSocket-Version", new ArrayList<String>()); 
		requestOptions.headers.get("Sec-WebSocket-Version").add(""+options.protocolVersion);

		requestOptions.headers.put("Sec-WebSocket-Key", new ArrayList<String>()); 
		requestOptions.headers.get("Sec-WebSocket-Key").add(key);

		// If we have basic auth.
		if (auth!=null) {
			///requestOptions.headers['Authorization'] = 'Basic ' + new Buffer(auth).toString('base64');
			String authstr = Base64.encodeToString(auth.getBytes("utf-8"), Base64.DEFAULT);

			requestOptions.headers.put("Authorization", new ArrayList<String>()); 
			requestOptions.headers.get("Authorization").add("Basic " + authstr);
		}

		if (options.protocol != null) {
			///requestOptions.headers['Sec-WebSocket-Protocol'] = options.value.protocol;
			requestOptions.headers.put("Sec-WebSocket-Protocol", new ArrayList<String>()); 
			requestOptions.headers.get("Sec-WebSocket-Protocol").add(options.protocol);
		}

		if (options.host != null) {
			///requestOptions.headers['Host'] = options.value.host;
			requestOptions.headers.put("Host", new ArrayList<String>()); 
			requestOptions.headers.get("Host").add(options.host);
		}

		if (options.headers != null && !options.headers.isEmpty()) {
			/*for (var header in options.value.headers) {
       if (options.value.headers.hasOwnProperty(header)) {
        requestOptions.headers[header] = options.value.headers[header];
       }
    }*/
			for (Entry<String, String> kv : options.headers.entrySet()) {
				requestOptions.headers.put(kv.getKey(), new ArrayList<String>());
				requestOptions.headers.get(kv.getKey()).add(kv.getValue());
			}
		}

		/*
  if (options.isDefinedAndNonNull('pfx')
   || options.isDefinedAndNonNull('key')
   || options.isDefinedAndNonNull('passphrase')
   || options.isDefinedAndNonNull('cert')
   || options.isDefinedAndNonNull('ca')
   || options.isDefinedAndNonNull('ciphers')
   || options.isDefinedAndNonNull('rejectUnauthorized')) {

    if (options.isDefinedAndNonNull('pfx')) requestOptions.pfx = options.value.pfx;
    if (options.isDefinedAndNonNull('key')) requestOptions.key = options.value.key;
    if (options.isDefinedAndNonNull('passphrase')) requestOptions.passphrase = options.value.passphrase;
    if (options.isDefinedAndNonNull('cert')) requestOptions.cert = options.value.cert;
    if (options.isDefinedAndNonNull('ca')) requestOptions.ca = options.value.ca;
    if (options.isDefinedAndNonNull('ciphers')) requestOptions.ciphers = options.value.ciphers;
    if (options.isDefinedAndNonNull('rejectUnauthorized')) requestOptions.rejectUnauthorized = options.value.rejectUnauthorized;

    if (!agent) {
        // global agent ignores client side certificates
        agent = new httpObj.Agent(requestOptions);
    }
  }
		 */

		requestOptions.path = serverUrl.path!=null ? serverUrl.path : "/";

		if (agent!=null) {
			requestOptions.agent = agent;
		}

		if (isUnixSocket) {
			requestOptions.socketPath = serverUrl.pathname;
		}

		if (options.origin != null) {
			if (options.protocolVersion < 13) {
				requestOptions.headers.put("Sec-WebSocket-Origin", new ArrayList<String>());
				requestOptions.headers.get("Sec-WebSocket-Origin").add(options.origin);
			} else {
				requestOptions.headers.put("Origin", new ArrayList<String>());
				requestOptions.headers.get("Origin").add(options.origin);
			}
		}

		final WebSocket self = this;

		// TBD... https, httpps
		///var req = httpObj.request(requestOptions);
		final ClientRequest req = options.httpp ? 
				httpp.request(context, requestOptions, null) : 
				http.request(context, requestOptions, null);

				/*req.on('error', function(error) {
    self.emit('error', error);
    cleanupWebsocketResources.call(this, error);
  });*/
				req.on("error", new Listener(){

					@Override
					public void onEvent(Object error) throws Exception {
						self.emit("error", error!=null? error.toString() : null);
						cleanupWebsocketResources.onEvent(error!=null? error.toString() : null);		
					}

				});

				/*
  req.once('response', function(res) {
    if (!self.emit('unexpected-response', req, res)) {
      var error = new Error('unexpected server response (' + res.statusCode + ')');
      req.abort();
      self.emit('error', error);
    }
    cleanupWebsocketResources.call(this, error);
  });
				 */
				req.onceResponse(new ClientRequest.responseListener(){
					public void onResponse(IncomingMessage res) throws Exception {
						String error=null;
						if (!self.emit("unexpected-response", /*req,*/ res)) {
							error = "unexpected server response (" + res.statusCode() + ")"; ///new Error('unexpected server response (' + res.statusCode + ')');
							req.abort();
							self.emit("error", error);
						}
						cleanupWebsocketResources.onEvent(error);
					}
				});

				req.onceUpgrade(new ClientRequest.upgradeListener(){

					@Override
					public void onUpgrade(IncomingMessage res, AbstractSocket socket,
							ByteBuffer upgradeHead) throws Exception {
						
						Log.d(TAG, "got upgrade");

						if (self.readyState == WebSocket.CLOSED) {
							// client closed before server accepted connection
							self.emit("close");
							self.removeAllListeners();
							socket.end(null, null, null);
							return;
						}

						Log.d(TAG, "res.headers:"+res.headers());
						
						String serverKey = 
								res.headers().containsKey("sec-websocket-accept") ? 
							   (res.headers().get("sec-websocket-accept").isEmpty() ? null : 
								res.headers().get("sec-websocket-accept").get(0)) : null; ///res.headers['sec-websocket-accept'];

							   // TBD...
										///if (typeof serverKey == 'undefined' || serverKey !== expectedServerKey) {
										if (false/*serverKey == null || !serverKey.equalsIgnoreCase(expectedServerKey)*/) {
											Log.d(TAG, "invalid server key: "+serverKey+", expectedServerKey:"+expectedServerKey);
											
											self.emit("error", "invalid server key");
											self.removeAllListeners();
											socket.end(null, null, null);
											return;
										}

										String serverProt = 
												res.headers().containsKey("sec-websocket-protocol") ?
														(res.headers().get("sec-websocket-protocol").isEmpty() ? null : 
															res.headers().get("sec-websocket-protocol").get(0)) : null;

														///String[] protList = (options.protocol!=null ? options.protocol : "").split(", *"); ///(options.value.protocol || "").split(/, */);
														String protList = (options.protocol!=null ? options.protocol : "");

														String protError = null;
														if (null==options.protocol && serverProt!=null) {
															protError = "server sent a subprotocol even though none requested";
														} else if (null!=options.protocol && null==serverProt) {
															protError = "server sent no subprotocol even though requested";
														} else if (serverProt!=null && (protList.indexOf(serverProt) == -1)) {
															protError = "server responded with an invalid protocol";
														}
														if (protError!=null) {
															self.emit("error", protError);
															self.removeAllListeners();
															socket.end(null, null, null);
															return;
														} else if (serverProt != null) {
															self.protocol = serverProt;
														}

														establishConnection(/*self, Receiver, Sender,*/ socket, upgradeHead);

														// perform cleanup on http resources
														req.removeAllListeners();
														// TBD...
														///req = null;
														///agent = null;

					}

				});

				req.end(null, null, null);
				this.readyState = WebSocket.CONNECTING;
	}

	// POJO beans
	public static class message_data_b {
		public     Object data;
		public opcOptions flags;

		public message_data_b(Object data, opcOptions flags) {
			this.data  = data;
			this.flags = flags;
		}
	}

	public static class error_code_b {
		public    int errorCode;
		public String reason;

		public error_code_b(String reason, int errorCode) {
			this.reason    = reason;
			this.errorCode = errorCode;
		}
	}

	public static class close_code_b {
		public    int closeCode;
		public String message;

		public close_code_b(String message, int closeCode) {
			this.message   = message;
			this.closeCode = closeCode;
		}
	}

	// receiver event handlers
	private class ReceiverClass extends Receiver {

		public ReceiverClass() throws Exception {
			super();
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void ontext(String text, opcOptions options) throws Exception {
			options.binary = false;
			emit("message", new message_data_b(text, options));		
		}

		@Override
		protected void onbinary(ByteBuffer buf, opcOptions options) throws Exception {
			options.binary = true;
			emit("message", new message_data_b(buf, options));				
		}

		@Override
		protected void onping(ByteBuffer buf, opcOptions options) throws Exception {
			///flags = flags || {};
			///pong(data, {mask: !self._isServer, binary: flags.binary === true}, true);
			pong(buf, new SendOptions(options.binary, !_isServer), true);
			emit("ping", new message_data_b(buf, options));
		}

		@Override
		protected void onpong(ByteBuffer buf, opcOptions options) throws Exception {
			emit("pong", new message_data_b(buf, options));
		}

		@Override
		protected void onclose(int code, String message, opcOptions options) throws Exception {
			///flags = flags || {};
			close(code, message);		
		}

		@Override
		protected void onerror(String reason, int errorCode) throws Exception {
			// close the connection when the receiver reports a HyBi error code
			/*self.close(typeof errorCode != 'undefined' ? errorCode : 1002, "");
	    if (self.listeners("error").length > 0) {
	      self.emit('error', reason, errorCode);
	    }*/
			close(errorCode != 0 ? errorCode : 1002, "");
			if (listenerCount("error") > 0) {
				emit("error", new error_code_b(reason, errorCode));
			}
		}

	}

	private void establishConnection(
			/*Receiver ReceiverClass, Sender SenderClass, */
			final AbstractSocket socket, final ByteBuffer upgradeHead) throws Exception {
		this._socket = socket;
		///socket.setTimeout(0);
		socket.setNoDelay(true);
		final WebSocket self = this;

		this._receiver = new ReceiverClass();

		// socket cleanup handlers
		/*socket.on('end', cleanupWebsocketResources.bind(this));
  socket.on('close', cleanupWebsocketResources.bind(this));
  socket.on('error', cleanupWebsocketResources.bind(this));
		 */
		socket.on("end",   cleanupWebsocketResources);
		socket.on("close", cleanupWebsocketResources);
		socket.on("error", cleanupWebsocketResources);

		// ensure that the upgradeHead is added to the receiver
		/*function firstHandler(data) {
    if (self.readyState != WebSocket.OPEN) return;
    if (upgradeHead && upgradeHead.length > 0) {
      self.bytesReceived += upgradeHead.length;
      var head = upgradeHead;
      upgradeHead = null;
      self._receiver.add(head);
    }
    dataHandler = realHandler;
    if (data) {
      self.bytesReceived += data.length;
      self._receiver.add(data);
    }
  }*/

		// subsequent packets are pushed straight to the receiver
		/*function realHandler(data) {
    if (data) self.bytesReceived += data.length;
    self._receiver.add(data);
  }*/
		final Listener realHandler = new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {				
				ByteBuffer data = (ByteBuffer)raw;
				Log.d(TAG, "realHandler: "+data);

				if (data!=null) {
					self.bytesReceived += data.capacity();
					self._receiver.add(data);	
				}
			}

		};
		///var dataHandler = firstHandler;

		final Listener firstHandler = new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				ByteBuffer data = (ByteBuffer)raw;
				
				Log.d(TAG, "firstHandler, data: "+data+", upgradeHead:"+upgradeHead);

				// TBD...
				///dataHandler = realHandler;
				socket.removeListener("data", this); 
				socket.addListener("data", realHandler);
				///socket.on("data", realHandler);
                Log.d(TAG, "retrain data handler");
				
				if (self.readyState != WebSocket.OPEN) return;
				if (upgradeHead!=null && upgradeHead.capacity() > 0) {
					self.bytesReceived += upgradeHead.capacity();
					
					// copy one
					ByteBuffer head = ByteBuffer.allocate(upgradeHead.capacity());
					head.put(upgradeHead); head.flip();
					// TBD...
					///upgradeHead = null;
					upgradeHead.clear();
					
					self._receiver.add(head);
				}
				
				if (data != null) {
					self.bytesReceived += data.capacity();
					self._receiver.add(data);
				}
			}

		};

		// if data was passed along with the http upgrade,
		// this will schedule a push of that on to the receiver.
		// this has to be done on next tick, since the caller
		// hasn't had a chance to set event handlers on this client
		// object yet.
		///process.nextTick(firstHandler);
		context.nextTick(new nextTickListener(){

			@Override
			public void onNextTick() throws Exception {
				firstHandler.onEvent(null);		
			}

		});

		/*
  // receiver event handlers
  self._receiver.ontext = function (data, flags) {
    flags = flags || {};
    self.emit('message', data, flags);
  };
  self._receiver.onbinary = function (data, flags) {
    flags = flags || {};
    flags.binary = true;
    self.emit('message', data, flags);
  };
  self._receiver.onping = function(data, flags) {
    flags = flags || {};
    self.pong(data, {mask: !self._isServer, binary: flags.binary === true}, true);
    self.emit('ping', data, flags);
  };
  self._receiver.onpong = function(data, flags) {
    self.emit('pong', data, flags);
  };
  self._receiver.onclose = function(code, data, flags) {
    flags = flags || {};
    self.close(code, data);
  };
  self._receiver.onerror = function(reason, errorCode) {
    // close the connection when the receiver reports a HyBi error code
    self.close(typeof errorCode != 'undefined' ? errorCode : 1002, '');
    if (self.listeners('error').length > 0) {
      self.emit('error', reason, errorCode);
    }
  };
		 */

		// finalize the client
		this._sender = new Sender(socket);
		/*this._sender.on('error', function(error) {
    self.close(1002, '');
    self.emit('error', error);
  });*/
		this._sender.on("error", new Listener(){
			public void onEvent(final Object error) throws Exception {
				self.close(1002, "");
				self.emit("error", error!=null ? error.toString() : null);
			}
		});

		this.readyState = WebSocket.OPEN;
		this.emit("open");

		// TBD...
		///socket.on("data", dataHandler);
		socket.on("data", firstHandler);
		socket.on("drain", new Listener(){
			public void onEvent(final Object error) throws Exception {
				self.emit("drain");
			}
		});

	}


}
