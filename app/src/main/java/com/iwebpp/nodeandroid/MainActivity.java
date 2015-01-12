package com.iwebpp.nodeandroid;

import com.iwebpp.crypto.tests.NaclCertTest;
import com.iwebpp.crypto.tests.TweetNaclFastTest;

import com.iwebpp.middleware.test.ConnectTest;
import com.iwebpp.node.api.tests.SimpleApiTest;
import com.iwebpp.node.js.rhino.Host;
import com.iwebpp.node.js.tests.RhinoTest;
import com.iwebpp.node.tests.DnsTest;
import com.iwebpp.node.tests.EE2Test;
import com.iwebpp.node.tests.EventHandlerTest;
import com.iwebpp.node.tests.HttpParserTest;
import com.iwebpp.node.tests.HttpTest;
import com.iwebpp.node.tests.HttppTest;
import com.iwebpp.node.tests.StreamTest;
import com.iwebpp.node.tests.TcpTest;
import com.iwebpp.node.tests.UdtTest;
import com.iwebpp.node.tests.UrlTest;
import com.iwebpp.wspp.tests.SecureWebSocketServerTest;
import com.iwebpp.wspp.tests.WebSocketServerTest;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

public class MainActivity extends ActionBarActivity {

    private EditText editText;

    final private static String TAG = "MainActivity";
    private MenuItem runScriptMenuItem;

    @Override
	protected void onCreate(Bundle savedInstanceState) {

        // prevent getActionBar() == null
//        if (android.os.Build.VERSION.SDK_INT > 11) {
//            requestWindowFeature(Window.FEATURE_ACTION_BAR);
//            requestWindowFeature(Window.FEATURE_NO_TITLE);
//        }

		super.onCreate(savedInstanceState);


		setContentView(R.layout.activity_main);

//        if (android.os.Build.VERSION.SDK_INT <= 11) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }else
//        {
//            getActionBar().setDisplayHomeAsUpEnabled(true); // still nullptr, dammit. // FIXME
//        }
		
        String js = getIntent().getStringExtra("js");

        editText = (EditText) findViewById(R.id.textView);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (runScriptMenuItem != null)
                {
                    runScriptMenuItem.setEnabled(!TextUtils.isEmpty(editText.getText().toString()));
                }
            }
        });

        if (!TextUtils.isEmpty(js)) {
            // TODO highlight javascript code
            editText.setText(js);
        }

	}

    private void runHardCodedTests()
    {
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
        new EE2Test().start();
        new StreamTest().start();
        new TcpTest().start();
        new UdtTest().start();

        new HttpParserTest().start();
        new DnsTest().start();
        new HttpTest().start();
        new HttppTest().start();
        new UrlTest().start();
        ///new WebSocketTest().start();
        new WebSocketServerTest().start();

        // Connect middleware tests
        new ConnectTest().start();

        // JS engine tests
        new RhinoTest().start();

        // NodeApi tests
        new EventHandlerTest().start();
        new SimpleApiTest().start();

        // Crypto tests
        ///new TweetNaclTest().start();
        new TweetNaclFastTest().start();
        new NaclCertTest().start();

        // SecureWebSocket tests
        new SecureWebSocketServerTest().start();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

        runScriptMenuItem = menu.findItem(R.id.run_script);
        runScriptMenuItem.setEnabled(!TextUtils.isEmpty(editText.getText().toString()));

		return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.run_script:
                if (editText != null)
                {
                    runJavascript(editText.getText().toString());
                }
            default: return super.onOptionsItemSelected(item);
        }

    }

    private void runJavascript(final String js) {
        new Thread(new Runnable() {
            public void run() {
                Log.d(TAG, "start test");

                try {
                    Host host = new Host() {
                        @Override
                        public String content() {
                            return js;
                        }
                    };

                    Log.d(TAG, "exit test");
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
