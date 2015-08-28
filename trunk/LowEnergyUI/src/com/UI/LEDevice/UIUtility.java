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
    		if(mPDialog != null)
    		{
    			mPDialog.setTitle("Process...");
    			mPDialog.setMessage(message);
    			mPDialog.show();
    		}
    		else
    		{
    			mPDialog = ProgressDialog.show(context, "Process...", message);
    		}
    	}
    	else
    	{
    		if(mPDialog != null)
    			mPDialog.dismiss();
    	}
}
}
