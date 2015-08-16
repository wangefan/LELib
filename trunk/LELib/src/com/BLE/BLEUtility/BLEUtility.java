package com.BLE.BLEUtility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;

public class BLEUtility
{
	//inner define
	final private String mTAG = "BLEUtility";
	final private static UUID mSUUIDString = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
	final private static UUID mCUUIDString = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
	private enum ConnStatus { CONN_STATE_IDLE, CONN_STATE_CONNECTING, CONN_STATE_CONNECTED}
		
	// Device scan callback.
    @SuppressLint("NewApi")
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) 
        {
        	// check if has device already?
        	boolean bAdd = true; 
        	for(BLEDevice cBTDevice : mbtDeviceList) 
        	{
				String strSrcDevice = cBTDevice.getAddress();
				if(true == device.getAddress().equals(strSrcDevice)) {
					bAdd = false;
					break;
				}
			}
        	
        	if(bAdd) 
        	{
        		String name = device.getName();
        		String add = device.getAddress();
        		if(name != null && add != null)
        		{
        			BLEDevice cBTDeivce = new BLEDevice(name, add);
    				mbtDeviceList.add(cBTDeivce);
    				
    				//fire to listener.
    				if(mListener != null) {
    					mListener.onGetLEDevice(cBTDeivce);
    				}	
        		}
        	}
        }
    };
    
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    @SuppressLint("NewApi")
	private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() 
    {
    	@Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) 
    	{
            if (status == BluetoothGatt.GATT_SUCCESS) {
            	//Set Characteristic Notification
            	List<BluetoothGattService> lstServices = gatt.getServices();
            	for(BluetoothGattService btService : lstServices) 
            	{	
            		if(btService.getUuid().equals(mSUUIDString) ) {
            			BluetoothGattCharacteristic btCharct = btService.getCharacteristic(mCUUIDString);
            			if(btCharct != null) 
            			{
                			mResetCharacteristic();
                			
                			// Characteristic has notify property
                			if(0 < (btCharct.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                			{            					
                				//To trigger that can receive data from callback "onCharacteristicChanged"
                				mBluetoothGatt.setCharacteristicNotification(btCharct, true);
                			}
                			mBTCharct = btCharct;
                			mHandlerConnTimeout.removeCallbacksAndMessages(null);
                			mConnStatus = ConnStatus.CONN_STATE_CONNECTED;
                			mFireConnected();
                			if(mBAutoReconnect)
                	        	mSetCheckConnTimer(false);
                			return;
            			}
            		}
            	}
            }
            
            //discover no services.
            mDisconnect();
        	mFireConnectError("discover no services.");
            if(mBAutoReconnect)
	        	mSetCheckConnTimer(true);
        }
    	
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) 
            {
            	if(mBluetoothGatt == null || false == mBluetoothGatt.discoverServices())
            	{
            		mDisconnect();
            		mFireConnectError("discover services = false");
            		if(mBAutoReconnect)
        	        	mSetCheckConnTimer(true);
            	}
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	mDisconnect();
            	mFireConnectError("BluetoothProfile.STATE_DISCONNECTED");
    	        if(mBAutoReconnect)
    	        	mSetCheckConnTimer(true);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) 
            {
            	final byte[] data = characteristic.getValue();
            	MyLog.d(mTAG, "onCharacteristicRead, Thread id = " + android.os.Process.myTid() + " , data = [" + data + "]");
            }
        }
        
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) 
            {
            	final byte[] data = characteristic.getValue();
            	MyLog.d(mTAG, "onCharacteristicWrite, Thread id = " + android.os.Process.myTid() + ", data = [" + data + "]");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) 
            {
            	mResData = new String(data);
                mFireReceivingData(mResData);
                MyLog.d(mTAG, "onCharacteristicChanged, Thread id = " + android.os.Process.myTid() + ", data = [" + data + "]");
            }
            mlockWriteRead.unlock();
        }
    };
	
    //Data members
    static private BLEUtility mMe = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothGatt mBluetoothGatt = null;
	private BluetoothGattCharacteristic mBTCharct = null;
	private ArrayList<BLEDevice> mbtDeviceList = null;
	private IBLEUtilityListener mListener = null;
	private BLEDevice mDestBTLEDevice = null;
	private ConnStatus mConnStatus = ConnStatus.CONN_STATE_IDLE;
	private Context mContext = null;
	private boolean mBAutoReconnect = false;
	private Handler mHandlerCheckConn = new Handler();
	private Handler mHandlerConnTimeout = new Handler();
	private Lock mlockWriteRead = new ReentrantLock();   
	private String mResData = "";
	
	//private functions
	//default constructor
	private BLEUtility(Context context) 
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    mbtDeviceList = new ArrayList<BLEDevice>();
	    mContext = context;
	    mBAutoReconnect = false;
	}
	
	// @param: boolean bSetTimer
	// true: set timer with 8secs to connect again, 
	// false: remove timer and all pending runnable.
	protected void mSetCheckConnTimer(boolean bSetTimer) {
		if(bSetTimer) 
		{
			final int CHECK_TIME_STAMP = 8000;
			Runnable checkConn = new Runnable() 
			{
				@Override
				public void run(){
					if(mDestBTLEDevice != null)
					connect(mDestBTLEDevice.getAddress());
				}
			};
			mHandlerCheckConn.postDelayed(checkConn, CHECK_TIME_STAMP);
		}
		else {
			mHandlerCheckConn.removeCallbacksAndMessages(null);
		}
	}
	
	@SuppressLint("NewApi")
	private void mResetCharacteristic() {
		if(mBluetoothGatt != null && mBTCharct != null)
			mBluetoothGatt.setCharacteristicNotification(mBTCharct, false);
		mBTCharct = null;
	}
	
	@SuppressLint("NewApi")
	private void mDisconnect()
	{
		if (mBluetoothGatt != null) {
			mResetCharacteristic();
	        mBluetoothGatt.close();
	        mBluetoothGatt = null;
	        mHandlerConnTimeout.removeCallbacksAndMessages(null);
        }
        
		mConnStatus = ConnStatus.CONN_STATE_IDLE;
	}
	
	void mFireConnecting()
	{
		if(mListener != null)
			mListener.onConnecting();
	}
	
	void mFireConnectError(String message)
	{
		if(mListener != null)
			mListener.onConnectError(message);
	}
	
	void mFireConnected()
	{
		if(mListener != null)
			mListener.onConnected();
	}
	
	void mFireDisconnected()
	{
		if(mListener != null)
			mListener.onDisconnected();
	}
	
	void mFireReceivingData(String strData)
	{
		if(mListener != null)
			mListener.onRead(strData);
	}
	
    //public methods
	static public BLEUtility getInstance(Context context)
	{
		if(mMe == null)
			mMe = new BLEUtility(context);
		return mMe;
	}
	
    /**
	 * Set listener from client.
	 */
    public void setListener(IBLEUtilityListener listener)
    {
    	mListener = listener;
    }
    
    /**
	 * start to scan devices, will report devices by
	 * IBLEUtilityListener.onGetLEDevice()
	 */
    @SuppressLint("NewApi")
	public void startScanLEDevices() 
	{
    	if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
		{
    		mbtDeviceList.clear();
			mBluetoothAdapter.startLeScan(mLeScanCallback);
		}
	}
	
    /**
   	 * stop to scan devices.
   	 */
	@SuppressLint("NewApi")
	public void stopScanLEDevices() 
	{
		if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) 
		{
			mBluetoothAdapter.stopLeScan(mLeScanCallback);
		}
	}
	
	/**
   	 * To enable/disable auto re-connect if 
   	 * connetion is break.
   	 */
	public void setAutoReconnect(boolean enable)
	{
		mBAutoReconnect = enable;
		if(mBAutoReconnect == false)
			mSetCheckConnTimer(false);
	}

	public boolean isAutoReconnect() {
		return mBAutoReconnect;
	}
	 
	/**
   	 * connect to low energy device, 
   	 * param:MAC address.
   	 * Throw:
   	 */
	@SuppressLint("NewApi")
	public void connect(final String deviceMACAddr)
	{
		try {
			if(mConnStatus != ConnStatus.CONN_STATE_IDLE)
				throw new BLEUtilityException(BLEUtilityException.CONN_ALREADY_CONN);
			
			 //clean checking connection timer first, it should trigger after connecting error. 
	        if(mBAutoReconnect)
	        	mSetCheckConnTimer(false);
	        
	        mConnStatus = ConnStatus.CONN_STATE_CONNECTING;
			mFireConnecting();
			
			if(deviceMACAddr == null || deviceMACAddr.length() <= 0)
				throw new BLEUtilityException(BLEUtilityException.CONN_BADTARGET);
			
			if(mBluetoothAdapter == null)
				new BLEUtilityException(BLEUtilityException.BT_NOTENABLE);
				
			mDestBTLEDevice = null;
			for (BLEDevice device : mbtDeviceList) {
				if(device.getAddress().equals(deviceMACAddr)) {
					mDestBTLEDevice = device;
					break;
				}
			}
			
			if(mDestBTLEDevice == null)
				mDestBTLEDevice = new BLEDevice("un-known name", deviceMACAddr);
				
			final BluetoothDevice receivedDevice = mBluetoothAdapter.getRemoteDevice(mDestBTLEDevice.getAddress());
		    if (receivedDevice == null) 
		       	throw new BLEUtilityException(BLEUtilityException.BT_CANT_GETDEVICE);
		        
	        // We want to directly connect to the device, so we are setting the autoConnect
	        // parameter to false.
	        mBluetoothGatt = receivedDevice.connectGatt(mContext, false, mGattCallback);
	        
	        //disconnect if timeout
	        final long SCAN_PERIOD = 10000;
	        mHandlerConnTimeout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(ConnStatus.CONN_STATE_CONNECTED != mConnStatus) {
                    	mDisconnect();
                    	mFireConnectError("Connect no response");
            	        if(mBAutoReconnect)
            	        	mSetCheckConnTimer(true);
                    }
                }
            }, SCAN_PERIOD);
		}
		catch (BLEUtilityException e) {
			mDisconnect();
            if(mBAutoReconnect)
	        	mSetCheckConnTimer(true);
        	mFireConnectError(e.getMessage());
		}
		catch (Exception e) {
			mDisconnect();
			String message = "Default Exception";
			if(e != null)
				message = e.getMessage();
            if(mBAutoReconnect)
	        	mSetCheckConnTimer(true);
        	mFireConnectError(message);
		}
	}
	
	public void disconnect() {
		mDisconnect();
		mFireDisconnected();
	}
	
	/*
	public void read() throws BLEUtilityException
	{
		// Characteristic has read property
		if(mBTCharct == null)
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		if ((mBTCharct.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) 
		{
			if(mBluetoothGatt.readCharacteristic(mBTCharct) == false)
			{
				throw new BLEUtilityException(BLEUtilityException.CHAR_READFAIL);
			}
        }
		else
		{
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		}
	}
	*/
	public void write(String command) throws BLEUtilityException
	{
		// Characteristic has read property
		if(mBTCharct == null)
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		if ((mBTCharct.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) 
		{
			String strHex = String.format("%x", new BigInteger(1, command.getBytes()));
			MyLog.d(mTAG, "command hex =[" + strHex);
			mBTCharct.setValue(command.getBytes());
			if(mBluetoothGatt.writeCharacteristic(mBTCharct) == false)
			{
				throw new BLEUtilityException(BLEUtilityException.CHAR_WRITEFAIL);
			}
        }
		else
		{
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		}
	}
	
	//blocking call, return read data
	public String writeCmd(String command)
	{
		mlockWriteRead.lock();
		try {
			mResData = "";
			write(command);
			final int nWaitMilliSecs = 2000;
			final int nWaitStep = 100;
			int nTime = 0;
			do {
				if(mResData.length() > 0)
				{
					return mResData;
				}
				Thread.sleep(nWaitStep);
				nTime += nWaitStep;
			}
			while(nTime < nWaitMilliSecs);
			if(mResData.length() <= 0)
				throw new BLEUtilityException(BLEUtilityException.CHAR_READFAIL);
		} catch (BLEUtilityException e) {
			e.printStackTrace();
			mResData = "";
			mlockWriteRead.unlock();
		} catch (InterruptedException e) {
			e.printStackTrace();
			mResData = "";
			mlockWriteRead.unlock();
		}
		return mResData;
	}
}