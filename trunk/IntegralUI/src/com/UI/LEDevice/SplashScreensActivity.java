package com.UI.LEDevice;

import com.BLE.BLEUtility.BLEDevice;
import com.BLE.BLEUtility.BLEUtility;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.ImageView;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import com.UI.font.RobotoTextView;
import com.UI.kenburnsview.KenBurnsView;

public class SplashScreensActivity extends Activity {
	//constanct
	public static final String SPLASH_SCREEN_OPTION = "com.csform.android.uiapptemplate.SplashScreensActivity";
	public static final String SPLASH_SCREEN_OPTION_1 = "Option 1";
	public static final String SPLASH_SCREEN_OPTION_2 = "Option 2";
	public static final String SPLASH_SCREEN_OPTION_3 = "Option 3";
	
	// Splash screen timer
    private static int SPLASH_TIME_OUT = 5000;
	private KenBurnsView mKenBurns;
	private ImageView mLogo;
	private RobotoTextView mWelcomeText;
	private boolean mBRegGetIntegral;
	private BLEDevice mIntegralDevice;
	
	//Inner classes
	BroadcastReceiver mreceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
            if (BLEUtility.ACTION_GET_LEDEVICE.equals(action)) 
            {
            	if(mIntegralDevice == null)
            		mIntegralDevice = (BLEDevice) intent.getSerializableExtra(BLEUtility.ACTION_GET_LEDEVICE_KEY);
            }
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE); //Removing ActionBar
		setContentView(R.layout.splashscreenactivity);
		BLEUtility.initBLEUtility(getApplicationContext());
		//initialize preference value
		IntegralSetting.initSharedPreferences(getApplicationContext());
		
		//Init controls
		mKenBurns = (KenBurnsView) findViewById(R.id.ken_burns_images);
		mLogo = (ImageView) findViewById(R.id.logo);
		mWelcomeText = (RobotoTextView) findViewById(R.id.welcome_text);
		mKenBurns.setImageResource(R.drawable.splash_screen_background);
		
		String category = SPLASH_SCREEN_OPTION_3;
		setAnimation(category);
		
		mBRegGetIntegral = false;
		mIntegralDevice = null;
		
		//Scan integral first if no logged device and BT opened
		if(IntegralSetting.getDeviceMACAddr().length() <= 0 && 
		   BluetoothAdapter.getDefaultAdapter() != null && 
		   BluetoothAdapter.getDefaultAdapter().isEnabled())
		{
			final IntentFilter intentFilter = new IntentFilter();
	        intentFilter.addAction(BLEUtility.ACTION_GET_LEDEVICE);
			registerReceiver(mreceiver, intentFilter);	
			mBRegGetIntegral = true;
			
			BLEUtility.getInstance().startScanLEDevices();
		}
		
		new Handler().postDelayed(new Runnable() {
			 
            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */
 
            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashScreensActivity.this, MainActivity.class);
                String deviceName = "";
                String deviceAddr = "";
                if(mIntegralDevice != null)
                {
                	deviceName = mIntegralDevice.getDeviceName();
                	deviceAddr = mIntegralDevice.getAddress();
                }
                i.putExtra("DeviceName", deviceName);
                i.putExtra("DeviceAddr", deviceAddr);
                startActivity(i);
 
                if(mBRegGetIntegral)
                {
                	unregisterReceiver(mreceiver);	
                	BLEUtility.getInstance().stopScanLEDevices();
                }
                
                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
	}
	
	/** Animation depends on category.
	 * */
	private void setAnimation(String category) {
		if (category.equals(SPLASH_SCREEN_OPTION_1)) {
			animation1();
		} else if (category.equals(SPLASH_SCREEN_OPTION_2)) {
			animation2();
		} else if (category.equals(SPLASH_SCREEN_OPTION_3)) {
			animation2();
			animation3();
		}
	}

	private void animation1() {
		ObjectAnimator scaleXAnimation = ObjectAnimator.ofFloat(mLogo, "scaleX", 5.0F, 1.0F);
		scaleXAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		scaleXAnimation.setDuration(1200);
		ObjectAnimator scaleYAnimation = ObjectAnimator.ofFloat(mLogo, "scaleY", 5.0F, 1.0F);
		scaleYAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		scaleYAnimation.setDuration(1200);
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(mLogo, "alpha", 0.0F, 1.0F);
		alphaAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
		alphaAnimation.setDuration(1200);
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.play(scaleXAnimation).with(scaleYAnimation).with(alphaAnimation);
		animatorSet.setStartDelay(500);
		animatorSet.start();
	}
	
	private void animation2() {
		mLogo.setAlpha(1.0F);
		Animation anim = AnimationUtils.loadAnimation(this, R.anim.translate_top_to_center);
		mLogo.startAnimation(anim);
	}
	
	private void animation3() {
		ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(mWelcomeText, "alpha", 0.0F, 1.0F);
		alphaAnimation.setStartDelay(1700);
		alphaAnimation.setDuration(500);
		alphaAnimation.start();
	}
}
