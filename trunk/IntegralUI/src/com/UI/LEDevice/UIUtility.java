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
    		if(mPDialog == null)
    		{
    			mPDialog = new ProgressDialog(context, R.style.MyProgressDlg);
    			mPDialog.setCancelable(false);
        		mPDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Small);
    		}
    		mPDialog.setMessage(message);
    		mPDialog.show();
    	}
    	else
    	{
    		if(mPDialog != null)
    			mPDialog.hide();
    	}
    }
}
