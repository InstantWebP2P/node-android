package com.iwebpp.Event;

import java.util.List;

public interface eventEmitter {
	public boolean emit(String event);
	public boolean emit(String event, Object data);

	public static interface listener {
		void invoke(String event, int status, Object data);
	};

	public boolean addListener(String event, listener cb);
	public boolean on(String event, listener cb);
	public boolean once(String event,  listener cb);
	public boolean removeListener(String event, listener cb);
	public boolean removeListener(String event);
	public boolean removeListener();
	public boolean setMaxListeners(int n);
	public List<listener> listeners(String event);  
	public int listenerCount(String event);

}
