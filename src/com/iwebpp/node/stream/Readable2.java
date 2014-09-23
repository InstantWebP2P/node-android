package com.iwebpp.node.stream;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

import com.iwebpp.node.EventEmitter;
import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.others.TripleState;

import android.text.TextUtils;
import android.util.Log;


public abstract class Readable2 
extends EventEmitter2 
implements Readable {
	private final static String TAG = "Readable2";
	private State _readableState;
	private boolean readable;
	private NodeContext context;

	public static class Options {

		private int highWaterMark;
		private boolean objectMode;
		private String defaultEncoding;
		private String encoding;
		public boolean readable;

		public Options(
				int highWaterMark, 
				String encoding,
				boolean objectMode, 
				String defaultEncoding,
				boolean readable) {
			this.highWaterMark = highWaterMark;
			this.encoding = encoding;
			this.objectMode = objectMode;
			this.defaultEncoding = defaultEncoding;
			this.readable = readable;
		}
		/**
		 * @return the highWaterMark
		 */
		public int getHighWaterMark() {
			return highWaterMark;
		}
		/**
		 * @return the objectMode
		 */
		public boolean isObjectMode() {
			return objectMode;
		}
		/**
		 * @return the defaultEncoding
		 */
		public String getDefaultEncoding() {
			return defaultEncoding;
		}
		/**
		 * @return the encoding
		 */
		public String getEncoding() {
			return encoding;
		}
		@SuppressWarnings("unused")
		private Options() {}
	}
	
	public static class State {
		boolean objectMode;
		int highWaterMark;
		List<Object> buffer; // ByteBuffer or String
		private int length;
		List<Writable> pipes;
		int pipesCount;
		TripleState flowing;
		
		/**
		 * @param flowing the flowing to set
		 */
		public void setFlowing(TripleState flowing) {
			this.flowing = flowing;
		}
		/**
		 * @return the flowing
		 */
		public TripleState isFlowing() {
			return flowing;
		}
		boolean ended;
		private boolean endEmitted;
		private boolean reading;
		boolean sync;
		boolean needReadable;
		boolean emittedReadable;
		boolean readableListening;
		String defaultEncoding;
		int awaitDrain;
		boolean readingMore;
		CharsetDecoder decoder;
		String encoding;
		private boolean resumeScheduled;


		protected State(Options options, final Readable2 stream) {
			///options = options || {};

			// object stream flag. Used to make read(n) ignore n and to
			// make all the buffer merging and length checks go away
			this.setObjectMode(options.objectMode);

			// TBD...
			///if (stream instanceof Duplex)
			///    this.objectMode = this.objectMode || !!options.readableObjectMode;

			// the point at which it stops calling _read() to fill the buffer
			// Note: 0 is a valid value, means "don't call _read preemptively ever"
			int hwm = options.highWaterMark;
			int defaultHwm = this.isObjectMode() ? 16 : 16 * 1024;
			this.highWaterMark = (hwm >= 0) ? hwm : defaultHwm;

			// cast to ints.
			///this.highWaterMark = ~~this.highWaterMark;

			this.buffer = new ArrayList<Object>();
			this.setLength(0);
			this.pipes = new ArrayList<Writable>();
			this.pipesCount = 0;
			this.flowing = TripleState.MAYBE;
			this.setEnded(false);
			this.setEndEmitted(false);
			this.setReading(false);

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
			this.defaultEncoding = options.defaultEncoding != null ? 
					options.defaultEncoding : "UTF-8";

			// the number of writers that are awaiting a drain event in .pipe()s
			this.awaitDrain = 0;

			// if true, a maybeReadMore has been scheduled
			this.readingMore = false;

			this.setDecoder(null);
			this.encoding = null;
			if (!Util.zeroString(options.encoding)) {
				///if (!StringDecoder)
				///	StringDecoder = require('string_decoder').StringDecoder;
				///this.decoder = new StringDecoder(options.encoding);
				this.setDecoder(Charset.forName(options.encoding).newDecoder());
				this.encoding = options.encoding;
			}
		}
		@SuppressWarnings("unused")
		private State() {}
		/**
		 * @return the decoder
		 */
		public CharsetDecoder getDecoder() {
			return decoder;
		}
		/**
		 * @param decoder the decoder to set
		 */
		public void setDecoder(CharsetDecoder decoder) {
			this.decoder = decoder;
		}
		/**
		 * @return the ended
		 */
		public boolean isEnded() {
			return ended;
		}
		/**
		 * @param ended the ended to set
		 */
		public void setEnded(boolean ended) {
			this.ended = ended;
		}
		/**
		 * @return the objectMode
		 */
		public boolean isObjectMode() {
			return objectMode;
		}
		/**
		 * @param objectMode the objectMode to set
		 */
		public void setObjectMode(boolean objectMode) {
			this.objectMode = objectMode;
		}
		/**
		 * @return the resumeScheduled
		 */
		public boolean isResumeScheduled() {
			return resumeScheduled;
		}
		/**
		 * @param resumeScheduled the resumeScheduled to set
		 */
		public void setResumeScheduled(boolean resumeScheduled) {
			this.resumeScheduled = resumeScheduled;
		}
		/**
		 * @return the endEmitted
		 */
		public boolean isEndEmitted() {
			return endEmitted;
		}
		/**
		 * @param endEmitted the endEmitted to set
		 */
		public void setEndEmitted(boolean endEmitted) {
			this.endEmitted = endEmitted;
		}
		/**
		 * @return the reading
		 */
		public boolean isReading() {
			return reading;
		}
		/**
		 * @param reading the reading to set
		 */
		public void setReading(boolean reading) {
			this.reading = reading;
		}
		/**
		 * @return the length
		 */
		public int getLength() {
			return length;
		}
		/**
		 * @param length the length to set
		 */
		public void setLength(int length) {
			this.length = length;
		}
	}

	protected Readable2(NodeContext context, Options options) {
		super();

		this.context = context;
		
		///if (!(this instanceof Readable))
		///	    return new Readable(options);

		this._readableState = new State(options, this);

		// legacy
		this.readable = options.readable;/// true;
	}
	@SuppressWarnings("unused")
	private Readable2() {}

	// Unshift should *always* be something directly out of read()
	public boolean unshift(Object chunk) throws Exception {
		State state = this._readableState;
		return readableAddChunk(this, state, chunk, "", true);
	}

	private  boolean readableAddChunk(Readable2 stream, State state, 
			Object chunk, String encoding, boolean addToFront) throws Exception {
		String er = chunkInvalid(state, chunk);
		if (er != null) {
			stream.emit("error", er);
		} else if (chunk == null) {
			state.setReading(false);
			if (!state.isEnded())
				onEofChunk(stream, state);
		} else if (state.isObjectMode() || chunk != null && Util.chunkLength(chunk) > 0) {
			if (state.isEnded() && !addToFront) {
				///var e = new Error('stream.push() after EOF');
				String e = "stream.push() after EOF";
				stream.emit("error", e);
			} else if (state.isEndEmitted() && addToFront) {
				////var e = new Error('stream.unshift() after end event');
				String e = "stream.unshift() after end event";
				stream.emit("error", e);
			} else {
				if (state.getDecoder()!=null && !addToFront && Util.zeroString(encoding)) {
					chunk = state.getDecoder().decode((ByteBuffer) chunk).toString();
					Log.d(TAG, "decoded chunk "+chunk);
				}

				if (!addToFront)
					state.setReading(false);

				// if we want the data now, just emit it.
				if (state.flowing==TripleState.TRUE && state.getLength() == 0 && !state.sync) {
					stream.emit("data", chunk);
					stream.read(0);
				} else {
					// update the buffer info.
					state.setLength(state.getLength()
							+ (state.isObjectMode() ? 1 : Util.chunkLength(chunk)));
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
			state.setReading(false);
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
	private  boolean needMoreData(State state) {
		Log.d(TAG, "needMoreData: state.length:"+
				state.getLength()+",state.highWaterMark:"+
				state.highWaterMark);

		return !state.isEnded() &&
				(state.needReadable ||
						state.getLength() < state.highWaterMark ||
						state.getLength() == 0);
	}

	// backwards compatibility.
	public boolean setEncoding(String enc) {
		/*if (!StringDecoder)
 StringDecoder = require('string_decoder').StringDecoder;
this._readableState.decoder = new StringDecoder(enc);
this._readableState.encoding = enc;
		 */
		if (!Util.zeroString(enc)) {
			this._readableState.setDecoder(Charset.forName(enc).newDecoder());
			this._readableState.encoding = enc;
		} else {
			this._readableState.setDecoder(null);
			this._readableState.encoding = null;
		}

		return true;
	};

	//Don't raise the hwm > 128MB
	private  int MAX_HWM = 0x800000;
	private  int roundUpToNextPowerOf2(int n) {
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

	private  int howMuchToRead(int n, State state) {
		if (state.getLength() == 0 && state.isEnded())
			return 0;

		if (state.isObjectMode())
			return n == 0 ? 0 : 1;

		///if (util.isNull(n) || isNaN(n)) {
		if (n < 0) {
			// only flow one buffer at a time
			if (state.flowing==TripleState.TRUE && state.buffer.size() > 0)
				return Util.chunkLength(state.buffer.get(0));
			else
				return state.getLength();
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
		if (n > state.getLength()) {
			if (!state.isEnded()) {
				state.needReadable = true;
				return 0;
			} else
				return state.getLength();
		}

		return n;
	}

	//you can override either this method, or the async _read(n) below.
	public Object read(int n) throws Exception {
		Log.d(TAG, "read " + n);
		State state = this._readableState;
		int nOrig = n;

		///if (!util.isNumber(n) || n > 0)
		if (n > 0 || n < 0)
			state.emittedReadable = false;

		// if we're doing read(0) to trigger a readable event, but we
		// already have a bunch of data in the buffer, then just trigger
		// the 'readable' event and move on.
		if (n == 0 &&
				state.needReadable &&
				(state.getLength() >= state.highWaterMark || state.isEnded())) {
			Log.d(TAG, "read: emitReadable" + state.getLength() + "" + state.isEnded());
			if (state.getLength() == 0 && state.isEnded())
				endReadable(this);
			else
				emitReadable(this);
			return null;
		}

		n = howMuchToRead(n, state);

		// if we've ended, and we're now clear, then finish it up.
		if (n == 0 && state.isEnded()) {
			if (state.getLength() == 0)
				endReadable(this);
			return null;
		}

		// All the actual chunk generation logic needs to be
		// *below* the call to _read.  The reason is that in certain
		// synthetic stream cases, such as passthrough streams, _read
		// may be a completely synchronous operation which may change
		// the state of the read buffer, providing enough data when
		// before there was *not* enough.
		//
		// So, the steps are:
		// 1. Figure out what the state of things will be after we do
		// a read from the buffer.
		//
		// 2. If that resulting state will trigger a _read, then call _read.
		// Note that this may be asynchronous, or synchronous.  Yes, it is
		// deeply ugly to write APIs this way, but that still doesn't mean
		// that the Readable class should behave improperly, as streams are
		// designed to be sync/async agnostic.
		// Take note if the _read call is sync or async (ie, if the read call
		// has returned yet), so that we know whether or not it's safe to emit
		// 'readable' etc.
		//
		// 3. Actually pull the requested chunks out of the buffer and return.

		// if we need a readable event, then we need to do some reading.
		boolean doRead = state.needReadable;
		Log.d(TAG, "need readable " + doRead);

		// if we currently have less than the highWaterMark, then also read some
		if (state.getLength() == 0 || state.getLength() - n < state.highWaterMark) {
			doRead = true;
			Log.d(TAG, "length less than watermark " + doRead);
		}

		// however, if we've ended, then there's no point, and if we're already
		// reading, then it's unnecessary.
		if (state.isEnded() || state.isReading()) {
			doRead = false;
			Log.d(TAG, "reading or ended " + doRead);
		}

		if (doRead) {
			Log.d(TAG, "do read");
			state.setReading(true);
			state.sync = true;
			// if the length is currently zero, then we *need* a readable event.
			if (state.getLength() == 0)
				state.needReadable = true;
			// call internal read method
			this._read(state.highWaterMark);
			state.sync = false;
		}

		// If _read pushed data synchronously, then `reading` will be false,
		// and we need to re-evaluate how much data we can return to the user.
		if (doRead && !state.isReading())
			n = howMuchToRead(nOrig, state);

		Object ret;
		if (n > 0)
			ret = fromList(n, state);
		else
			ret = null;

		///if (util.isNull(ret)) {
		if (Util.isNull(ret)) {
			state.needReadable = true;
			n = 0;
		}

		state.setLength(state.getLength() - n);

		// If we have nothing in the buffer, then we want to know
		// as soon as we *do* get something into the buffer.
		if (state.getLength() == 0 && !state.isEnded())
			state.needReadable = true;

		// If we tried to read() past the EOF, then emit end on the next tick.
		if (nOrig != n && state.isEnded() && state.getLength() == 0)
			endReadable(this);

		///if (!util.isNull(ret))
		if (!Util.isNull(ret))
			this.emit("data", ret);

		return ret;
	}

	//Pluck off n bytes from an array of buffers.
	//Length is the combined lengths of all the buffers in the list.
	private  Object fromList(int n, State state) {
		List<Object> list = state.buffer;
		int length = state.getLength();
		boolean stringMode = state.getDecoder() != null;
		boolean objectMode = !!state.isObjectMode();
		Object ret;
		
		
		Log.d(TAG, "fromList n="+n);

		// nothing in the list, definitely empty.
		if (list.size() == 0)
			return null;

		if (length == 0) {
			Log.d(TAG, "length == 0");
			
			ret = null;
		} else if (objectMode) {
			Log.d(TAG, "objectMode");

			///ret = list.shift();
			ret = list.get(0);
		} else if (n==0 || n >= length) {	
			Log.d(TAG, "n==0 || n >= length");

			// read it all, truncate the array.
			if (stringMode) {
				///ret = list.join('');
				ret = TextUtils.join("", list);
				
				Log.d(TAG, "join.string:"+ret.toString());
			} else {
				///ret = Buffer.concat(list, length);
				ret = Util.concatByteBuffer(list, length);
			}
			
			///list.length = 0;
			list.clear();
		} else {			
			// read just some of it.
			///if (n < list[0].length) {
			if (n < Util.chunkLength(list.get(0))) {
				Log.d(TAG, "n < Util.chunkLength(list.get(0))");

				// just take a part of the first list item.
				// slice is the same for buffers and strings.
				Object buf = list.get(0);
				///ret = buf.slice(0, n);
				ret = Util.chunkSlice(buf, 0, n);
				///list[0] = buf.slice(n);
				list.set(0, Util.chunkSlice(buf, n));
			} else if (n == Util.chunkLength(list.get(0))) {
				Log.d(TAG, "n == Util.chunkLength(list.get(0))");

				// first list is a perfect match
				///ret = list.shift();
				ret = list.remove(0);
			} else {
				Log.d(TAG, "complex case");

				// complex case.
				// we have enough to cover it, but it spans past the first buffer.
				if (stringMode) {
					Log.d(TAG, "stringMode");

					ret = "";
				} else {
					///ret = new Buffer(n);
					ret = ByteBuffer.allocate(n);
				}

				int c = 0;
				for (int i = 0, l = list.size(); i < l && c < n; i++) {
					Object buf = list.get(0);
					int cpy = Math.min(n - c, Util.chunkLength(buf));

					if (stringMode) {
						///ret += buf.slice(0, cpy);
						String rs = (String)ret;
						rs += (String)(Util.chunkSlice(buf, 0, cpy));
						ret = rs;
						
						Log.d(TAG, "string mode complex buf:"+buf.toString());
						Log.d(TAG, "string mode ret buf:"+ret.toString());
					} else {
						///buf.copy(ret, c, 0, cpy);
						ByteBuffer rb = (ByteBuffer)ret;
						rb.put((ByteBuffer)Util.chunkSlice(buf, 0, cpy));
						///ret = rb;
					}

					///if (cpy < buf.length)
					if (cpy < Util.chunkLength(buf))
						///list[0] = buf.slice(cpy);
						list.set(0, Util.chunkSlice(buf, cpy));
					else
						///list.shift();
						list.remove(0);

					c += cpy;
				}
				
				// flip ByteBuffer
				if (!stringMode) 
					((ByteBuffer)ret).flip();
			}
		}

		return ret;
	}

	private void endReadable(final Readable2 stream) throws Exception {
		final State state = stream._readableState;

		// If we get here before consuming all the bytes, then that is a
		// bug in node.  Should never happen.
		if (state.getLength() > 0)
			throw new Exception("endReadable called on non-empty stream");

		if (!state.isEndEmitted()) {
			state.setEnded(true);
			///TDB...
			///process.nextTick(function() {
			context.nextTick(new NodeContext.nextTickCallback() {

				@Override
				public void onNextTick() throws Exception {
					// Check that we didn't get one last unshift.
					if (!state.isEndEmitted() && state.getLength() == 0) {
						state.setEndEmitted(true);
						stream.readable = false;
						stream.emit("end");
					}
				}
				
			});
		}
	}

	//at this point, the user has presumably seen the 'readable' event,
	//and called read() to consume some data.  that may have triggered
	//in turn another _read(n) call, in which case reading = true if
	//it's in progress.
	//However, if we're not ended, or reading, and the length < hwm,
	//then go ahead and try to read some more preemptively.
	private void maybeReadMore(final Readable2 stream, final State state) throws Exception {
		if (!state.readingMore) {
			state.readingMore = true;
			//TBD...
			///process.nextTick(function() {
			context.nextTick(new NodeContext.nextTickCallback() {

				@Override
				public void onNextTick() throws Exception {
					maybeReadMore_(stream, state);
				}
				
			});
		}
	}
	private void maybeReadMore_(Readable2 stream, State state) throws Exception {
		int len = state.getLength();
		while (!state.isReading() && state.flowing!=TripleState.TRUE && !state.isEnded() &&
				state.getLength() < state.highWaterMark) {
			Log.d(TAG, "maybeReadMore read 0");
			stream.read(0);
			if (len == state.getLength())
				// didn't get any data, stop spinning.
				break;
			else
				len = state.getLength();
		}
		state.readingMore = false;
	}

	//Don't emit readable right away in sync mode, because this can trigger
	//another read() call => stack overflow.  This way, it might trigger
	//a nextTick recursion warning, but that's not so bad.
	private void emitReadable(final Readable2 stream) throws Exception {
		State state = stream._readableState;
		state.needReadable = false;
		if (!state.emittedReadable) {
			Log.d(TAG, "emitReadable " + state.flowing);
			state.emittedReadable = true;
			if (state.sync) {
				//TBD...
				///process.nextTick(function() {
				context.nextTick(new NodeContext.nextTickCallback() {

					@Override
					public void onNextTick() throws Exception {
						emitReadable_(stream);

					}
				});
			} else
				emitReadable_(stream);
		}
	}
	private  void emitReadable_(Readable2 stream) throws Exception {
		Log.d(TAG, "emit readable");
		stream.emit("readable");
		flow(stream);
	}

	private  void flow(Readable2 stream) throws Exception {
		State state = stream._readableState;
		Log.d(TAG, "flow " + state.flowing);
		if (state.flowing==TripleState.TRUE) {
			Object chunk;
			do {
				///var chunk = stream.read();
				chunk = stream.read(-1);
			} while (null != chunk && state.flowing==TripleState.TRUE);
		}
	}

	private  void onEofChunk(Readable2 stream, State state) throws Exception {
		if (state.getDecoder()!=null && !state.isEnded()) {
			///Object chunk = state.decoder.end();
			
			// Reset decoder anyway
			/*
			CharBuffer cbuf = CharBuffer.allocate(1024 * 1024);
			state.getDecoder().flush(cbuf);
			String chunk = cbuf.toString();

			if (chunk!=null && Util.chunkLength(chunk)>0) {
				state.buffer.add(chunk);
				state.length += state.isObjectMode() ? 1 : Util.chunkLength(chunk);
			}
			*/
			state.getDecoder().reset();
		}
		state.setEnded(true);

		// emit 'readable' now to make sure it gets picked up.
		emitReadable(stream);
	}

	private  String chunkInvalid(State state, Object chunk) {
		String er = null;
		/*if (!util.isBuffer(chunk) &&
      !util.isString(chunk) &&
      !util.isNullOrUndefined(chunk) &&
      !state.objectMode) {
    er = new TypeError('Invalid non-string/buffer chunk');
  }*/
		if (!Util.isBuffer(chunk) &&
				!Util.isString(chunk) &&
				!Util.isNullOrUndefined(chunk) &&
				!state.isObjectMode()) 
			er = "Invalid non-string/buffer chunk";
		return er;
	}

	public Writable pipe(final Writable dest, boolean pipeOpts) throws Exception {
		  final Readable2 src = this;
		  final State state = this._readableState;

		  /*switch (state.pipesCount) {
		    case 0:
		      state.pipes = dest;
		      break;
		    case 1:
		      state.pipes = [state.pipes, dest];
		      break;
		    default:
		      state.pipes.push(dest);
		      break;
		  }*/
		  state.pipes.add(dest);
		  state.pipesCount += 1;
		  Log.d(TAG, "pipe count=" + state.pipesCount + "opts=" + pipeOpts);

		  /*
		  boolean doEnd = (!pipeOpts || pipeOpts.end !== false) &&
		              dest !== process.stdout &&
		              dest !== process.stderr;
*/
		  boolean doEnd = pipeOpts;
		  
		  final Listener onend = new Listener() {

			  @Override
			  public void onEvent(Object readable) throws Exception {
				  Log.d(TAG, "onend");
				  dest.end(null, null, null);
			  }

		  };

		  // when the dest drains, it reduces the awaitDrain counter
		  // on the source.  This would be more elegant with a .once()
		  // handler in flow(), but adding and removing repeatedly is
		  // too slow.
		  final Listener ondrain = pipeOnDrain(src);
		  dest.on("drain", ondrain);  
		  
          final Listener ondata = new Listener() {

			@Override
			public void onEvent(Object chunk) throws Exception {
			    Log.d(TAG, "ondata");
			    boolean ret = dest.write(chunk, null, null);
			    if (false == ret) {
			      Log.d(TAG, "false write response, pause " + src._readableState.awaitDrain);
			      src._readableState.awaitDrain++;
			      src.pause();
			    }
			  }
        	  
          };	
          src.on("data", ondata);

          // if the dest has an error, then stop piping into it.
          // however, don't suppress the throwing behavior for this.
          final Listener onerror = new Listener() {

        	  @Override
        	  public void onEvent(Object er) throws Exception {
        		  Log.d(TAG, "onerror " + er);
        		  unpipe(src, (Writable2) dest);
        		  dest.removeListener("error", this);
        		  if (dest.listenerCount("error") == 0)
        			  dest.emit("error", er);
        	  }

          };
		  
		  // This is a brutally ugly hack to make sure that our error handler
		  // is attached before any userland ones.  NEVER DO THIS.
		  /*if (!dest._events || !dest._events.error)
		    dest.on("error", onerror);
		  else if (Array.isArray(dest._events.error))
		    dest._events.error.unshift(onerror);
		  else
		    dest._events.error = [onerror, dest._events.error];
          */
          // TBD...
          dest.addListener("error", onerror, 0);

		  // Both close and finish should trigger unpipe, but only once.
		  final Listener onclose = new Listener() {

        	  @Override
        	  public void onEvent(Object er) throws Exception {
      		    ///dest.removeListener("finish", onfinish);
        		dest.removeListener("finish");
    		    unpipe(src, (Writable2) dest);
    		  }

          };
		  dest.once("close", onclose);
		  
		  final Listener onfinish = new Listener() {

        	  @Override
        	  public void onEvent(Object er) throws Exception {
      		    Log.d(TAG, "onfinish");
    		    dest.removeListener("close", onclose);
    		    unpipe(src, (Writable2) dest);
    		  }

          };
		  dest.once("finish", onfinish);

		  // tell the dest that it's being piped to
		  dest.emit("pipe", src);

		  // start the flow if it hasn't been started already.
		  if (state.flowing!=TripleState.TRUE) {
		    Log.d(TAG, "pipe resume");
		    src.resume();
		  }
		  		  
		  final Listener cleanup = new Listener() {

			  @Override
			  public void onEvent(Object data) throws Exception {
				  Log.d(TAG, "cleanup");

				  // cleanup event handlers once the pipe is broken
				  dest.removeListener("close", onclose);
				  dest.removeListener("finish", onfinish);
				  dest.removeListener("drain", ondrain);
				  dest.removeListener("error", onerror);
				  ///dest.removeListener("unpipe", onunpipe);
				  src.removeListener("end", onend);
				  src.removeListener("end", this);
				  src.removeListener("data", ondata);

				  // if the reader is waiting for a drain event from this
				  // specific writer, then it would cause it to never start
				  // flowing again.
				  // So, if this is awaiting a drain, then we just call it now.
				  // If we don't know, then assume that we are waiting for one.
				  Writable2 wdest = (Writable2)dest;
				  ///if (state.awaitDrain &&
				  ///(!dest._writableState || dest._writableState.needDrain))
				  if (state.awaitDrain>0 && wdest.isNeedDrain())
					  ondrain.onEvent(null);
			  }

		  };
		  
		  final Listener onunpipe = new Listener() {

			  @Override
			  public void onEvent(Object readable) throws Exception {
				  Log.d(TAG, "onunpipe");
				  if (readable.equals(src)) {
					  cleanup.onEvent(null);
				  }
			  }

		  };
		  dest.once("unpipe", onunpipe);
		  
		  final Listener endFn = doEnd ? onend : cleanup;
		  if (state.isEndEmitted())
			  // TBD...
			  ///process.nextTick(endFn);
			  context.nextTick(new NodeContext.nextTickCallback() {

				  @Override
				  public void onNextTick() throws Exception {	
					  endFn.onEvent(null);
				  }
				  
			  });
		  else
			  src.once("end", endFn);
		  
		  return dest;
}

	private  void unpipe(Readable2 src, Writable2 dest) throws Exception {
		Log.d(TAG, "unpipe");
		src.unpipe(dest);
	}

	private  Listener pipeOnDrain(final Readable2 src) {
		return new Listener () {
			@Override
			public void onEvent(Object data) throws Exception {
				State state = src._readableState;

				Log.d(TAG, "pipeOnDrain "+state.awaitDrain);
				if (state.awaitDrain > 0)
					state.awaitDrain--;
				if (state.awaitDrain == 0 && src.listenerCount("data")>0) {
					state.flowing = TripleState.TRUE;
					flow(src);
				}
			}
		};
	}

	@Override
	public Readable unpipe(Writable dest) throws Exception {
		State state = this._readableState;

		Log.d(TAG, "pipesCount "+state.pipesCount);
		
		// if we're not piping anywhere, then do nothing.
		if (state.pipesCount == 0)
			return this;

		// just one destination.  most common case.
		if (state.pipesCount == 1) {
			// passed in one, but it's not the right one.
			if (dest!=null && !state.pipes.contains(dest))
				return this;

			///if (!dest)
			if (dest == null)
				dest = state.pipes.get(0);

			// got a match.
			if (dest != null)
				dest.emit("unpipe", this);

			state.pipes.clear();
			state.pipesCount = 0;
			state.flowing = TripleState.FALSE;

			return this;
		}

		// slow case. multiple pipe destinations.
		if (dest == null) {
			// remove all.
			List<Writable> dests = state.pipes;
			int len = state.pipesCount;

			for (int i = 0; i < len; i++)
				dests.get(i).emit("unpipe", this);

			state.pipes.clear();
			state.pipesCount = 0;
			state.flowing = TripleState.FALSE;

			return this;
		}

		// try to find the right one.
		int i = state.pipes.indexOf(dest);
		if (i == -1)
			return this;

		///state.pipes.splice(i, 1);
		state.pipes.remove(i);
		state.pipesCount -= 1;
		///if (state.pipesCount == 1)
		///  state.pipes = state.pipes[0];

		dest.emit("unpipe", this);

		return this;
	}

	// set up data events if they are asked for
	// Ensure readable listeners eventually get something
	@Override
	public EventEmitter on(final String ev, final Listener fn) throws Exception {
		EventEmitter res = super.on(ev, fn);

		// If listening to data, and it has not explicitly been paused,
		// then call resume to start the flow of data on the next tick.
		// TBD... always do resume ???
		if (ev == "data" && TripleState.FALSE != this._readableState.flowing) {
			this.resume();
		}

		if (ev == "readable" && this.readable) {
			State state = this._readableState;
			if (!state.readableListening) {
				state.readableListening = true;
				state.emittedReadable = false;
				state.needReadable = true;
				if (!state.isReading()) {
					final Readable2 self = this;
					///TBD...
					///process.nextTick(function() {
					context.nextTick(new NodeContext.nextTickCallback() {

						@Override
						public void onNextTick() throws Exception {
							Log.d(TAG, "readable nexttick read 0");
							self.read(0);
						}
						
					});
				} else if (state.getLength() > 0) {
					emitReadable(this);
				}
			}
		}

		return res;
	}

	// pause() and resume() are remnants of the legacy readable stream API
	// If the user uses them, then switch into old mode.
	public Readable resume() throws Exception {
		State state = this._readableState;
		if (state.flowing!=TripleState.TRUE) {
			Log.d(TAG, "resume");
			state.flowing = TripleState.TRUE;
			resume(this, state);
		}
		return this;
	}

	private  void resume(final Readable2 stream, final State state) throws Exception {
		if (!state.isResumeScheduled()) {
			state.setResumeScheduled(true);
			// TBD...
			///process.nextTick(function() {
			context.nextTick(new NodeContext.nextTickCallback() {

				@Override
				public void onNextTick() throws Exception {
					resume_(stream, state);
				}
				
			});
		}
	}

	private  void resume_(Readable2 stream, State state) throws Exception {
		if (!state.isReading()) {
			Log.d(TAG, "resume read 0");
			stream.read(0);
		}

		state.setResumeScheduled(false);

		stream.emit("resume");
		flow(stream);

		if (state.flowing==TripleState.TRUE && !state.isReading())
			stream.read(0);
	}

	public Readable pause() throws Exception {
		Log.d(TAG, "call pause flowing=" + this._readableState.flowing);
		if (TripleState.FALSE != this._readableState.flowing) {
			Log.d(TAG, "pause");
			this._readableState.flowing = TripleState.FALSE;
			this.emit("pause");
		}
		return this;
	};

	public boolean readable() {
		return readable;
	}
	public void readable(boolean readable) {
		this.readable = readable;
	}
	
	// wrap an old-style stream as the async data source.
	// This is *not* part of the readable stream interface.
	// It is an ugly unfortunate mess of history.
	public static Readable2 wrap(final NodeContext ctx, final Readable stream, Options options) throws Exception {		  
		return new WrapReadable2(ctx, options, stream);
	}
	
	// Manually shove something into the read() buffer.
	// This returns true if the highWaterMark has not been hit yet,
	// similar to how Writable.write() returns true if you should
	// write() some more.
	public boolean push(Object chunk, String encoding) throws Exception {
		State state = this._readableState;

		if (Util.isString(chunk) && !state.isObjectMode()) {
			encoding = encoding != null ?  encoding : state.defaultEncoding;
			if (encoding != state.encoding) {
				///chunk = new Buffer(chunk, encoding);
				chunk = ByteBuffer.wrap(((String)chunk).getBytes(encoding));
				encoding = "";
			}
		}

		return readableAddChunk(this, state, chunk, encoding, false);
	}

	// _read(size)
	// abstract method.  to be overridden in specific implementation classes.
	// call cb(er, data) where data is <= n in length.
	// for virtual (non-string, non-buffer) streams, "length" is somewhat
	// arbitrary, and perhaps not very meaningful.
	protected abstract void _read(int size) throws Exception;
	/**
	 * @return the _readableState
	 */
	public State get_readableState() {
		return _readableState;
	}
	
}
