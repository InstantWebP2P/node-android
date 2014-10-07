// Copyright (c) 2014 Tom Zhou<iwebpp@gmail.com>


package com.iwebpp.node.http;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.iwebpp.node.NodeContext;
import com.iwebpp.node.net.AbstractSocket;

public class ReqOptions {
	
	public String path;
	public String host;
	public int port = -1;
	public String localAddress;
	public int localPort = -1;
	public String servername;
	public String encoding;
	
	public Agent agent;
	public createConnectionF createConnection;
	public String protocol;
	public String auth;
	public int defaultPort = 80;
	public String hostname;
	public boolean setHost = false;
	public String socketPath;
	public String method;
	public Map<String, List<String>> headers;

	// Agent specific
	public int maxFreeSockets = 256;
	public int maxSockets = 5;
	public boolean keepAlive;
	public int keepAliveMsecs;
	
	// Protocol select: http or httpp
	// - httpp: true, run http over udt; otherwise run http over tcp
	public boolean httpp = false;
	
	public interface createConnectionF {
		public AbstractSocket createConnection(
				NodeContext ctx, 
				String address, int port,
				String localAddress, int localPort,
				final AbstractSocket.ConnectListener cb) throws Exception;
	}

	public ReqOptions(){
		this.headers = new Hashtable<String, List<String>>();
	}
	
}
