package com.UI.LEDevice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
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
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
	private final static String ACTION_GROUP_READ_OK_KEY = "ACTION_GROUP_READ_OK_KEY";
	private final static String ACTION_GROUP_READ_FAIL = "ACTION_GROUP_READ_FAIL";
	private final static String ACTION_GROUP_READ_FAIL_KEY = "ACTION_GROUP_READ_FAIL_KEY";
	private static final long SCAN_PERIOD = 5000; // Stops scanning after 8 seconds.
	//data member
	private AnimatedExpandableListView mListView;
	private ExampleAdapter mAdapter;
	private static Handler mUIHanlder = new Handler();
	private Handler mScanPeriodHandler = new Handler();
	private ArrayList<BLEDevice> mLeDevices = new ArrayList<BLEDevice>();
	BLEDevice mPreDevice = null;
	private boolean mBConnected = false;
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
            	UIUtility.showProgressDlg(MainActivity.this, false, "disconnected");
            	String message = intent.getStringExtra(BLEUtility.ACTION_CONNSTATE_DISCONNECTED_KEY);
				Toast.makeText(MainActivity.this, "Disconnected, cause = " + message, Toast.LENGTH_SHORT).show();
            	mBConnected = false;
            	updateUIForConn();
                return;
            }
			else if(BLEUtility.ACTION_CONNSTATE_CONNECTING.equals(action))
			{
				UIUtility.showProgressDlg(MainActivity.this, true, "connecting");
			}
			else if(BLEUtility.ACTION_CONNSTATE_CONNECTED.equals(action))
			{
				UIUtility.showProgressDlg(MainActivity.this, false, "connected");
				IntegralSetting.setDeviceName(mPreDevice.getDeviceName());
				IntegralSetting.setDeviceMACAddr(mPreDevice.getAddress());
				mBConnected = true;
				updateUIForConn();
				Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
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
            	UIUtility.showProgressDlg(MainActivity.this, true, "sending cmd");
            }
            else if(BLEUtility.ACTION_SENCMD_OK.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "sending cmd OK");
            	Toast.makeText(MainActivity.this, "sending cmd OK", Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_FAIL.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "sending cmd fail");
            	Toast.makeText(MainActivity.this, "sending cmd fail", Toast.LENGTH_SHORT).show();
            }
            else if (BLEUtility.ACTION_SENCMD_READ.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, "sending read cmd");
            }
            else if(BLEUtility.ACTION_SENCMD_READ_CONTENT.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "read cmd ok");
            	String message = intent.getStringExtra(BLEUtility.ACTION_SENCMD_READ_CONTENT_KEY);
            	Toast.makeText(MainActivity.this, "read cmd ok, response = " + message, Toast.LENGTH_SHORT).show();
            }
            else if(BLEUtility.ACTION_SENCMD_READ_FAIL.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "read cmd fail");
            	Toast.makeText(MainActivity.this, "read cmd fail", Toast.LENGTH_SHORT).show();
            }
            else if(ACTION_GROUP_READ_OK.equals(action))
            {
            	int [] idArr = intent.getIntArrayExtra(ACTION_GROUP_READ_OK_KEY);
            	ChildWrtItem childItem =(ChildWrtItem) mAdapter.getChild(idArr[0], idArr[1]) ;
            	if(childItem != null)
            	{
            		if(childItem.mParentItem != null)
            			childItem.mParentItem.unCheckAllWrtChild();
            		childItem.mBIsChecked = true;
            		mAdapter.notifyDataSetChanged();
            	}
            	++mReadCount;
            	if(mReadCount >= mGoalReadCount)
            	{
            		UIUtility.showProgressDlg(MainActivity.this, false, "read done");
            		Toast.makeText(MainActivity.this, "read done", Toast.LENGTH_SHORT).show();
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
            	++mReadCount;
            	if(mReadCount >= mGoalReadCount)
            	{
            		UIUtility.showProgressDlg(MainActivity.this, false, "read done");
            		Toast.makeText(MainActivity.this, "read done", Toast.LENGTH_SHORT).show();
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
		public String mGroupTitle;
		public String mGroupIcon;
		List<ChildItem> items = new ArrayList<ChildItem>();
		
		public void doReadRsp()
		{
			MyLog.d(mTag, "doReadRsp begin");
			ChildReadItem readItem = null; 
			for(ChildItem chdItem: items)
			{
				if(chdItem instanceof ChildReadItem)
					readItem = (ChildReadItem) chdItem;
			}
			if(readItem != null)
			{
				final ChildReadItem chdReadItemTemp = readItem;
				final List<ChildItem> itemsTemp = items;
				Thread workerThread = new Thread() 
				{
				    public void run() {
				    	MyLog.d(mTag, "doReadRsp, BLEUtility.writeCmd in thread" + Thread.currentThread().getId());
				    	MyLog.d(mTag, "doReadRsp, BLEUtility.writeCmd write cmd = " + chdReadItemTemp.mCommand);
				    	byte [] rsp = BLEUtility.getInstance().writeCmd(CmdProcObj.addCRC(chdReadItemTemp.mCommand, false));
				    	byte [] rspCal = CmdProcObj.calCRC(rsp, true);
				    	String strRspCal = "";
				    	if(rspCal != null)
				    		strRspCal = new String(rspCal);
				    	MyLog.d(mTag, "doReadRsp, read content = " + strRspCal);
				    	for(ChildItem item: itemsTemp)
				    	{
				    		if(item instanceof ChildWrtItem == false)
				    			continue;
				    		final ChildWrtItem wrtItem = (ChildWrtItem) item;

				    		String itrRsp = wrtItem.mCommand;
				    		MyLog.d(mTag, "doReadRsp, compare from = " + itrRsp);
				    		if(strRspCal.equals(itrRsp) == true)
							{
								MyLog.d(mTag, "doReadRsp, read ok");
								mUIHanlder.post(new Runnable() {
									@Override
									public void run() {
										final Intent brd = new Intent(ACTION_GROUP_READ_OK);
										brd.putExtra(ACTION_GROUP_READ_OK_KEY, new int[] {mID, wrtItem.mID});
								        MainActivity.this.sendBroadcast(brd);
									}
								});
								return;
							}
				    	}
				    	MyLog.d(mTag, "doReadRsp, read fail");
						mUIHanlder.post(new Runnable() {
							@Override
							public void run() {
								final Intent brd = new Intent(ACTION_GROUP_READ_FAIL);
								brd.putExtra(ACTION_GROUP_READ_FAIL_KEY, new int[] {mID, -1});
						        MainActivity.this.sendBroadcast(brd);
							}
						});
				    }
				};
				workerThread.start();
			}
			else
				MyLog.d(mTag, "doReadRsp, GroupItem has no read item");
		}
		
		public void unCheckAllWrtChild()
		{
			for(ChildItem childItem: items)
				if(childItem instanceof ChildWrtItem)
					((ChildWrtItem)childItem).mBIsChecked = false;
		}
	}
	
	private abstract class ChildItem {
		public GroupItem mParentItem =  null;
		public int mID = -1;
		public String mTitle;
		public String mCommand;
		public abstract String getIcon();
		
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

	private class ChildWrtItem extends ChildItem  {
		private String mTag = "ChildWrtItem";
		public boolean mBIsChecked = false;
		public String mCheckedIcon = "";
		public String mUnCheckedIcon = "";
		public String mCommandRes = "";
		
		@Override
		public String getIcon() {
			if(mBIsChecked)
				return mCheckedIcon;
			else
				return mUnCheckedIcon;
		}
		
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
								final Intent brd = new Intent(ACTION_GROUP_READ_OK);
								brd.putExtra(ACTION_GROUP_READ_OK_KEY, new int[] {mParentItem.mID, mID});
						        MainActivity.this.sendBroadcast(brd);
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
	}
	
	private class ChildReadItem extends ChildItem implements Serializable{
		private static final long serialVersionUID = 4L;
		private String mTag = "ChildReadItem";
		public String mIcon = "";
		public List<String> mCommandResColl = new ArrayList<String>();
		
		@Override
		public String getIcon() {
			return mIcon;
		}
		
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
			    	String strRspCal = "";
			    	if(rspCal != null)
			    		strRspCal = new String(rspCal);
			    	MyLog.d(mTag, "doWriteCmdAndReadRsp, read content = " + strRspCal);
			    	for(final String itrRsp: mCommandResColl)
			    	{
			    		MyLog.d(mTag, "doWriteCmdAndReadRsp, compare from = " + itrRsp);
			    		if(strRspCal.equals(itrRsp) == true)
						{
							MyLog.d(mTag, "doWriteCmdAndReadRsp, read ok");
							mUIHanlder.post(new Runnable() {
								@Override
								public void run() {
									if(bBroadCastTemp)
										broadCastActionMsg(BLEUtility.ACTION_SENCMD_READ_CONTENT, BLEUtility.ACTION_SENCMD_READ_CONTENT_KEY, itrRsp);
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
		
		@Override
		public String getIcon() {
			return mIcon;
		}
		
		public void doIt()
		{
			MyLog.d(mTag, "Read config begin");
			//UIUtility.showProgressDlg(MainActivity.this, true, "Read Config..");
			for(int idxGroup = 1; idxGroup < mAdapter.getGroupCount(); ++idxGroup)
			{
				GroupItem groupItem = mAdapter.getGroup(idxGroup);
				groupItem.doReadRsp();
			}
		}
	}

	private static class ChildHolder {
		TextView mTitle;
		FontelloTextView  mIcon;
	}

	private static class GroupHolder {
		TextView mGroupTitle;
		FontelloTextView  mGroupIcon;
	}

	/**
	 * Adapter for our list of {@link GroupItem}s.
	 */
	private class ExampleAdapter extends AnimatedExpandableListAdapter {
		private LayoutInflater inflater;

		private List<GroupItem> mCollItems;

		public ExampleAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void setData(List<GroupItem> items) {
			mCollItems = items;
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
			ChildHolder chdholder;
			ChildItem item = getChild(groupPosition, childPosition);
			if (convertView == null) {
				chdholder = new ChildHolder();
				convertView = inflater.inflate(R.layout.list_item, parent, false);
				chdholder.mTitle = (TextView) convertView.findViewById(R.id.textTitle);
				chdholder.mIcon = (FontelloTextView) convertView.findViewById(R.id.lstChildItemIcon);
				convertView.setTag(chdholder);
			} else {
				chdholder = (ChildHolder) convertView.getTag();
			}

			chdholder.mTitle.setText(item.mTitle);
			chdholder.mIcon.setText(item.getIcon());

			return convertView;
		}

		@Override
		public int getRealChildrenCount(int groupPosition) {
			return mCollItems.get(groupPosition).items.size();
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
				holder.mGroupIcon = (FontelloTextView) convertView.findViewById(R.id.lstGroupItemIcon);
				convertView.setTag(holder);
			} else {
				holder = (GroupHolder) convertView.getTag();
			}

			holder.mGroupTitle.setText(item.mGroupTitle);
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
		    GroupItem cmdgroup = new GroupItem();
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
		    		if(strNodeName.compareTo("WriteCmd") == 0)
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
				    			((ChildReadItem)command).mCommandResColl.add(new String(strVal));
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    			++mGoalReadCount;
		    		}
	    			else if(strNodeName.compareTo("ReadAllCmd") == 0)
		    		{
		    			command = new ChildReadAllItem();
		    			command.mParentItem = cmdgroup;
		    			command.mID = nID++;
		    			command.mTitle = cmdNode.getAttributes().getNamedItem("Title").getNodeValue();
		    			command.mCommand = cmdNode.getAttributes().getNamedItem("Cmd").getNodeValue();
		    			cmdgroup.items.add(command);
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
				if(childItem != null)
				{
					if(true == (childItem instanceof ChildWrtItem))
					{
						((ChildWrtItem)childItem).doWriteCmdAndReadRsp();
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
				}
				return false;
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
		if(mBConnected)
		{
			mListView.setEnabled(true);
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(getResources().getString(R.string.menu_disconn));
		}
		else 
		{
			mListView.setEnabled(false);
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(getResources().getString(R.string.menu_conn));
		}
	}
	
	private void requestBTOrConn(){
		if(BluetoothAdapter.getDefaultAdapter() == null || BluetoothAdapter.getDefaultAdapter().isEnabled() == false)
		{
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	        startActivityForResult(enableBtIntent, BTSettingActivity.REQUEST_ENABLE_BT);
		}	
		else
		{
			connectToIntegral();
		}
	}
	
	private void connectToIntegral(){
		if(BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled() == true)
		{
			if(IntegralSetting.getDeviceMACAddr().length() <= 0)
			{
				UIUtility.showProgressDlg(MainActivity.this, true, "scan devices");
				mLeDevices.clear();
				BLEUtility.getInstance().startScanLEDevices();
				mScanPeriodHandler.postDelayed(new Runnable() {
	                @Override
	                public void run() {
	                    BLEUtility.getInstance().stopScanLEDevices();
	                    UIUtility.showProgressDlg(MainActivity.this, false, "find devices end");
	                    if(mLeDevices.size() == 0)
	                    {
	                    	Toast.makeText(MainActivity.this, "find no devices", Toast.LENGTH_SHORT).show();
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
