package com.UI.LEDevice;

import com.BLE.BLEUtility.BLEUtility;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.widget.TextView;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.UI.font.FontelloTextView;
import com.UI.kenburnsview.KenBurnsView;

public class SplashScreensActivity extends Activity {
	//constanct
	public static final String SPLASH_SCREEN_OPTION = "com.csform.android.uiapptemplate.SplashScreensActivity";
	public static final String SPLASH_SCREEN_OPTION_1 = "Option 1";
	public static final String SPLASH_SCREEN_OPTION_2 = "Option 2";
	public static final String SPLASH_SCREEN_OPTION_3 = "Option 3";
	
	// Splash screen timer
    private static int SPLASH_TIME_OUT = 3000;
	private KenBurnsView mKenBurns;
	private FontelloTextView mLogo;
	private TextView mWelcomeText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE); //Removing ActionBar
		setContentView(R.layout.splashscreenactivity);
		BLEUtility.initBLEUtility(getApplicationContext());
		
		//Init controls
		mKenBurns = (KenBurnsView) findViewById(R.id.ken_burns_images);
		mLogo = (FontelloTextView) findViewById(R.id.logo);
		mWelcomeText = (TextView) findViewById(R.id.welcome_text);
		mKenBurns.setImageResource(R.drawable.splash_screen_background);
		
		String category = SPLASH_SCREEN_OPTION_1;
		setAnimation(category);
		
		new Handler().postDelayed(new Runnable() {
			 
            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */
 
            @Override
            public void run() {
                // This method will be executed once the timer is over
                // Start your app main activity
                Intent i = new Intent(SplashScreensActivity.this, ConnectionActivity.class);
                startActivity(i);
 
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
