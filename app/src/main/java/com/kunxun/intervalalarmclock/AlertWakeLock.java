package com.kunxun.intervalalarmclock;

import android.content.Context;
import android.os.PowerManager;

public class AlertWakeLock {

    private static PowerManager.WakeLock sCPUWakeLock;

    static void acquireCpuWakeLock(Context context) {
        if (sCPUWakeLock != null) {
            return;
        }

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        sCPUWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "AlarmClock");

        sCPUWakeLock.acquire();
    }

    static void releaseCpuLock(){
        if(sCPUWakeLock != null) {
            sCPUWakeLock.release();
            sCPUWakeLock = null;
        }
    }
}
