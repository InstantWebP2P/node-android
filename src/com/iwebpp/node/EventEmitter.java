package com.iwebpp.node;

import java.util.List;

public interface EventEmitter {
	public boolean emit(final String event) throws Exception;
	public boolean emit(final String event, final Object data) throws Exception;

	public interface Listener {
		public void onEvent(final Object data) throws Exception;
	};

	public boolean on(final String event, final Listener cb) throws Exception;
	public boolean once(final String event, final Listener cb) throws Exception;
	
	public boolean addListener(final String event, final Listener cb);
	public boolean addListener(String event, Listener cb, int priority);

	public boolean removeListener(final String event, final Listener cb);
	public boolean removeListener(final String event);
	public boolean removeListener();
	public boolean setMaxListeners(final String event, final int n);
	public List<Listener> listeners(final String event);  
	public int listenerCount(final String event);
}
