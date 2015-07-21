package com.ak.yellow.ribbon.locker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends Activity {
    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = ONE_SECOND * 60;
    private static final long ONE_HOUR = ONE_MINUTE * 60;
    private static final long ONE_DAY = ONE_HOUR * 24;
    private static final String THE_DAY = "2014-04-16";
    private static final String BASIC_DATE_PORMAT = "yyyy-MM-dd";

    private TextView mTimeView = null;
    private TextView mDDayView = null;
    private ImageView mSwitchRibbon = null;
    private ImageView mSwitchUnlock = null;

    private android.view.ViewGroup.LayoutParams mParams = null;
    private int[] mUnlockPoint = null;
    private String mDateFormat = null;
    private TimeUpdateThread mTimeUpdateThrad = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullscreenMode(Build.VERSION.SDK_INT);
        // Set up our Lockscreen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_main);

        mDateFormat = getString(R.string.date_format);
        mTimeView = (TextView) findViewById(R.id.time_view);
        mTimeUpdateThrad = new TimeUpdateThread();
        mTimeUpdateThrad.start();

        mDDayView = (TextView) findViewById(R.id.d_day_view);

        RibbonTouchListener ribbonListener = new RibbonTouchListener();
        mSwitchRibbon = (ImageView) findViewById(R.id.switch_yellow_ribbon);
        mSwitchRibbon.setOnTouchListener(ribbonListener);
        mSwitchRibbon.setOnClickListener(ribbonListener);
        mSwitchUnlock = (ImageView) findViewById(R.id.switch_unlock_point);

        startService(new Intent(this, YellowRibbonLockerService.class));

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    @Override
    public void onBackPressed() {
        // Just return to ignore back key event
        return;
    }

    @Override
    public void onUserLeaveHint() {
        // Close unlock screen when user push the home key button
        unlockScreen(null);
    }

    public void unlockScreen(View view) {
        mTimeUpdateThrad.killThread();
        finish();
    }

    private boolean isOnUnlockPoint(int x) {
        if (mUnlockPoint[0] <= x
                && mUnlockPoint[0] + mSwitchUnlock.getWidth() >= x) {
            return true;
        } else {
            return false;
        }
    }

    private void updateTimeView() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Calendar today = Calendar.getInstance();
                DateFormat df = new SimpleDateFormat(mDateFormat,
                        Locale.getDefault());
                mTimeView.setText(df.format(today.getTime()));

                df = new SimpleDateFormat(BASIC_DATE_PORMAT,
                        Locale.getDefault());
                Calendar theDay = Calendar.getInstance();
                try {
                    theDay.setTime(df.parse(THE_DAY));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                long passedDays = (today.getTime().getTime() - theDay.getTime()
                        .getTime()) / ONE_DAY;

                mDDayView.setText(String.format(
                        getString(R.string.days_format), passedDays + 1));
            }
        });
    }

    private class RibbonTouchListener implements OnTouchListener,
            OnClickListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            RelativeLayout.LayoutParams params = null;

            switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mUnlockPoint = new int[2];
                mSwitchUnlock.getLocationOnScreen(mUnlockPoint);
                mParams = mSwitchRibbon.getLayoutParams();
                break;

            case MotionEvent.ACTION_MOVE:
                params = new RelativeLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

                params.leftMargin = (int) event.getRawX()
                        - (mSwitchRibbon.getWidth() / 2);
                v.setLayoutParams(params);

                if (isOnUnlockPoint((int) event.getRawX())) {
                    unlockScreen(v);
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isOnUnlockPoint((int) event.getRawX())) {
                    unlockScreen(v);
                } else {
                    v.setLayoutParams(mParams);
                }
                break;
            }

            return false;
        }

        @Override
        public void onClick(View v) {
        }

    }

    private class TimeUpdateThread extends Thread {
        private boolean mIsAlive = false;

        @Override
        public void run() {
            while (!mIsAlive) {
                updateTimeView();
                try {
                    sleep(ONE_SECOND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        public void killThread() {
            mIsAlive = true;
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setFullscreenMode(int versionCode) {
        if (versionCode < Build.VERSION_CODES.JELLY_BEAN) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
    }
}
