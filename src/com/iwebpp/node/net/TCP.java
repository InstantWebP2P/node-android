// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.net;

import android.util.Log;

import com.iwebpp.libuvpp.Address;
import com.iwebpp.libuvpp.cb.StreamConnectionCallback;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.StreamHandle;
import com.iwebpp.libuvpp.handles.TCPHandle;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;

public final class TCP {
	private final static String TAG = "TCP";

	public static final class Socket 
	extends AbstractSocket {
		@SuppressWarnings("unused")
		private final static String TAG = "TCP:Socket";
		protected AbstractServer server;

		public Socket(NodeContext context, Options options) throws Exception {
			super(context, options);
			// TODO Auto-generated constructor stub
		}
		private Socket() throws Exception{super(null, null);};

		@Override
		protected int _bind(String address, int port) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.bind(address, port);
			} else 
				return -1;
		}

		@Override
		protected int _bind6(String address, int port) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.bind6(address, port);
			} else 
				return -1;
		}

		@Override
		protected int _connect(String address, int port) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.connect(address, port);
			} else 
				return -1;
		}

		@Override
		protected int _connect6(String address, int port) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.connect6(address, port);
			} else 
				return -1;
		}

		@Override
		protected Address _getSocketName() {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.getSocketName();
			} else 
				return null;
		}

		@Override
		protected Address _getPeerName() {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.getPeerName();
			} else 
				return null;
		}
		
		@Override
		public int setNoDelay(final boolean enable) {			
			// backwards compatibility: assume true when `enable` is omitted
			if (this._handle /*&& this._handle.setNoDelay*/ != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.setNoDelay(enable);
			} else 
				return -1;
		}
		
		@Override
		public int setKeepAlive(final boolean enable, final int delay) {
			// backwards compatibility: assume true when `enable` is omitted
			if (this._handle /*&& this._handle.setNoDelay*/ != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.setKeepAlive(enable, delay);
			} else 
				return -1;
		}

		@Override
		protected StreamHandle _createHandle(final LoopHandle loop) {
			return new TCPHandle(loop);
		}
		
	}

	// /* [ options, ] listener */
	public static class Server 
	extends AbstractServer {
		private final static String TAG = "TCP:Server";

		public Server(NodeContext context, Options options,
				ConnectionListener listener) throws Exception {
			super(context, options, listener);
			// TODO Auto-generated constructor stub
		}
		private Server() throws Exception{super(null, null, null);};

		@Override
		protected int _bind(String address, int port) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.bind(address, port);
			} else 
				return -1;
		}

		@Override
		protected int _bind6(String address, int port) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.bind6(address, port);
			} else 
				return -1;
		}

		@Override
		protected int _listen(int backlog) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.listen(backlog);
			} else 
				return -1;
		}

		@Override
		protected int _accept(StreamHandle client) {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.accept(client);
			} else 
				return -1;
		}

		@Override
		protected Address _getSocketName() {
			if (this._handle != null) {
				TCPHandle tcp = (TCPHandle)this._handle;
				
				return tcp.getSocketName();
			} else 
				return null;
		}
		
		@Override
		protected StreamHandle _createHandle(final LoopHandle loop) {
			return new TCPHandle(loop);
		}
		
		@Override
		protected StreamHandle _createServerHandle(String address, int port, int addressType, int fd) {
			TCPHandle handle = (TCPHandle) _createHandle(context.getLoop());
			int err = 0;


			Log.d(TAG, "bind to " + (address /*|| 'anycast'*/)+":"+port);
			if (Util.zeroString(address)) {
				// Try binding to ipv6 first
				err = handle.bind6("::", port);
				if (err!=0) {
					handle.close();
					// Fallback to ipv4
					return _createServerHandle("0.0.0.0", port, 4, -1);
				}
			} else if (addressType == 6) {
				err = handle.bind6(address, port);
			} else {
				err = handle.bind(address, port);
			}

			if (err!=0) {
				Log.d(TAG, "bind err "+err);
				handle.close();
				return null;
			}

			return handle;
		}
		
		@Override
		protected void _listen2(String address, int port, int addressType, 
				int backlog, int fd) throws Exception {
			Log.d(TAG, "listen2 "+address+":"+port+":"+addressType+":"+backlog);
			final AbstractServer self = this;

			boolean alreadyListening = false;

			// If there is not yet a handle, we need to create one and bind.
			// In the case of a server sent via IPC, we don't need to do this.
			if (null==self._handle) {
				Log.d(TAG, "_listen2: create a handle");

				/*var rval = createServerHandle(address, port, addressType, fd);
			    if (util.isNumber(rval)) {
			      var error = errnoException(rval, 'listen');
			      process.nextTick(function() {
			        self.emit('error', error);
			      });
			      return;
			    }*/
				StreamHandle rval = _createServerHandle(address, port, addressType, fd);
				if (rval == null) {
					final String error = "err listen";
					///process.nextTick(function() {
					context.nextTick(new NodeContext.nextTickListener() {

						@Override
						public void onNextTick() throws Exception {
							self.emit("error", error);
						}

					});
					return;
				}

				///alreadyListening = (process.platform === 'win32');
				self._handle = rval;
			} else {
				///debug('_listen2: have a handle already');
				Log.d(TAG, "_listen2: have a handle already");
			}

			///self._handle.onconnection = onconnection;

			// onConnection callback
			StreamConnectionCallback onconnection = new StreamConnectionCallback(){

				@Override
				public void onConnection(int status, Exception error)
						throws Exception {
					///var handle = this;
					///var self = handle.owner;
					StreamHandle handle = self._handle;
					StreamHandle clientHandle = _createHandle(context.getLoop());
					int err = handle.accept(clientHandle);


					Log.d(TAG, "onconnection");

					if (err!=0) {
						///self.emit('error', errnoException(err, 'accept'));
						self.emit("error", "err accept "+err);
						return;
					}

					if (/*self.maxConnections &&*/ self.get_connections() >= self.getMaxConnections()) {
						Log.d(TAG, "exceed maxim connections");

						clientHandle.close();
						return;
					}

					/*AbstractSocket socket = new AbstractSocket({
						handle: clientHandle,
						allowHalfOpen: self.allowHalfOpen
					});*/
					Socket socket = new Socket(
							context, 
							new Socket.Options(clientHandle, true, true, self.isAllowHalfOpen()));
					socket.readable(true); socket.writable(true);

					self.set_connections(self.get_connections() + 1);
					socket.server = self;

					///DTRACE_NET_SERVER_CONNECTION(socket);
					///COUNTER_NET_SERVER_CONNECTION(socket);
					self.emit("connection", socket);
				}

			};
			self._handle.setConnectionCallback(onconnection);

			///self._handle.owner = self;

			int err = 0;
			if (!alreadyListening)
				err = _listen(self._handle, backlog);

			if (0!=err) {
				///var ex = errnoException(err, "listen");
				final String ex = "err listen";
				self._handle.close();
				self._handle = null;
				///process.nextTick(function() {
				context.nextTick(new NodeContext.nextTickListener() {

					@Override
					public void onNextTick() throws Exception {
						self.emit("error", ex);
					}

				});

				Log.d(TAG, ex);

				return;
			}

			///process.nextTick(function() {
			context.nextTick(new NodeContext.nextTickListener() {

				@Override
				public void onNextTick() throws Exception {
					// ensure handle hasn't closed
					if (self._handle != null)
						self.emit("listening");
				}

			});
		}
		
	}	

	public static Server createServer(
			final NodeContext context, 
			final Server.ConnectionListener listener) throws Exception {
		return new Server(context, new Server.Options(false), listener);
	}

	// Target API:
	//
	// var s = net.connect({port: 80, host: 'google.com'}, function() {
	//   ...
	// });
	//
	// There are various forms:
	//
	// connect(options, [cb])
	// connect(port, [host], [cb])
	// connect(path, [cb]);
	//
	public static Socket createConnection(
			NodeContext ctx, 
			String address, int port,
			String localAddress, int localPort,
			final AbstractSocket.ConnectListener cb) throws Exception {
		Log.d(TAG, "createConnection " + address + ":" + port + "@"+localAddress+":"+localPort);

		Socket s = new Socket(ctx, new Socket.Options(null, false, false, true));

		s.connect(address, port, localAddress, localPort, cb);

		return s;
	}
	
	public static Socket connect(
			NodeContext ctx, 
			String address, int port,
			String localAddress, int localPort,
			final Socket.ConnectListener cb) throws Exception {
		Log.d(TAG, "connect " + address + ":" + port + "@"+localAddress+":"+localPort);

		Socket s = new Socket(ctx, new Socket.Options(null, false, false, true));

		s.connect(address, port, localAddress, localPort, cb);

		return s;
	}
	
}
