package com.iwebpp.node.net;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;

import com.iwebpp.node.TCP;
import com.iwebpp.node.TCP.Socket;
import com.iwebpp.node.net.HttpParser.http_errno;
import com.iwebpp.node.net.HttpParser.http_method;
import com.iwebpp.node.net.HttpParser.http_parser_type;

public abstract class IncomingParser 
extends HttpParser {
	private final static String TAG = "IncomingParser";

	protected TCP.Socket socket;
	private IncomingMessage incoming;
	protected CharsetDecoder decoder;

	private String [] fields_;///[32];  // header fields
	private String [] values_;///[32];  // header values
	private String url_;
	private String status_message_;
	private int num_fields_;
	private int num_values_;
	private boolean have_flushed_;
	private boolean got_exception_;
	private ByteBuffer current_buffer_;

	private int maxHeaderPairs;

	private List<String> _headers;

	private String _url;

	protected IncomingParser(http_parser_type type, TCP.Socket socket) {
		super(type, socket);
		// TODO Auto-generated constructor stub
		this.socket = socket;
		this._headers = new ArrayList<String>();
		this.decoder = Charset.forName("utf-8").newDecoder();

		this.fields_ = new String[32];
		this.values_ = new String[32];
		this.url_ = "";
		this._url = "";
		this.status_message_ = "";
		this.num_fields_ = this.num_values_ = 0;
		this.have_flushed_ = this.got_exception_ = false;

		this.current_buffer_ = null;
	}
	private IncomingParser(){super(null, null);}


	private void Init(http_parser_type type) {
		super.reset(type);

		this._headers.clear();

		url_ = "";
		_url = "";
		status_message_ = "";
		num_fields_ = 0;
		num_values_ = 0;
		have_flushed_ = false;
		got_exception_ = false;
	}

	// spill headers and request path to JS land
	private void Flush() {
		parserOnHeaders(CreateHeaders(), url_);

		///if (r.IsEmpty())
		///	got_exception_ = true;

		url_ = "";
		have_flushed_ = true;
	}

	private List<String> CreateHeaders() {
		// num_values_ is either -1 or the entry # of the last header
		// so num_values_ == 0 means there's a single header
		List<String> headers = new ArrayList<String>();

		for (int i = 0; i < this.num_values_; i ++) {
			headers.add(this.fields_[i]);
			headers.add(this.values_[i]);
		}

		return headers;
	}

	public void Pause(boolean should_pause) {
		pause(should_pause);
	}

	public void Reinitialize(http_parser_type type) {
		Init(type);
	}

	public int Finish() throws Exception {
		got_exception_ = false;

		int rv = execute(null);

		if (got_exception_)
			return 0;

		if (rv != 0) {
			http_errno err = HTTP_PARSER_ERRNO();

			throw new Exception(err.desc());
		}

		return rv;
	}

	// var bytesParsed = parser->execute(buffer);
	public int Execute(ByteBuffer buffer_obj) throws Exception {
		int buffer_len = buffer_obj.capacity();

		// This is a hack to get the current_buffer to the callbacks with the least
		// amount of overhead. Nothing else will run while http_parser_execute()
		// runs, therefore this pointer can be set and used for the execution.
		current_buffer_ = buffer_obj;
		got_exception_ = false;

		int nparsed = execute(current_buffer_);

		// Unassign the 'buffer_' variable
		current_buffer_.clear();

		// If there was an exception in one of the callbacks
		if (got_exception_)
			return 0;

		// If there was a parse error in one of the callbacks
		// TODO(bnoordhuis) What if there is an error on EOF?
		if (!isUpgrade() && nparsed != buffer_len) {
			http_errno err = HTTP_PARSER_ERRNO();

			throw new Exception(err.desc());
		}

		return nparsed;
	}

	// Only called in the slow case where slow means
	// that the request headers were either fragmented
	// across multiple TCP packets or too large to be
	// processed in a single run. This method is also
	// called to process trailing HTTP headers.
	private void parserOnHeaders(List<String> headers, String url) {
		// Once we exceeded headers limit - stop collecting them
		if (this.maxHeaderPairs <= 0 ||
				this._headers.size() < this.maxHeaderPairs) {
			///this._headers = this._headers.concat(headers);
			this._headers.addAll(headers);
		}
		this._url += url != null ? url : "";
	}

	// info.headers and info.url are set only if .onHeaders()
	// has not been called for this request.
	//
	// info.url is not set for response parsers but that's not
	// applicable here since all our parsers are request parsers.
	///function parserOnHeadersComplete(info) {
	private boolean parserOnHeadersComplete(parseInfo info) throws Exception {
		///debug('parserOnHeadersComplete', info);
		Log.d(TAG, "parserOnHeadersComplete "+info);

		///var parser = this;
		List<String> headers = info.headers;
		String url = info.url;

		if (null == headers || headers.isEmpty()) {
			headers = _headers;
			_headers.clear();
		}

		if (null==url || ""==url) {
			url = _url;
			_url = "";
		}

		/*parser.incoming = new IncomingMessage(parser.socket);
	  parser.incoming.httpVersionMajor = info.versionMajor;
	  parser.incoming.httpVersionMinor = info.versionMinor;
	  parser.incoming.httpVersion = info.versionMajor + '.' + info.versionMinor;
	  parser.incoming.url = url;
		 */
		// TBD...
		this.incoming = new IncomingMessage(null, null, (TCP.Socket)super.data);

		///var n = headers.length;
		int n = headers.size();

		// If parser.maxHeaderPairs <= 0 - assume that there're no limit
		if (maxHeaderPairs > 0) {
			n = Math.min(n, maxHeaderPairs);
		}

		incoming._addHeaderLines(headers, n);

		if (super.getType() == http_parser_type.HTTP_REQUEST/*isNumber(info.method)*/) {
			// server only
			incoming.setMethod(info.method.desc()) ;
		} else {
			// client only
			incoming.setStatusCode(info.statusCode);
			incoming.setStatusMessage(info.statusMessage);
		}

		incoming.setUpgrade(info.upgrade);

		boolean skipBody = false; // response to HEAD or CONNECT

		if (!info.upgrade) {
			// For upgraded connections and CONNECT method request,
			// we'll emit this after parser.execute
			// so that we can capture the first part of the new protocol
			skipBody = onIncoming(incoming, info.shouldKeepAlive);
		}

		return skipBody;
	}
	// POJO bean
	private class parseInfo {

		public boolean shouldKeepAlive;
		public boolean upgrade;
		public http_method method;
		public String url;
		public List<String> headers;
		public int statusCode;
		public String statusMessage;
		public String versionMajor;
		public String versionMinor;
	}

	protected abstract boolean onIncoming(IncomingMessage incoming, boolean shouldKeepAlive) throws Exception;


	// XXX This is a mess.
	// TODO: http.Parser should be a Writable emits request/response events.
	///function parserOnBody(b, start, len) {
	private void parserOnBody(ByteBuffer b) throws Exception {
		IncomingParser parser = this;
		IncomingMessage stream = parser.incoming;

		// if the stream has already been removed, then drop it.
		if (null==stream)
			return;

		Socket socket = stream.socket();

		int len = b == null ? 0 : b.capacity();

		// pretend this was the result of a stream._read call.
		if (len > 0 && !stream.is_dumped()) {
			///var slice = b.slice(start, start + len);
			boolean ret = stream.push(b, null);
			if (!ret)
				IncomingMessage.readStop(socket);
		}
	}

	///function parserOnMessageComplete() {
	private void parserOnMessageComplete() throws Exception {
		IncomingParser parser = this;
		IncomingMessage stream = parser.incoming;

		if (stream!=null) {
			stream.setComplete(true);
			// Emit any trailing headers.
			List<String> headers = parser._headers;
			if (headers!=null && !headers.isEmpty()) {
				incoming._addHeaderLines(headers, headers.size());
				_headers.clear();
				_url = "";
			}

			if (!stream.isUpgrade())
				// For upgraded connections, also emit this after parser.execute
				stream.push(null, null);
		}

		if (stream!=null && 0<incoming.get_pendings().size()) {
			// For emit end event
			stream.push(null, null);
		}

		// force to read the next incoming message
		IncomingMessage.readStart(parser.socket);
	}

	@Override
	protected int on_message_begin() throws Exception {
		num_fields_ = num_values_ = 0;
		url_ = "";
		status_message_ = "";
		return 0;
	}

	@Override
	protected int on_url(ByteBuffer url) throws Exception {
		url_ = decoder.decode(url).toString();
		return 0;
	}

	@Override
	protected int on_status(ByteBuffer status) throws Exception {
		status_message_ = decoder.decode(status).toString();
		return 0;
	}

	@Override
	protected int on_header_field(ByteBuffer field) throws Exception {
		if (num_fields_ == num_values_) {
			// start of new field name
			num_fields_++;
			///if (num_fields_ == ARRAY_SIZE(fields_)) {
			if (num_fields_ == fields_.length) {
				// ran out of space - flush to javascript land
				Flush();
				num_fields_ = 1;
				num_values_ = 0;
			}
			fields_[num_fields_ - 1] = "";
		}

		///assert(num_fields_ < static_cast<int>(ARRAY_SIZE(fields_)));
		assert(num_fields_ < fields_.length);
		assert(num_fields_ == num_values_ + 1);

		fields_[num_fields_ - 1] = decoder.decode(field).toString();

		return 0;
	}

	@Override
	protected int on_header_value(ByteBuffer vaule) throws Exception {
		if (num_values_ != num_fields_) {
			// start of new header value
			num_values_++;
			values_[num_values_ - 1] = "";
		}

		assert(num_values_ < values_.length);
		assert(num_values_ == num_fields_);

		values_[num_values_ - 1] = decoder.decode(vaule).toString();

		return 0;
	}

	@Override
	protected int on_headers_complete() throws Exception {
		///Local<Object> message_info = Object::New(env()->isolate());
		parseInfo message_info = new parseInfo();

		if (have_flushed_) {
			// Slow case, flush remaining headers.
			Flush();
		} else {
			// Fast case, pass headers and URL to JS land. 
			message_info.headers = CreateHeaders();
			if (getType() == http_parser_type.HTTP_REQUEST)
				message_info.url = url_;
		}
		num_fields_ = num_values_ = 0;

		// METHOD
		if (getType() == http_parser_type.HTTP_REQUEST) {
			message_info.method = getMethod();
		}

		// STATUS
		if (getType() == http_parser_type.HTTP_RESPONSE) {		      
			message_info.statusCode = getStatus_code();
			message_info.statusMessage = status_message_;
		}

		// VERSION
		message_info.versionMajor = ""+super.getHttp_major();
		message_info.versionMinor = ""+super.getHttp_minor();
		message_info.shouldKeepAlive = super.http_should_keep_alive();
		message_info.upgrade = super.isUpgrade();

		return parserOnHeadersComplete(message_info) ? 1 : 0;
	}

	@Override
	protected int on_body(ByteBuffer body) throws Exception {
		parserOnBody(body);

		return 0;
	}

	@Override
	protected int on_message_complete() throws Exception {
		if (num_fields_ > 0)
			Flush();  // Flush trailing HTTP headers.

		parserOnMessageComplete();

		return 0;
	}

}
