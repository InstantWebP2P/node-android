package com.iwebpp.node;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.iwebpp.libuvpp.Address;
import com.iwebpp.libuvpp.cb.StreamCloseCallback;
import com.iwebpp.libuvpp.cb.StreamConnectCallback;
import com.iwebpp.libuvpp.cb.StreamConnectionCallback;
import com.iwebpp.libuvpp.cb.StreamReadCallback;
import com.iwebpp.libuvpp.cb.StreamShutdownCallback;
import com.iwebpp.libuvpp.cb.StreamWriteCallback;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.UDTHandle;
import com.iwebpp.node.Writable2.WriteReq;

public final class UDT {
	
	public static final class Socket extends Duplex {
		private final static String TAG = "UDT:Socket";

		private boolean _connecting;
		private boolean _hadError;
		private UDTHandle _handle;
		private String _host;
		private boolean readable;
		private boolean writable;
		private Object _pendingData;
		private String _pendingEncoding;
		private boolean allowHalfOpen;
		private boolean destroyed;
		private int bytesRead;
		private int _bytesDispatched;
		private Object _writev;

		private Server server;

		private boolean _consuming;

		private Address _sockname;

		private Address _peername;
		
		private NodeContext context;

		public static class Options {

			public UDTHandle handle;
			public long fd;
			public boolean readable;
			public boolean writable;
			public boolean allowHalfOpen;
			
			public Options(boolean allowHalfOpen, UDTHandle handle) {
				this.allowHalfOpen = allowHalfOpen;

				this.handle = handle;
				this.readable = false;
				this.writable = false;
								
				this.fd = -1;
			}
			
			@SuppressWarnings("unused")
			private Options(){}
		};

		public Socket(NodeContext context, Options options) throws Exception {
			// TBD...
			super(context, 
				  new Readable2.Options(-1, "", false, "utf8"), 
			      new Writable2.Options(-1, false, "utf8", false));
			

			final Socket self = this;
			
			// node context
			this.context = context;
			
			///if (!(this instanceof Socket)) return new Socket(options);

			this._connecting = false;
			this._hadError = false;
			this._handle = null;
			this._host = null;

			/*if (Util.isNumber(options))
			    options = { fd: options }; // Legacy interface.
			  else if (Util.isUndefined(options))
			    options = {};
			 */

			///stream.Duplex.call(this, options);

			if (options.handle != null) {
				this._handle = options.handle; // private
			} /*TBD...else if (!Util.isUndefined(options.fd)) {
			    this._handle = createHandle(options.fd);

			    this._handle.open(options.fd);
			    if ((options.fd == 1 || options.fd == 2) &&
			        (this._handle instanceof Pipe) &&
			        process.platform === 'win32') {
			      // Make stdout and stderr blocking on Windows
			      var err = this._handle.setBlocking(true);
			      if (err)
			        throw errnoException(err, 'setBlocking');
			    }

			    this.readable = options.readable;
			    this.writable = options.writable;
			  } */else {
				  // these will be set once there is a connection
				  this.readable = this.writable = false;
			  }

			// shut down the socket when we're finished with it.

			// the user has called .end(), and all the bytes have been
			// sent out to the other side.
			// If allowHalfOpen is false, or if the readable side has
			// ended already, then destroy.
			// If allowHalfOpen is true, then we need to do a shutdown,
			// so that only the writable side will be cleaned up.
			final Listener onSocketFinish = new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					// If still connecting - defer handling 'finish' until 'connect' will happen
					if (self._connecting) {
						Log.d(TAG, "osF: not yet connected");
						self.once("connect", this);
						return;
					}

					Log.d(TAG, "onSocketFinish");
					if (!self.readable || self.get_readableState().ended) {
						Log.d(TAG, "oSF: ended, destroy "+self.get_readableState());
						self.destroy(null);
						return;
					}

					Log.d(TAG, "oSF: not ended, call shutdown()");

					// otherwise, just shutdown, or destroy() if not possible
					if (self._handle==null /*|| !self._handle.shutdown*/) {
						self.destroy(null);
						return;
					}

					/*var req = { oncomplete: afterShutdown };
					  var err = this._handle.shutdown(req);

					  if (err)
						  return this._destroy(errnoException(err, 'shutdown'));
					 */
					self._handle.setShutdownCallback(new StreamShutdownCallback(){

						@Override
						public void onShutdown(int status, Exception error)
								throws Exception {
							///var self = handle.owner;

							Log.d(TAG, "afterShutdown destroyed="+self.destroyed+","+
									self.get_readableState());

							// callback may come after call to destroy.
							if (self.destroyed)
								return;

							if (self.get_readableState().ended) {
								Log.d(TAG, "readableState ended, destroying");
								self.destroy(null);
							} else {
								///self.once("_socketEnd", self.destroy);
								self.once("_socketEnd", new Listener(){

									@Override
									public void onEvent(Object data)
											throws Exception {
										self.destroy(null);
									}

								});
							}
						}

					});
					int err = self._handle.closeWrite();

					if (err!=0) {
						self._destroy("shutdown", null);
						return;
					}
				}

			};
			this.on("finish", onSocketFinish);

