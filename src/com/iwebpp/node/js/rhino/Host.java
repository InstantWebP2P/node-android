package com.iwebpp.node.js.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import com.iwebpp.SimpleDebug;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.js.JS;

/*
 * @description
 *   NodeJS host env implementation with Rhino
 *   Notes:
 *     the internal nodejs module has been imported in JS standard scope, 
 *     just use it, like http, httpp, TCP, UDT, Dns, Url, Readable2, Writable2, etc
 *   
 * */
public abstract class Host 
extends SimpleDebug 
implements JS {

	private final static String TAG = "Host";

	private final NodeContext nodectx; // node.js native context

	private Context jsctx; // js context

	private ScriptableObject scope;

	public Host() {
		nodectx = new NodeContext();
	}

	@Override
	public NodeContext getNodeContext() {
		return nodectx;
	}
	
	@Override
	/* 
	 * @description 
	 *   NodeJS like require, TBD...
	 * */
	public String require(String module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	/*
	 * @description
	 *   refer to https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Embedding_tutorial#runScript
	 * */
	public boolean execute() {
		boolean ret = true;
		
		// Entering a Context
		jsctx = Context.enter();

		// Turn off optimization to make Rhino Android compatible
		jsctx.setOptimizationLevel(-1);

		try {
			// Initializing standard objects
			scope = jsctx.initStandardObjects();

			 // Expose node-android context in js as NodeCurrentContext alias as NCC
		    ScriptableObject.putProperty(scope, "NodeCurrentContext", Context.javaToJS(nodectx, scope));

		    // Expose node-android API in js
		    String nodejs = "var NodeJS = new JavaImporter(" +
                            "com.iwebpp.node.EventEmitter2," +
                            "com.iwebpp.node.Dns," +
                            "com.iwebpp.node.Url," +
                            "com.iwebpp.node.http," +
                            "com.iwebpp.node.net," +
                            "com.iwebpp.node.stream," +
                            "com.iwebpp.wspp.WebSocket," +
                            "com.iwebpp.wspp.WebSocketServer," +
                            "android.util.Log" +
                            ");";
		    jsctx.evaluateString(scope, nodejs, "NodeJSAPI", 1, null);
		    
			// Evaluating user authored script in one line
		    String userscript = ("with(NodeJS){var NCC=NodeCurrentContext;" + content() + "}").replace("[\r\n]+", "");
		    
		    ///DebugLevel lvl = getDebugLevel();
		    ///setDebugLevel(DebugLevel.INFO);
		    info(TAG, "user script: \n\n"+userscript+"\n\n");
		    ///setDebugLevel(lvl);
		    
		    jsctx.evaluateString(scope, userscript, "UserContent", 1, null);

			// Run node-android loop
			nodectx.execute();
		} catch (Throwable e) {
			ret = false;
			
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Context.exit();
		}

		return ret;
	}

}
