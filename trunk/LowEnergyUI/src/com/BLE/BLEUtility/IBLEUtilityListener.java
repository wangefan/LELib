package com.BLE.BLEUtility;

public interface IBLEUtilityListener
{
	/**
	 * Callback to get low energy devices after calling 
	 * StartScanLEDevices.
	 * Stop scanning by calling StopScanLEDevices.
	 * This callback is fired from worker thread.
	 */
	public void onGetLEDevice(final BLEDevice device);
	
	/**
	 * Callback to notify connection beginning.
	 */
	public void onConnecting();
	
	/**
	 * Callback to notify connection error.
	 * Message contains error information.
	 */
	public void onConnectError(String message);
	
	/**
	 * Callback to notify connected.
	 */
	public void onConnected();
	
	/**
	 * Callback to notify disconnected.
	 */
	public void onDisconnected();
	
	/**
	 * Callback to return string data.
	 */
	public void onRead(final String data);
}
