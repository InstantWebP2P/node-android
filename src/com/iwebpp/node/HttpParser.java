package com.iwebpp.node;

import java.nio.ByteBuffer;

public abstract class HttpParser {

	protected HttpParser(http_parser_type type, Object data) {
        this.data = data;
        this.type = type;
        this.http_errno = http_errno.HPE_OK;
        this.state = (type == http_parser_type.HTTP_REQUEST ? state.s_start_req : 
        	         (type == http_parser_type.HTTP_RESPONSE ? state.s_start_res : state.s_start_req_or_res));
	}
	@SuppressWarnings("unused")
	private HttpParser() {this.data = null;}

	public static enum http_parser_type {
		HTTP_REQUEST, HTTP_RESPONSE, HTTP_BOTH;
	}
	
	private static enum state {
	  	  s_dead (1) /* important that this is > 0 */

		, s_start_req_or_res
		, s_res_or_resp_H
		, s_start_res
		, s_res_H
		, s_res_HT
		, s_res_HTT
		, s_res_HTTP
		, s_res_first_http_major
		, s_res_http_major
		, s_res_first_http_minor
		, s_res_http_minor
		, s_res_first_status_code
		, s_res_status_code
		, s_res_status_start
		, s_res_status
		, s_res_line_almost_done

		, s_start_req

		, s_req_method
		, s_req_spaces_before_url
		, s_req_schema
		, s_req_schema_slash
		, s_req_schema_slash_slash
		, s_req_server_start
		, s_req_server
		, s_req_server_with_at
		, s_req_path
		, s_req_query_string_start
		, s_req_query_string
		, s_req_fragment_start
		, s_req_fragment
		, s_req_http_start
		, s_req_http_H
		, s_req_http_HT
		, s_req_http_HTT
		, s_req_http_HTTP
		, s_req_first_http_major
		, s_req_http_major
		, s_req_first_http_minor
		, s_req_http_minor
		, s_req_line_almost_done

		, s_header_field_start
		, s_header_field
		, s_header_value_discard_ws
		, s_header_value_discard_ws_almost_done
		, s_header_value_discard_lws
		, s_header_value_start
		, s_header_value
		, s_header_value_lws

		, s_header_almost_done

		, s_chunk_size_start
		, s_chunk_size
		, s_chunk_parameters
		, s_chunk_size_almost_done

		, s_headers_almost_done
		, s_headers_done

		/* Important: 's_headers_done' must be the last 'header' state. All
		 * states beyond this must be 'body' states. It is used for overflow
		 * checking. See the PARSING_HEADER() macro.
		 */

		, s_chunk_data
		, s_chunk_data_almost_done
		, s_chunk_data_done

		, s_body_identity
		, s_body_identity_eof

		, s_message_done;
		
		private state(int value) {}
		private state() {}
	}
	
	private static enum header_states {
	  	  h_general                            (0)
		, h_C                                  (1)
		, h_CO                                 (2) 
		, h_CON                                (3)

		, h_matching_connection                (4)
		, h_matching_proxy_connection          (5)
		, h_matching_content_length            (6)
		, h_matching_transfer_encoding         (7)
		, h_matching_upgrade                   (8)

		, h_connection                         (9)
		, h_content_length                     (10)
		, h_transfer_encoding                  (11)
		, h_upgrade                            (12)
 
		, h_matching_transfer_encoding_chunked (13)
		, h_matching_connection_keep_alive     (14)
		, h_matching_connection_close          (15)

		, h_transfer_encoding_chunked          (16)
		, h_connection_keep_alive              (17)
		, h_connection_close                   (18);

	  	private int value;
		private header_states(int value){
			this.value = value;
		}
	};

	private static enum http_host_state {
		  s_http_host_dead       (1)
		, s_http_userinfo_start  (2)
		, s_http_userinfo        (3)
		, s_http_host_start      (4)
		, s_http_host_v6_start   (5)
		, s_http_host            (6)
		, s_http_host_v6         (7)
		, s_http_host_v6_end     (8)
		, s_http_host_port_start (9)
		, s_http_host_port       (10);

		private int value;
		http_host_state(int value){
			this.value = value;
		}
	}

	/* Request Methods */
	public static enum http_method {
		HTTP_DELETE      ("DELETE"),
		HTTP_GET         ("GET"),
		HTTP_HEAD        ("HEAD"),
		HTTP_POST        ("POST"),
		HTTP_PUT         ("PUT"),
		
