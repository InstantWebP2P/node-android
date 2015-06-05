// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.tests;

import android.util.Log;

import com.iwebpp.libuvpp.handles.TimerHandle;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.EventEmitter.Listener;
import com.iwebpp.node.NodeContext.IntervalListener;
import com.iwebpp.node.NodeContext.TimeoutListener;
import com.iwebpp.node.net.AbstractServer.CloseListener;
import com.iwebpp.node.net.AbstractServer.ListeningCallback;
import com.iwebpp.node.net.AbstractSocket.ConnectListener;
import com.iwebpp.node.net.UDT;
import com.iwebpp.node.net.UDT.Server;
import com.iwebpp.node.net.UDT.Socket;
import com.iwebpp.node.stream.Writable.WriteCB;

import junit.framework.TestCase;

public final class UdtTest extends TestCase {
    private static final String TAG = "UdtTest";
    private NodeContext ctx;

    public void testListening() throws Exception {
        Server srv;
        srv = new UDT.Server(ctx, new Server.Options(false), null);

        srv.listen("0.0.0.0", 51688, new ListeningCallback() {

            @Override
            public void onListening() {
                Log.d(TAG, "UDT server listening on 0.0.0.0:51688");
            }

        });
    }

    public void testClosure() throws Exception {
        final int port = 52688;

        final Server srv;
        final Socket cln;
        srv = new UDT.Server(ctx, new Server.Options(false), null);

        srv.listen("0.0.0.0", port, new ListeningCallback() {

            @Override
            public void onListening() {
                Log.d(TAG, "UDT server listening on 0.0.0.0:" + port);
            }

        });

        cln = new UDT.Socket(ctx, new Socket.Options(null, false, false, true));

        ///cln.setEncoding("utf8");

        cln.connect(port, new ConnectListener() {

            @Override
            public void onConnect() throws Exception {

                Log.d(TAG, "got connected");

                cln.on("close", new Listener() {

                    @Override
                    public void onEvent(Object data) throws Exception {
                        Log.d(TAG, "client closed");
                    }

                });

                // close client
                cln.end(null, null, null);

            }
        });

        // close server after 6s
        ctx.setTimeout(new TimeoutListener() {

            @Override
            public void onTimeout() throws Exception {
                srv.close(new CloseListener() {

                    @Override
                    public void onClose(String error) {
                        if (Util.zeroString(error))
                            Log.d(TAG, "server closed ok");
                        else {
                            Log.d(TAG, "server closed failed " + error);
                            fail("server closed failed " + error);
                        }

                    }

                });

            }

        }, 6000);
    }

    public void testListening6() throws Exception {
        Server srv;
        srv = new UDT.Server(ctx, new Server.Options(false), null);

        srv.listen("::", 51866, new ListeningCallback() {

            @Override
            public void onListening() {
                Log.d(TAG, "UDT server listening on IPv6 :::51866");
            }

        });
    }

    public void testConnect6() throws Exception {
        Server srv;
        final Socket cln;
        int port = 51868;

        srv = new UDT.Server(ctx, new Server.Options(false), null);

        srv.on("connection", new Listener() {

            @Override
            public void onEvent(Object data) throws Exception {
                Socket peer = (Socket) data;

                peer.pipe(peer, true);

                Log.d(TAG, "got connection 6 then echo it");
            }

        });
        srv.listen("::", port, null);

        cln = new UDT.Socket(ctx, new Socket.Options(null, false, false, true));

        cln.setEncoding("utf8");

        cln.connect(6, "::1", port, new ConnectListener() {

            @Override
            public void onConnect() throws Exception {

                Log.d(TAG, "got connected 6");

                cln.on("readable", new Listener() {

                    @Override
                    public void onEvent(Object data) throws Exception {
                        Object chunk;

                        while (null != (chunk = cln.read(68))) {
                            Log.d(TAG, "client read: " + Util.chunkToString(chunk, "utf8"));
                        }

                    }

                });

                ///while (cln.write("hello word", "utf-8", new WriteCB(){
                cln.write("hello word", "utf-8", new WriteCB() {

                    @Override
                    public void writeDone(String error) throws Exception {
                        Log.d(TAG, "client write done @" + System.currentTimeMillis());
                        fail("client write done @" + System.currentTimeMillis());
                    }

                });

                cln.on("drain", new Listener() {

                    @Override
                    public void onEvent(Object data) throws Exception {
                        Log.d(TAG, "client write drained");

                        ///while (cln.write("hello word: "+System.currentTimeMillis(), "utf-8", null));
                    }

                });

                // write after 2s
                TimerHandle interval = ctx.setInterval(new IntervalListener() {

                    @Override
                    public void onInterval() throws Exception {
                        cln.write("hello word IPv6: " + System.currentTimeMillis(), "utf-8", null);
                    }

                }, 2000);
                ///ctx.clearInterval(interval);
            }

        });
    }

    public void testConnect() throws Exception {
        Server srv;
        final Socket cln;
        int port = 51686;

        srv = new UDT.Server(ctx, new Server.Options(false), null);

        srv.on("connection", new Listener() {

            @Override
            public void onEvent(Object data) throws Exception {
                Socket peer = (Socket) data;

                peer.pipe(peer, true);

                Log.d(TAG, "got connection then echo it");
            }

        });
        srv.listen("0.0.0.0", port, null);

        cln = new UDT.Socket(ctx, new Socket.Options(null, false, false, true));

        cln.setEncoding("utf8");

        cln.connect(port, new ConnectListener() {

            @Override
            public void onConnect() throws Exception {

                Log.d(TAG, "got connected");

                cln.on("readable", new Listener() {

                    @Override
                    public void onEvent(Object data) throws Exception {
                        Object chunk;

                        while (null != (chunk = cln.read(68))) {
                            Log.d(TAG, "client read: " + Util.chunkToString(chunk, "utf8"));
                        }

                    }

                });

                ///while (cln.write("hello word", "utf-8", new WriteCB(){
                cln.write("hello word", "utf-8", new WriteCB() {

                    @Override
                    public void writeDone(String error) throws Exception {
                        Log.d(TAG, "client write done @" + System.currentTimeMillis());
                        fail("client write done @" + System.currentTimeMillis());
                    }

                });

                cln.on("drain", new Listener() {

                    @Override
                    public void onEvent(Object data) throws Exception {
                        Log.d(TAG, "client write drained");

                        ///while (cln.write("hello word: "+System.currentTimeMillis(), "utf-8", null));
                    }

                });

                // write after 2s
                TimerHandle interval = ctx.setInterval(new IntervalListener() {

                    @Override
                    public void onInterval() throws Exception {
                        cln.write("hello word: " + System.currentTimeMillis(), "utf-8", null);
                    }

                }, 2000);
                ///ctx.clearInterval(interval);
            }

        });
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.ctx = new NodeContext();
    }
}
