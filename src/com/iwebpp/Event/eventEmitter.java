package com.iwebpp.Event;

import java.util.List;

public interface eventEmitter {
	public boolean emit(final String event);
	public boolean emit(final String event, final Object data);

	public static interface Listener {
		void invoke(final Object data);
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
