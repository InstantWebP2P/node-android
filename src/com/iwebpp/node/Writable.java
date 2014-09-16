package com.iwebpp.node;


public interface Writable extends EventEmitter {
	interface WriteCB {
		void writeDone(final String error) throws Exception;
	}
	boolean write(Object chunk, String encoding, WriteCB cb) throws Exception;
	boolean end(Object chunk, String encoding, WriteCB cb) throws Exception;
	boolean writable();
}
