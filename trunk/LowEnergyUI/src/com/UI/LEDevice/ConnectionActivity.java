package com.UI.LEDevice;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageButton;


public class ConnectionActivity extends BTSettingActivity {
	//Data members.
	ImageButton   mBtnConn;
	ImageButton   mBtnScan;
	CheckBox      mAutoConn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(getResources().getString(R.string.strConnActTitle));
		setContentView(R.layout.connectionactivity);
		
		//init UI controls
		mBtnConn = (ImageButton) findViewById(R.id.idConn);
		mBtnScan = (ImageButton) findViewById(R.id.idSetConn);
		mAutoConn = (CheckBox) findViewById(R.id.idAutoConn);
				
		//initialize preference value
		IntegralSetting.initSharedPreferences(this);
	}

	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		IntegralSetting.destroySharedPreferences();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( 0 >= IntegralSetting.getDeviceMACAddr().length() )
		{
			mBtnConn.setEnabled(false);
			mAutoConn.setEnabled(false);
		}
		else {
			mBtnConn.setEnabled(true);
			mAutoConn.setEnabled(true);
		}
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	@Override
	protected void mDoThingsAtrEnableBTActy() {
		// TODO Auto-generated method stub
		
	}
}
