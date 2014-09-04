package com.iwebpp.node;


public interface Writable extends EventEmitter {
	interface WriteCB {
		void invoke(final String error) throws Exception;
	}
	boolean write(Object chunk, String encoding, WriteCB cb) throws Exception;
	void end(Object chunk, String encoding, WriteCB cb) throws Exception;
	boolean writable();
}
