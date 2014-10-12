package com.iwebpp.node;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import android.util.Log;

public final class Dns 
extends EventEmitter2 {
	private static final String TAG = "Dns";
	
	private static final Map<String, String> _errdesc;
	static {
		_errdesc = new Hashtable<String, String>();
		
		_errdesc.put("NODATA",               "DNS server returned answer with no data.");
		_errdesc.put("FORMERR",              "DNS server claims query was misformatted.");
		_errdesc.put("SERVFAIL",             "DNS server returned general failure.");
		_errdesc.put("NOTFOUND",             "Domain name not found.");
		_errdesc.put("NOTIMP",               "DNS server does not implement requested operation.");
		_errdesc.put("REFUSED",              "DNS server refused query.");
		_errdesc.put("BADQUERY",             "Misformatted DNS query.");
		_errdesc.put("BADNAME",              "Misformatted domain name.");
		_errdesc.put("BADFAMILY",            "Misformatted domain name.");
		_errdesc.put("BADRESP",              "Misformatted DNS reply.");
		_errdesc.put("CONNREFUSED",          "Could not contact DNS servers.");
		_errdesc.put("TIMEOUT",              "Timeout while contacting DNS servers.");
		_errdesc.put("EOF",                  "End of file.");
		_errdesc.put("FILE",                 "Error reading file.");
		_errdesc.put("NOMEM",                "Out of memory.");
		_errdesc.put("DESTRUCTION",          "Channel is being destroyed.");
		_errdesc.put("BADSTR",               "Misformatted string.");
		_errdesc.put("BADFLAGS",             "Illegal flags specified.");
		_errdesc.put("NONAME",               "Given hostname is not numeric.");
		_errdesc.put("BADHINTS",             "Illegal hints flags specified.");
		_errdesc.put("NOTINITIALIZED",       "c-ares library initialization not yet performed.");
		_errdesc.put("LOADIPHLPAPI",         "Error loading iphlpapi.dll.");
		_errdesc.put("ADDRGETNETWORKPARAMS", "Could not find GetNetworkParams function.");
		_errdesc.put("CANCELLED",            "DNS query cancelled.");
	}

	// return first matched IP
	public static String lookup(String domain) {

		try {
			InetAddress ipaddr = InetAddress.getByName(domain);
			String ip = ipaddr.getHostAddress();

			Log.d(TAG, "domain:"+domain+",ip:"+ip);
			
			return ip;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			///e.printStackTrace();
		}

		return null;
	}
	
	public static String lookup(String domain, int family) {

		try {
			InetAddress[] ipaddrs = InetAddress.getAllByName(domain);
			String ip = null;

			// return first matched IP
			for (int i = 0; i < ipaddrs.length; i ++) {
				String addr = ipaddrs[i].getHostAddress();
				
				if (family == 4 && Util.isIPv4(addr)) {
					ip = addr; break;
				} else

				if (family == 6 && Util.isIPv6(addr)) {
					ip = addr; break;
				}
			}
			
			Log.d(TAG, "domain:"+domain+",ip:"+ip);

			return ip;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			///e.printStackTrace();
		}

		return null;
	}
	
	public static void lookup(String domain, int family, lookupCallback callback) {
		
	}
	public interface lookupCallback {
		public void onLookup(NodeError err, String ip, int family) throws Exception;
	}
	
	public static List<String> reverse(String ip) {

		try {
			InetAddress[] ipaddrs = InetAddress.getAllByName(ip);

			List<String> domains = new ArrayList<String>();

			for (int i = 0; i < ipaddrs.length; i ++) 
				domains.add(ipaddrs[i].getHostName());
			
			return domains;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			///e.printStackTrace();
		}

		return null;
	}

    public static void reverse(String ip, reverseCallback callback) {
		
	}
	public interface reverseCallback {
		public void onReverse(NodeError err, String [] domains) throws Exception;
	}
	
}
