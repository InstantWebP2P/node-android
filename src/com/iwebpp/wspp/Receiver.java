// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>

package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedList;
import java.util.List;

import android.util.Log;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;

public abstract class Receiver {
	
	private static final String TAG = "Receiver";
	
	private class State {

	    public State(int activeFragmentedOperation, boolean lastFragment,
				boolean masked, int opcode, boolean fragmentedOperation) {
			super();
			this.activeFragmentedOperation = activeFragmentedOperation;
			this.lastFragment = lastFragment;
			this.masked = masked;
			this.opcode = opcode;
			this.fragmentedOperation = fragmentedOperation;
		}
	    
		private int activeFragmentedOperation = -1;
	    private boolean lastFragment = false;
	    private boolean masked = false;
	    private int opcode = 0;
	    private boolean fragmentedOperation = false;
	  
	}

	private ByteBuffer expectBuffer;
	private BufferPool fragmentedBufferPool;
	private BufferPool unfragmentedBufferPool;
	private State state;
	private List<ByteBuffer> overflow;
	private ByteBuffer headerBuffer;
	private int expectOffset;
	private PacketHandler expectHandler;
	private LinkedList<Object> currentMessage;
	private boolean dead; 
	private final OpcHandler _handler_text;
	private final OpcHandler _handler_binary;
	private final OpcHandler _handler_close;
	private final OpcHandler _handler_ping;
	private final OpcHandler _handler_pong;
	private CharsetDecoder _utf8_decoder;
	private NodeContext context;


	public static class opcOptions {
		public boolean masked;
		public ByteBuffer buffer;
		public boolean binary;
		
		public opcOptions(boolean masked, ByteBuffer buffer, boolean binary) {
			this.masked = masked;
			this.buffer = buffer;
			this.binary = binary;
		}
	}
	
