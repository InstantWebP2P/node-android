// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.stream;

import com.iwebpp.node.EventEmitter;


public interface Writable extends EventEmitter {
	public interface WriteCB {
		public void writeDone(final String error) throws Exception;
	}
	
	public boolean write(Object chunk, String encoding, WriteCB cb) throws Exception;
	public boolean write(Object chunk, String encoding) throws Exception;
	public boolean write(Object chunk) throws Exception;
	public boolean write() throws Exception;

	public boolean end(Object chunk, String encoding, WriteCB cb) throws Exception;
	public boolean end(Object chunk, String encoding) throws Exception;
	public boolean end(Object chunk) throws Exception;
	public boolean end() throws Exception;

	public boolean writable();
	public void    writable(boolean writable);
}
