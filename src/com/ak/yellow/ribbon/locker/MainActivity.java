package com.ak.yellow.ribbon.locker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class MainActivity extends Activity {
    private ImageView mSwitchRibbon = null;
    private ImageView mSwitchUnlock = null;

    private android.view.ViewGroup.LayoutParams mParams = null;
    private int[] mUnlockPoint = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up our Lockscreen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_main);

        RibbonTouchListener ribbonListener = new RibbonTouchListener();
        mSwitchRibbon = (ImageView) findViewById(R.id.switch_yellow_ribbon);
        mSwitchRibbon.setOnTouchListener(ribbonListener);
        mSwitchRibbon.setOnClickListener(ribbonListener);
        mSwitchUnlock = (ImageView) findViewById(R.id.switch_unlock_point);

        startService(new Intent(this, YellowRibbonLockerService.class));
    }

    @Override
    public void onBackPressed() {
        // Just return to ignore back key event
        return;
    }

    public void unlockScreen(View view) {
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
}
