package com.UI.LEDevice;

import java.io.IOException;
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
import com.BLE.Buttons.BLEButton;
import com.BLE.Buttons.BLEButton.LECmd;
import com.UI.LEDevice.AnimatedExpandableListView.AnimatedExpandableListAdapter;
import com.utility.CmdProcObj;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends CustomTitleActivity 
{
	//constant 
	private final String mTAG = "MainActivity";
	//data member
	private AnimatedExpandableListView mListView;
	private ExampleAdapter mAdapter;
	private static Handler mUIHanlder = new Handler();
	
	//Inner classes
	BroadcastReceiver mBtnReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            
            if (BLEUtility.ACTION_SENCMD_BEGIN.equals(action)) 
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
            else if(BLEUtility.ACTION_CONNSTATE_DISCONNECTED.equals(action))
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "disconnected");
            	String message = intent.getStringExtra(BLEUtility.ACTION_CONNSTATE_DISCONNECTED_KEY);
            	Toast.makeText(MainActivity.this, "disconnected, cause = " + message, Toast.LENGTH_SHORT).show();
            	finish();
                return;
            }
		}
	};
	
	private static class GroupItem {
		String title;
		List<ChildItem> items = new ArrayList<ChildItem>();
	}
	
	private class ChildItem {
		public String mTitle;
		public String mCommand;
		protected void broadCastAction(String action)
		{
			final Intent brdConnState = new Intent(action);
	        MainActivity.this.sendBroadcast(brdConnState);
		}
		protected void broadCastActionMsg(String action, String key, String message)
		{
			final Intent brd = new Intent(action);
			brd.putExtra(key, message);
	        MainActivity.this.sendBroadcast(brd);
		}	
		
	}

	private class ChildWrtItem extends ChildItem {
		private String mTag = "ChildWrtItem";
		public String mCommandRes;
		
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
	
	private class ChildReadItem extends ChildItem {
		private String mTag = "ChildWrtItem";
		public List<String> mCommandResColl = new ArrayList<String>();
		public void doWriteCmdAndReadRsp()
		{
			MyLog.d(mTag, "doWriteCmdAndReadRsp begin");
			broadCastAction(BLEUtility.ACTION_SENCMD_READ);
			
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
							broadCastAction(BLEUtility.ACTION_SENCMD_READ_FAIL);
						}
					});
			    }
			};
			workerThread.start();
		}
	}

	private static class ChildHolder {
		TextView title;
		//TextView hint;
	}

	private static class GroupHolder {
		TextView title;
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
			ChildHolder holder;
			ChildItem item = getChild(groupPosition, childPosition);
			if (convertView == null) {
				holder = new ChildHolder();
				convertView = inflater.inflate(R.layout.list_item, parent,
						false);
				holder.title = (TextView) convertView
						.findViewById(R.id.textTitle);
				/*holder.hint = (TextView) convertView
						.findViewById(R.id.textHint);*/
				convertView.setTag(holder);
			} else {
				holder = (ChildHolder) convertView.getTag();
			}

			holder.title.setText(item.mTitle);
			//holder.hint.setText(item.hint);

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
				convertView = inflater.inflate(R.layout.group_item, parent,
						false);
				holder.title = (TextView) convertView
						.findViewById(R.id.textTitle);
				convertView.setTag(holder);
			} else {
				holder = (GroupHolder) convertView.getTag();
			}

			holder.title.setText(item.title);

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
		getActionBar().setDisplayHomeAsUpEnabled(true);
		 
		registerReceiver(mBtnReceiver, makeServiceActionsIntentFilter());	
		setContentView(R.layout.activity_expandable_list_view);

		//parsing XML
		List<GroupItem> groupItems = new ArrayList<GroupItem>();
		XPath xpath = XPathFactory.newInstance().newXPath();  
		String expression = "//CmdGroup";  
		
		InputSource inputSource = null;
		try {
			inputSource = new InputSource(getAssets().open("Commands.xml"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
		    cmdgroup.title = attributes.getNamedItem("Title").getNodeValue();
		    groupItems.add(cmdgroup);
		    NodeList cmdsList = cmdGroupNode.getChildNodes();
		    if(cmdsList != null)
		    {
		    	for(int idxCmd = 0; idxCmd < cmdsList.getLength(); ++idxCmd) { 
		    		Node cmdNode = cmdsList.item(idxCmd);  
		    		String strNodeName = cmdNode.getLocalName();
		    		if(strNodeName == null)
		    			continue;
		    		ChildItem command = null;
		    		if(strNodeName.compareTo("WriteCmd") == 0)
		    		{
		    			command = new ChildWrtItem();
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
				    			((ChildWrtItem)command).mCommandRes = new String(strVal);
				    			break;
				    		}
		    			}
		    			cmdgroup.items.add(command);
		    		}
		    		else if(strNodeName.compareTo("ReadCmd") == 0)
		    		{
		    			command = new ChildReadItem();
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
						((ChildReadItem)childItem).doWriteCmdAndReadRsp();
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
	}
	
	private static IntentFilter makeServiceActionsIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_BEGIN);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_OK);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ_CONTENT);
        intentFilter.addAction(BLEUtility.ACTION_SENCMD_READ_FAIL);
        intentFilter.addAction(BLEUtility.ACTION_CONNSTATE_DISCONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return intentFilter;
    }
	
	@Override
    protected void onResume() {
		super.onResume();
	}
	
	@Override
    protected void onPause() {
        super.onPause();
    }
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mBtnReceiver);
		super.onDestroy();
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
        super.onActivityResult(requestCode, resultCode, data);
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.findItem(R.id.menu_disconnect).setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
        
        case android.R.id.home:
            onBackPressed();
            return true;
        case R.id.menu_disconnect:
        	BLEUtility.getInstance().disconnect();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	String getCustTitle() {
		return getResources().getString(R.string.strMainActivity);
	}
}
