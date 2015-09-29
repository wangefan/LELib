package com.UI.LEDevice;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class IntegralSetting {
	//Constant 
	private static final String _NAME = "IntegralSetting";
    private static SharedPreferences _sp = null;
    private static final String AUTOCONN = "AutoConn";
    private static final String DEVICENAME = "DeviceName";
    private static final String DEVICEADDR = "DeviceAddr";
    
    public static void initSharedPreferences(Context c) {
    	if (_sp==null) {
            _sp = c.getSharedPreferences(_NAME, 0);
        }
    }
    
    public static void destroySharedPreferences() {
		_sp = null;
	}
    
    public static boolean isAutoConn() {
        return _sp.getBoolean(AUTOCONN, false);
    }

    public static void setAutoConn(boolean enable) {
        Editor editor = _sp.edit();
        editor.putBoolean(AUTOCONN, enable);
        editor.commit();
    }
    
    public static String getDeviceName() {
        return _sp.getString(DEVICENAME, "");
    }
    
    public static void setDeviceName(String name) {
        Editor editor = _sp.edit();
        editor.putString(DEVICENAME, name);
        editor.commit();
    }
    
    public static String getDeviceMACAddr() {
    	 return _sp.getString(DEVICEADDR, "");
    }
    
    public static void setDeviceMACAddr(String addr) {
        Editor editor = _sp.edit();
        editor.putString(DEVICEADDR, addr);
        editor.commit();
    }
}
