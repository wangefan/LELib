package com.BLE.Buttons;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;
import com.LELib.R;

public class BLEButton2State extends BLEWriteCmdButton {

	//data members
	private RadioButton mCurCheckedRadBtn = null;
	private RadioButton [] mRadBtns;
	private TextView    mTvTitle;
	
	//constructors
	public BLEButton2State(Context context) {
		super(context);
	}

	public BLEButton2State(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBLEButton2State();
	}
	
	public BLEButton2State(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        initBLEButton2State();
    }  
	
	//member functions
	private void initBLEButton2State() {
		
		final LayoutInflater layoutInflater =
	            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.blebutton2state, this, true);
		mRadBtns = new RadioButton[3];
		mRadBtns[0] = (RadioButton) findViewById(R.id.radioButton1);
		mRadBtns[1] = (RadioButton) findViewById(R.id.radioButton2);
		mTvTitle = (TextView) findViewById(R.id.tvTitle);
		
		for(int idxBtn = 0; idxBtn < 2; ++idxBtn)
		{
			final int idxBtnTemp = idxBtn;
			if(mRadBtns[idxBtn] != null)
			{
				mRadBtns[idxBtn].setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						doWriteCmdAndReadRsp(mCmdsColl.get(idxBtnTemp));
					}
				});
			}
		}
				
		String cmd1Title = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1CmdTitle);
        String cmd2Title = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState2CmdTitle);
        String cmd1 = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1Cmd);
        String cmd2 = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState2Cmd);
        String cmd1Res = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1CmdRes);
        String cmd2Res = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState2CmdRes);
        mTypedArray.recycle();
        mCmdsColl = new ArrayList<LECmd>();
        if(mCmdsColl != null)
        {
        	mCmdsColl.add(new LECmd(cmd1Title, cmd1, cmd1Res));
        	mCmdsColl.add(new LECmd(cmd2Title, cmd2, cmd2Res));
        }
        saveUIState();
	}

	@Override
	protected void saveUIState() {
		for(int idxBtn = 0; idxBtn < 2; ++idxBtn)
		{
			if(mRadBtns[idxBtn] != null && mRadBtns[idxBtn].isChecked())
			{
				mCurCheckedRadBtn = mRadBtns[idxBtn];
				String title = mCmdsColl.get(idxBtn).mCmdTitle;
				mTvTitle.setText(title);
				break;
			}
		}
	}

	@Override
	protected void restoreUIState() {
		for(int idxBtn = 0; idxBtn < 2; ++idxBtn)
		{
			if(mRadBtns[idxBtn] != null && mRadBtns[idxBtn].isChecked())
			{
				if(mRadBtns[idxBtn] == mCurCheckedRadBtn)
					return;
				mRadBtns[idxBtn].setChecked(false);
				break;
			}
		}
		if(mCurCheckedRadBtn != null)
		{
			mCurCheckedRadBtn.setChecked(true);
		}
	}
}
