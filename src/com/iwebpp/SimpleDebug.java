package com.iwebpp;

import android.util.Log;

public class SimpleDebug {

	public enum DebugLevel {
		NO    (0),
		ERROR (1),
		WARN  (2),
		DEBUG (3),
		INFO  (4);
		
		private int level;
		
		private DebugLevel(int lvl) {
			this.level = lvl;
		}
		
		public int level() {
			return this.level;
		}
	}
	
	/* default debug level as warn */
	private static DebugLevel DEBUG_LEVEL = DebugLevel.WARN;
	
	/*
	 * @description 
	 *   Simple Debug implementation 
	 * */
	public static void setDebugLevel(DebugLevel lvl) {
		DEBUG_LEVEL = lvl;		
	}

	public static DebugLevel getDebugLevel() {
		return DEBUG_LEVEL;
	}

	public static int e(String tag, String message) {
	    if (DEBUG_LEVEL.level() >=  DebugLevel.ERROR.level())
	        return Log.e(tag, message);
	    
	    return -2;
	}
	public int error(String tag, String message) {
		return e(tag, message);
	}
	
	public static int w(String tag, String message) {
	    if (DEBUG_LEVEL.level() >=  DebugLevel.WARN.level())
	        return Log.w(tag, message);
	    
	    return -2;
	}
	public static int warn(String tag, String message) {
		return w(tag, message);
	}
	
	public static int d(String tag, String message) {
	    if (DEBUG_LEVEL.level() >=  DebugLevel.DEBUG.level())
	        return Log.d(tag, message);
	    
	    return -2;
	}
	public static int debug(String tag, String message) {
		return d(tag, message);
	}
	
	public static int i(String tag, String message) {
	    if (DEBUG_LEVEL.level() >=  DebugLevel.INFO.level())
	        return Log.i(tag, message);
	    
	    return -2;
	}
	public static int info(String tag, String message) {
		return i(tag, message);
	}
	
}
