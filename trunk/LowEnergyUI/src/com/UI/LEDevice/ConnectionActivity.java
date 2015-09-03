package com.UI.LEDevice;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


public class ConnectionActivity extends BTSettingActivity {
	//Constant
	public static final String KEY_GET_BT_DEVICE = "KEY_GET_BT_DEVICE";
	
	private static final int REQUEST_GET_LE_DEVICE = 1;

	//inner class and listener
	private BroadcastReceiver mReceiver = new BroadcastReceiver() 
	{
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(action.equals(BLEUtility.ACTION_CONNSTATE_CONNECTING))
			{
				UIUtility.showProgressDlg(ConnectionActivity.this, true, "connecting...");
			}
			else if(action.equals(BLEUtility.ACTION_CONNSTATE_DISCONNECTED))
			{
				UIUtility.showProgressDlg(ConnectionActivity.this, false, "disconnect");
				String message = intent.getStringExtra(BLEUtility.ACTION_CONNSTATE_DISCONNECTED_KEY);
				Toast.makeText(ConnectionActivity.this, "disconnect, cause = " + message, Toast.LENGTH_SHORT).show();
			}
			if(action.equals(BLEUtility.ACTION_CONNSTATE_CONNECTED))
			{
				UIUtility.showProgressDlg(ConnectionActivity.this, false, "connected");
            	Toast.makeText(ConnectionActivity.this, "Connected", Toast.LENGTH_SHORT).show();
            	IntegralSetting.setDeviceMACAddr(mLastConnDevice.getAddress());
            	IntegralSetting.setDeviceName(mLastConnDevice.getDeviceName());
            	Intent i = new Intent(ConnectionActivity.this, MainActivity.class);
	            startActivity(i);
			}
		}
	};
		
	//Data members.
	RingButton    mRingButton;

	TextView      mTVConnectTo;

	CheckBox      mAutoConn;
	BLEDevice 	  mLastConnDevice;
	
	//functions
	private static IntentFilter makeIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_CONNECTING);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_CONNECTED);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_DISCONNECTED);
        return intentFilter;
    }
	
	private void connectTo(String name, String address) {
		mLastConnDevice = new BLEDevice(name, address);
		BLEUtility.getInstance().connect(mLastConnDevice.getAddress());	
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle(getResources().getString(R.string.strConnActTitle));
		setContentView(R.layout.connectionactivity);
		
		//init UI controls
		mRingButton = (RingButton)findViewById(R.id.ringButton);
		mTVConnectTo = (TextView)  findViewById(R.id.tvConnectTo);
		mAutoConn = (CheckBox) findViewById(R.id.idAutoConn);
		mLastConnDevice = null;
				
		if(mRingButton != null)
		{
			mRingButton.setOnClickListener(new RingButton.OnClickListener()
			{
	            @Override
	            public void clickUp() {
	            	ConnectionActivity.this.connectTo(IntegralSetting.getDeviceName(), IntegralSetting.getDeviceMACAddr());
	            }

	            @Override
	            public void clickDown() {
	            	Intent i = new Intent(ConnectionActivity.this, ScanLEDeviceActivity.class);
		            startActivityForResult(i, REQUEST_GET_LE_DEVICE);
	            }
	       });
		}
		
		//initialize preference value
		IntegralSetting.initSharedPreferences(this);
		
		registerReceiver(mReceiver, makeIntentFilter());	
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		IntegralSetting.destroySharedPreferences();
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if( 0 >= IntegralSetting.getDeviceMACAddr().length() )
		{
		
			mAutoConn.setEnabled(false);
			mTVConnectTo.setVisibility(View.INVISIBLE);
		}
		else {
			
			mAutoConn.setEnabled(true);
			String text = "Connect To: " + IntegralSetting.getDeviceName();
			mTVConnectTo.setText(text);
			mTVConnectTo.setVisibility(View.VISIBLE);
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
	
	   @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	       switch (requestCode) {
	        case REQUEST_GET_LE_DEVICE :
	        {
	        	if(resultCode == Activity.RESULT_OK ) 
	        	{
	        		final BLEDevice device = (BLEDevice) data.getSerializableExtra(KEY_GET_BT_DEVICE);
	        		connectTo(device.getDeviceName(), device.getAddress());
	        	}
	        }
	        break;
	        }
	       super.onActivityResult(requestCode, resultCode, data);
	   }
}
