package com.iwebpp.node.tests;

import android.util.Log;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.Readable;
import com.iwebpp.node.Readable2;
import com.iwebpp.node.Writable;
import com.iwebpp.node.Writable2;

public final class StreamTest {
	private static final String TAG = "StreamTest";

    private class DummyReadable extends Readable2 {

		DummyReadable(Options options) {
			super(options);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void _read(int size) {
			// TODO Auto-generated method stub
			
		}
    	
    }
    
    private class DummyWritable extends Writable2 {

		public DummyWritable(Options options) {
			super(options);
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean _write(Object chunk, String encoding, WriteCB cb) {
			// TODO Auto-generated method stub
			return false;
		}
    	
    }
    
    private boolean testPipe() {
    	Readable2.Options rops = new Readable2.Options();
    	rops.defaultEncoding = "";
    	rops.encoding = "";
    	rops.highWaterMark = -1;
    	rops.objectMode = false;
    	
		Readable rs = new DummyReadable(rops);
		
		Writable2.Options wops = new Writable2.Options();
		wops.defaultEncoding = "";
		wops.decodeStrings = false;
		wops.highWaterMark = -1;
		wops.objectMode = false;
		
    	Writable ws = new DummyWritable(wops);
    	
    	rs.pipe(ws, true);
    	
		return true;
	}
    
    public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				final StreamTest test = new StreamTest();

				test.testPipe();			    
			}
		})).start();

	}
    
}
