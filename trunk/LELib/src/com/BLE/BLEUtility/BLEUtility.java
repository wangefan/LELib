package com.BLE.BLEUtility;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.ArrayUtils;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

public class BLEUtility
{
	//public define
	/**
	 * broadcast to get low energy devices after calling 
	 * StartScanLEDevices.
	 * Stop scanning by calling StopScanLEDevices.
	 * This broadcast is fired from worker thread.
	 */
	public final static String ACTION_GET_LEDEVICE = "ACTION_GET_LEDEVICE";
	public final static String ACTION_GET_LEDEVICE_KEY = "ACTION_GET_LEDEVICE_KEY";
	public final static String ACTION_CONNSTATE_CONNECTED = "ACTION_CONNSTATE_CONNECTED";
	public final static String ACTION_CONNSTATE_DISCONNECTED = "ACTION_CONNSTATE_DISCONNECTED";
	public final static String ACTION_CONNSTATE_DISCONNECTED_KEY = "ACTION_CONNSTATE_DISCONNECTED_KEY";
	public final static String ACTION_CONNSTATE_CONNECTING = "ACTION_CONNSTATE_CONNECTING";
	public final static String ACTION_DATA = "ACTION_DATA";
	public final static String ACTION_DATA_KEY = "ACTION_DATA_KEY";
	public static final String ACTION_SENCMD_BEGIN = "com.BLE.BLEUtility.ACTION_SENCMD_BEGIN";
    public static final String ACTION_SENCMD_OK = "com.BLE.BLEUtility.ACTION_SENCMD_OK";
    public static final String ACTION_SENCMD_FAIL = "com.BLE.BLEUtility.ACTION_SENCMD_FAIL";
    public static final String ACTION_SENCMD_SWFORCE = "com.BLE.BLEUtility.ACTION_SENCMD_SWFORCE";
    public static final String ACTION_SENCMD_READ = "com.BLE.BLEUtility.ACTION_SENCMD_READ";
    public final static String ACTION_SENCMD_READ_CONTENT_KEY = "com.BLE.BLEUtility.ACTION_SENCMD_READ_CONTENT_KEY";
    public static final String ACTION_SENCMD_READ_CONTENT = "com.BLE.BLEUtility.ACTION_SENCMD_READ_CONTENT";
    public static final String ACTION_SENCMD_READ_FAIL = "com.BLE.BLEUtility.ACTION_SENCMD_READ_FAIL";
    public final static String ACTION_ITEM_READ_UPDATE = "com.BLE.BLEUtility.ACTION_ITEM_READ_UPDATE";
    public final static String ACTION_ITEM_READ_END = "com.BLE.BLEUtility.ACTION_ITEM_READ_END";
	public final static String ACTION_WRTREAD_WRT_BEG = "ACTION_WRTREAD_WRT_BEG";
	public final static String ACTION_WRTREAD_WRT_UPDATE = "ACTION_WRTREAD_WRT_UPDATE";
	public final static String ACTION_WRTREAD_WRT_FAIL = "ACTION_WRTREAD_WRT_FAIL";
	
    //Messages
    public static final String DISCONNECTED_CAUSE_NORSP = "Connect no response";
    public static final String DISCONNECTED_CAUSE_DIS = "disconnted";
    public static final String DISCONNECTED_CAUSE_NOSERVICE = "discover no services";
    public static final String DISCONNECTED_CAUSE_PFLDIS = "BluetoothProfile.STATE_DISCONNECTED";
	
    //inner define
	final private String mTag = "BLEUtility";
	final private static UUID mSUUIDString = UUID.fromString("edee2909-12b0-3e9d-1042-4c0bc820c4dc");
	final private static UUID mCUUIDString = UUID.fromString("1249c28c-63b4-219b-814a-393944dec8c1");
	private enum ConnStatus { CONN_STATE_DISCONNECTED, CONN_STATE_CONNECTING, CONN_STATE_CONNECTED}
		 
