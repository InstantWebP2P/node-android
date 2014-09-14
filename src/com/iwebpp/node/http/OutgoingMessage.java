package com.iwebpp.node.http;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.TCP;
import com.iwebpp.node.Util;
import com.iwebpp.node.Writable2;
import com.iwebpp.node.NodeContext.nextTickCallback;
import com.iwebpp.node.TCP.Socket;

public class OutgoingMessage 
extends Writable2 {

	private List<Object> output;
	private List<String> outputEncodings;
	private List<WriteCB> outputCallbacks;
	private boolean writable;
	private boolean _last;
	private boolean shouldKeepAlive;
	/**
	 * @return the shouldKeepAlive
	 */
	public boolean isShouldKeepAlive() {
		return shouldKeepAlive;
	}

	/**
	 * @param shouldKeepAlive the shouldKeepAlive to set
	 */
	public void setShouldKeepAlive(boolean shouldKeepAlive) {
		this.shouldKeepAlive = shouldKeepAlive;
	}


	private boolean chunkedEncoding;
	private boolean useChunkedEncodingByDefault;
	private boolean sendDate;
	private boolean _hasBody;
	private String _trailer;
	private boolean finished;
	private boolean _hangupClose;
	private TCP.Socket socket;
	private Socket connection;
	private Map<String, String> _removedHeader;
	private boolean _headerSent;
	private String _header;
	protected NodeContext context;

	public OutgoingMessage(NodeContext ctx, Options options) {
		super(ctx, options);
		this.context = ctx;

		this.output = new ArrayList<Object>();
		this.outputEncodings = new ArrayList<String>();
		this.outputCallbacks = new ArrayList<WriteCB>();

		this.writable = true;

		this._last = false;
		this.chunkedEncoding = false;
		this.shouldKeepAlive = true;
		this.useChunkedEncodingByDefault = true;
		this.sendDate = false;
		this._removedHeader = new Hashtable<String, String>();

		this._hasBody = true;
		this._trailer = "";

		this.finished = false;
		this._hangupClose = false;

		this.socket = null;
		this.connection = null;
	}

	/*
	 * OutgoingMessage.prototype.setTimeout = function(msecs, callback) {
if (callback)
this.on('timeout', callback);
if (!this.socket) {
this.once('socket', function(socket) {
  socket.setTimeout(msecs);
});
} else
this.socket.setTimeout(msecs);
};
	 * */

	// It's possible that the socket will be destroyed, and removed from
	// any messages, before ever calling this.  In that case, just skip
	// it, since something else is destroying this connection anyway.
	///OutgoingMessage.prototype.destroy = function(error) {
	public void destroy(final String error) throws Exception {
		if (this.socket != null)
			this.socket.destroy(error);
		else
			this.once("socket", new Listener() {

				@Override
				public void onListen(Object data) throws Exception {
					TCP.Socket socket = (TCP.Socket)data;
					socket.destroy(error);
				}

			});
	}


	// This abstract either writing directly to the socket or buffering it.
	///OutgoingMessage.prototype._send = function(data, encoding, callback) {
	public boolean _send(Object data, String encoding, WriteCB callback) throws Exception {
		// This is a shameful hack to get the headers and first body chunk onto
		// the same packet. Future versions of Node are going to take care of
		// this at a lower level and in a more general way.
		if (!this._headerSent) {
			if (Util.isString(data) &&
					encoding != "hex" &&
					encoding != "base64") {
				data = this._header + data;
			} else {
				///this.output.unshift(this._header);
				///this.outputEncodings.unshift("binary");
				///this.outputCallbacks.unshift(null);
				this.output.add(0, data);
				this.outputEncodings.add(0, encoding);
				this.outputCallbacks.add(0, null);
			}
			this._headerSent = true;
		}
		return this._writeRaw(data, encoding, callback);
	}


	///OutgoingMessage.prototype._writeRaw = function(data, encoding, callback) {
	public boolean _writeRaw(Object data, String encoding, final WriteCB callback) throws Exception {
		///if (data.length === 0) {
		if (data == null || Util.chunkLength(data)==0) {
			///if (util.isFunction(callback))
			if (callback != null)
				///process.nextTick(callback);
				context.nextTick(new nextTickCallback(){

					@Override
					public void onNextTick() throws Exception {
						callback.writeDone(null);							
					}

				});
			return true;
		}

		if (this.connection!=null &&
				this.connection._httpMessage == this &&
				this.connection.writable() &&
				!this.connection.isDestroyed()) {
			// There might be pending data in the this.output buffer.
			///while (this.output.length) {
			while (this.output.size() > 0) {
				if (!this.connection.writable()) {
					this._buffer(data, encoding, callback);
					return false;
				}
				///var c = this.output.shift();
				///var e = this.outputEncodings.shift();
				///var cb = this.outputCallbacks.shift();
				Object c = this.output.remove(0);
				String e = this.outputEncodings.remove(0);
				WriteCB cb = this.outputCallbacks.remove(0);
				
				this.connection.write(c, e, cb);
			}

			// Directly write to socket.
			return this.connection.write(data, encoding, callback);
		} else if (this.connection!=null && this.connection.isDestroyed()) {
			// The socket was destroyed.  If we're still trying to write to it,
			// then we haven't gotten the 'close' event yet.
			return false;
		} else {
			// buffer, as long as we're not destroyed.
			this._buffer(data, encoding, callback);
			return false;
		}
	}

	private boolean _buffer(Object data, String encoding, WriteCB callback) {
		this.output.add(data);
		this.outputEncodings.add(encoding);
		this.outputCallbacks.add(callback);
		return false;
	}

	
	@Override
	protected void _write(Object chunk, String encoding, WriteCB cb)
			throws Exception {
		// TODO Auto-generated method stub

	}


}
