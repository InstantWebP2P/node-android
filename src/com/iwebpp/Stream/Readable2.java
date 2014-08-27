package com.iwebpp.Stream;

import java.nio.ByteBuffer;

import com.iwebpp.Event.eventEmitter2;

public abstract class Readable2 
extends eventEmitter2 
implements Readable {

	Readable2() {
		super();
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object read() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object read(int size) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean setEncoding(String encoding) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pause() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pipe(Writable ws) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unpipe(Writable ws) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unshift(ByteBuffer chunk) {
		// TODO Auto-generated method stub
		return false;
	}

	// _read(size)
	public abstract void _read();
	public abstract void _read(int size);
}
