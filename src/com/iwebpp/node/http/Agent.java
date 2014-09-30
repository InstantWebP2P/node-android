// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.http;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.NodeContext;
import com.iwebpp.node.Util;
import com.iwebpp.node.net.AbstractSocket;
import com.iwebpp.node.net.AbstractSocket.ConnectListener;
import com.iwebpp.node.net.TCP;
import com.iwebpp.node.net.UDT;

//New Agent code.

//The largest departure from the previous implementation is that
//an Agent instance holds connections for a variable number of host:ports.
//Surprisingly, this is still API compatible as far as third parties are
//concerned. The only code that really notices the difference is the
//request object.

//Another departure is that all code related to HTTP parsing is in
//ClientRequest.onSocket(). The Agent is now *strictly*
//concerned with managing a connection pool.

public abstract class Agent 
extends EventEmitter2 {
	private final static String TAG = "Agent";

	public static final int defaultMaxSockets = Integer.MAX_VALUE;
	private int defaultPort;
	/**
	 * @return the defaultPort
	 */
	public int defaultPort() {
		return defaultPort;
	}

	private String protocol;
	/**
	 * @return the protocol
	 */
	public String protocol() {
		return protocol;
	}

	private ReqOptions options;
	private Map<String, List<ClientRequest>> requests;
	/**
	 * @return the requests
	 */
	public Map<String, List<ClientRequest>> requests() {
		return requests;
	}

	private Map<String, List<AbstractSocket>> sockets;
	/**
	 * @return the sockets
	 */
	public Map<String, List<AbstractSocket>> sockets() {
		return sockets;
	}

	private Map<String, List<AbstractSocket>> freeSockets;
	private int keepAliveMsecs;
	private boolean keepAlive;
	private int maxSockets;
	private int maxFreeSockets;
	private NodeContext context;

	public Agent(NodeContext ctx, ReqOptions options) throws Exception {
		final Agent self = this;

		self.context = ctx;

		self.defaultPort = 80;
		self.protocol = "http:";

		self.options = options;

		// don't confuse net and make it think that we're connecting to a pipe
		self.options.path = null;
		self.requests = new Hashtable<String, List<ClientRequest>>();
		self.sockets = new Hashtable<String, List<AbstractSocket>>();
		self.freeSockets = new Hashtable<String, List<AbstractSocket>>();
		self.keepAliveMsecs = self.options.keepAliveMsecs > 0 ? self.options.keepAliveMsecs : 1000;
		self.keepAlive = self.options.keepAlive;/// || false;
		self.maxSockets = self.options.maxSockets > 0 ? self.options.maxSockets : Agent.defaultMaxSockets;
		self.maxFreeSockets = self.options.maxFreeSockets > 0 ?  self.options.maxFreeSockets : 256;

		self.on("free", new Listener() {

			@Override
			public void onEvent(Object raw) throws Exception {
				socket_options_b data = (socket_options_b)raw;
				AbstractSocket   socket = data.socket;
				ReqOptions options = data.options;

				String name = self.name(options);
				Log.d(TAG, "agent.on(free) " + name);

				if (!socket.isDestroyed() &&
						self.requests.containsKey(name) && 
						self.requests.get(name).size() > 0
						) {
					///self.requests[name].shift().onSocket(socket);
					self.requests.get(name).remove(0).onSocket(socket);

					if (self.requests.get(name).size() == 0) {
						// don't leak
						///delete self.requests[name];
						self.requests.remove(name);
					}
				} else {
					// If there are no pending requests, then put it in
					// the freeSockets pool, but only if we're allowed to do so.
					ClientRequest req = (ClientRequest) socket.get_httpMessage();
					if (req!=null &&
							req.shouldKeepAlive &&
							!socket.isDestroyed() &&
							self.options.keepAlive) {
						List<AbstractSocket> freeSockets = self.freeSockets.containsKey(name) ? self.freeSockets.get(name) : null;
						int freeLen = freeSockets != null ? freeSockets.size() : 0;
						int count = freeLen;

						if (self.sockets.containsKey(name))
							count += self.sockets.get(name).size();

						if (count >= self.maxSockets || freeLen >= self.maxFreeSockets) {
							self.removeSocket(socket, options);
							socket.destroy(null);
						} else {
							freeSockets = freeSockets!=null ? freeSockets : new LinkedList<AbstractSocket>();
							self.freeSockets.put(name, freeSockets);
							socket.setKeepAlive(true, self.keepAliveMsecs);
							socket.unref();
							socket.set_httpMessage(null);
							self.removeSocket(socket, options);
							freeSockets.add(socket);
						}
					} else {
						self.removeSocket(socket, options);
						socket.destroy(null);
					}
				}

			}

		});

	}

	// POJO beans
	public static final class socket_options_b {
		public AbstractSocket socket;
		public ReqOptions     options;

		public socket_options_b(AbstractSocket socket, ReqOptions options) {
			this.socket  = socket;
			this.options = options;
		}
	}


	// Get the key for a given set of request options
	public String name(ReqOptions options) {
		String name = "";

		if (!Util.zeroString(options.host))
			name += options.host;
		else
			name += "localhost";

		name += ":";

		if (options.port > 0)
			name += options.port;

		name += ':';

		if (!Util.zeroString(options.localAddress))
			name += options.localAddress;

		name += ':';

		if (options.localPort > 0)
			name += options.localPort;

		return name;
	}

	public void addRequest(ClientRequest req, ReqOptions options) throws Exception {
		// Legacy API: addRequest(req, host, port, path)
		/*if (typeof options === 'string') {
	    options = {
	      host: options,
	      port: arguments[2],
	      path: arguments[3]
	    };
	  }*/

		String name = this.name(options);
		/*if (!this.sockets[name]) {
	    this.sockets[name] = [];
	  }*/
		if (!this.sockets.containsKey(name))
			this.sockets.put(name, new LinkedList<AbstractSocket>());

		///var freeLen = this.freeSockets[name] ? this.freeSockets[name].length : 0;
		///var sockLen = freeLen + this.sockets[name].length;
		int freeLen = this.freeSockets.containsKey(name) ? this.freeSockets.get(name).size() : 0;
		int sockLen = freeLen + this.sockets.get(name).size();

		if (freeLen > 0) {
			// we have a free socket, so use that.
			AbstractSocket socket = this.freeSockets.get(name).remove(0);
			Log.d(TAG, "have free socket");

			// don't leak
			if (this.freeSockets.get(name).isEmpty())
				this.freeSockets.remove(name);

			socket.ref();

			req.onSocket(socket);

			this.sockets.get(name).add(socket);
		} else if (sockLen < this.maxSockets) {
			Log.d(TAG, "call onSocket " + sockLen + " " + freeLen);
			// If we are under maxSockets create a new one.
			req.onSocket(this.createSocket(req, options));
		} else {
			Log.d(TAG, "wait for socket");
			// We are over limit so we'll add it to the queue.
			if (!this.requests.containsKey(name)) {
				this.requests.put(name, new LinkedList<ClientRequest>());
			}
			this.requests.get(name).add(req);
		}
	}

	/**
	 * @return the maxSockets
	 */
	public int maxSockets() {
		return maxSockets;
	}

	/**
	 * @return the keepAlive
	 */
	public boolean keepAlive() {
		return keepAlive;
	}

	// Abstract interface
	protected abstract AbstractSocket createConnection(
			NodeContext ctx, 
			String address, int port,
			String localAddress, int localPort,
			final AbstractSocket.ConnectListener cb) throws Exception;

	public AbstractSocket createSocket(ClientRequest req, final ReqOptions options) throws Exception {
		final Agent self = this;
		///options = util._extend({}, options);
		///options = util._extend(options, self.options);

		options.servername = options.host;
		if (req!=null) {
			List<String> hostHeader = req.getHeader("host");
			if (hostHeader!=null && hostHeader.size()>0) {
				String hh = hostHeader.get(0);
				options.servername = hh.replaceAll(":.*$", "");
			}
		}

		String name = self.name(options);

		Log.d(TAG, "createConnection "+name+" "+options);

		options.encoding = null;
		final AbstractSocket s = createConnection(
				context, 
				options.servername, 
				options.port, 
				options.localAddress, 
				options.localPort,
				null);

		if (!self.sockets.containsKey(name)) {
			self.sockets.put(name, new LinkedList<AbstractSocket>());
		}
		this.sockets.get(name).add(s);

		Log.d(TAG, "sockets "+ name+" "+ this.sockets.get(name).size());

		final Listener onFree = new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				self.emit("free", new socket_options_b(s, options));
			}

		};
		s.on("free", onFree);

		final Listener onClose = new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				Log.d(TAG, "CLIENT socket onClose");
				// This is the only place where sockets get removed from the Agent.
				// If you want to remove a socket from the pool, just close it.
				// All socket errors end in a close event anyway.
				self.removeSocket(s, options);
			}

		};
		s.on("close", onClose);

		final Listener onRemove = new Listener(){

			@Override
			public void onEvent(Object data) throws Exception {
				// We need this function for cases like http 'upgrade'
				// (defined by WebSockets) where we need to remove a socket from the
				// pool because it'll be locked up indefinitely
				Log.d(TAG, "CLIENT socket onRemove");
				self.removeSocket(s, options);
				s.removeListener("close", onClose);
				s.removeListener("free", onFree);
				s.removeListener("agentRemove", this);
			}

		};
		s.on("agentRemove", onRemove);

		return s;
	}

	public void removeSocket(AbstractSocket s, ReqOptions options) throws Exception {
		String name = this.name(options);

		Log.d(TAG, "removeSocket "+ name+ " destroyed:" + s.isDestroyed());

		if (this.sockets.containsKey(name))
			if (this.sockets.get(name).contains(s)) {
				this.sockets.get(name).remove(s);

				if (this.sockets.get(name).size() == 0)
					this.sockets.remove(name);
			}

		// If the socket was destroyed, remove it from the free buffers too.
		if (s.isDestroyed())
			if (this.freeSockets.containsKey(name))
				if (this.freeSockets.get(name).contains(s)) {
					this.freeSockets.get(name).remove(s);

					if (this.freeSockets.get(name).size() == 0)
						this.freeSockets.remove(name);
				}

		if (this.requests.containsKey(name) && this.requests.get(name).size() > 0) {
			Log.d(TAG, "removeSocket, have a request, make a socket");
			ClientRequest req = this.requests.get(name).get(0);
			// If we have pending requests and a socket gets closed make a new one
			this.createSocket(req, options).emit("free");
		}

	}

	public void destroy() throws Exception {
		/*
	  var sets = [this.freeSockets, this.sockets];
	  sets.forEach(function(set) {
	    Object.keys(set).forEach(function(name) {
	      set[name].forEach(function(socket) {
	        socket.destroy();
	      });
	    });
	  });*/

		for (List<AbstractSocket> ss : this.freeSockets.values())
			for (AbstractSocket s : ss)
				s.destroy(null);

		for (List<AbstractSocket> ss : this.sockets.values())
			for (AbstractSocket s : ss)
				s.destroy(null);
	}

	///public static Agent globalAgent = new Agent();

	// HTTP agent
	public static class HttpAgent 
	extends Agent {

		public HttpAgent(NodeContext ctx, ReqOptions options) throws Exception {
			super(ctx, options);
			// TODO Auto-generated constructor stub
		}
		private HttpAgent() throws Exception{super(null, null);}
		
		@Override
		protected AbstractSocket createConnection(
				NodeContext ctx,
				String address, int port, 
				String localAddress, int localPort,
				ConnectListener cb) throws Exception {
			// TODO Auto-generated method stub
			return TCP.createConnection(ctx, address, port, localAddress, localPort, cb);
		}
		
	}
	
	// HTTPP agent
	public static class HttppAgent 
	extends Agent {

		public HttppAgent(NodeContext ctx, ReqOptions options) throws Exception {
			super(ctx, options);
			// TODO Auto-generated constructor stub
		}
		private HttppAgent() throws Exception{super(null, null);}
		
		@Override
		protected AbstractSocket createConnection(
				NodeContext ctx,
				String address, int port, 
				String localAddress, int localPort,
				ConnectListener cb) throws Exception {
			// TODO Auto-generated method stub
			return UDT.createConnection(ctx, address, port, localAddress, localPort, cb);
		}
		
	}
	
}
