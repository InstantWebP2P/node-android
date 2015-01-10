// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node;

import java.util.Date;


import com.iwebpp.SimpleDebug;
import com.iwebpp.libuvpp.LibUV;
import com.iwebpp.libuvpp.cb.TimerCallback;
import com.iwebpp.libuvpp.handles.LoopHandle;
import com.iwebpp.libuvpp.handles.TimerHandle;

// Node.android libUV loop context
public final class NodeContext extends SimpleDebug {
    private static final String TAG = "NodeContext";
	
	static {
		// call an idempotent LibUV method just to ensure that the native lib is loaded
		LibUV.cwd();
	}

	private LoopHandle loop;

	/**
	 * @return the loop
	 */
	public LoopHandle getLoop() {
		return loop;
	}

	public NodeContext() {
		this.loop = new LoopHandle();
	}

	public void execute() throws Throwable {
		this.loop.run();
	}

	public void destroy() {
		this.loop.destroy();
	}
	
	/*
	 * DOM-style timers
	 */
	// setTimeout/clearTimeout
	public interface TimeoutListener{
		public void onTimeout() throws Exception;
	}
	// after: ms
    public TimerHandle setTimeout(final TimeoutListener callback, int after) {
    	final TimerHandle timer = new TimerHandle(loop);
    	
    	timer.setCloseCallback(new TimerCallback() {
            @Override
            public void onTimer(final int i) throws Exception {
                debug(TAG, "setTimeout timer closed");
            }
        });

        timer.setTimerFiredCallback(new TimerCallback() {
            @Override
            public void onTimer(final int status) throws Exception {
                debug(TAG, "setTimeout timer fired");

                callback.onTimeout();
                
                timer.close();
            }
        });

        timer.start(after, 0);
    	
    	return timer;
    }
    public void clearTimeout(TimerHandle timer) {
        timer.close();
    }
    
    // setInterval/clearInterval
	public interface IntervalListener{
		public void onInterval() throws Exception;
	}
	// repeat: ms
    public TimerHandle setInterval(final IntervalListener callback, int repeat) {
    	final TimerHandle timer = new TimerHandle(loop);
    	
    	timer.setCloseCallback(new TimerCallback() {
            @Override
            public void onTimer(final int i) throws Exception {
                debug(TAG, "setInterval timer closed");
            }
        });

        timer.setTimerFiredCallback(new TimerCallback() {
            @Override
            public void onTimer(final int status) throws Exception {
                debug(TAG, "setInterval timer fired");

                callback.onInterval();
            }
        });

        timer.start(repeat, repeat);
    	
    	return timer;
    }
    public void clearInterval(TimerHandle timer) {
        timer.close();
    }
    
    // fire on next tick
    public void nextTick(final nextTickListener next) {
        final TimerHandle timer = new TimerHandle(loop);

        timer.setCloseCallback(new TimerCallback() {
            @Override
            public void onTimer(final int i) throws Exception {
                debug(TAG, "nextTick timer closed");
            }
        });

        timer.setTimerFiredCallback(new TimerCallback() {
            @Override
            public void onTimer(final int status) throws Exception {
                debug(TAG, "nextTick timer fired");

                next.onNextTick();
                
                timer.close();
            }
        });

        timer.start(0, 0);
    }
    public interface nextTickListener {
    	void onNextTick() throws Exception;
    }

    // 
    private volatile String dateCached = null;
    public String utcDate() {
    	if (null == dateCached) {
    		Date d = new Date();
    		// TBD... GTC 
    		dateCached = d.toString();

    		setTimeout(new TimeoutListener() {

    			@Override
    			public void onTimeout() throws Exception {
    				dateCached = null;					
    			}

    		}, 1000);
    	}

    	return dateCached;
    }
    
}

