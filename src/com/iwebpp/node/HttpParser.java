package com.iwebpp.node;

import java.nio.ByteBuffer;

public abstract class HttpParser {

	protected HttpParser(http_parser_type type, Object data) {
        this.data = data;
        this.type = type;
        this.http_errno = http_errno.HPE_OK;
        this.State = (type == http_parser_type.HTTP_REQUEST ? State.s_start_req : 
        	         (type == http_parser_type.HTTP_RESPONSE ? State.s_start_res : State.s_start_req_or_res));
	}
	@SuppressWarnings("unused")
	private HttpParser() {this.data = null;}

	public static enum http_parser_type {
		HTTP_REQUEST, HTTP_RESPONSE, HTTP_BOTH;
	}
	
	private enum State {
	  	  s_dead                                (1) /* important that this is > 0 */

		, s_start_req_or_res                    (2) 
		, s_res_or_resp_H                       (3)
		, s_start_res                           (4)
		, s_res_H                               (5)
		, s_res_HT                              (6)
		, s_res_HTT                             (7)
		, s_res_HTTP                            (8)
		, s_res_first_http_major                (9)
		, s_res_http_major                      (10)
		, s_res_first_http_minor                (11)
		, s_res_http_minor                      (12)
		, s_res_first_status_code               (13)
		, s_res_status_code                     (14)
		, s_res_status_start                    (15)
		, s_res_status                          (16)
		, s_res_line_almost_done                (17)

		, s_start_req                           (18)

		, s_req_method                          (19)
		, s_req_spaces_before_url               (20)
		, s_req_schema                          (21)
		, s_req_schema_slash                    (22)
		, s_req_schema_slash_slash              (23)
		, s_req_server_start                    (24)
		, s_req_server                          (25)
		, s_req_server_with_at                  (26)
		, s_req_path                            (27)
		, s_req_query_string_start              (28)
		, s_req_query_string                    (29)
		, s_req_fragment_start                  (30)
		, s_req_fragment                        (31)
		, s_req_http_start                      (32)
		, s_req_http_H                          (33)
		, s_req_http_HT                         (34)
		, s_req_http_HTT                        (35)
		, s_req_http_HTTP                       (36)
		, s_req_first_http_major                (37)
		, s_req_http_major                      (38)
		, s_req_first_http_minor                (39)
		, s_req_http_minor                      (40)
		, s_req_line_almost_done                (41)

		, s_header_field_start                  (42)
		, s_header_field                        (43)
		, s_header_value_discard_ws             (44)
		, s_header_value_discard_ws_almost_done (45)
		, s_header_value_discard_lws            (46)
		, s_header_value_start                  (47)
		, s_header_value                        (48)
		, s_header_value_lws                    (49)

		, s_header_almost_done                  (50)

		, s_chunk_size_start                    (51)
		, s_chunk_size                          (52)
		, s_chunk_parameters                    (53)
		, s_chunk_size_almost_done              (54)

		, s_headers_almost_done                 (55)
		, s_headers_done                        (56)

		/* Important: 's_headers_done' must be the last 'header' State. All
		 * states beyond this must be 'body' states. It is used for overflow
		 * checking. See the PARSING_HEADER() macro.
		 */

		, s_chunk_data                          (57)
		, s_chunk_data_almost_done              (58)
		, s_chunk_data_done                     (59)

		, s_body_identity                       (60)
		, s_body_identity_eof                   (61)

		, s_message_done                        (62);
		
	  	private int state;
		private State(int state) {
			this.state = state;
		}
	}
	
	private enum header_states {
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

	  	private int state;
		private header_states(int state){
			this.state = state;
		}
	};

	private enum http_host_state {
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

		private int state;
		http_host_state(int state){
			this.state = state;
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
		///XX(INVALID_INTERNAL_STATE, "encountered unexpected internal State")
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
		HPE_INVALID_INTERNAL_STATE("encountered unexpected internal State"),
		HPE_STRICT("strict mode assertion failed"),
		HPE_PAUSED("parser is paused"),
		HPE_UNKNOWN("an unknown error occurred");
		
		private String desc;
		private http_errno(String desc) {
			this.desc = desc;
		}
	}
	
	/* Get an http_errno value from an http_parser */
	private http_errno HTTP_PARSER_ERRNO() {
		return this.http_errno;
	}
	
	/* Flag values for http_parser.flags field */
	private enum Flags { 
		F_CHUNKED                (1 << 0),
		F_CONNECTION_KEEP_ALIVE  (1 << 1),
		F_CONNECTION_CLOSE       (1 << 2),
		F_TRAILING               (1 << 3),
		F_UPGRADE                (1 << 4),
		F_SKIPBODY               (1 << 5);
		
		private int flag;
		private Flags(int flag) {
			this.flag= flag;
		}
	}

	private enum http_parser_url_fields { 
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

	private static long ULLONG_MAX() {
		return Long.MAX_VALUE; 
	}
	
	private static int MIN(int a, int b) {
		return ((a) < (b) ? (a) : (b));
	}
	
	private static int ARRAY_SIZE(char [] a) {
		return a.length;
	}
	
	private static boolean BIT_AT(char [] a, char i) {
		return (0 != ( a[i >> 3] &                  
				      (1 << (i & 7))));
	}
	
	private static char ELEM_AT(char [] a, int i, char v) {
		return ((int) (i) < ARRAY_SIZE(a) ? (a)[(i)] : (v));
	}
	
	private void SET_ERRNO(http_errno e) {
		this.http_errno = e;
	}
	
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
	
	private boolean PARSING_HEADER() {
		return (this.state.state <= State.s_headers_done.state);
	}
	
	/* Does the parser need to see an EOF to find the end of the message? */
	private boolean http_message_needs_eof () {
		if (type == http_parser_type.HTTP_REQUEST) {
			return false;
		}

		/* See RFC 2616 section 4.4 */
		if (status_code / 100 == 1 || /* 1xx e.g. Continue */
			status_code == 204 ||     /* No Content */
			status_code == 304 ||     /* Not Modified */
		   (flags & Flags.F_SKIPBODY.flag)!=0) {     /* response to a HEAD request */
			return false;
		}

		if ((flags & Flags.F_CHUNKED.flag)!=0 || content_length != ULLONG_MAX()) {
			return false;
		}

		return true;
	}
	
