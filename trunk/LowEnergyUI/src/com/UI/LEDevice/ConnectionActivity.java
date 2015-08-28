package com.UI.LEDevice;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.IBLEUtilityListener;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Toast;


public class ConnectionActivity extends BTSettingActivity {
	//Constant
	public static final String KEY_GET_BT_DEVICE = "KEY_GET_BT_DEVICE";
	
	private static final int REQUEST_GET_LE_DEVICE = 1;

	//inner class and listener
	private IBLEUtilityListener mBLEUtilityListenerListener = new IBLEUtilityListener() 
	{
		@Override
	    public void onGetLEDevice(final BLEDevice device) {
	    	
	    }

		@Override
		public void onConnecting() {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	UIUtility.showProgressDlg(ConnectionActivity.this, true, "connecting...");
                }
            });
		}

		@Override
		public void onConnectError(String message) {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	UIUtility.showProgressDlg(ConnectionActivity.this, false, "connect error");
                	Toast.makeText(ConnectionActivity.this, "Connect error", Toast.LENGTH_SHORT).show();
                }
            });
		}

		@Override
		public void onConnected() {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	UIUtility.showProgressDlg(ConnectionActivity.this, false, "connected");
                	Toast.makeText(ConnectionActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }
            });
		}

		@Override
		public void onDisconnected() {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	UIUtility.showProgressDlg(ConnectionActivity.this, false, "Disconnected");
                	Toast.makeText(ConnectionActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                }
            });
		}

		@Override
		public void onRead(final String data) {
			
		}
	};
		
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
				
		if(mBtnScan != null)
		{
			mBtnScan.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent i = new Intent(ConnectionActivity.this, ScanLEDeviceActivity.class);
		            startActivityForResult(i, REQUEST_GET_LE_DEVICE);
				}
			});
		}
		
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
	
	   @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	       switch (requestCode) {
	        case REQUEST_GET_LE_DEVICE :
	        {
	        	if(resultCode == Activity.RESULT_OK ) 
	        	{
	        		BLEDevice device = (BLEDevice) data.getSerializableExtra(KEY_GET_BT_DEVICE);
	        		BLEUtility.getInstance(this).connect(device.getAddress());
	        	}
	        }
	        break;
	        }
	       super.onActivityResult(requestCode, resultCode, data);
	   }
}
