package com.BLE.Buttons;


import com.LELib.R;

import android.content.Context;
import android.content.res.TypedArray;
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
	
	//virtual functions
	public abstract String toggleNextState();
}
