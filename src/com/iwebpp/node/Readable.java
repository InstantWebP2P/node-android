package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Readable extends EventEmitter {
	public Object read(int size);
	public boolean push(Object chunk, String encoding);
	public boolean setEncoding(String encoding);
    public boolean pause();
    public boolean resume();
    public Writable pipe(Writable dest);
    public boolean unpipe(Writable dest);
    public boolean unshift(ByteBuffer chunk);
    public boolean readable();
}
