package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Readable extends EventEmitter {
	public Object read(int size) throws Throwable;
	public boolean push(Object chunk, String encoding) throws Throwable;
	public boolean setEncoding(String encoding);
    public Readable pause();
    public Readable resume();
    public Writable pipe(Writable dest, boolean end);
    public Readable unpipe(Writable dest);
    public boolean unshift(Object chunk) throws Throwable;
    public boolean readable();
}