		/* pathological */               
		HTTP_CONNECT     ("CONNECT"),
		HTTP_OPTIONS     ("OPTIONS"),
		HTTP_TRACE       ("TRACE"),
		
		/* webdav */                  
		HTTP_COPY        ("COPY"),
		HTTP_LOCK        ("LOCK"),
		HTTP_MKCOL       ("MKCOL"),
		HTTP_MOVE        ("MOVE"),
		HTTP_PROPFIND    ("PROPFIND"),
		HTTP_PROPPATCH   ("PROPPATCH"),
		HTTP_SEARCH      ("SEARCH"),
		HTTP_UNLOCK      ("UNLOCK"),
		
		/* subversion */  
		HTTP_REPORT      ("REPORT"),
		HTTP_MKACTIVITY  ("MKACTIVITY"),
		HTTP_CHECKOUT    ("CHECKOUT"),
		HTTP_MERGE       ("MERGE"),
		
		/* upnp */
		HTTP_MSEARCH     ("MSEARCH"),
		HTTP_NOTIFY      ("NOTIFY"),
		HTTP_SUBSCRIBE   ("SUBSCRIBE"),
		HTTP_UNSUBSCRIBE ("UNSUBSCRIBE"),
		
		/* RFC-5789 */
		HTTP_PATCH       ("PATCH"),
		HTTP_PURGE       ("PURGE");

		private String desc;
		private http_method(String desc) {
			this.desc = desc;
		}
	}
	
	/* Define HPE_* values for each errno value above */
	public static enum http_errno {
		/* No error */                                                     
		///XX(OK, "success")                                                  
		HPE_OK("success"),    
		
		/* Callback-related errors */                                      
		///XX(CB_message_begin, "the on_message_begin callback failed")       
		///XX(CB_url, "the on_url callback failed")                           
		///XX(CB_header_field, "the on_header_field callback failed")         
		///XX(CB_header_value, "the on_header_value callback failed")         
		///XX(CB_headers_complete, "the on_headers_complete callback failed") 
		///XX(CB_body, "the on_body callback failed")                         
		///XX(CB_message_complete, "the on_message_complete callback failed") 
		///XX(CB_status, "the on_status callback failed")                     
		HPE_CB_message_begin("the on_message_begin callback failed"),
		HPE_CB_url("the on_url callback failed"),
		HPE_CB_header_field("the on_header_field callback failed" ),
		HPE_CB_header_value("the on_header_value callback failed"),
		HPE_CB_headers_complete("the on_headers_complete callback failed"),
		HPE_CB_body("the on_body callback failed"),
		HPE_CB_message_complete("the on_message_complete callback failed"),
		HPE_CB_status("the on_status callback failed"),

		/* Parsing-related errors */                                       
		///XX(INVALID_EOF_STATE, "stream ended at an unexpected time")       
		///XX(HEADER_OVERFLOW,                                                
		///   "too many header bytes seen; overflow detected")               
		///XX(CLOSED_CONNECTION,                                              
		///   "data received after completed connection: close message")     
		///XX(INVALID_VERSION, "invalid HTTP version")                        
		///XX(INVALID_STATUS, "invalid HTTP status code")                     
		///XX(INVALID_METHOD, "invalid HTTP method")                          
		///XX(INVALID_URL, "invalid URL")                                     
		HPE_INVALID_EOF_STATE("stream ended at an unexpected time"),
		HPE_HEADER_OVERFLOW("too many header bytes seen; overflow detected"),
		HPE_CLOSED_CONNECTION("data received after completed connection: close message"),
		HPE_INVALID_VERSION("invalid HTTP version"),
		HPE_INVALID_STATUS("invalid HTTP status code"),
		HPE_INVALID_METHOD("invalid HTTP method"),
		HPE_INVALID_URL("invalid URL"),	  

