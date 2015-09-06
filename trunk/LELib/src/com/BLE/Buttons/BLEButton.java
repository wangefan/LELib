package com.BLE.Buttons;


import java.util.ArrayList;
import com.LELib.R;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
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
    
    public static final String ACTION_SENCMD_READ =
            "com.BLE.Buttons.ACTION_SENCMD_READ";
    
    public static final String ACTION_SENCMD_READ_CONTENT =
            "com.BLE.Buttons.ACTION_SENCMD_READ_CONTENT";
    
    public static final String ACTION_SENCMD_READ_FAIL =
            "com.BLE.Buttons.ACTION_SENCMD_READ_FAIL";
    
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
	protected float mPosX = 0.0f;
	protected float mPosY = 0.0f;
	
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
		mPosX = mTypedArray.getFloat(R.styleable.LECmdsStyleDef_posX, 0.0f);
		mPosY = mTypedArray.getFloat(R.styleable.LECmdsStyleDef_posY, 0.0f);
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
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
	
		float destX = (float)(((View) (getParent())).getWidth()) * mPosX;
		float destY = (float)(((View) (getParent())).getHeight()) * mPosY;
		setX(destX);
		setY(destY);
		
		super.onWindowFocusChanged(hasFocus);
	}
}
