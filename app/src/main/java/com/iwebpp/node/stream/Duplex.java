// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.stream;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.NodeContext.nextTickListener;

public abstract class Duplex
extends Readable2 
implements Writable {
	private Writable2 _writable;
	protected Writable2.State _writableState;
	private boolean allowHalfOpen;
	private NodeContext context;
	
	/**
	 * @return the allowHalfOpen
	 */
	public boolean isAllowHalfOpen() {
		return allowHalfOpen;
	}

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
	
	public static class Options {
		private boolean allowHalfOpen = true;
		private Readable2.Options roptions;
		private Writable2.Options woptions;
		
		public Options(Readable2.Options roptions, Writable2.Options woptions, boolean allowHalfOpen) {
			this.roptions = roptions;
			this.woptions = woptions;
			this.allowHalfOpen = allowHalfOpen;
		}
		@SuppressWarnings("unused")
		private Options(){}
	}
	
	protected Duplex(NodeContext ctx, Options options) {
		super(ctx, options.roptions);
        this.context = ctx;
        
		_writable = new DuplexWritable(ctx, options.woptions, this);
		_writableState = _writable._writableState;
		
		final Duplex self = this;

		/*
		  if (options && options.readable === false)
		    this.readable = false;

		  if (options && options.writable === false)
		    this.writable = false;

		  this.allowHalfOpen = true;
		  if (options && options.allowHalfOpen === false)
		    this.allowHalfOpen = false;
		 */
		this.allowHalfOpen = options.allowHalfOpen;
		
		this.once("end", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {

				// the no-half-open enforcer
				///function onend() {
				// if we allow half-open state, or if the writable side ended,
				// then we're ok.
				if (self.allowHalfOpen || self._writableState.isEnded())
					return;

				// no more data can be written.
				// But allow more writes to happen in this tick.
				///process.nextTick(this.end.bind(this));
				context.nextTick(new nextTickListener(){

					@Override
					public void onNextTick() throws Exception {
                        self.end(null, null, null);						
					}
					
				});
				///}

			}

		});
	}

	private Duplex(){
		super(null, null);
	}
	
	@Override
	public boolean write(Object chunk, String encoding, WriteCB cb) throws Exception {
		return _writable.write(chunk, encoding, cb);
	}
	@Override
	public boolean write(Object chunk, String encoding) throws Exception {
		return _writable.write(chunk, encoding);
	}
	@Override
	public boolean write(Object chunk) throws Exception {
		return _writable.write(chunk);
	}
	@Override
	public boolean write() throws Exception {
		return _writable.write();
	}
	
	@Override
	public boolean end(Object chunk, String encoding, WriteCB cb) throws Exception {
		return _writable.end(chunk, encoding, cb);
	}
	@Override
	public boolean end(Object chunk, String encoding) throws Exception {
		return _writable.end(chunk, encoding);
	}
	@Override
	public boolean end(Object chunk) throws Exception {
		return _writable.end(chunk);
	}
	@Override
	public boolean end() throws Exception {
		return _writable.end();
	}
	
	@Override
	public boolean writable() {
		return _writable.writable();
	}
	public void writable(boolean writable) {
		 _writable.writable(writable);
	}
	
    public void cork() {
    	_writable.cork();
    }
    public void uncork() throws Exception {
    	_writable.uncork();
    }
    public int corked() {
    	return _writable.corked();
    }
    
	public boolean isNeedDrain() {
		return _writable.isNeedDrain();
	}
    
	@Override
	protected abstract void _read(int size) throws Exception;
	
	protected abstract void _write(Object chunk, String encoding, WriteCB cb) throws Exception;
	
}
