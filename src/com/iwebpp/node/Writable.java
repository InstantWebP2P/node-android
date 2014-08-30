package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Writable extends EventEmitter {
	public interface WriteCB {
		void invoke(final String error) throws Throwable;
	}
    public boolean write(Object chunk, String encoding, WriteCB cb) throws Throwable;
    public void end(Object chunk, String encoding, WriteCB cb) throws Throwable;
    
	public boolean writable();
}
