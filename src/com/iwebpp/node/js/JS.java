package com.iwebpp.node.js;

import com.iwebpp.node.NodeContext;

public interface JS {

	/*
	 * @description
	 *   Authored javascript content
	 * */
	public String content();

	/*
	 * @description
	 *   Node.js require
	 * */
	public String require(String module);

	/*
	 * @description
	 *   Node.js context execute
	 * */
	public void execute();

	/*
	 * @description
	 *   node-android native context
	 * */
	public NodeContext getNodeContext();

}
