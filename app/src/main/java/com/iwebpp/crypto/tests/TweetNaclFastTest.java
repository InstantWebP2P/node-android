package com.iwebpp.crypto.tests;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import android.util.Log;

import com.iwebpp.crypto.TweetNaclFast;

public final class TweetNaclFastTest {
	private static final String TAG = "TweetNaclFastTest";

	/**
	* Curve25519 test vectors to help ensure correctness and interoperability
	* copied from Kalium project (https://github.com/abstractj/kalium)
	*/

	public static final String BOB_PRIVATE_KEY = "5dab087e624a8a4b79e17f8b83800ee66f3bb1292618b6fd1c2f8b27ff88e0eb";
	public static final String BOB_PUBLIC_KEY = "de9edb7d7b7dc1b4d35b61c2ece435373f8343c85b78674dadfc7e146f882b4f";

	public static final String ALICE_PRIVATE_KEY = "77076d0a7318a57d3c16c17251b26645df4c2f87ebc0992ab177fba51db92c2a";
	public static final String ALICE_PUBLIC_KEY = "8520f0098930a754748b7ddcb43ef75a0dbf3a0d26381af4eba4a98eaa9b4e6a";
	public static final String ALICE_MULT_BOB = "4a5d9d5ba4ce2de1728e3bf480350f25e07e21c947d19e3376f09b3c1e161742";

	public static final String BOX_NONCE = "69696ee955b62b73cd62bda875fc73d68219e0036b7a0b37";
	public static final String BOX_MESSAGE = "be075fc53c81f2d5cf141316ebeb0c7b5228c52a4c62cbd44b66849b64244ffc" +
	"e5ecbaaf33bd751a1ac728d45e6c61296cdc3c01233561f41db66cce314adb31" +
	"0e3be8250c46f06dceea3a7fa1348057e2f6556ad6b1318a024a838f21af1fde" +
	"048977eb48f59ffd4924ca1c60902e52f0a089bc76897040e082f93776384864" +
	"5e0705";
	public static final String BOX_CIPHERTEXT = "f3ffc7703f9400e52a7dfb4b3d3305d98e993b9f48681273c29650ba32fc76ce" +
	"48332ea7164d96a4476fb8c531a1186ac0dfc17c98dce87b4da7f011ec48c972" +
	"71d2c20f9b928fe2270d6fb863d51738b48eeee314a7cc8ab932164548e526ae" +
	"90224368517acfeabd6bb3732bc0e9da99832b61ca01b6de56244a9e88d5f9b3" +
	"7973f622a43d14a6599b1f654cb45a74e355a5";

	public static final String SECRET_KEY = "1b27556473e985d462cd51197a9a46c76009549eac6474f206c4ee0844f68389";

	public static final String SIGN_PRIVATE = "b18e1d0045995ec3d010c387ccfeb984d783af8fbb0f40fa7db126d889f6dadd";
	public static final String SIGN_MESSAGE = "916c7d1d268fc0e77c1bef238432573c39be577bbea0998936add2b50a653171" +
	"ce18a542b0b7f96c1691a3be6031522894a8634183eda38798a0c5d5d79fbd01" +
	"dd04a8646d71873b77b221998a81922d8105f892316369d5224c9983372d2313" +
	"c6b1f4556ea26ba49d46e8b561e0fc76633ac9766e68e21fba7edca93c4c7460" +
	"376d7f3ac22ff372c18f613f2ae2e856af40";
	public static final String SIGN_SIGNATURE = "6bd710a368c1249923fc7a1610747403040f0cc30815a00f9ff548a896bbda0b" +
	"4eb2ca19ebcf917f0f34200a9edbad3901b64ab09cc5ef7b9bcc3c40c0ff7509";
	public static final String SIGN_PUBLIC = "77f48b59caeda77751ed138b0ec667ff50f8768c25d48309a8f386a2bad187fb";

