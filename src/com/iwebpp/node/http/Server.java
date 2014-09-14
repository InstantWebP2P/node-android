package com.iwebpp.node.http;

import java.nio.ByteBuffer;

import com.iwebpp.node.EventEmitter2;
import com.iwebpp.node.TCP;
import com.iwebpp.node.TCP.Socket;

public class Server 
extends EventEmitter2 {

	///server.listen(port, [hostname], [backlog], [callback])
	public int listen(
			int port, 
			String hostname, 
			int backlog, 
			ListeningCallback cb) throws Exception {


		return 0;
	}
	public static interface ListeningCallback {
		public void onListening() throws Exception;
	}

	public int close(final closeListener cb) throws Exception {
		if (cb != null) onClose(cb);

		return 0;
	}

	public int maxHeadersCount(int max) {
		return max;
	}

	///server.setTimeout(msecs, callback)
	///server.timeout

	// Event listeners
	public void onRequest(final requestListener cb) throws Exception {
		this.on("request", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {
				request_response_t data = (request_response_t)raw;

				cb.onRequest(data.request, data.response);
			}

		});
	}
	public static interface requestListener {
		public void onRequest(IncomingMessage req, ServerResponse res) throws Exception;
	}

	public void onConnection(final connectionListener cb) throws Exception {
		this.on("connection", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {
				TCP.Socket data = (TCP.Socket)raw;

				cb.onConnection(data);
			}

		});
	}
	public static interface connectionListener {
		public void onConnection(TCP.Socket socket) throws Exception;
	}

	public void onClose(final closeListener cb) throws Exception {
		this.on("close", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {                   
				cb.onClose();
			}

		});
	}
	public static interface closeListener {
		public void onClose() throws Exception;
	}

	public void onCheckContinue(final checkContinueListener cb) throws Exception {
		this.on("checkContinue", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {
				request_response_t data = (request_response_t)raw;

				cb.onCheckContinue(data.request, data.response);
			}

		});
	}
	public static interface checkContinueListener {
		public void onCheckContinue(IncomingMessage req, ServerResponse res) throws Exception;
	}

	public void onCheckContinue(final connectListener cb) throws Exception {
		this.on("connect", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {
				request_socket_head_t data = (request_socket_head_t)raw;

				cb.onConnect(data.request, data.socket, data.head);
			}

		});
	}
	public static interface connectListener {
		public void onConnect(IncomingMessage request, Socket socket, ByteBuffer head) throws Exception;
	}

	public void onUpgrade(final upgradeListener cb) throws Exception {
		this.on("upgrade", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {
				request_socket_head_t data = (request_socket_head_t)raw;

				cb.onUpgrade(data.request, data.socket, data.head);
			}

		});
	}
	public static interface upgradeListener {
		public void onUpgrade(IncomingMessage request, Socket socket, ByteBuffer head) throws Exception;
	}

	public void onClientError(final clientErrorListener cb) throws Exception {
		this.on("upgrade", new Listener(){

			@Override
			public void onListen(Object raw) throws Exception {
				exception_socket_t data = (exception_socket_t)raw;

				cb.onClientError(data.exception, data.socket);
			}

		});
	}
	public static interface clientErrorListener {
		public void onClientError(String exception, Socket socket) throws Exception;
	}

	// POJO beans
	private class request_response_t {
		IncomingMessage request;
		ServerResponse  response;
	}
	private class request_socket_head_t {
		IncomingMessage request;
		TCP.Socket      socket;
		ByteBuffer      head;
	}
	private class exception_socket_t {
		String     exception;
		TCP.Socket socket;
	}

}