	protected Receiver() throws Exception {		
        final Receiver self = this;
        
        this._utf8_decoder = Charset.forName("utf8").newDecoder();
        
		  // memory pool for fragmented messages
		  /*int fragmentedPoolPrevUsed = -1;
		  this.fragmentedBufferPool = new BufferPool(1024, function(db, length) {
		    return db.used + length;
		  }, function(db) {
		    return fragmentedPoolPrevUsed = fragmentedPoolPrevUsed >= 0 ?
		      (fragmentedPoolPrevUsed + db.used) / 2 :
		      db.used;
		  });*/
		///int fragmentedPoolPrevUsed = -1;
		
		this.fragmentedBufferPool = new BufferPool(1024, new BufferPool.Strategy() {

			@Override
			public int _shrinkStrategy(BufferPool db) {
				return db.PoolPrevUsed = db.PoolPrevUsed >= 0 ?
						(db.PoolPrevUsed + db.used()) / 2 :
							db.used();
			}

			@Override
			public int _growStrategy(BufferPool db, int length) {
				return db.used() + length;
			}

		});
				
		  // memory pool for unfragmented messages
		  /*var unfragmentedPoolPrevUsed = -1;
		  this.unfragmentedBufferPool = new BufferPool(1024, function(db, length) {
		    return db.used + length;
		  }, function(db) {
		    return unfragmentedPoolPrevUsed = unfragmentedPoolPrevUsed >= 0 ?
		      (unfragmentedPoolPrevUsed + db.used) / 2 :
		      db.used;
		  });*/
		  ///int unfragmentedPoolPrevUsed = -1;
		  this.unfragmentedBufferPool = new BufferPool(1024, new BufferPool.Strategy() {

			  @Override
			  public int _shrinkStrategy(BufferPool db) {
				  return db.PoolPrevUsed = db.PoolPrevUsed >= 0 ?
						  (db.PoolPrevUsed + db.used()) / 2 :
							  db.used();
			  }

			  @Override
			  public int _growStrategy(BufferPool db, int length) {
				  return db.used() + length;
			  }

		  });
		  
		  /*
		  this.state = {
		    activeFragmentedOperation: null,
		    lastFragment: false,
		    masked: false,
		    opcode: 0,
		    fragmentedOperation: false
		  };*/
		  this.state = new State(-1, false, false, 0, false);
		  
		  this.overflow = new LinkedList<ByteBuffer>();///[];
		  this.headerBuffer = ByteBuffer.allocate(10);///new Buffer(10);
		  this.expectOffset = 0;
		  this.expectBuffer = null;
		  this.expectHandler = null;
		  this.currentMessage = new LinkedList<Object>();///[];
		  this.expectHeader(2, new processPacket());
		  this.dead = false;

		  /*this.onerror = function() {};
		  this.ontext = function() {};
		  this.onbinary = function() {};
		  this.onclose = function() {};
		  this.onping = function() {};
		  this.onpong = function() {};*/

		  // Opc handlers: text, binary, close, ping, pong
		  this._handler_text = new OpcHandler(){

			  @Override
			  public void start(ByteBuffer data) throws Exception {
				  if (data != null) {
					  String dstr = "";
					  for (int i = 0; i < data.capacity(); i ++)
						  dstr += " "+data.get(i);
					  Log.d(TAG, dstr);
				  }
				  
				  ///var self = this;
				  // decode length
				  int firstLength = data.get(1) & 0x7f;
				  if (firstLength < 126) {
					  ///opcodes['1'].getData.call(self, firstLength);
					  getData(firstLength);
				  }
				  else if (firstLength == 126) {
					  /*self.expectHeader(2, function(data) {
						  opcodes['1'].getData.call(self, readUInt16BE.call(data, 0));
					  });*/
					  self.expectHeader(2, new PacketHandler(){
							public void onPacket(ByteBuffer data) throws Exception {
								  getData(data.order(ByteOrder.BIG_ENDIAN).getShort(0) & 0xffff);
							}
					  });
				  }
				  else if (firstLength == 127) {
					  /*
					  self.expectHeader(8, function(data) {
						  if (readUInt32BE.call(data, 0) != 0) {
							  self.error('packets with length spanning more than 32 bit is currently not supported', 1008);
							  return;
						  }
						  opcodes['1'].getData.call(self, readUInt32BE.call(data, 4));
					  });*/
					  
					  self.expectHeader(8, new PacketHandler(){

						  @Override
						  public void onPacket(ByteBuffer data) throws Exception {
							  data.order(ByteOrder.BIG_ENDIAN);

							  if (data.getInt(0) != 0) {
								  self.error("packets with length spanning more than 32 bit is currently not supported", 1008);
								  return;
							  }
							  getData(data.getInt(4));
						  }

					  });
				  }
			  }

			  @Override
			  public void getData(final int length) throws Exception {
			      ///var self = this;
			      if (self.state.masked) {
			        /*self.expectHeader(4, function(data) {
			          var mask = data;
			          self.expectData(length, function(data) {
			            opcodes['1'].finish.call(self, mask, data);
			          });
			        });*/
			    	  self.expectHeader(4, new PacketHandler(){

						@Override
						public void onPacket(ByteBuffer data) throws Exception {
                            final byte[] mask = new byte[4]; ///data.array();
                            mask[0] = data.get(0);
                            mask[1] = data.get(1);
                            mask[2] = data.get(2);
                            mask[3] = data.get(3);
                                                        
                            Log.d(TAG, "mask: "+mask[0]+" "+mask[1]+" "+mask[2]+" "+mask[3]);

                            self.expectData(length, new PacketHandler(){

								@Override
								public void onPacket(ByteBuffer data2) throws Exception {
                                    finish(mask, data2);									
								}
                            	
                            });
						}
			    		  
			    	  });
			      }
			      else {
			        /*self.expectData(length, function(data) {
			          opcodes['1'].finish.call(self, null, data);
			        });*/
                      self.expectData(length, new PacketHandler(){

						@Override
						public void onPacket(ByteBuffer data) throws Exception {
                            finish(null, data);																
						}
                    	  
                      });
			      }
			    }

			  @Override
			  public void finish(byte[] mask, ByteBuffer data) throws Exception {
			      Log.d(TAG, "ontext, mask:"+mask+",data:"+data);

			      Object packet = self.unmask(mask, data, true);
			      
			      Log.d(TAG, "ontext, mask:"+mask+",packet:"+packet);
			      
			      if (packet != null) self.currentMessage.add(packet);
			      if (self.state.lastFragment) {
			        ByteBuffer messageBuffer = self.concatBuffers(self.currentMessage);
			        if (!Validation.isValidUTF8(messageBuffer)) {
			          error("invalid utf8 sequence", 1007);
			          return;
			        }
			        /// TBD...
			        ///self.ontext(messageBuffer.toString("utf8"), {masked: this.state.masked, buffer: messageBuffer});
			        self.ontext(self._utf8_decoder.decode(
			        		messageBuffer).toString(), 
			        		new opcOptions(self.state.masked, messageBuffer, false));
			        
			        ///self.currentMessage = [];
			        self.currentMessage.clear();
			      }
			      self.endPacket();
			    }

		  };
		  
		  this._handler_binary = new OpcHandler(){

			  @Override
			  public void start(ByteBuffer data) throws Exception {
				  if (data != null) {
					  String dstr = "";
					  for (int i = 0; i < data.capacity(); i ++)
						  dstr += " "+data.get(i);
					  Log.d(TAG, dstr);
				  }
					
			      ///var self = this;
			      // decode length
			      int firstLength = data.get(1) & 0x7f;
			      if (firstLength < 126) {
			        ///opcodes['2'].getData.call(self, firstLength);
			    	  getData(firstLength);
			      }
			      else if (firstLength == 126) {
			    	  /*self.expectHeader(2, function(data) {
			          opcodes['2'].getData.call(self, readUInt16BE.call(data, 0));
			        });*/
			    	  self.expectHeader(2, new PacketHandler(){

			    		  @Override
			    		  public void onPacket(ByteBuffer data) throws Exception {
					    	  getData(data.order(ByteOrder.BIG_ENDIAN).getShort(0) & 0xffff);
			    		  }

			    	  });
			      }
			      else if (firstLength == 127) {
			        /*self.expectHeader(8, function(data) {
			          if (readUInt32BE.call(data, 0) != 0) {
			            self.error('packets with length spanning more than 32 bit is currently not supported', 1008);
			            return;
			          }
			          opcodes['2'].getData.call(self, readUInt32BE.call(data, 4, true));
			        });*/
			    	  self.expectHeader(8, new PacketHandler(){

						@Override
						public void onPacket(ByteBuffer data) throws Exception {
							data.order(ByteOrder.BIG_ENDIAN);
							
							if (data.getInt(0) != 0) {
								self.error("packets with length spanning more than 32 bit is currently not supported", 1008);
								return;
							}
							
							getData(data.getInt(4));
						}
			    		  
			    	  });
			      }
			    }

			  @Override
			  public void getData(final int length) throws Exception {
				  ///var self = this;
				  if (self.state.masked) {
					  /*self.expectHeader(4, function(data) {
			          var mask = data;
			          self.expectData(length, function(data) {
			            opcodes['2'].finish.call(self, mask, data);
			          });
			        });*/
					  self.expectHeader(4, new PacketHandler(){

						  @Override
						  public void onPacket(ByteBuffer data) throws Exception {
							  final byte[] mask = new byte[4]; ///data.array();
							  mask[0] = data.get(0);
							  mask[1] = data.get(1);
							  mask[2] = data.get(2);
							  mask[3] = data.get(3);

							  self.expectData(length, new PacketHandler(){

								  @Override
								  public void onPacket(ByteBuffer data2)
										  throws Exception {
									  finish(mask, data2);									
								  }

							  });
						  }

					  });
				  }
				  else {
					  /*
			        self.expectData(length, function(data) {
			          opcodes['2'].finish.call(self, null, data);
			        });*/
					  self.expectData(length, new PacketHandler(){

						  @Override
						  public void onPacket(ByteBuffer data) throws Exception {
							  finish(null, data);							
						  }

					  });
				  }
			  }

			  @Override
			  public void finish(byte[] mask, ByteBuffer data) throws Exception {
			      Object packet = self.unmask(mask, data, true);
			      Log.d(TAG, "onbinary, mask:"+mask+",packet:"+packet);

			      if (packet != null) self.currentMessage.add(packet);
			      if (self.state.lastFragment) {
			        ByteBuffer messageBuffer = self.concatBuffers(self.currentMessage);
			        self.onbinary(messageBuffer, new opcOptions(self.state.masked, messageBuffer, true));
			        self.currentMessage.clear();/// = [];
			      }
			      self.endPacket();
			    }

		  };
		  
		  this._handler_close = new OpcHandler(){

			  @Override
			  public void start(ByteBuffer data) throws Exception {
				  if (data != null) {
					  String dstr = "";
					  for (int i = 0; i < data.capacity(); i ++)
						  dstr += " "+data.get(i);
					  Log.d(TAG, dstr);
				  }
					
				  ///var self = this;
				  if (self.state.lastFragment == false) {
					  self.error("fragmented close is not supported", 1002);
					  return;
				  }

				  // decode length
				  int firstLength = data.get(1) & 0x7f;
				  if (firstLength < 126) {
					  ///opcodes['8'].getData.call(self, firstLength);
					  getData(firstLength);
				  }
				  else {
					  self.error("control frames cannot have more than 125 bytes of data", 1002);
				  }
			  }

			  @Override
			  public void getData(final int length) throws Exception {
				  ///var self = this;
				  if (self.state.masked) {
					  /*
			        self.expectHeader(4, function(data) {
			          var mask = data;
			          self.expectData(length, function(data) {
			            opcodes['8'].finish.call(self, mask, data);
			          });
			        });*/
					  self.expectHeader(4, new PacketHandler(){

						  @Override
						  public void onPacket(ByteBuffer data) throws Exception {
							  final byte[] mask = new byte[4]; ///data.array();
							  mask[0] = data.get(0);
							  mask[1] = data.get(1);
							  mask[2] = data.get(2);
							  mask[3] = data.get(3);

							  self.expectData(length, new PacketHandler(){

								  @Override
								  public void onPacket(ByteBuffer data2)
										  throws Exception {
									  finish(mask, data2);									
								  }

							  });
						  }

					  });
				  }
				  else {
					  /*self.expectData(length, function(data) {
						  opcodes['8'].finish.call(self, null, data);
					  });*/
					  self.expectData(length, new PacketHandler(){

						@Override
						public void onPacket(ByteBuffer data) throws Exception {
                            finish(null, data);							
						}
						  
					  });
				  }
			  }

			  @Override
			  public void finish(byte[] mask, ByteBuffer data) throws Exception {
			      ///var self = this;
			      data = (ByteBuffer) self.unmask(mask, data, true);
			      Log.d(TAG, "onclose, mask:"+mask+",data:"+data);

			      if (data!=null && data.capacity() == 1) {
			        self.error("close packets with data must be at least two bytes long", 1002);
			        return;
			      }
			      int code = data!=null && data.capacity() > 1 ? data.order(ByteOrder.BIG_ENDIAN).getShort(0) & 0xffff : 1000 ; ///readUInt16BE.call(data, 0) : 1000;
			      if (!ErrorCodes.isValidErrorCode(code)) {
			        self.error("invalid error code", 1002);
			        return;
			      }
			      String message = "";
			      if (data!=null && data.capacity() > 2) {
			        ByteBuffer messageBuffer = (ByteBuffer) Util.chunkSlice(data, 2, data.capacity());///data.slice(2);
			        if (!Validation.isValidUTF8(messageBuffer)) {
			          self.error("invalid utf8 sequence", 1007);
			          return;
			        }
			        ///message = messageBuffer.toString('utf8');
			        message = self._utf8_decoder.decode(messageBuffer).toString();
			      }
			      self.onclose(code, message, new opcOptions(self.state.masked, null, false));//{masked: self.state.masked});
			      self.reset();
			    }

		  };
		  
		  this._handler_ping = new OpcHandler(){

			  @Override
			  public void start(ByteBuffer data) throws Exception {
				  if (data != null) {
					  String dstr = "";
					  for (int i = 0; i < data.capacity(); i ++)
						  dstr += " "+data.get(i);
					  Log.d(TAG, dstr);
				  }
					
				  ///var self = this;
				  if (self.state.lastFragment == false) {
					  self.error("fragmented ping is not supported", 1002);
					  return;
				  }

				  // decode length
				  int firstLength = data.get(1) & 0x7f;
				  if (firstLength < 126) {
					  ///opcodes['9'].getData.call(self, firstLength);
					  getData(firstLength);
				  }
				  else {
					  self.error("control frames cannot have more than 125 bytes of data", 1002);
				  }
			  }

			  @Override
			  public void getData(final int length) throws Exception {
			      ///var self = this;
			      if (self.state.masked) {
			    	  /*
			        self.expectHeader(4, function(data) {
			          var mask = data;
			          self.expectData(length, function(data) {
			            opcodes['9'].finish.call(self, mask, data);
			          });
			        });*/
			    	  self.expectHeader(4, new PacketHandler(){

			    		  @Override
			    		  public void onPacket(ByteBuffer data) throws Exception {
			    			  final byte[] mask = new byte[4]; ///data.array();
			    			  mask[0] = data.get(0);
			    			  mask[1] = data.get(1);
			    			  mask[2] = data.get(2);
			    			  mask[3] = data.get(3);

			    			  self.expectData(length, new PacketHandler(){

			    				  @Override
			    				  public void onPacket(ByteBuffer data2)
			    						  throws Exception {
			    					  finish(mask, data2);									
			    				  }

			    			  });
			    		  }

			    	  });
			      }
			      else {
			    	  /*
			        self.expectData(length, function(data) {
			          opcodes['9'].finish.call(self, null, data);
			        });*/
			    	  self.expectData(length, new PacketHandler(){

			    		  @Override
			    		  public void onPacket(ByteBuffer data) throws Exception {
			    			  finish(null, data);							
			    		  }

			    	  });
			      }
			    }

			  @Override
			  public void finish(byte[] mask, ByteBuffer data) throws Exception {
			      self.onping((ByteBuffer) self.unmask(mask, data, true), new opcOptions(self.state.masked, null, true));///{masked: this.state.masked, binary: true});
			      self.endPacket();
			    }

		  };
		  
		  this._handler_pong = new OpcHandler(){

			  @Override
			  public void start(ByteBuffer data) throws Exception {
			      ///var self = this;
			      if (self.state.lastFragment == false) {
			        self.error("fragmented pong is not supported", 1002);
			        return;
			      }

			      // decode length
			      int firstLength = data.get(1) & 0x7f;
			      if (firstLength < 126) {
			        ///opcodes['10'].getData.call(self, firstLength);
			    	  getData(firstLength);
			      }
			      else {
			        self.error("control frames cannot have more than 125 bytes of data", 1002);
			      }
			    }

			  @Override
			  public void getData(final int length) throws Exception {
			      ///var self = this;
			      if (self.state.masked) {
			    	  /*
			        this.expectHeader(4, function(data) {
			          var mask = data;
			          self.expectData(length, function(data) {
			            opcodes['10'].finish.call(self, mask, data);
			          });
			        });*/
			    	  self.expectHeader(4, new PacketHandler(){

			    		  @Override
			    		  public void onPacket(ByteBuffer data) throws Exception {
			    			  final byte[] mask = new byte[4]; ///data.array();
			    			  mask[0] = data.get(0);
			    			  mask[1] = data.get(1);
			    			  mask[2] = data.get(2);
			    			  mask[3] = data.get(3);
			    			
			    			  self.expectData(length, new PacketHandler(){
			    				  @Override
					    		  public void onPacket(ByteBuffer data2) throws Exception {
			    					  finish(mask, data2);
			    				  }
			    			  });
			    		  }

			    	  });
			      }
			      else {
			    	  /*
			        this.expectData(length, function(data) {
			          opcodes['10'].finish.call(self, null, data);
			        });*/
			    	  self.expectData(length, new PacketHandler(){
			    		  
			    		  @Override
			    		  public void onPacket(ByteBuffer data) throws Exception {
				    		  finish(null, data);
			    		  }

			    	  });
			      }
			    }

			  @Override
			  public void finish(byte[] mask, ByteBuffer data) throws Exception {
			      self.onpong((ByteBuffer) self.unmask(mask, data, true), new opcOptions(self.state.masked, null, true));
			      self.endPacket();
			    }

		  };
		  
	}
	

