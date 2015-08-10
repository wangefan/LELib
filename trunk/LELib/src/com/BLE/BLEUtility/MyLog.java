package com.BLE.BLEUtility;
import android.util.Log;

public class MyLog 
{	
	public static boolean _DEBUG = false; 
	public static void d(String tag, String msg) {
		if(_DEBUG)
			Log.d("BLE", "[" + tag + "]:" + msg);		
    }
	
	public static void d(String tag, String msg, Throwable tr) {
		if(_DEBUG)
			Log.d("BLE", "[" + tag + "]:" + msg, tr);
    }
	
	public static void e(String tag, String msg) {
		if(_DEBUG)
			Log.e("BLE", "[" + tag + "]:" + msg);
    }
	
	public static void e(String tag, String msg, Throwable tr) {
		if(_DEBUG)
			Log.e("BLE", "[" + tag + "]:" + msg, tr);
    }
	
}
