package com.iwebpp.Stream;

import java.nio.ByteBuffer;

public interface Writable {
	public static interface writeCB {
		void invoke(int error);
	};
    public boolean write(ByteBuffer chunk);
    public boolean write(ByteBuffer chunk, writeCB cb);

    public boolean write(String chunk);
    public boolean write(String chunk, writeCB cb);
    public boolean write(String chunk, String encoding);
    public boolean write(String chunk, String encoding, writeCB cb);
    
    public boolean end();

    public boolean end(ByteBuffer chunk);
    public boolean end(ByteBuffer chunk, writeCB cb);

    public boolean end(String chunk);
    public boolean end(String chunk, writeCB cb);
    public boolean end(String chunk, String encoding);
    public boolean end(String chunk, String encoding, writeCB cb);
}
