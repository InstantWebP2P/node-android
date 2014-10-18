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
					"  Log.d('RhinoTest', 'TCP connected done');" +
					"" +
					"  cln.setEncoding('utf-8');" +
					"" +
					"  cln.onData(function(data){" +
					"    Log.d('RhinoTest', 'tcp:'+data.toString());" +
					"  });" +
					"" +
					"  NCC.setInterval(function(){" +
					"    cln.write('Hello js TCP', 'utf-8', null);" +
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

	private class UdtTest extends Host {

		@Override
		public String content() {
			// TODO Auto-generated method stub
			String content = "Log.d('RhinoTest', 'UdtTest, js');";
			
			// tcp server
			content += 
					"var srv = UDT.createServer(NCC, function(socket){" +
			        "  socket.pipe(socket);" +
					"});";
			
			content +=
					"srv.listen('0.0.0.0', 51668, function(){" +
					"  Log.d('RhinoTest', 'UDT server listening on 0.0.0.0:51668');" +
					"});";
			
			// tcp client
			content += 
					"NCC.setTimeout(function(){";
					
			content += 
					"var cln = UDT.connect(NCC, 'localhost', 51668, null, -1, function(){" +
					"  Log.d('RhinoTest', 'UDT connected done');" +
					"" +
					"  cln.setEncoding('utf-8');" +
					"" +
					"  cln.onData(function(data){" +
					"    Log.d('RhinoTest', 'UDT:'+data.toString());" +
					"  });" +
					"" +
					"  NCC.setInterval(function(){" +
					"    cln.write('Hello js UDT', 'utf-8', null);" +
					"  }, 2000);" +
					"});";
			
			content += 
					"}, 2000);";
			
			return content;
		}
		
	}
	private boolean testUdt() throws Exception {		
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					new UdtTest().execute();
					
					Log.d(TAG, "exit test");
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    
			}
		}).start();

		return true;
	}
	
	private class HttpTest extends Host {

		@Override
		public String content() {
			// TODO Auto-generated method stub
			String content = "Log.d('RhinoTest', 'HttpTest, js');";
			
			// http server
			content += 
					"var srv = http.createServer(NCC, function(req, res){" +
			        "  Log.d('RhinoTest', 'req.url:'+req.url()+',headers:'+req.headers());" +
			        "  res.setHeader('from', 'Rhino js http');" +
			        "  res.writeHead(200);" +
			        "  res.end('http from js', 'utf-8', null);" +
					"});";
			
			content +=
					"srv.listen(51669, '0.0.0.0', function(){" +
					"  Log.d('RhinoTest', 'Http server listening on 0.0.0.0:51669');" +
					"});";
			
			// http request
			content += 
					"NCC.setTimeout(function(){" +
			        "  Log.d('RhinoTest', 'http get ...');";

			content += 
					"  NCC.setInterval(function(){" +
					"    Log.d('RhinoTest', 'http get iteration ');";
			
			content += 
					"    http.get(NCC, 'http://localhost:51669', function(res){" +
			
					"      Log.d('RhinoTest', 'http got response, headers:'+res.headers());" +
					"" +
					"      res.setEncoding('utf-8');" +
					"" +
					"      res.on('data', function(data){" +
					"        Log.d('RhinoTest', 'http:'+data.toString());" +
					"      });" +
					
					"    });";
			
			content += 
					"  }, 2000);";
			
			content += 
					"}, 2000);";
			return content;
		}
		
	}
	private boolean testHttp() throws Exception {		
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					new HttpTest().execute();
					
					Log.d(TAG, "exit test");
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    
			}
		}).start();

		return true;
	}

	private class HttppTest extends Host {

		@Override
		public String content() {
			// TODO Auto-generated method stub
			String content = "Log.d('RhinoTest', 'HttppTest, js');";
			
			// http server
			content += 
					"var srv = httpp.createServer(NCC, function(req, res){" +
			        "  Log.d('RhinoTest', 'httpp req.url:'+req.url()+',headers:'+req.headers());" +
			        "  res.setHeader('from', 'Rhino js httpp');" +
			        "  res.writeHead(200);" +
			        "  res.end('httpp from js', 'utf-8', null);" +
					"});";
			
			content +=
					"srv.listen(51669, '0.0.0.0', function(){" +
					"  Log.d('RhinoTest', 'Httpp server listening on 0.0.0.0:51669');" +
					"});";
			
			// http request
			content += 
					"NCC.setTimeout(function(){" +
			        "  Log.d('RhinoTest', 'httpp get ...');";

			content += 
					"  NCC.setInterval(function(){" +
					"    Log.d('RhinoTest', 'httpp get iteration ');";
			
			content += 
					"    httpp.get(NCC, 'http://localhost:51669', function(res){" +
			
					"      Log.d('RhinoTest', 'httpp got response, headers:'+res.headers());" +
					"" +
					"      res.setEncoding('utf-8');" +
					"" +
					"      res.on('data', function(data){" +
					"        Log.d('RhinoTest', 'httpp:'+data.toString());" +
					"      });" +
					
					"    });";
			
			content += 
					"  }, 2000);";
			
			content += 
					"}, 2000);";
			return content;
		}
		
	}
	private boolean testHttpp() throws Exception {		
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					new HttppTest().execute();
					
					Log.d(TAG, "exit test");
				} catch (Throwable e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    
			}
		}).start();

		return true;
	}
	private class WebSocketTest extends Host {

		@Override
		public String content() {
			// TODO Auto-generated method stub
			String content = "Log.d('RhinoTest', 'WebSocketTest, js');";
			
			// websocket server
			content += 
					"var wssopt = new WebSocketServer.Options();" +
					"wssopt.port = 6869;" +
					"wssopt.path = '/wspp';" +
		
					"var wss = new WebSocketServer(NCC, wssopt, function(){" +
			        "  Log.d('RhinoTest', 'websocket server listening ... '+wssopt.port);" +
			        "" +
			        "  var ws = new WebSocket(NCC, 'ws://localhost:6869/wspp', null, new WebSocket.Options());" +
			        "" +
			        "  ws.onmessage(function(event){" +
			        "    if (event.isBinary()) {" +
			        "      Log.d('RhinoTest', 'ws binary message: '+event.getData().toString());" +
			        "    } else {" +
			        "      Log.d('RhinoTest', 'ws text message: '+event.getData().toString());" +
			        "    }" +
			        "  });" +
			        "" +
			        "  ws.onerror(function(event){" +
                    "    Log.d('RhinoTest', 'ws error:'+event.getCode()+',message:'+event.getError());" +
                    "  });" +
                    "" +
                    "  ws.onopen(function(){" +
                    "    ws.send('Hello, Am Tom.', new WebSocket.SendOptions(false, false), null);" +
                    "" +
                    "    NCC.setInterval(function(){" +
                    "      ws.send('Hello, tom zhou @'+new Date(), new WebSocket.SendOptions(false, true), null);" +
                    "    }, 3000);" +
                    "  });" +
                    "" +
					"});" +
					"";
			
			content +=
					"wss.onconnection(function(socket){" +
					"  Log.d('RhinoTest', 'new ws client:'+socket.toString());" +
					"" +
					"  socket.onmessage(function(event){" +
					"    Log.d('RhinoTest', 'client message: '+event.toString());" +
					"" +
				    "    if (event.isBinary()) {" +
			        "      Log.d('RhinoTest', 'client binary message: '+event.getData().toString());" +
			        "" +
			        "      socket.send(event.getData(), new WebSocket.SendOptions(true, true), null);" +
			        "    } else {" +
			        "      Log.d('RhinoTest', 'client text message: '+event.getData().toString());" +
			        "" +
			        "      socket.send(event.getData().toString()+'@srv', new WebSocket.SendOptions(false, false), null);" +
			        "    }" +
					"  });" +
					"" +
					"  socket.send('Hello@srv', new WebSocket.SendOptions(false, false), null);" +
					"});";
			
			
			return content;
		}
		
	}
	
	private boolean testWebsocket() throws Exception {		
		new Thread(new Runnable() {
			public void run() {
				Log.d(TAG, "start test");
				
				try {
					new WebSocketTest().execute();
					
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
			testUdt();

			testHttp();
			testHttpp();
			
			testWebsocket();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
