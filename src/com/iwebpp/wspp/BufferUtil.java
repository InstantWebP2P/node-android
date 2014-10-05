package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import com.iwebpp.node.Util;

public final class BufferUtil {
  
  public static ByteBuffer merge(/*ByteBuffer mergedBuffer,*/ List<Object> buffers) {
	  ///buffers.add(0, mergedBuffer);
	  ByteBuffer mergedBuffer = Util.concatByteBuffer(buffers, 0);
	  
	  return mergedBuffer;
  }
  
  public static ByteBuffer mask(ByteBuffer source, byte[] mask, ByteBuffer output,
		  int offset, int length) {
	  int maskNum = 
			  ((mask[0] <<  0) &       0xff) |
			  ((mask[1] <<  8) &     0xff00) |
			  ((mask[2] << 16) &   0xff0000) |
			  ((mask[3] << 24) & 0xff000000);

	  int i = 0;
	  source.order(ByteOrder.LITTLE_ENDIAN); output.order(ByteOrder.LITTLE_ENDIAN);
	  for (; i < length - 3; i += 4) {
		  int num = maskNum ^ source.getInt(i);
		  ///if (num < 0) num = 4294967296 + num;
		  output.putInt(offset + i, num);
	  }

	  switch (length % 4) {
	  case 3: output.put(offset + i + 2, (byte) (source.get(i + 2) ^ mask[2]));
	  case 2: output.put(offset + i + 1, (byte) (source.get(i + 1) ^ mask[1]));
	  case 1: output.put(offset + i + 0, (byte) (source.get(i + 0) ^ mask[0]));
	  case 0:;
	  }
	  
	  output.flip();
	  
	  return output;
  }
  
  public static ByteBuffer unmask(ByteBuffer data, byte[] mask) {
	  int maskNum = 
			  ((mask[0] <<  0) &       0xff) |
			  ((mask[1] <<  8) &     0xff00) |
			  ((mask[2] << 16) &   0xff0000) |
			  ((mask[3] << 24) & 0xff000000);

	  int length = data.capacity();
	  int i = 0;

	  data.order(ByteOrder.LITTLE_ENDIAN);
	  for (; i < length - 3; i += 4) {
		  int num = maskNum ^ data.getInt(i);
		  ///if (num < 0) num = 4294967296 + num;
		  data.putInt(i, num);
	  }
	  switch (length % 4) {
	  case 3: data.putInt(i + 2, data.getInt(i + 2) ^ mask[2]);
	  case 2: data.putInt(i + 1, data.getInt(i + 1) ^ mask[1]);
	  case 1: data.putInt(i + 0, data.getInt(i + 0) ^ mask[0]);
	  case 0:;
	  }

	  data.flip();

	  return data;
  }
  
}
