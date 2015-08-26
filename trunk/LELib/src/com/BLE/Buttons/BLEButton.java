package com.BLE.Buttons;


import java.util.ArrayList;
import com.LELib.R;
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
    
    public static final String ACTION_SENCMD_OK =
            "com.BLE.Buttons.ACTION_SENCMD_OK";
    
    public static final String ACTION_SENCMD_FAIL =
            "com.BLE.Buttons.ACTION_SENCMD_FAIL";
    
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
}
