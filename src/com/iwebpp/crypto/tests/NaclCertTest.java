package com.iwebpp.crypto.tests;

import java.util.ArrayList;

import com.iwebpp.crypto.NaclCert;
import com.iwebpp.crypto.TweetNaclFast;

import android.util.Log;

public final class NaclCertTest {
	private static final String TAG = "NaclCertTest";

	private boolean testSelfCert() throws Exception {

		NaclCert.CAInfo info = new NaclCert.CAInfo();

		info.ca = "iwebpp.com";
		info.tte = System.currentTimeMillis() + 3600000L*24*365000;
		NaclCert.CAInfo ca = NaclCert.generateCA(info);
		Log.d(TAG, "\n\ttestSelfCert/ca:"+ca.toString());

		// verify self-cert
		if (!NaclCert.validate(info.cert)) {
			Log.e(TAG, "\n\ttestSelfCert/ca verify failed\n");
			return false;
		} else
			Log.d(TAG, "\n\ttestSelfCert/cert verify success\n");

		return true;
	}
	
	private boolean testCaCert() throws Exception {
		NaclCert.CAInfo info = new NaclCert.CAInfo();

		info.ca = "iwebpp.com";
		info.tte = System.currentTimeMillis() + 3600000L*24*365000;
		NaclCert.CAInfo ca = NaclCert.generateCA(info);
		if (info.tte != (info.tte&0xffffffffffffL)) {
			Log.w(TAG, "tte overflow: "+info.tte);
		}

		// Generate Nacl Box keypair
		TweetNaclFast.Box.KeyPair bkp = TweetNaclFast.Box.keyPair();

		// prepare reqdesc
		NaclCert.ReqDescSignByCa desc = new NaclCert.ReqDescSignByCa();
		desc.version = "1.0";
		desc.type = "ca";
		desc.tte = System.currentTimeMillis() + 3600000L*24*365;
		desc.publickey = bkp.getPublicKey();
		
		// domains 
		desc.names = new ArrayList<String>();
		desc.names.add("51dese.com");
		desc.names.add("ruyier.com");
		
		// ips
		desc.ips = new ArrayList<String>();
		desc.ips.add("127.0.0.1");
		desc.ips.add("10.1.1.1");

		NaclCert.CaCert cert = NaclCert.generate(desc, ca.secretkey, ca.cert);
		if (cert!=null)
			Log.d(TAG, "\n\ttestCaCert/cert:"+cert.stringify());
		else {
			Log.d(TAG, "\n\ttestCaCert/cert generate failed");
			return false;
		}
		
		// validate cert
		if (!NaclCert.validate(cert, ca.cert)) {
			Log.e(TAG, "\n\ttestCaCert/cert verify failed\n");
			return false;
		} else
			Log.d(TAG, "\n\ttestCaCert/cert verify success\n");

		return true;
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				try {
					testSelfCert();
					testCaCert();	
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		})).start();

	}
}
