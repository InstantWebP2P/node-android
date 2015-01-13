package com.iwebpp.node.tests;

import java.util.List;

import android.util.Log;

import com.iwebpp.node.Dns;
import com.iwebpp.node.Util;

import junit.framework.TestCase;

public final class DnsTest extends TestCase {
	private static final String TAG = "DnsTest";

	public void testLookup() throws Exception {	
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

		
		// IP test
		String ipt0 = "127.0.0.1";
		
		if (!Util.isIPv4(ipt0)) 
			Log.d(TAG, "isIPv4 test failed on "+ipt0);
		
		if (Util.isIPv6(ipt0)) 
			Log.d(TAG, "isIPv6 test failed on "+ipt0);
		
		if (!Util.isIP(ipt0)) 
			Log.d(TAG, "isIP test failed on "+ipt0);
		
		
		String ipt1 = "::1";
		
		if (Util.isIPv4(ipt1)) 
			Log.d(TAG, "isIPv4 test failed on "+ipt1);
		
		if (!Util.isIPv6(ipt1)) 
			Log.d(TAG, "isIPv6 test failed on "+ipt1);
		
		if (!Util.isIP(ipt1)) 
			Log.d(TAG, "isIP test failed on "+ipt1);
		
		
		String ipt2 = "localhost";
		
		if (Util.isIPv4(ipt2)) 
			Log.d(TAG, "isIPv4 test failed on "+ipt2);
		
		if (Util.isIPv6(ipt2)) 
			Log.d(TAG, "isIPv6 test failed on "+ipt2);
		
		if (Util.isIP(ipt2)) 
			Log.d(TAG, "isIP test failed on "+ipt2);

		
		String ipt3 = "1.127.0.0.1";
		
		if (Util.isIPv4(ipt3)) 
			Log.d(TAG, "isIPv4 test failed on "+ipt3);
		
		if (Util.isIPv6(ipt3)) 
			Log.d(TAG, "isIPv6 test failed on "+ipt3);
		
		if (Util.isIP(ipt3)) 
			Log.d(TAG, "isIP test failed on "+ipt3);

		
		String ipt4 = ":::1";
		
		if (Util.isIPv4(ipt4)) 
			Log.d(TAG, "isIPv4 test failed on "+ipt4);
		
		if (Util.isIPv6(ipt4)) 
			Log.d(TAG, "isIPv6 test failed on "+ipt4);
		
		if (Util.isIP(ipt4)) 
			Log.d(TAG, "isIP test failed on "+ipt4);
	}
}
