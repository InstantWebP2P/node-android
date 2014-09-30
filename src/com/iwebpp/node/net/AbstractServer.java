// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.net;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.iwebpp.libuvpp.Address;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.StreamHandle;
import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;


public abstract class AbstractServer 
extends EventEmitter2 {
	private final static String TAG = "AbstractServer";

	private int _connections;
	protected StreamHandle _handle;
	private List<List<AbstractSocket>> _slaves;
	private boolean allowHalfOpen;

	private int maxConnections = 1024;

	private Address _sockname;

	protected NodeContext context;

	protected void _emitCloseIfDrained() throws Exception {
		Log.d(TAG, "SERVER _emitCloseIfDrained");
		final AbstractServer self = this;

		if (self._handle!=null || self.get_connections()>0) {
			Log.d(TAG, "SERVER handle? " + self._handle +
					" connections? " + self.get_connections());
			return;
		}

		// TBD...
		///process.nextTick(function() {
		context.nextTick(new NodeContext.nextTickListener() {

			@Override
			public void onNextTick() throws Exception {
				Log.d(TAG, "SERVER: emit close");
				self.emit("close");
			}

		});
	}

	public AbstractServer(
			final NodeContext context, 
			Options options, 
			final ConnectionListener listener) throws Exception {
		AbstractServer self = this;

		// node context
		this.context = context;

		// set initial onConnection callback
		if (listener != null) {
			self.on("connection", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					// TODO Auto-generated method stub
					AbstractSocket socket = (AbstractSocket)data;
					listener.onConnection(socket);
				}

			});
		}

		this.set_connections(0);

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
		this._slaves = new ArrayList< List<AbstractSocket> >();

		this.setAllowHalfOpen(options.allowHalfOpen);
	}
	@SuppressWarnings("unused")
	private AbstractServer(){}

	public static class Options {

		public boolean allowHalfOpen;

		public Options(boolean allowHalfOpen) {
			this.allowHalfOpen = allowHalfOpen;
		}
		@SuppressWarnings("unused")
		private Options(){}
	} 

	protected static int _listen(StreamHandle handle, int backlog) {
		// Use a backlog of 512 entries. We pass 511 to the listen() call because
		// the kernel does: backlogsize = roundup_pow_of_two(backlogsize + 1);
		// which will thus give us a backlog of 512 entries.
		return handle.listen(backlog>0? backlog : 511);
	}

	public void listen(String address, int port, int addressType, 
			int backlog, int fd, final ListeningCallback cb) throws Exception {
		AbstractServer self = this;

		///if (util.isFunction(lastArg)) {
		if (cb != null) {
			self.once("listening", new Listener(){

				@Override
				public void onEvent(Object data) throws Exception {
					// TODO Auto-generated method stub
					cb.onListening();
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
			Address out = this._getSocketName();
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

		return this.get_connections();
	}

	public void close(final CloseListener cb) throws Exception {

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
						cb.onClose(data!=null? data.toString() : "");
					}

				});
			}
		}


		if (this._handle != null) {
			this._handle.close();
			this._handle = null;
		}


		this._emitCloseIfDrained();
	}

	protected void _setupSlave(List<AbstractSocket> socketList) {
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


	// Event listeners
	public void onConnection(final ConnectionListener cb) throws Exception {
		this.on("connection", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				AbstractSocket data = (AbstractSocket)raw;
				cb.onConnection(data);					
			}

		});
	}
	public static interface ConnectionListener {
		public void onConnection(AbstractSocket socket);
	}

	public void onceClose(final CloseListener cb) throws Exception {
		this.once("close", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				cb.onClose(raw!=null? raw.toString() : "");					
			}

		});
	}
	public static interface CloseListener {
		public void onClose(String error);
	}
	
	public void onceListening(final ListeningCallback cb) throws Exception {
		this.once("listening", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				cb.onListening();					
			}

		});
	}
	public static interface ListeningCallback {
		public void onListening();
	}

	public void onError(final ErrorListener cb) throws Exception {
		this.on("error", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				cb.onError(raw!=null? raw.toString() : "");					
			}

		});
	}
	public static interface ErrorListener {
		public void onError(String error);
	}

	/**
	 * @return the _connections
	 */
	public int get_connections() {
		return _connections;
	}

	/**
	 * @param _connections the _connections to set
	 */
	public void set_connections(int _connections) {
		this._connections = _connections;
	}

	/**
	 * @return the maxConnections
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * @param maxConnections the maxConnections to set
	 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * @return the allowHalfOpen
	 */
	public boolean isAllowHalfOpen() {
		return allowHalfOpen;
	}

	/**
	 * @param allowHalfOpen the allowHalfOpen to set
	 */
	public void setAllowHalfOpen(boolean allowHalfOpen) {
		this.allowHalfOpen = allowHalfOpen;
	}
	
	
	// Abstract server methods
    protected abstract int _bind(final String address, final int port);
    protected abstract int _bind6(final String address, final int port);
    
    protected abstract Address _getSocketName();

    protected abstract int _listen(final int backlog);

    protected abstract int _accept(final StreamHandle client);
    
    protected abstract StreamHandle _createHandle(final LoopHandle loop);
    protected abstract StreamHandle _createServerHandle(String address, int port, 
			int addressType, int fd);
	
    protected abstract void _listen2(String address, int port, int addressType, 
			int backlog, int fd) throws Exception;
	
}
