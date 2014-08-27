package com.iwebpp.Event;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


import android.util.Log;


public class eventEmitter2 implements eventEmitter {
	private final static String TAG = "eventEmitter2";
    private Map<String, List<Listener>> events;
    private Map<String, Integer> maxEvents;
    
    protected eventEmitter2() {
    	this.events = new Hashtable<String, List<Listener>>();
    	this.maxEvents = new Hashtable<String, Integer>();
    }
    
	@Override
	public boolean emit(String event) {
		if (events.containsKey(event)) {
			while (events.get(event).iterator().hasNext())
				events.get(event).iterator().next().invoke(null);
		} else {
			Log.d(TAG, "unknown event "+event);
			return false;
		}
		return true;
	}

	@Override
	public boolean emit(String event, Object data) {
		if (events.containsKey(event)) {
			while (events.get(event).iterator().hasNext())
				events.get(event).iterator().next().invoke(data);
		} else {
			Log.d(TAG, "unknown event "+event);
			return false;
		}
		return true;
	}

	@Override
	public boolean addListener(String event, Listener cb) {
		// check maxListens
		if (maxEvents.containsKey(event) && 
			maxEvents.get(event) < listenerCount(event)) {
			Log.d(TAG, "exceed maxListeners@"+event);

			return false;
		}
		
		if (!events.containsKey(event)) {
			events.put(event, new ArrayList<Listener>());
		}
		return events.get(event).add(cb);
	}

	@Override
	public boolean on(String event, Listener cb) {
		return addListener(event, cb);
	}

	@Override
	public boolean once(final String event, final Listener ocb) {
		return addListener(event, new Listener(){

			@Override
			public void invoke(final Object data) {
				// TODO Auto-generated method stub
				ocb.invoke(data);

				// remove listener
				removeListener(event, this);
			}

		});
	}

	@Override
	public boolean removeListener(String event, Listener cb) {
		if (!events.containsKey(event)) {
			return true;
		} else {
			return events.get(event).remove(cb);
		}
	}

	@Override
	public boolean removeListener(String event) {
		if (events.containsKey(event)) {
			events.get(event).clear();
		}
		
		return true;
	}

	@Override
	public boolean removeListener() {
		// TODO Auto-generated method stub
		events.clear();
		return true;
	}

	@Override
	public boolean setMaxListeners(String event, int n) {
		// TODO Auto-generated method stub
		this.maxEvents.put(event, n);
		return true;
	}

	@Override
	public List<Listener> listeners(String event) {
		// TODO Auto-generated method stub
		return events.get(event);
	}

	@Override
	public int listenerCount(String event) {
		// TODO Auto-generated method stub
		return listeners(event).size();
	}

}