	// Device scan callback.
    @SuppressLint("NewApi")
	private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) 
        {
        	ArrayUtils.reverse(scanRecord);	 
        	String discoveryServceID = String.format("%x", new BigInteger(1, scanRecord));
        	
        	if(discoveryServceID.indexOf(mSUUIDString.toString().replace("-", "")) != -1)
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
        				
        				//Broadcast.
        				final Intent brdConnState = new Intent(ACTION_GET_LEDEVICE);
        				brdConnState.putExtra(ACTION_GET_LEDEVICE_KEY, cBTDeivce);
        		        mContext.sendBroadcast(brdConnState);
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
        	mFireDisconnected(DISCONNECTED_CAUSE_NOSERVICE);
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
            		mFireDisconnected(DISCONNECTED_CAUSE_NOSERVICE);
            		if(mBAutoReconnect)
        	        	mSetCheckConnTimer(true);
            	}
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            	mDisconnect();
            	mFireDisconnected(DISCONNECTED_CAUSE_PFLDIS);
    	        if(mBAutoReconnect)
    	        	mSetCheckConnTimer(true);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS && mBWaitData &&characteristic.getValue() != null) 
            {
            	final byte[] data = characteristic.getValue();
            	byte resDataTemp[] = new byte [data.length];
            	for(int idxData = 0; idxData < data.length; ++idxData)
            		resDataTemp[idxData] = data[idxData];
            	mResData = resDataTemp;
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        	final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0 && mBWaitData) 
            {
            	byte resDataTemp[] = new byte [data.length];
            	for(int idxData = 0; idxData < data.length; ++idxData)
            		mResData[idxData] = data[idxData];
            	mResData = resDataTemp;
                MyLog.d(mTag, "onCharacteristicChanged, Thread id = " + android.os.Process.myTid() + ", data = [" + data + "]");
            }
        }
    };
	
    //Data members
    static private BLEUtility mMe = null;
	private BluetoothAdapter mBluetoothAdapter = null;
	private BluetoothGatt mBluetoothGatt = null;
	private BluetoothGattCharacteristic mBTCharct = null;
	private ArrayList<BLEDevice> mbtDeviceList = null;
	private BLEDevice mDestBTLEDevice = null;
	private ConnStatus mConnStatus = ConnStatus.CONN_STATE_DISCONNECTED;
	static private Context mContext = null;
	private boolean mBAutoReconnect = false;
	private Handler mHandlerCheckConn = new Handler();
	private Handler mHandlerConnTimeout = new Handler();
	private Lock mlockWriteRead = new ReentrantLock();   
	private byte[] mResData = null;
	private boolean mBWaitData;
	
	//private functions
	//default constructor
	private BLEUtility() 
	{
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	    mbtDeviceList = new ArrayList<BLEDevice>();
	    mBAutoReconnect = false;
	    mBWaitData = false;
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
        
		mConnStatus = ConnStatus.CONN_STATE_DISCONNECTED;
	}
	
	/*
    private void mBroadcastConnChangeWithInfo(CONN_STATE connState, String message)
	{
		// Client should use Servic.GetConnState() to get status
		mConnState = connState;
		final Intent brdConnState = new Intent(ACTION_CONN_STATE_CHANGED);
		brdConnState.putExtra(ACTION_CONN_STATE_CHANGED_KEY, message);
        sendBroadcast(brdConnState);
	}
	*/
	
	void mFireConnecting()
	{
		final Intent brd = new Intent(ACTION_CONNSTATE_CONNECTING);
		mContext.sendBroadcast(brd);
	}
	
	void mFireDisconnected(String message)
	{
		final Intent brd = new Intent(ACTION_CONNSTATE_DISCONNECTED);
		brd.putExtra(ACTION_CONNSTATE_DISCONNECTED_KEY, message);
		mContext.sendBroadcast(brd);
	}
	
	void mFireConnected()
	{
		final Intent brd = new Intent(ACTION_CONNSTATE_CONNECTED);
		mContext.sendBroadcast(brd);
	}
	
	void mFireReceivingData(String strData)
	{
		final Intent brd = new Intent(ACTION_DATA);
		brd.putExtra(ACTION_DATA_KEY, strData);
		mContext.sendBroadcast(brd);
	}
	
    //public methods
	static public BLEUtility getInstance()
	{
		if(mMe == null)
			mMe = new BLEUtility();
		return mMe;
	}
	
	//public methods
	static public void initBLEUtility(Context context)
	{
		mContext = context;
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
			mBluetoothAdapter.startLeScan( mLeScanCallback);
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
	
	public boolean isConnect() {
		return mConnStatus != ConnStatus.CONN_STATE_DISCONNECTED;
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
			if(mConnStatus != ConnStatus.CONN_STATE_DISCONNECTED)
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
                    	mFireDisconnected(DISCONNECTED_CAUSE_NORSP);
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
        	mFireDisconnected(e.getMessage());
		}
		catch (Exception e) {
			mDisconnect();
			String message = "Default Exception";
			if(e != null)
				message = e.getMessage();
            if(mBAutoReconnect)
	        	mSetCheckConnTimer(true);
        	mFireDisconnected(message);
		}
	}
	
	public void disconnect() {
		mDisconnect();
		mFireDisconnected(DISCONNECTED_CAUSE_DIS);
	}
	
	public void read() throws BLEUtilityException
	{
		MyLog.d(mTag, "read begin");
		// Characteristic has read property
		if(mBTCharct == null)
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		if ((mBTCharct.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) 
		{
			if(mBluetoothGatt.readCharacteristic(mBTCharct) == false)
			{
				MyLog.d(mTag, "readCharacteristic false");
				throw new BLEUtilityException(BLEUtilityException.CHAR_READFAIL);
			}
        }
		else
		{
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		}
	}
	
	public void write(byte [] command) throws BLEUtilityException
	{
		// Characteristic has read property
		if(mBTCharct == null)
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		if ((mBTCharct.getProperties() | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) 
		{
			String strCmd = new String(command);
			String strHex = String.format("%x", new BigInteger(1, command));
			MyLog.d(mTag, "command = " + strCmd +", hex = " + strHex);
			mBTCharct.setValue(command);
			if(mBluetoothGatt.writeCharacteristic(mBTCharct) == false)
			{
				MyLog.d(mTag, "write command false");
				throw new BLEUtilityException(BLEUtilityException.CHAR_WRITEFAIL);
			}
			else
				MyLog.d(mTag, "write success, Thread id = " + Thread.currentThread().getId() + " , command = [" + strCmd + "]");
        }
		else
		{
			throw new BLEUtilityException(BLEUtilityException.CHAR_NOTREADY);
		}
	}
	
	//blocking call, return read data
	public byte[] writeCmd(byte[] command)
	{
		mlockWriteRead.lock();
		MyLog.d(mTag, "writeCmd begin (in lock now), in thread = " + Thread.currentThread().getId());
		try {
			mResData = null;
			write(command);
			mBWaitData = true;
			Thread.sleep(400);
			read();
			final int nWaitMilliSecs = 2000;
			final int nWaitStep = 100;
			int nTime = 0;
			do {
				Thread.sleep(nWaitStep);
				if(mResData != null)
					break;
				nTime += nWaitStep;
			}
			while(nTime < nWaitMilliSecs);
			if(mResData == null)
			{
				MyLog.d(mTag, "timeout, throw BLEUtilityException read fail");
				throw new BLEUtilityException(BLEUtilityException.CHAR_READFAIL);
			}
		} catch (BLEUtilityException e) {
			MyLog.d(mTag, "catch BLEUtilityException");
			e.printStackTrace();
			mBWaitData = false;
			mResData = null;
			
		} catch (InterruptedException e) {
			MyLog.d(mTag, "catch InterruptedException");
			e.printStackTrace();
			mBWaitData = false;
			mResData = null;
		}
		finally {
			String strData = "", strHex = "";
			if(mResData != null)
			{
				strData = new String(mResData);
				strHex = String.format("%x", new BigInteger(1, mResData));
			}
			MyLog.d(mTag, "read data = " + strData + ", hex = " + strHex);	
			MyLog.d(mTag, "mlockWriteRead.unlock(), in thread = " + Thread.currentThread().getId() + ", writeCmd end");
			mBWaitData = false;
			mlockWriteRead.unlock();
		}
		
		return mResData;
	}
}
