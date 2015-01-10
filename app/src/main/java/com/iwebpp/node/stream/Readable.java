// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.stream;

import com.iwebpp.node.EventEmitter;


public interface Readable extends EventEmitter {
	public Object   read(int size) throws Exception;
	
	public boolean  setEncoding(String encoding);
	
	public Readable pause() throws Exception;
	public Readable resume() throws Exception;
	
	public Writable pipe(Writable dest, boolean end) throws Exception;
	public Readable unpipe(Writable dest) throws Exception;
	
	public boolean  unshift(Object chunk) throws Exception;
	
	public boolean  readable();
	public void     readable(boolean readable);
}
