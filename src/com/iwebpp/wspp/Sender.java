// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>

package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.stream.Writable.WriteCB;


/**
 * HyBi Sender implementation
 */
public class Sender 
extends EventEmitter2 {
	private AbstractSocket _socket;
	private boolean firstFragment;
	private NodeContext context;
	private byte[] _randomMask;

	public Sender(NodeContext ctx, AbstractSocket socket) {
		this.context = ctx;

		this._socket = socket;
		this.firstFragment = true;
		this._randomMask = null;
	}
	@SuppressWarnings("unused")
	private Sender(){}


	/**
	 * Sends a close instruction to the remote party.
	 * @throws Exception 
	 *
	 * @api public
	 */
	public boolean close(int code, Object data, boolean mask) throws Exception {
		if (code > 0) {
			if (!ErrorCodes.isValidErrorCode(code)) 
				throw new Exception("first argument must be a valid error code number");
		}
		code = code > 0 ? code : 1000;
		///var dataBuffer = new Buffer(2 + (data ? Buffer.byteLength(data) : 0));
		ByteBuffer dataBuffer = ByteBuffer.allocate(2 + (data!=null ? Util.chunkByteLength(data, "utf8") : 0));

		///writeUInt16BE.call(dataBuffer, code, 0);
		dataBuffer.order(ByteOrder.BIG_ENDIAN).putShort(0, (short) (code & 0xffff));

		///if (dataBuffer.length > 2) dataBuffer.write(data, 2);
		// TBD...
		if (dataBuffer.capacity() > 2) dataBuffer.put(Util.chunkToBuffer(data, "utf8"));

		return this.frameAndSend(0x8, dataBuffer, true, mask, null);
	}

	public static final class SendOptions {
		public boolean binary;
		public boolean mask;
		public boolean fin;

		SendOptions(boolean binary, boolean mask, boolean fin) {
			this.binary = binary;
			this.mask   = mask;
			this.fin    = fin;
		}
	}

	/**
	 * Sends a ping message to the remote party.
	 * @throws Exception 
	 *
	 * @api public
	 */
	public boolean ping(Object data, SendOptions options) throws Exception {
		boolean mask = options!=null && options.mask;
		return this.frameAndSend(0x9, data!=null ? data : "", true, mask, null);
	}

	/**
	 * Sends a pong message to the remote party.
	 * @throws Exception 
	 *
	 * @api public
	 */
	public boolean pong(Object data, SendOptions options) throws Exception {
		boolean mask = options!=null && options.mask;
		return this.frameAndSend(0xa, data!=null ? data : "", true, mask, null);
	}

	/**
	 * Sends text or binary data to the remote party.
	 * @throws Exception 
	 *
	 * @api public
	 */
	public boolean send(Object data, SendOptions options, WriteCB cb) throws Exception {
		boolean finalFragment = options!=null && options.fin == false ? false : true;
		boolean mask = options!=null && options.mask;
		int opcode = options!=null && options.binary == false ? 1 : 2;

		if (this.firstFragment == false) 
			opcode = 0;
		else 
			this.firstFragment = false;

		if (finalFragment) this.firstFragment = true;

		return this.frameAndSend(opcode, data, finalFragment, mask, cb);
	}


	/**
	 * Frames and sends a piece of data according to the HyBi WebSocket protocol.
	 * @throws Exception 
	 *
	 * @api private
	 */
	private boolean frameAndSend(int opcode, Object data, boolean finalFragment,
			boolean maskData, WriteCB cb) throws Exception {
		boolean canModifyData = false;
		boolean out = false;

		/*
		if (!data) {
			try {
				out = this._socket.write(new Buffer([opcode | (finalFragment ? 0x80 : 0), 0 | (maskData ? 0x80 : 0)].concat(maskData ? [0, 0, 0, 0] : [])), 'binary', cb);
			}
			catch (e) {
				if (typeof cb == 'function') cb(e);
				else this.emit('error', e);
			}
			return out;
		}*/
		if (data == null) {
			ByteBuffer tbw;

			if (maskData) {
				tbw = ByteBuffer.allocate(6);
				tbw.put((byte) (opcode | (finalFragment ? 0x80 : 0)));
				tbw.put((byte) (0 | (maskData ? 0x80 : 0)));
				tbw.putInt(0);
			} else {
				tbw = ByteBuffer.allocate(2);
				tbw.put((byte) (opcode | (finalFragment ? 0x80 : 0)));
				tbw.put((byte) (0 | (maskData ? 0x80 : 0)));
			}

			tbw.flip();
			try {
				out = this._socket.write(tbw, null, cb);
			} catch (Exception e) {
				if (cb != null) cb.writeDone(e.toString());
				else this.emit("error", e.toString());
			}
			
			return out;
		}
		
/*
		if (!Buffer.isBuffer(data)) {
			canModifyData = true;
			if (data && (typeof data.byteLength !== 'undefined' || typeof data.buffer !== 'undefined')) {
				data = getArrayBuffer(data);
			} else {
				data = new Buffer(data);
			}
		}*/
		if (!Util.isBuffer(data) && Util.isString(data)) {
			data = Util.chunkToBuffer(data, "utf8");
		} else {
			if (cb != null) cb.writeDone("Invalid data");
			else this.emit("error", "Invalid data");
			
			return out;
		}
		

		int dataLength = Util.chunkByteLength(data, null);
		int dataOffset = maskData ? 6 : 2;
		int secondByte = dataLength;

		if (dataLength >= 65536) {
			dataOffset += 8;
			secondByte = 127;
		}
		else if (dataLength > 125) {
			dataOffset += 2;
			secondByte = 126;
		}

		boolean mergeBuffers = dataLength < 32768 || (maskData && !canModifyData);
		int totalLength = mergeBuffers ? dataLength + dataOffset : dataOffset;
		///var outputBuffer = new Buffer(totalLength);
		ByteBuffer outputBuffer = ByteBuffer.allocate(totalLength);
		///outputBuffer[0] = finalFragment ? opcode | 0x80 : opcode;
		outputBuffer.put(0, (byte) (finalFragment ? opcode | 0x80 : opcode));

		switch (secondByte) {
		case 126:
			///writeUInt16BE.call(outputBuffer, dataLength, 2);
			outputBuffer.order(ByteOrder.BIG_ENDIAN).putShort(2, (short) dataLength);
			break;
		case 127:
			///writeUInt32BE.call(outputBuffer, 0, 2);
			///writeUInt32BE.call(outputBuffer, dataLength, 6);
			outputBuffer.order(ByteOrder.BIG_ENDIAN).putInt(2, 0);
			outputBuffer.order(ByteOrder.BIG_ENDIAN).putInt(6, dataLength);
			break;
		}

		if (maskData) {
			///outputBuffer[1] = secondByte | 0x80;
			outputBuffer.put(1, (byte) (secondByte | 0x80));
			
			byte[] mask = this._randomMask!=null ? this._randomMask : (this._randomMask = getRandomMask());
			/*outputBuffer[dataOffset - 4] = mask[0];
			outputBuffer[dataOffset - 3] = mask[1];
			outputBuffer[dataOffset - 2] = mask[2];
			outputBuffer[dataOffset - 1] = mask[3];*/
			outputBuffer.put(dataOffset - 4, mask[0]);
			outputBuffer.put(dataOffset - 3, mask[1]);
			outputBuffer.put(dataOffset - 2, mask[2]);
			outputBuffer.put(dataOffset - 1, mask[3]);

			if (mergeBuffers) {
				BufferUtil.mask((ByteBuffer) data, mask, outputBuffer, dataOffset, dataLength);
				try {
					out = this._socket.write(outputBuffer, null, cb);
				}
				catch (Exception e) {
					if (cb != null) cb.writeDone(e.toString());
					else this.emit("error", e.toString());
				}
			}
			else {
				BufferUtil.mask((ByteBuffer) data, mask, (ByteBuffer) data, 0, dataLength);
				try {
					///this._socket.write(outputBuffer, 'binary');
					///out = this._socket.write(data, 'binary', cb);
					this._socket.write(outputBuffer, null, null);
					out = this._socket.write(data, null, cb);
				}
				catch (Exception e) {
					if (cb != null) cb.writeDone(e.toString());
					else this.emit("error", e.toString());
				}
			}
		}
		else {
			///outputBuffer[1] = secondByte;
			outputBuffer.put(1, (byte) secondByte);
			if (mergeBuffers) {
				///data.copy(outputBuffer, dataOffset);
				outputBuffer.put((ByteBuffer) data);
				
				try {
					out = this._socket.write(outputBuffer, null, cb);
				}
				catch (Exception e) {
					if (cb != null) cb.writeDone(e.toString());
					else this.emit("error", e.toString());
				}
			}
			else {
				try {
					///this._socket.write(outputBuffer, 'binary');
					///out = this._socket.write(data, 'binary', cb);
					this._socket.write(outputBuffer, null, null);
					out = this._socket.write(data, null, cb);
				}
				catch (Exception e) {
					if (cb != null) cb.writeDone(e.toString());
					else this.emit("error", e.toString());
				}
			}
		}
		
		return out;
	}

	private static byte[] getRandomMask() {
		byte[] ret = new byte[4];

		ret[0] = (byte) Math.ceil(Math.random() * 255);
		ret[1] = (byte) Math.ceil(Math.random() * 255);
		ret[2] = (byte) Math.ceil(Math.random() * 255);
		ret[3] = (byte) Math.ceil(Math.random() * 255);

		return ret;
	}

}
