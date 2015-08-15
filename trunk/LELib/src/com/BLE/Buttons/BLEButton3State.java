package com.BLE.Buttons;

import java.util.ArrayList;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.BLEUtilityException;
import com.LELib.R;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;


public class BLEButton3State extends BLEButton {
	//data members
	private ArrayList<LECmd> mCmdsColl;
	private String mCurStateCmd = "";
	private RadioButton mRadBtn1;
	private RadioButton mRadBtn2;
	private RadioButton mRadBtn3;
	
	//constructors
	public BLEButton3State(Context context) {
		super(context);
	}

	public BLEButton3State(Context context, AttributeSet attrs) {
		super(context, attrs);
		initBLEButton3State();
	}
	
	public BLEButton3State(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        initBLEButton3State();
    }  
	
	//member functions
	private void initBLEButton3State() {
		
		final LayoutInflater layoutInflater =
	            (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		layoutInflater.inflate(R.layout.blebutton3state, this, true);
		mRadBtn1 = (RadioButton) findViewById(R.id.radioButton1);
		mRadBtn2 = (RadioButton) findViewById(R.id.radioButton2);
		mRadBtn3 = (RadioButton) findViewById(R.id.radioButton3);
		
		if(mRadBtn1 != null)
		{
			mRadBtn1.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					
					String cmd = mCmdsColl.get(0).mCmd;
					try {
						BLEUtility.getInstance(getContext()).write(cmd);
					} catch (BLEUtilityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		}
		
		if(mRadBtn2 != null)
		{
			mRadBtn2.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String cmd = mCmdsColl.get(1).mCmd;
					try {
						BLEUtility.getInstance(getContext()).write(cmd);
					} catch (BLEUtilityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
		}
		
		if(mRadBtn3 != null)
		{
			mRadBtn3.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					String cmd = mCmdsColl.get(2).mCmd;
					try {
						BLEUtility.getInstance(getContext()).write(cmd);
					} catch (BLEUtilityException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
			});
		}
		
		String cmd1Title = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1CmdTitle);
        String cmd2Title = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState2CmdTitle);
        String cmd3Title = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState3CmdTitle);
        String cmd1 = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1Cmd);
        String cmd2 = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState2Cmd);
        String cmd3 = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState3Cmd);
        String cmd1Res = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState1CmdRes);
        String cmd2Res = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState2CmdRes);
        String cmd3Res = mTypedArray.getString(R.styleable.LECmdsStyleDef_LEBtnState3CmdRes);
        mTypedArray.recycle();
        mCmdsColl = new ArrayList<LECmd>();
        if(mCmdsColl != null)
        {
        	mCmdsColl.add(new LECmd(cmd1Title, cmd1, cmd1Res));
        	mCmdsColl.add(new LECmd(cmd2Title, cmd2, cmd2Res));
        	mCmdsColl.add(new LECmd(cmd3Title, cmd3, cmd3Res));
        }
	}

	@Override
	public String toggleNextState() {
		/*String [] cmds = (String[]) mCmdsColl.keySet().toArray();
		int idxGoal = 0;
		for(int idxCmd = 0; idxCmd < cmds.length; ++idxCmd)
		{
			if(cmds[idxCmd].compareTo(mCurStateCmd) == 0) {
				idxGoal = idxCmd;
				break;
			}
		}
		idxGoal += 1;
		idxGoal = (idxGoal >= cmds.length? 0 : idxGoal);*/
		return "";//cmds[idxGoal];
	}
}
