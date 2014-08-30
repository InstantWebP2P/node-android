package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Readable extends EventEmitter {
	public Object read(int size) throws Throwable;
	public boolean push(Object chunk, String encoding) throws Throwable;
	public boolean setEncoding(String encoding);
    public Readable pause() throws Throwable;
    public Readable resume() throws Throwable;
    public Writable pipe(Writable dest, boolean end) throws Throwable;
    public Readable unpipe(Writable dest) throws Throwable;
    public boolean unshift(Object chunk) throws Throwable;
    public boolean readable();
}
