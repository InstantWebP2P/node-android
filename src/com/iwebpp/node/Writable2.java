package com.iwebpp.node;

import java.nio.ByteBuffer;


public abstract class Writable2 
extends EventEmitter2 
implements Writable {

	@Override
	public boolean write(Object chunk, String encoding, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void end(Object chunk, String encoding, writeCB cb) {
		// TODO Auto-generated method stub
		return;
	}
	
	// _write(chunk, encoding, callback)
	public abstract boolean _write(Object chunk, String encoding, writeCB cb);

}