		///XX(INVALID_HOST, "invalid host")                                   
		///XX(INVALID_PORT, "invalid port")                                   
		///XX(INVALID_PATH, "invalid path")                                   
		///XX(INVALID_QUERY_STRING, "invalid query string")                   
		///XX(INVALID_FRAGMENT, "invalid fragment")                           
		///XX(LF_EXPECTED, "LF character expected")                           
		///XX(INVALID_HEADER_TOKEN, "invalid character in header")            
		///XX(INVALID_CONTENT_LENGTH,                                        
		///   "invalid character in content-length header")                  
		///XX(INVALID_CHUNK_SIZE,                                            
		///   "invalid character in chunk size header")                      
		///XX(INVALID_CONSTANT, "invalid constant string")                    
		///XX(INVALID_INTERNAL_STATE, "encountered unexpected internal state")
		///XX(STRICT, "strict mode assertion failed")                        
		///XX(PAUSED, "parser is paused")                                     
		///XX(UNKNOWN, "an unknown error occurred")
		HPE_INVALID_HOST("invalid host"),
		HPE_INVALID_PORT("invalid port"),
		HPE_INVALID_PATH("invalid path"),
		HPE_INVALID_QUERY_STRING("invalid query string"),
		HPE_INVALID_FRAGMENT("invalid fragment"),
		HPE_LF_EXPECTED("LF character expected"),
		HPE_INVALID_HEADER_TOKEN("invalid character in header"),
		HPE_INVALID_CONTENT_LENGTH("invalid character in content-length header"),
		HPE_INVALID_CHUNK_SIZE("invalid character in chunk size header"),
		HPE_INVALID_CONSTANT("invalid constant string"),
		HPE_INVALID_INTERNAL_STATE("encountered unexpected internal state"),
		HPE_STRICT("strict mode assertion failed"),
		HPE_PAUSED("parser is paused"),
		HPE_UNKNOWN("an unknown error occurred");
		
		private String desc;
		private http_errno(String desc) {
			this.desc = desc;
		}
	}
	
	/* Flag values for http_parser.flags field */
	private static enum flags { 
		F_CHUNKED                (1 << 0),
		F_CONNECTION_KEEP_ALIVE  (1 << 1),
		F_CONNECTION_CLOSE       (1 << 2),
		F_TRAILING               (1 << 3),
		F_UPGRADE                (1 << 4),
		F_SKIPBODY               (1 << 5);
		
		private int flag;
		private flags(int flag) {
			this.flag= flag;
		}
	}

	private static enum http_parser_url_fields { 
		UF_SCHEMA            (0),
		UF_HOST              (1),
		UF_PORT              (2),
		UF_PATH              (3),
		UF_QUERY             (4),
		UF_FRAGMENT          (5),
		UF_USERINFO          (6),
		UF_MAX               (7);
		
		private int field;

		private http_parser_url_fields(int field) {
			this.field = field;
		}
	}

	private final static String PROXY_CONNECTION  = "proxy-connection";
	private final static String CONNECTION        = "connection";
	private final static String CONTENT_LENGTH    = "content-length";
	private final static String TRANSFER_ENCODING = "transfer-encoding";
	private final static String UPGRADE           = "upgrade";
	private final static String CHUNKED           = "chunked";
	private final static String KEEP_ALIVE        = "keep-alive";
	private final static String CLOSE             = "close";
	
	static final String method_strings[] =
		  {
		///#define XX(num, name, string) #string,
		///  HTTP_METHOD_MAP(XX)
		///#undef XX
		  };
	
