package com.UI.LEDevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.UI.font.FontelloTextView;
import com.UI.font.RobotoTextView;
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
import android.net.Uri;
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
				
				//Update MAC address
				String strTleMain = String.format("%s [%s]", getResources().getString(R.string.drawer_title_Main), 
						IntegralSetting.getDeviceMACAddr());  
				mDrawerItems.get(0).setTitle(strTleMain);
				
				updateUIForConn();
				Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        		if(mIntegral != null)
        			mIntegral.doThingsAfterConnted();
        		mAdapter.notifyDataSetChanged();
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
				
				//Update v mode
				String strVMode = new String(intent.getStringExtra(BLEUtility.ACTION_UPDATE_ABOUT_VMODE_KEY));
				String strVmodeFull = getResources().getString(R.string.drawer_title_HDMIVideo_none);
				if(strVMode.length() > 0) {
					Character x = strVMode.charAt(7);
					Character y = strVMode.charAt(9);
					Character z = strVMode.charAt(11);
					String vMode1 = (y.compareTo('0') == 0) ? getResources().getString(R.string.aboutVmodeDVI) : getResources().getString(R.string.aboutVmodeHDMI);
					int nX = Character.getNumericValue(x);
					String vMode2 = mVmode2Coll.get(nX);
					String vMode3 = (z.compareTo('0') == 0) ? getResources().getString(R.string.aboutVmode3G) : getResources().getString(R.string.aboutVmode6G);
					strVmodeFull = String.format("%s %s %s", vMode1, vMode2, vMode3);
				}
				mDrawerItems.get(1).setTitle(strVmodeFull);
				
				//update link status
				String strLinkSt = new String(intent.getStringExtra(BLEUtility.ACTION_UPDATE_ABOUT_LINKST_KEY));
				String strLinkStFull = getResources().getString(R.string.drawer_title_HDMILink_none);
				if(strLinkSt.length() > 0) {
					Character rxTop = strLinkSt.charAt(7);
					Character rxBot = strLinkSt.charAt(9);
					Character txTop = strLinkSt.charAt(11);
					Character txBot = strLinkSt.charAt(13);
					String strRxTop = (rxTop == '0') ? getResources().getString(R.string.drawer_HDMILink_Nac) :
						getResources().getString(R.string.drawer_HDMILink_Act);
					String strRxBot = (rxBot == '0') ? getResources().getString(R.string.drawer_HDMILink_Nac) :
						getResources().getString(R.string.drawer_HDMILink_Act);
					String strTxTop = (txTop == '0') ? getResources().getString(R.string.drawer_HDMILink_Nac) :
						getResources().getString(R.string.drawer_HDMILink_Act);
					String strTxBot = (txBot == '0') ? getResources().getString(R.string.drawer_HDMILink_Nac) :
						getResources().getString(R.string.drawer_HDMILink_Act);
					strLinkStFull = String.format(getResources().getString(R.string.drawer_title_HDMILink), 
							strRxTop, strRxBot, strTxTop, strTxBot);
				}
				mDrawerItems.get(2).setTitle(strLinkStFull);
				String strVer = new String(intent.getStringExtra(BLEUtility.ACTION_UPDATE_ABOUT_VER_KEY));
				String strVerFull = getResources().getString(R.string.drawer_title_DEVICEVER_none);
				if(strVer.length() > 0)
				{
					final int nXXStart = 4, nYYStart = 7, nZZStart = 10, nWWStart = 13, nCount = 2;  
					String xx = strVer.substring(nXXStart, nXXStart + nCount);
					String yy = strVer.substring(nYYStart, nYYStart + nCount);
					String zz = strVer.substring(nZZStart, nZZStart + nCount);
					String ww = strVer.substring(nWWStart, nWWStart + nCount);
					strVerFull = String.format("FW ver. %s, %s, %s, %s", xx, yy, zz, ww);
				}
				
				mDrawerItems.get(3).setTitle(strVerFull);
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
    private static final Map<Integer , String> mVmode2Coll;
    static
    {
    	mVmode2Coll = new HashMap<Integer , String>();
    	mVmode2Coll.put(0, "");
    	mVmode2Coll.put(1, "640x480p60");
    	mVmode2Coll.put(2, "720x480p60");
    	mVmode2Coll.put(3, "720x480p60w");
    	mVmode2Coll.put(4, "1280x720p60");
    	mVmode2Coll.put(5, "1920x1080i60");
    	mVmode2Coll.put(6, "720x480i60");
    	mVmode2Coll.put(7, "720x480i60w");
    	mVmode2Coll.put(8, "720x240p60n");
    	mVmode2Coll.put(9, "720x240p60w");
    	mVmode2Coll.put(10, "2880x480i60n");
    	mVmode2Coll.put(11, "2880x480i60w");
    	mVmode2Coll.put(12, "2880x240p60n");
    	mVmode2Coll.put(13, "2880x240p60w");
    	mVmode2Coll.put(14, "1440x480p60n");
    	mVmode2Coll.put(15, "1440x480p60w");
    	mVmode2Coll.put(16, "1920x1080p60");
    	mVmode2Coll.put(17, "720x576p50");
    	mVmode2Coll.put(18, "720x576p50w");
    	mVmode2Coll.put(19, "1280x720p50");
    	mVmode2Coll.put(20, "1920x1080i50");
    	mVmode2Coll.put(21, "720x576i50");
    	mVmode2Coll.put(22, "720x576i50w");
    	mVmode2Coll.put(23, "720x288p50n");
    	mVmode2Coll.put(24, "720x288p50w");
    	mVmode2Coll.put(25, "2880x576i50n");
    	mVmode2Coll.put(26, "2880x576i50w");
    	mVmode2Coll.put(27, "2880x288p50n");
    	mVmode2Coll.put(28, "2880x288p50w");
    	mVmode2Coll.put(29, "1440x576p50n");
    	mVmode2Coll.put(30, "1440x576p50w");
    	mVmode2Coll.put(31, "1920x1080p50");
    	mVmode2Coll.put(32, "1920x1080p24");
    	mVmode2Coll.put(33, "1920x1080p25");
    	mVmode2Coll.put(34, "1920x1080p30");
    	mVmode2Coll.put(35, "2880x480p60n");
    	mVmode2Coll.put(36, "2880x480p60w");
    	mVmode2Coll.put(37, "2880x576p50n");
    	mVmode2Coll.put(38, "2880x576p50w");
    	mVmode2Coll.put(39, "1920x1080i50");
    	mVmode2Coll.put(40, "1920x1080i100");
    	mVmode2Coll.put(41, "1280x720p100");
    	mVmode2Coll.put(42, "720x576p100n");
    	mVmode2Coll.put(43, "720x576p100w");
    	mVmode2Coll.put(44, "720x576i100n");
    	mVmode2Coll.put(45, "720x576i100w");
    	mVmode2Coll.put(46, "1920x1080i120");
    	mVmode2Coll.put(47, "1280x720p120");
    	mVmode2Coll.put(48, "720x480p120n");
    	mVmode2Coll.put(49, "720x480p120w");
    	mVmode2Coll.put(50, "720x480i120n");
    	mVmode2Coll.put(51, "720x480i120w");
    	mVmode2Coll.put(52, "720x576p200n");
    	mVmode2Coll.put(53, "720x576p200w");
    	mVmode2Coll.put(54, "720x576i200n");
    	mVmode2Coll.put(55, "720x576i200w");
    	mVmode2Coll.put(56, "720x480p240n");
    	mVmode2Coll.put(57, "720x480p240w");
    	mVmode2Coll.put(58, "720x480i240n");
    	mVmode2Coll.put(59, "720x480i240w");
    	mVmode2Coll.put(60, "1280x720p24");
    	mVmode2Coll.put(61, "1280x720p25");
    	mVmode2Coll.put(62, "1280x720p30");
    	mVmode2Coll.put(63, "1920x1080p120");
    	mVmode2Coll.put(64, "1920x1080p100");
    	mVmode2Coll.put(65, "1920x1080p100");
    	mVmode2Coll.put(66, "1920x1080p100");
    	mVmode2Coll.put(67, "1920x1080p100");
    	mVmode2Coll.put(68, "1920x1080p100");
    	mVmode2Coll.put(69, "1920x1080p100");
    	mVmode2Coll.put(70, "1920x1080p100");
    	mVmode2Coll.put(71, "1920x1080p100");
    	mVmode2Coll.put(72, "1920x1080p100");
    	mVmode2Coll.put(73, "1920x1080p100");
    	mVmode2Coll.put(74, "1920x1080p100");
    	mVmode2Coll.put(75, "1920x1080p100");
    	mVmode2Coll.put(76, "1920x1080p100");
    	mVmode2Coll.put(77, "1920x1080p100");
    	mVmode2Coll.put(78, "1920x1080p100");
    	mVmode2Coll.put(79, "1920x1080p100");
    	mVmode2Coll.put(80, "1920x1080p100");
    	mVmode2Coll.put(81, "1680x720p30");
    	mVmode2Coll.put(82, "1680x720p50");
    	mVmode2Coll.put(83, "1680x720p60");
    	mVmode2Coll.put(84, "1680x720p100");
    	mVmode2Coll.put(85, "1680x720p120");
    	mVmode2Coll.put(86, "2560x1080p24");
    	mVmode2Coll.put(87, "2650x1080p25");
    	mVmode2Coll.put(88, "2650x1080p30");
    	mVmode2Coll.put(89, "2560x1080p50");
    	mVmode2Coll.put(90, "2650x1080p60");
    	mVmode2Coll.put(91, "1080p100");
    	mVmode2Coll.put(92, "1080p120");
    	mVmode2Coll.put(93, "3840x2160p24");
    	mVmode2Coll.put(94, "3840x2160p25");
    	mVmode2Coll.put(95, "3840x2160p30");
    	mVmode2Coll.put(96, "3840x2160p50");
    	mVmode2Coll.put(97, "3840x2160p60");
    	mVmode2Coll.put(98, "4096x2160p24");
    	mVmode2Coll.put(99, "4096x2160p25");
    	mVmode2Coll.put(100, "4096x2160p30");
    	mVmode2Coll.put(101, "4096x2160p50");
    	mVmode2Coll.put(102, "4096x2160p60");
    	mVmode2Coll.put(103, "3840x2160p24");
    	mVmode2Coll.put(104, "3840x2160p25");
    	mVmode2Coll.put(105, "3840x2160p30");
    	mVmode2Coll.put(106, "3840x2160p50");
    }
	
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
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		FontelloTextView linkHdIcon = (FontelloTextView) findViewById(R.id.linkhd_icon);
		if(linkHdIcon != null)
			linkHdIcon.setText(getResources().getString(R.string.drawer_icon_LINKHDF));
		RobotoTextView linkHdTitle = (RobotoTextView)findViewById(R.id.linkhd_title);
		if(linkHdTitle != null)
			linkHdTitle.setText(getResources().getString(R.string.drawer_title_LINKHDF));

		MaterialRippleLayout linkhdLayout = (MaterialRippleLayout)findViewById(R.id.linkhd);
		if(linkhdLayout != null)
		{
			linkhdLayout.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.hdfury.com/"));
					startActivity(browserIntent);
				}
			});
		}
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
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_HDMIVideo,
				getResources().getString(R.string.drawer_title_HDMIVideo_none),
				DrawerItem.DRAWER_ITEM_HDMIVideo));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_HDMILink,
				getResources().getString(R.string.drawer_title_HDMILink_none),
				DrawerItem.DRAWER_ITEM_HDMILink));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_DEVICEVER,
				getResources().getString(R.string.drawer_title_DEVICEVER_none),
				DrawerItem.DRAWER_ITEM_DEVICEVER));
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