	/* Macros for character classes; depends on strict-mode  */
	/*#define CR                  '\r'
	#define LF                  '\n'
	#define LOWER(c)            (unsigned char)(c | 0x20)
	#define IS_ALPHA(c)         (LOWER(c) >= 'a' && LOWER(c) <= 'z')
	#define IS_NUM(c)           ((c) >= '0' && (c) <= '9')
	#define IS_ALPHANUM(c)      (IS_ALPHA(c) || IS_NUM(c))
	#define IS_HEX(c)           (IS_NUM(c) || (LOWER(c) >= 'a' && LOWER(c) <= 'f'))
	#define IS_MARK(c)          ((c) == '-' || (c) == '_' || (c) == '.' || \
	  (c) == '!' || (c) == '~' || (c) == '*' || (c) == '\'' || (c) == '(' || \
	  (c) == ')')
	#define IS_USERINFO_CHAR(c) (IS_ALPHANUM(c) || IS_MARK(c) || (c) == '%' || \
	  (c) == ';' || (c) == ':' || (c) == '&' || (c) == '=' || (c) == '+' || \
	  (c) == '$' || (c) == ',')

	#if HTTP_PARSER_STRICT
	#define TOKEN(c)            (tokens[(unsigned char)c])
	#define IS_URL_CHAR(c)      (BIT_AT(normal_url_char, (unsigned char)c))
	#define IS_HOST_CHAR(c)     (IS_ALPHANUM(c) || (c) == '.' || (c) == '-')
	#else
	#define TOKEN(c)            ((c == ' ') ? ' ' : tokens[(unsigned char)c])
	#define IS_URL_CHAR(c)                                                         \
	  (BIT_AT(normal_url_char, (unsigned char)c) || ((c) & 0x80))
	#define IS_HOST_CHAR(c)                                                        \
	  (IS_ALPHANUM(c) || (c) == '.' || (c) == '-' || (c) == '_')
	#endif


	#define start_state (parser->type == HTTP_REQUEST ? s_start_req : s_start_res)
	*/
	
	private static char CR() {
		return '\r';
	}
	private static char LF() {
		return '\n';
	}
	private static char LOWER(char c) {
		return Character.toLowerCase(c);
	}
	private static boolean IS_ALPHA(char c) {
		return (LOWER(c) >= 'a' && LOWER(c) <= 'z');
	}
	private static boolean IS_NUM(char c) {
		return ((c) >= '0' && (c) <= '9');
	}
	private static boolean IS_ALPHANUM(char c) {
		return (IS_ALPHA(c) || IS_NUM(c));
	}
	private static boolean IS_HEX(char c) {
		return (IS_NUM(c) || (LOWER(c) >= 'a' && LOWER(c) <= 'f'));
	}
	private static boolean IS_MARK(char c) {
		return ((c) == '-' || (c) == '_' || (c) == '.' || 
				(c) == '!' || (c) == '~' || (c) == '*' || 
				(c) == '\'' || (c) == '(' || (c) == ')');
	}
	private static boolean IS_USERINFO_CHAR(char c) {
		return (IS_ALPHANUM(c) || IS_MARK(c) ||
				(c) == '%' || (c) == ';' || (c) == ':' || 
				(c) == '&' || (c) == '=' || (c) == '+' ||
				(c) == '$' || (c) == ',');
	}
	
	private static char TOKEN(char c) {
		return (tokens[c]);
	}

	private static boolean IS_URL_CHAR(char c) {     
		return (BIT_AT(normal_url_char, c));
	}
	
	private static boolean IS_HOST_CHAR(char c) {
		return (IS_ALPHANUM(c) || (c) == '.' || (c) == '-');
	}
	
	private State start_state() {
		return (type == http_parser_type.HTTP_REQUEST ? State.s_start_req : State.s_start_res);
	}
	
	/* Our URL parser.
	 *
	 * This is designed to be shared by http_parser_execute() for URL validation,
	 * hence it has a State transition + byte-for-byte interface. In addition, it
	 * is meant to be embedded in http_parser_parse_url(), which does the dirty
	 * work of turning State transitions URL components for its API.
	 *
	 * This function should only be invoked with non-space characters. It is
	 * assumed that the caller cares about (and can detect) the transition between
	 * URL and non-URL states by looking for these.
	 */
	private State parse_url_char(State s, final char ch)
	{
	  if (ch == ' ' || ch == '\r' || ch == '\n') {
	    return State.s_dead;
	  }

	///#if HTTP_PARSER_STRICT
	  if (ch == '\t' || ch == '\f') {
	    return State.s_dead;
	  }
	///#endif

	  switch (s) {
	    case s_req_spaces_before_url:
	      /* Proxied requests are followed by scheme of an absolute URI (alpha).
	       * All methods except CONNECT are followed by '/' or '*'.
	       */

	      if (ch == '/' || ch == '*') {
	        return State.s_req_path;
	      }

	      if (IS_ALPHA(ch)) {
	        return State.s_req_schema;
	      }

	      break;

	    case s_req_schema:
	      if (IS_ALPHA(ch)) {
	        return s;
	      }

	      if (ch == ':') {
	        return State.s_req_schema_slash;
	      }

	      break;

	    case s_req_schema_slash:
	      if (ch == '/') {
	        return State.s_req_schema_slash_slash;
	      }

	      break;

	    case s_req_schema_slash_slash:
	      if (ch == '/') {
	        return State.s_req_server_start;
	      }

	      break;

	    case s_req_server_with_at:
	      if (ch == '@') {
	        return State.s_dead;
	      }

	    /* FALLTHROUGH */
	    case s_req_server_start:
	    case s_req_server:
	      if (ch == '/') {
	        return State.s_req_path;
	      }

	      if (ch == '?') {
	        return State.s_req_query_string_start;
	      }

	      if (ch == '@') {
	        return State.s_req_server_with_at;
	      }

	      if (IS_USERINFO_CHAR(ch) || ch == '[' || ch == ']') {
	        return State.s_req_server;
	      }

	      break;

	    case s_req_path:
	      if (IS_URL_CHAR(ch)) {
	        return s;
	      }

	      switch (ch) {
	        case '?':
	          return State.s_req_query_string_start;

	        case '#':
	          return State.s_req_fragment_start;
	      }

	      break;

	    case s_req_query_string_start:
	    case s_req_query_string:
	      if (IS_URL_CHAR(ch)) {
	        return State.s_req_query_string;
	      }

	      switch (ch) {
	        case '?':
	          /* allow extra '?' in query string */
	          return State.s_req_query_string;

	        case '#':
	          return State.s_req_fragment_start;
	      }

	      break;

	    case s_req_fragment_start:
	      if (IS_URL_CHAR(ch)) {
	        return State.s_req_fragment;
	      }

	      switch (ch) {
	        case '?':
	          return State.s_req_fragment;

	        case '#':
	          return s;
	      }

	      break;

	    case s_req_fragment:
	      if (IS_URL_CHAR(ch)) {
	        return s;
	      }

	      switch (ch) {
	        case '?':
	        case '#':
	          return s;
	      }

	      break;

	    default:
	      break;
	  }

	  /* We should never fall out of the switch above unless there's an error */
	  return State.s_dead;
	}

	
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
	private final static long HTTP_PARSER_VERSION_MAJOR = 2;
	private final static long HTTP_PARSER_VERSION_MINOR = 3;
	private final static long HTTP_PARSER_VERSION_PATCH = 0;

