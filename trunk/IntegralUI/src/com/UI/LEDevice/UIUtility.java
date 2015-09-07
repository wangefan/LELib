package com.UI.LEDevice;

import android.app.ProgressDialog;
import android.content.Context;

public class UIUtility {
	static private ProgressDialog mPDialog = null;
	
	//member functions
	static public void showProgressDlg(Context context, boolean bShow, String message)
    {
    	if(bShow)
    	{
    		mPDialog = ProgressDialog.show(context, "Process...", message);
    	}
    	else
    	{
    		if(mPDialog != null)
    			mPDialog.dismiss();
    	}
    }
}
