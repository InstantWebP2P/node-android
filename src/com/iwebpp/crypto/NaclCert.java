// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.crypto;

import java.util.ArrayList;
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


	// Root CA, CA name to Cert map
	private static Map<String, SelfCert> rootCA; 
	static {
		// default CA by iwebpp.com
		SelfCert ca_iwebpp = new SelfCert();
		rootCA.put("iwebpp.com", ca_iwebpp);
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
		ReqDescSignBySelf reqdesc;
		AppendDesc        append;
		
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
		ReqDescSignByCa reqdesc;
		AppendDesc      append;
		
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
			cert.desc = DescSignBySelf.parse((JSONObject)(json.get("desc")));
			cert.sign = Signature.parse((JSONObject)(json.get("sign")));

			return cert;
		}

		public static SelfCert parse(String jstr) throws JSONException {
			debug(TAG, "SelfCert<-:" + jstr);

			JSONObject json = new JSONObject(jstr);

			return parse(json);
		}
	}

	public static class CaCert {
		public DescSignByCa desc;
		public Signature    sign;
		
		public CaCert() {
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

			debug(TAG, "CaCert->:" + jstr);

			return jstr;
		}

		public static CaCert parse(JSONObject json) throws JSONException {
			CaCert cert = new CaCert();
			cert.desc = DescSignByCa.parse((JSONObject)(json.get("desc")));
			cert.sign = Signature.parse((JSONObject)(json.get("sign")));

			return cert;
		}

		public static CaCert parse(String jstr) throws JSONException {
			debug(TAG, "CaCert<-:" + jstr);

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

	// @description Generate CaCert
	// @param req, CA reqdesc
	// @param cakey, nacl sign secretkey
	// @param cacert, nacl sign self cert
	// @return cert on success, null on fail
	public static CaCert generate(ReqDescSignByCa req, byte[] cakey, CaCert ca) throws Exception {
		CaCert cert = new CaCert();

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

	// @description Validate CaCert
	// @param cert, cert signed by CA
	// @param ca, CA cert signed by self
	// @return true on success, false on fail
	public static boolean validate(CaCert cert, SelfCert ca) throws Exception {
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

		// nacl sign public key
		byte[] signPublicKey = cert.desc.reqdesc.publickey;

		// stringify desc
		String descstr = cert.desc.stringify();
		d(TAG, "\nvalidate for self-signed:"+descstr);
		byte[] descbuf = descstr.getBytes("utf-8");

		// extract signature
		byte[] signature = cert.sign.signature;

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

}
