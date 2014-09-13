package com.iwebpp.node;

import java.util.List;

public interface EventEmitter {
	boolean emit(final String event) throws Exception;
	boolean emit(final String event, final Object data) throws Exception;

	interface Listener {
		void onListen(final Object data) throws Exception;
	};

	boolean on(final String event, final Listener cb) throws Exception;
	boolean once(final String event, final Listener cb) throws Exception;
	
	boolean addListener(final String event, final Listener cb);
	boolean addListener(String event, Listener cb, int priority);

	boolean removeListener(final String event, final Listener cb);
	boolean removeListener(final String event);
	boolean removeListener();
	boolean setMaxListeners(final String event, final int n);
	List<Listener> listeners(final String event);  
	int listenerCount(final String event);
}
