package com.iwebpp.node;

import java.nio.ByteBuffer;

import android.util.Log;

import com.iwebpp.libuvpp.cb.StreamReadCallback;
import com.iwebpp.libuvpp.handles.TCPHandle;

public final class TCP {
	
	public static final class Socket extends Duplex {
		private final static String TAG = "TCP:Socket";

		private boolean _connecting;
		private boolean _hadError;
		private TCPHandle _handle;
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

		public class TcpOptions {

			public TCPHandle handle;
			public long fd;
			public boolean readable;
			public boolean writable;
			public boolean allowHalfOpen;
			
		};
		
		public Socket(TcpOptions options) throws Exception {
			super(null, null);
			// TODO Auto-generated constructor stub
			

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
			  Listener onSocketFinish = null;
			  this.on("finish", onSocketFinish);

			  Listener onSocketEnd = null;
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
						int nread = buffer.capacity();
						
						///var handle = this;
						///Socket self = handle.owner;
						TCPHandle handle = self._handle;
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

						if (self._readableState.length == 0) {
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
		
		protected static void maybeDestroy(Socket self) {
			// TODO Auto-generated method stub
			
		}

		private Socket() {super(null, null);}

		@Override
		public void _read(int size) throws Exception {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean _write(Object chunk, String encoding, WriteCB cb)
				throws Exception {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	// /* [ options, ] listener */
	public static final class Server extends EventEmitter2 {
		
	}

	public static Object createHandle(long fd) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
