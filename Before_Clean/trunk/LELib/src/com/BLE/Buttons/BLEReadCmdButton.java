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

public class BLEReadCmdButton extends BLEButton{
	
	//constant and define
	private final String mTag = "BLEReadCmdButton";
		
	//data members
	private ImageButton mBtnRead = null;
	private TextView    mTvTitle;
	
	//constructors
	public BLEReadCmdButton(Context context) {
		super(context);
	}

	public BLEReadCmdButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBLEReadCmdButton();
	}
	
	public BLEReadCmdButton(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        initBLEReadCmdButton();
    }  
	
	//member functions
	private void initBLEReadCmdButton() {
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
		layoutInflater.inflate(R.layout.blebuttonread, this, true);
		mTvTitle = (TextView) findViewById(R.id.tvTitle);
		if(mTvTitle != null) {
			mTvTitle.setText(cmdTitle);
		}
		
		mBtnRead = (ImageButton) findViewById(R.id.imageButton);
		if(mBtnRead != null) {
			mBtnRead.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					doWriteCmdAndReadRsp(mCmdsColl.get(0));
				}
			});
		}
	}
	
	protected void doWriteCmdAndReadRsp(LECmd leCmd)
	{
		MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
		broadCastAction(BLEUtility.ACTION_SENCMD_READ);
		
		final LECmd tempLeCmd = leCmd;
		Thread workerThread = new Thread() {
		    public void run() {
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
		    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + tempLeCmd.mCmd);
		    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(tempLeCmd.mCmd, false));
		    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
		    	String strRspCal = "";
		    	if(rspCal != null)
		    		strRspCal = new String(rspCal);
				if(strRspCal.contains(tempLeCmd.mCmdRes) == true)
				{
					MyLog.d(mTag, "doWriteCmdAndReadRsp, read ok, content = " + strRspCal);
					mUIHanlder.post(new Runnable() {

						@Override
						public void run() {
							broadCastAction(BLEUtility.ACTION_SENCMD_READ_CONTENT);
						}
					});
				}
				else
				{
					MyLog.d(mTag, "doWriteCmdAndReadRsp, read fail");
					mUIHanlder.post(new Runnable() {
						@Override
						public void run() {
							broadCastAction(BLEUtility.ACTION_SENCMD_READ_FAIL);
						}
					});
				}
		    }
		};
		workerThread.start();
	}
}
