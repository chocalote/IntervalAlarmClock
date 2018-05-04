package com.kunxun.intervalalarmclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * Sets alarm on ACTION_BOOT_COMPLETED.  Resets alarm on
 * TIME_SET, TIMEZONE_CHANGED
 */

public class AlarmInitReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Remove the snooze alarm after a boot.
        if(action.equals(Intent.ACTION_BOOT_COMPLETED)){
            Alarms.saveSnoozeAlert(context, -1, -1);
        }
        Alarms.disableExpiredAlarms(context);
        Alarms.setNextAlert(context);
    }
}
