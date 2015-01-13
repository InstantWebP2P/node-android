package com.iwebpp.nodeandroid.test;

import android.app.Instrumentation;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.iwebpp.node.js.rhino.Host;
import com.iwebpp.nodeandroid.MainActivity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by jasm on 1/12/15.
 */
public class MainActivityInstrumentationTestCase extends ActivityInstrumentationTestCase2<MainActivity> {

    private Intent intent;
    private MainActivity activity;
    private Instrumentation instrumentation;

    public MainActivityInstrumentationTestCase() {
        super(MainActivity.class);
    }

    /**
     *
     * TODO figure out how to run Activity#onOptionsMenuSelected
     * from the unit test
     *
     * @throws Exception
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        this.instrumentation = super.getInstrumentation();
        this.activity = (MainActivity) super.getActivity();

        intent = new Intent();
        intent.putExtra("js", "null"); // put something useful in the unit tests
        setActivityIntent(intent);
    }

    private void runScript(final String js) {
        Class<?> c = null;
        try {
            c = MainActivity.class;
            Method  method = c.getDeclaredMethod ("runScript", new Class[] { String.class });
            method.invoke(activity, js);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public void testHelloWorld()
    {
        runScript("Log.d('RhinoTest', 'Helloworld, js');");
    }

    /** TODO use TextUtils#concat */
    public void testTcp()
    {
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

        runScript(content);
    }

    public void testUdt()
    {
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

        runScript(content);
    }

    public void testHttp()
    {
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

        runScript(content);
    }

    public void testHttpp()
    {
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

        runScript(content);
    }

    public void testWebSocket ()
    {
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


        runScript(content);
    }

    public void testRequire()
    {
        String content = "Log.d('RhinoTest', 'RequireTest, js');";

        // require module
        content += "var any = require('any.js'); Log.d('RhinoTest', 'modulepath:'+any.modulepath);";
        content += "var tom = require('tom.js'); Log.d('RhinoTest', 'modulepath:'+any.modulepath);";

        runScript(content);
    }

    public void testEmitter()
    {
        String content = "Log.d('RhinoTest', 'EventEmitterTest, js');";

        // extends JS obj from java EventEmitter2 class
        content +=
                "var eventjs = {where: function(){return 'js';}};" +
                        "var event = new JavaAdapter(EventEmitter2, eventjs);" +
                        "" +
                        "event.on('js', function(data){" +
                        "  Log.d('RhinoTest', 'js event:'+data);" +
                        "});" +
                        "" +
                        "event.on('java', function(data){" +
                        "  Log.d('RhinoTest', 'java event:'+data);" +
                        "});" +
                        "" +
                        "event.emit('js', 'emit from js haha');" +
                        "" +
                        "event.emit('java', 'emit for java haha');" +
                        "";

        runScript(content);
    }

    public void testStream()
    {
        String content = "Log.d('RhinoTest', 'StreamTest, js');";

        // extends JS obj from java EventEmitter2 class
        content +=
                "var rcnt = 0;" +
                        "var readjs = {" +
                        "  _read: function(n){" +
                        "    if (rcnt < 68) " +
                        "      this.push(''+rcnt++, 'utf-8');" +
                        "    else " +
                        "      this.push(null, null);" +
                        "  }" +
                        "};" +
                        "" +
                        "var readobj = new JavaAdapter(Readable2, readjs, NCC, new Readable2.Options(16, 'utf8', false, 'utf8', true));" +
                        "" +
                        "readobj.on('readable', function(){" +
                        "  Log.d('RhinoTest', 'js testRead_less: start...');" +
                        "" +
                        "  var chunk;" +
                        "  while (null != (chunk = readobj.read(3))) {" +
                        "    Log.d(TAG, 'testRead_less:'+chunk.toString());" +
                        "  }" +
                        "" +
                        "  Log.d('RhinoTest', 'js testRead_less: ...end');" +
                        "" +
                        "});" +
                        "";

        runScript(content);
    }

    public void testExtension()
    {
        String content = "Log.d('RhinoTest', 'ExtensionTest, js');";

			/*
			 * js> // test non-empty constructor with protected field
			 * js> x = JavaAdapter(java.util.Vector, {test: function() {return this.elementData.length;}}, 20);
			 * []
			 * js> x.test()
			 *
			 * 20
			 * */
        content +=
                "var x = new JavaAdapter(java.util.Vector, {test: function() {return this.elementData.length;}}, 20);" +
                        "Log.d('RhinoTest', 'extension test:'+x.test());";

        runScript(content);
    }

}
