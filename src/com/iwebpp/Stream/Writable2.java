package com.iwebpp.Stream;

import java.nio.ByteBuffer;

public abstract class Writable2 implements Writable {

	@Override
	public boolean write(ByteBuffer chunk) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean write(ByteBuffer chunk, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean write(String chunk) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean write(String chunk, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean write(String chunk, String encoding) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean write(String chunk, String encoding, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end(ByteBuffer chunk) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end(ByteBuffer chunk, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end(String chunk) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end(String chunk, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end(String chunk, String encoding) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean end(String chunk, String encoding, writeCB cb) {
		// TODO Auto-generated method stub
		return false;
	}
	
	// _write(chunk, encoding, callback)
	public abstract boolean _write(ByteBuffer chunk, writeCB cb);
	public abstract boolean _write(String chunk, String encoding, writeCB cb);

}
