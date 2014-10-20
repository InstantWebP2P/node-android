package com.iwebpp.node.api;

import com.iwebpp.EventHandler;
import com.iwebpp.node.NodeContext;

public abstract class SimpleApi 
extends EventHandler 
implements NodeApi {

	private final static String TAG = "SimpleApi";

	private NodeContext context;

	private boolean runOnce;

	public SimpleApi() {
		this.runOnce = false;
		this.context = new NodeContext();
	}

	@Override
	public NodeContext getNodeContext() {
		// TODO Auto-generated method stub
		return context;
	}

	@Override
	public void execute() throws Exception {
		debug(TAG, "execute");

		if (!runOnce) {
			runOnce = true;
			debug(TAG, "execute once");

			// enter event loop in new thread
			new Thread(new Runnable(){

				@Override
				public void run() {
					try {
						// execute user code once
						content();

						// enter event loop
						context.execute();
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
