package com.iwebpp.node;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;

public final class Util {
	// Buffer
    public static boolean isBuffer(Object chunk) {
    	return chunk instanceof Buffer;
    } 
    
    public static boolean isString(Object chunk) {
    	return chunk instanceof String;
    } 
    
    public static boolean isNullOrUndefined(Object chunk) {
    	return chunk == null;
    }
    
    public static int chunkLength(Object chunk) {
    	if (isBuffer(chunk)) {
    		ByteBuffer bb = (ByteBuffer)chunk;
    		return bb.capacity();
    	}
    	
    	if (isString(chunk)) {
    		String s = (String)chunk;
    		return s.length();
    	}
    	
		return 0;
    } 
    
    public static Object chunkSlice(Object chunk, int start, int end) {
    	if (isBuffer(chunk)) {
    		ByteBuffer bb = (ByteBuffer)chunk;
    		int opos = bb.position(); bb.position(start);
    		int olmt = bb.limit(); bb.limit(end);
    		
			ByteBuffer rb = bb.slice();
			bb.limit(olmt); bb.position(opos);
			
			return rb;
    	}
    	
    	if (isString(chunk)) {
    		String ss = (String)chunk;
    		
    		return ss.substring(start, end);
    	}
    	
    	return null;
    }
    
    public static Object chunkSlice(Object chunk, int start) {
    	if (isBuffer(chunk)) {
    		ByteBuffer bb = (ByteBuffer)chunk;
    		int opos = bb.position(); bb.position(start);
    		
    		ByteBuffer rb = bb.slice();
			bb.position(opos);
			
			return rb;
    	}
    	
    	if (isString(chunk)) {
    		String ss = (String)chunk;
    		
    		return ss.substring(start, ss.length());
    	}
    	
    	return null;
    }
    
    public static boolean zeroString(String s) {
    	if (s == null) 
    		return true;
    	else 
    		return s == "";
    }

    public static ByteBuffer concatByteBuffer(List<Object> list, int length) {
    	if (length <= 0) {
    		length = 0;
    		
    		for (Object b : list) length += ((Buffer) b).position();
    	}
    		
    	if (length > 0) {
    		ByteBuffer bb = ByteBuffer.allocate(length);

    		for (Object b : list) bb.put((ByteBuffer)b);

    		return bb;
    	} else 
    		return null;
    }
}
