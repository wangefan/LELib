package com.UI.LEDevice;

import java.util.ArrayList;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.BLEUtilityException;
import com.BLE.BLEUtility.IBLEUtilityListener;
import com.BLE.BLEUtility.MyLog;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity 
{
	//constant 
	private static final long SCAN_PERIOD = 10000; // Stops scanning after 10 seconds.
	private final String mTAG = "MainActivity";
	
	//data member
	private BLEUtility mBLEUtility = null;
	private boolean mScanning = false;
	private Handler mScanPeriodHandler = new Handler();
	private LeDeviceListAdapter mLeDeviceListAdapter = null;
	private ProgressDialog mPDialog = null;
	private TextView mtvRead = null;
	private Button mBtnWrite = null;
	private Button mBtnDisconnect = null;
	private Button  mlstCommands = null;
	private String  mstrCommand = "";
	
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
                	mtvRead.setText(data);
                	Toast.makeText(MainActivity.this, "read data:" + data, Toast.LENGTH_SHORT).show();
                }
            });
		}
	};
	
	//member functions
	private void showProgressDlg(boolean bShow, String message)
    {
    	if(bShow)
    	{
    		if(mPDialog != null)
    			mPDialog.show();
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
		 
		 mBLEUtility = new BLEUtility(this);
		 mBLEUtility.setListener(mBLEUtilityListenerListener);
		 setContentView(R.layout.mainactivity);
	     
	     //=============init conrols==========
	     mBtnWrite = (Button) findViewById(R.id.btnWrite);
	 	 if(mBtnWrite != null)
	     {
	 		mBtnWrite.setOnClickListener(new OnClickListener() 
	 		{
				@Override
				public void onClick(View v) 
				{
					mtvRead.setText("no response...");
					try {
						if(mstrCommand != null)
							mBLEUtility.write(mstrCommand);
					} catch (BLEUtilityException e) {
						Toast.makeText(MainActivity.this, "write error, cause = [" + e.getMessage() +"]", Toast.LENGTH_SHORT).show();
					}
				}
	    	 });
	     }
	 	 
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
	 	 
	     mlstCommands = (Button) findViewById(R.id.commands);
	     if(mlstCommands != null)
	     {
	    	 mlstCommands.setOnClickListener(new OnClickListener() 
		 	 {
	    		 @Override
	    		 public void onClick(View v) 
	    		 {
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setTitle("Choose commands");
					final String [] cmdArr = MainActivity.this.getResources().getStringArray(R.array.commands_entries);    
					builder.setItems(R.array.commands_entries, new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int item) {
					    	mstrCommand = cmdArr[item];
					    	mstrCommand += "\r";
					        mlstCommands.setText(mstrCommand);
					        MyLog.d(mTAG, "mstrCommand = ["+ mstrCommand +"]");
					    }
					});
					AlertDialog alert = builder.create();
					alert.show();
	    		 }
		 	 });
	     }
	        
	 	mtvRead = (TextView) findViewById(R.id.tvRead);
	 	mLeDeviceListAdapter = new LeDeviceListAdapter();
	    setListAdapter(mLeDeviceListAdapter);
	 	//=============init conrols end==========
	     scanLeDevice(true);	
	}
	
	@Override
    protected void onPause() {
        super.onPause();
        mScanPeriodHandler.removeCallbacksAndMessages(null);
        scanLeDevice(false);
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
