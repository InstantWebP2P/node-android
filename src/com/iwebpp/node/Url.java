package com.iwebpp.node;

import java.util.Hashtable;
import java.util.Map;

import android.net.Uri;


public final class Url 
extends EventEmitter2 {
    
	public static class UrlObj {
		public String     href = null;
		public String protocol = null;
		public boolean slashes = false;
		public String     host = null;
		public String     auth = null;
		public String hostname = null;
		public int        port = -1;
		public String pathname = null;
		public String   search = null;
		public String     path = null;
		public String    query = null;
		public String     hash = null;
		
		public Map<String, String> queryParams;
		
		public boolean parseQueryString  = false;
		public boolean slashesDenoteHost = false;
		
		UrlObj() {
			this.queryParams = new Hashtable<String, String>();
		}
	}
	
	public static UrlObj parse(
			String urlStr, 
			boolean parseQueryString, 
			boolean slashesDenoteHost) throws Exception {
		UrlObj obj = new UrlObj();
		
		Uri url = Uri.parse(urlStr).normalizeScheme();
		
		obj.parseQueryString  = parseQueryString;
		obj.slashesDenoteHost = slashesDenoteHost;
		
		obj.href     = urlStr.toLowerCase();
		
		obj.protocol = url.getScheme()!=null ? url.getScheme()+":" : null;
		obj.slashes  = slashesDenoteHost;
		obj.hostname = url.getHost();
		obj.port     = url.getPort();
		obj.auth     = url.getUserInfo();
		obj.pathname = url.getPath();
		obj.query    = url.getQuery();
		obj.hash     = url.getFragment()!=null ? "#"+url.getFragment() : null;
		
	    obj.search =     obj.query!=null ? "?"+obj.query    : null;
        obj.host   = (obj.hostname!=null ? obj.hostname : "") + (      obj.port>0 ? ":"+obj.port : "");
		obj.path   = (obj.pathname!=null ? obj.pathname : "") + (obj.search!=null ?   obj.search : "");
		
		// only find first matched key
		if (obj.parseQueryString && obj.query!=null)
			for (String k : url.getQueryParameterNames())
				obj.queryParams.put(k, url.getQueryParameter(k));
		
		return obj;
	} 
	
	public static UrlObj parse(String urlStr) throws Exception {
		return parse(urlStr, true, true);
	} 
	
	public static String format(UrlObj obj) throws Exception {
		String str = "";
		
		str += obj.protocol!=null ? obj.protocol : "http:";
		
		str += obj.slashes ? "//" : "";
		
		str += obj.auth!=null ? obj.auth+"@" : "";
		
		       if (obj.host != null) {
			str += obj.host;
		} else if (obj.hostname!=null) {
			str += obj.hostname;
			str += obj.port>0 ? ":"+obj.port : "";
		} else {
			throw new Exception("Miss URL hostname");
		}
		
		       if (obj.path != null) {
			str += obj.path;
		} else if (obj.pathname != null) {
			str += obj.pathname;
			
			       if (obj.search != null) {
				str += obj.search;
			} else if (obj.query != null) {
				str += "?" + obj.query;
			}
		} else {
			// TBD...
			///str += "/";
			
			if (obj.search != null) {
				str += obj.search;
			} else if (obj.query != null) {
				str += "?" + obj.query;
			}
		}
		       
		str += obj.hash!=null ? obj.hash : "";
		       
		return str;
	}
	
	public static String resolve(String from, String to) {		
		return Uri.withAppendedPath(Uri.parse(from), to).toString();
	}
	
}
