package com.BLE.Buttons;


import java.util.ArrayList;

import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.LELib.R;
import com.utility.CmdProcObj;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public abstract class BLEButton extends LinearLayout {
	//constant and define
	private final String mTag = "BLEButton";
	//broadcast actions.
    public static final String ACTION_SENCMD_BEGIN =
            "com.BLE.Buttons.ACTION_SENCMD_BEGIN";
    
    public static final String ACTION_SENCMD_END =
            "com.BLE.Buttons.ACTION_SENCMD_END";
    
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
     * @Name: broadCastAction()
     * @Description: send broadcast intent.
     * return: N/A 
     * <!----------------------------------------------------------------->
     * */
	protected void broadCastAction(String action)
	{
		final Intent brdConnState = new Intent(action);
        getContext().sendBroadcast(brdConnState);
	}
	
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
		MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
		broadCastAction(ACTION_SENCMD_BEGIN);
		
		final LECmd tempLeCmd = lecmd;
		Thread workerThread = new Thread() {
		    public void run() {
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + tempLeCmd.mCmd);
		    	String rsp = BLEUtility.getInstance(getContext()).writeCmd(CmdProcObj.proc(tempLeCmd.mCmd));
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd response = " + rsp);
				if(rsp.equals(tempLeCmd.mCmdRes) == true)
				{
					MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
					mUIHanlder.post(new Runnable() {

						@Override
						public void run() {
							saveUIState();
							broadCastAction(ACTION_SENCMD_END);
						}
					});
				}
				else
				{
					MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd not match response");
					mUIHanlder.post(new Runnable() {
						@Override
						public void run() {
							restoreUIState();
							broadCastAction(ACTION_SENCMD_END);
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
}
