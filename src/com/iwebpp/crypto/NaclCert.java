// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.crypto;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.iwebpp.SimpleDebug;

public final class NaclCert extends SimpleDebug {
	private final static String TAG = "NaclCert";

	private final static String CERT_VERSION = "1.0";


	// Root CA cert, CA name to Cert map
	public final static Map<String, SelfCert> rootCACert; 
	static {
		rootCACert = new Hashtable<String, SelfCert>();

		try {
			// default CA cert by iwebpp.com
			SelfCert ca_iwebpp = SelfCert.parse("{\"desc\":{\"version\":\"1.0\",\"type\":\"self\",\"ca\":\"iwebpp.com\",\"tte\":4570381246341,\"publickey\":[237,135,86,100,145,128,37,184,250,64,66,132,116,123,207,51,182,199,59,95,17,186,93,249,220,212,109,77,200,222,157,67],\"signtime\":1416781246454,\"gid\":\"d2f971fc-98ad-4dea-ada2-74ebc129ed99\"},\"sign\":{\"signature\":[214,154,215,247,146,167,144,7,25,170,129,182,224,231,13,239,250,159,139,23,184,249,151,12,153,188,61,76,32,215,218,31,185,251,224,222,15,3,17,53,121,125,166,143,167,52,148,146,85,94,234,202,196,157,211,142,134,74,109,78,7,123,177,2]}}");
			rootCACert.put("iwebpp.com", ca_iwebpp);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Test CA info, including cert and secretkey, CA name to CA info map
	public final static Map<String, CAInfo> testCA; 
	static {
		testCA = new Hashtable<String, CAInfo>();

		try {
			// test CA by iwebpp.com
			CAInfo ca_iwebpp = CAInfo.parse("{\"cert\":{\"desc\":{\"version\":\"1.0\",\"type\":\"self\",\"ca\":\"iwebpp.com\",\"tte\":1732375104475,\"publickey\":[16,239,203,168,67,4,190,200,68,163,63,140,27,142,10,25,65,227,92,199,166,33,30,92,73,221,145,174,220,55,82,34],\"signtime\":1417015104534,\"gid\":\"8d0fdd95-566c-4917-b158-36bace3254c7\"},\"sign\":{\"signature\":[84,224,227,61,149,247,74,147,167,225,148,123,103,7,168,101,136,193,121,64,93,37,82,154,3,116,119,206,5,56,96,74,87,195,58,110,233,117,52,57,237,80,91,39,25,223,50,114,201,72,159,158,75,0,230,13,33,34,134,167,171,129,52,0]}},\"secretkey\":[146,248,181,166,252,192,146,133,46,43,69,244,31,182,120,173,115,43,14,89,157,78,77,216,13,240,28,84,186,40,174,232,16,239,203,168,67,4,190,200,68,163,63,140,27,142,10,25,65,227,92,199,166,33,30,92,73,221,145,174,220,55,82,34]}");
			testCA.put("iwebpp.com", ca_iwebpp);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
		
	// Beans
	public static class ReqDescSignBySelf {
		public String version;
		public String type;
		public long   tte;
		public String ca;
		public byte[] publickey;
	}

	public static class ReqDescSignByCa {
		public String version;
		public String type;
		public long   tte;
		public String ca;
		public byte[] publickey;

		public List<String> names;
		public List<String> ips;
	}

	public static class AppendDesc {
		public String gid;
		public long   signtime;
	}

	public static class DescSignBySelf {
		public ReqDescSignBySelf reqdesc;
		public AppendDesc        append;

		public DescSignBySelf() {
			reqdesc = new ReqDescSignBySelf();
			append  = new AppendDesc();
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();

			// put reqdesc
			json.put("version", reqdesc.version);
			json.put("type", reqdesc.type);
			json.put("tte", reqdesc.tte);
			json.put("ca", reqdesc.ca);

			// publickey
			JSONArray pka = new JSONArray(); 
			for (int i = 0; i < reqdesc.publickey.length; i ++)
				pka.put(i, reqdesc.publickey[i]&0xff);

			json.put("publickey", pka);

			// put append
			json.put("gid", append.gid);
			json.put("signtime", append.signtime);

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "DescSignBySelf->:" + jstr);

			return jstr;
		}

		public static DescSignBySelf parse(String jstr) throws JSONException {
			debug(TAG, "DescSignBySelf<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		}

		public static DescSignBySelf parse(JSONObject json) throws JSONException {
			DescSignBySelf desc = new DescSignBySelf();

			// parse reqdesc
			desc.reqdesc.version = json.getString("version");
			desc.reqdesc.type    = json.getString("type");
			desc.reqdesc.tte     = json.getLong("tte");
			desc.reqdesc.ca      = json.getString("ca");

			// publickey
			JSONArray pka = json.getJSONArray("publickey");
			byte[] pkb = new byte[pka.length()];
			for (int i = 0; i < pka.length(); i ++)
				pkb[i] = (byte) (pka.getInt(i)&0xff);

			desc.reqdesc.publickey = pkb;

			// parse append
			desc.append.gid      = json.getString("gid");
			desc.append.signtime = json.getLong("signtime");

			return desc;
		}
	}

	public static class DescSignByCa {
		public ReqDescSignByCa reqdesc;
		public AppendDesc      append;

		public DescSignByCa() {
			reqdesc = new ReqDescSignByCa();
			append  = new AppendDesc();
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();

			// put reqdesc
			json.put("version", reqdesc.version);
			json.put("type", reqdesc.type);
			json.put("tte", reqdesc.tte);
			json.put("ca", reqdesc.ca);

			// publickey
			JSONArray pka = new JSONArray(); 
			for (int i = 0; i < reqdesc.publickey.length; i ++)
				pka.put(i, reqdesc.publickey[i]&0xff);

			json.put("publickey", pka);

			// names
			JSONArray namea = new JSONArray(); 
			if (reqdesc.names!=null)
				for (String name : reqdesc.names)
					namea.put(name);

			json.put("names", namea);

			// ips
			JSONArray ipa = new JSONArray(); 
			if (reqdesc.ips!=null)
				for (String ip : reqdesc.ips)
					ipa.put(ip);

			json.put("ips", ipa);

			// put append
			json.put("gid", append.gid);
			json.put("signtime", append.signtime);

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "DescSignByCa->:" + jstr);

			return jstr;
		}

		public static DescSignByCa parse(String jstr) throws JSONException {
			debug(TAG, "DescSignByCa<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		}

		public static DescSignByCa parse(JSONObject json) throws JSONException {
			DescSignByCa desc = new DescSignByCa();

			// parse reqdesc
			desc.reqdesc.version = json.getString("version");
			desc.reqdesc.type    = json.getString("type");
			desc.reqdesc.tte     = json.getLong("tte");
			desc.reqdesc.ca      = json.getString("ca");

			// publickey
			JSONArray pka = json.getJSONArray("publickey");
			byte[] pkb = new byte[pka.length()];
			for (int i = 0; i < pka.length(); i ++)
				pkb[i] = (byte) (pka.getInt(i)&0xff);

			desc.reqdesc.publickey = pkb;

			// names
			JSONArray namea = json.getJSONArray("names");
			List<String> names = new ArrayList<String>();
			for (int i = 0; i < namea.length(); i ++)
				names.add(namea.getString(i));

			desc.reqdesc.names = names;

			// ips
			JSONArray ipa = json.getJSONArray("ips");
			List<String> ips = new ArrayList<String>();
			for (int i = 0; i < ipa.length(); i ++)
				ips.add(ipa.getString(i));

			desc.reqdesc.ips = ips;

			// parse append
			desc.append.gid      = json.getString("gid");
			desc.append.signtime = json.getLong("signtime");

			return desc;
		}

	}

	public static class Signature {
		public byte [] signature;

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();

			JSONArray siga = new JSONArray(); 
			for (int i = 0; i < signature.length; i ++)
				siga.put(i, signature[i]&0xff);

			json.put("signature", siga);

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "Signature->:" + jstr);

			return jstr;
		}

		public static Signature parse(String jstr) throws JSONException {
			debug(TAG, "Signature<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		} 

		public static Signature parse(JSONObject json) throws JSONException {
			Signature sig = new Signature();

			JSONArray siga = json.getJSONArray("signature");
			byte[] sigb = new byte[siga.length()];
			for (int i = 0; i < siga.length(); i ++)
				sigb[i] = (byte) (siga.getInt(i)&0xff);

			sig.signature = sigb;

			return sig;
		} 
	}

	public static class SelfCert {
		public DescSignBySelf desc;
		public Signature      sign;

		public SelfCert() {
			desc = new DescSignBySelf();
			sign = new Signature();
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();

			json.put("desc", desc.toJSON());
			json.put("sign", sign.toJSON());

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "SelfCert->:" + jstr);

			return jstr;
		}

		public static SelfCert parse(JSONObject json) throws JSONException {
			SelfCert cert = new SelfCert();
			cert.desc = DescSignBySelf.parse(json.getJSONObject("desc"));
			cert.sign = Signature.parse(json.getJSONObject("sign"));

			return cert;
		}

		public static SelfCert parse(String jstr) throws JSONException {
			debug(TAG, "SelfCert<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		}
	}

	public static class Cert {
		public DescSignByCa desc;
		public Signature    sign;

		public Cert() {
			desc = new DescSignByCa();
			sign = new Signature();
		}

		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();

			json.put("desc", desc.toJSON());
			json.put("sign", sign.toJSON());

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "Cert->:" + jstr);

			return jstr;
		}

		public static Cert parse(JSONObject json) throws JSONException {
			Cert cert = new Cert();
			cert.desc = DescSignByCa.parse(json.getJSONObject("desc"));
			cert.sign = Signature.parse(json.getJSONObject("sign"));

			return cert;
		}

		public static Cert parse(String jstr) throws JSONException {
			debug(TAG, "Cert<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		}
	}

	// @description Generate SelfCert
	// @param req, self reqdesc
	// @param cakey, nacl sign secretkey
	// @return cert on success, null on fail
	public static SelfCert generate(ReqDescSignBySelf req, byte[] cakey) throws Exception {
		SelfCert cert = new SelfCert();

		// check type
		if (!req.type.equalsIgnoreCase("self")) {
			e(TAG, "Invalid cert request type");
			return null;
		}

		// check version
		if (!req.version.equalsIgnoreCase(CERT_VERSION)) {
			e(TAG, "Invalid cert request version");
			return null;
		}

		// check time-to-expire
		if (req.tte < System.currentTimeMillis()) {
			e(TAG, "Invalid cert time-to-expire, smaller than current time");
			return null;
		}

		// check CA sign secret key
		if (cakey.length != TweetNaclFast.Signature.secretKeyLength) {
			e(TAG, "Invalid CA secret key");
			return null;
		}

		// append fields
		AppendDesc apnd = new AppendDesc();
		apnd.signtime = System.currentTimeMillis();
		apnd.gid = UUID.randomUUID().toString();

		// full self desc
		DescSignBySelf desc = new DescSignBySelf();
		desc.reqdesc = req;
		desc.append  = apnd;

		// stringify desc
		String descstr = desc.stringify();
		d(TAG, "\ngenerate for "+descstr);
		byte[] descbuf = descstr.getBytes("utf-8");

		// sign signature
		TweetNaclFast.Signature sig = new TweetNaclFast.Signature(null, cakey);

		byte[] sm = sig.sign(descbuf);

		cert.desc = desc;

		cert.sign = new Signature();
		cert.sign.signature = new byte[TweetNaclFast.Signature.signatureLength];
		for (int i = 0; i < cert.sign.signature.length; i ++)
			cert.sign.signature[i] = sm[i];

		return cert;
	}

	// @description Generate Cert
	// @param req, CA reqdesc
	// @param cakey, nacl sign secretkey
	// @param cacert, nacl sign self cert
	// @return cert on success, null on fail
	public static Cert generate(ReqDescSignByCa req, byte[] cakey, SelfCert ca) throws Exception {
		Cert cert = new Cert();

		// check type
		if (!req.type.equalsIgnoreCase("ca")) {
			e(TAG, "Invalid cert request type");
			return null;
		}

		// check version
		if (!req.version.equalsIgnoreCase(CERT_VERSION)) {
			e(TAG, "Invalid cert request version");
			return null;
		}

		// check time-to-expire
		if (req.tte < System.currentTimeMillis()) {
			e(TAG, "Invalid cert time-to-expire, smaller than current time");
			return null;
		}

		// check CA sign secret key
		if (cakey.length != TweetNaclFast.Signature.secretKeyLength) {
			e(TAG, "Invalid CA secret key");
			return null;
		}

		// check CA 
		if (!validate(ca)) {
			e(TAG, "Invalid CA cert");
			return null;
		}

		// override CA field
		req.ca = ca.desc.reqdesc.ca;

		// check tte
		if (req.tte > ca.desc.reqdesc.tte) {
			e(TAG, "Invalid cert time-to-expire, bigger than CA");
			return null;
		}

		// append fields
		AppendDesc apnd = new AppendDesc();
		apnd.signtime = System.currentTimeMillis();
		apnd.gid = UUID.randomUUID().toString();

		// full CA desc
		DescSignByCa desc = new DescSignByCa();
		desc.reqdesc = req;
		desc.append  = apnd;

		// stringify desc
		String descstr = desc.stringify();
		d(TAG, "\ngenerate for "+descstr);
		byte[] descbuf = descstr.getBytes("utf-8");

		// sign signature
		TweetNaclFast.Signature sig = new TweetNaclFast.Signature(null, cakey);

		byte[] sm = sig.sign(descbuf);

		cert.desc = desc;

		cert.sign = new Signature();
		cert.sign.signature = new byte[TweetNaclFast.Signature.signatureLength];
		for (int i = 0; i < cert.sign.signature.length; i ++)
			cert.sign.signature[i] = sm[i];

		return cert;
	}

	// @description Validate SelfCert
	// @param cert, selfcert
	// @return true on success, false on fail
	public static boolean validate(SelfCert cert) throws Exception {
		boolean ret = true;

		// check type
		if (!cert.desc.reqdesc.type.equalsIgnoreCase("self")) {
			e(TAG, "Invalid cert request type");
			return false;
		}

		// check version
		if (!cert.desc.reqdesc.version.equalsIgnoreCase(CERT_VERSION)) {
			e(TAG, "Invalid cert version");
			return false;
		}

		// check time-to-expire
		if (cert.desc.reqdesc.tte < System.currentTimeMillis()) {
			e(TAG, "nacl cert expired");
			return false;
		}

		// nacl sign public key
		byte[] signPublicKey = cert.desc.reqdesc.publickey;

		// stringify desc
		String descstr = cert.desc.stringify();
		d(TAG, "\nvalidate for self-signed:"+descstr);
		byte[] descbuf = descstr.getBytes("utf-8");

		// extract signature
		byte[] signature = cert.sign.signature;
		// check signature length
		if (!(signature!=null && signature.length==TweetNaclFast.Signature.signatureLength)) {
			w(TAG, "Invalid signature length");
			return false;
		}
				
		// verify signature
		TweetNaclFast.Signature sig = new TweetNaclFast.Signature(signPublicKey, null);

		byte[] sm = new byte[signature.length + descbuf.length];
		for (int i = 0; i < signature.length; i ++)
			sm[i] = signature[i];
		for (int i = 0; i < descbuf.length; i ++)
			sm[i+signature.length] = descbuf[i];

		if (null == sig.open(sm)) {
			w(TAG, "Verify signature failed");
			return false;
		}

		return ret;
	}

	// @description Validate Cert
	// @param cert, cert signed by CA
	// @param ca, CA cert signed by self
	// @return true on success, false on fail
	public static boolean validate(Cert cert, SelfCert ca) throws Exception {
		boolean ret = true;

		// check type
		if (!cert.desc.reqdesc.type.equalsIgnoreCase("ca")) {
			e(TAG, "Invalid cert request type");
			return false;
		}

		// check version
		if (!cert.desc.reqdesc.version.equalsIgnoreCase(CERT_VERSION)) {
			e(TAG, "Invalid cert version");
			return false;
		}

		// check time-to-expire
		if (cert.desc.reqdesc.tte < System.currentTimeMillis()) {
			e(TAG, "nacl cert expired");
			return false;
		}

		// check CA
		if (!validate(ca)) {
			e(TAG, "Invalid CA cert");
			return false;
		}

		// check CA name
		if (!cert.desc.reqdesc.ca.equalsIgnoreCase(ca.desc.reqdesc.ca)) {
			e(TAG, "CA not matched");
			return false;
		}

		// check CA time-to-expire
		if (cert.desc.reqdesc.tte > ca.desc.reqdesc.tte) {
			e(TAG, "Invalid cert time-to-expire, bigger than CA");
			return false;
		}

		// extract nacl sign publicKey
		byte[] casignPublicKey = ca.desc.reqdesc.publickey;

		// stringify desc
		String descstr = cert.desc.stringify();
		d(TAG, "\nvalidate for CA-signed:"+descstr);
		byte[] descbuf = descstr.getBytes("utf-8");

		// extract signature
		byte[] signature = cert.sign.signature;
		// check signature length
		if (!(signature!=null && signature.length==TweetNaclFast.Signature.signatureLength)) {
			w(TAG, "Invalid signature length");
			return false;
		}

		// verify signature
		TweetNaclFast.Signature sig = new TweetNaclFast.Signature(casignPublicKey, null);

		byte[] sm = new byte[signature.length + descbuf.length];
		for (int i = 0; i < signature.length; i ++)
			sm[i] = signature[i];
		for (int i = 0; i < descbuf.length; i ++)
			sm[i+signature.length] = descbuf[i];

		if (null == sig.open(sm)) {
			w(TAG, "Verify signature failed");
			return false;
		}

		return ret;
	}

	// @description Check domain
	public static boolean checkDomain(Cert cert, String expectDomain) {
		boolean ret = false;

		if (cert.desc.reqdesc.names!=null)
			for (String name : cert.desc.reqdesc.names)
				if (name.equalsIgnoreCase(expectDomain)) {
					ret = true;
					break;
				}

		return ret;
	}

	// @description Check ip
	public static boolean checkIP(Cert cert, String expectIP) {
		boolean ret = false;

		if (cert.desc.reqdesc.ips!=null)
			for (String ip : cert.desc.reqdesc.ips)
				if (ip.equalsIgnoreCase(expectIP)) {
					ret = true;
					break;
				}

		return ret;
	}

	// @description Generate self-sign CA
	public static class CAInfo {
		public String ca;        // CA name
		public long  tte;        // time-to-expire as ms

		public SelfCert cert;    // self-signed cert
		public byte[] secretkey; // Nacl sign secret key
		
		
		public String toString() {
			String str = "ca:"+ca+"\n";
			str += "tte:"+tte+"\n";
			if (cert!=null)
				try {
					str += cert.stringify();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					///e.printStackTrace();
				}
			if (secretkey!=null) str += "secretkey:"+secretkey.toString();

			return str;
		}
		
		public JSONObject toJSON() throws JSONException {
			JSONObject json = new JSONObject();

			// cert
			json.put("cert", cert.toJSON());

			// secretkey
			JSONArray ska = new JSONArray();
			for (int i = 0; i < secretkey.length; i ++)
				ska.put(i, secretkey[i]&0xff);
			json.put("secretkey", ska);

			return json;
		}

		public String stringify() throws JSONException {
			String jstr = toJSON().toString();

			debug(TAG, "CAInfo->:" + jstr);

			return jstr;
		}

		public static CAInfo parse(JSONObject json) throws JSONException {
			CAInfo ca = new CAInfo();
			
			// self-cert
			ca.cert = SelfCert.parse(json.getJSONObject("cert"));
			ca.ca = ca.cert.desc.reqdesc.ca;
			ca.tte = ca.cert.desc.reqdesc.tte;

			// secretkey
			JSONArray ska = json.getJSONArray("secretkey");
			ca.secretkey = new byte[ska.length()];
			for (int i = 0; i < ska.length(); i ++)
				ca.secretkey[i] = (byte) (ska.getInt(i) & 0xff);

			return ca;
		}

		public static CAInfo parse(String jstr) throws JSONException {
			debug(TAG, "CAInfo<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		}
		
	}
	public static CAInfo generateCA(CAInfo info) throws Exception {
		// prepare self-sign reqdesc
		ReqDescSignBySelf reqdesc = new ReqDescSignBySelf();
		reqdesc.version = CERT_VERSION;
		reqdesc.type    = "self";
		reqdesc.ca      = info.ca;
		reqdesc.tte     = info.tte;

		// genereate Sign keypair
		TweetNaclFast.Signature.KeyPair skp = TweetNaclFast.Signature.keyPair();
		reqdesc.publickey = skp.getPublicKey();

		// generate cert
		SelfCert cert = generate(reqdesc, skp.getSecretKey());

		// fill cert and secret key in CAInfo
		info.cert = cert;
		info.secretkey = skp.getSecretKey();

		return info;
	}

	public static Cert generate(ReqDescSignByCa req, CAInfo ca) throws Exception {
		return generate(req, ca.secretkey, ca.cert);
	}

}
