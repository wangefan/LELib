package com.UI.LEDevice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.UI.LEDevice.AnimatedExpandableListView.AnimatedExpandableListAdapter;
import com.UI.font.FontelloTextView;
import com.utility.CmdProcObj;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends CustomTitleActivity 
{
	//constant 
	private final String mTAG = "MainActivity";
	private final static String ACTION_GROUP_READ_OK = "ACTION_GROUP_READ_OK";
	private final static String ACTION_GROUP_READ_OK_GETITEMID_KEY = "ACTION_GROUP_READ_OK_GETITEMID_KEY";
	private final static String ACTION_GROUP_READ_OK_GETGRP_STATUS_KEY = "ACTION_GROUP_READ_OK_GETGRP_STATUS_KEY";
	private final static String ACTION_GROUP_READ_FAIL = "ACTION_GROUP_READ_FAIL";
	private final static String ACTION_GROUP_READ_FAIL_KEY = "ACTION_GROUP_READ_FAIL_KEY";
	private final static String ACTION_SEND_CMD_OK = "ACTION_SEND_CMD_OK";
	private static final long SCAN_PERIOD = 5000; // Stops scanning after 8 seconds.
	//data member
	private AnimatedExpandableListView mListView;
	private ExampleAdapter mAdapter;
	private static Handler mUIHanlder = new Handler();
	private Handler mScanPeriodHandler = new Handler();
	private ArrayList<BLEDevice> mLeDevices = new ArrayList<BLEDevice>();
	private BLEDevice mPreDevice = null;
	private ChildReadAllItem mReadAllCmd = null;
	private ChildItem    mPreCmdToExecute = null;
	private Menu mMenu = null;
	private int mGoalReadCount = 0;
	private int mReadCount = 0; 
	
	//Inner classes
	BroadcastReceiver mBtnReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            
			if(BLEUtility.ACTION_CONNSTATE_DISCONNECTED.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsDisconn);
            	String message = intent.getStringExtra(BLEUtility.ACTION_CONNSTATE_DISCONNECTED_KEY);
				Toast.makeText(MainActivity.this, "Disconnected, cause = " + message, Toast.LENGTH_SHORT).show();
            	mPreCmdToExecute = null;
            	updateUIForConn();
                return;
            }
			else if(BLEUtility.ACTION_CONNSTATE_CONNECTING.equals(action))
			{
				UIUtility.showProgressDlg(MainActivity.this, true, R.string.prgsConnting);
			}
			else if(BLEUtility.ACTION_CONNSTATE_CONNECTED.equals(action))
			{
				UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsConnted);
				IntegralSetting.setDeviceName(mPreDevice.getDeviceName());
				IntegralSetting.setDeviceMACAddr(mPreDevice.getAddress());
				updateUIForConn();
				Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
				if(mPreCmdToExecute != null)
				{
					executeCmd(mPreCmdToExecute);
					mPreCmdToExecute = null;
				}
				else if(mReadAllCmd != null)
					mReadAllCmd.doIt();
			}
			else if(BLEUtility.ACTION_GET_LEDEVICE.equals(action))
			{
				BLEDevice integralDevice = (BLEDevice) intent.getSerializableExtra(BLEUtility.ACTION_GET_LEDEVICE_KEY);
				if(integralDevice != null)
				{
					mLeDevices.add(integralDevice);
					MyLog.d(mTAG, "Get le device , "+mLeDevices.size()+"=>[" + integralDevice.getAddress()+"]");
				}
			}
			//Blew are commands relaive 
			else if (BLEUtility.ACTION_SENCMD_BEGIN.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, R.string.prgsSendingCmd);
            }
			else if(ACTION_SEND_CMD_OK.equals(action))
			{
				UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsSedingCmdOK);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdOK, Toast.LENGTH_SHORT).show();
			}
            else if(BLEUtility.ACTION_SENCMD_OK.equals(action)) 
            {
            	int [] idArr = intent.getIntArrayExtra(ACTION_GROUP_READ_OK_GETITEMID_KEY);
            	String groupStatus = intent.getStringExtra(ACTION_GROUP_READ_OK_GETGRP_STATUS_KEY);
            	//update GroupItem first
            	GroupItem groupItem = mAdapter.getGroup(idArr[0]);
            	groupItem.mGroupResponse = groupStatus;
            	groupItem.unCheckAllWrtChild();
        		MyLog.d(mTAG, "Group item " + groupItem.mGroupTitle + " read OK.");
        		
            	//update ChildItem if needed
            	ChildWrtChkItem childItem = null;
            	if(idArr[1] >= 0 ) 
            		childItem = (ChildWrtChkItem) mAdapter.getChild(idArr[0], idArr[1]) ;
            	if(childItem != null)
            	{
            		childItem.mBIsChecked = true;
            		MyLog.d(mTAG, "ChildWrtItem " + childItem.mTitle + " update OK.");
            	}
            	
            	mAdapter.notifyDataSetChanged();
            	UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsSedingCmdOK);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdOK, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_FAIL.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsSedingCmdFail);
            	Toast.makeText(MainActivity.this, R.string.prgsSedingCmdFail, Toast.LENGTH_SHORT).show();
            }
            else if (BLEUtility.ACTION_SENCMD_READ.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, R.string.prgsSedingReadCmd);
            }
            else if(BLEUtility.ACTION_SENCMD_READ_CONTENT.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsSedingReadCmdOK);
            	String message = intent.getStringExtra(BLEUtility.ACTION_SENCMD_READ_CONTENT_KEY);
            	Toast.makeText(MainActivity.this, "read cmd ok, response = " + message, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_READ_FAIL.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsReadCmdFail);
            	Toast.makeText(MainActivity.this, R.string.prgsReadCmdFail, Toast.LENGTH_SHORT).show();
            }
            else if(ACTION_GROUP_READ_OK.equals(action))
            {
            	int [] idArr = intent.getIntArrayExtra(ACTION_GROUP_READ_OK_GETITEMID_KEY);
            	String groupStatus = intent.getStringExtra(ACTION_GROUP_READ_OK_GETGRP_STATUS_KEY);
            	//update GroupItem first
            	GroupItem groupItem = mAdapter.getGroup(idArr[0]);
            	groupItem.mGroupResponse = groupStatus;
            	groupItem.unCheckAllWrtChild();
        		MyLog.d(mTAG, "Group item " + groupItem.mGroupTitle + " read OK.");
        		
            	//update ChildItem if needed
        		ChildWrtChkItem childItem = null;
            	if(idArr[1] >= 0 ) 
            		childItem = (ChildWrtChkItem) mAdapter.getChild(idArr[0], idArr[1]) ;
            	if(childItem != null)
            	{
            		childItem.mBIsChecked = true;
            		MyLog.d(mTAG, "ChildWrtChkItem" + childItem.mTitle + " update OK.");
            	}
            	
            	mAdapter.notifyDataSetChanged();
            	if(mReadCount >= mGoalReadCount)
            	{
            		UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsReadConfigEnd);
            		Toast.makeText(MainActivity.this, R.string.prgsReadConfigEnd, Toast.LENGTH_SHORT).show();
            	}
            }
            else if(ACTION_GROUP_READ_FAIL.equals(action))
            {
            	int [] idArr = intent.getIntArrayExtra(ACTION_GROUP_READ_FAIL_KEY);
            	GroupItem groupItem =(GroupItem) mAdapter.getGroup(idArr[0]);
            	if(groupItem != null)
            	{
            		MyLog.d(mTAG, "Group item " + groupItem.mGroupTitle + " read fail.");
            		//Todo: log read fail cmd
            	}
            	mAdapter.notifyDataSetChanged();
            	if(mReadCount >= mGoalReadCount)
            	{
            		UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsReadConfigEnd);
            		Toast.makeText(MainActivity.this, R.string.prgsReadConfigEnd, Toast.LENGTH_SHORT).show();
            	}
            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
				if (state == BluetoothAdapter.STATE_OFF) 
				{
					BLEUtility.getInstance().disconnect();
				}
			}
		}
	};
	
	private class GroupItem {
		public int mID = -1;
		private String mTag = "GroupItem";
		public String mGroupTitle = "";
		public String mGroupResponse = "";
		public boolean mBIsOutofDate = false;
		public String mGroupIcon = "";
		List<ChildItem> items = new ArrayList<ChildItem>();
		
		public ChildReadItem getReadItem() {
			ChildReadItem readItem = null;
			for(ChildItem chdItem: items)
			{
				if(chdItem instanceof ChildReadItem)
					readItem = (ChildReadItem) chdItem;
			}
			return readItem;
		}
		
		public void unCheckAllWrtChild()
		{
			for(ChildItem childItem: items)
				if(childItem instanceof ChildWrtChkItem)
					((ChildWrtChkItem)childItem).mBIsChecked = false;
		}
	}
	
	private static Object mLockSequence = new Object();
	private static int mCurrentSeqn = -1;
	private class CanReadGroup extends GroupItem {
		private String mTag = "CanReadGroup";
		private int mNExeSequence = -1;
		private final int mNTry = 3;
	
		public void doReadRsp()
		{
			ChildReadItem readItem = getReadItem(); 
			
			if(readItem != null)
			{
				final ChildReadItem chdReadItemTemp = readItem;
				final List<ReadCmdStructur> resCollTemp = chdReadItemTemp.mCommandResColl;
				Thread workerThread = new Thread() 
				{
				    public void run() {
			    	synchronized(mLockSequence) {
			    		while(mNExeSequence != mReadCount)
			    		{
			    			try {
								mLockSequence.wait();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    		}
			    		for(int idxNTry = 0; idxNTry < mNTry; ++ idxNTry)
			    		{
			    			MyLog.d(mTag, "doReadRsp, sequ = " + mNExeSequence + ", try "+( idxNTry+1) +"times, begin in synchronized(mLockSequence), in thread = " + Thread.currentThread().getId());
					    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(chdReadItemTemp.mCommand, false));
					    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
					    	String strRspCal = "";
					    	if(rspCal != null)
					    		strRspCal = new String(rspCal);
					    	MyLog.d(mTag, "doReadRsp, read content = " + strRspCal + ", in thread = " + Thread.currentThread().getId());
					    	
					    	for(final ReadCmdStructur rdCmdStr: resCollTemp)
					    	{
					    		String itrRsp = rdCmdStr.mResponseString;
					    		MyLog.d(mTag, "doReadRsp, compare from = " + itrRsp+ ", in thread = " + Thread.currentThread().getId());
					    		if(strRspCal.equals(itrRsp) == true)
								{
					    			++mReadCount;
					    			mBIsOutofDate= false; 
									MyLog.d(mTag, "doReadRsp, read ok, post ACTION_GROUP_READ_OK to UI, in thread = " + Thread.currentThread().getId());
									mUIHanlder.post(new Runnable() {
										@Override
										public void run() {
											final Intent brd = new Intent(ACTION_GROUP_READ_OK);
											brd.putExtra(ACTION_GROUP_READ_OK_GETITEMID_KEY, new int[] {mID, Integer.parseInt(rdCmdStr.mRefWrtCmdID)});
											brd.putExtra(ACTION_GROUP_READ_OK_GETGRP_STATUS_KEY, rdCmdStr.mResponseTitleString);
									        MainActivity.this.sendBroadcast(brd);
										}
									});
									mLockSequence.notifyAll();
									return;
								}
					    	}
					    	try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			    		}
			    		
			    		++mReadCount;
			    		mBIsOutofDate= true; 
				    	MyLog.d(mTag, "doReadRsp, read fail, post ACTION_GROUP_READ_FAIL to UI, in thread = " + Thread.currentThread().getId());
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								final Intent brd = new Intent(ACTION_GROUP_READ_FAIL);
								brd.putExtra(ACTION_GROUP_READ_FAIL_KEY, new int[] {mID, -1});
								MainActivity.this.sendBroadcast(brd);
							}
						});
						mLockSequence.notifyAll();	
			    	}
				    }
				};
				workerThread.start();
			}
			else
				MyLog.d(mTag, "doReadRsp, GroupItem has no read item");
		}
	}
	
	public abstract class ChildItem {
		public GroupItem mParentItem =  null;
		public int mID = -1;
		public String mTitle;
		public String mCommand;
		
		protected void broadCastAction(String action)
		{
			final Intent brd = new Intent(action);
	        MainActivity.this.sendBroadcast(brd);
		}
		
		protected void broadCastActionMsg(String action, String key, String message)
		{
			final Intent brd = new Intent(action);
			brd.putExtra(key, message);
	        MainActivity.this.sendBroadcast(brd);
		}	
	}
	
	private class ChildWrtChkItem extends ChildItem  {
		private String mTag = "ChildWrtChkItem";
		public boolean mBIsChecked = false;
		public String mCommandRes = "";
		
		public void doWriteCmdAndReadRsp()
		{
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			broadCastAction(BLEUtility.ACTION_SENCMD_BEGIN);
			
			Thread workerThread = new Thread() {
			    public void run() {
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + mCommand);
			    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(mCommand, true));
			    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
			    	String strRspCal = "";
			    	if(rspCal != null)
			    		strRspCal = new String(rspCal);
					if(strRspCal.equals(mCommandRes) == true)
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
						ChildReadItem readItem = mParentItem.getReadItem();
						ReadCmdStructur readcmdStr = null;
						if(readItem != null)
						{
							for(ReadCmdStructur readcmdStrItr: readItem.mCommandResColl)
							{
								if(Integer.parseInt(readcmdStrItr.mRefWrtCmdID) == mID)
								{
									readcmdStr = readcmdStrItr;
									break;
								}
							}
						}
						final ReadCmdStructur readcmdStrTemp = readcmdStr;
						
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								mParentItem.mBIsOutofDate= false; 
								final Intent brd = new Intent(BLEUtility.ACTION_SENCMD_OK);
								brd.putExtra(ACTION_GROUP_READ_OK_GETITEMID_KEY, new int[] {mParentItem.mID, mID});
								if(readcmdStrTemp != null)
									brd.putExtra(ACTION_GROUP_READ_OK_GETGRP_STATUS_KEY, readcmdStrTemp.mResponseTitleString);
						        MainActivity.this.sendBroadcast(brd);
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
	}

	private class ChildWrtItem extends ChildWrtChkItem  {
		private String mTag = "ChildWrtItem";
		public String mCheckedIcon = "";
		public String mUnCheckedIcon = "";
		
		public String getIcon() {
			if(mBIsChecked)
				return mCheckedIcon;
			else
				return mUnCheckedIcon;
		}
	}

	public class ReadCmdStructur {
		public String mResponseString = "";
		public String mResponseTitleString = "";
		public String mRefWrtCmdID = "";
		public ReadCmdStructur(String responseString, String responseTitleString, String refWrtCmdID) {
			mResponseString = new String(responseString);
			mResponseTitleString = new String(responseTitleString);
			mRefWrtCmdID = new String(refWrtCmdID);
		}
	}
	
	public class ChildReadItem extends ChildItem implements Serializable{
		private static final long serialVersionUID = 4L;
		private String mTag = "ChildReadItem";
		public String mIcon = "";
		
		
		public List<ReadCmdStructur> mCommandResColl = new ArrayList<ReadCmdStructur>();
		
		public void doWriteCmdAndReadRsp(boolean bBroadCast)
		{
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			if(bBroadCast)
				broadCastAction(BLEUtility.ACTION_SENCMD_READ);
			final boolean bBroadCastTemp = bBroadCast;
			
			Thread workerThread = new Thread() {
			    public void run() {
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + mCommand);
			    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(mCommand, false));
			    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
			    	String strRsp = "", strRspHex = "", strRspCal = "", strRspCalHex = "";
			    	if(rsp != null)
			    	{
			    		strRsp = new String(rsp);
			    		strRspHex = String.format("%x", new BigInteger(1, rsp));
			    	}
			    	if(rspCal != null)
			    	{
			    		strRspCal = new String(rspCal);
			    		strRspCalHex = String.format("%x", new BigInteger(1, rspCal));
			    	}
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, read from integral = " + strRsp);
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, read from integral hex = " + strRspHex);
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, read after CRC = " + strRspCal);
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, read after CRC hex = " + strRspCalHex);
			    	for(final ReadCmdStructur itrRsp: mCommandResColl)
			    	{
			    		MyLog.d(mTag, "doWriteCmdAndReadRsp, compare from = " + itrRsp);
			    		if(strRspCal.equals(itrRsp.mResponseString) == true)
						{
							MyLog.d(mTag, "doWriteCmdAndReadRsp, read ok");
							mUIHanlder.post(new Runnable() {
								@Override
								public void run() {
									if(bBroadCastTemp)
										broadCastActionMsg(BLEUtility.ACTION_SENCMD_READ_CONTENT, BLEUtility.ACTION_SENCMD_READ_CONTENT_KEY, itrRsp.mResponseString);
								}
							});
							return;
						}
			    	}
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, read fail");
					mUIHanlder.post(new Runnable() {
						@Override
						public void run() {
							if(bBroadCastTemp)
								broadCastAction(BLEUtility.ACTION_SENCMD_READ_FAIL);
						}
					});
			    }
			};
			workerThread.start();
		}
	}
	
	private class ChildReadAllItem extends ChildItem implements Serializable{
		private static final long serialVersionUID = 5L;
		private String mTag = "ChildReadAllItem";
		public String mIcon = "";
		
		public void doIt()
		{
			MyLog.d(mTag, "Read config begin");
			UIUtility.showProgressDlg(MainActivity.this, true, R.string.prgsReadConfig);
			mReadCount = 0;
			for(int idxGroup = 0; idxGroup < mAdapter.getGroupCount(); ++idxGroup)
			{
				GroupItem groupItem = mAdapter.getGroup(idxGroup);
				if(groupItem instanceof CanReadGroup)
				{
					((CanReadGroup)groupItem).doReadRsp();
				}
			}
		}
	}
	
	private class ChildWrtCECItem extends ChildItem {
		private String mTag = "ChildWrtCECItem";
		public String mIcon = "";
		public String mCommandRes = "";
		
		public void doWriteCmdAndReadRsp()
		{
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			broadCastAction(BLEUtility.ACTION_SENCMD_BEGIN);
			
			Thread workerThread = new Thread() {
			    public void run() {
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + mCommand);
			    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(mCommand, true));
			    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
			    	String strRspCal = "";
			    	if(rspCal != null)
			    		strRspCal = new String(rspCal);
					if(strRspCal.equals(mCommandRes) == true)
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
						
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								mParentItem.mBIsOutofDate= false; 
								final Intent brd = new Intent(ACTION_SEND_CMD_OK);
						        MainActivity.this.sendBroadcast(brd);
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
		
		public void doIt()
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
			builder.setTitle(R.string.InputDlgTitle);

			// Set up the input
			final EditText input = new EditText(MainActivity.this);
			input.setText(R.string.InputDlgDefaultCEC);
			// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			builder.setView(input);

			// Set up the buttons
			builder.setPositiveButton(R.string.InputDlgOK, new DialogInterface.OnClickListener() { 
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			    	mCommand = input.getText().toString();
			    	mTitle = String.format(MainActivity.this.getResources().getString(R.string.InputDlgCECFormatTitle), mCommand);
			    	doWriteCmdAndReadRsp();
			    }
			});
			builder.setNegativeButton(R.string.InputDlgCan, new DialogInterface.OnClickListener() {
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			        dialog.cancel();
			    }
			});

			builder.show();
		}
	}
	private static class ChildHolder {
		TextView mTitle;
		FontelloTextView  mIcon;
		CheckBox  mCheckBox;
	}

	private static class GroupHolder {
		TextView mGroupTitle;
		TextView mGroupRespStatus;
		FontelloTextView  mGroupIcon;
	}

	/**
	 * Adapter for our list of {@link GroupItem}s.
	 */
	private class ExampleAdapter extends AnimatedExpandableListAdapter {
		private LayoutInflater inflater;
		private ArrayList<ArrayList<Integer>> mHhiddenPositionsColl = new ArrayList<ArrayList<Integer>>();  //[group, list hidden index]

		private List<GroupItem> mCollItems;

		public ExampleAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void setData(List<GroupItem> items) {
			mCollItems = items;
			for(int idxGrp = 0; idxGrp < mCollItems.size(); ++idxGrp)
			{
				GroupItem grpItem = mCollItems.get(idxGrp);
				if(grpItem != null)
				{
					ArrayList<Integer> hiddenChdsIndex = new ArrayList<Integer>();
					mHhiddenPositionsColl.add(hiddenChdsIndex);
					for(int idxChd = 0; idxChd < grpItem.items.size(); ++idxChd)
					{
						ChildItem chdItem = grpItem.items.get(idxChd);
						if(chdItem != null && chdItem instanceof ChildReadItem)
							hiddenChdsIndex.add(idxChd);
					}
				}
			}
		}

		@Override
		public ChildItem getChild(int groupPosition, int childPosition) {
			return mCollItems.get(groupPosition).items.get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getRealChildView(int groupPosition, int childPosition,
				boolean isLastChild, View convertView, ViewGroup parent) {
			
			ArrayList<Integer> hiddenPositions = mHhiddenPositionsColl.get(groupPosition);
			for(Integer hiddenIndex : hiddenPositions) {
	            if(hiddenIndex <= childPosition) {
	            	childPosition = childPosition + 1;
	            }
	            else
	            	break;
	        }
			
			ChildHolder chdholder = new ChildHolder();
			ChildItem item = getChild(groupPosition, childPosition);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item, parent, false);
				chdholder.mTitle = (TextView) convertView.findViewById(R.id.textTitle);
				chdholder.mIcon = (FontelloTextView) convertView.findViewById(R.id.lstChildItemIcon);
				chdholder.mCheckBox = (CheckBox) convertView.findViewById(R.id.chkWt);
				chdholder.mCheckBox.setEnabled(false);
				convertView.setTag(chdholder);
			} 
			
			chdholder = (ChildHolder) convertView.getTag();
			chdholder.mTitle.setText(item.mTitle);
			if(item instanceof ChildWrtItem)
			{
				chdholder.mIcon.setVisibility(View.VISIBLE);
				chdholder.mIcon.setText(((ChildWrtItem)item).getIcon());
				chdholder.mCheckBox.setVisibility(View.INVISIBLE);
			}
			else if(item instanceof ChildWrtChkItem)
			{
				chdholder.mIcon.setVisibility(View.INVISIBLE);
				chdholder.mCheckBox.setChecked(((ChildWrtChkItem) item).mBIsChecked);
				chdholder.mCheckBox.setVisibility(View.VISIBLE);
			}
			else if(item instanceof ChildWrtCECItem)
			{
				chdholder.mIcon.setVisibility(View.VISIBLE);
				chdholder.mIcon.setText(((ChildWrtCECItem)item).mIcon);
				chdholder.mCheckBox.setVisibility(View.INVISIBLE);
			}
			else if(item instanceof ChildReadAllItem)
			{
				chdholder.mIcon.setVisibility(View.VISIBLE);
				chdholder.mIcon.setText(((ChildReadAllItem)item).mIcon);
				chdholder.mCheckBox.setVisibility(View.INVISIBLE);
			}
				
			return convertView;
		}

		@Override
		public int getRealChildrenCount(int groupPosition) {
			GroupItem grpItem = mCollItems.get(groupPosition);
			int nTotalChdCount = grpItem.items.size();
			int nHiddenCount = mHhiddenPositionsColl.get(groupPosition).size();
			return nTotalChdCount - nHiddenCount;
		}

		@Override
		public GroupItem getGroup(int groupPosition) {
			return mCollItems.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return mCollItems.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
				View convertView, ViewGroup parent) {
			GroupHolder holder;
			GroupItem item = getGroup(groupPosition);
			if (convertView == null) {
				holder = new GroupHolder();
				convertView = inflater.inflate(R.layout.group_item, parent, false);
				holder.mGroupTitle = (TextView) convertView.findViewById(R.id.textTitle);
				holder.mGroupRespStatus = (TextView) convertView.findViewById(R.id.textRespStatus);
				holder.mGroupIcon = (FontelloTextView) convertView.findViewById(R.id.lstGroupItemIcon);
				convertView.setTag(holder);
			} else {
				holder = (GroupHolder) convertView.getTag();
			}

			holder.mGroupTitle.setText(item.mGroupTitle);
			if(item.mBIsOutofDate)
			{
				holder.mGroupRespStatus.setTextColor(getResources().getColor(R.color.material_red_200));
				holder.mGroupRespStatus.setText(getResources().getString(R.string.groupOutofDate));
			}
			else
			{
				holder.mGroupRespStatus.setTextColor(getResources().getColor(R.color.custom_green_color));
				holder.mGroupRespStatus.setText(item.mGroupResponse);
			}
			holder.mGroupIcon.setText(item.mGroupIcon);

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int arg0, int arg1) {
			return true;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setTitle("Low Energy Devices");
		 
		registerReceiver(mBtnReceiver, makeServiceActionsIntentFilter());	
		setContentView(R.layout.activity_expandable_list_view);
		
		String devName = getIntent().getStringExtra("DeviceName");
		String devAddr = getIntent().getStringExtra("DeviceAddr");
		if(devName.length() > 0 && devAddr.length() > 0)
		{
			IntegralSetting.setDeviceName(devName);
			IntegralSetting.setDeviceMACAddr(devAddr);
		}

		//parsing XML
		List<GroupItem> groupItems = new ArrayList<GroupItem>();
		mGoalReadCount = 0;
		XPath xpath = XPathFactory.newInstance().newXPath();  
		String expression = "//CmdGroup";  
		
		File path = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_DOWNLOADS);
				
	    File file = new File(path, "Commands.xml");
		InputSource inputSource = null;
		if(file.exists())
		{
			try {
				inputSource = new InputSource(new FileInputStream(file));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			try {
				inputSource = new InputSource(getAssets().open("Commands.xml"));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}  
		}
		
		NodeList nodes = null;
		try {
			nodes = (NodeList) xpath.evaluate(expression, inputSource, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int idxCmdGroup = 0; idxCmdGroup<nodes.getLength(); ++idxCmdGroup) {  
		    Node cmdGroupNode = nodes.item(idxCmdGroup);  
		    NamedNodeMap attributes = cmdGroupNode.getAttributes();  
		    String isCanReadGroup = attributes.getNamedItem("CanRead").getNodeValue();
		    GroupItem cmdgroup = null;
		    if(isCanReadGroup.equals("false"))
		    	cmdgroup = new GroupItem();
		    else
		    {
		    	cmdgroup = new CanReadGroup();
		    	((CanReadGroup)cmdgroup).mNExeSequence = Integer.parseInt(attributes.getNamedItem("Sequence").getNodeValue());
		    	++mGoalReadCount;
		    }
		    cmdgroup.mID = idxCmdGroup;
		    cmdgroup.mGroupTitle = attributes.getNamedItem("Title").getNodeValue();
		    cmdgroup.mGroupIcon = attributes.getNamedItem("Icon").getNodeValue();
		    groupItems.add(cmdgroup);
		    NodeList cmdsList = cmdGroupNode.getChildNodes();
		    if(cmdsList != null)
		    {
		    	int nID = 0;
		    	for(int idxCmd = 0; idxCmd < cmdsList.getLength(); ++idxCmd) {
		    		Node cmdNode = cmdsList.item(idxCmd);  
		    		String strNodeName = cmdNode.getLocalName();
		    		if(strNodeName == null)
		    			continue;
		    		ChildItem command = null;
		    		if(strNodeName.compareTo("WriteCmdChk") == 0)
		    		{
		    			command = new ChildWrtChkItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("Cmd").getNodeValue();
		    			for(int idxCmdRes = 0; idxCmdRes < cmdNode.getChildNodes().getLength(); ++idxCmdRes) { 
		    				Node cmdResNode = cmdNode.getChildNodes().item(idxCmdRes); 
				    		String strResNodeName = cmdResNode.getLocalName();
				    		if(strResNodeName == null)
				    			continue;
				    		if(strResNodeName.compareTo("CmdRes") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			((ChildWrtChkItem)command).mCommandRes = new String(strVal);
				    			break;
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    		}
		    		else if(strNodeName.compareTo("WriteCmd") == 0)
		    		{
		    			command = new ChildWrtItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("Cmd").getNodeValue();
		    			((ChildWrtItem)command).mCheckedIcon = cmdNode.getAttributes().getNamedItem("CheckedIcon").getNodeValue();
		    			((ChildWrtItem)command).mUnCheckedIcon = cmdNode.getAttributes().getNamedItem("UnCheckedIcon").getNodeValue();
		    			for(int idxCmdRes = 0; idxCmdRes < cmdNode.getChildNodes().getLength(); ++idxCmdRes) { 
		    				Node cmdResNode = cmdNode.getChildNodes().item(idxCmdRes); 
				    		String strResNodeName = cmdResNode.getLocalName();
				    		if(strResNodeName == null)
				    			continue;
				    		if(strResNodeName.compareTo("CmdRes") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			((ChildWrtItem)command).mCommandRes = new String(strVal);
				    			break;
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    		}
		    		else if(strNodeName.compareTo("WriteCECCmd") == 0)
		    		{
		    			command = new ChildWrtCECItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("Cmd").getNodeValue();
		    			((ChildWrtCECItem)command).mIcon = cmdNode.getAttributes().getNamedItem("Icon").getNodeValue();
		    			for(int idxCmdRes = 0; idxCmdRes < cmdNode.getChildNodes().getLength(); ++idxCmdRes) { 
		    				Node cmdResNode = cmdNode.getChildNodes().item(idxCmdRes); 
				    		String strResNodeName = cmdResNode.getLocalName();
				    		if(strResNodeName == null)
				    			continue;
				    		if(strResNodeName.compareTo("CmdRes") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			((ChildWrtCECItem)command).mCommandRes = new String(strVal);
				    			break;
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    		}
		    		else if(strNodeName.compareTo("ReadCmd") == 0)
		    		{
		    			command = new ChildReadItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("Cmd").getNodeValue();
		    			for(int idxCmdRes = 0; idxCmdRes < cmdNode.getChildNodes().getLength(); ++idxCmdRes) { 
				    		Node cmdResNode = cmdNode.getChildNodes().item(idxCmdRes); 
				    		String strResNodeName = cmdResNode.getLocalName();
				    		if(strResNodeName == null)
				    			continue;
				    		if(strResNodeName.compareTo("CmdRes") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			String reRespTitle = cmdResNode.getAttributes().getNamedItem("Title").getNodeValue();
				    			String refWrtCmdID = cmdResNode.getAttributes().getNamedItem("refWmdID").getNodeValue();
				    			ReadCmdStructur rs = new ReadCmdStructur(strVal, reRespTitle, refWrtCmdID);
				    			((ChildReadItem)command).mCommandResColl.add(rs);
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    		}
	    			else if(strNodeName.compareTo("ReadAllCmd") == 0)
		    		{
		    			command = new ChildReadAllItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("Cmd").getNodeValue();
		    			((ChildReadAllItem)command).mIcon = cmdNode.getAttributes().getNamedItem("Icon").getNodeValue();
		    			cmdgroup.items.add(command);
		    			mReadAllCmd = (ChildReadAllItem) command;
		    		}
		    	}
		    }
		    
		    
		}
		//parsing XML end
		mAdapter = new ExampleAdapter(this);
		mAdapter.setData(groupItems);

		mListView = (AnimatedExpandableListView) findViewById(R.id.list_view);
		mListView.setAdapter(mAdapter);

		// In order to show animations, we need to use a custom click handler
		// for our ExpandableListView.
		mListView.setOnGroupClickListener(new OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
				// We call collapseGroupWithAnimation(int) and
				// expandGroupWithAnimation(int) to animate group
				// expansion/collapse.
				if (mListView.isGroupExpanded(groupPosition)) {
					mListView.collapseGroupWithAnimation(groupPosition);
				} else {
					mListView.expandGroupWithAnimation(groupPosition);
				}
				return true;
			}

		});
		
		mListView.setOnChildClickListener(new OnChildClickListener() {

			@Override
			public boolean onChildClick(ExpandableListView parent, View v,
					int groupPosition, int childPosition, long id) {
				
				ChildItem childItem = mAdapter.getChild(groupPosition, childPosition);
				if(needRequestBT() == true)
				{
					MainActivity.this.mPreCmdToExecute = childItem;
					return false;
				}
				else if(BLEUtility.getInstance().isConnect() == false)
				{
					MainActivity.this.mPreCmdToExecute = childItem;
					MainActivity.this.connectToIntegral();
					return false;
				}
				
				return executeCmd(childItem);
			}
			
		});

		// Set indicator (arrow) to the right
		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		int width = size.x;
		//Log.v("width", width + "");
		Resources r = getResources();
		int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
				50, r.getDisplayMetrics());
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
			mListView.setIndicatorBounds(width - px, width);
		} else {
			mListView.setIndicatorBoundsRelative(width - px, width);
		}
		
		requestBTOrConn();
	}
	
	private static IntentFilter makeServiceActionsIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_BEGIN);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_OK);
        intentFilter.addAction(ACTION_SEND_CMD_OK);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ_CONTENT);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_CONNECTING);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_CONNECTED);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_DISCONNECTED);
        intentFilter.addAction(BLEUtility.ACTION_GET_LEDEVICE);
        intentFilter.addAction(ACTION_GROUP_READ_OK);
        intentFilter.addAction(ACTION_GROUP_READ_FAIL);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }
	
	private void updateUIForConn()
	{
		if(BLEUtility.getInstance().isConnect())
		{
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(getResources().getString(R.string.menu_disconn));
		}
		else 
		{
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(getResources().getString(R.string.menu_conn));
		}
	}
	
	private boolean needRequestBT() {
		if((BluetoothAdapter.getDefaultAdapter() == null || BluetoothAdapter.getDefaultAdapter().isEnabled() == false))
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, BTSettingActivity.REQUEST_ENABLE_BT);
	        return true;
		}	
		return false;
	}
	
	private void requestBTOrConn() {
		if(needRequestBT() == true)
			return;
		connectToIntegral();
	}
	
	private boolean executeCmd(ChildItem childItem){
		if(childItem != null)
		{
			if(true == (childItem instanceof ChildWrtItem))
			{
				((ChildWrtItem)childItem).doWriteCmdAndReadRsp();
				return true;
			}
			else if(true == (childItem instanceof ChildWrtChkItem))
			{
				((ChildWrtChkItem)childItem).doWriteCmdAndReadRsp();
				return true;
			}
			else if(true == (childItem instanceof ChildReadItem))
			{
				((ChildReadItem)childItem).doWriteCmdAndReadRsp(true);
				return true;
			}
			else if(true == (childItem instanceof ChildReadAllItem))
			{
				((ChildReadAllItem)childItem).doIt();
				return true;
			}
			else if(true == (childItem instanceof ChildWrtCECItem))
			{
				((ChildWrtCECItem)childItem).doIt();
				return true;
			}
		}
		return false;
	}
	
	private void connectToIntegral(){
		if(BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled() == true)
		{
			if(IntegralSetting.getDeviceMACAddr().length() <= 0)
			{
				UIUtility.showProgressDlg(MainActivity.this, true, R.string.prgsScanDev);
				mLeDevices.clear();
				BLEUtility.getInstance().startScanLEDevices();
				mScanPeriodHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    BLEUtility.getInstance().stopScanLEDevices();
	                    UIUtility.showProgressDlg(MainActivity.this, false, R.string.prgsScanNoDev);
	                    if(mLeDevices.size() == 0)
	                    {
	                    	Toast.makeText(MainActivity.this, R.string.prgsScanNoDev, Toast.LENGTH_SHORT).show();
	                    }
	                    else if(mLeDevices.size() == 1)
	                    {
	                    	BLEDevice device = mLeDevices.get(0);
	                    	if(device != null)
	                    	{
	                    		mPreDevice = device;
	                    		BLEUtility.getInstance().connect(device.getAddress());
	                    	}
	                    }
	                    else //mLeDevices.size() > 1
	                    {
	                    	AlertDialog.Builder builderSingle = new AlertDialog.Builder(
	                                MainActivity.this);
	                        builderSingle.setIcon(R.drawable.ic_icon);
	                        builderSingle.setTitle(getResources().getString(R.string.dlgChooseIntegral));
	                        builderSingle.setNegativeButton(getResources().getString(R.string.dlgCancel),
	                                new DialogInterface.OnClickListener() {

	                                    @Override
	                                    public void onClick(DialogInterface dialog, int which) {
	                                        dialog.dismiss();
	                                    }
	                                });
	                        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
	                        		MainActivity.this,
	                                android.R.layout.select_dialog_singlechoice);
	                        for(BLEDevice leDevice: mLeDevices)
	                        	arrayAdapter.add(leDevice.getDeviceName() + " [" + leDevice.getAddress() + "]");

	                        builderSingle.setAdapter(arrayAdapter,
	                                new DialogInterface.OnClickListener() {

	                                    @Override
	                                    public void onClick(DialogInterface dialog, int which) {
	                                        BLEDevice device = mLeDevices.get(which);
	                                        mPreDevice = device;
	                                        BLEUtility.getInstance().connect(device.getAddress());
	                                    }
	                                });
	                        builderSingle.show();
	                    }
	                }
	            }, SCAN_PERIOD);
			}
			else
			{
				mPreDevice = new BLEDevice(IntegralSetting.getDeviceName(), IntegralSetting.getDeviceMACAddr());
				BLEUtility.getInstance().connect(IntegralSetting.getDeviceMACAddr());
			}
		}	
	}
	
	@Override
    protected void onResume() {
		updateUIForConn();
		super.onResume();
	}
	
	@Override
    protected void onPause() {
        super.onPause();
    }
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mBtnReceiver);
		BLEUtility.getInstance().disconnect();
		super.onDestroy();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
       	case BTSettingActivity.REQUEST_ENABLE_BT:
       	{
       		if(resultCode == Activity.RESULT_OK ) 
        	{
       			connectToIntegral();
        	}
       		else if(resultCode == Activity.RESULT_CANCELED)
       		{
       			mPreCmdToExecute = null;
       		}
       	}
       	break;
       }
        super.onActivityResult(requestCode, resultCode, data);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.menu_connect).setVisible(true);
		mMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
        
        case android.R.id.home:
        	BLEUtility.getInstance().disconnect();
        	break;
        case R.id.menu_connect:
        {
        	String tle = (String) item.getTitle(); 
        	if(tle.compareTo(getResources().getString(R.string.menu_disconn)) == 0)
        		BLEUtility.getInstance().disconnect();
        	else if(tle.compareTo(getResources().getString(R.string.menu_conn)) == 0)
        		requestBTOrConn();
        }
        break;
        case R.id.menu_clearConn:
        {
        	IntegralSetting.setDeviceMACAddr("");
        	Toast.makeText(MainActivity.this, R.string.msgResetConn, Toast.LENGTH_SHORT).show();
        }
        break;
        default:	
		}	
		return super.onOptionsItemSelected(item);
	}

	@Override
	String getCustTitle() {
		return getResources().getString(R.string.strMainActivity);
	}
}