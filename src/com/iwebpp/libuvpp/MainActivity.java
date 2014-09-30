package com.iwebpp.libuvpp;

import com.iwebpp.libuvpp.R;
import com.iwebpp.libuvpp.tests.*;
import com.iwebpp.node.tests.EE2Test;
import com.iwebpp.node.tests.HttpParserTest;
import com.iwebpp.node.tests.HttpTest;
import com.iwebpp.node.tests.HttppTest;
import com.iwebpp.node.tests.StreamTest;
import com.iwebpp.node.tests.TcpTest;
import com.iwebpp.node.tests.UdtTest;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// UVPP Unit tests 
		/*new VersionTest().start();
		new CWDTest().start();
		new TCPHandleTest().start();
		new UDTHandleTest().start();
		new UDPHandleTest().start();
		new LoopHandleTest().start();
		new IdleHandleTest().start();
		new TimerHandleTest().start();
		new AsyncHandleTest().start();
		*/
		///new PipeHandleTest().start();
		///new ProcessHandleTest().start();
		///new TCPHandleTest().start();

		///new FileEventHandleTest().start();
		
		// Node Unit tests
		///new EE2Test().start();
		///new StreamTest().start();
        ///new TcpTest().start();
        ///new UdtTest().start();
		new HttpParserTest().start();
		new HttpTest().start();
		new HttppTest().start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
