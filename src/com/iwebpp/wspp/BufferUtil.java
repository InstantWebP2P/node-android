// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>

package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import android.util.Log;

import com.iwebpp.node.Util;

public final class BufferUtil {
	
  private static final String TAG = "BufferUtil";
  
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

  public static void fastCopy(int length, ByteBuffer srcBuffer, ByteBuffer dstBuffer, int dstOffset) {
	  /*switch (length) {
    default: srcBuffer.copy(dstBuffer, dstOffset, 0, length); break;
    case 16: dstBuffer[dstOffset+15] = srcBuffer[15];
    case 15: dstBuffer[dstOffset+14] = srcBuffer[14];
    case 14: dstBuffer[dstOffset+13] = srcBuffer[13];
    case 13: dstBuffer[dstOffset+12] = srcBuffer[12];
    case 12: dstBuffer[dstOffset+11] = srcBuffer[11];
    case 11: dstBuffer[dstOffset+10] = srcBuffer[10];
    case 10: dstBuffer[dstOffset+9] = srcBuffer[9];
    case 9: dstBuffer[dstOffset+8] = srcBuffer[8];
    case 8: dstBuffer[dstOffset+7] = srcBuffer[7];
    case 7: dstBuffer[dstOffset+6] = srcBuffer[6];
    case 6: dstBuffer[dstOffset+5] = srcBuffer[5];
    case 5: dstBuffer[dstOffset+4] = srcBuffer[4];
    case 4: dstBuffer[dstOffset+3] = srcBuffer[3];
    case 3: dstBuffer[dstOffset+2] = srcBuffer[2];
    case 2: dstBuffer[dstOffset+1] = srcBuffer[1];
    case 1: dstBuffer[dstOffset] = srcBuffer[0];
  }*/
	  Log.d(TAG, "fastCopy, srcBuffer:"+srcBuffer+",dstBuffer:"+dstBuffer+", length:"+length+",dstOffset:"+dstOffset);

	  if (length == 0)
		  return;

	  dstBuffer.position(dstOffset); dstBuffer.limit(dstBuffer.capacity());
	  srcBuffer.position(        0); srcBuffer.limit(srcBuffer.capacity());

	  ///Log.d(TAG, "fastCopy, srcBuffer.array: "+srcBuffer.array()+", srcBuffer.arrayOffset: "+srcBuffer.arrayOffset());

	  ///dstBuffer.put(srcBuffer.array(), srcBuffer.arrayOffset(), length);
	  for (int i = 0; i < length; i++)
		  dstBuffer.put(srcBuffer.get(i));

	  dstBuffer.position(0); dstBuffer.limit(dstBuffer.capacity());
	  srcBuffer.position(0); srcBuffer.limit(srcBuffer.capacity());
	  
	  Log.d(TAG, "fastCopy, srcBuffer:"+srcBuffer+",dstBuffer:"+dstBuffer);
  }

  public static ByteBuffer renewBuffer(ByteBuffer buf) {
	  buf.position(0); buf.limit(buf.capacity());
	  
	  return buf;
  }
  
}
