package com.iwebpp.wspp;

import java.nio.ByteBuffer;

import com.iwebpp.node.Util;

public final class BufferPool {

	private ByteBuffer _buffer;
	private int _offset;
	private int _used;
	private int _changeFactor;
	private Strategy _strategy;
	protected int PoolPrevUsed;

	protected BufferPool(int initialSize, Strategy _strategy) {
		this._buffer = initialSize>0 ? ByteBuffer.allocate(initialSize) : null;
		this._offset = 0;
		this._used = 0;
		this._changeFactor = 0;
		
		this._strategy = _strategy;
		
		this.PoolPrevUsed = -1;
	}
	@SuppressWarnings("unused")
	private BufferPool(){}

	protected int size() {
		return this._buffer == null ? 0 : this._buffer.capacity();
	}

	protected int used() {
		return this._used;
	}


	protected ByteBuffer get(int length) {
		if (this._buffer == null || this._offset + length > this._buffer.capacity()) {
			ByteBuffer newBuf = ByteBuffer.allocate(this._strategy._growStrategy(this, length));
			this._buffer = newBuf;
			this._offset = 0;
		}
		this._used += length;
		///var buf = this._buffer.slice(this._offset, this._offset + length);
		ByteBuffer buf = (ByteBuffer) Util.chunkSlice(this._buffer, this._offset, this._offset + length);
		this._offset += length;
		return buf;
	}

	protected void reset(boolean forceNewBuffer) {
		int len = this._strategy._shrinkStrategy(this);
		if (len < this.size()) this._changeFactor -= 1;
		if (forceNewBuffer || this._changeFactor < -2) {
			this._changeFactor = 0;
			this._buffer = len>0 ? ByteBuffer.allocate(len) : null;
		}
		this._offset = 0;
		this._used = 0;
	}
	
	protected interface Strategy {
		public int _growStrategy(BufferPool db, int length);
		public int _shrinkStrategy(BufferPool db);
	}

}
