package com.UI.LEDevice;

public class DrawerItem {
	/* Commented tags are expected in future updates.
	 */
	public static final int DRAWER_ITEM_Main = 9;
	public static final int DRAWER_ITEM_HDMILink = 10;
	public static final int DRAWER_ITEM_HDMIVideo = 11;
	public static final int DRAWER_ITEM_DEVICEVER = 12;
	public static final int DRAWER_ITEM_LINKHDF = 13;
	
	public DrawerItem(int icon, int title, int tag) {
		this.icon = icon;
		this.title = title;
		this.tag = tag;
	}

	private int icon;
	private int title;
	private int tag;

	public int getIcon() {
		return icon;
	}

	public void setIcon(int icon) {
		this.icon = icon;
	}

	public int getTitle() {
		return title;
	}

	public void setTitle(int title) {
		this.title = title;
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}
}
