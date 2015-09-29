package com.BLE.Buttons;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.LELib.R;
import com.utility.CmdProcObj;

public class BLEOneWrtCmdButton extends BLEButton {

	//constant and define
	private final String mTag = "BLEOneWrtCmdButton";
	
	//data members
	private ImageButton mBtnWrite = null;
	private TextView    mTvTitle;
	
	//constructors
	public BLEOneWrtCmdButton(Context context) {
		super(context);
	}

	public BLEOneWrtCmdButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBLEOneWrtCmdButton();
	}
	public BLEOneWrtCmdButton(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        initBLEOneWrtCmdButton();
    }  
	
	//member functions
	private void initBLEOneWrtCmdButton() {
		String cmdTitle = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1CmdTitle);        
        String cmd = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1Cmd);
        String cmdRes = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1CmdRes);
        
        mTypedArray.recycle();
        mCmdsColl = new ArrayList<LECmd>();
        if(mCmdsColl != null)
        {
        	mCmdsColl.add(new LECmd(cmdTitle, cmd, cmdRes));
        }
        
		final LayoutInflater layoutInflater =
	            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.blebuttonwrite, this, true);
		mTvTitle = (TextView) findViewById(R.id.tvTitle);
		if(mTvTitle != null) {
			mTvTitle.setText(cmdTitle);
		}
		
		mBtnWrite = (ImageButton) findViewById(R.id.imageButton);
		if(mBtnWrite != null) {
			mBtnWrite.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					doWriteCmdAndReadRsp(mCmdsColl.get(0));
				}
			});
		}
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
		broadCastAction(BLEUtility.ACTION_SENCMD_BEGIN);
		
		final LECmd tempLeCmd = leCmd;
		Thread workerThread = new Thread() {
		    public void run() {
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + tempLeCmd.mCmd);
		    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(tempLeCmd.mCmd, true));
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
							
							broadCastAction(BLEUtility.ACTION_SENCMD_OK);
						}
					});
				}
				else
				{
					MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd not match response");
					mUIHanlder.post(new Runnable() {
						@Override
						public void run() {
							
							broadCastAction(BLEUtility.ACTION_SENCMD_FAIL);
						}
					});
				}
		    }
		};
		workerThread.start();
	}
	
	//virtual functions
	protected void saveUIState() {}
	
	protected void restoreUIState() {}

}
