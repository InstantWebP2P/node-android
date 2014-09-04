package com.iwebpp.node;


public interface Readable extends EventEmitter {
	Object read(int size) throws Exception;
	boolean setEncoding(String encoding);
    Readable pause() throws Exception;
    Readable resume() throws Exception;
    Writable pipe(Writable dest, boolean end) throws Exception;
    Readable unpipe(Writable dest) throws Exception;
    boolean unshift(Object chunk) throws Exception;
	boolean readable();
}
