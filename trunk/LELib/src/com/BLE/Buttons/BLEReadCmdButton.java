package com.BLE.Buttons;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.BLE.Buttons.BLEButton.LECmd;
import com.LELib.R;

public class BLEReadCmdButton extends BLEButton{
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
					
				}
			});
		}
	}
}
