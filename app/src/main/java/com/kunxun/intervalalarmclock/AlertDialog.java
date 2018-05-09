package com.kunxun.intervalalarmclock;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


/**
 * Full screen alarm alert: pops visible indicator and plays alarm tone. This
 * activity shows the alert as a dialog.
 */
public class AlertDialog extends AlertActivity {

    // If we try to check the keyguard more than 5 times, just launch the full screen activity.
    private int mKeyguardRetryCount;

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            handleScreenOff((KeyguardManager) msg.obj);
        }
    };

    private final BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            KeyguardManager keyguardManager =
                    (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            assert keyguardManager != null;
            handleScreenOff(keyguardManager);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Listen for the screen turning off so that when the screen comes back on,
        // the user does not need to unlock the phone to dismiss the alarm.
        registerReceiver(mScreenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mScreenReceiver);
        // Remove any of the keyguard messages just in case
        mHandler.removeMessages(0);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void handleScreenOff(final KeyguardManager keyguardManager) {

        if (!keyguardManager.inKeyguardRestrictedInputMode() && checkRetryCount()) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(0, keyguardManager), 500);
        } else {
            // Launch the full screen activity but do not turn the screen on.
            Intent intent = new Intent(this, AlertActivity.class);
            intent.putExtra(Alarms.ALARM_INTENT_EXTRA, mAlarm);
            intent.putExtra(SCREEN_OFF, true);
            startActivity(intent);
            finish();
        }
    }

    private boolean checkRetryCount() {
        int MAX_KEYGUARD_CHECKS = 5;
        return mKeyguardRetryCount++ < MAX_KEYGUARD_CHECKS;
    }
}
