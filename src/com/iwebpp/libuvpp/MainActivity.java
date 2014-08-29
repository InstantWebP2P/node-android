package com.iwebpp.libuvpp;

import com.iwebpp.libuvpp.R;
import com.iwebpp.libuvpp.tests.*;
import com.iwebpp.node.tests.EE2Test;

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
		*////new PipeHandleTest().start();
		///new ProcessHandleTest().start();

		///new FileEventHandleTest().start();
		
		// Node Unit tests
		new EE2Test().start();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
