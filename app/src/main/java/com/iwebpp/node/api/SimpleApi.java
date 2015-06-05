// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.api;

import java.util.LinkedList;
import java.util.List;

import com.iwebpp.EventHandler;
import com.iwebpp.node.NodeContext;

/*
 * @description
 *    SimpleApi run node-android context in a separate thread,
 *    and communicate with UI thread through EventHandler
 * */
public abstract class SimpleApi 
extends EventHandler 
implements NodeApi {

	private final static String TAG = "SimpleApi";

	private List<NodeContext> contextQueue;
	private NodeContext current;

	private int runTimes;

	public SimpleApi() {
		this.runTimes = 0;
		this.contextQueue = new LinkedList<NodeContext>();
		this.current = null;
	}

	@Override
	// @description return current node context
	public NodeContext getNodeContext() {
		// TODO Auto-generated method stub
		return current;
	}

	@Override
	public void execute() throws Exception {
		debug(TAG, "execute");

		{
			// create new context
			if (current != null) contextQueue.add(current);
			current = new NodeContext();

			runTimes ++;
			debug(TAG, "execute times: " + runTimes);

			// enter event loop in new thread
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						// execute user code once
						content();

						// enter event loop
						current.execute();
					} catch (Throwable e) {
						// TODO Auto-generated catch block
						///e.printStackTrace();
						error(TAG, "node-android context execute failed: "+e.toString());
					}			
				}

			}).start();
		}
	}

	@Override
	public abstract void content() throws Exception;

}
