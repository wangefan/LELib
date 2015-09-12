package com.UI.LEDevice;

import java.util.ArrayList;
import java.util.List;

import com.BLE.BLEUtility.BLEUtility;
import com.BLE.Buttons.BLEButton;
import com.UI.LEDevice.AnimatedExpandableListView.AnimatedExpandableListAdapter;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
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
	
	//Inner classes
	BroadcastReceiver mBtnReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            
            if (BLEButton.ACTION_SENCMD_BEGIN.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, "sending cmd");
            }
            else if (BLEButton.ACTION_SENCMD_READ.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, true, "sending read cmd");
            }
            else if(BLEButton.ACTION_SENCMD_OK.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "sending cmd OK");
            	Toast.makeText(MainActivity.this, "sending cmd OK", Toast.LENGTH_SHORT).show();
            }
            else if(BLEButton.ACTION_SENCMD_FAIL.equals(action)) 
            {
            	UIUtility.showProgressDlg(MainActivity.this, false, "sending cmd fail");
            	Toast.makeText(MainActivity.this, "sending cmd fail", Toast.LENGTH_SHORT).show();
            }
            else if(BLEButton.ACTION_SENCMD_READ_FAIL.equals(action))
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

	private static class ChildItem {
		String title;
		//String hint;
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

		private List<GroupItem> items;

		public ExampleAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		public void setData(List<GroupItem> items) {
			this.items = items;
		}

		@Override
		public ChildItem getChild(int groupPosition, int childPosition) {
			return items.get(groupPosition).items.get(childPosition);
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

			holder.title.setText(item.title);
			//holder.hint.setText(item.hint);

			return convertView;
		}

		@Override
		public int getRealChildrenCount(int groupPosition) {
			return items.get(groupPosition).items.size();
		}

		@Override
		public GroupItem getGroup(int groupPosition) {
			return items.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return items.size();
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

		List<GroupItem> items = new ArrayList<GroupItem>();

		// Populate our list with groups and it's children
		for (int i = 1; i < 100; i++) {
			GroupItem item = new GroupItem();

			item.title = "Expand this item " + i;

			for (int j = 0; j < i; j++) {
				ChildItem child = new ChildItem();
				child.title = "Expanded " + j;
				//child.hint = "Too awesome";

				item.items.add(child);
			}

			items.add(item);
		}
		
		

		mAdapter = new ExampleAdapter(this);
		mAdapter.setData(items);

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
        intentFilter.addAction(BLEButton.ACTION_SENCMD_BEGIN);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_OK);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_FAIL);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_READ);
        intentFilter.addAction(BLEButton.ACTION_SENCMD_READ_FAIL);
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
