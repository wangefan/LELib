package com.UI.LEDevice;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public abstract class BTSettingActivity extends Activity {
	protected boolean mActive = false;
	
	//This method be called after requesting turning on and allow BT.
	abstract protected void mDoThingsAtrEnableBTActy();
	
	//constant 
	public static final int REQUEST_ENABLE_BT = 2;
	
	//Members
	protected BluetoothAdapter mBluetoothAdapter;
	
	//member funcitons
	protected void mDoBTIntentForResult()
    {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
    
	private static IntentFilter makeBTActionsIntentFilter() 
	{
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		return intentFilter;
	}
	
	/*
     * <!----------------------------------------------------------------->
     * @Name: mBTActReceiver
     * @Description: Receiver the Bluetooth Turn on/off event. 
     * return: N/A 
     * <!----------------------------------------------------------------->
     * */
    private BroadcastReceiver mBTActReceiver = new BroadcastReceiver()
	{
		@Override
        public void onReceive(Context context, Intent intent) 
		{
        	final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                               BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) 
                {
                    
                } 
                else if (state == BluetoothAdapter.STATE_OFF) 
                {
                	if (!mBluetoothAdapter.isEnabled() && mActive) {
                		mDoBTIntentForResult();
                        return;
                    }
                }
            }
		}
    };
    
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_CANCELED)
            {
            	finish();
                return;
            }
            else	//allow
            {
            	mDoThingsAtrEnableBTActy();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState); 
        registerReceiver(mBTActReceiver, makeBTActionsIntentFilter());
    }
	
	@Override
	protected void onResume() {
		super.onResume();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null)
		{
            finish();
            return;
		}
		
		// Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
        	mDoBTIntentForResult();
            return;
        }
        mActive = true;
	}
	
	@Override
	protected void onPause() 
	{
		mActive = false;
		super.onPause();
	}
	
	@Override
	protected void onDestroy() 
	{
		unregisterReceiver(mBTActReceiver);
		super.onDestroy();
	}
}
