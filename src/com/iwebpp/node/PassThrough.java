package com.iwebpp.node;

public final class PassThrough 
extends Transform {

	public PassThrough(NodeContext ctx, Options roptions,
			com.iwebpp.node.Writable2.Options woptions) {
		super(ctx, roptions, woptions);
		// TODO Auto-generated constructor stub
	}
	private PassThrough(){super(null, null, null);}

	@Override
	public void _transform(Object chunk, String encoding,
			afterTransformCallback cb) throws Exception {
		  cb.afterTransform(null, chunk);
	}

	@Override
	public void _flush(flushCallback cb) throws Exception {
	    cb.onFlush(null);
	}

}
