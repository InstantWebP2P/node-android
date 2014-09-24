package com.iwebpp.node.tests;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import android.util.Log;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.HttpParser;
import com.iwebpp.node.HttpParser.http_parser_type;

public final class HttpParserTest 
extends EventEmitter2 {
	private static final String TAG = "HttpParserTest";

	private class DummyParser extends HttpParser {
		private CharsetDecoder decoder = Charset.forName("utf-8").newDecoder();
		
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
			Log.d(TAG, "on_url: "+decoder.decode(url));

			return 0;
		}

		@Override
		protected int on_status(ByteBuffer status) throws Exception {
			Log.d(TAG, "on_status: "+decoder.decode(status));

			return 0;
		}

		@Override
		protected int on_header_field(ByteBuffer field) throws Exception {
			Log.d(TAG, "on_header_field: "+decoder.decode(field));

			return 0;
		}

		@Override
		protected int on_header_value(ByteBuffer vaule) throws Exception {
			Log.d(TAG, "on_header_value: "+decoder.decode(vaule));

			return 0;
		}

		@Override
		protected int on_headers_complete() {
			Log.d(TAG, "on_headers_complete:");
			
			if (getType() == http_parser_type.HTTP_REQUEST)
				Log.d(TAG, "\t\trequest method: "+getMethod().desc());
			else 
				Log.d(TAG, "\t\tresponse status code: "+getStatus_code());
			
			return 0;
		}

		@Override
		protected int on_body(ByteBuffer body) throws Exception {
			Log.d(TAG, "on_body: "+decoder.decode(body));

			return 0;
		}

		@Override
		protected int on_message_complete() {
			Log.d(TAG, "on_message_complete");
			
			return 0;
		}

	}
	
	private boolean testParseRequest(ByteBuffer data) {
		int recved = data.capacity();
		
		DummyParser parser = new DummyParser(http_parser_type.HTTP_REQUEST, data);

        /* Start up / continue the parser.
         * Note we pass recved==0 to signal that EOF has been recieved.
         */
        try {
        	int nparsed = parser.execute(data);

        	if (parser.isUpgrade()) {
        		/* handle new protocol */
        		Log.d(TAG, "testParseRequest: upgrade hand new protocol");
        	} else if (nparsed != recved) {
        		/* Handle error. Usually just close the connection. */
        		Log.d(TAG, "testParseRequest: Handle error. Usually just close the connection.");

        		return false;
        	}
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	private boolean testParseResponse(ByteBuffer data) {
		int recved = data.capacity();
		
		DummyParser parser = new DummyParser(http_parser_type.HTTP_RESPONSE, data);

        /* Start up / continue the parser.
         * Note we pass recved==0 to signal that EOF has been recieved.
         */
        try {
        	int nparsed = parser.execute(data);

        	if (parser.isUpgrade()) {
        		/* handle new protocol */
        		Log.d(TAG, "testParseResponse: upgrade hand new protocol");
        	} else if (nparsed != recved) {
        		/* Handle error. Usually just close the connection. */
        		Log.d(TAG, "testParseResponse: Handle error. Usually just close the connection.");

        		return false;
        	}
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return true;
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				// request messages
				String messages_request[] = {
						// GET empty
						"GET / HTTP/1.1\r\n" +
				         "\r\n"+
				         "GET / HTTP/1.1\r\n" +
				         ///"\r\n"+
				         "GET / HTTP/1.1\r\n" +
				         "\r\n",
				         
				      // GET chain
							"GET / HTTP/1.1\r\n" +
					         "Host: 192.188.1.103:6288\r\n"+
					         "Connection: keep-alive\r\n" +
					         "\r\n"+
					         
					         "GET / HTTP/1.1\r\n" +
					         "Host: 192.188.1.103:6288\r\n"+
					         "Connection: keep-alive\r\n" +
					         "\r\n"+
					         
					         "GET / HTTP/1.1\r\n" +
					         "Host: 192.188.1.103:6288\r\n"+
					         "Connection: keep-alive\r\n" +
					         "\r\n"+
					         
					         "GET / HTTP/1.1\r\n" +
					         "Host: 192.188.1.103:6288\r\n"+
					         "Connection: keep-alive\r\n" +
					         "\r\n",
					         
						// CURL_GET 0
						"GET /test HTTP/1.1\r\n" +
						"User-Agent: curl/7.18.0 (i486-pc-linux-gnu) libcurl/7.18.0 OpenSSL/0.9.8g zlib/1.2.3.3 libidn/1.1\r\n" +
						"Host: 0.0.0.0=5000\r\n" +
						"Accept: */*\r\n" +
						"\r\n" ,

						// FIREFOX_GET 1
						"GET /favicon.ico HTTP/1.1\r\n" +
				         "Host: 0.0.0.0=5000\r\n" +
				         "User-Agent: Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9) Gecko/2008061015 Firefox/3.0\r\n" +
				         "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\r\n" +
				         "Accept-Language: en-us,en;q=0.5\r\n" +
				         "Accept-Encoding: gzip,deflate\r\n" +
				         "Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7\r\n" +
				         "Keep-Alive: 300\r\n" +
				         "Connection: keep-alive\r\n" +
				         "\r\n" ,
						
						// DUMBFUCK 2
						"GET /dumbfuck HTTP/1.1\r\n" +
				         "aaaaaaaaaaaaa:++++++++++\r\n" +
				         "\r\n" ,
						
						// FRAGMENT_IN_URI 3
						"GET /forums/1/topics/2375?page=1#posts-17408 HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// GET_NO_HEADERS_NO_BODY 4
						"GET /get_no_headers_no_body/world HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// GET_ONE_HEADER_NO_BODY 5
						"GET /get_one_header_no_body HTTP/1.1\r\n" +
				         "Accept: */*\r\n" +
				         "\r\n" ,
						
						// GET_FUNKY_CONTENT_LENGTH 6
						"GET /get_funky_content_length_body_hello HTTP/1.0\r\n" +
				         "conTENT-Length: 5\r\n" +
				         "\r\n" +
				         "HELLO",
				         
						// POST_IDENTITY_BODY_WORLD 7
						"POST /post_identity_body_world?q=search#hey HTTP/1.1\r\n" +
				         "Accept: */*\r\n" +
				         "Transfer-Encoding: identity\r\n" +
				         "Content-Length: 5\r\n" +
				         "\r\n" +
				         "World",
						
						// POST_CHUNKED_ALL_YOUR_BASE 8
						"POST /post_chunked_all_your_base HTTP/1.1\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "1e\r\nall your base are belong to us\r\n" +
				         "0\r\n" +
				         "\r\n" ,
						
						// TWO_CHUNKS_MULT_ZERO_END 9
						"POST /two_chunks_mult_zero_end HTTP/1.1\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "5\r\nhello\r\n" +
				         "6\r\n world\r\n" +
				         "000\r\n" +
				         "\r\n" ,
						
						// CHUNKED_W_TRAILING_HEADERS 10
						"POST /chunked_w_trailing_headers HTTP/1.1\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "5\r\nhello\r\n" +
				         "6\r\n world\r\n" +
				         "0\r\n" +
				         "Vary: *\r\n" +
				         "Content-Type: text/plain\r\n" +
				         "\r\n" ,
						
						// CHUNKED_W_BULLSHIT_AFTER_LENGTH 11
						"POST /chunked_w_bullshit_after_length HTTP/1.1\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "5; ihatew3;whatthefuck=aretheseparametersfor\r\nhello\r\n" +
				         "6; blahblah; blah\r\n world\r\n" +
				         "0\r\n" +
				         "\r\n" ,
						
						// WITH_QUOTES 12
						"GET /with_\"stupid\"_quotes?foo=\"bar\" HTTP/1.1\r\n\r\n" ,
						
						// APACHEBENCH_GET 13
						"GET /test HTTP/1.0\r\n" +
				         "Host: 0.0.0.0:5000\r\n" +
				         "User-Agent: ApacheBench/2.3\r\n" +
				         "Accept: */*\r\n\r\n" ,
						
						// QUERY_URL_WITH_QUESTION_MARK_GET 14
						"GET /test.cgi?foo=bar?baz HTTP/1.1\r\n\r\n" ,
						
						// PREFIX_NEWLINE_GET 15
						"\r\nGET /test HTTP/1.1\r\n\r\n" ,
						
						// UPGRADE_REQUEST 16
						"GET /demo HTTP/1.1\r\n" +
				         "Host: example.com\r\n" +
				         "Connection: Upgrade\r\n" +
				         "Sec-WebSocket-Key2: 12998 5 Y3 1  .P00\r\n" +
				         "Sec-WebSocket-Protocol: sample\r\n" +
				         "Upgrade: WebSocket\r\n" +
				         "Sec-WebSocket-Key1: 4 @1  46546xW%0l 1 5\r\n" +
				         "Origin: Http://example.com\r\n" +
				         "\r\n" +
				         "Hot diggity dogg",
						
						// CONNECT_REQUEST 17
						"CONNECT 0-home0.netscape.com:443 HTTP/1.0\r\n" +
				         "User-agent: Mozilla/1.1N\r\n" +
				         "Proxy-authorization: basic aGVsbG86d29ybGQ=\r\n" +
				         "\r\n" +
				         "some data\r\n" +
				         "and yet even more data",
						
						// REPORT_REQ 18
						"REPORT /test HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// NO_HTTP_VERSION 19
						"GET /\r\n" +
				         "\r\n" ,
						
						// MSEARCH_REQ 20
						"M-SEARCH * HTTP/1.1\r\n" +
				         "HOST: 239.255.255.250:1900\r\n" +
				         "MAN: \"ssdp:discover\"\r\n" +
				         "ST: \"ssdp:all\"\r\n" +
				         "\r\n" ,
						
						// LINE_FOLDING_IN_HEADER 21
						"GET / HTTP/1.1\r\n" +
				         "Line1:   abc\r\n" +
				         "\tdef\r\n" +
				         " ghi\r\n" +
				         "\t\tjkl\r\n" +
				         "  mno \r\n" +
				         "\t \tqrs\r\n" +
				         "Line2: \t line2\t\r\n" +
				         "Line3:\r\n" +
				         " line3\r\n" +
				         "Line4: \r\n" +
				         " \r\n" +
				         "Connection:\r\n" +
				         " close\r\n" +
				         "\r\n" ,
						
						// QUERY_TERMINATED_HOST 22
						"GET Http://hypnotoad.org?hail=all HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// QUERY_TERMINATED_HOSTPORT 23
						"GET Http://hypnotoad.org:1234?hail=all HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// SPACE_TERMINATED_HOSTPORT 24
						"GET Http://hypnotoad.org:1234 HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// PATCH_REQ 25
						"PATCH /file.txt HTTP/1.1\r\n" +
				         "Host: www.example.com\r\n" +
				         "Content-Type: application/example\r\n" +
				         "If-Match: \"e0023aa4e\"\r\n" +
				         "Content-Length: 10\r\n" +
				         "\r\n" +
				         "cccccccccc",
						
						// CONNECT_CAPS_REQUEST 26
						 "CONNECT HOME0.NETSCAPE.COM:443 HTTP/1.0\r\n" +
				         "User-agent: Mozilla/1.1N\r\n" +
				         "Proxy-authorization: basic aGVsbG86d29ybGQ=\r\n" +
				         "\r\n" ,
						
						// EAT_TRAILING_CRLF_NO_CONNECTION_CLOSE 27
						"POST / HTTP/1.1\r\n" +
				         "Host: www.example.com\r\n" +
				         "Content-Type: application/x-www-form-urlencoded\r\n" +
				         "Content-Length: 4\r\n" +
				         "\r\n" +
				         "q=42\r\n" ,
						
						// EAT_TRAILING_CRLF_WITH_CONNECTION_CLOSE 28
						"POST / HTTP/1.1\r\n" +
				         "Host: www.example.com\r\n" +
				         "Content-Type: application/x-www-form-urlencoded\r\n" +
				         "Content-Length: 4\r\n" +
				         "Connection: close\r\n" +
				         "\r\n" +
				         "q=42\r\n" ,
						
						// PURGE_REQ 29
						"PURGE /file.txt HTTP/1.1\r\n" +
				         "Host: www.example.com\r\n" +
				         "\r\n" ,
						
						// SEARCH_REQ 30
						"SEARCH / HTTP/1.1\r\n" +
				         "Host: www.example.com\r\n" +
				         "\r\n" ,
						
						// PROXY_WITH_BASIC_AUTH 31
						"GET Http://a%12:b!&*$@hypnotoad.org:1234/toto HTTP/1.1\r\n" +
				         "\r\n" ,
						
						// LINE_FOLDING_IN_HEADER_WITH_LF 32
						"GET / HTTP/1.1\n" +
				         "Line1:   abc\n" +
				         "\tdef\n" +
				         " ghi\n" +
				         "\t\tjkl\n" +
				         "  mno \n" +
				         "\t \tqrs\n" +
				         "Line2: \t line2\t\n" +
				         "Line3:\n" +
				         " line3\n" +
				         "Line4: \n" +
				         " \n" +
				         "Connection:\n" +
				         " close\n" +
				         "\n"
						
						
				};

				// response messages
				String messages_response[] = {
						// GOOGLE_301 0
						"HTTP/1.1 301 Moved Permanently\r\n" +
				         "Location: Http://www.google.com/\r\n" +
				         "Content-Type: text/html; charset=UTF-8\r\n" +
				         "Date: Sun, 26 Apr 2009 11:11:49 GMT\r\n" +
				         "Expires: Tue, 26 May 2009 11:11:49 GMT\r\n" +
				         "X-$PrototypeBI-Version: 1.6.0.3\r\n" + /* $ char in header field */
				         "Cache-Control: public, max-age=2592000\r\n" +
				         "Server: gws\r\n" +
				         "Content-Length:  219  \r\n" +
				         "\r\n" +
				         "<HTML><HEAD><meta Http-equiv=\"content-type\" content=\"text/html;charset=utf-8\">\n" +
				         "<TITLE>301 Moved</TITLE></HEAD><BODY>\n" +
				         "<H1>301 Moved</H1>\n" +
				         "The document has moved\n" +
				         "<A HREF=\"Http://www.google.com/\">here</A>.\r\n" +
				         "</BODY></HTML>\r\n" ,
						
						// NO_CONTENT_LENGTH_RESPONSE 1
						"HTTP/1.1 200 OK\r\n" +
				         "Date: Tue, 04 Aug 2009 07:59:32 GMT\r\n" +
				         "Server: Apache\r\n" +
				         "X-Powered-By: Servlet/2.5 JSP/2.1\r\n" +
				         "Content-Type: text/xml; charset=utf-8\r\n" +
				         "Connection: close\r\n" +
				         "\r\n" +
				         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				         "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"Http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				         "  <SOAP-ENV:Body>\n" +
				         "    <SOAP-ENV:Fault>\n" +
				         "       <faultcode>SOAP-ENV:Client</faultcode>\n" +
				         "       <faultstring>Client Error</faultstring>\n" +
				         "    </SOAP-ENV:Fault>\n" +
				         "  </SOAP-ENV:Body>\n" +
				         "</SOAP-ENV:Envelope>",
						
						// NO_HEADERS_NO_BODY_404 2
						 "HTTP/1.1 404 Not Found\r\n\r\n" ,
						
						// NO_REASON_PHRASE 3
						"HTTP/1.1 301\r\n\r\n" ,
						
						// TRAILING_SPACE_ON_CHUNKED_BODY 4
						"HTTP/1.1 200 OK\r\n" +
				         "Content-Type: text/plain\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "25  \r\n" +
				         "This is the data in the first chunk\r\n" +
				         "\r\n" +
				         "1C\r\n" +
				         "and this is the second one\r\n" +
				         "\r\n" +
				         "0  \r\n" +
				         "\r\n" ,
				         
						// NO_CARRIAGE_RET 5
						"HTTP/1.1 200 OK\n" +
				         "Content-Type: text/html; charset=utf-8\n" +
				         "Connection: close\n" +
				         "\n" +
				         "these headers are from Http://news.ycombinator.com/",
				         
						// PROXY_CONNECTION 6
						"HTTP/1.1 200 OK\r\n" +
				         "Content-Type: text/html; charset=UTF-8\r\n" +
				         "Content-Length: 11\r\n" +
				         "Proxy-Connection: close\r\n" +
				         "Date: Thu, 31 Dec 2009 20:55:48 +0000\r\n" +
				         "\r\n" +
				         "hello world",
				         
						// UNDERSTORE_HEADER_KEY 7
						"HTTP/1.1 200 OK\r\n" +
				         "Server: DCLK-AdSvr\r\n" +
				         "Content-Type: text/xml\r\n" +
				         "Content-Length: 0\r\n" +
				         "DCLK_imp: v7;x;114750856;0-0;0;17820020;0/0;21603567/21621457/1;;~okv=;dcmt=text/xml;;~cs=o\r\n\r\n" ,
				         
						//  BONJOUR_MADAME_FR 8
						"HTTP/1.0 301 Moved Permanently\r\n" +
				         "Date: Thu, 03 Jun 2010 09:56:32 GMT\r\n" +
				         "Server: Apache/2.2.3 (Red Hat)\r\n" +
				         "Cache-Control: public\r\n" +
				         "Pragma: \r\n" +
				         "Location: Http://www.bonjourmadame.fr/\r\n" +
				         "Vary: Accept-Encoding\r\n" +
				         "Content-Length: 0\r\n" +
				         "Content-Type: text/html; charset=UTF-8\r\n" +
				         "Connection: keep-alive\r\n" +
				         "\r\n" ,
				         
						// RES_FIELD_UNDERSCORE 9
						"HTTP/1.1 200 OK\r\n" +
				         "Date: Tue, 28 Sep 2010 01:14:13 GMT\r\n" +
				         "Server: Apache\r\n" +
				         "Cache-Control: no-cache, must-revalidate\r\n" +
				         "Expires: Mon, 26 Jul 1997 05:00:00 GMT\r\n" +
				         ".et-Cookie: PlaxoCS=1274804622353690521; path=/; domain=.plaxo.com\r\n" +
				         "Vary: Accept-Encoding\r\n" +
				         "_eep-Alive: timeout=45\r\n" + /* semantic value ignored */
				         "_onnection: Keep-Alive\r\n" + /* semantic value ignored */
				         "Transfer-Encoding: chunked\r\n" +
				         "Content-Type: text/html\r\n" +
				         "Connection: close\r\n" +
				         "\r\n" +
				         "0\r\n\r\n" ,
				         
						//  NON_ASCII_IN_STATUS_LINE 10
						"HTTP/1.1 500 Oriëntatieprobleem\r\n" +
				         "Date: Fri, 5 Nov 2010 23:07:12 GMT+2\r\n" +
				         "Content-Length: 0\r\n" +
				         "Connection: close\r\n" +
				         "\r\n" ,
				         
						// HTTP_VERSION_0_9 11
						"HTTP/0.9 200 OK\r\n" +
				         "\r\n" ,
				         
						//NO_CONTENT_LENGTH_NO_TRANSFER_ENCODING_RESPONSE 12
						"HTTP/1.1 200 OK\r\n" +
				         "Content-Type: text/plain\r\n" +
				         "\r\n" +
				         "hello world",
				         
						// NO_BODY_HTTP10_KA_200 13
						"HTTP/1.0 200 OK\r\n" +
				         "Connection: keep-alive\r\n" +
				         "\r\n" ,
				         
						// NO_BODY_HTTP10_KA_204 14
						"HTTP/1.0 204 No content\r\n" +
				         "Connection: keep-alive\r\n" +
				         "\r\n" ,
				         
						// NO_BODY_HTTP11_KA_200 15
						"HTTP/1.1 200 OK\r\n" +
				         "\r\n" ,
				         
						// NO_BODY_HTTP11_KA_204 16
						"HTTP/1.1 204 No content\r\n" +
				         "\r\n" ,
				         
						// NO_BODY_HTTP11_NOKA_204 17
						"HTTP/1.1 204 No content\r\n" +
				         "Connection: close\r\n" +
				         "\r\n" ,
				         
						// NO_BODY_HTTP11_KA_CHUNKED_200 18
						"HTTP/1.1 200 OK\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "0\r\n" +
				         "\r\n" ,
				         
						//  AMAZON_COM 20
						"HTTP/1.1 301 MovedPermanently\r\n" +
				         "Date: Wed, 15 May 2013 17:06:33 GMT\r\n" +
				         "Server: Server\r\n" +
				         "x-amz-id-1: 0GPHKXSJQ826RK7GZEB2\r\n" +
				         "p3p: policyref=\"Http://www.amazon.com/w3c/p3p.xml\",CP=\"CAO DSP LAW CUR ADM IVAo IVDo CONo OTPo OUR DELi PUBi OTRi BUS PHY ONL UNI PUR FIN COM NAV INT DEM CNT STA HEA PRE LOC GOV OTC \"\r\n" +
				         "x-amz-id-2: STN69VZxIFSz9YJLbz1GDbxpbjG6Qjmmq5E3DxRhOUw+Et0p4hr7c/Q8qNcx4oAD\r\n" +
				         "Location: Http://www.amazon.com/Dan-Brown/e/B000AP9DSU/ref=s9_pop_gw_al1?_encoding=UTF8&refinementId=618073011&pf_rd_m=ATVPDKIKX0DER&pf_rd_s=center-2&pf_rd_r=0SHYY5BZXN3KR20BNFAY&pf_rd_t=101&pf_rd_p=1263340922&pf_rd_i=507846\r\n" +
				         "Vary: Accept-Encoding,User-Agent\r\n" +
				         "Content-Type: text/html; charset=ISO-8859-1\r\n" +
				         "Transfer-Encoding: chunked\r\n" +
				         "\r\n" +
				         "1\r\n" +
				         "\n\r\n" +
				         "0\r\n" +
				         "\r\n" ,
				         
						// EMPTY_REASON_PHRASE_AFTER_SPACE 20
				         "HTTP/1.1 200 \r\n" +
				         "\r\n"
						
				};
				
				try {
					// parse request
					for (int i = 0; i < messages_request.length; i ++) 
					{
						Log.d(TAG, "\n\n\n\tOn request message " + i);

						if (!testParseRequest(ByteBuffer.wrap(messages_request[i].getBytes("utf-8"))))
							Log.d(TAG, "\n\n\t!!!Parse request failed on "+messages_request[i]);
					}
					
					// parse response
					for (int i = 0; i < messages_response.length; i ++)
					{
						Log.d(TAG, "\n\n\n\tOn response message " + i);
						
						if (!testParseResponse(ByteBuffer.wrap(messages_response[i].getBytes("utf-8"))))
							Log.d(TAG, "\n\n\t!!!Parse response failed on "+messages_response[i]);
					}
					
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
			}
		})).start();

	}
}
