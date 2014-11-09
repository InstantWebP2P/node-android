package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iwebpp.crypto.TweetNaclFast;
import com.iwebpp.libuvpp.handles.TimerHandle;
import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.stream.Duplex;
import com.iwebpp.node.stream.Readable2;
import com.iwebpp.node.stream.Writable.WriteCB;
import com.iwebpp.node.stream.Writable2;
import com.iwebpp.wspp.Receiver.opcOptions;
import com.iwebpp.wspp.WebSocket.ErrorEvent;
import com.iwebpp.wspp.WebSocket.MessageEvent;
import com.iwebpp.wspp.WebSocket.OpenEvent;
import com.iwebpp.wspp.WebSocket.SendOptions;
import com.iwebpp.wspp.WebSocket.error_code_b;
import com.iwebpp.wspp.WebSocket.message_data_b;
import com.iwebpp.wspp.WebSocket.oncloseListener;
import com.iwebpp.wspp.WebSocket.onerrorListener;
import com.iwebpp.wspp.WebSocket.onmessageListener;
import com.iwebpp.wspp.WebSocket.onopenListener;

public final class SecureWebSocket 
extends EventEmitter2 {

	private static final String TAG = "SecureWebSocket";

	private static final int HANDSHAKE_TIMEOUT = 2000; // 2s

	private final WebSocket ws;
	private sws_state_t state;

	private TimerHandle hs_tmo;

	private static enum sws_state_t {
		SWS_STATE_NEW               (0),
		SWS_STATE_CONNECTED         (1),

		// hand shake process /////////
		SWS_STATE_HANDSHAKE_START   (2),

		// client -> server
		SWS_STATE_SEND_CLIENT_HELLO (3), 
		SWS_STATE_RECV_CLIENT_HELLO (4), 

		// server -> client
		SWS_STATE_SEND_SERVER_HELLO (5),
		SWS_STATE_RECV_SERVER_HELLO (6),

		// client -> server
		SWS_STATE_SEND_CLIENT_READY (7),
		SWS_STATE_RECV_CLIENT_READY (8),

		SWS_STATE_HANDSHAKE_DONE    (9);
		////////////////////////////////

		private int state;
		private sws_state_t(int state) {
			this.state = state;
		}
	}
	private static enum sws_opc_t {
		SWS_OPC_CLIENT_HELLO (0),
		SWS_OPC_SERVER_HELLO (1),
		SWS_OPC_CLIENT_READY (2);
		
		private int opc;
		private sws_opc_t(int opc) {
			this.opc = opc;
		}
		private int opc() {
			return this.opc;
		}
	}
	
	private boolean isServer;

	// protocol version 1
	private static final int PROTO_VERSION = 1;
	
	private SecInfo mySecInfo;

	private NodeContext context;

	private BinaryStream binaryStream;


	// Hand shake message beans	
	private class client_hello_b {
		public int opc;

		public int version; // sws protocol version: 1
		
		public byte[] nonce; // 8 bytes

		public byte[] client_public_key; // 32 bytes 
		
		public final static int nonceLength = 8; // 8 bytes
		
		public String stringify() throws JSONException {
			JSONObject json = new JSONObject();
			
			json.put("opc", opc);
			json.put("version", version);
			
			JSONArray na = new JSONArray(); 
			for (int i = 0; i < nonce.length; i ++)
				na.put(i, nonce[i]&0xff);
			json.put("nonce", na);
			
			JSONArray cpka = new JSONArray(); 
			for (int i = 0; i < client_public_key.length; i ++)
				cpka.put(i, client_public_key[i]&0xff);
			json.put("client_public_key", cpka);

			debug(TAG, "client_hello_b->:" + json.toString());
			
			return json.toString();
		}

		public client_hello_b(String jstr) throws JSONException {
			debug(TAG, "client_hello_b<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			this.nonce = new byte[nonceLength];
			JSONArray na = json.getJSONArray("nonce");
			for (int i = 0; i < na.length(); i ++)
				this.nonce[i] = (byte) (na.getInt(i)&0xff);

			this.client_public_key = new byte[TweetNaclFast.Box.publicKeyLength];
			JSONArray cpka = json.getJSONArray("client_public_key");
			for (int i = 0; i < cpka.length(); i ++)
				this.client_public_key[i] = (byte) (cpka.getInt(i)&0xff);
		}
		public client_hello_b() {}
	}
	
	private class server_hello_b {
		public int opc;

		public int version; // sws protocol version: 1
		
		public byte[] server_public_key;
		
		// encrypted nonce, share_key
		public byte[] nonce; // 8 bytes
		public byte[] share_key; 
		
		public final static int nonceLength = 8; // 8 bytes

		public String stringify() throws Exception {
			JSONObject json = new JSONObject();
			
			json.put("opc", opc);
			json.put("version", version);
			
			JSONArray spka = new JSONArray(); 
			for (int i = 0; i < server_public_key.length; i ++)
				spka.put(i, server_public_key[i]&0xff);
			json.put("server_public_key", spka);
			
			// encrypt nonce, share_key
			byte[] nonce_share_key = new byte[nonce.length+share_key.length];
			for (int i = 0; i < nonce.length; i ++)
				nonce_share_key[i] = nonce[i];
			
			for (int i = 0; i < share_key.length; i ++)
				nonce_share_key[i+nonce.length] = share_key[i];
			
			// Constructor temp NACL tx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					SecureWebSocket.this.theirPublicKey,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.theirNonce));

			byte[] s_nonce_share_key = tmpBox.box(nonce_share_key);
			if (!(s_nonce_share_key!=null && s_nonce_share_key.length==(nonce_share_key.length+TweetNaclFast.Box.overheadLength)))
				throw new Exception("server_hello_b encrypt nonce_share_key failed");
			
			// 
			JSONArray s_nonce_share_key_a = new JSONArray(); 
			for (int i = 0; i < s_nonce_share_key.length; i ++)
				s_nonce_share_key_a.put(i, s_nonce_share_key[i]&0xff);
			json.put("s_nonce_share_key_a", s_nonce_share_key_a);
			
			debug(TAG, "server_hello_b->:" + json.toString());

			return json.toString();
		}
		
		public server_hello_b(String jstr) throws Exception {
			debug(TAG, "server_hello_b<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			JSONArray spka = json.getJSONArray("server_public_key");
			this.server_public_key = new byte[spka.length()];

			for (int i = 0; i < spka.length(); i ++)
				this.server_public_key[i] = (byte) (spka.getInt(i)&0xff);

			// decrypt nonce, share_key
			JSONArray s_nonce_share_key_a = json.getJSONArray("s_nonce_share_key_a");
            byte[] s_nonce_share_key = new byte[s_nonce_share_key_a.length()];
			
			for (int i = 0; i < s_nonce_share_key_a.length(); i ++)
				s_nonce_share_key[i] = (byte) (s_nonce_share_key_a.getInt(i)&0xff);
			
			// Constructor temp NACL rx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					this.server_public_key,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.myNonce));
			
			byte[] nonce_share_key = tmpBox.open(s_nonce_share_key);
			if (!(nonce_share_key!=null && nonce_share_key.length==(s_nonce_share_key.length-TweetNaclFast.Box.overheadLength)))
				throw new Exception("server_hello_b decrypt nonce_share_key failed");
			
			// extract nonce
			this.nonce = new byte[nonceLength];
			for (int i = 0; i < nonce.length; i ++)
				this.nonce[i] = nonce_share_key[i];
			
			// extract shared key
			this.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
			for (int i = 0; i < share_key.length; i ++)
				this.share_key[i] = nonce_share_key[i+nonce.length];
		}
		
		public server_hello_b(){}
	}
	
	private class client_ready_b {
		public int opc;
		
		public int version; // sws protocol version: 1

		// encrypted nonce, share_key
		public byte[] nonce; // 8 bytes
		public byte[] share_key; 
		
		public final static int nonceLength = 8; // 8 bytes

		public String stringify() throws Exception {
			JSONObject json = new JSONObject();
			
			json.put("opc", opc);
			json.put("version", version);
			
			// encrypt nonce, share_key
			byte[] nonce_share_key = new byte[nonce.length+share_key.length];
			for (int i = 0; i < nonce.length; i ++)
				nonce_share_key[i] = nonce[i];
			
			for (int i = 0; i < share_key.length; i ++)
				nonce_share_key[i+nonce.length] = share_key[i];

			// Constructor temp NACL tx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					SecureWebSocket.this.theirPublicKey,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.theirNonce));

			byte[] s_nonce_share_key = tmpBox.box(nonce_share_key);
			if (!(s_nonce_share_key!=null && s_nonce_share_key.length==(nonce_share_key.length+TweetNaclFast.Box.overheadLength)))
				throw new Exception("client_ready_b encrypt nonce_share_key failed");
			
			// 
			JSONArray s_nonce_share_key_a = new JSONArray(); 
			for (int i = 0; i < s_nonce_share_key.length; i ++)
				s_nonce_share_key_a.put(i, s_nonce_share_key[i]&0xff);
			json.put("s_nonce_share_key_a", s_nonce_share_key_a);
			
			debug(TAG, "client_ready_b->:" + json.toString());

			return json.toString();
		}

		public client_ready_b(String jstr) throws Exception {
			debug(TAG, "client_ready_b<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			// decrypt nonce, share_key
			JSONArray s_nonce_share_key_a = json.getJSONArray("s_nonce_share_key_a");
            byte[] s_nonce_share_key = new byte[s_nonce_share_key_a.length()];
			
			for (int i = 0; i < s_nonce_share_key_a.length(); i ++)
				s_nonce_share_key[i] = (byte) (s_nonce_share_key_a.getInt(i)&0xff);

			// Constructor temp NACL rx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					SecureWebSocket.this.theirPublicKey,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.myNonce));
						
			byte[] nonce_share_key = tmpBox.open(s_nonce_share_key);
			if (!(nonce_share_key!=null && nonce_share_key.length==(s_nonce_share_key.length-TweetNaclFast.Box.overheadLength)))
				throw new Exception("server_hello_b decrypt nonce_share_key failed");
			
			// extract nonce
			this.nonce = new byte[nonceLength];
			for (int i = 0; i < nonce.length; i ++)
				this.nonce[i] = nonce_share_key[i];
			
			// extract shared key
			this.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
			for (int i = 0; i < share_key.length; i ++)
				this.share_key[i] = nonce_share_key[i+nonce.length];
		}
		
		public client_ready_b() {}
	}
	
	// secure info
	public static class SecInfo {
		private byte[] pk;
		private byte[] sk;
		private String cert;
		private String ca;
		private boolean rejectUnauthorized;

		public SecInfo(byte[] pk, byte[] sk) {
			this.pk = pk;
			this.sk = sk;

			this.cert = null;
			this.ca   = null;
			this.rejectUnauthorized = false;
		}
		public byte[] getPublicKey() {
			return this.pk;
		}
		public byte[] getSecretKey() {
			return this.sk;
		}
		public String getCert() {
			return this.cert;
		}
		public String getCa() {
			return this.ca;
		}
		public boolean isRejectUnauthorized() {
			return rejectUnauthorized;
		}
	}

	// Client constructor
	@SuppressWarnings("unused")
	public SecureWebSocket(final NodeContext ctx, String address, WebSocket.Options options, SecInfo sec) throws Exception {		
		// context
		this.context = ctx;
		
		// setup security info
		this.mySecInfo   = sec;
		this.myPublicKey = sec.getPublicKey();
		this.mySecretKey = sec.getSecretKey();
		this.ca          = sec.getCa();
		this.myCert      = sec.getCert();
		
		// check security info
		if (PROTO_VERSION >= 1) {
			if (!(this.myPublicKey!=null && this.myPublicKey.length==TweetNaclFast.Box.publicKeyLength))
				throw new Exception("Invalid nacl public key");
			
			if (!(this.mySecretKey!=null && this.mySecretKey.length==TweetNaclFast.Box.secretKeyLength))
				throw new Exception("Invalid nacl secret key");
		}
		if (PROTO_VERSION >= 2) {
			if (!(this.ca!=null))
				throw new Exception("Invalid nacl CA");
			
			if (!(this.myCert!=null))
				throw new Exception("Invalid nacl cert");
		}
		
		// client
		this.isServer = false;
		
		// FSM 
		this.state = sws_state_t.SWS_STATE_NEW;

		this.ws = new WebSocket(ctx, address, null, options);
		this.ws.onopen(new WebSocket.onopenListener() {

			@Override
			public void onOpen(OpenEvent event) throws Exception {
				// TODO Auto-generated method stub
				SecureWebSocket.this.state = sws_state_t.SWS_STATE_CONNECTED;

				SecureWebSocket.this.ws.onmessage(new WebSocket.onmessageListener() {

					@Override
					public void onMessage(MessageEvent event) throws Exception {
						debug(TAG, "Client got message:"+event.getData().toString());

						if (SecureWebSocket.this.state == sws_state_t.SWS_STATE_HANDSHAKE_DONE) {
							// Secure Context process
							if (event.isBinary()) {
								ByteBuffer bb = (ByteBuffer) event.getData();

								if (bb!=null && bb.capacity()>0) {
									// authenticated decrypt cipher to plain buffer
                                    byte[] plain = SecureWebSocket.this.rxSecretBox.open(bb.array(), bb.arrayOffset());
                                    		
                                    // check security
                                    if (plain != null) {
                                    	// increase nonce
                                    	SecureWebSocket.this.rxSecretBox.incrNonce();

                                    	// emit plain message
                                    	message_data_b msg = new message_data_b(ByteBuffer.wrap(plain), new Receiver.opcOptions(false, null, true));
                                    	SecureWebSocket.this.emit("message", msg);
                                    } else {
    									warn(TAG, "hacked ByteBuffer, ingore it");
    									SecureWebSocket.this.emit("warn", "hacked ByteBuffer, ingore it");
    								}
								} else {
									warn(TAG, "invalid ByteBuffer");
									SecureWebSocket.this.emit("warn", "invalid ByteBuffer");
								}
							} else {
								// TBD... String 
								warn(TAG, "don't support string so far");
								SecureWebSocket.this.emit("warn", "don't support string so far");
							}
						} else {
							// Handle Shake process
							if (SecureWebSocket.this.state == sws_state_t.SWS_STATE_SEND_CLIENT_HELLO) {
								// extract ServerHello message
								if (!event.isBinary()) {
									// capture JSON parse exception
									try {
										server_hello_b shm = new server_hello_b(event.getData().toString());

										if (shm.opc == sws_opc_t.SWS_OPC_SERVER_HELLO.opc() &&
											shm.version == PROTO_VERSION) {
											debug(TAG, "ServerHello message:"+event.getData().toString());

											// update secure info
											SecureWebSocket.this.theirPublicKey = shm.server_public_key;
											SecureWebSocket.this.rxSharekey = shm.share_key;
											SecureWebSocket.this.theirNonce = shm.nonce; 
											
											// send ClientReady message
											client_ready_b crm = new client_ready_b();
											crm.opc = sws_opc_t.SWS_OPC_CLIENT_READY.opc();
											crm.version = PROTO_VERSION;
											
											// nonce
											crm.nonce = new byte[client_hello_b.nonceLength];
											randombytes(crm.nonce, crm.nonce.length);
							                
							                // shared key
							                crm.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
											randombytes(crm.share_key, crm.share_key.length);
											
											// update secure info
											SecureWebSocket.this.myNonce = crm.nonce;
											SecureWebSocket.this.txShareKey = crm.share_key;
											
											// constructor NACL tx box
											SecureWebSocket.this.txBox = new TweetNaclFast.Box(
													SecureWebSocket.this.theirPublicKey, 
													SecureWebSocket.this.mySecretKey, 
													toLong(SecureWebSocket.this.myNonce));
											
											SecureWebSocket.this.txSecretBox = new TweetNaclFast.SecretBox(
													SecureWebSocket.this.txShareKey, 
													toLong(SecureWebSocket.this.myNonce));
											
											SecureWebSocket.this.ws.send(crm.stringify(), new WebSocket.SendOptions(false, false), new WriteCB(){

												@Override
												public void writeDone(String error) throws Exception {
													if (error != null) {
														SecureWebSocket.this.emit("error", "send_client_ready:"+error);
														// close ws
														SecureWebSocket.this.ws.close(0, null);
													} else {
														// clear hand shake timeout
														SecureWebSocket.this.hs_tmo.close();

														// update state to SWS_STATE_SEND_CLIENT_READY

														// Am ready  
														SecureWebSocket.this.state = sws_state_t.SWS_STATE_SEND_CLIENT_READY;

														ctx.setTimeout(new NodeContext.TimeoutListener() {

															@Override
															public void onTimeout() throws Exception {
																// construct NACL rx box
																SecureWebSocket.this.rxBox = new TweetNaclFast.Box(
																		SecureWebSocket.this.theirPublicKey, 
																		SecureWebSocket.this.mySecretKey, 
																		toLong(SecureWebSocket.this.theirNonce));
																
																SecureWebSocket.this.rxSecretBox = new TweetNaclFast.SecretBox(
																		SecureWebSocket.this.rxSharekey, 
																		toLong(SecureWebSocket.this.theirNonce));

																// set hand shake done
																SecureWebSocket.this.state = sws_state_t.SWS_STATE_HANDSHAKE_DONE;

                                                                // Flush sendCache
																for (send_cache_b c : SecureWebSocket.this.sendCache)
																	SecureWebSocket.this.send(c.chunk, c.options, c.cb);
																SecureWebSocket.this.sendCache.clear();
																
																// emit Secure event
																SecureWebSocket.this.emit("secure");
															}

														}, 50); // 50ms delay
													}					
												}
											});
										} else {
											SecureWebSocket.this.emit("warn", "invalid handshake server-hello message:"+event.getData().toString());
										}
									} catch (Exception e) {
										SecureWebSocket.this.emit("warn", e.toString()+"parse handshake message failed, skip it:"+event.getData().toString());
									} 
								} else {
									SecureWebSocket.this.emit("warn", "invalid handshake binary message:"+event.getData().toString());
								}
							} else {
								SecureWebSocket.this.emit("warn", "unknown handshake message:"+event.getData().toString());
							}
						}
					}

				});

				// 1.
				// send ClientHello message
				client_hello_b chm = new client_hello_b();
				chm.opc = sws_opc_t.SWS_OPC_CLIENT_HELLO.opc();
				chm.version = PROTO_VERSION;
				chm.client_public_key = SecureWebSocket.this.myPublicKey;
				
				chm.nonce = new byte[client_hello_b.nonceLength];
				randombytes(chm.nonce, chm.nonce.length);

				// update secure info
				SecureWebSocket.this.myNonce = chm.nonce;
				
				SecureWebSocket.this.ws.send(chm.stringify(), new WebSocket.SendOptions(false, false), new WriteCB(){

					@Override
					public void writeDone(String error) throws Exception {
						if (error != null) {
							SecureWebSocket.this.emit("error", "send_client_hello:"+error);
							// close ws
							SecureWebSocket.this.ws.close(0, null);
						} else {
							// update state to SWS_STATE_SEND_CLIENT_HELLO
							SecureWebSocket.this.state = sws_state_t.SWS_STATE_SEND_CLIENT_HELLO;
						}					
					}

				});

				// update state to SWS_STATE_HANDSHAKE_START
				SecureWebSocket.this.state = sws_state_t.SWS_STATE_HANDSHAKE_START;
				
				// 2.
				// start hand-shake timer
				SecureWebSocket.this.hs_tmo = ctx.setTimeout(new NodeContext.TimeoutListener() {

					@Override
					public void onTimeout() throws Exception {
						if (SecureWebSocket.this.state != sws_state_t.SWS_STATE_HANDSHAKE_DONE) {
							debug(TAG, "handshake timeout");

							SecureWebSocket.this.emit("timeout", "handshake timeout");

							// close ws
							SecureWebSocket.this.ws.close(0, null);
						}
					}

				}, HANDSHAKE_TIMEOUT);
				
			}

		});

		// Send cache
		this.sendCache = new LinkedList<send_cache_b>();
	}

	// ServerClient constructor
	@SuppressWarnings("unused")
	protected SecureWebSocket(final NodeContext ctx, WebSocket ws, SecInfo sec) throws Exception {
		// setup security info
		this.mySecInfo   = sec;
		this.myPublicKey = sec.getPublicKey();
		this.mySecretKey = sec.getSecretKey();
		this.ca          = sec.getCa();
		this.myCert      = sec.getCert();
		
		// check security info
		if (PROTO_VERSION >= 1) {
			if (!(this.myPublicKey!=null && this.myPublicKey.length==TweetNaclFast.Box.publicKeyLength))
				throw new Exception("Invalid nacl public key");
			
			if (!(this.mySecretKey!=null && this.mySecretKey.length==TweetNaclFast.Box.secretKeyLength))
				throw new Exception("Invalid nacl secret key");
		}
		if (PROTO_VERSION >= 2) {
			if (!(this.ca!=null))
				throw new Exception("Invalid nacl CA");
			
			if (!(this.myCert!=null))
				throw new Exception("Invalid nacl cert");
		}
		
		// server client
		this.isServer = true;
		
		// FSM 
		this.state = sws_state_t.SWS_STATE_NEW;

		this.ws = ws;
		
		// Hand shake process
		{
			SecureWebSocket.this.state = sws_state_t.SWS_STATE_CONNECTED;

			SecureWebSocket.this.ws.onmessage(new WebSocket.onmessageListener() {

				@Override
				public void onMessage(MessageEvent event) throws Exception {
					debug(TAG, "ServerClient got message:"+event.getData().toString());
					
					if (SecureWebSocket.this.state == sws_state_t.SWS_STATE_HANDSHAKE_DONE) {
						// Secure Context process
						if (event.isBinary()) {
							ByteBuffer bb = (ByteBuffer) event.getData();

							if (bb!=null && bb.capacity()>0) {
								// authenticated decrypt cipher to plain buffer
								byte[] plain = SecureWebSocket.this.rxSecretBox.open(bb.array(), bb.arrayOffset());

								// check security
								if (plain != null) {
									// increase nonce
									SecureWebSocket.this.rxSecretBox.incrNonce();

									// emit plain message
									message_data_b msg = new message_data_b(ByteBuffer.wrap(plain), new Receiver.opcOptions(false, null, true));
									SecureWebSocket.this.emit("message", msg);
								} else {
									warn(TAG, "hacked ByteBuffer, ingore it");
									SecureWebSocket.this.emit("warn", "hacked ByteBuffer, ingore it");
								}
							} else {
								warn(TAG, "invalid ByteBuffer");
								SecureWebSocket.this.emit("warn", "invalid ByteBuffer");
							}
						} else {
							// TBD... String 
							warn(TAG, "don't support string so far");
							SecureWebSocket.this.emit("warn", "don't support string so far");
						}
					} else {
						// Handle Shake process
						if (SecureWebSocket.this.state == sws_state_t.SWS_STATE_HANDSHAKE_START) {
							// extract ClientHello message
							if (!event.isBinary()) {
								// capture JSON parse exception
								try {
									client_hello_b chm = new client_hello_b(event.getData().toString());

									if (chm.opc == sws_opc_t.SWS_OPC_CLIENT_HELLO.opc() &&
										chm.version == PROTO_VERSION) {
										debug(TAG, "ClientHello message:"+event.getData().toString());

										// update secure info
										SecureWebSocket.this.theirPublicKey = chm.client_public_key;
										SecureWebSocket.this.theirNonce = chm.nonce; 

										// send ServerHello message
										server_hello_b shm = new server_hello_b();
										shm.opc = sws_opc_t.SWS_OPC_SERVER_HELLO.opc();
										shm.version = PROTO_VERSION;

										// nonce
										shm.nonce = new byte[server_hello_b.nonceLength];
										randombytes(shm.nonce, shm.nonce.length);

										// shared key
										shm.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
										randombytes(shm.share_key, shm.share_key.length);

										// server public key
										shm.server_public_key = SecureWebSocket.this.myPublicKey;
										
										// update secure info
										SecureWebSocket.this.myNonce = shm.nonce;
										SecureWebSocket.this.txShareKey = shm.share_key;
										
										debug(TAG, "ServerHello message:"+shm.toString());

										// construct NACL tx box
										SecureWebSocket.this.txBox = new TweetNaclFast.Box(
												SecureWebSocket.this.theirPublicKey,
												SecureWebSocket.this.mySecretKey,
												toLong(SecureWebSocket.this.myNonce));

										SecureWebSocket.this.txSecretBox = new TweetNaclFast.SecretBox(
												SecureWebSocket.this.txShareKey, 
												toLong(SecureWebSocket.this.myNonce));

										SecureWebSocket.this.ws.send(shm.stringify(), new WebSocket.SendOptions(false, false), new WriteCB(){

											@Override
											public void writeDone(String error) throws Exception {
												if (error != null) {
													SecureWebSocket.this.emit("error", "send_server_hello:"+error);
													// close ws
													SecureWebSocket.this.ws.close(0, null);
												} else {
													// update state to SWS_STATE_SEND_SERVER_HELLO
													SecureWebSocket.this.state = sws_state_t.SWS_STATE_SEND_SERVER_HELLO;
												}					
											}
										});
									} else {
										SecureWebSocket.this.emit("warn", "invalid handshake client-hello message:"+event.getData().toString());
									}
								} catch (Exception e) {
									SecureWebSocket.this.emit("warn", e.toString()+"parse handshake message failed, skip it:"+event.getData().toString());
								} 
							} else {
								SecureWebSocket.this.emit("warn", "invalid handshake binary message:"+event.getData().toString());
							}
						} else if (SecureWebSocket.this.state == sws_state_t.SWS_STATE_SEND_SERVER_HELLO) {
							// extract ClientHello message
							if (!event.isBinary()) {
								// capture JSON parse exception
								try {
									client_ready_b crm = new client_ready_b(event.getData().toString());

									if (crm.opc == sws_opc_t.SWS_OPC_CLIENT_READY.opc() &&
										crm.version == PROTO_VERSION) {
										debug(TAG, "ClientReady message:"+event.getData().toString());

										// clear hand shake timeout
										SecureWebSocket.this.hs_tmo.close();

										// update secure info
										SecureWebSocket.this.rxSharekey = crm.share_key;
										SecureWebSocket.this.theirNonce = crm.nonce; 

										// construct NACL rx box
										SecureWebSocket.this.rxBox = new TweetNaclFast.Box(
												SecureWebSocket.this.theirPublicKey, 
												SecureWebSocket.this.mySecretKey, 
												toLong(SecureWebSocket.this.theirNonce));
										
										SecureWebSocket.this.rxSecretBox = new TweetNaclFast.SecretBox(
												SecureWebSocket.this.rxSharekey, 
												toLong(SecureWebSocket.this.theirNonce));

										// set hand shake done
										SecureWebSocket.this.state = sws_state_t.SWS_STATE_HANDSHAKE_DONE;

										// Flush sendCache
										for (send_cache_b c : SecureWebSocket.this.sendCache)
											SecureWebSocket.this.send(c.chunk, c.options, c.cb);
										SecureWebSocket.this.sendCache.clear();

										// emit Secure event
										SecureWebSocket.this.emit("secure");
									} else {
										SecureWebSocket.this.emit("warn", "invalid handshake client-ready message:"+event.getData().toString());
									}
								} catch (Exception e) {
									SecureWebSocket.this.emit("warn", e.toString()+"parse handshake message failed, skip it:"+event.getData().toString());
								} 
							} else {
								SecureWebSocket.this.emit("warn", "invalid handshake binary message:"+event.getData().toString());
							}
						} else {
							SecureWebSocket.this.emit("warn", "unknown handshake message:"+event.getData().toString());
						}
					}
				}

			});

			// update state to SWS_STATE_HANDSHAKE_START
			SecureWebSocket.this.state = sws_state_t.SWS_STATE_HANDSHAKE_START;
		}

		// start hand-shake timer
		this.hs_tmo = ctx.setTimeout(new NodeContext.TimeoutListener() {

			@Override
			public void onTimeout() throws Exception {
				if (SecureWebSocket.this.state != sws_state_t.SWS_STATE_HANDSHAKE_DONE) {
					debug(TAG, "handshake timeout");
					
					SecureWebSocket.this.emit("timeout", "handshake timeout");

					// close ws
					SecureWebSocket.this.ws.close(0, null);
				}
			}

		}, HANDSHAKE_TIMEOUT);
		
		// Send cache
		this.sendCache = new LinkedList<send_cache_b>();
	}
	
	public void onclose(final oncloseListener cb) throws Exception {
		this.ws.onclose(cb);
	}
	
	public void onerror(final onerrorListener cb) throws Exception {
		this.ws.onerror(cb);

		this.on("error", new Listener() {

			@Override
			public void onEvent(Object data) throws Exception {
				cb.onError(new ErrorEvent(-1, data!=null? data.toString():"unknown", SecureWebSocket.this.ws));				
			}

		});
	}
	
	public void onmessage(final onmessageListener cb) throws Exception {
		final SecureWebSocket self = this;
				
        this.removeListener("message");
        
        this.on("message", new Listener(){

			@Override
			public void onEvent(Object raw) throws Exception {
				message_data_b data = (message_data_b)raw;
				
                cb.onMessage(new MessageEvent(data.data, data.flags.binary ? "Binary" : "Text", self.ws));				
			}
        	
        });
	}
	
	public void onopen(final onopenListener cb) throws Exception {
		final SecureWebSocket self = this;
		
        this.removeListener("secure");
        
        this.on("secure", new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
                cb.onOpen(new OpenEvent(self.ws));				
			}
        	
        });
	}
	
	/*
	 * @description Duplex stream wrapper 
	 * */
	public static class BinaryStream extends Duplex {
		private static final String TAG = "SecureWebSocket:BinaryStream";

		private final SecureWebSocket host;
		
		protected BinaryStream(NodeContext ctx, SecureWebSocket host) throws Exception {
			super(ctx, new Duplex.Options(
					new Readable2.Options(-1, null, false, "utf8", true), 
					new Writable2.Options(-1, false, "utf8", false, true), 
					false));
			// TODO Auto-generated constructor stub

			this.host = host;
			
			// capture plain message
			this.host.onmessage(new onmessageListener(){

				@Override
				public void onMessage(MessageEvent event) throws Exception {
					if (event.isBinary()) {
						// push data with pause
						if (!BinaryStream.this.push(event.getData(), null)) {
							BinaryStream.this.host.pause();
                		}
					} else {
						// TBD... String
						warn(TAG, "don't support string");
						BinaryStream.this.emit("warn", "don't support string");
					}
				}
				
			});
		}

		@Override
		protected void _read(int size) throws Exception {
			if (this.host.state == sws_state_t.SWS_STATE_HANDSHAKE_DONE)
				this.host.resume();

			return;
		}

		@Override
		protected void _write(Object chunk, String encoding, WriteCB cb)
				throws Exception {
			if (chunk!=null && chunk instanceof ByteBuffer) {
				this.host.send(chunk, new SendOptions(true, false), cb);
			} else {
				// TBD... String
				warn(TAG, "Not support write string");
				cb.writeDone("Not support write string");
			}
		}

	}

	public BinaryStream binaryStream() throws Exception {
		if (this.binaryStream == null)
			this.binaryStream = new BinaryStream(this.context, this);
		
		return this.binaryStream;
	}
	
	/*
	 * @description expose WebSocket API
	 * */
	public void pause() throws Exception {
		this.ws.pause();
	}
	public void resume() throws Exception {
		this.ws.resume();
	}
	public void terminate() throws Exception {
		this.ws.terminate();
	}
	public String getUrl() {
		return this.ws.getUrl();	
	}
	public int getBytesReceived() {
		return this.ws.getBytesReceived();	
	}
	public int bufferedAmount() {
		return this.ws.bufferedAmount();
	}
	
	public void close(int code, Object data) throws Exception {
		this.ws.close(code, data);	
	}

	/* 
	 * @description override send method
	 * */
	public boolean send(final Object chunk, final SendOptions options, final WriteCB cb) throws Exception {
		if (this.state == sws_state_t.SWS_STATE_HANDSHAKE_DONE) {
			if (chunk!=null) {
				if (options.binary) {
					ByteBuffer bb;

					if (chunk instanceof ByteBuffer)
						bb = (ByteBuffer) chunk;
					else if (chunk instanceof byte[])
						bb = ByteBuffer.wrap((byte[]) chunk);
					else {
						warn(TAG, "Not support binary");
						SecureWebSocket.this.emit("warn", "Not support binary");
						
						if (cb!=null) cb.writeDone("Not support binary");
						return true;
					}
					
					// authenticated encrypt plain to cipher buffer
					byte[] cipher = SecureWebSocket.this.txSecretBox.box(bb.array(), bb.arrayOffset());

					// check security
					if (cipher != null) {
						// increase nonce
						SecureWebSocket.this.txSecretBox.incrNonce();

						// write data out
						SecureWebSocket.this.ws.send(ByteBuffer.wrap(cipher), new WebSocket.SendOptions(true, false), cb);
					} else {
						warn(TAG, "hacked write ByteBuffer, ingore it");
						SecureWebSocket.this.emit("warn", "hacked write ByteBuffer, ingore it");
						
						if (cb!=null) cb.writeDone("hacked write ByteBuffer, ingore it");
					}
				} else {
					// TBD... String 
					warn(TAG, "don't support write string so far");
					SecureWebSocket.this.emit("warn", "don't support write string so far");
					
					if (cb!=null) cb.writeDone("don't support write string so far");
				}
			} else {
				warn(TAG, "invalid write data");
				SecureWebSocket.this.emit("warn", "invalid write data");
				
				if (cb!=null) cb.writeDone("invalid write data");
			}
		} else {
			// 3.
			// cache Send
			debug(TAG, "cache Send");
			this.sendCache.add(new send_cache_b(chunk, options, cb));
			return false;
		}
		
		return true;
	}

	private List<send_cache_b> sendCache;
	private class send_cache_b {
		private Object chunk;
		private SendOptions options;
		private WriteCB cb;

		private send_cache_b(Object chunk, SendOptions options, WriteCB cb) {
			this.chunk = chunk;
			this.options = options;
			this.cb = cb;
		}
	}
	
	// NACL authenticated encryption
	private byte[] mySecretKey;
	private byte[] myPublicKey;
	private String myCert;
	
	private byte[] theirPublicKey;
	private String theirCert;
	
	private String ca;
	private byte[] caPublicKey;
	
	private byte[] myNonce;
	private byte[] theirNonce;
	
	private byte[] txShareKey;
	private byte[] rxSharekey;
	
	private TweetNaclFast.Box txBox;
	private TweetNaclFast.Box rxBox;

	private TweetNaclFast.SecretBox txSecretBox;
	private TweetNaclFast.SecretBox rxSecretBox;

	private TweetNaclFast.Signature signature;
	
	/*
	 * @description
	 *   Java Random generator
	 * */
	private static final Random jrandom = new Random();

	private static void randombytes(byte [] x, int len) {
		int ret = len % 8;
		long rnd;

		for (int i = 0; i < len-ret; i += 8) {
			rnd = jrandom.nextLong();

			x[i+0] = (byte) (rnd >>>  0);
			x[i+1] = (byte) (rnd >>>  8);
			x[i+2] = (byte) (rnd >>> 16);
			x[i+3] = (byte) (rnd >>> 24);
			x[i+4] = (byte) (rnd >>> 32);
			x[i+5] = (byte) (rnd >>> 40);
			x[i+6] = (byte) (rnd >>> 48);
			x[i+7] = (byte) (rnd >>> 56);
		}

		if (ret > 0) {         
			rnd = jrandom.nextLong();
			for (int i = len-ret; i < len; i ++)
				x[i] = (byte) (rnd >>> 8*i);
		}
	}
	
	private long toLong(byte[] x) {
		long ret = 0;
		
		for (int i = 0; i < x.length; i ++) {
			long tmp = x[i] & 0xffL;
			ret |= tmp << (8*(7-i%8));
		}
		
		return ret;
	}
	
}
