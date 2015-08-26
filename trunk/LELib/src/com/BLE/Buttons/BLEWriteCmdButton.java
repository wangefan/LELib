package com.BLE.Buttons;

import java.util.ArrayList;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.utility.CmdProcObj;

public abstract class BLEWriteCmdButton extends BLEButton {

	//constant and define
	private final String mTag = "BLEWriteCmdButton";
	
	//constructors
	public BLEWriteCmdButton(Context context) {
		super(context);
	}

	public BLEWriteCmdButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	public BLEWriteCmdButton(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
    }  
	
	 /*
     * <!----------------------------------------------------------------->
     * @Name: doWriteCmdAndReadRsp()
     * @Description: write cmd to device and read response in worker thread.
     * @param: LECmd lecmd. 
     * return: N/A 
     * <!----------------------------------------------------------------->
     * */
	protected void doWriteCmdAndReadRsp(LECmd leCmd)
	{
		MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
		broadCastAction(ACTION_SENCMD_BEGIN);
		
		final LECmd tempLeCmd = leCmd;
		Thread workerThread = new Thread() {
		    public void run() {
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + tempLeCmd.mCmd);
		    	byte [] rsp = BLEUtility.getInstance(getContext()).writeCmd(CmdProcObj.addCRC(tempLeCmd.mCmd, true));
		    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
		    	String strRspCal = "";
		    	if(rspCal != null)
		    		strRspCal = new String(rspCal);
				if(strRspCal.equals(tempLeCmd.mCmdRes) == true)
				{
					MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
					mUIHanlder.post(new Runnable() {

						@Override
						public void run() {
							saveUIState();
							broadCastAction(ACTION_SENCMD_OK);
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
							broadCastAction(ACTION_SENCMD_FAIL);
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
