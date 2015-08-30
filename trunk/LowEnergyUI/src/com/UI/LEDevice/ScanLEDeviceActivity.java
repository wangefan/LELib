package com.UI.LEDevice;

import java.util.ArrayList;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ScanLEDeviceActivity extends ListActivity {
	//constant 
	private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
	private final String mTAG = "ScanLEDeviceActivity";
	private static final int REQUEST_ENABLE_BT = 1;

	//data member
	private boolean mScanning = false;
	private Handler mScanPeriodHandler = new Handler();
	private LeDeviceListAdapter mLeDeviceListAdapter = null;
	
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
            mInflator = ScanLEDeviceActivity.this.getLayoutInflater();
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
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) 
			{
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
			else if(action.equals(BLEUtility.ACTION_GET_LEDEVICE))
			{
				BLEDevice cBTDeivce = (BLEDevice) intent.getSerializableExtra(BLEUtility.ACTION_GET_LEDEVICE_KEY);
				mLeDeviceListAdapter.addDevice(cBTDeivce);
                mLeDeviceListAdapter.notifyDataSetChanged();
			}
		}
	};
	
	private void mDoBTIntentForResult()
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
	
    private void scanLeDevice(final boolean enable) 
    {
    	try {
    		if(BLEUtility.getInstance() != null) {
	    		if (enable) {
		            // Stops scanning after a pre-defined scan period.
		        	mScanPeriodHandler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                    mScanning = false;
		                    BLEUtility.getInstance().stopScanLEDevices();
		                    invalidateOptionsMenu();
		                }
		            }, SCAN_PERIOD);
		     
		            //will start scan a period times ad receive devices under "onGetLEDevice".
		        	BLEUtility.getInstance().startScanLEDevices();
		        	mScanning = true;  
		        } else {
		        	BLEUtility.getInstance().stopScanLEDevices();
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
		getActionBar().setTitle(getResources().getString(R.string.strSeatchIntegral));
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		registerReceiver(mReceiver, makeServiceActionsIntentFilter());	
		setContentView(R.layout.scanledeviceactivity);
	 	 
	 	mLeDeviceListAdapter = new LeDeviceListAdapter();
	    setListAdapter(mLeDeviceListAdapter);
	 	//=============init conrols end==========
	     scanLeDevice(true);	
	}
	
	private static IntentFilter makeServiceActionsIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BLEUtility.ACTION_GET_LEDEVICE);
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
		unregisterReceiver(mReceiver);
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
        if (device == null || BLEUtility.getInstance() == null) return;
        scanLeDevice(false);
        Intent resultInt = new Intent();
        resultInt.putExtra(ConnectionActivity.KEY_GET_BT_DEVICE, device);
        setResult(RESULT_OK, resultInt);
        onBackPressed();
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