	private boolean testBoxKalium() throws UnsupportedEncodingException {
	
		Log.d(TAG, "testBoxKalium: test vectors from Kalium project");                 

		// explicit nonce
		byte [] theNonce = TweetNaclFast.hexDecode(BOX_NONCE);
		Log.d(TAG, "BOX_NONCE: " + "\"" + TweetNaclFast.hexEncodeToString(theNonce) + "\"");                 
		

		// keypair A
		byte [] ska = TweetNaclFast.hexDecode(ALICE_PRIVATE_KEY);
		TweetNaclFast.Box.KeyPair ka = TweetNaclFast.Box.keyPair_fromSecretKey(ska);

		Log.d(TAG, "ska: " + "\"" + TweetNaclFast.hexEncodeToString(ka.getSecretKey()) + "\"");                 		
		Log.d(TAG, "pka: " + "\"" + TweetNaclFast.hexEncodeToString(ka.getPublicKey()) + "\"");                 
		
		// keypair B
		byte [] skb = TweetNaclFast.hexDecode(BOB_PRIVATE_KEY);
		TweetNaclFast.Box.KeyPair kb = TweetNaclFast.Box.keyPair_fromSecretKey(skb);
		
		Log.d(TAG, "skb: " + "\"" + TweetNaclFast.hexEncodeToString(kb.getSecretKey()) + "\"");                 
		Log.d(TAG, "pkb: " + "\"" + TweetNaclFast.hexEncodeToString(kb.getPublicKey()) + "\"");                 
		
		// peer A -> B
		TweetNaclFast.Box pabFast = new TweetNaclFast.Box(kb.getPublicKey(), ka.getSecretKey());

		// peer B -> A
		TweetNaclFast.Box pbaFast = new TweetNaclFast.Box(ka.getPublicKey(), kb.getSecretKey());

		// messages

		Log.d(TAG, "BOX_MESSAGE: \n" + BOX_MESSAGE.toUpperCase());
		Log.d(TAG, "BOX_CIPHERTEXT: \n" + BOX_CIPHERTEXT.toUpperCase());

		// cipher A -> B
		byte [] cabFast = pabFast.box(TweetNaclFast.hexDecode(BOX_MESSAGE), theNonce);
		Log.d(TAG, "cabFast: \n" + TweetNaclFast.hexEncodeToString(cabFast));

		if(BOX_CIPHERTEXT.toUpperCase().equals(TweetNaclFast.hexEncodeToString(cabFast))) {
					Log.d(TAG, "\n TweetNaclFast is compatible with Kalium test vector for Box::box");
		} else {
					Log.d(TAG, "\n\n!!! TweetNaclFast Box::box/open failed Kalium compatibility !!!\n");
					return false;
		}

		byte [] mbaFastFast = pbaFast.open(cabFast, theNonce);
		Log.d(TAG, "mbaFastFast: \n" + TweetNaclFast.hexEncodeToString(mbaFastFast));                 

		if(BOX_MESSAGE.toUpperCase().equals(TweetNaclFast.hexEncodeToString(mbaFastFast))) {
					Log.d(TAG, "\n TweetNaclFast is compatible with Kalium test vector for Box::open");
		} else {
					Log.d(TAG, "\n\n!!! TweetNaclFast Box::box/open failed Kalium compatibility !!!\n");
					return false;
		}

		return true;
	}


