package com.iwebpp.node;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public abstract class Readable2 
extends EventEmitter2 
implements Readable {
	private final static String TAG = "Readable2";
	private List<Object> dataBuffer;
	private boolean didOnEnd = false;
	private volatile boolean isReadable = false;

	Readable2() {
		super();
		// TODO Auto-generated constructor stub
		dataBuffer = new ArrayList<Object>();
	}

	@Override
	public Object read(int size) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean setEncoding(String encoding) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pause() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resume() {
		// TODO Auto-generated method stub
		return false;
	}

	public Writable2 pipe(final Writable2 dest, boolean end) {
		final Readable2 source = this;

		final EventEmitter.Listener ondata = new EventEmitter.Listener() {
			@Override
			public void invoke(Object chunk) {
				if (dest.writable()) {
					if (false == dest.write(chunk, null, null)) {
						source.pause();
					}
				}
			}
		};
		source.on("data", ondata);

		final EventEmitter.Listener ondrain = new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) {
				if (source.readable()) {
					source.resume();
				}
			}
		};
		((EventEmitter2) dest).on("drain", ondrain);

		final EventEmitter.Listener onend = new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) {
				if (didOnEnd) return;
				didOnEnd = true;

				dest.end(null, null, null);
			}
		};

		final EventEmitter.Listener onclose = new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) {
				if (didOnEnd) return;
				didOnEnd = true;

				///if (util.isFunction(dest.destroy)) dest.destroy();
			}
		};

		// If the 'end' option is not supplied, dest.end() will be called when
		// source gets the 'end' or 'close' events.  Only dest.end() once.
		if (end != false) {
			source.on("end", onend);
			source.on("close", onclose);
		}

		// don't leave dangling pipes when there are errors.
		final EventEmitter.Listener onerror = new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) {
				{
					source.removeListener("data");
					source.removeListener("end");
					source.removeListener("close");
					source.removeListener("error");
					
					((EventEmitter2) dest).removeListener("drain");
					((EventEmitter2) dest).removeListener("error");
					((EventEmitter2) dest).removeListener("close");
				}
				
			    try {
					if (listenerCount("error") == 0) {
					  throw new Exception("Unhandled error"); // Unhandled stream error in pipe.
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
		};

		// remove all the event listeners that were added.
		final EventEmitter.Listener cleanup = new EventEmitter.Listener() {
			@Override
			public void invoke(Object data) {
				source.removeListener("data");
				source.removeListener("end");
				source.removeListener("close");
				source.removeListener("error");
				
				((EventEmitter2) dest).removeListener("drain");
				((EventEmitter2) dest).removeListener("error");
				((EventEmitter2) dest).removeListener("close");
			}
		};

		source.on("error", onerror);
		((EventEmitter2) dest).on("error", onerror);

		source.on("end", cleanup);
		source.on("close", cleanup);

		((EventEmitter2) dest).on("close", cleanup);

		((EventEmitter2) dest).emit("pipe", source);

		// Allow for unix-like usage: A.pipe(B).pipe(C)
		return dest;
	}

	@Override
	public boolean unpipe(Writable ws) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unshift(ByteBuffer chunk) {
		// TODO Auto-generated method stub
		return false;
	}

    public boolean readable() {
    	return isReadable;
    }

	// _read(size)
	public abstract void _read(int size);
}
