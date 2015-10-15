package com.UI.LEDevice;

import java.util.ArrayList;
import java.util.List;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
	//constant 
	private final static int REQUEST_ENABLE_BT = 5;
	private final String mTAG = "MainActivity";
	private static final long SCAN_PERIOD = 5000; // Stops scanning after 8 seconds.
	
	//Inner classes
	BroadcastReceiver mBdReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            
			if(BLEUtility.ACTION_CONNSTATE_DISCONNECTED.equals(action))
            {
            	UIUtility.showProgressDlg(false, R.string.prgsDisconn);
            	String message = intent.getStringExtra(BLEUtility.ACTION_CONNSTATE_DISCONNECTED_KEY);
				Toast.makeText(MainActivity.this, "Disconnected, cause = " + message, Toast.LENGTH_SHORT).show();
            	updateUIForConn();
            	//setPullBKTask(false);
                return;
            }
			else if(BLEUtility.ACTION_CONNSTATE_CONNECTING.equals(action))
			{
				UIUtility.showProgressDlg(true, R.string.prgsConnting);
			}
			else if(BLEUtility.ACTION_CONNSTATE_CONNECTED.equals(action))
			{
				UIUtility.showProgressDlg(false, R.string.prgsConnted);
				IntegralSetting.setDeviceName(mPreDevice.getDeviceName());
				IntegralSetting.setDeviceMACAddr(mPreDevice.getAddress());
				updateUIForConn();
				Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        		if(mIntegral != null)
        			mIntegral.doThingsAfterConnted();
			}
			else if(BLEUtility.ACTION_GET_LEDEVICE.equals(action))
			{
				BLEDevice integralDevice = (BLEDevice) intent.getSerializableExtra(BLEUtility.ACTION_GET_LEDEVICE_KEY);
				if(integralDevice != null)
				{
					mLeDevices.add(integralDevice);
					MyLog.d(mTAG, "Get le device , "+mLeDevices.size()+"=>[" + integralDevice.getAddress()+"]");
				}
			}
			else if(BLEUtility.ACTION_UPDATE_ABOUT.equals(action)) {
				MyLog.d(mTAG, "Update About");
				String strVer = new String(intent.getStringExtra(BLEUtility.ACTION_UPDATE_ABOUT_VER_KEY));
				mDrawerItems.get(1).setTitle(strVer);
				String strVMode = new String(intent.getStringExtra(BLEUtility.ACTION_UPDATE_ABOUT_VMODE_KEY));
				mDrawerItems.get(2).setTitle(strVMode);
				String strLinkSt = new String(intent.getStringExtra(BLEUtility.ACTION_UPDATE_ABOUT_LINKST_KEY));
				mDrawerItems.get(3).setTitle(strLinkSt);
				mAdapter.notifyDataSetChanged();
			}
			//Blew are commands relaive 
			else if (BLEUtility.ACTION_SENCMD_BEGIN.equals(action)) 
            {
            	UIUtility.showProgressDlg(true, R.string.prgsSendingCmd);
            }
            else if(BLEUtility.ACTION_SENCMD_OK.equals(action)) 
            {
            	updateExpView();
            	UIUtility.showProgressDlg(false, R.string.prgsSedingCmdOK);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdOK, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_FAIL.equals(action)) 
            {
            	updateExpView();
            	UIUtility.showProgressDlg(false, R.string.prgsSedingCmdFail);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdFail, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_SWFORCE.equals(action)) 
            {
            	UIUtility.showProgressDlg(false, R.string.prgsSedingCmdFail);
            	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
    			builder.setTitle(R.string.AlertDlgMsgTitle);
    			builder.setMessage(R.string.AlertDlgMsg);
    			// Set up the buttons
    			builder.setPositiveButton(R.string.InputDlgOK, new DialogInterface.OnClickListener() { 
    			    @Override
    			    public void onClick(DialogInterface dialog, int which) {
    			    	dialog.cancel();
    			    }
    			});

    			builder.show();
            }
            else if (BLEUtility.ACTION_SENCMD_READ.equals(action)) 
            {
            	UIUtility.showProgressDlg(true, R.string.prgsSedingReadCmd);
            }
            else if (BLEUtility.ACTION_WRTREAD_WRT_BEG.equals(action)) 
            {
            	UIUtility.showProgressDlg(true, R.string.prgsSendingCmd);
            }
            else if(BLEUtility.ACTION_SENCMD_READ_CONTENT.equals(action))
            {
            	UIUtility.showProgressDlg(false, R.string.prgsSedingReadCmdOK);
            	String message = intent.getStringExtra(BLEUtility.ACTION_SENCMD_READ_CONTENT_KEY);
            	Toast.makeText(MainActivity.this, "read cmd ok, response = " + message, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_READ_FAIL.equals(action))
            {
            	UIUtility.showProgressDlg(false, R.string.prgsReadCmdFail);
            	Toast.makeText(MainActivity.this, R.string.prgsReadCmdFail, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_ITEM_READ_UPDATE.equals(action))
            {	
            	updateExpView();
            }
            else if(BLEUtility.ACTION_ITEM_READ_END.equals(action))
            {
            	UIUtility.showProgressDlg(false, R.string.prgsReadConfigEnd);
            	Toast.makeText(MainActivity.this, R.string.prgsReadConfigEnd, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_WRTREAD_WRT_UPDATE.equals(action)) {
            	updateExpView();
            	UIUtility.showProgressDlg(false, R.string.prgsSendingCmd);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdOK, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_WRTREAD_WRT_FAIL.equals(action)) {
            	updateExpView();
            	UIUtility.showProgressDlg(false, R.string.prgsSendingCmd);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdFail, Toast.LENGTH_SHORT).show();
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
				if (state == BluetoothAdapter.STATE_OFF) 
				{
					BLEUtility.getInstance().disconnect();
					//setPullBKTask(false);
				}
			}
		}
	};
		
	//data members
	private ListView mDrawerList;
	private DrawerAdapter mAdapter; 
	private List<DrawerItem> mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private RelativeLayout mDrawerRelativeLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private Menu mMenu = null;
	private CharSequence mTitle;
	private Handler mHandler;
	private boolean mShouldFinish = false;
	private Handler mScanPeriodHandler = new Handler();
	private BLEDevice mPreDevice = null;
	private ArrayList<BLEDevice> mLeDevices = new ArrayList<BLEDevice>();
	private ExpandaListActivity mIntegral = null;
	
	//Member functions
	public void updateUIForConn()
	{
		if(BLEUtility.getInstance().isConnect())
		{
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(R.string.menu_disconn);
		}
		else 
		{
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(R.string.menu_conn);
		}
	}
	
	private static IntentFilter makeActionsIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_BEGIN);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_OK);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_SWFORCE);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ_CONTENT);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_CONNECTING);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_CONNECTED);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_DISCONNECTED);
        intentFilter.addAction(BLEUtility.ACTION_GET_LEDEVICE);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BLEUtility.ACTION_ITEM_READ_UPDATE);
        intentFilter.addAction(BLEUtility.ACTION_ITEM_READ_END);
        intentFilter.addAction(BLEUtility.ACTION_WRTREAD_WRT_BEG);
        intentFilter.addAction(BLEUtility.ACTION_WRTREAD_WRT_UPDATE);
        intentFilter.addAction(BLEUtility.ACTION_WRTREAD_WRT_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_UPDATE_ABOUT);
        return intentFilter;
    }
	
	public void connectToIntegral(){
		if(BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled() == true)
		{
			if(IntegralSetting.getDeviceMACAddr().length() <= 0)
			{
				UIUtility.showProgressDlg(true, R.string.prgsScanDev);
				mLeDevices.clear();
				BLEUtility.getInstance().startScanLEDevices();
				mScanPeriodHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    BLEUtility.getInstance().stopScanLEDevices();
	                    UIUtility.showProgressDlg(false, R.string.prgsScanNoDev);
	                    if(mLeDevices.size() == 0)
	                    {
	                    	Toast.makeText(MainActivity.this, R.string.prgsScanNoDev, Toast.LENGTH_SHORT).show();
	                    }
	                    else if(mLeDevices.size() == 1)
	                    {
	                    	BLEDevice device = mLeDevices.get(0);
	                    	if(device != null)
	                    	{
	                    		mPreDevice = device;
	                    		BLEUtility.getInstance().connect(device.getAddress());
	                    	}
	                    }
	                    else //mLeDevices.size() > 1
	                    {
	                    	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
	                    			MainActivity.this);
	                        builderSingle.setIcon(R.drawable.ic_icon);
	                        builderSingle.setTitle(getResources().getString(R.string.dlgChooseIntegral));
	                        builderSingle.setNegativeButton(getResources().getString(R.string.dlgCancel),
	                                new DialogInterface.OnClickListener() {

	                                    @Override
	                                    public void onClick(DialogInterface dialog, int which) {
	                                        dialog.dismiss();
	                                    }
	                                });
	                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
	                        		MainActivity.this,
	                                android.R.layout.select_dialog_singlechoice);
	                        for(BLEDevice leDevice: mLeDevices)
	                        	arrayAdapter.add(leDevice.getDeviceName() + " [" + leDevice.getAddress() + "]");

	                        builderSingle.setAdapter(arrayAdapter,
	                                new DialogInterface.OnClickListener() {

	                                    @Override
	                                    public void onClick(DialogInterface dialog, int which) {
	                                        BLEDevice device = mLeDevices.get(which);
	                                        mPreDevice = device;
	                                        BLEUtility.getInstance().connect(device.getAddress());
	                                    }
	                                });
	                        builderSingle.show();
	                    }
	                }
	            }, SCAN_PERIOD);
			}
			else
			{
				mPreDevice = new BLEDevice(IntegralSetting.getDeviceName(), IntegralSetting.getDeviceMACAddr());
				BLEUtility.getInstance().connect(IntegralSetting.getDeviceMACAddr());
			}
		}	
	}
	
	public boolean needRequestBT() {
		if((BluetoothAdapter.getDefaultAdapter() == null || BluetoothAdapter.getDefaultAdapter().isEnabled() == false))
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
	        return true;
		}	
		return false;
	}
	
	private void updateExpView() {
		final Intent brd = new Intent(ExpandaListActivity.ACTION_UPDATELIST);
	    sendBroadcast(brd);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
       	case REQUEST_ENABLE_BT:
       	{
       		if(resultCode == Activity.RESULT_OK ) 
        	{
       			connectToIntegral();
        	}
       	}
       	break;
       }
        super.onActivityResult(requestCode, resultCode, data);
    }

	//Overrride functions
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ImageLoader imageLoader = ImageLoader.getInstance();
		if (!imageLoader.isInited()) {
			imageLoader.init(ImageLoaderConfiguration.createDefault(this));
		}

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerRelativeLayout = (RelativeLayout) findViewById(R.id.left_drawer);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
				R.string.drawer_open, R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				getSupportActionBar().setTitle(R.string.strAbout);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerToggle.setDrawerIndicatorEnabled(true);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mTitle = getTitle();
	
		mDrawerList = (ListView) findViewById(R.id.list_view_drawer);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		prepareNavigationDrawerItems();
		mAdapter = new DrawerAdapter(this, mDrawerItems, true);
		mDrawerList.setAdapter(mAdapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mHandler = new Handler();
		UIUtility.init(this);
		registerReceiver(mBdReceiver, makeActionsIntentFilter());	

		if (savedInstanceState == null) {
			selectItem(0, mDrawerItems.get(0).getTag());
			if(needRequestBT() == false) {
	    		connectToIntegral();	
			}
		}
	}
	
	@Override
	public void onResume() {
		updateUIForConn();
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(mBdReceiver);
		BLEUtility.getInstance().disconnect();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (!mShouldFinish && !mDrawerLayout.isDrawerOpen(mDrawerRelativeLayout)) {
			Toast.makeText(getApplicationContext(), R.string.confirm_exit,
					Toast.LENGTH_SHORT).show();
			mShouldFinish = true;
			mDrawerLayout.openDrawer(mDrawerRelativeLayout);
		} else if (!mShouldFinish && mDrawerLayout.isDrawerOpen(mDrawerRelativeLayout)) {
			mDrawerLayout.closeDrawer(mDrawerRelativeLayout);
		} else {
			super.onBackPressed();
		}
	}

	private void prepareNavigationDrawerItems() {
		mDrawerItems = new ArrayList<DrawerItem>();
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_Main,
				getResources().getString(R.string.drawer_title_Main),
				DrawerItem.DRAWER_ITEM_Main));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_DEVICEVER,
				getResources().getString(R.string.drawer_title_DEVICEVER),
				DrawerItem.DRAWER_ITEM_DEVICEVER));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_HDMIVideo,
				getResources().getString(R.string.drawer_title_HDMIVideo),
				DrawerItem.DRAWER_ITEM_HDMIVideo));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_HDMILink,
				getResources().getString(R.string.drawer_title_HDMILink),
				DrawerItem.DRAWER_ITEM_HDMILink));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_LINKHDF,
				getResources().getString(R.string.drawer_title_LINKHDF),
				DrawerItem.DRAWER_ITEM_LINKHDF));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		menu.findItem(R.id.menu_connect).setVisible(true);
		mMenu = menu;
		updateUIForConn();
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
        
        case android.R.id.home:
        	BLEUtility.getInstance().disconnect();
        	break;
        case R.id.menu_connect:
        {
        	String tle = (String) item.getTitle(); 
        	if(tle.compareTo(getResources().getString(R.string.menu_disconn)) == 0)
        		BLEUtility.getInstance().disconnect();
        	else if(tle.compareTo(getResources().getString(R.string.menu_conn)) == 0) {
        		selectItem(0, mDrawerItems.get(0).getTag());
        		if(needRequestBT() == false) {
            		connectToIntegral();
        		}
        	}
        }
        break;
        case R.id.menu_clearConn:
        {
        	IntegralSetting.setDeviceMACAddr("");
        	Toast.makeText(this, R.string.msgResetConn, Toast.LENGTH_SHORT).show();
        }
        break;
        default:	
		}	
		return super.onOptionsItemSelected(item);
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position, mDrawerItems.get(position).getTag());
		}
	}

	private Fragment selectItem(int position, int drawerTag) {
		Fragment fragment = getFragmentByDrawerTag(drawerTag);
		commitFragment(fragment);

		mDrawerList.setItemChecked(position, true);
		setTitle(mDrawerItems.get(position).getTitle());
		mDrawerLayout.closeDrawer(mDrawerRelativeLayout);
		return fragment;
	}

	private Fragment getFragmentByDrawerTag(int drawerTag) {
		Fragment fragment;
		if (drawerTag == DrawerItem.DRAWER_ITEM_Main) {
			mIntegral = ExpandaListActivity.newInstance();
			fragment = mIntegral;
		} else {
			fragment = new Fragment();
		}
		mShouldFinish = false;
		return fragment;
	}

	private class CommitFragmentRunnable implements Runnable {

		private Fragment fragment;

		public CommitFragmentRunnable(Fragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void run() {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment).commit();
		}
	}

	public void commitFragment(Fragment fragment) {
		// Using Handler class to avoid lagging while
		// committing fragment in same time as closing
		// navigation drawer
		mHandler.post(new CommitFragmentRunnable(fragment));
	}

	@Override
	public void setTitle(int titleId) {
		setTitle(getString(titleId));
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
}