package com.UI.LEDevice;

import java.util.ArrayList;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.Buttons.BLEButton;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity 
{
	//constant 
	private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
	private final String mTAG = "MainActivity";
	private static final int REQUEST_ENABLE_BT = 1;

	//data member
	private boolean mScanning = false;
	private Handler mScanPeriodHandler = new Handler();
	private Button mBtnDisconnect = null;
	
	BroadcastReceiver mBtnReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            
            if (BLEButton.ACTION_SENCMD_BEGIN.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, "sending cmd");
            }
            else if (BLEButton.ACTION_SENCMD_READ.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, "sending read cmd");
            }
            else if(BLEButton.ACTION_SENCMD_OK.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "sending cmd OK");
            	Toast.makeText(MainActivity.this, "sending cmd OK", Toast.LENGTH_SHORT).show();
            }
            else if(BLEButton.ACTION_SENCMD_FAIL.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "sending cmd fail");
            	Toast.makeText(MainActivity.this, "sending cmd fail", Toast.LENGTH_SHORT).show();
            }
            else if(BLEButton.ACTION_SENCMD_READ_FAIL.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "read cmd fail");
            	Toast.makeText(MainActivity.this, "read cmd fail", Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_CONNSTATE_DISCONNECTED.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "disconnected");
            	String message = intent.getStringExtra(BLEUtility.ACTION_CONNSTATE_DISCONNECTED_KEY);
            	Toast.makeText(MainActivity.this, "disconnected, cause = " + message, Toast.LENGTH_SHORT).show();
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) 
                {
                    
                } 
                else if (state == BluetoothAdapter.STATE_OFF) 
                {
                	if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                		mDoBTIntentForResult();
                        return;
                    }
                }
            }
		}
	};
	
    private void mDoBTIntentForResult()
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("Low Energy Devices");
		 
		registerReceiver(mBtnReceiver, makeServiceActionsIntentFilter());	
		setContentView(R.layout.mainactivity);
		 
		//=============init conrols==========
	 	mBtnDisconnect = (Button) findViewById(R.id.btnDis);
	 	if(mBtnDisconnect != null)
	    {
	 		mBtnDisconnect.setOnClickListener(new OnClickListener() 
	 		{
	 			@Override
				public void onClick(View v) 
				{
					BLEUtility.getInstance().disconnect();
				}
	    	});
	     }
	}
	
	private static IntentFilter makeServiceActionsIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEButton.ACTION_SENCMD_BEGIN);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_OK);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_FAIL);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_READ);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_READ_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }
	
	@Override
    protected void onResume() {
		super.onResume();
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter == null)
		{
            finish();
            return;
		}
		
		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
        	mDoBTIntentForResult();
            return;
        }	
	}
	
	@Override
    protected void onPause() {
        super.onPause();
        mScanPeriodHandler.removeCallbacksAndMessages(null);
    }
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mBtnReceiver);
		super.onDestroy();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_CANCELED)
            {
            	finish();
                return;
            }
            else;	//allow
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		 if (!mScanning) {
	            menu.findItem(R.id.menu_stop).setVisible(false);
	            menu.findItem(R.id.menu_scan).setVisible(true);
	            menu.findItem(R.id.menu_refresh).setActionView(null);
	        } else {
	            menu.findItem(R.id.menu_stop).setVisible(true);
	            menu.findItem(R.id.menu_scan).setVisible(false);
	            menu.findItem(R.id.menu_refresh).setActionView(
	                    R.layout.actionbar_indeterminate_progress);
	        }
		return true;
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
        
        case android.R.id.home:
            onBackPressed();
            return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
