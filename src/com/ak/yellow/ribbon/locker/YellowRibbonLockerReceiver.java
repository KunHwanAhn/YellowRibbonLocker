package com.ak.yellow.ribbon.locker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class YellowRibbonLockerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // If the screen was just turned on or it just booted up,
        // start Lock screen Activity
        if (action.equals(Intent.ACTION_SCREEN_OFF)
                || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

}
