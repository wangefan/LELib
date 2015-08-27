package com.UI.LEDevice;

import android.app.Activity;
import android.os.Bundle;


public class ConnectionActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("Low Energy Devices");
		 
		
		setContentView(R.layout.connectionactivity);
		 
	}
}