	/* Tokens as defined by rfc 2616. Also lowercases them.
	 *        token       = 1*<any CHAR except CTLs or separators>
	 *     separators     = "(" | ")" | "<" | ">" | "@"
	 *                    | "," | ";" | ":" | "\" | <">
	 *                    | "/" | "[" | "]" | "?" | "="
	 *                    | "{" | "}" | SP | HT
	 */
	private final static char tokens[] = {
		/*   0 nul    1 soh    2 stx    3 etx    4 eot    5 enq    6 ack    7 bel  */
		0,       0,       0,       0,       0,       0,       0,       0,
		/*   8 bs     9 ht    10 nl    11 vt    12 np    13 cr    14 so    15 si   */
		0,       0,       0,       0,       0,       0,       0,       0,
		/*  16 dle   17 dc1   18 dc2   19 dc3   20 dc4   21 nak   22 syn   23 etb */
		0,       0,       0,       0,       0,       0,       0,       0,
		/*  24 can   25 em    26 sub   27 esc   28 fs    29 gs    30 rs    31 us  */
		0,       0,       0,       0,       0,       0,       0,       0,
		/*  32 sp    33  !    34  "    35  #    36  $    37  %    38  &    39  '  */
		0,      '!',      0,      '#',     '$',     '%',     '&',    '\'',
		/*  40  (    41  )    42  *    43  +    44  ,    45  -    46  .    47  /  */
		0,       0,      '*',     '+',      0,      '-',     '.',      0,
		/*  48  0    49  1    50  2    51  3    52  4    53  5    54  6    55  7  */
		'0',     '1',     '2',     '3',     '4',     '5',     '6',     '7',
		/*  56  8    57  9    58  :    59  ;    60  <    61  =    62  >    63  ?  */
		'8',     '9',      0,       0,       0,       0,       0,       0,
		/*  64  @    65  A    66  B    67  C    68  D    69  E    70  F    71  G  */
		0,      'a',     'b',     'c',     'd',     'e',     'f',     'g',
		/*  72  H    73  I    74  J    75  K    76  L    77  M    78  N    79  O  */
		'h',     'i',     'j',     'k',     'l',     'm',     'n',     'o',
		/*  80  P    81  Q    82  R    83  S    84  T    85  U    86  V    87  W  */
		'p',     'q',     'r',     's',     't',     'u',     'v',     'w',
		/*  88  X    89  Y    90  Z    91  [    92  \    93  ]    94  ^    95  _  */
		'x',     'y',     'z',      0,       0,       0,      '^',     '_',
		/*  96  `    97  a    98  b    99  c   100  d   101  e   102  f   103  g  */
		'`',     'a',     'b',     'c',     'd',     'e',     'f',     'g',
		/* 104  h   105  i   106  j   107  k   108  l   109  m   110  n   111  o  */
		'h',     'i',     'j',     'k',     'l',     'm',     'n',     'o',
		/* 112  p   113  q   114  r   115  s   116  t   117  u   118  v   119  w  */
		'p',     'q',     'r',     's',     't',     'u',     'v',     'w',
		/* 120  x   121  y   122  z   123  {   124  |   125  }   126  ~   127 del */
		'x',     'y',     'z',      0,      '|',      0,      '~',       0
	};

	private final static byte unhex[] = {
		 -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
		,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
		,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
		, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,-1,-1,-1,-1,-1,-1
		,-1,10,11,12,13,14,15,-1,-1,-1,-1,-1,-1,-1,-1,-1
		,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
		,-1,10,11,12,13,14,15,-1,-1,-1,-1,-1,-1,-1,-1,-1
		,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1
    };
	
	// strict parse mode
	private final static char normal_url_char[] = {
		/*   0 nul    1 soh    2 stx    3 etx    4 eot    5 enq    6 ack    7 bel  */
		        0    |   0    |   0    |   0    |   0    |   0    |   0    |   0,
		/*   8 bs     9 ht    10 nl    11 vt    12 np    13 cr    14 so    15 si   */
		        0    |   0    |   0    |   0    |    0   |   0    |   0    |   0,
		/*  16 dle   17 dc1   18 dc2   19 dc3   20 dc4   21 nak   22 syn   23 etb */
		        0    |   0    |   0    |   0    |   0    |   0    |   0    |   0,
		/*  24 can   25 em    26 sub   27 esc   28 fs    29 gs    30 rs    31 us  */
		        0    |   0    |   0    |   0    |   0    |   0    |   0    |   0,
		/*  32 sp    33  !    34  "    35  #    36  $    37  %    38  &    39  '  */
		        0    |   2    |   4    |   0    |   16   |   32   |   64   |  128,
		/*  40  (    41  )    42  *    43  +    44  ,    45  -    46  .    47  /  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/*  48  0    49  1    50  2    51  3    52  4    53  5    54  6    55  7  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/*  56  8    57  9    58  :    59  ;    60  <    61  =    62  >    63  ?  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |   0,
		/*  64  @    65  A    66  B    67  C    68  D    69  E    70  F    71  G  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/*  72  H    73  I    74  J    75  K    76  L    77  M    78  N    79  O  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/*  80  P    81  Q    82  R    83  S    84  T    85  U    86  V    87  W  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/*  88  X    89  Y    90  Z    91  [    92  \    93  ]    94  ^    95  _  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/*  96  `    97  a    98  b    99  c   100  d   101  e   102  f   103  g  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/* 104  h   105  i   106  j   107  k   108  l   109  m   110  n   111  o  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/* 112  p   113  q   114  r   115  s   116  t   117  u   118  v   119  w  */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |  128,
		/* 120  x   121  y   122  z   123  {   124  |   125  }   126  ~   127 del */
		        1    |   2    |   4    |   8    |   16   |   32   |   64   |   0, 
	};

	
	/* Result structure for http_parser_parse_url().
	 *
	 * Callers should index into field_data[] with UF_* values if field_set
	 * has the relevant (1 << UF_*) bit set. As a courtesy to clients (and
	 * because we probably have padding left over), we convert any port to
	 * a uint16_t.
	 */
	private class http_parser_url {
		int field_set;              /* Bitmask of (1 << UF_*) values */
		int port;                   /* Converted UF_PORT string */