	/**
	 * Start processing a new packet.
	 *
	 * @api private
	 */
	private interface PacketHandler{
		public void onPacket(ByteBuffer data) throws Exception;
	}
	
	private class processPacket implements PacketHandler {

		@Override
		public void onPacket(ByteBuffer data) throws Exception {
			Log.d(TAG, "processPacket.onPacket: "+data);

			if (data != null) {
				String dstr = "";
				for (int i = 0; i < data.capacity(); i ++)
					dstr += " "+data.get(i);
				Log.d(TAG, dstr);
			}
			
			///if ((data[0] & 0x70) != 0) {
			if ((data.get(0) & 0x70) != 0) {
				error("reserved fields must be empty", 1002);
				return;
			}
			state.lastFragment = (data.get(0) & 0x80) == 0x80;
			state.masked = (data.get(1) & 0x80) == 0x80;
			int opcode = data.get(0) & 0xf;
		
			Log.d(TAG, "opcode: "+opcode+", masked:"+state.masked+",lastFragment:"+state.fragmentedOperation);
			
			if (opcode == 0) {
				// continuation frame
				state.fragmentedOperation = true;
				state.opcode = state.activeFragmentedOperation;
				if (!(state.opcode == 1 || state.opcode == 2)) {
					error("continuation frame cannot follow current opcode", 1002);
					return;
				}
			}
			else {
				if (opcode < 3 && state.activeFragmentedOperation != -1) {
					error("data frames after the initial data frame must have opcode 0", 1002);
					return;
				}
				state.opcode = opcode;
				if (state.lastFragment == false) {
					state.fragmentedOperation = true;
					state.activeFragmentedOperation = opcode;
				}
				else state.fragmentedOperation = false;
			}
			OpcHandler handler = opcodes(state.opcode); ///opcodes[state.opcode];
			if (handler == null) error("no handler for opcode " + state.opcode, 1002);
			else {
				handler.start(data);
			}
		}

	}
	
