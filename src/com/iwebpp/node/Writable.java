package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Writable extends EventEmitter {
	public interface WriteCB {
		void invoke(final String error);
	}
    public boolean write(Object chunk, String encoding, WriteCB cb);
    public void end(Object chunk, String encoding, WriteCB cb);
    
	public boolean writable();
}
