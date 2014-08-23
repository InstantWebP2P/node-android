package com.iwebpp.libuvpp;

import com.iwebpp.libuvpp.R;
import com.iwebpp.libuvpp.libUVPP;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		libUVPP lib = new libUVPP();
		///lib.ares_library_init(0);
		lib.addSum(6, 8);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
