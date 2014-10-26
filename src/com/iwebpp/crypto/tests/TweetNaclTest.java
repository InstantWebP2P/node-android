package com.iwebpp.crypto.tests;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

import com.iwebpp.crypto.TweetNacl;

import android.util.Log;

public final class TweetNaclTest {
	private static final String TAG = "TweetNaclTest";

	private boolean testBox() throws UnsupportedEncodingException {
		// keypair A
		TweetNacl.Box.KeyPair ka = TweetNacl.Box.keyPair();
		
		// keypair B
		TweetNacl.Box.KeyPair kb = TweetNacl.Box.keyPair();

		// peer A -> B
		TweetNacl.Box pab = new TweetNacl.Box(kb.getPublicKey(), ka.getSecretKey(), 0);

		// peer B -> A
		TweetNacl.Box pba = new TweetNacl.Box(ka.getPublicKey(), kb.getSecretKey(), 0);

		// messages
		String m0 = "Helloword, TweetNacl...";
		
		// cipher A -> B
		byte [] cab = pab.box(m0.getBytes("utf-8"));
		
		byte [] mba = pba.open(cab);
		
		String nm0 = new String(mba, "utf-8");
		if (nm0.equals(m0)) {
			Log.d(TAG, "box/open string success @" + m0);
		} else {
			Log.e(TAG, "box/open string failed @" + m0 + " / " + nm0);
		}
		
		// cipher B -> A
        byte [] b0 = new byte[1518];
        
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
	
	private boolean testSecretBox() throws UnsupportedEncodingException {
		// shared key
		byte [] shk = new byte[TweetNacl.SecretBox.keyLength];
		for (int i = 0; i < shk.length; i ++)
			shk[i] = 0x66;

		// peer A -> B
		TweetNacl.SecretBox pab = new TweetNacl.SecretBox(shk, 0x68);

		// peer B -> A
		TweetNacl.SecretBox pba = new TweetNacl.SecretBox(shk, 0x68);

		// messages
		String m0 = "Helloword, TweetNacl...";
		
		// cipher A -> B
		byte [] mb0 = m0.getBytes("utf-8");

		String mb0t = "mb0/"+mb0.length + ": ";
		for (int i = 0; i < mb0.length; i ++)
			mb0t += " "+mb0[i];
		Log.d(TAG, mb0t);

        Log.d(TAG, "box@" + System.currentTimeMillis());
		byte [] cab = pab.box(mb0);
		
		String cabt = "cab/"+cab.length + ": ";
		for (int i = 0; i < cab.length; i ++)
			cabt += " "+cab[i];
		Log.d(TAG, cabt);
		
		byte [] mba = pba.open(cab);
        Log.d(TAG, "open@" + System.currentTimeMillis());

		String mbat = "mba/"+mba.length + ": ";
		for (int i = 0; i < mba.length; i ++)
			mbat += " "+mba[i];
		Log.d(TAG, mbat);
		
		String nm0 = new String(mba, "utf-8");
		if (nm0.equals(m0)) {
			Log.d(TAG, "secret box/open success @" + m0);
		} else {
			Log.e(TAG, "secret box/open failed @" + m0 + " / " + nm0);
		}
				
		return true;
	}
	
	private boolean testSign() throws UnsupportedEncodingException {
		// keypair A
		TweetNacl.Signature.KeyPair ka = TweetNacl.Signature.keyPair();

		// keypair B
		TweetNacl.Signature.KeyPair kb = TweetNacl.Signature.keyPair();

		// peer A -> B
		TweetNacl.Signature pab = new TweetNacl.Signature(kb.getPublicKey(), ka.getSecretKey());

		// peer B -> A
		TweetNacl.Signature pba = new TweetNacl.Signature(ka.getPublicKey(), kb.getSecretKey());

		// messages
		String m0 = "Helloword, TweetNacl...";

		// signature A -> B
		byte [] sab = pab.sign(m0.getBytes("utf-8"));
		byte [] oba = pba.open(sab);

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
		
		return true;
	}
	
	/*
	 * bench test using tweetnacl.c, libsoldium result
	 * */
	private boolean testBench() {
		
		return true;
	}
	
	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				try {
					testSign();
					testSecretBox();
					testBox();
					
					testBench();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			    
			}
		})).start();

	}


}
