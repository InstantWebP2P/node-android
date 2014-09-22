package com.iwebpp.node.stream;

import com.iwebpp.node.NodeContext;

public final class PassThrough 
extends Transform {

	public PassThrough(NodeContext ctx, Duplex.Options options) {
		super(ctx, options);
		// TODO Auto-generated constructor stub
	}
	private PassThrough(){super(null, null);}

	@Override
	protected void _transform(Object chunk, String encoding,
			afterTransformCallback cb) throws Exception {
		  cb.afterTransform(null, chunk);
	}

	@Override
	protected void _flush(flushCallback cb) throws Exception {
	    cb.onFlush(null);
	}

}
