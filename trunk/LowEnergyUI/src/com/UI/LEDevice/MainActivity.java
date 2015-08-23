package com.UI.LEDevice;

import java.util.ArrayList;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.IBLEUtilityListener;
import com.BLE.BLEUtility.MyLog;
import com.BLE.Buttons.BLEButton;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity 
{
	//constant 
	private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
	private final String mTAG = "MainActivity";
	private static final int REQUEST_ENABLE_BT = 1;

	//data member
	private BLEUtility mBLEUtility = null;
	private boolean mScanning = false;
	private Handler mScanPeriodHandler = new Handler();
	private LeDeviceListAdapter mLeDeviceListAdapter = null;
	private ProgressDialog mPDialog = null;
	private com.BLE.Buttons.BLEButton3State mBtnWrite = null;
	private Button mBtnDisconnect = null;
	
	//inner class
	// Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BLEDevice> mLeDevices;	
        private LayoutInflater mInflator;
        class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
        }
        
        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BLEDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BLEDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public void clear() {
            mLeDevices.clear();
        }
        
        public BLEDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_ledevice, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BLEDevice device = mLeDevices.get(i);
            final String deviceName = device.getDeviceName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("unknown device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
    
	//Listener for listen ConnectManagerService
	private IBLEUtilityListener mBLEUtilityListenerListener = new IBLEUtilityListener() 
	{
		@Override
	    public void onGetLEDevice(final BLEDevice device) {
	    	runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
	    }

		@Override
		public void onConnecting() {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	showProgressDlg(true, "connecting...");
                }
            });
		}

		@Override
		public void onConnectError(String message) {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	showProgressDlg(false, "connect error");
                	Toast.makeText(MainActivity.this, "Connect error", Toast.LENGTH_SHORT).show();
                }
            });
		}

		@Override
		public void onConnected() {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	showProgressDlg(false, "connected");
                	Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }
            });
		}

		@Override
		public void onDisconnected() {
			runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	showProgressDlg(false, "Disconnected");
                	Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                }
            });
		}

		@Override
		public void onRead(final String data) {
			runOnUiThread(new Runnable() {
                @Override
                public void run() 
                {
                	Toast.makeText(MainActivity.this, "read data:" + data, Toast.LENGTH_SHORT).show();
                }
            });
		}
	};
	
	BroadcastReceiver mBtnReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            
            if (BLEButton.ACTION_SENCMD_BEGIN.equals(action)) 
            {
            	showProgressDlg(true, "sending cmd");
            }
            else if(BLEButton.ACTION_SENCMD_OK.equals(action)) 
            {
            	showProgressDlg(false, "sending cmd OK");
            	Toast.makeText(MainActivity.this, "sending cmd OK", Toast.LENGTH_SHORT).show();
            }
            else if(BLEButton.ACTION_SENCMD_FAIL.equals(action)) 
            {
            	showProgressDlg(false, "sending cmd fail");
            	Toast.makeText(MainActivity.this, "sending cmd fail", Toast.LENGTH_SHORT).show();
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
	
	//member functions
	private void showProgressDlg(boolean bShow, String message)
    {
    	if(bShow)
    	{
    		if(mPDialog != null)
    		{
    			mPDialog.setTitle("Process...");
    			mPDialog.setMessage(message);
    			mPDialog.show();
    		}
    		else
    		{
    			mPDialog = ProgressDialog.show(this, "Process...", message);
    		}
    	}
    	else
    	{
    		if(mPDialog != null)
    			mPDialog.dismiss();
    	}
    }
	
    private void mDoBTIntentForResult()
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
	
    private void scanLeDevice(final boolean enable) 
    {
    	try {
    		if(mBLEUtility != null) {
	    		if (enable) {
		            // Stops scanning after a pre-defined scan period.
		        	mScanPeriodHandler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                    mScanning = false;
		                    mBLEUtility.stopScanLEDevices();
		                    invalidateOptionsMenu();
		                }
		            }, SCAN_PERIOD);
		     
		            //will start scan a period times ad receive devices under "onGetLEDevice".
		        	mBLEUtility.startScanLEDevices();
		        	mScanning = true;  
		        } else {
		    		mBLEUtility.stopScanLEDevices();
		    		mScanning = false;
		        }
    		}
        }
        catch (Exception e) {
        	Toast.makeText(this, "scanLeDevice(" + enable +") fail, exception " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
        invalidateOptionsMenu();	//trigger  onCreateOptionsMenu
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("Low Energy Devices");
		 
		registerReceiver(mBtnReceiver, makeServiceActionsIntentFilter());	
		 
		mBLEUtility = BLEUtility.getInstance(this);
		mBLEUtility.setListener(mBLEUtilityListenerListener);
		setContentView(R.layout.mainactivity);
		 
		//=============init conrols==========
		mBtnWrite = (com.BLE.Buttons.BLEButton3State) findViewById(R.id.btnWrite);
	 	mBtnDisconnect = (Button) findViewById(R.id.btnDis);
	 	 if(mBtnDisconnect != null)
	     {
	 		mBtnDisconnect.setOnClickListener(new OnClickListener() 
	 		{
				@Override
				public void onClick(View v) 
				{
					mBLEUtility.disconnect();
				}
	    	 });
	     }
	 	 
	 	mLeDeviceListAdapter = new LeDeviceListAdapter();
	    setListAdapter(mLeDeviceListAdapter);
	 	//=============init conrols end==========
	     scanLeDevice(true);	
	}
	
	private static IntentFilter makeServiceActionsIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEButton.ACTION_SENCMD_BEGIN);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_OK);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_FAIL);
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
        scanLeDevice(false);
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
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BLEDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null || mBLEUtility == null) return;
        scanLeDevice(false);
        mBLEUtility.connect(device.getAddress());
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
        case R.id.menu_scan:
            mLeDeviceListAdapter.clear();
            scanLeDevice(true);
            break;
        case R.id.menu_stop:
            scanLeDevice(false);
            break;
        case android.R.id.home:
            onBackPressed();
            return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
