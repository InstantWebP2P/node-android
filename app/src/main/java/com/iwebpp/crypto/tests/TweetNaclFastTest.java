package com.iwebpp.crypto.tests;

import java.io.UnsupportedEncodingException;

import android.util.Log;

import com.iwebpp.crypto.TweetNaclFast;

import junit.framework.TestCase;

public final class TweetNaclFastTest extends TestCase {
	private static final String TAG = "TweetNaclFastTest";

	public void testBox() throws UnsupportedEncodingException {
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
		
		// stress test
		for (int t = 0; t < 19; t ++, m0+=m0) {
			byte [] mb0 = m0.getBytes("utf-8");

			Log.d(TAG, "\n\n\tbox streess/"+(mb0.length/1000.0) +"kB: " + t + " times");

			// cipher A -> B
			Log.d(TAG, "box ...@" + System.currentTimeMillis());
			byte [] cab = pab.box(mb0);
			Log.d(TAG, "... box@" + System.currentTimeMillis());

			/*String cabt = "";
			for (int i = 0; i < cab.length; i ++)
				cabt += " "+cab[i];
			Log.d(TAG, "cabt: "+cabt);*/

			Log.d(TAG, "\nbox open ...@" + System.currentTimeMillis());
			byte [] mba = pba.open(cab);
			Log.d(TAG, "... box open@" + System.currentTimeMillis());

			/*String mbat = "";
			for (int i = 0; i < mba.length; i ++)
				mbat += " "+mba[i];
			Log.d(TAG, "mbat: "+mbat);*/

			String nm0 = new String(mba, "utf-8");
			if (nm0.equals(m0)) {
				Log.d(TAG, "box/open string success");
			} else {
				Log.e(TAG, "box/open string failed @" + m0 + " / " + nm0);
			}
            assertEquals(nm0, m0);
		}
	}
	
	public void testSecretBox() throws UnsupportedEncodingException {
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
			
			Log.d(TAG, "\n\n\tsecret-box streess/"+(mb0.length/1000.0) +"kB: " + t + " times");

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
			}

            assertEquals (String.format("t=%d", t), nm0, m0);
		}
	}
	
	public void testSign() throws UnsupportedEncodingException {
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
            assertEquals (nm0, m0);
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
            assertEquals (nm0, m0);
		}
	}
	
	/*
	 * SHA-512
	 * */
	public void testHash() throws UnsupportedEncodingException {
		String m0 = "Helloword, Am Tom ...";
		byte [] b0 = m0.getBytes("utf-8");
		
        Log.d(TAG, "\nsha512...@" + System.currentTimeMillis());
		byte [] hash = TweetNaclFast.Hash.sha512(b0);
        Log.d(TAG, "...sha512@" + System.currentTimeMillis());

		String hst = "sha512@"+m0 + "/"+b0.length + ": ";
		for (int i = 0; i < hash.length; i ++)
			hst += " "+hash[i];
		Log.d(TAG, hst);
	}
	
	/*
	 * bench test using tweetnacl.c, tweetnacl.js result
	 * */
	public void testBench() {
	}
	
}
