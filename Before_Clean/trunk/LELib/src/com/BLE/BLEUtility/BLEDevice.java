package com.BLE.BLEUtility;
import java.io.Serializable;
import android.bluetooth.BluetoothDevice;

public class BLEDevice implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	//Data members
	private String mStrDeviceName = null;
	private String mStrDeviceAddress = null;
	
	//constructor 
	BLEDevice()
	{
		mStrDeviceName = "";
		mStrDeviceAddress = "";
	}
	
	public BLEDevice(String strDeviceName, String strDeviceAddress)
	{
		mStrDeviceName = new String(strDeviceName);
		mStrDeviceAddress = new String(strDeviceAddress);
	}
	
	//constructor 
	BLEDevice(BLEDevice src)
	{
		mStrDeviceName = new String(src.getDeviceName());
		mStrDeviceAddress = new String(src.getAddress());
	}
	
	BLEDevice(BluetoothDevice src)
	{
		mStrDeviceName = new String(src.getName());
		mStrDeviceAddress = new String(src.getAddress());
	}
	
	public void getParamFromBTDevice(BluetoothDevice src)
	{
		mStrDeviceName = src.getName();
		mStrDeviceAddress = src.getAddress();
	}
	
	public String getDeviceName() {
		return mStrDeviceName;
	}

	public String getAddress() {
		return mStrDeviceAddress;
	}
}
