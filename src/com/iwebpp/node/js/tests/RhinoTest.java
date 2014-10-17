package com.iwebpp.node.js.tests;

import com.iwebpp.node.js.rhino.Host;

import android.util.Log;


public final class RhinoTest {
	private static final String TAG = "RhinoTest";
	
	private class HelloWorld extends Host {

		@Override
		public String content() {
			// TODO Auto-generated method stub
			return "Log.d('RhinoTest', 'Helloworld, js');";
		}
		
	}
	private boolean testHelloword() throws Exception {		
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					new HelloWorld().execute();
					
					Log.d(TAG, "exit test");
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    
			}
		}).start();

		return true;
	}
	
	private class TcpTest extends Host {

		@Override
		public String content() {
			// TODO Auto-generated method stub
			String content = "Log.d('RhinoTest', 'TcpTest, js');";
			
			// tcp server
			content += 
					"var srv = TCP.createServer(NCC, function(socket){" +
			        "  socket.pipe(socket);" +
					"});";
			
			content +=
					"srv.listen('0.0.0.0', 51668, function(){" +
					"  Log.d('RhinoTest', 'TCP server listening on 0.0.0.0:51668');" +
					"});";
			
			// tcp client
			content += 
					"NCC.setTimeout(function(){";
					
			content += 
					"var cln = TCP.connect(NCC, 'localhost', 51668, null, -1, function(){" +
					"  Log.d('RhinoTest', 'connected done');" +
					"" +
					"  cln.setEncoding('utf-8');" +
					"" +
					"  cln.onData(function(data){" +
					"    Log.d('RhinoTest', 'tcp:'+data.toString());" +
					"  });" +
					"" +
					"  NCC.setInterval(function(){" +
					"    cln.write('Hello js', 'utf-8', null);" +
					"  }, 2000);" +
					"});";
			
			content += 
					"}, 2000);";
			
			return content;
		}
		
	}
	private boolean testTcp() throws Exception {		
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					new TcpTest().execute();
					
					Log.d(TAG, "exit test");
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    
			}
		}).start();

		return true;
	}
	
	public RhinoTest(){
	}
	
	public void start() {
		try {
			testHelloword();
			testTcp();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
