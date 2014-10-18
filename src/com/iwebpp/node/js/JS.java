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
	 * @param module - module path, like file path
	 * @return serialized JS Object, JS engine dependent
	 * */
	public String require(String module);

	/*
	 * @description
	 *   Node.js context execute
	 * @return false on error
	 * */
	public boolean execute();

	/*
	 * @description
	 *   node-android native context
	 * */
	public NodeContext getNodeContext();

}
