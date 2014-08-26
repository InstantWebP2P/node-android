package com.iwebpp.Stream;

import java.nio.ByteBuffer;

public interface Readable {
	public int read();
	public int read(int size);
	public boolean setEncoding(String encoding);
    public boolean pause();
    public boolean resume();
    public boolean pipe(Writable ws);
    public boolean unpipe(Writable ws);
    public boolean unshift(ByteBuffer chunk);
}