	private boolean testBox() throws UnsupportedEncodingException {
		// keypair A
		byte [] ska = new byte[32]; for (int i = 0; i < 32; i ++) ska[i] = 0;
		TweetNaclFast.Box.KeyPair ka = TweetNaclFast.Box.keyPair_fromSecretKey(ska);
		
		String skat = "";
		for (int i = 0; i < ka.getSecretKey().length; i ++)
			skat += " "+ka.getSecretKey()[i];
		Log.d(TAG, "skat: "+skat);
		
		
		String pkat = "";
		for (int i = 0; i < ka.getPublicKey().length; i ++)
			pkat += " "+ka.getPublicKey()[i];
		Log.d(TAG, "pkat: "+pkat);
		
		// keypair B
		byte [] skb = new byte[32]; for (int i = 0; i < 32; i ++) skb[i] = 1;
		TweetNaclFast.Box.KeyPair kb = TweetNaclFast.Box.keyPair_fromSecretKey(skb);
		
		String skbt = "";
		for (int i = 0; i < kb.getSecretKey().length; i ++)
			skbt += " "+kb.getSecretKey()[i];
		Log.d(TAG, "skbt: "+skbt);
		
		String pkbt = "";
		for (int i = 0; i < kb.getPublicKey().length; i ++)
			pkbt += " "+kb.getPublicKey()[i];
		Log.d(TAG, "pkbt: "+pkbt);
		
		// peer A -> B
		TweetNaclFast.Box pab = new TweetNaclFast.Box(kb.getPublicKey(), ka.getSecretKey(), 0);

		// peer B -> A
		TweetNaclFast.Box pba = new TweetNaclFast.Box(ka.getPublicKey(), kb.getSecretKey(), 0);

		// messages
		String m0 = "Helloword, Am Tom ...";
		
		// cipher A -> B
		byte [] cab = pab.box(m0.getBytes("utf-8"));
		String cabt = "";
		for (int i = 0; i < cab.length; i ++)
			cabt += " "+cab[i];
		Log.d(TAG, "cabt: "+cabt);
		
		byte [] mba = pba.open(cab);
		String mbat = "";
		for (int i = 0; i < mba.length; i ++)
			mbat += " "+mba[i];
		Log.d(TAG, "mbat: "+mbat);
		
		String nm0 = new String(mba, "utf-8");
		if (nm0.equals(m0)) {
			Log.d(TAG, "box/open string success @" + m0);
		} else {
			Log.e(TAG, "box/open string failed @" + m0 + " / " + nm0);
		}
		
		// cipher B -> A
        byte [] b0 = new byte[6];
        
        Log.d(TAG, "box@" + System.currentTimeMillis());
        byte [] cba = pba.box(b0);
		byte [] mab = pab.open(cba);
        Log.d(TAG, "open@" + System.currentTimeMillis());

		if (b0.length == mab.length) {
			int rc = 0;
			
			for (int i = 0; i < b0.length; i ++)
				if (!(b0[i] == mab[i])) {
					rc = -1;
					Log.e(TAG, "box/open binary failed @" + b0[i] + " / " + mab[i]);
				}

			if (rc == 0)
				Log.d(TAG, "box/open binary success @" + b0);
		} else {
			Log.e(TAG, "box/open binary failed @" + b0 + " / " + mab);
		}

		return true;
	}

