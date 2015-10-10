package com.UI.LEDevice;

import java.util.ArrayList;
import java.util.List;

import com.BLE.BLEUtility.BLEUtility;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {

	private ListView mDrawerList;
	private List<DrawerItem> mDrawerItems;
	private DrawerLayout mDrawerLayout;
	private ActionBarDrawerToggle mDrawerToggle;
	private Menu mMenu = null;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;

	private Handler mHandler;

	private boolean mShouldFinish = false;
	
	public void updateUIForConn()
	{
		if(BLEUtility.getInstance().isConnect())
		{
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(R.string.menu_disconn);
		}
		else 
		{
			if(mMenu != null)
				mMenu.findItem(R.id.menu_connect).setTitle(R.string.menu_conn);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ImageLoader imageLoader = ImageLoader.getInstance();
		if (!imageLoader.isInited()) {
			imageLoader.init(ImageLoaderConfiguration.createDefault(this));
		}

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
				R.string.drawer_open, R.string.drawer_close) {
			public void onDrawerClosed(View view) {
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu();
			}

			public void onDrawerOpened(View drawerView) {
				getSupportActionBar().setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerToggle.setDrawerIndicatorEnabled(true);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mTitle = mDrawerTitle = getTitle();
		mDrawerList = (ListView) findViewById(R.id.list_view);

		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		prepareNavigationDrawerItems();
		mDrawerList.setAdapter(new DrawerAdapter(this, mDrawerItems, true));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		mHandler = new Handler();

		if (savedInstanceState == null) {
			int position = 0;
			selectItem(position, mDrawerItems.get(position).getTag());
		}
	}
	
	@Override
	public void onResume() {
		updateUIForConn();
		super.onResume();
	}
	
	@Override
	public void onDestroy() {
		BLEUtility.getInstance().disconnect();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (!mShouldFinish && !mDrawerLayout.isDrawerOpen(mDrawerList)) {
			Toast.makeText(getApplicationContext(), R.string.confirm_exit,
					Toast.LENGTH_SHORT).show();
			mShouldFinish = true;
			mDrawerLayout.openDrawer(mDrawerList);
		} else if (!mShouldFinish && mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mDrawerLayout.closeDrawer(mDrawerList);
		} else {
			super.onBackPressed();
		}
	}

	private void prepareNavigationDrawerItems() {
		mDrawerItems = new ArrayList<DrawerItem>();
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_Main,
				R.string.drawer_title_Main,
				DrawerItem.DRAWER_ITEM_Main));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_HDMILink,
				R.string.drawer_title_HDMILink,
				DrawerItem.DRAWER_ITEM_HDMILink));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_HDMIVideo,
				R.string.drawer_title_HDMIVideo,
				DrawerItem.DRAWER_ITEM_HDMIVideo));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_DEVICEVER,
				R.string.drawer_title_DEVICEVER,
				DrawerItem.DRAWER_ITEM_DEVICEVER));
		mDrawerItems.add(new DrawerItem(R.string.drawer_icon_LINKHDF,
				R.string.drawer_title_LINKHDF,
				DrawerItem.DRAWER_ITEM_LINKHDF));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		menu.findItem(R.id.menu_connect).setVisible(true);
		mMenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		switch (item.getItemId()) {
        
        case android.R.id.home:
        	BLEUtility.getInstance().disconnect();
        	break;
        case R.id.menu_connect:
        {
        	String tle = (String) item.getTitle(); 
        	if(tle.compareTo(getResources().getString(R.string.menu_disconn)) == 0)
        		BLEUtility.getInstance().disconnect();
        	else if(tle.compareTo(getResources().getString(R.string.menu_conn)) == 0) {
        		Fragment integral = selectItem(0, mDrawerItems.get(0).getTag());
        		if(((ExpandaListActivity)integral) != null)
        			((ExpandaListActivity)integral).requestBTOrConn();
        	}
        		
        }
        break;
        case R.id.menu_clearConn:
        {
        	IntegralSetting.setDeviceMACAddr("");
        	Toast.makeText(this, R.string.msgResetConn, Toast.LENGTH_SHORT).show();
        }
        break;
        default:	
		}	
		return super.onOptionsItemSelected(item);
	}

	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position, mDrawerItems.get(position).getTag());
		}
	}

	private Fragment selectItem(int position, int drawerTag) {
		Fragment fragment = getFragmentByDrawerTag(drawerTag);
		commitFragment(fragment);

		mDrawerList.setItemChecked(position, true);
		setTitle(mDrawerItems.get(position).getTitle());
		mDrawerLayout.closeDrawer(mDrawerList);
		return fragment;
	}

	private Fragment getFragmentByDrawerTag(int drawerTag) {
		Fragment fragment;
		if (drawerTag == DrawerItem.DRAWER_ITEM_Main) {
			fragment = ExpandaListActivity.newInstance();
		} else {
			fragment = new Fragment();
		}
		mShouldFinish = false;
		return fragment;
	}

	private class CommitFragmentRunnable implements Runnable {

		private Fragment fragment;

		public CommitFragmentRunnable(Fragment fragment) {
			this.fragment = fragment;
		}

		@Override
		public void run() {
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction()
					.replace(R.id.content_frame, fragment).commit();
		}
	}

	public void commitFragment(Fragment fragment) {
		// Using Handler class to avoid lagging while
		// committing fragment in same time as closing
		// navigation drawer
		mHandler.post(new CommitFragmentRunnable(fragment));
	}

	@Override
	public void setTitle(int titleId) {
		setTitle(getString(titleId));
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
}