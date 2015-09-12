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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

public class MainActivity extends Activity {
    private static final long ONE_SECOND = 1000;
    private static final long ONE_MINUTE = ONE_SECOND * 60;
    private static final long ONE_HOUR = ONE_MINUTE * 60;
    private static final long ONE_DAY = ONE_HOUR * 24;
    private static final long VIBRATE_PERIOD = 50;
    private static final String THE_DAY = "2014-04-16";
    private static final String BASIC_DATE_FORMAT = "yyyy-MM-dd";
    private static final String DAY_ONLY_FORMAT = "dd";

    private TextView mTimeView = null;
    private TextView mDDayView = null;
    private ImageView mPowerButton = null;
    private ImageView mScreenLockerCircle = null;

    private float mBaseX, mBaseY;
    private int mCircleWidth = 0, mCircleHeight = 0;
    private String mDateFormat = null;
    private TimeUpdateThread mTimeUpdateThrad = null;
    private Vibrator mVibrator = null;
    private boolean mPassedUnlockPoint = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullscreenMode(Build.VERSION.SDK_INT);
        // Set up our Lockscreen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_main);

        mDateFormat = getString(R.string.date_format);
        mTimeView = (TextView) findViewById(R.id.time_view);
        mTimeUpdateThrad = new TimeUpdateThread();
        mTimeUpdateThrad.start();

        mDDayView = (TextView) findViewById(R.id.d_day_view);

        mScreenLockerCircle = (ImageView) findViewById(R.id.screen_locker_circle_view);
        mScreenLockerCircle.setTag((Integer) R.drawable.screen_locker_circle_lock);

        if (getSystemService(VIBRATOR_SERVICE) != null) {
            mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        boolean powerState = checkPowerState();
        PowerListener powerListener = new PowerListener();
        mPowerButton = (ImageView) findViewById(R.id.power_button);
        mPowerButton.setOnClickListener(powerListener);
        updatePowerButton(powerState);

        startService(new Intent(this, YellowRibbonLockerService.class));

        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        CallStateListener callListener = new CallStateListener();
        manager.listen(callListener, PhoneStateListener.LISTEN_CALL_STATE);

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
        unlockScreen();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        float unlockSize = mScreenLockerCircle.getWidth() * 0.5f;

        if (mCircleWidth == 0 && mCircleHeight == 0) {
            mCircleWidth = mScreenLockerCircle.getWidth();
            mCircleHeight = mScreenLockerCircle.getHeight();
        }
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            float deltaX = mScreenLockerCircle.getWidth() * 0.5f;
            float deltaY = mScreenLockerCircle.getHeight() * 0.6f;
            mBaseX = x;
            mBaseY = y;
            mScreenLockerCircle
                    .setImageResource(R.drawable.screen_locker_circle_lock);
            mScreenLockerCircle.setTag((Integer) R.drawable.screen_locker_circle_lock);
            mScreenLockerCircle.setVisibility(View.VISIBLE);
            doActionDown(x - deltaX, y - deltaY);
            break;

        case MotionEvent.ACTION_UP:
            mScreenLockerCircle.setVisibility(View.INVISIBLE);
            if (isOnUnlockPoint(x, y, unlockSize)) {
                vibrate();
                unlockScreen();
            }
            break;

        case MotionEvent.ACTION_MOVE:
            updateScreenLockerCircle(isOnUnlockPoint(x, y, unlockSize));
            break;
        }

        return super.onTouchEvent(event);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doActionDown(float x, float y) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            LayoutParams params = new LayoutParams(mCircleWidth, mCircleHeight);
            params.leftMargin = (int) x;
            params.topMargin = (int) y;
            mScreenLockerCircle.setLayoutParams(params);
        } else {
            mScreenLockerCircle.setX(x);
            mScreenLockerCircle.setY(y);
        }
    }

    private void vibrate() {
        if (mVibrator != null) {
            mVibrator.vibrate(VIBRATE_PERIOD);
        }
    }

    private boolean isOnUnlockPoint(float currX, float currY, float radius) {
        if ((Math.pow(currX - mBaseX, 2) + Math.pow(currY - mBaseY, 2)) > Math
                .pow(radius, 2)) {
            return true;
        } else {
            return false;
        }
    }

    private void updateScreenLockerCircle(boolean isOnUnlockPoint) {
        if (isOnUnlockPoint) {
            if (!mPassedUnlockPoint) {
                mScreenLockerCircle
                        .setImageResource(R.drawable.screen_locker_circle_unlock);
                mScreenLockerCircle.setTag((Integer) R.drawable.screen_locker_circle_unlock);
                vibrate();
                mPassedUnlockPoint = true;
            }
        } else {
            if (mPassedUnlockPoint) {
                mScreenLockerCircle
                        .setImageResource(R.drawable.screen_locker_circle_lock);
                mScreenLockerCircle.setTag((Integer) R.drawable.screen_locker_circle_lock);
                mPassedUnlockPoint = false;
            }
        }
    }

    public void unlockScreen() {
        mTimeUpdateThrad.killThread();
        finish();
    }

    private void updateTimeView() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                Calendar today = Calendar.getInstance();
                DateFormat df = new SimpleDateFormat(mDateFormat,
                        Locale.getDefault());
                mTimeView.setText(df.format(today.getTime()));

                df = new SimpleDateFormat(BASIC_DATE_FORMAT,
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

    private boolean checkPowerState() {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String powerStateKey = getString(R.string.locker_power_state_key);
        boolean isTurnOn = getResources().getBoolean(
                R.bool.locker_power_default_state);
        return sharedPref.getBoolean(powerStateKey, isTurnOn);
    }

    private void updatePowerButton(boolean powerState) {
        Drawable drawable = mPowerButton.getDrawable().mutate();

        if (powerState) {
            drawable.setColorFilter(Color.YELLOW, Mode.MULTIPLY);
        } else {
            drawable.setColorFilter(Color.DKGRAY, Mode.MULTIPLY);
            Toast.makeText(this, R.string.turn_off_toast_string,
                    Toast.LENGTH_LONG).show();
            updateOffDate();
        }
        mPowerButton.setImageDrawable(drawable);
    }

    private void updateOffDate() {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String offDateKey = getString(R.string.locker_off_date_key);
        Calendar today = Calendar.getInstance();
        DateFormat df = new SimpleDateFormat(DAY_ONLY_FORMAT,
                Locale.getDefault());
        String offDay = df.format(today.getTime());

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(offDateKey, offDay);
        editor.commit();
    }

    private class CallStateListener extends PhoneStateListener {

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:
                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:
                unlockScreen();
                break;

            case TelephonyManager.CALL_STATE_RINGING:
                break;
            }
        }

    }

    private class PowerListener implements OnClickListener {

        @Override
        public void onClick(View v) {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext());
            String powerStateKey = getString(R.string.locker_power_state_key);
            boolean isTurnOn = checkPowerState();
            updatePowerButton(!isTurnOn);

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean(powerStateKey, !isTurnOn);
            editor.commit();
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setFullscreenMode(int versionCode) {
        if (versionCode < Build.VERSION_CODES.HONEYCOMB) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        } else {
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
        }
    }
}
