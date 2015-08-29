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
	 * Callback to notify disconnected.
	 * Message contains information.
	 */
	public void onDisconnected(String message);
	
	/**
	 * Callback to notify connected.
	 */
	public void onConnected();
	
	/**
	 * Callback to return string data.
	 */
	public void onRead(final String data);
}
