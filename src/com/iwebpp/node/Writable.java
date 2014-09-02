package com.iwebpp.node;


public interface Writable extends EventEmitter {
	interface WriteCB {
		void invoke(final String error) throws Throwable;
	}
	boolean write(Object chunk, String encoding, WriteCB cb) throws Throwable;
	void end(Object chunk, String encoding, WriteCB cb) throws Throwable;
	boolean writable();
}
