package com.iwebpp.node;

import java.util.List;

public interface EventEmitter {
	public boolean emit(final String event);
	public boolean emit(final String event, final Object data);

	public interface Listener {
		void invoke(final Object data) throws Throwable;
	};

	public boolean addListener(final String event, final Listener cb);
	public boolean on(final String event, final Listener cb);
	public boolean once(final String event, final Listener cb);
	public boolean removeListener(final String event, final Listener cb);
	public boolean removeListener(final String event);
	public boolean removeListener();
	public boolean setMaxListeners(final String event, final int n);
	public List<Listener> listeners(final String event);  
	public int listenerCount(final String event);
}
