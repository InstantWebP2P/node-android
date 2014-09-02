package com.iwebpp.node;

import java.util.List;

public interface EventEmitter {
	boolean emit(final String event) throws Throwable;
	boolean emit(final String event, final Object data) throws Throwable;

	interface Listener {
		void invoke(final Object data) throws Throwable;
	};

	boolean on(final String event, final Listener cb) throws Throwable;
	boolean once(final String event, final Listener cb) throws Throwable;
	
	boolean addListener(final String event, final Listener cb);
	boolean addListener(String event, Listener cb, int priority);

	boolean removeListener(final String event, final Listener cb);
	boolean removeListener(final String event);
	boolean removeListener();
	boolean setMaxListeners(final String event, final int n);
	List<Listener> listeners(final String event);  
	int listenerCount(final String event);
}
