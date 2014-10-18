package com.iwebpp.node.js;

import com.iwebpp.node.NodeContext;

/*
 * @description
 *   NodeJS host env interface implemented by JS engine
 * */
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
	 * @return generic JS Object, JS engine dependent
	 * */
	public Object require(String module);

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
