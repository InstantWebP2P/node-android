package com.iwebpp.middleware;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;

import com.iwebpp.SimpleDebug;
import com.iwebpp.node.http.HttpServer.requestListener;
import com.iwebpp.node.http.IncomingMessage;
import com.iwebpp.node.http.ServerResponse;

/**
 * @description Static file server from android Assert directory
 * */
public class Static extends SimpleDebug 
implements requestListener {

	private Context ctx;

	private String rootPath;

	private AssetManager assertManager;

	@Override
	public void onRequest(IncomingMessage req, ServerResponse res)
			throws Exception {
		// TODO Auto-generated method stub
		String filePath = rootPath + req.url();

		// check content type


		// get file from assert
		String tContents = "";
		try {
			InputStream stream = assertManager.open(filePath);

			int size = stream.available();
			byte[] buffer = new byte[size];
			stream.read(buffer);
			stream.close();
			tContents = new String(buffer, "utf-8");
		} catch (IOException e) {
			// Handle exceptions here
		}

		// return response
	}

	public Static(Context ctx, String root) {
		this.ctx = ctx;
		this.rootPath = root.charAt(root.length()-1) == '/' ? root : root + "/";
		this.assertManager = ctx.getAssets();
	}
	public Static(Context ctx) {
		this(ctx, "/www/");
	}

}
