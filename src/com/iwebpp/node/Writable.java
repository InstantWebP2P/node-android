package com.iwebpp.node;


public interface Writable extends EventEmitter {
	public interface WriteCB {
		public void writeDone(final String error) throws Exception;
	}
	public boolean write(Object chunk, String encoding, WriteCB cb) throws Exception;
	public boolean end(Object chunk, String encoding, WriteCB cb) throws Exception;
	public boolean writable();
}
