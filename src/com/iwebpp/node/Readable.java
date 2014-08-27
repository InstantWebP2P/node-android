package com.iwebpp.node;

import java.nio.ByteBuffer;

public interface Readable {
	public Object read(int size);
	public boolean push(Object chunk, String encoding);
	public boolean setEncoding(String encoding);
    public boolean pause();
    public boolean resume();
    public boolean unpipe(Writable ws);
    public boolean unshift(ByteBuffer chunk);
    public boolean readable();
}