	private boolean testBoxNonce() throws UnsupportedEncodingException {
	
		// explicit nonce
        byte [] theNonce = TweetNaclFast.makeBoxNonce();
		byte [] theNonce2 = TweetNaclFast.base64Decode(
		                      TweetNaclFast.base64EncodeToString(theNonce));
		Log.d(TAG, "BoxNonce Base64 test Equal: " + "\"" + java.util.Arrays.equals(theNonce, theNonce2) + "\"");
		byte [] theNonce3 = TweetNaclFast.hexDecode(
		                      TweetNaclFast.hexEncodeToString(theNonce));
		Log.d(TAG, "BoxNonce Hex test Equal: " + "\"" + java.util.Arrays.equals(theNonce, theNonce3) + "\"");
		String theNoncet = "";
		for (int i = 0; i < theNonce.length; i ++)
			theNoncet += " "+theNonce[i];
		Log.d(TAG, "BoxNonce: "+theNoncet);
		Log.d(TAG, "BoxNonce: " + "\"" + TweetNaclFast.base64EncodeToString(theNonce) + "\"");
		Log.d(TAG, "BoxNonce: " + "\"" + TweetNaclFast.hexEncodeToString(theNonce) + "\"");
                 
		

		// keypair A
		byte [] ska = new byte[32]; for (int i = 0; i < 32; i ++) ska[i] = 0;
		TweetNaclFast.Box.KeyPair ka = TweetNaclFast.Box.keyPair_fromSecretKey(ska);
		
		String skat = "";
		for (int i = 0; i < ka.getSecretKey().length; i ++)
			skat += " "+ka.getSecretKey()[i];
		Log.d(TAG, "skat: "+skat);
		
		String pkat = "";
		for (int i = 0; i < ka.getPublicKey().length; i ++)
			pkat += " "+ka.getPublicKey()[i];
		Log.d(TAG, "pkat: "+pkat);
		
		// keypair B
		byte [] skb = new byte[32]; for (int i = 0; i < 32; i ++) skb[i] = 1;
		TweetNaclFast.Box.KeyPair kb = TweetNaclFast.Box.keyPair_fromSecretKey(skb);
		
		String skbt = "";
		for (int i = 0; i < kb.getSecretKey().length; i ++)
			skbt += " "+kb.getSecretKey()[i];
		Log.d(TAG, "skbt: "+skbt);
		
		String pkbt = "";
		for (int i = 0; i < kb.getPublicKey().length; i ++)
			pkbt += " "+kb.getPublicKey()[i];
		Log.d(TAG, "pkbt: "+pkbt);
		
		// peer A -> B
		TweetNaclFast.Box pab = new TweetNaclFast.Box(kb.getPublicKey(), ka.getSecretKey());

		// peer B -> A
		TweetNaclFast.Box pba = new TweetNaclFast.Box(ka.getPublicKey(), kb.getSecretKey());

		// messages
		String m0 = "Helloword, Am Tom ...";
		
		// cipher A -> B
		byte [] cab = pab.box(m0.getBytes("utf-8"), theNonce);
		String cabt = "";
		for (int i = 0; i < cab.length; i ++)
			cabt += " "+cab[i];
		Log.d(TAG, "cabt: "+cabt);
		
		byte [] mba = pba.open(cab, theNonce);
		String mbat = "";
		for (int i = 0; i < mba.length; i ++)
			mbat += " "+mba[i];
		Log.d(TAG, "mbat: "+mbat);
		
		String nm0 = new String(mba, "utf-8");
		if (nm0.equals(m0)) {
			Log.d(TAG, "box/open string success (with nonce) @" + m0);
		} else {
			Log.e(TAG, "box/open string failed (with nonce) @" + m0 + " / " + nm0);
		}
		
		// cipher B -> A
        byte [] b0 = new byte[6];
        
        Log.d(TAG, "box@" + System.currentTimeMillis());
        byte [] cba = pba.box(b0, theNonce);
		byte [] mab = pab.open(cba, theNonce);
        Log.d(TAG, "open@" + System.currentTimeMillis());

		if (b0.length == mab.length) {
			int rc = 0;
			
			for (int i = 0; i < b0.length; i ++)
				if (!(b0[i] == mab[i])) {
					rc = -1;
					Log.e(TAG, "box/open binary failed (with nonce) @" + b0[i] + " / " + mab[i]);
				}

			if (rc == 0)
				Log.d(TAG, "box/open binary success (with nonce) @" + b0);
		} else {
			Log.e(TAG, "box/open binary failed (with nonce) @" + b0 + " / " + mab);
		}

		return true;
	}
	
	private boolean testSecretBox() throws UnsupportedEncodingException {
		// shared key
		byte [] shk = new byte[TweetNaclFast.SecretBox.keyLength];
		for (int i = 0; i < shk.length; i ++)
			shk[i] = 0x66;

		// peer A -> B
		TweetNaclFast.SecretBox pab = new TweetNaclFast.SecretBox(shk, 0);

		// peer B -> A
		TweetNaclFast.SecretBox pba = new TweetNaclFast.SecretBox(shk, 0);

		// messages
		String m0 = "Helloword, Am Tom ...";
		
		// cipher A -> B
		Log.d(TAG, "streess on secret box@"+m0);
		
		for (int t = 0; t < 19; t ++, m0 += m0) {
			byte [] mb0 = m0.getBytes("utf-8");
			
			Log.d(TAG, "\n\n\tstreess/"+(mb0.length/1000.0) +"kB: " + t + " times");

			/*String mb0t = "mb0/"+mb0.length + ": ";
			for (int i = 0; i < mb0.length; i ++)
				mb0t += " "+mb0[i];
			Log.d(TAG, mb0t);
*/
			Log.d(TAG, "secret box ...@" + System.currentTimeMillis());
			byte [] cab = pab.box(mb0);
			Log.d(TAG, "... secret box@" + System.currentTimeMillis());

			/*String cabt = "cab/"+cab.length + ": ";
			for (int i = 0; i < cab.length; i ++)
				cabt += " "+cab[i];
			Log.d(TAG, cabt);
*/
			Log.d(TAG, "\nsecret box open ...@" + System.currentTimeMillis());
			byte [] mba = pba.open(cab);
			Log.d(TAG, "... secret box open@" + System.currentTimeMillis());

			/*
			String mbat = "mba/"+mba.length + ": ";
			for (int i = 0; i < mba.length; i ++)
				mbat += " "+mba[i];
			Log.d(TAG, mbat);
*/
			
			String nm0 = new String(mba, "utf-8");
			if (nm0.equals(m0)) {
				Log.d(TAG, "\tsecret box/open succes");
			} else {
				Log.e(TAG, "\tsecret box/open failed @" + m0 + " / " + nm0);
				return false;
			}
		}
		
		return true;
	}