			// the EOF has been received, and no more bytes are coming.
			// if the writable side has ended already, then clean everything
			// up.
			Listener onSocketEnd = new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					// XXX Should not have to do as much crap in this function.
					// ended should already be true, since this is called *after*
					// the EOF errno and onread has eof'ed
					Log.d(TAG, "onSocketEnd "+self.get_readableState());
					self.get_readableState().ended = true;
					if (self.get_readableState().endEmitted) {
						self.readable = false;
						maybeDestroy(self);
					} else {
						self.once("end", new Listener(){

							public void onEvent(final Object data) throws Exception {
								self.readable = false;
								maybeDestroy(self);
							}

						});

						self.read(0);
					}

					if (!self.allowHalfOpen) {
						// TBD...
						///self.write = writeAfterFIN;
						self.destroySoon();
					}
				}

			};
			this.on("_socketEnd", onSocketEnd);

			initSocketHandle(this);

			this._pendingData = null;
			this._pendingEncoding = "";

			// handle strings directly
			this._writableState.decodeStrings = false;

			// default to *not* allowing half open sockets
			this.allowHalfOpen = options.allowHalfOpen;

			// if we have a handle, then start the flow of data into the
			// buffer.  if not, then this will happen when we connect
			if (this._handle!=null && options.readable)
				this.read(0);

		}

		protected void destroySoon() throws Exception {
			final Socket self = this;

			if (this.writable)
				this.end(null, null, null);

			if (this._writableState.finished)
				this.destroy(null);
			else
				this.once("finish", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						self.destroy(null);
					}

				});
		}

		// called when creating new Socket, or when re-using a closed Socket
		private static void initSocketHandle(final Socket self) {
			self.destroyed = false;
			self.bytesRead = 0;
			self._bytesDispatched = 0;

			// Handle creation may be deferred to bind() or connect() time.
			if (self._handle != null) {
				self._handle.owner = self;

				// This function is called whenever the handle gets a
				// buffer, or when there's an error reading.
				StreamReadCallback onread = new StreamReadCallback(){

					@Override
					public void onRead(ByteBuffer buffer) throws Exception {
						int nread = (buffer == null) ? 0 : buffer.capacity();

						///var handle = this;
						///Socket self = handle.owner;
						UDTHandle handle = self._handle;
						///assert(handle === self._handle, 'handle != self._handle');

						Timers._unrefActive(self);

						///debug('onread', nread);
						Log.d(TAG, "onread "+nread);

						if (nread > 0) {
							///debug('got data');
							Log.d(TAG, "got data");

							// read success.
							// In theory (and in practice) calling readStop right now
							// will prevent this from being called again until _read() gets
							// called again.

							// if it's not enough data, we'll just call handle.readStart()
							// again right away.
							self.bytesRead += nread;

							// Optimization: emit the original buffer with end points
							boolean ret = self.push(buffer, null);

							if (/*handle.reading &&*/ !ret) {
								///handle.reading = false;
								Log.d(TAG, "readStop");
								handle.readStop();
								///var err = handle.readStop();
								///if (err)
								///	self._destroy(errnoException(err, "read"));
							}
							return;
						}

						// if we didn't get any bytes, that doesn't necessarily mean EOF.
						// wait for the next one.
						if (nread == 0) {
							Log.d(TAG, "not any data, keep waiting");
							return;
						}

						// Error, possibly EOF.
						///if (nread != uv.UV_EOF) {
						///	return self._destroy(errnoException(nread, "read"));
						///}

						Log.d(TAG, "EOF");

						if (self.get_readableState().length == 0) {
							self.readable = false;
							maybeDestroy(self);
						}

						// push a null to signal the end of data.
						self.push(null, null);

						// internal end event so that we know that the actual socket
						// is no longer readable, and we can start the shutdown
						// procedure. No need to wait for all the data to be consumed.
						self.emit("_socketEnd");
					}

				};
				///self._handle.onread = onread;
				self._handle.setReadCallback(onread);

				// If handle doesn't support writev - neither do we
				///if (!self._handle.writev)
				self._writev = null;
			}
		}

		// Call whenever we set writable=false or readable=false
		protected static void maybeDestroy(Socket socket) throws Exception {
			if (
					!socket.readable &&
					!socket.writable &&
					!socket.destroyed &&
					!socket._connecting &&
					socket._writableState.length==0) {
				socket.destroy(null);
			}
		}

		public void destroy(String exception) throws Exception {
			Log.d(TAG, "destroy "+exception);
			this._destroy(exception, null);
		}

		private void _destroy(final String exception, final Listener cb) throws Exception {
			Log.d(TAG, "destroy");

			final Socket self = this;

			Listener fireErrorCallbacks = new Listener() {

				@Override
				public void onEvent(Object data) throws Exception {
					if (cb != null) cb.onEvent(exception);
					if (exception!=null && !self._writableState.errorEmitted) {
						// TBD...
						///process.nextTick(function() {
						context.nextTick(new NodeContext.nextTickCallback() {
							
					    	public void onNextTick() throws Exception {
								self.emit("error", exception);
					    	}

						});
						self._writableState.errorEmitted = true;
					}
				}

			};

			if (this.destroyed) {
				Log.d(TAG, "already destroyed, fire error callbacks");
				fireErrorCallbacks.onEvent(null);
				return;
			}

			self._connecting = false;

			this.readable = this.writable = false;

			Timers.unenroll(this);

			Log.d(TAG, "close");
			if (this._handle != null) {
				///if (this !== process.stderr)
				///debug('close handle');
				Log.d(TAG, "close handle");

				final boolean isException = exception != null ? true : false;

				/*this._handle.close(function() {
			      debug('emit close');
			      self.emit('close', isException);
			    });*/
				this._handle.close(new StreamCloseCallback() {

					@Override
					public void onClose() throws Exception { 
						Log.d(TAG, "emit close");
						self.emit("close", isException);
					}

				});

				///this._handle.onread = noop;
				this._handle.setReadCallback(new StreamReadCallback(){

					@Override
					public void onRead(ByteBuffer data) throws Exception {
						// TODO Auto-generated method stub

					}

				});

				this._handle = null;
			}

			// we set destroyed to true before firing error callbacks in order
			// to make it re-entrance safe in case Socket.prototype.destroy()
			// is called within callbacks
			this.destroyed = true;
			fireErrorCallbacks.onEvent(null);

			if (this.server != null) {
				// TBD...
				///COUNTER_NET_SERVER_CONNECTION_CLOSE(this);
				Log.d(TAG, "has server");
				this.server._connections--;
				///if (this.server._emitCloseIfDrained) {
				this.server._emitCloseIfDrained();
				///}
			}
		}

		private Socket() {super(null, null, null);}

		// TBD...
		/*Socket.prototype.listen = function() {
			  debug('socket.listen');
			  var self = this;
			  self.on('connection', arguments[0]);
			  listen(self, null, null, null);
			};


			Socket.prototype.setTimeout = function(msecs, callback) {
			  if (msecs > 0 && isFinite(msecs)) {
			    timers.enroll(this, msecs);
			    timers._unrefActive(this);
			    if (callback) {
			      this.once('timeout', callback);
			    }
			  } else if (msecs === 0) {
			    timers.unenroll(this);
			    if (callback) {
			      this.removeListener('timeout', callback);
			    }
			  }
			};


			Socket.prototype._onTimeout = function() {
			  debug('_onTimeout');
			  this.emit('timeout');
			};
		 */

		public void setTimeout(int msecs, Listener callback) throws Exception {
			if (msecs > 0 /*&& isFinite(msecs)*/) {
				Timers.enroll(this, msecs);
				Timers._unrefActive(this);
				if (callback != null) {
					this.once("timeout", callback);
				}
			} else if (msecs == 0) {
				Timers.unenroll(this);
				if (callback != null) {
					this.removeListener("timeout", callback);
				}
			}
		}

		private void _onTimeout() throws Exception {
			Log.d(TAG, "_onTimeout");
			this.emit("timeout");
		}

		public void setNoDelay(final boolean enable) {
			// backwards compatibility: assume true when `enable` is omitted
			if (this._handle /*&& this._handle.setNoDelay*/ != null)
				this._handle.setNoDelay(enable);
		}

		public void setKeepAlive(final boolean enable, final int delay) {
			// backwards compatibility: assume true when `enable` is omitted
			if (this._handle /*&& this._handle.setNoDelay*/ != null)
				this._handle.setKeepAlive(enable, delay);
		}

		public String remoteAddress() {
			return this._getpeername().getIp();
		}

		public int remotePort() {
			return this._getpeername().getPort();
		}

		public String remoteFamily() {
			return this._getpeername().getFamily();
		}

		private Address _getpeername() {
			if (null == this._handle /*|| !this._handle.getpeername*/) {
				return null;
			}
			if (null == this._peername) {
				Address out = this._handle.getPeerName();
				if (null == out) return null;  // FIXME(bnoordhuis) Throw?
				this._peername = out;
			}
			return this._peername;
		}

		public Address address() {
			return this._getsockname();
		}

		public String localAddress() {
			return this._getsockname().getIp();
		}

		public int localPort() {
			return this._getsockname().getPort();
		}

		public String family() {
			return this._getsockname().getFamily();
		}

		private Address _getsockname() {
			if (null == this._handle /*|| !this._handle.getsockname*/) {
				return null;
			}
			if (null == this._sockname) {
				Address out = this._handle.getSocketName();
				if (null == out) return null;  // FIXME(bnoordhuis) Throw?
				this._sockname = out;
			}
			return this._sockname;
		}
		
		public int bytesRead() {
			return this.bytesRead;
		}

		public int bytesWritten() throws UnsupportedEncodingException {
			int bytes = this._bytesDispatched;
			com.iwebpp.node.Writable2.State state = this._writableState;
			Object data = this._pendingData;
			String encoding = this._pendingEncoding;

			/*state.buffer.forEach(function(el) {
				if (util.isBuffer(el.chunk))
					bytes += el.chunk.length;
				else
					bytes += Buffer.byteLength(el.chunk, el.encoding);
			});*/
			for (WriteReq el : state.buffer) {
				if (Util.isBuffer(el.getChunk()))
					bytes += Util.chunkLength(el.getChunk());
				else
					bytes += Util.stringByteLength((String) el.getChunk(), el.getEncoding());
			}

			if (data != null) {
				if (Util.isBuffer(data))
					bytes += Util.chunkLength(data);
				else
					bytes += Util.stringByteLength((String) data, encoding);
			}

			return bytes;
		}

		@Override
		public Object read(int n) throws Exception {
			if (n == 0)
				return super.read(n);

			///this.read = stream.Readable.prototype.read;
			this._consuming = true;
			return super.read(n);
		}

		@Override
		public boolean write(Object chunk, String encoding, WriteCB cb) throws Exception {
			// check on writeAfterFIN 
			if (!this.allowHalfOpen && this.get_readableState().ended) {
				return writeAfterFIN(chunk, encoding, cb);
			} else {
				if (!Util.isString(chunk) && !Util.isBuffer(chunk))
					throw new Exception("invalid data");
				return super.write(chunk, encoding, cb);
			}
		}
		// Provide a better error message when we call end() as a result
		// of the other side sending a FIN.  The standard 'write after end'
		// is overly vague, and makes it seem like the user's code is to blame.
		private boolean writeAfterFIN(Object chunk, String encoding, final WriteCB cb) throws Exception {
			/*if (util.isFunction(encoding)) {
		    cb = encoding;
		    encoding = null;
		  }*/

			///var er = new Error('This socket has been ended by the other party');
			///er.code = 'EPIPE';
			final String er = "This socket has been ended by the other party";
			Socket self = this;
			// TODO: defer error events consistently everywhere, not just the cb
			self.emit("error", er);
			///if (util.isFunction(cb)) {
			if (cb != null) {
				///process.nextTick(function() {
				context.nextTick(new NodeContext.nextTickCallback() {

					@Override
					public void onNextTick() throws Exception {	
						cb.writeDone(er);
					}
					
				});
			}

			return false;
		}

		public String readyState() {
			if (this._connecting) {
				return "opening";
			} else if (this.readable && this.writable) {
				return "open";
			} else if (this.readable && !this.writable) {
				return "readOnly";
			} else if (!this.readable && this.writable) {
				return "writeOnly";
			} else {
				return "closed";
			}
		}

		public int bufferSize() {
			if (this._handle != null) {
				return (int) (this._handle.writeQueueSize() + this._writableState.length);
			}

			return 0;
		}

		// Just call handle.readStart until we have enough in the buffer
		@Override
		public void _read(final int n) throws Exception {
			final Socket self = this;

			Log.d(TAG, "_read");

			if (this._connecting || null==this._handle) {
				Log.d(TAG, "_read wait for connection");
				///this.once("connect", this._read.bind(this, n));
				this.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						self._read(n);
					}

				});
			} else if (!this._handle.reading) {
				// not already reading, start the flow
				Log.d(TAG, "Socket._read readStart");
				this._handle.reading = true;
				///var err = this._handle.readStart();
				///if (err)
				///	this._destroy(errnoException(err, "read"));
				this._handle.readStart();
			}
		}

		@Override
		public boolean end(Object data, String encoding, WriteCB cb) throws Exception {
			///stream.Duplex.prototype.end.call(this, data, encoding);
			super.end(data, encoding, null);
			this.writable = false;
			///DTRACE_NET_STREAM_END(this);

			// just in case we're waiting for an EOF.
			if (this.readable && !this.get_readableState().endEmitted)
				this.read(0);
			else
				maybeDestroy(this);
			
			return false;
		}

		/*
Socket.prototype._writev = function(chunks, cb) {
  this._writeGeneric(true, chunks, '', cb);
};*/

		@Override
		public void _write(Object chunk, String encoding, WriteCB cb)
				throws Exception {
			this._writeGeneric(false, chunk, encoding, cb);
		}

		private void _writeGeneric(final boolean writev, final Object data, final String encoding,
				final WriteCB cb) throws Exception {
			final Socket self = this;

			// If we are still connecting, then buffer this for later.
			// The Writable logic will buffer up any more writes while
			// waiting for this one to be done.
			if (this._connecting) {
				this._pendingData = data;
				this._pendingEncoding = encoding;
				/*
			    this.once("connect", function() {
			      this._writeGeneric(writev, data, encoding, cb);
			    });*/
				this.once("connect", new Listener(){

					@Override
					public void onEvent(Object dummy) throws Exception {
						// TODO Auto-generated method stub
						self._writeGeneric(writev, data, encoding, cb);
					}

				});

				return;
			}
			this._pendingData = null;
			this._pendingEncoding = "";

			Timers._unrefActive(this);

			if (null == this._handle) {
				///this._destroy("This socket is closed.", cb);
				this._destroy("This socket is closed.", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						cb.writeDone("This socket is closed.");
					}

				});

				return;
			}

			/*
			var req = { oncomplete: afterWrite, async: false };
			var err;

			if (writev) {
			    var chunks = new Array(data.length << 1);
			    for (var i = 0; i < data.length; i++) {
			      var entry = data[i];
			      var chunk = entry.chunk;
			      var enc = entry.encoding;
			      chunks[i * 2] = chunk;
			      chunks[i * 2 + 1] = enc;
			    }
			    err = this._handle.writev(req, chunks);

			    // Retain chunks
			    if (err == 0) req._chunks = chunks;
			  } else 
			{
				String enc;
				if (Util.isBuffer(data)) {
					req.buffer = data;  // Keep reference alive.
					enc = "buffer";
				} else {
					enc = encoding;
				}
				err = createWriteReq(req, this._handle, data, enc);
			}

			if (err)
				return this._destroy(errnoException(err, "write", req.error), cb);

			this._bytesDispatched += req.bytes;

			// If it was entirely flushed, we can write some more right now.
			// However, if more is left in the queue, then wait until that clears.
			if (req.async && this._handle.writeQueueSize != 0)
				req.cb = cb;
			else
				cb();
			 */
			// afterWrite
			this._handle.setWriteCallback(new StreamWriteCallback(){

				@Override
				public void onWrite(int status, Exception err)
						throws Exception {
					///var self = handle.owner;
					///if (self !== process.stderr && self !== process.stdout)
					Log.d(TAG, "afterWrite "+status);

					// callback may come after call to destroy.
					if (self.destroyed) {
						Log.d(TAG, "afterWrite destroyed");
						return;
					}

					if (status < 0) {
						///var ex = errnoException(status, 'write', err);
						String ex = "" + status + " write " + err;
						Log.d(TAG, "write failure:" + ex);
						///self._destroy(ex, req.cb);
						self._destroy(ex, null);
						return;
					}

					Timers._unrefActive(self);

					///if (self !== process.stderr && self !== process.stdout)
					Log.d(TAG, "afterWrite call cb");

					// TBD...
					///if (req.cb)
					///	req.cb.call(self);
				}

			});

			int err = 0;
			if (Util.isBuffer(data)) {
				err = this._handle.write((ByteBuffer)data);
			} else if (Util.isString(data)) {
				err = this._handle.write((String)data, encoding);
			} else {
				this._destroy("write invalid data", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						cb.writeDone("write invalid data");
					}

				});

				return;
			}

			if (err != 0) {
				this._destroy("write invalid data", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						cb.writeDone("write invalid data");
					}

				});

				return;
			}

			// TBD...
			///this._bytesDispatched += req.bytes;

			// If it was entirely flushed, we can write some more right now.
			// However, if more is left in the queue, then wait until that clears.
			///if (req.async && this._handle.writeQueueSize() != 0)
			///	req.cb = cb;
			///else
			cb.writeDone(null);

			return;
		}

		public static interface ConnectCallback {
			public void onConnect() throws Exception;
		}
		
		public void connect(int port, final ConnectCallback cb) throws Exception {
			// check handle //////////////////////
			if (this.destroyed) {
				this.get_readableState().reading = false;
				this.get_readableState().ended = false;
				this.get_readableState().endEmitted = false;
				this._writableState.ended = false;
				this._writableState.ending = false;
				this._writableState.finished = false;
				this._writableState.errorEmitted = false;
				this.destroyed = false;
				this._handle = null;
			}

			Socket self = this;
			///var pipe = !!options.path;
			///debug('pipe', pipe, options.path);

			if (null == this._handle) {
				///this._handle = pipe ? createPipe() : createUDT(context.getLoop());
				this._handle = createUDT(context.getLoop());
				initSocketHandle(this);
			}

			///if (util.isFunction(cb)) {
			if (cb != null) {
				self.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						cb.onConnect();
					}
					
				});
			}

			Timers._unrefActive(this);

			self._connecting = true;
			self.writable = true;
			///////////////////////////////////////////////

			connect(4, null, port, null, -1);
		}

		public void connect(String address ,int port, final ConnectCallback cb) throws Exception {
			// check handle //////////////////////
			if (this.destroyed) {
				this.get_readableState().reading = false;
				this.get_readableState().ended = false;
				this.get_readableState().endEmitted = false;
				this._writableState.ended = false;
				this._writableState.ending = false;
				this._writableState.finished = false;
				this._writableState.errorEmitted = false;
				this.destroyed = false;
				this._handle = null;
			}

			Socket self = this;
			///var pipe = !!options.path;
			///debug('pipe', pipe, options.path);

			if (null == this._handle) {
				///this._handle = pipe ? createPipe() : createUDT(context.getLoop());
				this._handle = createUDT(context.getLoop());
				initSocketHandle(this);
			}

			///if (util.isFunction(cb)) {
			if (cb != null) {
				self.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						cb.onConnect();
					}
					
				});
			}

			Timers._unrefActive(this);

			self._connecting = true;
			self.writable = true;
			///////////////////////////////////////////////

			connect(4, address, port, null, -1);
		}

		public void connect(
				String address ,int port,
				int localPort, final ConnectCallback cb) throws Exception {
			// check handle //////////////////////
			if (this.destroyed) {
				this.get_readableState().reading = false;
				this.get_readableState().ended = false;
				this.get_readableState().endEmitted = false;
				this._writableState.ended = false;
				this._writableState.ending = false;
				this._writableState.finished = false;
				this._writableState.errorEmitted = false;
				this.destroyed = false;
				this._handle = null;
			}

			Socket self = this;
			///var pipe = !!options.path;
			///debug('pipe', pipe, options.path);

			if (null == this._handle) {
				///this._handle = pipe ? createPipe() : createUDT(context.getLoop());
				this._handle = createUDT(context.getLoop());
				initSocketHandle(this);
			}

			///if (util.isFunction(cb)) {
			if (cb != null) {
				self.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						cb.onConnect();
					}
					
				});
			}

			Timers._unrefActive(this);

			self._connecting = true;
			self.writable = true;
			///////////////////////////////////////////////

			connect(4, address, port, null, localPort);
		}

		public void connect(
				String address ,int port,
				String localAddress, final ConnectCallback cb) throws Exception {
			// check handle //////////////////////
			if (this.destroyed) {
				this.get_readableState().reading = false;
				this.get_readableState().ended = false;
				this.get_readableState().endEmitted = false;
				this._writableState.ended = false;
				this._writableState.ending = false;
				this._writableState.finished = false;
				this._writableState.errorEmitted = false;
				this.destroyed = false;
				this._handle = null;
			}

			Socket self = this;
			///var pipe = !!options.path;
			///debug('pipe', pipe, options.path);

			if (null == this._handle) {
				///this._handle = pipe ? createPipe() : createUDT(context.getLoop());
				this._handle = createUDT(context.getLoop());
				initSocketHandle(this);
			}

			///if (util.isFunction(cb)) {
			if (cb != null) {
				self.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						cb.onConnect();
					}
					
				});
			}

			Timers._unrefActive(this);

			self._connecting = true;
			self.writable = true;
			///////////////////////////////////////////////

			connect(4, address, port, localAddress, -1);
		}

		public void connect(
				String address ,int port, 
				String localAddress, int localPort, final ConnectCallback cb) throws Exception {
			// check handle //////////////////////
			if (this.destroyed) {
				this.get_readableState().reading = false;
				this.get_readableState().ended = false;
				this.get_readableState().endEmitted = false;
				this._writableState.ended = false;
				this._writableState.ending = false;
				this._writableState.finished = false;
				this._writableState.errorEmitted = false;
				this.destroyed = false;
				this._handle = null;
			}

			Socket self = this;
			///var pipe = !!options.path;
			///debug('pipe', pipe, options.path);

			if (null == this._handle) {
				///this._handle = pipe ? createPipe() : createUDT(context.getLoop());
				this._handle = createUDT(context.getLoop());
				initSocketHandle(this);
			}

			///if (util.isFunction(cb)) {
			if (cb != null) {
				self.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						cb.onConnect();
					}
					
				});
			}

			Timers._unrefActive(this);

			self._connecting = true;
			self.writable = true;
			///////////////////////////////////////////////

			connect(4, address, port, localAddress, localPort);
		}

		public void connect(int addressType, String address ,int port, 
				final ConnectCallback cb) throws Exception {
			// check handle //////////////////////
			if (this.destroyed) {
				this.get_readableState().reading = false;
				this.get_readableState().ended = false;
				this.get_readableState().endEmitted = false;
				this._writableState.ended = false;
				this._writableState.ending = false;
				this._writableState.finished = false;
				this._writableState.errorEmitted = false;
				this.destroyed = false;
				this._handle = null;
			}

			Socket self = this;
			///var pipe = !!options.path;
			///debug('pipe', pipe, options.path);

			if (null == this._handle) {
				///this._handle = pipe ? createPipe() : createUDT(context.getLoop());
				this._handle = createUDT(context.getLoop());
				initSocketHandle(this);
			}

			///if (util.isFunction(cb)) {
			if (cb != null) {
				self.once("connect", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						cb.onConnect();
					}
					
				});
			}

			Timers._unrefActive(this);

			self._connecting = true;
			self.writable = true;
			///////////////////////////////////////////////

			connect(addressType, address, port, null, -1);
		}

		private void connect(int addressType, String address, int port, 
				String localAddress, int localPort) throws Exception {
			final Socket self = this;

			// TODO return promise from Socket.prototype.connect which
			// wraps _connectReq.

			/*assert.ok(self._connecting);

			  String err;
			  if (localAddress!=null || localPort>0) {
			    if (!Util.zeroString(localAddress) && !exports.isIP(localAddress))
			      err = new TypeError(
			          'localAddress should be a valid IP: ' + localAddress);
			    if (localPort && !util.isNumber(localPort))
			      err = new TypeError('localPort should be a number: ' + localPort);

			    var bind;

			    switch (addressType) {
			      case 4:
			        if (!localAddress)
			          localAddress = "0.0.0.0";
			        bind = self._handle.bind;
			        break;
			      case 6:
			        if (!localAddress)
			          localAddress = '::';
			        bind = self._handle.bind6;
			        break;
			      default:
			        err = new TypeError('Invalid addressType: ' + addressType);
			        break;
			    }

			    if (err) {
			      self._destroy(err);
			      return;
			    }

			    debug('binding to localAddress: %s and localPort: %d',
			          localAddress,
			          localPort);

			    bind = bind.bind(self._handle);
			    err = bind(localAddress, localPort);

			    if (err) {
			      self._destroy(errnoException(err, 'bind'));
			      return;
			    }
			  }
			 */

			// Always bind first
			// TBD... isIP
			if (Util.zeroString(localAddress)) {
				localAddress = (addressType == 6) ? "::" : "0.0.0.0";
			}
			if (localPort < 0 || localPort >= 65536) {
				localPort = 0;
			}

			Log.d(TAG, "binding to localAddress: " + localAddress +
					" and localPort: " + localPort);

			int err = 0;
			if (addressType == 6) {
				err = this._handle.bind6(localAddress, localPort);  
			} else {
				err = this._handle.bind(localAddress, localPort);  
			}
			if (err != 0) {
				///self._destroy(errnoException(err, 'bind'));
				this._destroy("err bind", null);
				return;
			}

			// Try to connect ...

			// afterConnect
			StreamConnectCallback afterConnect = new StreamConnectCallback() {

				@Override
				public void onConnect(int status, Exception error)
						throws Exception {
					///var self = handle.owner;

					// callback may come after call to destroy
					if (self.destroyed) {
						return;
					}

					///assert(handle === self._handle, 'handle != self._handle');

					Log.d(TAG, "afterConnect");

					///assert.ok(self._connecting);
					self._connecting = false;

					if (status == 0) {
						self.readable = readable;
						self.writable = writable;
						Timers._unrefActive(self);

						self.emit("connect");

						// start the first read, or get an immediate EOF.
						// this doesn't actually consume any bytes, because len=0.
						if (readable)
							self.read(0);

					} else {
						self._connecting = false;
						///self._destroy(errnoException(status, 'connect'));
						self._destroy("err connect status: "+status, null);
					}
				}

			};
			this._handle.setConnectCallback(afterConnect);

			if (Util.zeroString(address)) {
				address = (addressType == 6) ? "::1" : "127.0.0.1";
			}

			if (port <= 0 || port > 65535)
				throw new Exception("Port should be > 0 and < 65536");

			if (addressType == 6) {
				err = this._handle.connect6(address, port);
			} else {
				err = this._handle.connect(address, port);
			}

			if (err != 0) {
				///self._destroy(errnoException(err, 'connect'));
				this._destroy("err connect", null);
			}

			/*
			  var req = { oncomplete: afterConnect };
			  if (addressType === 6 || addressType === 4) {
			    port = port | 0;
			    if (port <= 0 || port > 65535)
			      throw new RangeError('Port should be > 0 and < 65536');

			    if (addressType === 6) {
			      err = self._handle.connect6(req, address, port);
			    } else if (addressType === 4) {
			      err = self._handle.connect(req, address, port);
			    }
			  } else {
			    err = self._handle.connect(req, address, afterConnect);
			  }

			  if (err) {
			    self._destroy(errnoException(err, 'connect'));
			  }
			 */
		}

	}
	
	// /* [ options, ] listener */
	public static final class Server extends EventEmitter2 {
		private final static String TAG = "UDT:Server";

		private int _connections;
		private UDTHandle _handle;
		private boolean _usingSlaves;
		private List<List<Socket>> _slaves;
		private boolean allowHalfOpen;

		private String _connectionKey;

		private int maxConnections = 1024;

		private Address _sockname;

		private NodeContext context;

		private void _emitCloseIfDrained() throws Exception {
			Log.d(TAG, "SERVER _emitCloseIfDrained");
			final Server self = this;

			if (self._handle!=null || self._connections>0) {
				Log.d(TAG, "SERVER handle? " + self._handle +
						" connections? " + self._connections);
				return;
			}

			// TBD...
			///process.nextTick(function() {
			context.nextTick(new NodeContext.nextTickCallback() {

				@Override
				public void onNextTick() throws Exception {
					Log.d(TAG, "SERVER: emit close");
					self.emit("close");
				}

			});
		}

		public Server(final NodeContext context, Options options, final ConnectionCallback listener) throws Exception {
			Server self = this;

			// node context
			this.context = context;
			
			// set initial onConnection callback
			if (listener != null) {
				self.on("connection", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						Socket socket = (Socket)data;
						listener.onConnection(socket);
					}

				});
			}

			this._connections = 0;

			/*
			 * 
  Object.defineProperty(this, 'connections', {
    get: util.deprecate(function() {

      if (self._usingSlaves) {
        return null;
      }
      return self._connections;
    }, 'connections property is deprecated. Use getConnections() method'),
    set: util.deprecate(function(val) {
      return (self._connections = val);
    }, 'connections property is deprecated. Use getConnections() method'),
    configurable: true, enumerable: true
  });*/

			this._handle = null;
			this._usingSlaves = false;
			this._slaves = new ArrayList< List<Socket> >();

			this.allowHalfOpen = options.allowHalfOpen;
		}

		public static class Options {

			public boolean allowHalfOpen;

			public Options(boolean allowHalfOpen) {
				this.allowHalfOpen = allowHalfOpen;
			}
			@SuppressWarnings("unused")
			private Options(){}
		} 

		public static interface ConnectionCallback {
			public void onConnection(Socket socket);
		}

		public static interface CloseCallback {
			public void onClose(String error);
		}

		public static interface ListenCallback {
			public void onListen();
		}
		
		private static int _listen(UDTHandle handle, int backlog) {
			// Use a backlog of 512 entries. We pass 511 to the listen() call because
			// the kernel does: backlogsize = roundup_pow_of_two(backlogsize + 1);
			// which will thus give us a backlog of 512 entries.
			return handle.listen(backlog>0? backlog : 511);
		}

		private UDTHandle _createServerHandle(String address, int port, int addressType, int fd) {
			UDTHandle handle = createUDT(context.getLoop());
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

		private void _listen2(String address, int port, int addressType, 
				int backlog, int fd) throws Exception {
			Log.d(TAG, "listen2 "+address+":"+port+":"+addressType+":"+backlog);
			final Server self = this;

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
				UDTHandle rval = _createServerHandle(address, port, addressType, fd);
				if (rval == null) {
					final String error = "err listen";
					///process.nextTick(function() {
					context.nextTick(new NodeContext.nextTickCallback() {
						
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
					UDTHandle handle = self._handle;
					UDTHandle clientHandle = createUDT(context.getLoop());
					int err = handle.accept(clientHandle);


					Log.d(TAG, "onconnection");

					if (err!=0) {
						///self.emit('error', errnoException(err, 'accept'));
						self.emit("error", "err accept "+err);
						return;
					}

					if (/*self.maxConnections &&*/ self._connections >= self.maxConnections) {
						Log.d(TAG, "exceed maxim connections");
						
						clientHandle.close();
						return;
					}

					/*Socket socket = new Socket({
						handle: clientHandle,
						allowHalfOpen: self.allowHalfOpen
					});*/
					Socket socket = new Socket(context, new Socket.Options(self.allowHalfOpen, clientHandle));
					socket.readable = socket.writable = true;


					self._connections++;
					socket.server = self;

					///DTRACE_NET_SERVER_CONNECTION(socket);
					///COUNTER_NET_SERVER_CONNECTION(socket);
					self.emit("connection", socket);
				}

			};
			self._handle.setConnectionCallback(onconnection);

			self._handle.owner = self;

			int err = 0;
			if (!alreadyListening)
				err = _listen(self._handle, backlog);

			if (0!=err) {
				///var ex = errnoException(err, "listen");
				final String ex = "err listen";
				self._handle.close();
				self._handle = null;
				///process.nextTick(function() {
				context.nextTick(new NodeContext.nextTickCallback() {
					
					@Override
					public void onNextTick() throws Exception {
						self.emit("error", ex);
					}
					
				});
				
				Log.d(TAG, ex);

				return;
			}

			// generate connection key, this should be unique to the connection
			this._connectionKey = addressType + ':' + address + ':' + port;

			///process.nextTick(function() {
			context.nextTick(new NodeContext.nextTickCallback() {

				@Override
				public void onNextTick() throws Exception {
					// ensure handle hasn't closed
					if (self._handle != null)
						self.emit("listening");
				}

			});
		}
		
		public void listen(String address, int port, int addressType, 
				int backlog, int fd, final ListenCallback cb) throws Exception {
			Server self = this;

			///if (util.isFunction(lastArg)) {
			if (cb != null) {
				self.once("listening", new Listener(){

					@Override
					public void onEvent(Object data) throws Exception {
						// TODO Auto-generated method stub
						cb.onListen();
					}

				});
			}

			_listen2(address, port, addressType, backlog, fd);
		}
		
		public Address address() {
			return this._getsockname();
		}

		public String localAddress() {
			return this._getsockname().getIp();
		}

		public int localPort() {
			return this._getsockname().getPort();
		}

		public String family() {
			return this._getsockname().getFamily();
		}

		private Address _getsockname() {
			if (null == this._handle /*|| !this._handle.getsockname*/) {
				return null;
			}
			if (null == this._sockname) {
				Address out = this._handle.getSocketName();
				if (null == out) return null;  // FIXME(bnoordhuis) Throw?
				this._sockname = out;
			}
			return this._sockname;
		}
		
		public int getConnections() {

			/*
			 * 
  function end(err, connections) {
    process.nextTick(function() {
      cb(err, connections);
    });
  }

  if (!this._usingSlaves) {
    return end(null, this._connections);
  }

  // Poll slaves
  var left = this._slaves.length,
      total = this._connections;

  function oncount(err, count) {
    if (err) {
      left = -1;
      return end(err);
    }

    total += count;
    if (--left === 0) return end(null, total);
  }

  this._slaves.forEach(function(slave) {
    slave.getConnections(oncount);
  });
			 */

			return this._connections;
		}

		public Server close(final CloseCallback cb) throws Exception {

			if (cb!=null) {
				if (null==this._handle) {
					this.once("close", new Listener(){

						@Override
						public void onEvent(Object data) throws Exception {
							// TODO Auto-generated method stub
							cb.onClose("Not running");
						}

					});
				} else {
					///this.once("close", cb);
					this.once("close", new Listener(){

						@Override
						public void onEvent(Object data) throws Exception {
							// TODO Auto-generated method stub
							cb.onClose((String) data);
						}

					});
				}
			}


			if (this._handle != null) {
				this._handle.close();
				this._handle = null;
			}


			this._emitCloseIfDrained();

			return this;
		}

		protected void _setupSlave(List<Socket> socketList) {
			this._usingSlaves = true;
			this._slaves.add(socketList);
		}

		public void ref() {
			if (this._handle!=null)
				this._handle.ref();
		};

		public void unref() {
			if (this._handle!=null)
				this._handle.unref();
		}


	}

	private static UDTHandle createUDT(final LoopHandle loop) {
		return new UDTHandle(loop);
	}

}
