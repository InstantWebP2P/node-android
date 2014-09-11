package com.iwebpp.node.tests;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import android.util.Log;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.HttpParser;
import com.iwebpp.node.HttpParser.http_parser_type;

public final class HttpParserTest extends EventEmitter2 {
	private static final String TAG = "HttpParserTest";

	private class DummyParser extends HttpParser {

		protected DummyParser(http_parser_type type, Object data) {
			super(type, data);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected int on_message_begin() {
			Log.d(TAG, "on_message_begin");

			return 0;
		}

		@Override
		protected int on_url(ByteBuffer url) throws Exception {
			Log.d(TAG, "on_url: "+new String(url.array(), "utf-8"));

			return 0;
		}

		@Override
		protected int on_status(ByteBuffer status) throws Exception {
			Log.d(TAG, "on_status: "+new String(status.array(), "utf-8"));

			return 0;
		}

		@Override
		protected int on_header_field(ByteBuffer field) throws Exception {
			Log.d(TAG, "on_header_field: "+new String(field.array(), "utf-8"));

			return 0;
		}

		@Override
		protected int on_header_value(ByteBuffer vaule) throws Exception {
			Log.d(TAG, "on_header_value: "+new String(vaule.array(), "utf-8"));

			return 0;
		}

		@Override
		protected int on_headers_complete() {
			Log.d(TAG, "on_headers_complete");
			
			return 0;
		}

		@Override
		protected int on_body(ByteBuffer body) throws Exception {
			Log.d(TAG, "on_body: "+new String(body.array(), "utf-8"));

			return 0;
		}

		@Override
		protected int on_message_complete() {
			Log.d(TAG, "on_message_complete");
			
			return 0;
		}

	}
	
	private boolean testParseRequestHeader(ByteBuffer data) {
		int recved = data.capacity();
		
		DummyParser parser = new DummyParser(http_parser_type.HTTP_REQUEST, data);

        /* Start up / continue the parser.
         * Note we pass recved==0 to signal that EOF has been recieved.
         */
        try {
			Log.d(TAG, "http_parser_execute bf");

        	int nparsed = parser.http_parser_execute(data);

			Log.d(TAG, "http_parser_execute af");

        	if (parser.isUpgrade()) {
        		/* handle new protocol */
        		Log.d(TAG, "testParseRequestHeader: upgrade hand new protocol");
        	} else if (nparsed != recved) {
        		/* Handle error. Usually just close the connection. */
        		Log.d(TAG, "testParseRequestHeader: Handle error. Usually just close the connection.");

        		return false;
        	}
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}

	private boolean testParseRequestBody(ByteBuffer data) {


		return true;
	}
	
	private boolean testParseResponseHeader(ByteBuffer data) {


		return true;
	}

	private boolean testParseResponseBody(ByteBuffer data) {


		return true;
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				// parse request
				String trh = "GET /test HTTP/1.1\r\n" +
						"User-Agent: curl/7.18.0 (i486-pc-linux-gnu) libcurl/7.18.0 OpenSSL/0.9.8g zlib/1.2.3.3 libidn/1.1\r\n" +
						"Host: 0.0.0.0=5000\r\n" +
						"Accept: */*\r\n" +
						"\r\n";

				try {
					testParseRequestHeader(ByteBuffer.wrap(trh.getBytes("utf-8")));

					///testParseRequestBody(null);

					// parse response
					///testParseResponseHeader(null);
					///testParseResponseBody(null);

				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		})).start();

	}
}
