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
import com.BLE.BLEUtility.BLEUtility;
import com.BLE.BLEUtility.MyLog;
import com.UI.LEDevice.AnimatedExpandableListView.AnimatedExpandableListAdapter;
import com.UI.font.FontelloTextView;
import com.utility.CmdProcObj;
import android.app.AlertDialog;
import android.support.v4.app.Fragment;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;

public class ExpandaListActivity extends Fragment 
{
	//constant 
	private final String mTAG = "ExpandaListActivity";
	public final static String ACTION_UPDATELIST = "com.UI.LEDevice.UPDATELIST";
	
	//data member
	private MainActivity  mFaActivity = null;
	private View  mIntegralView = null;
	private AnimatedExpandableListView mListView;
	private ExampleAdapter mAdapter;
	private static Handler mUIHanlder = new Handler();	
	private ArrayList<ChildWrtReadItem> mListWrtReadCmds = new ArrayList<ChildWrtReadItem>();
	private ChildReadAllItem mReadAllCmd = null;
	public  ChildItem    mPreCmdToExecute = null;
	private int mGoalReadCount = 0;
	final private int mReadFailGoalCount = 2;
	private int mReadCount = 0; 
	private int mReadFailCount = 0; 
	private File mExternalXMLFile = null; 
	private static Object mLockPollRead = new Object();
	
