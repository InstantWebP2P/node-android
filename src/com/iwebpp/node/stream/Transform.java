package com.iwebpp.node.stream;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.EventEmitter.Listener;

public abstract class Transform 
extends Duplex {

	private TransformState _transformState;

	public static class TransformState {
		private Transform stream;
		private boolean needTransform;
		private boolean transforming;
		private WriteCB writecb;
		private Object writechunk;
		private String writeencoding;

		public TransformState(Transform stream) {
			this.stream = stream;

			this.needTransform = false;
			this.transforming = false;
			this.writecb = null;
			this.writechunk = null;
		}
		@SuppressWarnings("unused")
		private TransformState(){}

		public void afterTransform(String er, Object data) throws Exception {
			afterTransform(stream, er, data);
		}

		private static void afterTransform(Transform stream, String er, Object data) throws Exception {
			TransformState ts = stream._transformState;
			ts.transforming = false;

			WriteCB cb = ts.writecb;

			if (null==cb) {
				///return stream.emit("error", new Error('no writecb in Transform class'));
				stream.emit("error", "no writecb in Transform class");
				return;
			}

			ts.writechunk = null;
			ts.writecb = null;

			///if (!util.isNullOrUndefined(data))
			if (!Util.isNullOrUndefined(data))
				stream.push(data, null);

			if (cb != null)
				cb.writeDone(er);

			State rs = stream.get_readableState();
			rs.setReading(false);
			if (rs.needReadable || rs.getLength() < rs.highWaterMark) {
				stream._read(rs.highWaterMark);
			}
		}

	}

	protected Transform(NodeContext ctx, Duplex.Options options) {
		super(ctx, options);
		// TODO Auto-generated constructor stub

		this._transformState = new TransformState(this);

		// when the writable side finishes, then flush out anything remaining.
		final Transform stream = this;

		// start out asking for a readable event once data is transformed.
		this.get_readableState().needReadable = true;

		// we have implemented the _read method, and done the other things
		// that Readable wants before the first _read call, so unset the
		// sync guard flag.
		this.get_readableState().sync = false;

		///this.once("prefinish", function() {
		this.once("prefinish", new Listener() {

			@Override
			public void onEvent(Object data) throws Exception {
				/*if (util.isFunction(stream._flush))
					  this._flush(function(er) {
						  done(stream, er);
					  });
				  else
					  done(stream);*/
				stream._flush(new flushCallback(){

					@Override
					public void onFlush(String er) throws Exception {
						done(stream, er);						
					}

				});
			}

		});

	}
	private Transform(){
		super(null, null);
	}

	public boolean push(Object chunk, String encoding) throws Exception {
		this._transformState.needTransform = false;
		return super.push(chunk, encoding);
	}

	protected void _write(Object chunk, String encoding, WriteCB cb) throws Exception {
		TransformState ts = this._transformState;
		ts.writecb = cb;
		ts.writechunk = chunk;
		ts.writeencoding = encoding;
		if (!ts.transforming) {
			State rs = this.get_readableState();
			if (ts.needTransform ||
				rs.needReadable ||
				rs.getLength() < rs.highWaterMark)
				this._read(rs.highWaterMark);
		}
	}

	// Doesn't matter what the args are here.
	// _transform does all the work.
	// That we got here means that the readable side wants more data.
	protected void _read(int size) throws Exception {
		final TransformState ts = this._transformState;

		if (!Util.isNull(ts.writechunk) && ts.writecb!=null && !ts.transforming) {
			ts.transforming = true;
			///this._transform(ts.writechunk, ts.writeencoding, ts.afterTransform);
			this._transform(ts.writechunk, ts.writeencoding, new afterTransformCallback(){

				@Override
				public void afterTransform(String error, Object data) throws Exception {
					ts.afterTransform(error, null);				
				}

			});
		} else {
			// mark that we need a transform, so that any data that comes in
			// will get processed, now that we've asked for it.
			ts.needTransform = true;
		}
	};


	private static boolean done(Transform stream, String er) throws Exception {
		///if (er != null) {
		if (!Util.zeroString(er)) {
			stream.emit("error", er);
			return false;
		}

		// if there's nothing in the write buffer, then that means
		// that nothing more will ever be provided
		com.iwebpp.node.stream.Writable2.State ws = stream._writableState;
		TransformState ts = stream._transformState;

		if (ws.getLength()!=0)
			throw new Exception("calling transform done when ws.length != 0");

		if (ts.transforming)
			throw new Exception("calling transform done when still transforming");

		return stream.push(null, null);
	}

	// This is the part where you do stuff!
	// override this function in implementation classes.
	// 'chunk' is an input chunk.
	//
	// Call `push(newChunk)` to pass along transformed output
	// to the readable side.  You may call 'push' zero or more times.
	//
	// Call `cb(err)` when you are done with this chunk.  If you pass
	// an error, then that'll put the hurt on the whole operation.  If you
	// never call cb(), then you'll never get another chunk.
	protected static interface afterTransformCallback {
		void afterTransform(String error, Object data) throws Exception;
	}
	protected abstract void _transform(final Object chunk, String encoding, 
			afterTransformCallback cb) throws Exception;

	protected static interface flushCallback {
		void onFlush(String error) throws Exception;
	}
	protected abstract void _flush(flushCallback cb) throws Exception;

}
