/**
 * Copyright (c) 2014 Tom Zhou
 * @author tomzhou
 * 
 */


package com.iwebpp;

import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.iwebpp.node.EventEmitter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/*
 * @description 
 *   Node.js EvenEmitter implementation with Android Handler
 * */
public class EventHandler 
extends SimpleDebug 
implements EventEmitter {
	private final static String TAG = "EventHandler";
    private msgHdl hdl;
    private Map<String, List<Listener>> events;
    private Map<String, Integer> maxEvents;

    // @description message handler
    private class msgHdl extends Handler {

    	/* (non-Javadoc)
    	 * @see android.os.Handler#handleMessage(android.os.Message)
    	 */
    	public void handleMessage (Message msg) {
    		Bundle bdl = msg.getData();
    		String event = bdl.getString("event");
    		Object data = msg.obj;

    		if (events.containsKey(event)) {
    			for (Listener cb : events.get(event))
    				try {
    					// always create new one to share in case ByteBuffer, etc
    					if (data!=null && data instanceof ByteBuffer) {
    						ByteBuffer bb = ((ByteBuffer)data).slice();
    						cb.onEvent(bb);
    					} else 
    						cb.onEvent(data);
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					///e.printStackTrace();
    					error(TAG, "Exception event "+event+","+e);
    				}
    		} else {
    			warn(TAG, "unknown event "+event);
    		}
    	}

    }
    
    EventHandler() {
    	this.hdl = new msgHdl();
    	this.events = new Hashtable<String, List<Listener>>();
    	this.maxEvents = new Hashtable<String, Integer>();
    }
    
	@Override
	public boolean emit(final String event, final Object data) throws Exception {
		Message msg = hdl.obtainMessage();
		Bundle bdl = new Bundle();
		
		// event string
		bdl.putString("event", event);
		msg.setData(bdl);
		
		// data object
		msg.obj = data;
		
		return events.containsKey(event) && hdl.sendMessage(msg);
	}

	@Override
	public boolean emit(String event) throws Exception {
		// TODO Auto-generated method stub
		return emit(event, null);
	}
	
	@Override
	public EventEmitter on(final String event, final Listener cb) {
		return addListener(event, cb);
	}

	@Override
	public EventEmitter once(final String event, final Listener ocb) {
		return addListener(event, new Listener(){

			@Override
			public void onEvent(final Object data) throws Exception {
				// TODO Auto-generated method stub
				ocb.onEvent(data);

				// remove listener
				removeListener(event, this);
			}

		});
	}

	@Override
	public EventEmitter addListener(final String event, final Listener cb) {
		// check maxListens
		if (maxEvents.containsKey(event) && 
			maxEvents.get(event) < listenerCount(event)) {
			warn(TAG, "exceed maxListeners@"+event+" at="+this);

			///return this;
		}

		if (!events.containsKey(event))
			events.put(event, new LinkedList<Listener>());

		events.get(event).add(cb);

		return this;
	}

	@Override
	public EventEmitter addListener(String event, Listener cb, int priority) {
		// check maxListens
		if (maxEvents.containsKey(event) && 
			maxEvents.get(event) < listenerCount(event)) {
			warn(TAG, "exceed maxListeners@"+event+" at="+this);

			///return this;
		}

		if (!events.containsKey(event))
			events.put(event, new LinkedList<Listener>());
		
		if (priority < listenerCount(event))
			events.get(event).add(priority, cb);
		else 
			events.get(event).add(cb);
		
		return this;
	}
	
	@Override
	public EventEmitter removeListener(final String event, final Listener cb) {
		if (events.containsKey(event) && events.get(event).contains(cb))
			events.get(event).remove(cb);
		
		return this;
	}

	@Override
	public EventEmitter removeListener(final String event) {
		if (events.containsKey(event))
			events.get(event).clear();
		
		return this;
	}

	@Override
	public EventEmitter removeListener() {
		events.clear();
		return this;
	}

	@Override
	public EventEmitter removeAllListeners() {
		events.clear();
		return this;
	}

	@Override
	public EventEmitter setMaxListeners(final String event, final int n) {
		this.maxEvents.put(event, n);
		return this;
	}

	@Override
	public List<Listener> listeners(final String event) {
		return events.containsKey(event) ? events.get(event) : null;
	}

	@Override
	public int listenerCount(final String event) {
		return events.containsKey(event) ? events.get(event).size() : 0;
	}

}