	private static IntentFilter makeMsgIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_UPDATELIST);
        return intentFilter;
    }
	
	//Inner classes
	BroadcastReceiver mMsgeceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if(ACTION_UPDATELIST.equals(action))
            {
				if(mAdapter != null)
					mAdapter.notifyDataSetChanged();
            }
		}
	};

	private class GroupItem {
		public int mID = -1;
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
	private class CanReadGroup extends GroupItem {
		private String mTag = "CanReadGroup";
		private int mNExeSequence = -1;
		private int mNTry = 1;
		
		private void doCheckReadItems() {
			if(mReadCount >= mGoalReadCount || mReadFailCount >= mReadFailGoalCount)
	    	{
				mUIHanlder.post(new Runnable() {
					@Override
					public void run() {
						broadCastAction(BLEUtility.ACTION_ITEM_READ_END);
					}
				});
				mReadCount = 0;
				mReadFailCount = 0;
	    	}	
		}
	
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
					    			mBIsOutofDate = false; 
					    			//update GroupItem first
					            	mGroupResponse = rdCmdStr.mResponseTitleString;
					            	unCheckAllWrtChild();
					        		MyLog.d(mTag, "Group item " + mGroupTitle + " read OK.");
					        		
					            	//update ChildItem if needed
					        		ChildWrtChkItem childItem = null;
					        		if(Integer.parseInt(rdCmdStr.mRefWrtCmdID) >= 0 && mID >= 0)
					        			childItem = (ChildWrtChkItem) mAdapter.getChild(mID, Integer.parseInt(rdCmdStr.mRefWrtCmdID)) ;
					            	if(childItem != null)
					            	{
					            		childItem.mBIsChecked = true;
					            		MyLog.d(mTAG, "ChildWrtChkItem" + childItem.mTitle + " update OK.");
					            	}
					            	
					            	
									MyLog.d(mTag, "doReadRsp, read ok, post ACTION_ITEM_READ_UPDATE to UI, in thread = " + Thread.currentThread().getId());
									mUIHanlder.post(new Runnable() {
										@Override
										public void run() {
											broadCastAction(BLEUtility.ACTION_ITEM_READ_UPDATE);
										}
									});
									
									doCheckReadItems();
									
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
			    		++mReadFailCount;
			    		++mReadCount;
			    		mBIsOutofDate= true; 
				    	MyLog.d(mTag, "doReadRsp, read fail, post ACTION_ITEM_READ_UPDATE to UI, in thread = " + Thread.currentThread().getId());
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								broadCastAction(BLEUtility.ACTION_ITEM_READ_UPDATE);
							}
						});
						
						doCheckReadItems();
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
		public String mCommand = "";
	}
	
	private class CECChildItem extends ChildItem {
		private String mTag = "CECChildItem";
		public  String [] mCECCmd = new String []{"", "", "", ""};
		public  String [] mCECCheckedIcon = new String []{"", "", "", ""};
		public  String [] mCECUnCheckedIcon = new String []{"", "", "", ""};
		public  String [] mCurIcon = new String []{"", "", "", ""};
		
		private void doWriteCmdAndReadRsp(final int nCEC) {
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			broadCastAction(BLEUtility.ACTION_SENCMD_BEGIN);
			
			Thread workerThread = new Thread() {
			    public void run() {
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write CEC cmd = " + mCECCmd[nCEC]);
			    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(mCECCmd[nCEC], true));
			    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
			    	String strRspCal = "";
			    	if(rspCal != null)
			    		strRspCal = new String(rspCal);
					if(strRspCal.equals("ok") == true)
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
						mCurIcon[nCEC] = mCECCheckedIcon[nCEC];
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
						mCurIcon[nCEC] = mCECUnCheckedIcon[nCEC];
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
		
		public void setCEC(final int nCEC, FontelloTextView ibCEC, MaterialRippleLayout cecLay, final boolean bClickable) {
			ibCEC.setText(mCurIcon[nCEC]);
			cecLay.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View arg0, MotionEvent arg1) {
					return !bClickable;
				}
			});
			
	    	ibCEC.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View arg0) {
					doWriteCmdAndReadRsp(nCEC);
				}
	    	});
		}
		
		public void setCEC1(FontelloTextView ibCEC1, MaterialRippleLayout cecLay1) {
			boolean bClickable = true;
			if(mCECUnCheckedIcon[0].length() == 0 || mCECCheckedIcon[0].length() == 0)
				bClickable = false;
			setCEC(0, ibCEC1, cecLay1, bClickable);
		}
		
		public void setCEC2(FontelloTextView ibCEC2, MaterialRippleLayout cecLay2) {
			boolean bClickable = true;
			if(mCECUnCheckedIcon[1].length() == 0 || mCECCheckedIcon[1].length() == 0)
				bClickable = false;
			setCEC(1, ibCEC2, cecLay2, bClickable);
		}
		
		public void setCEC3(FontelloTextView ibCEC3, MaterialRippleLayout cecLay3) {
			boolean bClickable = true;
			if(mCECUnCheckedIcon[2].length() == 0 || mCECCheckedIcon[2].length() == 0)
				bClickable = false;
			setCEC(2, ibCEC3, cecLay3, bClickable);
		}
		
		public void setCEC4(FontelloTextView ibCEC4, MaterialRippleLayout cecLay4) {
			boolean bClickable = true;
			if(mCECUnCheckedIcon[3].length() == 0 || mCECCheckedIcon[3].length() == 0)
				bClickable = false;
			setCEC(3, ibCEC4, cecLay4, bClickable);
		}
	}
	
	private class ChildWrtChkItem extends ChildItem  {
		private String mTag = "ChildWrtChkItem";
		public boolean mBIsChecked = false;
		public String mCommandResOK = "";
		public String mCommandResSWForce = "";
		
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
					if(strRspCal.equals(mCommandResOK) == true)
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
						
						if(readcmdStr != null) {
							//update GroupItem first
							mParentItem.mGroupResponse = (readcmdStr.mResponseTitleString == null) ? "" : readcmdStr.mResponseTitleString;
						}
						mParentItem.unCheckAllWrtChild();
		        		MyLog.d(mTag, "Group item " + mParentItem.mGroupTitle + " read OK.");
		        		
						mParentItem.mBIsOutofDate= false; 
						mBIsChecked = true;
		        		MyLog.d(mTag, mTitle + " update OK.");
		        		
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								broadCastAction(BLEUtility.ACTION_SENCMD_OK);
							}
						});
					}
					else if(strRspCal.equals(mCommandResSWForce) == true)
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, SWForce, should show UI to warn user");
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								broadCastAction(BLEUtility.ACTION_SENCMD_SWFORCE);
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
	
	public class ChildWrtReadItem extends ChildItem {
		private String mTag = "ChildWrtReadItem";
		public String mIcon = "";
		public String mCommand1 = "";
		public String mCommand2 = "";
		public String mResponseTitle = "";
		public boolean mBIsOutofDate = false; 
		public boolean mBIsStatus1 = false; //true: status1, false:status2
		public boolean mBUpdateGrouptTitle = false;
		public String mCommandRes = "";
		public int mNExeSequence = -1;
		
		public List<ReadCmdStructur> mCommandResColl = new ArrayList<ReadCmdStructur>();
		
		public ChildWrtReadItem() {
			updateStatus(false);
		}
		
		public String getStatusTitle() {
			return mResponseTitle;
		}
		
		private void updateStatus(boolean bIsStatus1) {
			mBIsStatus1 = bIsStatus1;
			if(mCommandResColl.size() > 0 ) {
				if(mBIsStatus1)
					mResponseTitle = mCommandResColl.get(0).mResponseTitleString;
				else
					mResponseTitle = mCommandResColl.get(1).mResponseTitleString;	
			}
		}
		
		private void doCheckReadItems() {
			if(mReadCount >= mGoalReadCount || mReadFailCount >= mReadFailGoalCount)
	    	{
				mUIHanlder.post(new Runnable() {
					@Override
					public void run() {
						broadCastAction(BLEUtility.ACTION_ITEM_READ_END);
					}
				});
				mReadCount = 0;
				mReadFailCount = 0;
	    	}	
		}
		
		public void doWriteCmdAndReadRsp() {
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			broadCastAction(BLEUtility.ACTION_WRTREAD_WRT_BEG);
			Thread workerThread = new Thread() {
			    public void run() {
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
			    	byte [] rsp = null;
			    	if(mBIsStatus1)
			    	{
			    		MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + mCommand2);
			    		rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(mCommand2, true));
			    	}
			    	else
			    	{
			    		MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + mCommand1);
			    		rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(mCommand1, true));
			    	}
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
			    	if(strRspCal.equals(mCommandRes) == true)
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
						updateStatus(!mBIsStatus1);			
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								broadCastAction(BLEUtility.ACTION_WRTREAD_WRT_UPDATE);
							}
						});
					}
					else
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd not match response");
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								broadCastAction(BLEUtility.ACTION_WRTREAD_WRT_FAIL);
							}
						});
					}
			    }
			};
			workerThread.start();
		}
		
		public void doReadRsp()
		{
			MyLog.d(mTag, "doReadRsp begin");
			Thread workerThread = new Thread() {
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
		    		MyLog.d(mTag, "doReadRsp, sequ = " + mNExeSequence + ", begin in synchronized(mLockSequence), in thread = " + Thread.currentThread().getId());
			    	MyLog.d(mTag, "doReadRsp, write cmd = " + mCommand);
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
			    	++mReadCount;
			    	updateStatus(false);
			    	MyLog.d(mTag, "doReadRsp, read from integral = " + strRsp);
			    	MyLog.d(mTag, "doReadRsp, read from integral hex = " + strRspHex);
			    	MyLog.d(mTag, "doReadRsp, read after CRC = " + strRspCal);
			    	MyLog.d(mTag, "doReadRsp, read after CRC hex = " + strRspCalHex);
			    	for(int idxRsp = 0; idxRsp < mCommandResColl.size(); ++idxRsp)
			    	{
			    		//0: status1, 1: status2
			    		ReadCmdStructur itrRsp = mCommandResColl.get(idxRsp); 
			    		MyLog.d(mTag, "doReadRsp, compare from = " + itrRsp);
			    		
			    		if(strRspCal.equals(itrRsp.mResponseString) == true)
						{
							MyLog.d(mTag, "doReadRsp, read ok, update status now");
							mBIsStatus1 = (idxRsp == 0) ? true : false;
							updateStatus(mBIsStatus1);
							mBIsOutofDate = false;
							if(mBUpdateGrouptTitle) {
								mParentItem.mBIsOutofDate = false;
								mParentItem.mGroupResponse = mResponseTitle;
							}
							mUIHanlder.post(new Runnable() {
								@Override
								public void run() {
									broadCastAction(BLEUtility.ACTION_ITEM_READ_UPDATE);
								}
							});
							doCheckReadItems();
							mLockSequence.notifyAll();
							return;
						}
			    	}
			    	MyLog.d(mTag, "doReadRsp, read fail");
			    	++mReadFailCount;
			    	mBIsOutofDate = true;
					if(mBUpdateGrouptTitle) {
						mParentItem.mBIsOutofDate = true;
					}
					mUIHanlder.post(new Runnable() {
						@Override
						public void run() {
							broadCastAction(BLEUtility.ACTION_ITEM_READ_UPDATE);
						}
					});
					doCheckReadItems();
					mLockSequence.notifyAll();
			    }
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
			UIUtility.showProgressDlg(true, R.string.prgsReadConfig);
			mReadCount = 0;
			mReadFailCount = 0;
			for(int idxGroup = 0; idxGroup < mAdapter.getGroupCount(); ++idxGroup)
			{
				GroupItem groupItem = mAdapter.getGroup(idxGroup);
				if(groupItem != null)
				{
					groupItem.mGroupResponse = "";
					groupItem.mBIsOutofDate = false; 
				}
			}
			for(int idxWrCmd = 0; idxWrCmd < mListWrtReadCmds.size(); ++idxWrCmd)
			{
				ChildWrtReadItem chdWrtReadItem = mListWrtReadCmds.get(idxWrCmd);
				if(chdWrtReadItem != null)
				{
					chdWrtReadItem.mResponseTitle = "";
					chdWrtReadItem.mBIsOutofDate = false; 
				}
			}
			
			mAdapter.notifyDataSetChanged();
			for(int idxGroup = 0; idxGroup < mAdapter.getGroupCount(); ++idxGroup)
			{
				GroupItem groupItem = mAdapter.getGroup(idxGroup);
				if(groupItem instanceof CanReadGroup)
				{
					((CanReadGroup)groupItem).doReadRsp();
				}
			}
			for(int idxWrCmd = 0; idxWrCmd < mListWrtReadCmds.size(); ++idxWrCmd)
			{
				ChildWrtReadItem chdWrtReadItem = mListWrtReadCmds.get(idxWrCmd);
				if(chdWrtReadItem != null)
					chdWrtReadItem.doReadRsp();
			}
		}
	}
	
	private class ChildWrtCECItem extends ChildItem {
		private String mTag = "ChildWrtCECItem";
		public String mIcon = "";
		public String mPreCmd = "";
		public String mCommandRes = "";
		
		public void doWriteCmdAndReadRsp()
		{
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			broadCastAction(BLEUtility.ACTION_SENCMD_BEGIN);
			
			Thread workerThread = new Thread() {
			    public void run() {
			    	String cmd = mPreCmd + " " + mCommand;
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd write cmd = " + cmd);
			    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(cmd, true));
			    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
			    	String strRspCal = "";
			    	if(rspCal != null)
			    		strRspCal = new String(rspCal);
					if(strRspCal.equals(mCommandRes) == true)
					{
						MyLog.d(mTag, "doWriteCmdAndReadRsp, BLEUtility.writeCmd match response");
						mParentItem.mBIsOutofDate= false; 
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
		
		public void doIt()
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(mFaActivity);
			builder.setTitle(R.string.InputDlgTitle);

			// Set up the input
			final EditText input = new EditText(mFaActivity);
			input.setText(mCommand);
			// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
			input.setInputType(InputType.TYPE_CLASS_TEXT);
			builder.setView(input);

			// Set up the buttons
			builder.setPositiveButton(R.string.InputDlgOK, new DialogInterface.OnClickListener() { 
			    @Override
			    public void onClick(DialogInterface dialog, int which) {
			    	mCommand = input.getText().toString();
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
		View  mChildNormal;
		TextView mTitle;
		TextView mRespTitle;
		FontelloTextView  mIcon;
		CheckBox  mCheckBox;
		
		View  mChildCEC;
		FontelloTextView mbtnCEC1;
		FontelloTextView mbtnCEC2;
		FontelloTextView mbtnCEC3;
		FontelloTextView mbtnCEC4;
		
		MaterialRippleLayout mbtnCECLay1;
		MaterialRippleLayout mbtnCECLay2;
		MaterialRippleLayout mbtnCECLay3;
		MaterialRippleLayout mbtnCECLay4;
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
			
			ChildHolder chdholder = null;
			ChildItem item = getChild(groupPosition, childPosition);
			if (convertView == null) {
				convertView = inflater.inflate(R.layout.list_item, parent, false);
				chdholder = new ChildHolder();
				chdholder.mChildCEC = (View)convertView.findViewById(R.id.lstCECLayout);
				chdholder.mbtnCEC1 = (FontelloTextView) convertView.findViewById(R.id.cecBtn1);
				chdholder.mbtnCEC2 = (FontelloTextView) convertView.findViewById(R.id.cecBtn2);
				chdholder.mbtnCEC3 = (FontelloTextView) convertView.findViewById(R.id.cecBtn3);
				chdholder.mbtnCEC4 = (FontelloTextView) convertView.findViewById(R.id.cecBtn4);
				chdholder.mbtnCECLay1 = (MaterialRippleLayout) convertView.findViewById(R.id.cecBtn1Lay);
				chdholder.mbtnCECLay2 = (MaterialRippleLayout) convertView.findViewById(R.id.cecBtn2Lay);
				chdholder.mbtnCECLay3 = (MaterialRippleLayout) convertView.findViewById(R.id.cecBtn3Lay);
				chdholder.mbtnCECLay4 = (MaterialRippleLayout) convertView.findViewById(R.id.cecBtn4Lay);
				chdholder.mChildNormal = (View)convertView.findViewById(R.id.lstChildRelativeLayout);
				chdholder.mTitle = (TextView) convertView.findViewById(R.id.textTitle);
				chdholder.mRespTitle = (TextView) convertView.findViewById(R.id.textRespStatus);
				chdholder.mIcon = (FontelloTextView) convertView.findViewById(R.id.lstChildItemIcon);
				chdholder.mCheckBox = (CheckBox) convertView.findViewById(R.id.chkWt);
				chdholder.mCheckBox.setEnabled(false);
				convertView.setTag(chdholder);
			} 
			else
				chdholder = (ChildHolder) convertView.getTag();
				
			chdholder.mTitle.setText(item.mTitle);
			chdholder.mChildNormal.setVisibility(View.VISIBLE);
			chdholder.mChildCEC.setVisibility(View.INVISIBLE);
			if(item instanceof ChildWrtItem)
			{
				chdholder.mRespTitle.setVisibility(View.INVISIBLE);
				chdholder.mIcon.setVisibility(View.VISIBLE);
				chdholder.mIcon.setText(((ChildWrtItem)item).getIcon());
				chdholder.mCheckBox.setVisibility(View.INVISIBLE);
			}
			else if(item instanceof CECChildItem)
			{
				chdholder.mChildNormal.setVisibility(View.INVISIBLE);
				chdholder.mChildCEC.setVisibility(View.VISIBLE);
				((CECChildItem)item).setCEC1(chdholder.mbtnCEC1, chdholder.mbtnCECLay1);
				((CECChildItem)item).setCEC2(chdholder.mbtnCEC2, chdholder.mbtnCECLay2);
				((CECChildItem)item).setCEC3(chdholder.mbtnCEC3, chdholder.mbtnCECLay3);
				((CECChildItem)item).setCEC4(chdholder.mbtnCEC4, chdholder.mbtnCECLay4);
			}
			else if(item instanceof ChildWrtChkItem)
			{
				chdholder.mRespTitle.setVisibility(View.INVISIBLE);
				chdholder.mIcon.setVisibility(View.INVISIBLE);
				chdholder.mCheckBox.setChecked(((ChildWrtChkItem) item).mBIsChecked);
				chdholder.mCheckBox.setVisibility(View.VISIBLE);
			}
			else if(item instanceof ChildWrtCECItem)
			{
				chdholder.mRespTitle.setVisibility(View.INVISIBLE);
				chdholder.mIcon.setVisibility(View.VISIBLE);
				chdholder.mIcon.setText(((ChildWrtCECItem)item).mIcon);
				chdholder.mCheckBox.setVisibility(View.INVISIBLE);
			}
			else if(item instanceof ChildReadAllItem)
			{
				chdholder.mRespTitle.setVisibility(View.INVISIBLE);
				chdholder.mIcon.setVisibility(View.VISIBLE);
				chdholder.mIcon.setText(((ChildReadAllItem)item).mIcon);
				chdholder.mCheckBox.setVisibility(View.INVISIBLE);
			}
			else if(item instanceof ChildWrtReadItem) {
				if(((ChildWrtReadItem)item).mBIsOutofDate && ((ChildWrtReadItem)item).mBUpdateGrouptTitle == false)
				{
					chdholder.mRespTitle.setTextColor(getResources().getColor(R.color.material_red_200));
					chdholder.mRespTitle.setText(getResources().getString(R.string.groupOutofDate));
				}
				else if(((ChildWrtReadItem)item).mBIsOutofDate == false && ((ChildWrtReadItem)item).mBUpdateGrouptTitle == false)
				{
					chdholder.mRespTitle.setTextColor(getResources().getColor(R.color.custom_green_color));
					chdholder.mRespTitle.setText(((ChildWrtReadItem)item).getStatusTitle());
				}
				else if(((ChildWrtReadItem)item).mBUpdateGrouptTitle == true)
				{
					chdholder.mRespTitle.setTextColor(getResources().getColor(R.color.custom_green_color));
					chdholder.mRespTitle.setText("");
				}
				chdholder.mRespTitle.setVisibility(View.VISIBLE);
				chdholder.mIcon.setVisibility(View.INVISIBLE);
				chdholder.mCheckBox.setChecked(((ChildWrtReadItem) item).mBIsStatus1);
				chdholder.mCheckBox.setVisibility(View.VISIBLE);
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
	
	public static ExpandaListActivity newInstance() {
		return new ExpandaListActivity();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mFaActivity  = (MainActivity) super.getActivity();
		mIntegralView = (View) inflater.inflate(R.layout.activity_expandable_list_view, container, false);
		mFaActivity.registerReceiver(mMsgeceiver, makeMsgIntentFilter());	
		String devName = mFaActivity.getIntent().getStringExtra("DeviceName");
		String devAddr = mFaActivity.getIntent().getStringExtra("DeviceAddr");
		if(devName.length() > 0 && devAddr.length() > 0)
		{
			IntegralSetting.setDeviceName(devName);
			IntegralSetting.setDeviceMACAddr(devAddr);
		}
		
		File path = Environment.getExternalStoragePublicDirectory(
	            Environment.DIRECTORY_DOWNLOADS);
				
		mExternalXMLFile = new File(path, "Commands.xml");
	    
		if(mExternalXMLFile.exists())
		{
			AlertDialog.Builder builderSingle = new AlertDialog.Builder(super.getActivity());
            builderSingle.setIcon(R.drawable.ic_icon);
            builderSingle.setTitle(R.string.dlgXMLIntegral);
            builderSingle.setMessage(R.string.dlgXMLIntegralMsg);
            builderSingle.setPositiveButton(R.string.dlgOK,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
        				InitPage();
        				mExternalXMLFile = null;
                    }
                });
            builderSingle.setNegativeButton(R.string.dlgCancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mExternalXMLFile = null;
                            InitPage();
                        }
                    });
           
            builderSingle.show();
		}
		else {
			mExternalXMLFile = null;
			InitPage();
		}
		return mIntegralView;
	}
	
	@Override
	public void onDestroy() {
		mFaActivity.unregisterReceiver(mMsgeceiver);
		super.onDestroy();
	}
	
	private void InitPage() {
		InputSource inputSource = null;
		try {
			if(mExternalXMLFile != null)
			{
				inputSource = new InputSource(new FileInputStream(mExternalXMLFile.getPath()));
			}
			else
			{
				inputSource = new InputSource(mFaActivity.getAssets().open("InternalCommands.xml"));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e1) {
			e1.printStackTrace();
		}  
		
		//parsing XML
		List<GroupItem> groupItems = new ArrayList<GroupItem>();
		mGoalReadCount = 0;
		XPath xpath = XPathFactory.newInstance().newXPath();  
		String expressCmdRoot = "/Commands";  
		String expression = "//CmdGroup";  
		NodeList nodes = null;
		try {
			NodeList nodeRoots = (NodeList) xpath.evaluate(expressCmdRoot, inputSource, XPathConstants.NODESET);
			Node cmdRootNode = nodeRoots.item(0);
			if(cmdRootNode != null) {
				NamedNodeMap attributes = cmdRootNode.getAttributes();  
			    
			    if(attributes != null && attributes.getNamedItem("CmdDelay") != null) {
			    	String cmdDelay = attributes.getNamedItem("CmdDelay").getNodeValue();
					BLEUtility.setCmdDelay(Integer.parseInt(cmdDelay));
			    }
			}
			
			nodes = (NodeList) xpath.evaluate(expression, cmdRootNode, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int idxCmdGroup = 0; idxCmdGroup<nodes.getLength(); ++idxCmdGroup) {  
		    Node cmdGroupNode = nodes.item(idxCmdGroup);  
		    NamedNodeMap attributes = cmdGroupNode.getAttributes();  
		    String isCanReadGroup = "";
		    if(attributes.getNamedItem("CanRead") != null)
		    	isCanReadGroup = attributes.getNamedItem("CanRead").getNodeValue();
		    
		    GroupItem cmdgroup = null;
		    if(isCanReadGroup.equals("false"))
		    	cmdgroup = new GroupItem();
		    else
		    {
		    	cmdgroup = new CanReadGroup();
		    	((CanReadGroup)cmdgroup).mNExeSequence = Integer.parseInt(attributes.getNamedItem("Sequence").getNodeValue());
		    	((CanReadGroup)cmdgroup).mNTry = Integer.parseInt(attributes.getNamedItem("Try").getNodeValue());
		    	++mGoalReadCount;
		    }
		    cmdgroup.mID = idxCmdGroup;
		    cmdgroup.mGroupTitle = "";
		    cmdgroup.mGroupIcon = "";
		    if(attributes.getNamedItem("Title") != null)
		    	cmdgroup.mGroupTitle = attributes.getNamedItem("Title").getNodeValue();
		    if(attributes.getNamedItem("Icon") != null)
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
				    		if(strResNodeName.compareTo("CmdResOK") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			((ChildWrtChkItem)command).mCommandResOK = new String(strVal);
				    		}
				    		else if(strResNodeName.compareTo("CmdResSWForce") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			((ChildWrtChkItem)command).mCommandResSWForce = new String(strVal);
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    		}
		    		else if(strNodeName.compareTo("CECChildItem") == 0)
		    		{
		    			command = new CECChildItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			((CECChildItem)command).mCECCmd[0] = cmdNode.getAttributes().getNamedItem("Cmd1").getNodeValue();
				    	((CECChildItem)command).mCECCmd[1] = cmdNode.getAttributes().getNamedItem("Cmd2").getNodeValue();
				    	((CECChildItem)command).mCECCmd[2] = cmdNode.getAttributes().getNamedItem("Cmd3").getNodeValue();
				    	((CECChildItem)command).mCECCmd[3] = cmdNode.getAttributes().getNamedItem("Cmd4").getNodeValue();
				    	
				    	((CECChildItem)command).mCECCheckedIcon[0] = cmdNode.getAttributes().getNamedItem("CheckedIcon1").getNodeValue();
				    	((CECChildItem)command).mCECCheckedIcon[1] = cmdNode.getAttributes().getNamedItem("CheckedIcon2").getNodeValue();
				    	((CECChildItem)command).mCECCheckedIcon[2] = cmdNode.getAttributes().getNamedItem("CheckedIcon3").getNodeValue();
				    	((CECChildItem)command).mCECCheckedIcon[3] = cmdNode.getAttributes().getNamedItem("CheckedIcon4").getNodeValue();
				    	
				    	((CECChildItem)command).mCECUnCheckedIcon[0] = cmdNode.getAttributes().getNamedItem("UnCheckedIcon1").getNodeValue();
				    	((CECChildItem)command).mCECUnCheckedIcon[1] = cmdNode.getAttributes().getNamedItem("UnCheckedIcon2").getNodeValue();
				    	((CECChildItem)command).mCECUnCheckedIcon[2] = cmdNode.getAttributes().getNamedItem("UnCheckedIcon3").getNodeValue();
				    	((CECChildItem)command).mCECUnCheckedIcon[3] = cmdNode.getAttributes().getNamedItem("UnCheckedIcon4").getNodeValue();
				    	
				    	((CECChildItem)command).mCurIcon[0] = ((CECChildItem)command).mCECUnCheckedIcon[0];
				    	((CECChildItem)command).mCurIcon[1] = ((CECChildItem)command).mCECUnCheckedIcon[1];
				    	((CECChildItem)command).mCurIcon[2] = ((CECChildItem)command).mCECUnCheckedIcon[2];
				    	((CECChildItem)command).mCurIcon[3] = ((CECChildItem)command).mCECUnCheckedIcon[3];
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
				    		if(strResNodeName.compareTo("CmdResOK") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			((ChildWrtItem)command).mCommandResOK = new String(strVal);
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
		    			if(cmdNode.getAttributes().getNamedItem("PreCmd") != null)
		    				((ChildWrtCECItem)command).mPreCmd = cmdNode.getAttributes().getNamedItem("PreCmd").getNodeValue();
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
	    			else if(strNodeName.compareTo("WrtReadCmd") == 0)
		    		{
	    				command = new ChildWrtReadItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("ReadCmd").getNodeValue();
		    			((ChildWrtReadItem)command).mCommand1 = cmdNode.getAttributes().getNamedItem("Cmd1").getNodeValue();
		    			((ChildWrtReadItem)command).mCommand2 = cmdNode.getAttributes().getNamedItem("Cmd2").getNodeValue();
		    			((ChildWrtReadItem)command).mCommandRes = cmdNode.getAttributes().getNamedItem("CmdRes").getNodeValue();
		    			((ChildWrtReadItem)command).mNExeSequence = Integer.parseInt(cmdNode.getAttributes().getNamedItem("Sequence").getNodeValue());
		    			Node ndUpdateGrpTtle = cmdNode.getAttributes().getNamedItem("UpdateGroupTitle");
		    			if(ndUpdateGrpTtle != null && ndUpdateGrpTtle.getNodeValue().compareTo("true") == 0)
		    				((ChildWrtReadItem)command).mBUpdateGrouptTitle = true;
		    				
		    			for(int idxCmdRes = 0; idxCmdRes < cmdNode.getChildNodes().getLength(); ++idxCmdRes) { 
				    		Node cmdResNode = cmdNode.getChildNodes().item(idxCmdRes); 
				    		String strResNodeName = cmdResNode.getLocalName();
				    		if(strResNodeName == null)
				    			continue;
				    		if(strResNodeName.compareTo("ReadRes") == 0)
				    		{
				    			String strVal = cmdResNode.getFirstChild().getNodeValue();
				    			String reRespTitle = cmdResNode.getAttributes().getNamedItem("Title").getNodeValue();
				    			ReadCmdStructur rs = new ReadCmdStructur(strVal, reRespTitle, "none");
				    			((ChildWrtReadItem)command).mCommandResColl.add(rs);
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    			mListWrtReadCmds.add((ChildWrtReadItem) command);
		    			++mGoalReadCount;
		    		}
		    	}
		    }
		    
		    
		}
		//parsing XML end
		mAdapter = new ExampleAdapter(mFaActivity);
		mAdapter.setData(groupItems);

		mListView = (AnimatedExpandableListView) mIntegralView.findViewById(R.id.animated_expandable_list_view);
		mListView.setAdapter(mAdapter);
		mListView.setGroupIndicator(null);

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
				if(mFaActivity.needRequestBT() == true)
				{
					mPreCmdToExecute = childItem;
					return false;
				}
				else if(BLEUtility.getInstance().isConnect() == false)
				{
					mPreCmdToExecute = childItem;
					mFaActivity.connectToIntegral();
					return false;
				}
				
				return executeCmd(childItem);
			}
			
		});

		// Set indicator (arrow) to the right
		Display display = mFaActivity.getWindowManager().getDefaultDisplay();
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
			else if(true == (childItem instanceof ChildWrtReadItem)) {
				((ChildWrtReadItem)childItem).doWriteCmdAndReadRsp();
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
	
	private void setPullBKTask(boolean bSet) {
		if(bSet) {
			
		}
		else {
			
		}
	}
	
	/*
	private void copyToInternal(String pathTo, File fileSrc) {
		try {
			InputStream is = new FileInputStream(fileSrc);
			FileOutputStream writer = mFaActivity.openFileOutput(pathTo, Context.MODE_PRIVATE);
			byte[] buff = new byte[500];
			int len;
			while((len = is.read(buff)) > 0 )
				writer.write(buff,0,len);
			is.close();
			writer.close();
		}
		catch (FileNotFoundException e1)
		{
			e1.printStackTrace();
		}
		catch (IOException e) { 
			e.printStackTrace();
		}
	}
	*/
	
	public void doThingsAfterConnted() {
		if(mPreCmdToExecute != null)
		{
			executeCmd(mPreCmdToExecute);
			mPreCmdToExecute = null;
		}
		else 
			doReadAllStatus();
		setPullBKTask(true);	
	}
	
	public void doReadAllStatus() {
		if(mReadAllCmd != null)
			mReadAllCmd.doIt();
	}

	public void broadCastAction(String action)
	{
		final Intent brd = new Intent(action);
        mFaActivity.sendBroadcast(brd);
	}
	
	public void broadCastActionMsg(String action, String key, String message)
	{
		final Intent brd = new Intent(action);
		brd.putExtra(key, message);
        mFaActivity.sendBroadcast(brd);
	}	
}