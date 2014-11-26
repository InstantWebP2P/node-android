// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.wspp;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iwebpp.crypto.NaclCert;
import com.iwebpp.crypto.TweetNaclFast;
import com.iwebpp.libuvpp.handles.TimerHandle;
import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Url;
import com.iwebpp.node.Url.UrlObj;
import com.iwebpp.node.stream.Duplex;
import com.iwebpp.node.stream.Readable2;
import com.iwebpp.node.stream.Writable.WriteCB;
import com.iwebpp.node.stream.Writable2;
import com.iwebpp.wspp.WebSocket.ErrorEvent;
import com.iwebpp.wspp.WebSocket.MessageEvent;
import com.iwebpp.wspp.WebSocket.OpenEvent;
import com.iwebpp.wspp.WebSocket.SendOptions;
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
	private String url;
    private NaclCert.Cert peerCert;
	
	// protocol version default as 1
	private int PROTO_VERSION = 1;
	
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

		public JSONObject toJSON() throws JSONException {
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

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "client_hello_b->:" + jstr);

			return jstr;
		}
		
		@SuppressWarnings("unused")
		public client_hello_b(JSONObject json) throws JSONException {
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
	
	// client hello message V2 same as V1
	
	private class server_hello_b {
		public int opc;

		public int version; // sws protocol version: 1
		
		public byte[] server_public_key;
		
		// encrypted nonce, share_key
		public byte[] nonce; // 8 bytes
		public byte[] share_key; 
		
		public final static int nonceLength = 8; // 8 bytes

		public JSONObject toJSON() throws Exception {
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
			json.put("s_blackbox_a", s_nonce_share_key_a);

			return json;
		}
		
		public String stringify() throws Exception {
			String jstr = toJSON().toString();
			
			debug(TAG, "server_hello_b->:" + jstr);

			return jstr;
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
			JSONArray s_nonce_share_key_a = json.getJSONArray("s_blackbox_a");
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
	// server hello message V2
	private class server_hello_b_v2 {
		public int opc;

		public int version; // sws protocol version: 1
		
		public byte[] server_public_key;
		
		// encrypted nonce, share_key
		public byte[] nonce; // 8 bytes
		public byte[] share_key; 
		
		public final static int nonceLength = 8; // 8 bytes
		
		public NaclCert.Cert cert;
		public boolean requireCert;
		
		public JSONObject toJSON() throws Exception {
			JSONObject json = new JSONObject();

			json.put("opc", opc);
			json.put("version", version);

			JSONArray spka = new JSONArray(); 
			for (int i = 0; i < server_public_key.length; i ++)
				spka.put(i, server_public_key[i]&0xff);
			json.put("server_public_key", spka);

			// cert, requireCert
			JSONObject crobj = new JSONObject();
			crobj.put("requireCert", requireCert);
			crobj.put("cert", cert.toJSON());
			String crstr = crobj.toString();
			byte [] crbuf = crstr.getBytes("utf-8");

			// encrypt nonce, share_key, cert, requireCert
			byte[] nonce_share_key_cert_requirecert = new byte[nonce.length+share_key.length+crbuf.length];
			for (int i = 0; i < nonce.length; i ++)
				nonce_share_key_cert_requirecert[i] = nonce[i];

			for (int i = 0; i < share_key.length; i ++)
				nonce_share_key_cert_requirecert[i+nonce.length] = share_key[i];
			
			for (int i = 0; i < crbuf.length; i ++)
				nonce_share_key_cert_requirecert[i+nonce.length+share_key.length] = crbuf[i];

			// Constructor temp NACL tx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					SecureWebSocket.this.theirPublicKey,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.theirNonce));

			byte[] s_nonce_share_key_cert_requirecert = tmpBox.box(nonce_share_key_cert_requirecert);
			if (!(s_nonce_share_key_cert_requirecert!=null && 
				  s_nonce_share_key_cert_requirecert.length==
				 (nonce_share_key_cert_requirecert.length+TweetNaclFast.Box.overheadLength)))
				throw new Exception("server_hello_b_v2 encrypt nonce_share_key failed");

			// 
			JSONArray s_nonce_share_key_cert_requirecert_a = new JSONArray(); 
			for (int i = 0; i < s_nonce_share_key_cert_requirecert.length; i ++)
				s_nonce_share_key_cert_requirecert_a.put(i, s_nonce_share_key_cert_requirecert[i]&0xff);
			json.put("s_blackbox_a", s_nonce_share_key_cert_requirecert_a);
			
			return json;
		}
		
		public String stringify() throws Exception {
			String jstr = toJSON().toString();
			
			debug(TAG, "server_hello_b_v2->:" + jstr);

			return jstr;
		}
		
		
		@SuppressWarnings("unused")
		public server_hello_b_v2(String jstr) throws Exception {
			debug(TAG, "server_hello_b_v2<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			JSONArray spka = json.getJSONArray("server_public_key");
			this.server_public_key = new byte[spka.length()];

			for (int i = 0; i < spka.length(); i ++)
				this.server_public_key[i] = (byte) (spka.getInt(i)&0xff);

			// decrypt nonce, share_key, cert, requirecert
			JSONArray s_nonce_share_key_cert_requirecert_a = json.getJSONArray("s_blackbox_a");
			byte[] s_nonce_share_key_cert_requirecert = new byte[s_nonce_share_key_cert_requirecert_a.length()];

			for (int i = 0; i < s_nonce_share_key_cert_requirecert_a.length(); i ++)
				s_nonce_share_key_cert_requirecert[i] = (byte) (s_nonce_share_key_cert_requirecert_a.getInt(i)&0xff);

			// Constructor temp NACL rx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					this.server_public_key,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.myNonce));

			byte[] nonce_share_key_cert_requirecert = tmpBox.open(s_nonce_share_key_cert_requirecert);
			if (!(nonce_share_key_cert_requirecert!=null && 
				  nonce_share_key_cert_requirecert.length==(s_nonce_share_key_cert_requirecert.length-TweetNaclFast.Box.overheadLength)))
				throw new Exception("server_hello_b_v2 decrypt nonce_share_key failed");

			// extract nonce
			this.nonce = new byte[nonceLength];
			for (int i = 0; i < nonce.length; i ++)
				this.nonce[i] = nonce_share_key_cert_requirecert[i];

			// extract shared key
			this.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
			for (int i = 0; i < share_key.length; i ++)
				this.share_key[i] = nonce_share_key_cert_requirecert[i+nonce.length];

			// extract cert, requireCert
			byte[] crbuf = new byte[nonce_share_key_cert_requirecert.length - nonce.length - share_key.length];
			for (int i = 0; i < crbuf.length; i ++)
				crbuf[i] = nonce_share_key_cert_requirecert[i+nonce.length+share_key.length];
			JSONObject crobj = new JSONObject(new String(crbuf, "utf-8"));
			
			// cert
			this.cert = NaclCert.Cert.parse(crobj.getJSONObject("cert"));

			// requireCert
			this.requireCert = crobj.getBoolean("requireCert");
		}
		
		@SuppressWarnings("unused")
		public server_hello_b_v2(){}
	}

	private class client_ready_b {
		public int opc;

		public int version; // sws protocol version: 1

		// encrypted nonce, share_key
		public byte[] nonce; // 8 bytes
		public byte[] share_key; 

		public final static int nonceLength = 8; // 8 bytes

		public JSONObject toJSON() throws Exception {
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
			json.put("s_blackbox_a", s_nonce_share_key_a);

			return json;
		}

		public String stringify() throws Exception {
			String jstr = toJSON().toString();

			debug(TAG, "client_ready_b->:" + jstr);

			return jstr;
		}

		@SuppressWarnings("unused")
		public client_ready_b(JSONObject json) throws Exception {
			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			// decrypt nonce, share_key
			JSONArray s_nonce_share_key_a = json.getJSONArray("s_blackbox_a");
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
				throw new Exception("client_ready_b decrypt nonce_share_key failed");

			// extract nonce
			this.nonce = new byte[nonceLength];
			for (int i = 0; i < nonce.length; i ++)
				this.nonce[i] = nonce_share_key[i];

			// extract shared key
			this.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
			for (int i = 0; i < share_key.length; i ++)
				this.share_key[i] = nonce_share_key[i+nonce.length];
		}

		public client_ready_b(String jstr) throws Exception {
			debug(TAG, "client_ready_b<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			// decrypt nonce, share_key
			JSONArray s_nonce_share_key_a = json.getJSONArray("s_blackbox_a");
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
				throw new Exception("client_ready_b decrypt nonce_share_key failed");

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
	// client ready message V2
	private class client_ready_b_v2 {
		public int opc;

		public int version; // sws protocol version: 1

		// encrypted nonce, share_key
		public byte[] nonce; // 8 bytes
		public byte[] share_key; 

		public final static int nonceLength = 8; // 8 bytes
		
		public NaclCert.Cert cert;

		public JSONObject toJSON() throws Exception {
			JSONObject json = new JSONObject();

			json.put("opc", opc);
			json.put("version", version);

			// stringify cert
			String certstr = cert!=null ? cert.toJSON().toString() : "{}";
			byte[] certbuf = certstr.getBytes("utf-8");
			
			// encrypt nonce, share_key, cert
			byte[] nonce_share_key_cert = new byte[nonce.length+share_key.length+certbuf.length];
			for (int i = 0; i < nonce.length; i ++)
				nonce_share_key_cert[i] = nonce[i];

			for (int i = 0; i < share_key.length; i ++)
				nonce_share_key_cert[i+nonce.length] = share_key[i];

			for (int i = 0; i < certbuf.length; i ++)
				nonce_share_key_cert[i+nonce.length+share_key.length] = certbuf[i];
			
			// Constructor temp NACL tx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					SecureWebSocket.this.theirPublicKey,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.theirNonce));

			byte[] s_nonce_share_key_cert = tmpBox.box(nonce_share_key_cert);
			if (!(s_nonce_share_key_cert!=null && s_nonce_share_key_cert.length==(nonce_share_key_cert.length+TweetNaclFast.Box.overheadLength)))
				throw new Exception("client_ready_b_v2 encrypt nonce_share_key failed");

			// 
			JSONArray s_nonce_share_key_cert_a = new JSONArray(); 
			for (int i = 0; i < s_nonce_share_key_cert.length; i ++)
				s_nonce_share_key_cert_a.put(i, s_nonce_share_key_cert[i]&0xff);
			json.put("s_blackbox_a", s_nonce_share_key_cert_a);

			return json;
		}

		public String stringify() throws Exception {
			String jstr = toJSON().toString();

			debug(TAG, "client_ready_b_v2->:" + jstr);

			return jstr;
		}

		public client_ready_b_v2(String jstr) throws Exception {
			debug(TAG, "client_ready_b_v2<-:" + jstr);

			JSONObject json = new JSONObject(jstr);			

			this.opc = json.getInt("opc");
			this.version = json.getInt("version");

			// decrypt nonce, share_key, cert
			JSONArray s_nonce_share_key_cert_a = json.getJSONArray("s_blackbox_a");
			byte[] s_nonce_share_key_cert = new byte[s_nonce_share_key_cert_a.length()];

			for (int i = 0; i < s_nonce_share_key_cert_a.length(); i ++)
				s_nonce_share_key_cert[i] = (byte) (s_nonce_share_key_cert_a.getInt(i)&0xff);

			// Constructor temp NACL rx box
			TweetNaclFast.Box tmpBox = new TweetNaclFast.Box(
					SecureWebSocket.this.theirPublicKey,
					SecureWebSocket.this.mySecretKey,
					toLong(SecureWebSocket.this.myNonce));

			byte[] nonce_share_key_cert = tmpBox.open(s_nonce_share_key_cert);
			if (!(nonce_share_key_cert!=null && nonce_share_key_cert.length==(s_nonce_share_key_cert.length-TweetNaclFast.Box.overheadLength)))
				throw new Exception("client_ready_b_v2 decrypt nonce_share_key failed");

			// extract nonce
			this.nonce = new byte[nonceLength];
			for (int i = 0; i < nonce.length; i ++)
				this.nonce[i] = nonce_share_key_cert[i];

			// extract shared key
			this.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
			for (int i = 0; i < share_key.length; i ++)
				this.share_key[i] = nonce_share_key_cert[i+nonce.length];

			// extract cert
			byte [] certbuf = new byte[nonce_share_key_cert.length - nonce.length - share_key.length];
			for (int i = 0; i < certbuf.length; i ++)
				certbuf[i] = nonce_share_key_cert[i+nonce.length+share_key.length];
			this.cert = NaclCert.Cert.parse(new String(certbuf, "utf-8"));
		}

		@SuppressWarnings("unused")
		public client_ready_b_v2(){}
	}

	// secure info
	public static class SecInfo {
		private int               version;     // protocol version, 1 or 2
		private byte[]            pk;          // nacl box publickey
		private byte[]            sk;          // nacl box secretkey
		private NaclCert.Cert     cert;        // nacl box publickey cert signed by CA
		private NaclCert.SelfCert ca;          // nacl signature publickey cert signed by self
		private boolean           requireCert; // whether need peer's cert

		// version 1
		public SecInfo(byte[] pk, byte[] sk) {
			this.version = 1;
			
			this.pk = pk;
			this.sk = sk;

			this.cert = null;
			this.ca   = null;
			this.requireCert = false;
		}
		// version 2
		public SecInfo(byte[] pk, byte[] sk, 
				NaclCert.Cert cert, NaclCert.SelfCert ca, boolean requireCert) {
			this.version = 2;
			
			this.pk = pk;
			this.sk = sk;

			this.cert = cert;
			this.ca   = ca;
			this.requireCert = requireCert;
		}
		@SuppressWarnings("unused")
		private SecInfo() {}
		
		public int getVersion() {
			return this.version;
		}
		public byte[] getPublicKey() {
			return this.pk;
		}
		public byte[] getSecretKey() {
			return this.sk;
		}
		public NaclCert.Cert getCert() {
			return this.cert;
		}
		public NaclCert.SelfCert getCa() {
			if (this.ca != null)
				return this.ca;
			else 
				return NaclCert.rootCA.get("iwebpp.com");
		}
		public boolean requireCert() {
			return requireCert;
		}
	}

	// Client Constructor
	@SuppressWarnings("unused")
	public SecureWebSocket(
			final NodeContext ctx, 
			final String address,
			final WebSocket.Options options,
			final SecInfo sec) throws Exception {		
		// context
		this.context = ctx;

		// client
		this.isServer = false;
		this.url = address;

		// check version
		PROTO_VERSION = sec!=null ? sec.getVersion() : 1;

		// Setup security info ///////////////////////////////////////////////////////////////////
		if (PROTO_VERSION >= 1) {
			// setup V1
			this.mySecInfo   = sec;
			this.myPublicKey = sec.getPublicKey();
			this.mySecretKey = sec.getSecretKey();
			
			// check V1
			if (!(this.myPublicKey!=null && this.myPublicKey.length==TweetNaclFast.Box.publicKeyLength))
				throw new Exception("Invalid nacl public key");

			if (!(this.mySecretKey!=null && this.mySecretKey.length==TweetNaclFast.Box.secretKeyLength))
				throw new Exception("Invalid nacl secret key");
		}
		if (PROTO_VERSION >= 2) {
			// setup V2
			this.caCert      = sec.getCa();
			this.myCert      = sec.getCert();

			// client always request server's Cert
			// server can request or not-request client's Cert
			this.requireCert = true;
		}
		//////////////////////////////////////////////////////////////////////////////////////////////

		// FSM 
		this.state = sws_state_t.SWS_STATE_NEW;

		this.ws = new WebSocket(ctx, address, null, options);
		this.ws.onopen(new WebSocket.onopenListener() {

			@Override
			public void onOpen(OpenEvent event) throws Exception {

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
										// V1
										if (PROTO_VERSION == 1) {
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

												// Constructor NACL tx box
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

															// Construct NACL rx box
															SecureWebSocket.this.rxBox = new TweetNaclFast.Box(
																	SecureWebSocket.this.theirPublicKey, 
																	SecureWebSocket.this.mySecretKey, 
																	toLong(SecureWebSocket.this.theirNonce));

															SecureWebSocket.this.rxSecretBox = new TweetNaclFast.SecretBox(
																	SecureWebSocket.this.rxSharekey, 
																	toLong(SecureWebSocket.this.theirNonce));

															// defer hand-shake done 20ms(about RTT)
															ctx.setTimeout(new NodeContext.TimeoutListener() {

																@Override
																public void onTimeout() throws Exception {
																	// set hand shake done
																	SecureWebSocket.this.state = sws_state_t.SWS_STATE_HANDSHAKE_DONE;

																	// Flush sendCache
																	for (send_cache_b c : SecureWebSocket.this.sendCache)
																		SecureWebSocket.this.send(c.chunk, c.options, c.cb);
																	SecureWebSocket.this.sendCache.clear();

																	// emit Secure event
																	SecureWebSocket.this.emit("secure");
																}

															}, 20); // 20ms delay
														}					
													}
												});
											} else {
												SecureWebSocket.this.emit("warn", "invalid handshake server-hello message:"+event.getData().toString());
											}
										} else
										// V2	
										if (PROTO_VERSION == 2) {
											server_hello_b_v2 shm = new server_hello_b_v2(event.getData().toString());

											if (shm.opc == sws_opc_t.SWS_OPC_SERVER_HELLO.opc() &&
												shm.version == PROTO_VERSION) {
												debug(TAG, "ServerHello message V2:"+event.getData().toString());

												// check server's PublicKey Cert /////////////////////////////////
												if (!(NaclCert.validate(shm.cert, SecureWebSocket.this.caCert) && 
													  compareByteArray(shm.server_public_key, shm.cert.desc.reqdesc.publickey))) {
													debug(TAG, "Invalid server cert");
													SecureWebSocket.this.emit("error", "Invalid server cert");
													SecureWebSocket.this.ws.close(0, null);
													return;
												}
												// check domain or ip
												UrlObj serverUrl = Url.parse(SecureWebSocket.this.url);
												String srvDomain = serverUrl.hostname!=null ? serverUrl.hostname : "";
												String srvIP = SecureWebSocket.this.ws.remoteAddress();
												debug(TAG, "expected server ip:"+srvIP);
												debug(TAG, "expected server domain:"+srvDomain);
												if (!(NaclCert.checkDomain(shm.cert, srvDomain) ||
													  NaclCert.checkIP(shm.cert, srvIP))) {
													error(TAG, "Invalid server endpoing");
													SecureWebSocket.this.emit("error", "Invalid server endpoing");
													SecureWebSocket.this.ws.close(0, null);
													return;
												}
												// record server's cert
												SecureWebSocket.this.peerCert = shm.cert;
												/////////////////////////////////////////////////////////////////////////
												
												// update secure info
												SecureWebSocket.this.theirPublicKey = shm.server_public_key;
												SecureWebSocket.this.rxSharekey = shm.share_key;
												SecureWebSocket.this.theirNonce = shm.nonce; 
												
												// send ClientReady message
												client_ready_b_v2 crm = new client_ready_b_v2();
												crm.opc = sws_opc_t.SWS_OPC_CLIENT_READY.opc();
												crm.version = PROTO_VERSION;

												// nonce
												crm.nonce = new byte[client_hello_b.nonceLength];
												randombytes(crm.nonce, crm.nonce.length);

												// shared key
												crm.share_key = new byte[TweetNaclFast.SecretBox.keyLength];
												randombytes(crm.share_key, crm.share_key.length);

												//  check if need cert /////////////////////////////////////
												if (shm.requireCert) {
													if (SecureWebSocket.this.myCert != null) {
														crm.cert = SecureWebSocket.this.myCert;
													} else {
														error(TAG, "Miss client cert");
														SecureWebSocket.this.emit("error", "Miss client cert");
														SecureWebSocket.this.ws.close(0, null);
														return;
													}
												}
												/////////////////////////////////////////////////////////////
												
												// update secure info
												SecureWebSocket.this.myNonce = crm.nonce;
												SecureWebSocket.this.txShareKey = crm.share_key;

												// Constructor NACL tx box
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

															// Construct NACL rx box
															SecureWebSocket.this.rxBox = new TweetNaclFast.Box(
																	SecureWebSocket.this.theirPublicKey, 
																	SecureWebSocket.this.mySecretKey, 
																	toLong(SecureWebSocket.this.theirNonce));

															SecureWebSocket.this.rxSecretBox = new TweetNaclFast.SecretBox(
																	SecureWebSocket.this.rxSharekey, 
																	toLong(SecureWebSocket.this.theirNonce));

															// defer hand-shake done 20ms(about RTT)
															ctx.setTimeout(new NodeContext.TimeoutListener() {

																@Override
																public void onTimeout() throws Exception {
																	// set hand shake done
																	SecureWebSocket.this.state = sws_state_t.SWS_STATE_HANDSHAKE_DONE;

																	// Flush sendCache
																	for (send_cache_b c : SecureWebSocket.this.sendCache)
																		SecureWebSocket.this.send(c.chunk, c.options, c.cb);
																	SecureWebSocket.this.sendCache.clear();

																	// emit Secure event
																	SecureWebSocket.this.emit("secure");
																}

															}, 20); // 20ms delay
														}					
													}
												});
											} else {
												SecureWebSocket.this.emit("warn", "invalid handshake server-hello message:"+event.getData().toString());
											}
										} else {
											SecureWebSocket.this.emit("error", "Invalid protocol version");
											// close ws
											SecureWebSocket.this.ws.close(0, null);
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

	// ServerClient Constructor
	@SuppressWarnings("unused")
	protected SecureWebSocket(final NodeContext ctx, final WebSocket ws, final SecInfo sec) throws Exception {
		// context
		this.context = ctx;

		// server client
		this.isServer = true;

		// check version
		PROTO_VERSION = sec!=null ? sec.getVersion() : 1;
				
		// Setup security info ///////////////////////////////////////////////////////////////////
		if (PROTO_VERSION >= 1) {
			// setup V1
			this.mySecInfo   = sec;
			this.myPublicKey = sec.getPublicKey();
			this.mySecretKey = sec.getSecretKey();
			
			// check V1
			if (!(this.myPublicKey!=null && this.myPublicKey.length==TweetNaclFast.Box.publicKeyLength))
				throw new Exception("Invalid nacl public key");

			if (!(this.mySecretKey!=null && this.mySecretKey.length==TweetNaclFast.Box.secretKeyLength))
				throw new Exception("Invalid nacl secret key");
		}
		if (PROTO_VERSION >= 2) {
			// setup V2
			this.caCert      = sec.getCa();
			this.myCert      = sec.getCert();

			// client always request server's Cert
			// server can request or not-request client's Cert
			this.requireCert = sec!=null ? sec.requireCert() : false;
		}
		//////////////////////////////////////////////////////////////////////////////////////////////

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
									// V1
									if (PROTO_VERSION == 1) {
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

											// Construct NACL tx box
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
									} else
										// V2	
										if (PROTO_VERSION == 2) {
											client_hello_b chm = new client_hello_b(event.getData().toString());

											if (chm.opc == sws_opc_t.SWS_OPC_CLIENT_HELLO.opc() &&
												chm.version == PROTO_VERSION) {
												debug(TAG, "ClientHello message:"+event.getData().toString());

												// update secure info
												SecureWebSocket.this.theirPublicKey = chm.client_public_key;
												SecureWebSocket.this.theirNonce = chm.nonce; 

												// send ServerHello message V2
												server_hello_b_v2 shm = new server_hello_b_v2();
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

												// add server cert ///////////////////////////////////////
												if (SecureWebSocket.this.myCert != null) {
													shm.cert = SecureWebSocket.this.myCert;
													shm.requireCert = SecureWebSocket.this.requireCert;
												} else {
													error(TAG, "Miss server cert");
													SecureWebSocket.this.emit("error", "Miss server cert");
													SecureWebSocket.this.ws.close(0, null);
													return;
												}
												////////////////////////////////////////////////////////

												// update secure info
												SecureWebSocket.this.myNonce = shm.nonce;
												SecureWebSocket.this.txShareKey = shm.share_key;

												debug(TAG, "ServerHello message V2:"+shm.toString());

												// Construct NACL tx box
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
										} else {
											SecureWebSocket.this.emit("error", "Invalid protocol version");
											// close ws
											SecureWebSocket.this.ws.close(0, null);
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
									// V1
									if (PROTO_VERSION == 1) {
										client_ready_b crm = new client_ready_b(event.getData().toString());

										if (crm.opc == sws_opc_t.SWS_OPC_CLIENT_READY.opc() &&
												crm.version == PROTO_VERSION) {
											debug(TAG, "ClientReady message:"+event.getData().toString());

											// clear hand shake timeout
											SecureWebSocket.this.hs_tmo.close();

											// update secure info
											SecureWebSocket.this.rxSharekey = crm.share_key;
											SecureWebSocket.this.theirNonce = crm.nonce; 

											// Construct NACL rx box
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
									} else 
										// V2	
										if (PROTO_VERSION == 2) {
											client_ready_b_v2 crm = new client_ready_b_v2(event.getData().toString());

											if (crm.opc == sws_opc_t.SWS_OPC_CLIENT_READY.opc() &&
												crm.version == PROTO_VERSION) {
												debug(TAG, "ClientReady message V2:"+event.getData().toString());

												// clear hand shake timeout
												SecureWebSocket.this.hs_tmo.close();

												// check client's PublicKey Cert /////////////////////////////////
												if (SecureWebSocket.this.requireCert) {
													// check cert V2
													if (!(NaclCert.validate(crm.cert, SecureWebSocket.this.caCert) && 
														  compareByteArray(SecureWebSocket.this.theirPublicKey, crm.cert.desc.reqdesc.publickey))) {
														debug(TAG, "Invalid client cert");
														SecureWebSocket.this.emit("error", "Invalid client cert");
														SecureWebSocket.this.ws.close(0, null);
														return;
													}
													// check ip
													String clnIP = SecureWebSocket.this.ws.remoteAddress();
													debug(TAG, "expected client ip:"+clnIP);
													if (!NaclCert.checkIP(crm.cert, clnIP)) {
														error(TAG, "Invalid client endpoing");
														SecureWebSocket.this.emit("error", "Invalid client endpoing");
														SecureWebSocket.this.ws.close(0, null);
														return;
													}
													// record client's cert
													SecureWebSocket.this.peerCert = crm.cert;
												}
												/////////////////////////////////////////////////////////////////////
												
												// update secure info
												SecureWebSocket.this.rxSharekey = crm.share_key;
												SecureWebSocket.this.theirNonce = crm.nonce; 

												// Construct NACL rx box
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
										} else {
											SecureWebSocket.this.emit("error", "Invalid protocol version");
											// close ws
											SecureWebSocket.this.ws.close(0, null);
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

	// Address info
	public String remoteAddress() {
		return this.ws.remoteAddress();
	}
	public int remotePort() {
		return this.ws.remotePort();
	}
	public String localAddress() {
		return this.ws.localAddress();
	}
	public int localPort() {
		return this.ws.localPort();
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
	// my security info
	private byte[] mySecretKey;
	private byte[] myPublicKey;
	private NaclCert.Cert myCert;
	
	private byte[] mySignSecretKey;
	private byte[] mySignPublicKey;
	private TweetNaclFast.Signature selfSignature;

	// peer security info
	private byte[] theirPublicKey;
	private String theirCert;
	
	private byte[] theirSignPublicKey;
	private TweetNaclFast.Signature theirSignature;

	// CA security info
	private NaclCert.SelfCert caCert;
	private byte[] caSignPublicKey;
	private TweetNaclFast.Signature caSignature;
	private boolean requireCert;
	
	// authenticated encryption info
	private byte[] myNonce;
	private byte[] theirNonce;
	
	private byte[] txShareKey;
	private byte[] rxSharekey;
	
	private TweetNaclFast.Box txBox;
	private TweetNaclFast.Box rxBox;

	private TweetNaclFast.SecretBox txSecretBox;
	private TweetNaclFast.SecretBox rxSecretBox;

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
			ret |= tmp << (8*(i%8));
		}
		
		return ret;
	}
	
	private static boolean compareByteArray(byte[] a, byte[] b) {
		if (!(a!=null && b!=null && a.length==b.length))
			return false;
		else for (int i = 0; i < a.length; i ++)
			if (a[i] != b[i])
				return false;

		return true;
	}
	
}
