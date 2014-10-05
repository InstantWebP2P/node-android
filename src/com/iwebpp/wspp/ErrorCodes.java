package com.iwebpp.wspp;

import java.util.Hashtable;
import java.util.Map;

public final class ErrorCodes {

	private final static Map<Integer, String> _descs;

	static {
		_descs = new Hashtable<Integer, String>();

		_descs.put(1000, "normal");
		_descs.put(1001, "going away");
		_descs.put(1002, "protocol error");
		_descs.put(1003, "unsupported data");
		_descs.put(1004, "reserved");
		_descs.put(1005, "reserved for extensions");
		_descs.put(1006, "reserved for extensions");
		_descs.put(1007, "inconsistent or invalid data");
		_descs.put(1008, "policy violation");
		_descs.put(1009, "message too big");
		_descs.put(1010, "extension handshake missing");
		_descs.put(1011, "an unexpected condition prevented the request from being fulfilled");
	}

	public static boolean isValidErrorCode(int code) {
		return (code >= 1000 && code <= 1011 && code != 1004 && code != 1005 && code != 1006) ||
			   (code >= 3000 && code <= 4999);
	}

	public static String desc(int code) {
		if (isValidErrorCode(code))
			return _descs.get(code);
		else 
			return "unknown error";
	}

}
