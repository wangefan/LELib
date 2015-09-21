package com.UI.LEDevice;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Point;
import android.view.Gravity;
import android.view.WindowManager;

public class UIUtility {
	static private ProgressDialog mPDialog = null;
	static private Point mPrgDlgSize = new Point();
	
	//member functions
	static public void showProgressDlg(Context context, boolean bShow, int messageID)
    {
    	if(bShow)
    	{
    		mPDialog = new ProgressDialog(context, R.style.MyProgressDlg);
    		mPDialog.setCancelable(false); 
    		mPDialog.setMessage(context.getText(messageID));
    		mPDialog.show();
    		
    		Point wndSize = new Point();
			mPDialog.getWindow().getWindowManager().getDefaultDisplay().getSize(wndSize);
    		mPrgDlgSize.x = 4 * wndSize.x / 5;
    		mPrgDlgSize.y = wndSize.y / 4;
    		WindowManager.LayoutParams lp = mPDialog.getWindow().getAttributes();     
    		lp.alpha = 0.6f;    
    		lp.dimAmount=0.0f;  
    		lp.width = mPrgDlgSize.x;
    		lp.height = mPrgDlgSize.y;
    		mPDialog.getWindow().setAttributes(lp);   
    	}
    	else
    	{
    		if(mPDialog != null)
    		{
    			mPDialog.dismiss();
    			mPDialog = null;
    		}
    	}
    }
}
