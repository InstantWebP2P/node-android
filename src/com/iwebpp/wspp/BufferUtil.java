// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>

package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import android.util.Log;

import com.iwebpp.node.Util;

public final class BufferUtil {
	
  private static final String TAG = "BufferUtil";
  
  protected static ByteBuffer merge(/*ByteBuffer mergedBuffer,*/ List<Object> buffers) throws Exception {
	  ///buffers.add(0, mergedBuffer);
	  ByteBuffer mergedBuffer = Util.concatByteBuffer(buffers, 0);
	  
	  return mergedBuffer;
  }
  
  protected static ByteBuffer mask(ByteBuffer source, byte[] mask, ByteBuffer output,
		  int offset, int length) {
	  Log.d(TAG, "unmask, source:"+source+",mask:"+mask);

	  long mask0 = mask[0], mask1 = mask[1], mask2 = mask[2], mask3 = mask[3];
	  long maskNum = 
			  ((mask0 <<  0) &               0xffL) |
			  ((mask1 <<  8) &             0xff00L) |
			  ((mask2 << 16) &           0xff0000L) |
			  ((mask3 << 24) &         0xff000000L) |
			  ((mask0 << 32) &       0xff00000000L) |
			  ((mask1 << 40) &     0xff0000000000L) |
			  ((mask2 << 48) &   0xff000000000000L) |
			  ((mask3 << 56) & 0xff00000000000000L) ;
	  
	  int i = 0;
	  
	  source.order(ByteOrder.LITTLE_ENDIAN); output.order(ByteOrder.LITTLE_ENDIAN);
	  for (; i < length - 7; i += 8) {
		  long num = maskNum ^ source.getLong(i);
		  ///if (num < 0) num = 4294967296 + num;
		  output.putLong(offset + i, num);
	  }

	  switch (length % 8) {
	  case 7: output.put(offset + i + 6, (byte) (source.get(i + 6) ^ mask[2]));
	  case 6: output.put(offset + i + 5, (byte) (source.get(i + 5) ^ mask[1]));
	  case 5: output.put(offset + i + 4, (byte) (source.get(i + 4) ^ mask[0]));
	  case 4: output.put(offset + i + 3, (byte) (source.get(i + 3) ^ mask[3]));
	  case 3: output.put(offset + i + 2, (byte) (source.get(i + 2) ^ mask[2]));
	  case 2: output.put(offset + i + 1, (byte) (source.get(i + 1) ^ mask[1]));
	  case 1: output.put(offset + i + 0, (byte) (source.get(i + 0) ^ mask[0]));
	  case 0:;
	  }
	  
	  renewBuffer(output);
	  
	  return output;
  }
  
  protected static ByteBuffer unmask(ByteBuffer data, byte[] mask) {
	  Log.d(TAG, "unmask, data:"+data+",mask:"+mask);
	  
	  long mask0 = mask[0], mask1 = mask[1], mask2 = mask[2], mask3 = mask[3];
	  long maskNum = 
			  ((mask0 <<  0) &               0xffL) |
			  ((mask1 <<  8) &             0xff00L) |
			  ((mask2 << 16) &           0xff0000L) |
			  ((mask3 << 24) &         0xff000000L) |
			  ((mask0 << 32) &       0xff00000000L) |
			  ((mask1 << 40) &     0xff0000000000L) |
			  ((mask2 << 48) &   0xff000000000000L) |
			  ((mask3 << 56) & 0xff00000000000000L) ;
	  
	  int length = data.capacity();
	  int i = 0;
	  
	  data.order(ByteOrder.LITTLE_ENDIAN);
	  for (; i < length - 7; i += 8) {
		  long num = maskNum ^ data.getLong(i);
		  ///if (num < 0) num = 4294967296 + num;
		  data.putLong(i, num);
	  }
	  switch (length % 8) {
	  case 7: data.put(i + 6, (byte) (data.get(i + 6) ^ mask[2]));
	  case 6: data.put(i + 5, (byte) (data.get(i + 5) ^ mask[1]));
	  case 5: data.put(i + 4, (byte) (data.get(i + 4) ^ mask[0]));
	  case 4: data.put(i + 3, (byte) (data.get(i + 3) ^ mask[3]));
	  case 3: data.put(i + 2, (byte) (data.get(i + 2) ^ mask[2]));
	  case 2: data.put(i + 1, (byte) (data.get(i + 1) ^ mask[1]));
	  case 1: data.put(i + 0, (byte) (data.get(i + 0) ^ mask[0]));
	  case 0:;
	  }

	  renewBuffer(data);
	  
	  return data;
  }

  protected static void fastCopy(int length, ByteBuffer srcBuffer, ByteBuffer dstBuffer, int dstOffset) {
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
	  srcBuffer.position(        0); srcBuffer.limit(              length);
	  
	  dstBuffer.put(srcBuffer);
	  /*
	  switch (length) {
	  default: 
		  for (int i = 0; i < length; i ++)
          dstBuffer.put(srcBuffer.get());
		  break;
		  
	  case 16: dstBuffer.put(srcBuffer.get());
	  case 15: dstBuffer.put(srcBuffer.get());
	  case 14: dstBuffer.put(srcBuffer.get());
	  case 13: dstBuffer.put(srcBuffer.get());
	  case 12: dstBuffer.put(srcBuffer.get());
	  case 11: dstBuffer.put(srcBuffer.get());
	  case 10: dstBuffer.put(srcBuffer.get());
	  case 9: dstBuffer.put(srcBuffer.get());
	  case 8: dstBuffer.put(srcBuffer.get());
      case 7: dstBuffer.put(srcBuffer.get());
	  case 6: dstBuffer.put(srcBuffer.get());
	  case 5: dstBuffer.put(srcBuffer.get());
	  case 4: dstBuffer.put(srcBuffer.get());
	  case 3: dstBuffer.put(srcBuffer.get());
	  case 2: dstBuffer.put(srcBuffer.get());
	  case 1: dstBuffer.put(srcBuffer.get());
	  }*/
	  
	  dstBuffer.position(0); dstBuffer.limit(dstBuffer.capacity());
	  srcBuffer.position(0); srcBuffer.limit(srcBuffer.capacity());
	  
	  Log.d(TAG, "fastCopy, srcBuffer:"+srcBuffer+",dstBuffer:"+dstBuffer);
  }

  protected static ByteBuffer renewBuffer(ByteBuffer buf) {
	  buf.position(0); buf.limit(buf.capacity());
	  
	  return buf;
  }
  
}
