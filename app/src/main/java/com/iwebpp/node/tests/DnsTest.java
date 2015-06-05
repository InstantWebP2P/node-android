package com.iwebpp.node.tests;

import java.util.List;

import android.util.Log;

import com.iwebpp.node.Dns;
import com.iwebpp.node.Util;

import junit.framework.TestCase;

public final class DnsTest extends TestCase {
    private static final String TAG = "DnsTest";
    public static final String HOST_0 = "localhost";
    public static final String HOST_1 = "sohu.com";
    public static final String HOST_2 = "iwebpp.com";
    public static final String HOST_3 = "ruyier.com";
    public static final String HOST_4 = "iwebvpn.com";
    private String ip0;
    private String ip1;
    private String ip2;
    private String ip3;
    private String ip4;
    public static final String IPT_0 = "127.0.0.1";
    public static final String IPT_1 = "::1";
    public static final String IPT_2 = "localhost";
    public static final String IPT_3 = "1.127.0.0.1";
    public static final String IPT_4 = ":::1";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Lookup
        ip0 = Dns.lookup(HOST_0);
        Log.d(TAG, "host:"+ HOST_0 +",ip:"+ ip0);

        ip1 = Dns.lookup(HOST_1);
        Log.d(TAG, "host:"+ HOST_1 +",ip:"+ ip1);

        ip2 = Dns.lookup(HOST_2);
        Log.d(TAG, "host:"+ HOST_2 +",ip:"+ ip2);

        ip3 = Dns.lookup(HOST_3);
        Log.d(TAG, "host:"+ HOST_3 +",ip:"+ ip3);

        ip4 = Dns.lookup(HOST_4);
        Log.d(TAG, "host:"+ HOST_4 +",ip:"+ ip4);
    }

    public void testReverseLookup() {
        List<String> hosts0 = Dns.reverse(ip0);
        if (!(hosts0 != null && hosts0.contains(HOST_0))) {
            Log.d(TAG, "Not matched ip:" + ip0);
        }
        Log.d(TAG, "\n\nip:" + ip0 + ",hosts:" + hosts0);
        assertTrue (hosts0 != null && hosts0.contains(HOST_0));

        List<String> hosts1 = Dns.reverse(ip1);
        if (!(hosts1 != null && hosts1.contains(HOST_1))) {
            Log.d(TAG, "Not matched ip:" + ip1);
        }
        Log.d(TAG, "ip:" + ip1 + ",hosts:" + hosts1);
        assertFalse (hosts1 != null && hosts1.contains(HOST_1));

        List<String> hosts2 = Dns.reverse(ip2);
        if (!(hosts2 != null && hosts2.contains(HOST_2))) {
            Log.d(TAG, "Not matched ip:" + ip2);
        }
        Log.d(TAG, "ip:" + ip2 + ",hosts:" + hosts2);
        assertFalse (hosts2 != null && hosts2.contains(HOST_2));

        List<String> hosts3 = Dns.reverse(ip3);
        if (!(hosts3 != null && hosts3.contains(HOST_3))) {
            Log.d(TAG, "Not matched ip:" + ip3);
        }
        Log.d(TAG, "ip:" + ip3 + ",hosts:" + hosts3);
        assertFalse (hosts3 != null && hosts3.contains(HOST_3));

        List<String> hosts4 = Dns.reverse(ip4);
        if (!(hosts4 != null && hosts4.contains(HOST_4))) {
            Log.d(TAG, "Not matched ip:" + ip4);
        }
        Log.d(TAG, "ip:" + ip4 + ",hosts:" + hosts4);
        assertFalse (hosts4 != null && hosts4.contains(HOST_4));
    }

    public void testIPAddress ()
    {
        if (!Util.isIPv4(IPT_0))
			Log.d(TAG, "isIPv4 test failed on "+ IPT_0);
        assertTrue (Util.isIPv4(IPT_0));
		
		if (Util.isIPv6(IPT_0))
			Log.d(TAG, "isIPv6 test failed on "+ IPT_0);
        assertFalse (Util.isIPv6(IPT_0));
		
		if (!Util.isIP(IPT_0))
			Log.d(TAG, "isIP test failed on "+ IPT_0);
		assertTrue (Util.isIP(IPT_0));

        if (Util.isIPv4(IPT_1))
			Log.d(TAG, "isIPv4 test failed on "+ IPT_1);
        assertFalse (Util.isIPv4(IPT_1));
		
		if (!Util.isIPv6(IPT_1))
			Log.d(TAG, "isIPv6 test failed on "+ IPT_1);
        assertTrue (Util.isIPv6(IPT_1));
		
		if (!Util.isIP(IPT_1))
			Log.d(TAG, "isIP test failed on "+ IPT_1);
        assertTrue (Util.isIP(IPT_1));

        if (Util.isIPv4(IPT_2))
			Log.d(TAG, "isIPv4 test failed on "+ IPT_2);
        assertFalse (Util.isIPv4(IPT_2));
		
		if (Util.isIPv6(IPT_2))
			Log.d(TAG, "isIPv6 test failed on "+ IPT_2);
        assertFalse (Util.isIPv6(IPT_2));
		
		if (Util.isIP(IPT_2))
			Log.d(TAG, "isIP test failed on "+ IPT_2);
        assertFalse (Util.isIP(IPT_2));


        if (Util.isIPv4(IPT_3))
			Log.d(TAG, "isIPv4 test failed on "+ IPT_3);
        assertFalse (Util.isIPv4(IPT_3));
		
		if (Util.isIPv6(IPT_3))
			Log.d(TAG, "isIPv6 test failed on "+ IPT_3);
        assertFalse (Util.isIPv6(IPT_3));
		
		if (Util.isIP(IPT_3))
			Log.d(TAG, "isIP test failed on "+ IPT_3);
        assertFalse (Util.isIP(IPT_3));

        if (Util.isIPv4(IPT_4))
			Log.d(TAG, "isIPv4 test failed on "+ IPT_4);
        assertFalse (Util.isIPv4(IPT_4));
		
		if (Util.isIPv6(IPT_4))
			Log.d(TAG, "isIPv6 test failed on "+ IPT_4);
        assertFalse (Util.isIPv6(IPT_4));
		
		if (Util.isIP(IPT_4))
			Log.d(TAG, "isIP test failed on "+ IPT_4);
        assertFalse (Util.isIP(IPT_4));
	}
}
