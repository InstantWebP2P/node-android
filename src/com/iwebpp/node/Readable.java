package com.iwebpp.node;


public interface Readable extends EventEmitter {
	Object read(int size) throws Throwable;
	boolean setEncoding(String encoding);
    Readable pause() throws Throwable;
    Readable resume() throws Throwable;
    Writable pipe(Writable dest, boolean end) throws Throwable;
    Readable unpipe(Writable dest) throws Throwable;
    boolean unshift(Object chunk) throws Throwable;
	boolean readable();
}
