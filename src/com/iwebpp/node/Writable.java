package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Writable {
	public interface writeCB {
		void invoke(final String error);
	}
    public boolean write(Object chunk, String encoding, writeCB cb);
    public void end(Object chunk, String encoding, writeCB cb);
    
	public boolean writable();
}
