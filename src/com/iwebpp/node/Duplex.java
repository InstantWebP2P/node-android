package com.iwebpp.node;

public abstract class Duplex extends Readable2 
implements Writable {
	private final static String TAG = "Duplex";

	private Writable2 _writable;
	protected Writable2.State _writableState;
	
	private class DuplexWritable extends Writable2 {
		private Duplex hold;

		protected DuplexWritable(Options options, Duplex hold) {
			super(options);
			// TODO Auto-generated constructor stub
			this.hold = hold;
		}
		private DuplexWritable() {super(null);}

		@Override
		public boolean _write(Object chunk, String encoding, WriteCB cb) throws Exception {
			// TODO Auto-generated method stub
			return hold._write(chunk, encoding, cb);
		}
		
	}
	
	protected Duplex(Readable2.Options roptions, Writable2.Options woptions) {
		super(roptions);
		// TODO Auto-generated constructor stub
		_writable = new DuplexWritable(woptions, this);
		_writableState = _writable._writableState;
	}

	private Duplex(){
		super(null);
	}
	
	@Override
	public boolean write(Object chunk, String encoding, WriteCB cb) throws Exception {
		// TODO Auto-generated method stub
		return _writable.write(chunk, encoding, cb);
	}
	@Override
	public void end(Object chunk, String encoding, WriteCB cb) throws Exception {
		// TODO Auto-generated method stub
		_writable.end(chunk, encoding, cb);
	}
	@Override
	public boolean writable() {
		// TODO Auto-generated method stub
		return _writable.writable();
	}
	
	@Override
	public abstract void _read(int size) throws Exception;
	
	public abstract boolean _write(Object chunk, String encoding, WriteCB cb) throws Exception;
	
}
