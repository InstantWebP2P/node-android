package com.iwebpp.node.tests;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import android.util.Log;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.Readable;
import com.iwebpp.node.Readable2;
import com.iwebpp.node.Util;
import com.iwebpp.node.Writable;
import com.iwebpp.node.Writable.WriteCB;
import com.iwebpp.node.Writable2;

public final class StreamTest {
	private static final String TAG = "StreamTest";

    private class Counting extends Readable2 {

    	 private int _max = 68;
    	 private int _index = 1;
    	
    	Counting() {
			super(new Options(-1, "utf8", false, "utf8"));
			// TODO Auto-generated constructor stub
			
			_index = 1;
		}

		@Override
		public void _read(int size) throws Throwable {
			// TODO Auto-generated method stub
			int i = this._index++;
			if (i > this._max)
				this.push(null, null);
			else {
				String str = " " + i;
				ByteBuffer buf = ByteBuffer.wrap(str.getBytes("utf8"));
				this.push(buf, null);
				///this.push(str, "utf8");
			}
		}
    	
    }
    
    private class DummyWritable extends Writable2 {

		public DummyWritable() {
			super(new Options(-1, true, "utf8", false));
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean _write(Object chunk, String encoding, WriteCB cb) throws Throwable {
			// TODO Auto-generated method stub
			if (Util.isString(chunk)) {
				Log.d(TAG, "encdoing "+encoding+":"+chunk.toString());
				
				if (cb != null) cb.invoke(null);
				
			} else {
				Log.d(TAG, "binary data "+chunk.toString());
				
				if (cb != null) cb.invoke(null);

				// decode chunk to string
				String result = Charset.forName("utf8").newDecoder().decode((ByteBuffer)chunk).toString();
				Log.d(TAG, "decoded string "+result);
			}
			
			return true;
		}
    	
    }
    
    private boolean testPipe() {    	
		final Readable rs = new Counting();
				
		final Writable ws = new DummyWritable();
    	
		try {
			ws.on("pipe", new Listener () {
				@Override
				public void invoke(Object src) throws Throwable {
					Log.d(TAG, "something is piping into the writer");
					assert(rs.equals(src));
				}

			});
			ws.on("unpipe", new Listener () {
				@Override
				public void invoke(Object src) throws Throwable {
					Log.d(TAG, "something has stopped piping into the writer");
					assert(rs.equals(src));
				}

			});
			
			rs.pipe(ws, true);
			///rs.unpipe(ws);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		return true;
	}
    
    private boolean testFinish() {
    	final Writable ws = new DummyWritable();

    	try {
    		ws.on("finish", new Listener() {
    			@Override
    			public void invoke(Object src) throws Throwable {
    				Log.d(TAG, "all writes are now complete.");
    			}
    		});
    		
    		for (int i = 0; i < 100; i ++) {
    			ws.write("hello, #" + i + "!\n", null, new WriteCB() {

    				@Override
    				public void invoke(String error) throws Throwable {
    					// TODO Auto-generated method stub
    					Log.d(TAG, "write done");
    				}

    			});
    		}
    		
    		ws.end("this is the end\n", null, null);    		
    	} catch (Throwable e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}

    	return true;
    }
    
    public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				final StreamTest test = new StreamTest();

				test.testPipe();
				///test.testFinish();
			}
		})).start();

	}
    
}
