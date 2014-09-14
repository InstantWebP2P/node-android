package com.iwebpp.node;

public abstract class Duplex
extends Readable2 
implements Writable {
	private Writable2 _writable;
	protected Writable2.State _writableState;
	
	/**
	 * @return the _writableState
	 */
	public Writable2.State get_writableState() {
		return _writableState;
	}

	private class DuplexWritable extends Writable2 {
		private Duplex hold;
		protected DuplexWritable(NodeContext context, Options options, Duplex hold) {
			super(context, options);
			this.hold = hold;
		}
		private DuplexWritable() {super(null, null);}

		@Override
		protected void _write(Object chunk, String encoding, WriteCB cb) throws Exception {
			hold._write(chunk, encoding, cb);
		}
		
	}
	
	protected Duplex(NodeContext ctx, Readable2.Options roptions, Writable2.Options woptions) {
		super(ctx, roptions);

		_writable = new DuplexWritable(ctx, woptions, this);
		_writableState = _writable._writableState;
	}

	private Duplex(){
		super(null, null);
	}
	
	@Override
	public boolean write(Object chunk, String encoding, WriteCB cb) throws Exception {
		return _writable.write(chunk, encoding, cb);
	}
	@Override
	public void end(Object chunk, String encoding, WriteCB cb) throws Exception {
		_writable.end(chunk, encoding, cb);
	}
	@Override
	public boolean writable() {
		return _writable.writable();
	}
	
	@Override
	protected abstract void _read(int size) throws Exception;
	
	protected abstract void _write(Object chunk, String encoding, WriteCB cb) throws Exception;
	
}
