package com.UI.LEDevice;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class IntegralSetting {
	private static final String _NAME = "IntegralSetting";
    private static SharedPreferences _sp = null;
    public static void initSharedPreferences(Context c) {
    	if (_sp==null) {
            _sp = c.getSharedPreferences(_NAME, 0);
        }
    }
    
    public static void destroySharedPreferences() {
		_sp = null;
	}
    
    public static boolean isSuspendBacklight(Context c) {
        return _sp.getBoolean("WakeLock", false);
    }

    public static void setSuspendBacklight(boolean enable) {
        Editor editor = _sp.edit();
        editor.putBoolean("WakeLock", enable);
        editor.commit();
    }
}
