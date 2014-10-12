package com.iwebpp.node.tests;

import java.util.List;

import android.util.Log;

import com.iwebpp.node.Dns;
import com.iwebpp.node.Url;

public final class DnsTest {
	private static final String TAG = "DnsTest";

	private boolean testLookup() throws Exception {	
		// Lookup
		String host0 = "localhost";
        String ip0 = Dns.lookup(host0);
		Log.d(TAG, "host:"+host0+",ip:"+ip0);
		
		String host1 = "sohu.com";
        String ip1 = Dns.lookup(host1);
		Log.d(TAG, "host:"+host1+",ip:"+ip1);
		
		String host2 = "iwebpp.com";
        String ip2 = Dns.lookup(host2);
		Log.d(TAG, "host:"+host2+",ip:"+ip2);
		
		String host3 = "ruyier.com";
        String ip3 = Dns.lookup(host3);
		Log.d(TAG, "host:"+host3+",ip:"+ip3);

		String host4 = "iwebvpn.com";
        String ip4 = Dns.lookup(host4);
		Log.d(TAG, "host:"+host4+",ip:"+ip4);
		
		// Reverse
		List<String>hosts0 = Dns.reverse(ip0);
		if (!(hosts0!=null && hosts0.contains(host0))) {
			Log.d(TAG, "Not matched ip:"+ip0);
		}
		Log.d(TAG, "\n\nip:"+ip0+",hosts:"+hosts0);

		List<String>hosts1 = Dns.reverse(ip1);
		if (!(hosts1!=null && hosts1.contains(host1))) {
			Log.d(TAG, "Not matched ip:"+ip1);
		}
		Log.d(TAG, "ip:"+ip1+",hosts:"+hosts1);
		
		List<String>hosts2 = Dns.reverse(ip2);
		if (!(hosts2!=null && hosts2.contains(host2))) {
			Log.d(TAG, "Not matched ip:"+ip2);
		}
		Log.d(TAG, "ip:"+ip2+",hosts:"+hosts2);
		
		List<String>hosts3 = Dns.reverse(ip3);
		if (!(hosts3!=null && hosts3.contains(host3))) {
			Log.d(TAG, "Not matched ip:"+ip3);
		}
		Log.d(TAG, "ip:"+ip3+",hosts:"+hosts3);

		List<String>hosts4 = Dns.reverse(ip4);
		if (!(hosts4!=null && hosts4.contains(host4))) {
			Log.d(TAG, "Not matched ip:"+ip4);
		}
		Log.d(TAG, "ip:"+ip4+",hosts:"+hosts4);

		return true;
	}

	public void start() {		
		(new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");

				try {
					testLookup();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			    
			}
		})).start();

	}
}
