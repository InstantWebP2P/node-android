package com.iwebpp.node.js.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.js.JS;

public abstract class Host implements JS {

	private final static String TAG = "Host";

	private final NodeContext nctx; // node.js native context

	private Context jctx; // js context

	private ScriptableObject scope;

	public Host() {
		nctx = new NodeContext();
	}

	@Override
	public NodeContext getNodeContext() {
		return nctx;
	}
	
	@Override
	public String require(String module) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	/*
	 * @description
	 *   refer to https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Embedding_tutorial#runScript
	 * */
	public void execute() {
		// Entering a Context
		jctx = Context.enter();

		// Turn off optimization to make Rhino Android compatible
		jctx.setOptimizationLevel(-1);

		try {
			// Initializing standard objects
			scope = jctx.initStandardObjects();

			 // Expose node-android context in js
		    ScriptableObject.putProperty(scope, "CurrentNodeContext", Context.javaToJS(nctx, scope));

		    // Expose node-android API in js
		    String nodejs = "var NodeJS = new JavaImporter(" +
                            "com.iwebpp.node.EventEmitter2," +
                            "com.iwebpp.node.Dns," +
                            "com.iwebpp.node.Url," +
                            "com.iwebpp.node.http," +
                            "com.iwebpp.node.net," +
                            "com.iwebpp.node.stream," +
                            "android.util.Log);";
		    jctx.evaluateString(scope, nodejs, "NodeJSAPI", 1, null);
		    
			// Evaluating user authored script
		    String userscript = "with(NodeJS){var NCC=CurrentNodeContext;" + content() + "}";
		    jctx.evaluateString(scope, userscript, "UserContent", 1, null);
			
			// Run node-android loop
			nctx.execute();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			Context.exit();
		}

	}

}
