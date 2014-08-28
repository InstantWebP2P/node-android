package com.iwebpp.node;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.iwebpp.node.Writable2.Options;


public abstract class Readable2 
extends EventEmitter2 
implements Readable {
	private final static String TAG = "Readable2";
	private boolean didOnEnd = false;
	private volatile boolean isReadable = false;
	private State _readableState;
	private boolean readable;

	public static class Options {
		
		public int highWaterMark;
		public boolean objectMode;
		public String defaultEncoding;
		public String encoding;
		
	}
	private class State {
		private boolean objectMode;
		private int highWaterMark;
		private List<Object> buffer; // ByteBuffer or String
		private int length;
		private List<Writable> pipes;
		private int pipesCount;
		private boolean flowing;
		private boolean ended;
		private boolean endEmitted;
		private boolean reading;
		private boolean sync;
		private boolean needReadable;
		private boolean emittedReadable;
		private boolean readableListening;
		private String defaultEncoding;
		private boolean ranOut;
		private int awaitDrain;
		private boolean readingMore;
		private CharsetDecoder decoder;
		private CharsetEncoder encoder;
		private String encoding;
		
		
		public State(Options options, final Readable2 stream) {
			///options = options || {};

			// object stream flag. Used to make read(n) ignore n and to
			// make all the buffer merging and length checks go away
			this.objectMode = !!options.objectMode;

			///if (stream instanceof Stream.Duplex)
			///	this.objectMode = this.objectMode || !!options.readableObjectMode;

			// the point at which it stops calling _read() to fill the buffer
			// Note: 0 is a valid value, means "don't call _read preemptively ever"
			int hwm = options.highWaterMark;
			int defaultHwm = this.objectMode ? 16 : 16 * 1024;
			this.highWaterMark = (hwm >= 0) ? hwm : defaultHwm;

			// cast to ints.
			///this.highWaterMark = ~~this.highWaterMark;

			this.buffer = new ArrayList<Object>();
			this.length = 0;
			this.pipes = new ArrayList<Writable>();
			this.pipesCount = 0;
			this.flowing = false;
			this.ended = false;
			this.endEmitted = false;
			this.reading = false;

			// a flag to be able to tell if the onwrite cb is called immediately,
			// or on a later tick.  We set this to true at first, because any
			// actions that shouldn't happen until "later" should generally also
			// not happen before the first write call.
			this.sync = true;

			// whenever we return null, then we set a flag to say
			// that we're awaiting a 'readable' event emission.
			this.needReadable = false;
			this.emittedReadable = false;
			this.readableListening = false;

			// Crypto is kind of old and crusty.  Historically, its default string
			// encoding is 'binary' so we have to make this configurable.
			// Everything else in the universe uses 'utf8', though.
			this.defaultEncoding = options.defaultEncoding != null ? options.defaultEncoding : "UTF-8";

			// when piping, we only care about 'readable' events that happen
			// after read()ing all the bytes and not getting any pushback.
			this.ranOut = false;

			// the number of writers that are awaiting a drain event in .pipe()s
			this.awaitDrain = 0;

			// if true, a maybeReadMore has been scheduled
			this.readingMore = false;

			this.decoder = null;
			this.encoding = null;
			if (options.encoding != null && options.encoding != "") {
				///if (!StringDecoder)
				///	StringDecoder = require('string_decoder').StringDecoder;
				///this.decoder = new StringDecoder(options.encoding);
				this.decoder =  Charset.forName(options.encoding).newDecoder();
				this.encoding = options.encoding;
			}
		}
		private State() {}
	}
	
	Readable2(Options options) {
		super();

		///if (!(this instanceof Readable))
		///	    return new Readable(options);

		this._readableState = new State(options, this);

		// legacy
		this.readable = true;
	}
	private Readable2() {}

	// Manually shove something into the read() buffer.
	// This returns true if the highWaterMark has not been hit yet,
	// similar to how Writable.write() returns true if you should
	// write() some more.
	public boolean push(Object chunk, String encoding) {
	  State state = this._readableState;

	  ///if (util.isString(chunk) && !state.objectMode) {
	  if ((chunk instanceof String) && !state.objectMode) {
	    encoding = encoding != null ?  encoding : state.defaultEncoding;
	    if (encoding != state.encoding) {
	      ///chunk = new Buffer(chunk, encoding);
			try {
				chunk = ByteBuffer.wrap(((String)chunk).getBytes(encoding));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	      encoding = "";
	    }
	  }

	  return readableAddChunk(this, state, chunk, encoding, false);
	}
	
	// Unshift should *always* be something directly out of read()
	public boolean unshift(Object chunk) {
	  State state = this._readableState;
	  return readableAddChunk(this, state, chunk, "", true);
	}
	
private static boolean readableAddChunk(Readable2 stream, State state, 
		Object chunk, String encoding, boolean addToFront) {
	  String er = chunkInvalid(state, chunk);
	  if (er != null) {
	    stream.emit("error", er);
	  } else if (chunk == null) {
	    state.reading = false;
	    if (!state.ended)
	      onEofChunk(stream, state);
	  } else if (state.objectMode || chunk != null /*&& chunk.length > 0*/) {
	    if (state.ended && !addToFront) {
	      ///var e = new Error('stream.push() after EOF');
	      String e = "stream.push() after EOF";
	      stream.emit("error", e);
	    } else if (state.endEmitted && addToFront) {
	      ////var e = new Error('stream.unshift() after end event');
	      String e = "stream.unshift() after end event";
	      stream.emit("error", e);
	    } else {
	      if (state.decoder!=null && !addToFront && encoding!=null)
			try {
				chunk = state.decoder.decode((ByteBuffer) chunk);
			} catch (CharacterCodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	      if (!addToFront)
	        state.reading = false;

	      // if we want the data now, just emit it.
	      if (state.flowing && state.length == 0 && !state.sync) {
	        stream.emit("data", chunk);
	        stream.read(0);
	      } else {
	        // update the buffer info.
	        state.length += state.objectMode ? 1 : (chunk instanceof ByteBuffer) ? 
					((ByteBuffer)chunk).position() : ((String)chunk).length();
	        if (addToFront) {
	          ///state.buffer.unshift(chunk);
	          state.buffer.add(0, chunk);	
	        } else
	          ///state.buffer.push(chunk);
	        	state.buffer.add(chunk);

	        if (state.needReadable)
	          emitReadable(stream);
	      }

	      maybeReadMore(stream, state);
	    }
	  } else if (!addToFront) {
	    state.reading = false;
	  }

	  return needMoreData(state);
	} 

//if it's past the high water mark, we can push in some more.
//Also, if we have no data yet, we can stand some
//more bytes.  This is to work around cases where hwm=0,
//such as the repl.  Also, if the push() triggered a
//readable event, and the user called read(largeNumber) such that
//needReadable was set, then we ought to push more, so that another
//'readable' event will be triggered.
private static boolean needMoreData(State state) {
	  return !state.ended &&
		         (state.needReadable ||
		          state.length < state.highWaterMark ||
		          state.length == 0);
}

// backwards compatibility.
public boolean setEncoding(String enc) {
	/*if (!StringDecoder)
 StringDecoder = require('string_decoder').StringDecoder;
this._readableState.decoder = new StringDecoder(enc);
this._readableState.encoding = enc;
	 */
	this._readableState.decoder =  Charset.forName(enc).newDecoder();
	this._readableState.encoding=enc;
	
	return true;
};

//Don't raise the hwm > 128MB
private static int MAX_HWM = 0x800000;
private static int roundUpToNextPowerOf2(int n) {
if (n >= MAX_HWM) {
 n = MAX_HWM;
} else {
 // Get the next highest power of 2
 n--;
 for (int p = 1; p < 32; p <<= 1) n |= n >> p;
 n++;
}
return n;
}

private static int howMuchToRead(int n, State state) {
	if (state.length == 0 && state.ended)
		return 0;

	if (state.objectMode)
		return n == 0 ? 0 : 1;

	///if (util.isNull(n) || isNaN(n)) {
	if (n < 0) {
		// only flow one buffer at a time
		if (state.flowing && state.buffer.size() != 0)
			return (state.buffer.get(0) instanceof ByteBuffer) ? 
					((ByteBuffer)state.buffer.get(0)).position() : ((String)state.buffer.get(0)).length();
		else
			return state.length;
	}

	if (n <= 0)
		return 0;

	// If we're asking for more than the target buffer level,
	// then raise the water mark.  Bump up to the next highest
	// power of 2, to prevent increasing it excessively in tiny
	// amounts.
	if (n > state.highWaterMark)
		state.highWaterMark = roundUpToNextPowerOf2(n);

	// don't have that much.  return null, unless we've ended.
	if (n > state.length) {
		if (!state.ended) {
			state.needReadable = true;
			return 0;
		} else
			return state.length;
	}

	return n;
}

private static void maybeReadMore(Readable2 stream, State state) {
	// TODO Auto-generated method stub
	
}
private static void emitReadable(Readable2 stream) {
	// TODO Auto-generated method stub
	
}
private static void onEofChunk(Readable2 stream, State state) {
	// TODO Auto-generated method stub
	
}
private static String chunkInvalid(State state, Object chunk) {
	String er = null;
	/*if (!util.isBuffer(chunk) &&
      !util.isString(chunk) &&
      !util.isNullOrUndefined(chunk) &&
      !state.objectMode) {
    er = new TypeError('Invalid non-string/buffer chunk');
  }*/
	if (!(chunk instanceof ByteBuffer) && 
		!(chunk instanceof String) &&
		!(chunk == null) &&
		!state.objectMode) 
		er = "Invalid non-string/buffer chunk";
	return er;
}

	@Override
	public Object read(int size) {
		// TODO Auto-generated method stub
		return 0;
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

	public Writable pipe(final Writable dest, boolean end) {
		final Readable source = this;

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
		dest.on("drain", ondrain);

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

					dest.removeListener("drain");
					dest.removeListener("error");
					dest.removeListener("close");
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

				dest.removeListener("drain");
				dest.removeListener("error");
				dest.removeListener("close");
			}
		};

		source.on("error", onerror);
		dest.on("error", onerror);

		source.on("end", cleanup);

		source.on("close", cleanup);
		dest.on("close", cleanup);

		dest.emit("pipe", source);

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