	public long http_parser_version() {
		return HTTP_PARSER_VERSION_MAJOR * 0x10000 |
			   HTTP_PARSER_VERSION_MINOR * 0x00100 |
			   HTTP_PARSER_VERSION_PATCH * 0x00001;
	}

	///void http_parser_init(http_parser *parser, enum http_parser_type type);

	protected int execute(ByteBuffer data) {
		char c, ch;
		
		/*int8_t unhex_val;
		const char *p = data;
		const char *header_field_mark = 0;
		const char *header_value_mark = 0;
		const char *url_mark = 0;
		const char *body_mark = 0;
		const char *status_mark = 0;
		*/
		byte unhex_val;
		int p = 0;
		int header_field_mark = -1;
		int header_value_mark = -1;
		int url_mark = -1;
		int body_mark = -1;
		int status_mark = -1;
		int len = data.capacity();
		
		/* We're in an error state. Don't bother doing anything. */
		if (HTTP_PARSER_ERRNO() != http_errno.HPE_OK) {
			return 0;
		}

		if (len == 0) {
			switch (state) {
			case s_body_identity_eof:
				/* Use of CALLBACK_NOTIFY() here would erroneously return 1 byte read if
				 * we got paused.
				 */
				///CALLBACK_NOTIFY_NOADVANCE(message_complete);
				return 0;

			case s_dead:
			case s_start_req_or_res:
			case s_start_res:
			case s_start_req:
				return 0;

			default:
				SET_ERRNO(http_errno.HPE_INVALID_EOF_STATE);
				return 1;
			}
		}


		if (state == State.s_header_field)
			header_field_mark = 0;///data;
		if (state == State.s_header_value)
			header_value_mark = 0;///data;
		switch (state) {
		case s_req_path:
		case s_req_schema:
		case s_req_schema_slash:
		case s_req_schema_slash_slash:
		case s_req_server_start:
		case s_req_server:
		case s_req_server_with_at:
		case s_req_query_string_start:
		case s_req_query_string:
		case s_req_fragment_start:
		case s_req_fragment:
			url_mark = 0;///data;
			break;
		case s_res_status:
			status_mark = 0;///data;
			break;
		}

		for (p=data; p != data + len; p++) {
			ch = *p;

			if (PARSING_HEADER(parser->state)) {
				++parser->nread;
				/* Don't allow the total size of the HTTP headers (including the status
				 * line) to exceed HTTP_MAX_HEADER_SIZE.  This check is here to protect
				 * embedders against denial-of-service attacks where the attacker feeds
				 * us a never-ending header that the embedder keeps buffering.
				 *
				 * This check is arguably the responsibility of embedders but we're doing
				 * it on the embedder's behalf because most won't bother and this way we
				 * make the web a little safer.  HTTP_MAX_HEADER_SIZE is still far bigger
				 * than any reasonable request or response so this should never affect
				 * day-to-day operation.
				 */
				if (parser->nread > HTTP_MAX_HEADER_SIZE) {
					SET_ERRNO(HPE_HEADER_OVERFLOW);
					goto error;
				}
			}

			reexecute_byte:
				switch (parser->state) {

				case s_dead:
					/* this state is used after a 'Connection: close' message
					 * the parser will error out if it reads another message
					 */
					if (ch == CR || ch == LF)
						break;

					SET_ERRNO(HPE_CLOSED_CONNECTION);
					goto error;

				case s_start_req_or_res:
				{
					if (ch == CR || ch == LF)
						break;
					parser->flags = 0;
					parser->content_length = ULLONG_MAX;

					if (ch == 'H') {
						parser->state = s_res_or_resp_H;

						CALLBACK_NOTIFY(message_begin);
					} else {
						parser->type = HTTP_REQUEST;
						parser->state = s_start_req;
						goto reexecute_byte;
					}

					break;
				}

				case s_res_or_resp_H:
					if (ch == 'T') {
						parser->type = HTTP_RESPONSE;
						parser->state = s_res_HT;
					} else {
						if (ch != 'E') {
							SET_ERRNO(HPE_INVALID_CONSTANT);
							goto error;
						}

						parser->type = HTTP_REQUEST;
						parser->method = HTTP_HEAD;
						parser->index = 2;
						parser->state = s_req_method;
					}
					break;

				case s_start_res:
				{
					parser->flags = 0;
					parser->content_length = ULLONG_MAX;

					switch (ch) {
					case 'H':
						parser->state = s_res_H;
						break;

					case CR:
					case LF:
						break;

					default:
						SET_ERRNO(HPE_INVALID_CONSTANT);
						goto error;
					}

					CALLBACK_NOTIFY(message_begin);
					break;
				}

				case s_res_H:
					STRICT_CHECK(ch != 'T');
					parser->state = s_res_HT;
					break;

				case s_res_HT:
					STRICT_CHECK(ch != 'T');
					parser->state = s_res_HTT;
					break;

				case s_res_HTT:
					STRICT_CHECK(ch != 'P');
					parser->state = s_res_HTTP;
					break;

				case s_res_HTTP:
					STRICT_CHECK(ch != '/');
					parser->state = s_res_first_http_major;
					break;

				case s_res_first_http_major:
					if (ch < '0' || ch > '9') {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_major = ch - '0';
					parser->state = s_res_http_major;
					break;

					/* major HTTP version or dot */
				case s_res_http_major:
				{
					if (ch == '.') {
						parser->state = s_res_first_http_minor;
						break;
					}

					if (!IS_NUM(ch)) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_major *= 10;
					parser->http_major += ch - '0';

					if (parser->http_major > 999) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					break;
				}

				/* first digit of minor HTTP version */
				case s_res_first_http_minor:
					if (!IS_NUM(ch)) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_minor = ch - '0';
					parser->state = s_res_http_minor;
					break;

					/* minor HTTP version or end of request line */
				case s_res_http_minor:
				{
					if (ch == ' ') {
						parser->state = s_res_first_status_code;
						break;
					}

					if (!IS_NUM(ch)) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_minor *= 10;
					parser->http_minor += ch - '0';

					if (parser->http_minor > 999) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					break;
				}

				case s_res_first_status_code:
				{
					if (!IS_NUM(ch)) {
						if (ch == ' ') {
							break;
						}

						SET_ERRNO(HPE_INVALID_STATUS);
						goto error;
					}
					parser->status_code = ch - '0';
					parser->state = s_res_status_code;
					break;
				}

				case s_res_status_code:
				{
					if (!IS_NUM(ch)) {
						switch (ch) {
						case ' ':
							parser->state = s_res_status_start;
							break;
						case CR:
							parser->state = s_res_line_almost_done;
							break;
						case LF:
							parser->state = s_header_field_start;
							break;
						default:
							SET_ERRNO(HPE_INVALID_STATUS);
							goto error;
						}
						break;
					}

					parser->status_code *= 10;
					parser->status_code += ch - '0';

					if (parser->status_code > 999) {
						SET_ERRNO(HPE_INVALID_STATUS);
						goto error;
					}

					break;
				}

				case s_res_status_start:
				{
					if (ch == CR) {
						parser->state = s_res_line_almost_done;
						break;
					}

					if (ch == LF) {
						parser->state = s_header_field_start;
						break;
					}

					MARK(status);
					parser->state = s_res_status;
					parser->index = 0;
					break;
				}

				case s_res_status:
					if (ch == CR) {
						parser->state = s_res_line_almost_done;
						CALLBACK_DATA(status);
						break;
					}

					if (ch == LF) {
						parser->state = s_header_field_start;
						CALLBACK_DATA(status);
						break;
					}

					break;

				case s_res_line_almost_done:
					STRICT_CHECK(ch != LF);
					parser->state = s_header_field_start;
					break;

				case s_start_req:
				{
					if (ch == CR || ch == LF)
						break;
					parser->flags = 0;
					parser->content_length = ULLONG_MAX;

					if (!IS_ALPHA(ch)) {
						SET_ERRNO(HPE_INVALID_METHOD);
						goto error;
					}

					parser->method = (enum http_method) 0;
					parser->index = 1;
					switch (ch) {
					case 'C': parser->method = HTTP_CONNECT; /* or COPY, CHECKOUT */ break;
					case 'D': parser->method = HTTP_DELETE; break;
					case 'G': parser->method = HTTP_GET; break;
					case 'H': parser->method = HTTP_HEAD; break;
					case 'L': parser->method = HTTP_LOCK; break;
					case 'M': parser->method = HTTP_MKCOL; /* or MOVE, MKACTIVITY, MERGE, M-SEARCH */ break;
					case 'N': parser->method = HTTP_NOTIFY; break;
					case 'O': parser->method = HTTP_OPTIONS; break;
					case 'P': parser->method = HTTP_POST;
					/* or PROPFIND|PROPPATCH|PUT|PATCH|PURGE */
					break;
					case 'R': parser->method = HTTP_REPORT; break;
					case 'S': parser->method = HTTP_SUBSCRIBE; /* or SEARCH */ break;
					case 'T': parser->method = HTTP_TRACE; break;
					case 'U': parser->method = HTTP_UNLOCK; /* or UNSUBSCRIBE */ break;
					default:
						SET_ERRNO(HPE_INVALID_METHOD);
						goto error;
					}
					parser->state = s_req_method;

					CALLBACK_NOTIFY(message_begin);

					break;
				}

				case s_req_method:
				{
					const char *matcher;
					if (ch == '\0') {
						SET_ERRNO(HPE_INVALID_METHOD);
						goto error;
					}

					matcher = method_strings[parser->method];
					if (ch == ' ' && matcher[parser->index] == '\0') {
						parser->state = s_req_spaces_before_url;
					} else if (ch == matcher[parser->index]) {
						; /* nada */
					} else if (parser->method == HTTP_CONNECT) {
						if (parser->index == 1 && ch == 'H') {
							parser->method = HTTP_CHECKOUT;
						} else if (parser->index == 2  && ch == 'P') {
							parser->method = HTTP_COPY;
						} else {
							SET_ERRNO(HPE_INVALID_METHOD);
							goto error;
						}
					} else if (parser->method == HTTP_MKCOL) {
						if (parser->index == 1 && ch == 'O') {
							parser->method = HTTP_MOVE;
						} else if (parser->index == 1 && ch == 'E') {
							parser->method = HTTP_MERGE;
						} else if (parser->index == 1 && ch == '-') {
							parser->method = HTTP_MSEARCH;
						} else if (parser->index == 2 && ch == 'A') {
							parser->method = HTTP_MKACTIVITY;
						} else {
							SET_ERRNO(HPE_INVALID_METHOD);
							goto error;
						}
					} else if (parser->method == HTTP_SUBSCRIBE) {
						if (parser->index == 1 && ch == 'E') {
							parser->method = HTTP_SEARCH;
						} else {
							SET_ERRNO(HPE_INVALID_METHOD);
							goto error;
						}
					} else if (parser->index == 1 && parser->method == HTTP_POST) {
						if (ch == 'R') {
							parser->method = HTTP_PROPFIND; /* or HTTP_PROPPATCH */
						} else if (ch == 'U') {
							parser->method = HTTP_PUT; /* or HTTP_PURGE */
						} else if (ch == 'A') {
							parser->method = HTTP_PATCH;
						} else {
							SET_ERRNO(HPE_INVALID_METHOD);
							goto error;
						}
					} else if (parser->index == 2) {
						if (parser->method == HTTP_PUT) {
							if (ch == 'R') {
								parser->method = HTTP_PURGE;
							} else {
								SET_ERRNO(HPE_INVALID_METHOD);
								goto error;
							}
						} else if (parser->method == HTTP_UNLOCK) {
							if (ch == 'S') {
								parser->method = HTTP_UNSUBSCRIBE;
							} else {
								SET_ERRNO(HPE_INVALID_METHOD);
								goto error;
							}
						} else {
							SET_ERRNO(HPE_INVALID_METHOD);
							goto error;
						}
					} else if (parser->index == 4 && parser->method == HTTP_PROPFIND && ch == 'P') {
						parser->method = HTTP_PROPPATCH;
					} else {
						SET_ERRNO(HPE_INVALID_METHOD);
						goto error;
					}

					++parser->index;
					break;
				}

				case s_req_spaces_before_url:
				{
					if (ch == ' ') break;

					MARK(url);
					if (parser->method == HTTP_CONNECT) {
						parser->state = s_req_server_start;
					}

					parser->state = parse_url_char((enum state)parser->state, ch);
					if (parser->state == s_dead) {
						SET_ERRNO(HPE_INVALID_URL);
						goto error;
					}

					break;
				}

				case s_req_schema:
				case s_req_schema_slash:
				case s_req_schema_slash_slash:
				case s_req_server_start:
				{
					switch (ch) {
					/* No whitespace allowed here */
					case ' ':
					case CR:
					case LF:
						SET_ERRNO(HPE_INVALID_URL);
						goto error;
					default:
						parser->state = parse_url_char((enum state)parser->state, ch);
						if (parser->state == s_dead) {
							SET_ERRNO(HPE_INVALID_URL);
							goto error;
						}
					}

					break;
				}

				case s_req_server:
				case s_req_server_with_at:
				case s_req_path:
				case s_req_query_string_start:
				case s_req_query_string:
				case s_req_fragment_start:
				case s_req_fragment:
				{
					switch (ch) {
					case ' ':
						parser->state = s_req_http_start;
						CALLBACK_DATA(url);
						break;
					case CR:
					case LF:
						parser->http_major = 0;
						parser->http_minor = 9;
						parser->state = (ch == CR) ?
								s_req_line_almost_done :
									s_header_field_start;
						CALLBACK_DATA(url);
						break;
					default:
						parser->state = parse_url_char((enum state)parser->state, ch);
						if (parser->state == s_dead) {
							SET_ERRNO(HPE_INVALID_URL);
							goto error;
						}
					}
					break;
				}

				case s_req_http_start:
					switch (ch) {
					case 'H':
						parser->state = s_req_http_H;
						break;
					case ' ':
						break;
					default:
						SET_ERRNO(HPE_INVALID_CONSTANT);
						goto error;
					}
					break;

				case s_req_http_H:
					STRICT_CHECK(ch != 'T');
					parser->state = s_req_http_HT;
					break;

				case s_req_http_HT:
					STRICT_CHECK(ch != 'T');
					parser->state = s_req_http_HTT;
					break;

				case s_req_http_HTT:
					STRICT_CHECK(ch != 'P');
					parser->state = s_req_http_HTTP;
					break;

				case s_req_http_HTTP:
					STRICT_CHECK(ch != '/');
					parser->state = s_req_first_http_major;
					break;

					/* first digit of major HTTP version */
				case s_req_first_http_major:
					if (ch < '1' || ch > '9') {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_major = ch - '0';
					parser->state = s_req_http_major;
					break;

					/* major HTTP version or dot */
				case s_req_http_major:
				{
					if (ch == '.') {
						parser->state = s_req_first_http_minor;
						break;
					}

					if (!IS_NUM(ch)) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_major *= 10;
					parser->http_major += ch - '0';

					if (parser->http_major > 999) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					break;
				}

				/* first digit of minor HTTP version */
				case s_req_first_http_minor:
					if (!IS_NUM(ch)) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_minor = ch - '0';
					parser->state = s_req_http_minor;
					break;

					/* minor HTTP version or end of request line */
				case s_req_http_minor:
				{
					if (ch == CR) {
						parser->state = s_req_line_almost_done;
						break;
					}

					if (ch == LF) {
						parser->state = s_header_field_start;
						break;
					}

					/* XXX allow spaces after digit? */

					if (!IS_NUM(ch)) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					parser->http_minor *= 10;
					parser->http_minor += ch - '0';

					if (parser->http_minor > 999) {
						SET_ERRNO(HPE_INVALID_VERSION);
						goto error;
					}

					break;
				}

				/* end of request line */
				case s_req_line_almost_done:
				{
					if (ch != LF) {
						SET_ERRNO(HPE_LF_EXPECTED);
						goto error;
					}

					parser->state = s_header_field_start;
					break;
				}

				case s_header_field_start:
				{
					if (ch == CR) {
						parser->state = s_headers_almost_done;
						break;
					}

					if (ch == LF) {
						/* they might be just sending \n instead of \r\n so this would be
						 * the second \n to denote the end of headers*/
						parser->state = s_headers_almost_done;
						goto reexecute_byte;
					}

					c = TOKEN(ch);

					if (!c) {
						SET_ERRNO(HPE_INVALID_HEADER_TOKEN);
						goto error;
					}

					MARK(header_field);

					parser->index = 0;
					parser->state = s_header_field;

					switch (c) {
					case 'c':
						parser->header_state = h_C;
						break;

					case 'p':
						parser->header_state = h_matching_proxy_connection;
						break;

					case 't':
						parser->header_state = h_matching_transfer_encoding;
						break;

					case 'u':
						parser->header_state = h_matching_upgrade;
						break;

					default:
						parser->header_state = h_general;
						break;
					}
					break;
				}

				case s_header_field:
				{
					c = TOKEN(ch);

					if (c) {
						switch (parser->header_state) {
						case h_general:
							break;

						case h_C:
							parser->index++;
							parser->header_state = (c == 'o' ? h_CO : h_general);
							break;

						case h_CO:
							parser->index++;
							parser->header_state = (c == 'n' ? h_CON : h_general);
							break;

						case h_CON:
							parser->index++;
							switch (c) {
							case 'n':
								parser->header_state = h_matching_connection;
								break;
							case 't':
								parser->header_state = h_matching_content_length;
								break;
							default:
								parser->header_state = h_general;
								break;
							}
							break;

							/* connection */

						case h_matching_connection:
							parser->index++;
							if (parser->index > sizeof(CONNECTION)-1
									|| c != CONNECTION[parser->index]) {
								parser->header_state = h_general;
							} else if (parser->index == sizeof(CONNECTION)-2) {
								parser->header_state = h_connection;
							}
							break;

							/* proxy-connection */

						case h_matching_proxy_connection:
							parser->index++;
							if (parser->index > sizeof(PROXY_CONNECTION)-1
									|| c != PROXY_CONNECTION[parser->index]) {
								parser->header_state = h_general;
							} else if (parser->index == sizeof(PROXY_CONNECTION)-2) {
								parser->header_state = h_connection;
							}
							break;

							/* content-length */

						case h_matching_content_length:
							parser->index++;
							if (parser->index > sizeof(CONTENT_LENGTH)-1
									|| c != CONTENT_LENGTH[parser->index]) {
								parser->header_state = h_general;
							} else if (parser->index == sizeof(CONTENT_LENGTH)-2) {
								parser->header_state = h_content_length;
							}
							break;

							/* transfer-encoding */

						case h_matching_transfer_encoding:
							parser->index++;
							if (parser->index > sizeof(TRANSFER_ENCODING)-1
									|| c != TRANSFER_ENCODING[parser->index]) {
								parser->header_state = h_general;
							} else if (parser->index == sizeof(TRANSFER_ENCODING)-2) {
								parser->header_state = h_transfer_encoding;
							}
							break;

							/* upgrade */

						case h_matching_upgrade:
							parser->index++;
							if (parser->index > sizeof(UPGRADE)-1
									|| c != UPGRADE[parser->index]) {
								parser->header_state = h_general;
							} else if (parser->index == sizeof(UPGRADE)-2) {
								parser->header_state = h_upgrade;
							}
							break;

						case h_connection:
						case h_content_length:
						case h_transfer_encoding:
						case h_upgrade:
							if (ch != ' ') parser->header_state = h_general;
							break;

						default:
							assert(0 && "Unknown header_state");
							break;
						}
						break;
					}

					if (ch == ':') {
						parser->state = s_header_value_discard_ws;
						CALLBACK_DATA(header_field);
						break;
					}

					if (ch == CR) {
						parser->state = s_header_almost_done;
						CALLBACK_DATA(header_field);
						break;
					}

					if (ch == LF) {
						parser->state = s_header_field_start;
						CALLBACK_DATA(header_field);
						break;
					}

					SET_ERRNO(HPE_INVALID_HEADER_TOKEN);
					goto error;
				}

				case s_header_value_discard_ws:
					if (ch == ' ' || ch == '\t') break;

					if (ch == CR) {
						parser->state = s_header_value_discard_ws_almost_done;
						break;
					}

					if (ch == LF) {
						parser->state = s_header_value_discard_lws;
						break;
					}

					/* FALLTHROUGH */

				case s_header_value_start:
				{
					MARK(header_value);

					parser->state = s_header_value;
					parser->index = 0;

					c = LOWER(ch);

					switch (parser->header_state) {
					case h_upgrade:
						parser->flags |= F_UPGRADE;
						parser->header_state = h_general;
						break;

					case h_transfer_encoding:
						/* looking for 'Transfer-Encoding: chunked' */
						if ('c' == c) {
							parser->header_state = h_matching_transfer_encoding_chunked;
						} else {
							parser->header_state = h_general;
						}
						break;

					case h_content_length:
						if (!IS_NUM(ch)) {
							SET_ERRNO(HPE_INVALID_CONTENT_LENGTH);
							goto error;
						}

						parser->content_length = ch - '0';
						break;

					case h_connection:
						/* looking for 'Connection: keep-alive' */
						if (c == 'k') {
							parser->header_state = h_matching_connection_keep_alive;
							/* looking for 'Connection: close' */
						} else if (c == 'c') {
							parser->header_state = h_matching_connection_close;
						} else {
							parser->header_state = h_general;
						}
						break;

					default:
						parser->header_state = h_general;
						break;
					}
					break;
				}

				case s_header_value:
				{

					if (ch == CR) {
						parser->state = s_header_almost_done;
						CALLBACK_DATA(header_value);
						break;
					}

					if (ch == LF) {
						parser->state = s_header_almost_done;
						CALLBACK_DATA_NOADVANCE(header_value);
						goto reexecute_byte;
					}

					c = LOWER(ch);

					switch (parser->header_state) {
					case h_general:
						break;

					case h_connection:
					case h_transfer_encoding:
						assert(0 && "Shouldn't get here.");
						break;

					case h_content_length:
					{
						uint64_t t;

						if (ch == ' ') break;

						if (!IS_NUM(ch)) {
							SET_ERRNO(HPE_INVALID_CONTENT_LENGTH);
							goto error;
						}

						t = parser->content_length;
						t *= 10;
						t += ch - '0';

						/* Overflow? Test against a conservative limit for simplicity. */
						if ((ULLONG_MAX - 10) / 10 < parser->content_length) {
							SET_ERRNO(HPE_INVALID_CONTENT_LENGTH);
							goto error;
						}

						parser->content_length = t;
						break;
					}

					/* Transfer-Encoding: chunked */
					case h_matching_transfer_encoding_chunked:
						parser->index++;
						if (parser->index > sizeof(CHUNKED)-1
								|| c != CHUNKED[parser->index]) {
							parser->header_state = h_general;
						} else if (parser->index == sizeof(CHUNKED)-2) {
							parser->header_state = h_transfer_encoding_chunked;
						}
						break;

						/* looking for 'Connection: keep-alive' */
					case h_matching_connection_keep_alive:
						parser->index++;
						if (parser->index > sizeof(KEEP_ALIVE)-1
								|| c != KEEP_ALIVE[parser->index]) {
							parser->header_state = h_general;
						} else if (parser->index == sizeof(KEEP_ALIVE)-2) {
							parser->header_state = h_connection_keep_alive;
						}
						break;

						/* looking for 'Connection: close' */
					case h_matching_connection_close:
						parser->index++;
						if (parser->index > sizeof(CLOSE)-1 || c != CLOSE[parser->index]) {
							parser->header_state = h_general;
						} else if (parser->index == sizeof(CLOSE)-2) {
							parser->header_state = h_connection_close;
						}
						break;

					case h_transfer_encoding_chunked:
					case h_connection_keep_alive:
					case h_connection_close:
						if (ch != ' ') parser->header_state = h_general;
						break;

					default:
						parser->state = s_header_value;
						parser->header_state = h_general;
						break;
					}
					break;
				}

				case s_header_almost_done:
				{
					STRICT_CHECK(ch != LF);

					parser->state = s_header_value_lws;
					break;
				}

				case s_header_value_lws:
				{
					if (ch == ' ' || ch == '\t') {
						parser->state = s_header_value_start;
						goto reexecute_byte;
					}

					/* finished the header */
					switch (parser->header_state) {
					case h_connection_keep_alive:
						parser->flags |= F_CONNECTION_KEEP_ALIVE;
						break;
					case h_connection_close:
						parser->flags |= F_CONNECTION_CLOSE;
						break;
					case h_transfer_encoding_chunked:
						parser->flags |= F_CHUNKED;
						break;
					default:
						break;
					}

					parser->state = s_header_field_start;
					goto reexecute_byte;
				}

				case s_header_value_discard_ws_almost_done:
				{
					STRICT_CHECK(ch != LF);
					parser->state = s_header_value_discard_lws;
					break;
				}

				case s_header_value_discard_lws:
				{
					if (ch == ' ' || ch == '\t') {
						parser->state = s_header_value_discard_ws;
						break;
					} else {
						/* header value was empty */
						MARK(header_value);
						parser->state = s_header_field_start;
						CALLBACK_DATA_NOADVANCE(header_value);
						goto reexecute_byte;
					}
				}

				case s_headers_almost_done:
				{
					STRICT_CHECK(ch != LF);

					if (parser->flags & F_TRAILING) {
						/* End of a chunked request */
						parser->state = NEW_MESSAGE();
						CALLBACK_NOTIFY(message_complete);
						break;
					}

					parser->state = s_headers_done;

					/* Set this here so that on_headers_complete() callbacks can see it */
					parser->upgrade =
							(parser->flags & F_UPGRADE || parser->method == HTTP_CONNECT);

					/* Here we call the headers_complete callback. This is somewhat
					 * different than other callbacks because if the user returns 1, we
					 * will interpret that as saying that this message has no body. This
					 * is needed for the annoying case of recieving a response to a HEAD
					 * request.
					 *
					 * We'd like to use CALLBACK_NOTIFY_NOADVANCE() here but we cannot, so
					 * we have to simulate it by handling a change in errno below.
					 */
					if (settings->on_headers_complete) {
						switch (settings->on_headers_complete(parser)) {
						case 0:
							break;

						case 1:
							parser->flags |= F_SKIPBODY;
							break;

						default:
							SET_ERRNO(HPE_CB_headers_complete);
							return p - data; /* Error */
						}
					}

					if (HTTP_PARSER_ERRNO(parser) != HPE_OK) {
						return p - data;
					}

					goto reexecute_byte;
				}

				case s_headers_done:
				{
					STRICT_CHECK(ch != LF);

					parser->nread = 0;

					/* Exit, the rest of the connect is in a different protocol. */
					if (parser->upgrade) {
						parser->state = NEW_MESSAGE();
						CALLBACK_NOTIFY(message_complete);
						return (p - data) + 1;
					}

					if (parser->flags & F_SKIPBODY) {
						parser->state = NEW_MESSAGE();
						CALLBACK_NOTIFY(message_complete);
					} else if (parser->flags & F_CHUNKED) {
						/* chunked encoding - ignore Content-Length header */
						parser->state = s_chunk_size_start;
					} else {
						if (parser->content_length == 0) {
							/* Content-Length header given but zero: Content-Length: 0\r\n */
							parser->state = NEW_MESSAGE();
							CALLBACK_NOTIFY(message_complete);
						} else if (parser->content_length != ULLONG_MAX) {
							/* Content-Length header given and non-zero */
							parser->state = s_body_identity;
						} else {
							if (parser->type == HTTP_REQUEST ||
									!http_message_needs_eof(parser)) {
								/* Assume content-length 0 - read the next */
								parser->state = NEW_MESSAGE();
								CALLBACK_NOTIFY(message_complete);
							} else {
								/* Read body until EOF */
								parser->state = s_body_identity_eof;
							}
						}
					}

					break;
				}

				case s_body_identity:
				{
					uint64_t to_read = MIN(parser->content_length,
							(uint64_t) ((data + len) - p));

					assert(parser->content_length != 0
							&& parser->content_length != ULLONG_MAX);

					/* The difference between advancing content_length and p is because
					 * the latter will automaticaly advance on the next loop iteration.
					 * Further, if content_length ends up at 0, we want to see the last
					 * byte again for our message complete callback.
					 */
					MARK(body);
					parser->content_length -= to_read;
					p += to_read - 1;

					if (parser->content_length == 0) {
						parser->state = s_message_done;

						/* Mimic CALLBACK_DATA_NOADVANCE() but with one extra byte.
						 *
						 * The alternative to doing this is to wait for the next byte to
						 * trigger the data callback, just as in every other case. The
						 * problem with this is that this makes it difficult for the test
						 * harness to distinguish between complete-on-EOF and
						 * complete-on-length. It's not clear that this distinction is
						 * important for applications, but let's keep it for now.
						 */
						CALLBACK_DATA_(body, p - body_mark + 1, p - data);
						goto reexecute_byte;
					}

					break;
				}

				/* read until EOF */
				case s_body_identity_eof:
					MARK(body);
					p = data + len - 1;

					break;

				case s_message_done:
					parser->state = NEW_MESSAGE();
					CALLBACK_NOTIFY(message_complete);
					break;

				case s_chunk_size_start:
				{
					assert(parser->nread == 1);
					assert(parser->flags & F_CHUNKED);

					unhex_val = unhex[(unsigned char)ch];
					if (unhex_val == -1) {
						SET_ERRNO(HPE_INVALID_CHUNK_SIZE);
						goto error;
					}

					parser->content_length = unhex_val;
					parser->state = s_chunk_size;
					break;
				}

				case s_chunk_size:
				{
					uint64_t t;

					assert(parser->flags & F_CHUNKED);

					if (ch == CR) {
						parser->state = s_chunk_size_almost_done;
						break;
					}

					unhex_val = unhex[(unsigned char)ch];

					if (unhex_val == -1) {
						if (ch == ';' || ch == ' ') {
							parser->state = s_chunk_parameters;
							break;
						}

						SET_ERRNO(HPE_INVALID_CHUNK_SIZE);
						goto error;
					}

					t = parser->content_length;
					t *= 16;
					t += unhex_val;

					/* Overflow? Test against a conservative limit for simplicity. */
					if ((ULLONG_MAX - 16) / 16 < parser->content_length) {
						SET_ERRNO(HPE_INVALID_CONTENT_LENGTH);
						goto error;
					}

					parser->content_length = t;
					break;
				}

				case s_chunk_parameters:
				{
					assert(parser->flags & F_CHUNKED);
					/* just ignore this shit. TODO check for overflow */
					if (ch == CR) {
						parser->state = s_chunk_size_almost_done;
						break;
					}
					break;
				}

				case s_chunk_size_almost_done:
				{
					assert(parser->flags & F_CHUNKED);
					STRICT_CHECK(ch != LF);

					parser->nread = 0;

					if (parser->content_length == 0) {
						parser->flags |= F_TRAILING;
						parser->state = s_header_field_start;
					} else {
						parser->state = s_chunk_data;
					}
					break;
				}

				case s_chunk_data:
				{
					uint64_t to_read = MIN(parser->content_length,
							(uint64_t) ((data + len) - p));

					assert(parser->flags & F_CHUNKED);
					assert(parser->content_length != 0
							&& parser->content_length != ULLONG_MAX);

					/* See the explanation in s_body_identity for why the content
					 * length and data pointers are managed this way.
					 */
					MARK(body);
					parser->content_length -= to_read;
					p += to_read - 1;

					if (parser->content_length == 0) {
						parser->state = s_chunk_data_almost_done;
					}

					break;
				}

				case s_chunk_data_almost_done:
					assert(parser->flags & F_CHUNKED);
					assert(parser->content_length == 0);
					STRICT_CHECK(ch != CR);
					parser->state = s_chunk_data_done;
					CALLBACK_DATA(body);
					break;

				case s_chunk_data_done:
					assert(parser->flags & F_CHUNKED);
					STRICT_CHECK(ch != LF);
					parser->nread = 0;
					parser->state = s_chunk_size_start;
					break;

				default:
					assert(0 && "unhandled state");
					SET_ERRNO(HPE_INVALID_INTERNAL_STATE);
					goto error;
				}
		}

		/* Run callbacks for any marks that we have leftover after we ran our of
		 * bytes. There should be at most one of these set, so it's OK to invoke
		 * them in series (unset marks will not result in callbacks).
		 *
		 * We use the NOADVANCE() variety of callbacks here because 'p' has already
		 * overflowed 'data' and this allows us to correct for the off-by-one that
		 * we'd otherwise have (since CALLBACK_DATA() is meant to be run with a 'p'
		 * value that's in-bounds).
		 */

		assert(((header_field_mark ? 1 : 0) +
				(header_value_mark ? 1 : 0) +
				(url_mark ? 1 : 0)  +
				(body_mark ? 1 : 0) +
				(status_mark ? 1 : 0)) <= 1);

		CALLBACK_DATA_NOADVANCE(header_field);
		CALLBACK_DATA_NOADVANCE(header_value);
		CALLBACK_DATA_NOADVANCE(url);
		CALLBACK_DATA_NOADVANCE(body);
		CALLBACK_DATA_NOADVANCE(status);

		return len;

		error:
			if (HTTP_PARSER_ERRNO(parser) == HPE_OK) {
				SET_ERRNO(HPE_UNKNOWN);
			}

		return (p - data);
	}

	/* If http_should_keep_alive() in the on_headers_complete or
	 * on_message_complete callback returns 0, then this should be
	 * the last message on the connection.
	 * If you are the server, respond with the "Connection: close" header.
	 * If you are the client, close the connection.
	 */
	///int http_should_keep_alive(const http_parser *parser);
	protected boolean http_should_keep_alive() {
		if (http_major > 0 && http_minor > 0) {
			/* HTTP/1.1 */
			if ((flags & Flags.F_CONNECTION_CLOSE.flag)!=0) {
				return false;
			}
		} else {
			/* HTTP/1.0 or earlier */
			if (0==(flags & Flags.F_CONNECTION_KEEP_ALIVE.flag)) {
				return false;
			}
		}

		return !http_message_needs_eof();
	}

	/* Returns a string version of the HTTP method. */
	///const char *http_method_str(enum http_method m);
	public static String http_method_str(http_method m) {
		return m.desc;
	}

	/* Return a string name of the given error */
	///const char *http_errno_name(enum http_errno err);
	public static String http_errno_name(http_errno err) {
		return err.name();
	}

	/* Return a string description of the given error */
	///const char *http_errno_description(enum http_errno err);
	public static String http_errno_description(http_errno err) {
		return err.desc;
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
	private State state;                /* enum State from http_parser.c */
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