	private boolean testSecretBoxNonce() throws UnsupportedEncodingException {
		
		// explicit nonce
        byte [] theNonce = TweetNaclFast.makeSecretBoxNonce();
		String theNoncet = "";
		for (int i = 0; i < theNonce.length; i ++)
			theNoncet += " "+theNonce[i];
		Log.d(TAG, "SecretBoxNonce: "+theNoncet);
	
		// shared key
		byte [] shk = new byte[TweetNaclFast.SecretBox.keyLength];
		for (int i = 0; i < shk.length; i ++)
			shk[i] = 0x66;

		// peer A -> B
		TweetNaclFast.SecretBox pab = new TweetNaclFast.SecretBox(shk);

		// peer B -> A
		TweetNaclFast.SecretBox pba = new TweetNaclFast.SecretBox(shk);

		// messages
		String m0 = "Helloword, Am Tom ...";
		
		// cipher A -> B
		Log.d(TAG, "stress on secret box with explicit nonce@"+m0);
		
		for (int t = 0; t < 19; t ++, m0 += m0) {
			byte [] mb0 = m0.getBytes("utf-8");
			
			Log.d(TAG, "\n\n\tstress/"+(mb0.length/1000.0) +"kB: " + t + " times");

			/*String mb0t = "mb0/"+mb0.length + ": ";
			for (int i = 0; i < mb0.length; i ++)
				mb0t += " "+mb0[i];
			Log.d(TAG, mb0t);
*/
			Log.d(TAG, "secret box ...@" + System.currentTimeMillis());
			byte [] cab = pab.box(mb0, theNonce);
			Log.d(TAG, "... secret box@" + System.currentTimeMillis());

			/*String cabt = "cab/"+cab.length + ": ";
			for (int i = 0; i < cab.length; i ++)
				cabt += " "+cab[i];
			Log.d(TAG, cabt);
*/
			Log.d(TAG, "\nsecret box open ...@" + System.currentTimeMillis());
			byte [] mba = pba.open(cab, theNonce);
			Log.d(TAG, "... secret box open@" + System.currentTimeMillis());

			/*
			String mbat = "mba/"+mba.length + ": ";
			for (int i = 0; i < mba.length; i ++)
				mbat += " "+mba[i];
			Log.d(TAG, mbat);
*/
			
			String nm0 = new String(mba, "utf-8");
			if (nm0.equals(m0)) {
				Log.d(TAG, "\tsecret box/open succes (with nonce)");
			} else {
				Log.e(TAG, "\tsecret box/open failed (with nonce) @" + m0 + " / " + nm0);
				return false;
			}
		}
		
		return true;
	}
	
