// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.api;

import com.iwebpp.node.NodeContext;

public interface NodeApi {

	/*
	 * @description
	 *   get node-android context
	 * */
	public NodeContext getNodeContext();
	
	/*
	 * @description
	 *   execute node-android context in a separate thread
	 * */
	public void execute() throws Exception;

	/*
	 * @description
	 *   user code content
	 * */
	public void content() throws Exception;
	
}
