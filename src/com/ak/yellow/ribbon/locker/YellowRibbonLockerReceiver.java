package com.ak.yellow.ribbon.locker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class YellowRibbonLockerReceiver extends BroadcastReceiver {
    private static final String DAY_ONLY_FORMAT = "dd";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(context.getApplicationContext());
        String powerStateKey = context
                .getString(R.string.locker_power_state_key);
        String offDateKey = context.getString(R.string.locker_off_date_key);

        String offDay = null;
        boolean haveToTurnOn = context.getResources().getBoolean(
                R.bool.locker_power_default_state);

        offDay = sharedPref.getString(offDateKey, offDay);
        haveToTurnOn = sharedPref.getBoolean(powerStateKey, haveToTurnOn);

        if (!haveToTurnOn) {
            Calendar today = Calendar.getInstance();
            DateFormat df = new SimpleDateFormat(DAY_ONLY_FORMAT,
                    Locale.getDefault());
            String currDay = df.format(today.getTime());
            if (offDay != null && !currDay.equalsIgnoreCase(offDay)) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(powerStateKey, !haveToTurnOn);
                editor.putString(offDateKey, null);
                editor.commit();
                haveToTurnOn = true;
            }
        }

        if (haveToTurnOn) {
            // If the screen was just turned on or it just booted up,
            // start Lock screen Activity
            if (action.equals(Intent.ACTION_SCREEN_OFF)
                    || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
                Intent i = new Intent(context, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(i);
            }
        }
    }

}