	/**
	 * Opcode handlers
	 */
	private interface OpcHandler {
		void start(ByteBuffer data) throws Exception;
	    void getData(int length) throws Exception;
	    void finish(byte[] mask, ByteBuffer data) throws Exception;
	}
	
	private OpcHandler opcodes(int opcode) {
		// text
		if (opcode ==  1) return _handler_text;
		
		// binary
		if (opcode ==  2) return _handler_binary;
		
		// close
		if (opcode ==  8) return _handler_close;
		
		// ping
		if (opcode ==  9) return _handler_ping;
		
		// pong
		if (opcode == 10) return _handler_pong;

		return null;
	}

/**
 * Unmask received data.
 * @throws CharacterCodingException 
 *
 * @api private
 */

private Object unmask(byte[] mask, ByteBuffer buf, boolean binary) throws CharacterCodingException {
  if (mask != null && buf != null) return BufferUtil.unmask(buf, mask);
  if (binary) return buf;
  
  // TBD...
  ///return buf != null ? buf.toString("utf8") : "";
  return buf != null ? _utf8_decoder.decode(buf).toString() : "";
}

/**
 * Concatenates a list of buffers.
 *
 * @api private
 */

private ByteBuffer concatBuffers(List<Object> buffers) {
	/*
  var length = 0;
  for (var i = 0, l = buffers.length; i < l; ++i) length += buffers[i].length;
  var mergedBuffer = new Buffer(length);
  bufferUtil.merge(mergedBuffer, buffers);
  return mergedBuffer;*/
	return Util.concatByteBuffer(buffers, 0);
}

/**
 * Handles an error
 * @throws Exception 
 *
 * @api private
 */

private Receiver error(String reason, int protocolErrorCode) throws Exception {
  this.reset();
  this.onerror(reason, protocolErrorCode);
  return this;
}


/**
 * Endprocessing a packet.
 * @throws Exception 
 *
 * @api private
 */

private void endPacket() throws Exception {
  if (!this.state.fragmentedOperation) this.unfragmentedBufferPool.reset(true);
  else if (this.state.lastFragment) this.fragmentedBufferPool.reset(false);
  this.expectOffset = 0;
  this.expectBuffer = null;
  this.expectHandler = null;
  if (this.state.lastFragment && this.state.opcode == this.state.activeFragmentedOperation) {
    // end current fragmented operation
    this.state.activeFragmentedOperation = -1; ///null;
  }
  this.state.lastFragment = false;
  this.state.opcode = this.state.activeFragmentedOperation != -1 ? this.state.activeFragmentedOperation : 0;
  this.state.masked = false;
  this.expectHeader(2, new processPacket());
};


/**
 * Reset the parser state.
 *
 * @api private
 */

private void reset() {
  if (this.dead) return;
  /*this.state = {
    activeFragmentedOperation: null,
    lastFragment: false,
    masked: false,
    opcode: 0,
    fragmentedOperation: false
  };*/
  this.state = new State(-1, false, false, 0, false);
  this.fragmentedBufferPool.reset(true);
  this.unfragmentedBufferPool.reset(true);
  this.expectOffset = 0;
  this.expectBuffer = null;
  this.expectHandler = null;
  this.overflow = new LinkedList<ByteBuffer>();///[];
  this.currentMessage = new LinkedList<Object>();///[];
}

/**
 * Waits for a certain amount of header bytes to be available, then fires a callback.
 * @throws Exception 
 *
 * @api private
 */
	private void expectHeader(int length, PacketHandler handler) throws Exception {
		Log.d(TAG, "expectHeader, length:"+length+",handler:"+handler);
		
		if (length == 0) {
			handler.onPacket(null);
			return;
		}
		this.expectBuffer = (ByteBuffer) Util.chunkSlice(this.headerBuffer, this.expectOffset, this.expectOffset + length); /// this.headerBuffer.slice(this.expectOffset, this.expectOffset + length);
		this.expectHandler = handler;
		int toRead = length;
		while (toRead > 0 && this.overflow.size() > 0) {
			ByteBuffer fromOverflow = this.overflow.remove(this.overflow.size() - 1); ///this.overflow.pop();
			if (toRead < fromOverflow.capacity()) this.overflow.add((ByteBuffer) Util.chunkSlice(fromOverflow, toRead, fromOverflow.capacity())/*fromOverflow.slice(toRead)*/);
			int read = Math.min(fromOverflow.capacity(), toRead);

			BufferUtil.fastCopy(read, (ByteBuffer) fromOverflow, this.expectBuffer, this.expectOffset);

			this.expectOffset += read;
			toRead -= read;
		}
		
		Log.d(TAG, "expectHeader, expectBuffer:"+expectBuffer+",expectOffset:"+expectOffset);
		
	}

/**
 * Waits for a certain amount of data bytes to be available, then fires a callback.
 *
 * @api private
 */
	private void  expectData(int length, PacketHandler handler) throws Exception {
		Log.d(TAG, "expectData, length:"+length+",handler:"+handler);

		if (length == 0) {
			handler.onPacket(null);
			return;
		}
		this.expectBuffer = this.allocateFromPool(length, this.state.fragmentedOperation);
		this.expectHandler = handler;
		int toRead = length;
		while (toRead > 0 && this.overflow.size() > 0) {
			ByteBuffer fromOverflow = this.overflow.remove(this.overflow.size() - 1); ///this.overflow.pop();
			if (toRead < fromOverflow.capacity()) this.overflow.add((ByteBuffer) Util.chunkSlice(fromOverflow, toRead, fromOverflow.capacity())/*fromOverflow.slice(toRead)*/);
			int read = Math.min(fromOverflow.capacity(), toRead);

			BufferUtil.fastCopy(read, fromOverflow, this.expectBuffer, this.expectOffset);

			this.expectOffset += read;
			toRead -= read;
		}
		
		Log.d(TAG, "expectData, expectBuffer:"+expectBuffer+",expectOffset:"+expectOffset);
		
	}

/**
 * Allocates memory from the buffer pool.
 *
 * @api private
 */
private ByteBuffer allocateFromPool(int length, boolean isFragmented) {
  return (isFragmented ? this.fragmentedBufferPool : this.unfragmentedBufferPool).get(length);
}
	
/**
 * Add new data to the parser.
 * @throws Exception 
 *
 * @api public
 */

protected void add(ByteBuffer data) throws Exception {
	Log.d(TAG, "add data: "+data);

	int dataLength = data!=null ? data.capacity() : 0; ///Util.chunkLength(data); ///data.length;
	if (dataLength == 0) return;
	if (this.expectBuffer == null) {
		this.overflow.add(data);
		return;
	}
	
	Log.d(TAG, "add data: ... 2");
	
	int toRead = Math.min(dataLength, this.expectBuffer.capacity() - this.expectOffset);
	BufferUtil.fastCopy(toRead, data, this.expectBuffer, this.expectOffset);
	
	Log.d(TAG, "add data: ... 3");

	this.expectOffset += toRead;
	if (toRead < dataLength) {
		this.overflow.add((ByteBuffer) Util.chunkSlice(data, toRead, data.capacity())/*data.slice(toRead)*/);
	}
	
	Log.d(TAG, "add data: ... 5");

	while (this.expectBuffer!=null && this.expectOffset == this.expectBuffer.capacity()) {
		ByteBuffer bufferForHandler = this.expectBuffer;
		this.expectBuffer = null;
		this.expectOffset = 0;
		///this.expectHandler.call(this, bufferForHandler);
		this.expectHandler.onPacket(bufferForHandler);
	}
	
	Log.d(TAG, "add data: ... 6");

}

/**
 * Releases all resources used by the receiver.
 *
 * @api public
 */
protected void cleanup() {
  this.dead = true;
  this.overflow = null;
  this.headerBuffer = null;
  this.expectBuffer = null;
  this.expectHandler = null;
  this.unfragmentedBufferPool = null;
  this.fragmentedBufferPool = null;
  this.state = null;
  this.currentMessage = null;
  
  /*
  this.onerror = null;
  this.ontext = null;
  this.onbinary = null;
  this.onclose = null;
  this.onping = null;
  this.onpong = null;*/
}

// Abstract methods
protected abstract void onerror(String reason, int protocolErrorCode) throws Exception;

protected abstract void ontext(String text, opcOptions options) throws Exception;

protected abstract void onbinary(ByteBuffer buf, opcOptions options) throws Exception;

protected abstract void onclose(int code, String message, opcOptions options) throws Exception;

protected abstract void onping(ByteBuffer buf, opcOptions options) throws Exception;

protected abstract void onpong(ByteBuffer buf, opcOptions options) throws Exception;

}