		class field_data_t {
			int off;                /* Offset into buffer in which field starts */
			int len;                /* Length of run in buffer */
		}

		field_data_t field_data[];

		http_parser_url() {
			field_data = new field_data_t[http_parser_url_fields.UF_MAX.field];
		}
	}

	/*
	 * 
struct http_parser_settings {
  http_cb      on_message_begin;
  http_data_cb on_url;
  http_data_cb on_status;
  http_data_cb on_header_field;
  http_data_cb on_header_value;
  http_cb      on_headers_complete;
  http_data_cb on_body;
  http_cb      on_message_complete;
};
	 * */
	protected abstract int on_message_begin();
	protected abstract int on_url(String url);
	protected abstract int on_status(int status);
	protected abstract int on_header_field(String field);
	protected abstract int on_header_value(String field);
	protected abstract int on_headers_complete(String field);
	protected abstract int on_body(ByteBuffer body);
	protected abstract int on_message_complete();

	/* Returns the library version. Bits 16-23 contain the major version number,
	 * bits 8-15 the minor version number and bits 0-7 the patch level.
	 * Usage example:
	 *
	 *   unsigned long version = http_parser_version();
	 *   unsigned major = (version >> 16) & 255;
	 *   unsigned minor = (version >> 8) & 255;
	 *   unsigned patch = version & 255;
	 *   printf("http_parser v%u.%u.%u\n", major, minor, version);
	 */
	public long http_parser_version() {
		return 0;
	}

	///void http_parser_init(http_parser *parser, enum http_parser_type type);

	protected int execute(ByteBuffer data) {
		return 0;
	}

	/* If http_should_keep_alive() in the on_headers_complete or
	 * on_message_complete callback returns 0, then this should be
	 * the last message on the connection.
	 * If you are the server, respond with the "Connection: close" header.
	 * If you are the client, close the connection.
	 */
	///int http_should_keep_alive(const http_parser *parser);
	protected boolean should_keep_alive() {
		return false;
	}

	/* Returns a string version of the HTTP method. */
	///const char *http_method_str(enum http_method m);
	public static String http_method_str(http_method m) {

		return "";
	}

	/* Return a string name of the given error */
	///const char *http_errno_name(enum http_errno err);
	public static String http_errno_name(http_errno err) {
		return "";
	}

	/* Return a string description of the given error */
	///const char *http_errno_description(enum http_errno err);
	public static String http_errno_description(http_errno err) {
		return "";
	}

	/* Parse a URL; return nonzero on failure */
	///int http_parser_parse_url(const char *buf, size_t buflen,
	///                          int is_connect,
	///                          struct http_parser_url *u);
	protected int parse_url(ByteBuffer buf, boolean is_connect, http_parser_url url) {
		return 0;
	}

	/* Pause or un-pause the parser; a nonzero value pauses */
	///void http_parser_pause(http_parser *parser, int paused);
	protected void pause(int paused) {

	}

	/* Checks if this is the final chunk of the body. */
	///int http_body_is_final(const http_parser *parser);
	protected boolean http_body_is_final() {
		return false;
	}

	///struct http_parser {
	/** PRIVATE **/
	private http_parser_type type;      /* enum http_parser_type */
	private int flags;                  /* F_* values from 'flags' enum; semi-public */
	private state state;                /* enum state from http_parser.c */
	private header_states header_state; /* enum header_state from http_parser.c */
	private int index;                  /* index into current matcher */

	private int nread;           /* # bytes read in various scenarios */
	private long content_length; /* # bytes in body (0 if no Content-Length header) */

	/** READ-ONLY **/
	private int http_major;
	private int http_minor;
	private int status_code;     /* responses only */
	private int method;          /* requests only */
	private http_errno http_errno;

	/* 1 = Upgrade header was present and the parser has exited because of that.
	 * 0 = No upgrade header present.
	 * Should be checked when http_parser_execute() returns in addition to
	 * error checking.
	 */
	private int upgrade;

	/** PUBLIC **/
	public final Object data; /* A pointer to get hook to the "connection" or "socket" object */
	///};


}
