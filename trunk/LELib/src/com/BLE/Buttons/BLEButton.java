package com.BLE.Buttons;


import java.util.ArrayList;

import com.BLE.BLEUtility.BLEUtility;
import com.LELib.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public abstract class BLEButton extends LinearLayout {
	//inner class
	public class LECmd
	{
		public String mCmdTitle = "";
		public String mCmd = "";
		public String mCmdRes = "";
		
		public LECmd(String cmdTitle, String cmd, String cmdRes) {
			mCmdTitle = cmdTitle;
			mCmd = cmd;
			mCmdRes = cmdRes;
		}
	}
	
	//Data members
	protected TypedArray mTypedArray;
	protected ArrayList<LECmd> mCmdsColl;
	protected Handler mUIHanlder = new Handler();
	
	//constructors
	public BLEButton(Context context) {
		super(context);
	}

	public BLEButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBLEButton(context, attrs);
	}
	public BLEButton(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        initBLEButton(context, attrs);  
    }  
	
	//To initialize BLEButton
	private void initBLEButton(Context context, AttributeSet attrs)  {  
		mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.LECmdsStyleDef); 
    }  
	
	//Member functions
	
	 /*
     * <!----------------------------------------------------------------->
     * @Name: doWriteCmdAndReadRsp()
     * @Description: write cmd to device and read response in worker thread.
     * @param: LECmd lecmd. 
     * return: N/A 
     * <!----------------------------------------------------------------->
     * */
	protected void doWriteCmdAndReadRsp(LECmd lecmd)
	{
		//Todo:broadcast event to do cmd now...
		
		final LECmd tempLeCmd = lecmd;
		Thread workerThread = new Thread() {
		    public void run() {
		    	
				String rsp = BLEUtility.getInstance(getContext()).writeCmd(tempLeCmd.mCmd);
				if(rsp.equals(tempLeCmd.mCmdRes) == true)
				{
					mUIHanlder.post(new Runnable() {

						@Override
						public void run() {
							saveUIState();
							//Todo:broadcast event to end cmd now...
						}
					});
				}
				else
				{
					mUIHanlder.post(new Runnable() {
						@Override
						public void run() {
							restoreUIState();
							//Todo:broadcast event to end cmd now...
						}
					});
				}
		    }
		};
		workerThread.start();
	}
	
	//virtual functions
	protected abstract void saveUIState();
	
	protected abstract void restoreUIState();
	
	public abstract String toggleNextState();
	
}
