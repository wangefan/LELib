package com.UI.LEDevice;

import com.UI.font.RobotoTextView;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

public abstract class CustomTitleActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getActionBar().setDisplayShowCustomEnabled(true);
		this.getActionBar().setDisplayShowTitleEnabled(false);

		LayoutInflater inflator = LayoutInflater.from(this);
		View customtitleview = inflator.inflate(R.layout.customtitleview, null);
		((RobotoTextView)customtitleview.findViewById(R.id.title)).setText(getCustTitle());
		//assign the view to the actionbar
		getActionBar().setCustomView(customtitleview);
	}
	
	abstract String getCustTitle();
}