	private boolean testSign() throws UnsupportedEncodingException {
		// keypair A
		TweetNaclFast.Signature.KeyPair ka = TweetNaclFast.Signature.keyPair();

		// keypair B
		TweetNaclFast.Signature.KeyPair kb = TweetNaclFast.Signature.keyPair();

		// peer A -> B
		TweetNaclFast.Signature pab = new TweetNaclFast.Signature(kb.getPublicKey(), ka.getSecretKey());

		// peer B -> A
		TweetNaclFast.Signature pba = new TweetNaclFast.Signature(ka.getPublicKey(), kb.getSecretKey());

		// messages
		String m0 = "Helloword, Am Tom ...";

		// signature A -> B
        Log.d(TAG, "\nsign...@" + System.currentTimeMillis());
		byte [] sab = pab.sign(m0.getBytes("utf-8"));
        Log.d(TAG, "...sign@" + System.currentTimeMillis());

		String sgt = "sign@"+m0 + ": ";
		for (int i = 0; i < TweetNaclFast.Signature.signatureLength; i ++)
			sgt += " "+sab[i];
		Log.d(TAG, sgt);
		
        Log.d(TAG, "verify...@" + System.currentTimeMillis());
		byte [] oba = pba.open(sab);
        Log.d(TAG, "...verify@" + System.currentTimeMillis());

		if (oba == null) {
			Log.e(TAG, "verify failed @" + m0);
		} else {
			String nm0 = new String(oba, "utf-8");
			if (nm0.equals(m0)) {
				Log.d(TAG, "sign success @" + m0);
			} else {
				Log.e(TAG, "sign failed @" + m0 + " / " + nm0);
			}
		}
		
		// keypair C
		byte [] seed = new byte[TweetNaclFast.Signature.seedLength]; for (int i = 0; i < seed.length; i ++) seed[i] = 0x66;
		
		TweetNaclFast.Signature.KeyPair kc = TweetNaclFast.Signature.keyPair_fromSeed(seed);
		
		String skct = "";
		for (int i = 0; i < kc.getSecretKey().length; i ++)
			skct += " "+kc.getSecretKey()[i];
		Log.d(TAG, "skct: "+skct);
		
		String pkct = "";
		for (int i = 0; i < kc.getPublicKey().length; i ++)
			pkct += " "+kc.getPublicKey()[i];
		Log.d(TAG, "pkct: "+pkct);
		
		// self-signed
		TweetNaclFast.Signature pcc = new TweetNaclFast.Signature(kc.getPublicKey(), kc.getSecretKey());

		Log.d(TAG, "\nself-sign...@" + System.currentTimeMillis());
		byte [] scc = pcc.sign(m0.getBytes("utf-8"));
		Log.d(TAG, "...self-sign@" + System.currentTimeMillis());

		String ssc = "self-sign@"+m0 + ": ";
		for (int i = 0; i < TweetNaclFast.Signature.signatureLength; i ++)
			ssc += " "+scc[i];
		Log.d(TAG, ssc);

		Log.d(TAG, "self-verify...@" + System.currentTimeMillis());
		byte [] occ = pcc.open(scc);
		Log.d(TAG, "...self-verify@" + System.currentTimeMillis());
		
		if (occ == null) {
			Log.e(TAG, "self-verify failed @" + m0);
		} else {
			String nm0 = new String(occ, "utf-8");
			if (nm0.equals(m0)) {
				Log.d(TAG, "self-sign success @" + m0);
			} else {
				Log.e(TAG, "self-sign failed @" + m0 + " / " + nm0);
			}
		}
		
		return true;
	}
	
	/*
	 * SHA-512
	 * */
	private boolean testHash() throws UnsupportedEncodingException {
		String m0 = "Helloword, Am Tom ...";
		byte [] b0 = m0.getBytes("utf-8");
		
        Log.d(TAG, "\nsha512...@" + System.currentTimeMillis());
		byte [] hash = TweetNaclFast.Hash.sha512(b0);
        Log.d(TAG, "...sha512@" + System.currentTimeMillis());

		String hst = "sha512@"+m0 + "/"+b0.length + ": ";
		for (int i = 0; i < hash.length; i ++)
			hst += " "+hash[i];
		Log.d(TAG, hst);
		
		return true;
	}
	
	/*
	 * bench test using tweetnacl.c, tweetnacl.js result
	 * */
	private boolean testBench() {
		
		return true;
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				try {
					testSecretBox();
					testSecretBoxNonce();
					testBox();
					testBoxNonce();
					testBoxKalium();
					
					testHash();
					testSign();
					
					///testBench();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			    
			}
		})).start();

	}

  public static void main(String[] args) {
    TweetNaclFastTest t = new TweetNaclFastTest();
    t.start();
  }


}
