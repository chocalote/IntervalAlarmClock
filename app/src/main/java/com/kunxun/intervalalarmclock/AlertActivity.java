package com.kunxun.intervalalarmclock;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Alarm Clock alarm alert: pops visible indicator and plays alarm
 * tone. This activity is the full screen version which shows over the lock
 * screen with the wallpaper as the background.
 */

public class AlertActivity extends Activity implements SlideBar.OnTriggerListener {

    private static final String DEFAULT_SNOOZE_TIME = "10";
//    private static final String DEFAULT_ALERT_TIME = "10";
    private static final String DEFAULT_VOLUME_BEHAVIOR = "2";
    protected static final String SCREEN_OFF = "screen_off";

    protected Alarm mAlarm;
    private int mVolumeBehavior;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case Alarms.ALARM_SNOOZE_ACTION:
                    snooze();
                    break;
                case Alarms.ALARM_DISMISS_ACTION:
                    dismiss(false);
                    break;
                default:
                    Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
                    if (alarm != null && alarm.id == mAlarm.id) {
                        dismiss(true);
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        mAlarm = getIntent().getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        // sign changed by reason
        mAlarm = Alarms.getAlarm(getContentResolver(), mAlarm.id);

        // Get the volume/camera button behavior setting
        final String vol = PreferenceManager.getDefaultSharedPreferences(this).
                getString(SettingActivity.KEY_VOLUME_BEHAVIOR, DEFAULT_VOLUME_BEHAVIOR);
        mVolumeBehavior = Integer.parseInt(vol);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        final Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Turn on the screen unless we are being launched from the AlarmAlert subclass.
        if (!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        }

        updateLayout();

        // Register to get the alarm killed/snooze/dismiss intent.
        IntentFilter filter = new IntentFilter(Alarms.ALARM_KILLED);
        filter.addAction(Alarms.ALARM_SNOOZE_ACTION);
        filter.addAction(Alarms.ALARM_DISMISS_ACTION);
        registerReceiver(mReceiver, filter);
    }

    private void updateLayout() {
        Button btnSnooze = findViewById(R.id.btnSnooze);
        btnSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snooze();
            }
        });

        SlideBar slideToUnLock = findViewById(R.id.slideBar);
        slideToUnLock.setOnTriggerListener(this);

        setTextView();
    }

    private void setTextView() {
        TextView textViewTime, textViewName;
        textViewTime = findViewById(R.id.textViewTime);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        textViewTime.setText(dateFormat.format(new Date()));

        textViewName = findViewById(R.id.textViewName);
        textViewName.setText(mAlarm.getNameOrDefault(this));
    }

    @Override
    public void onTrigger() {
        dismiss(false);

    }

    private void snooze() {
        // Do not snooze if the snooze button is disabled
        if (!findViewById(R.id.btnSnooze).isEnabled()) {
            dismiss(false);
            return;
        }

        final String snooze = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(SettingActivity.KEY_ALARM_SNOOZE, DEFAULT_SNOOZE_TIME);
        int snoozeMinutes = Integer.parseInt(snooze);
        final long snoozeTime = System.currentTimeMillis() + (1000 * 60 * snoozeMinutes);
        Alarms.saveSnoozeAlert(this, mAlarm.id, snoozeTime);

        // Get the display time for the snooze and update the notification
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(snoozeTime);

        // Append (snooze) to the label.
        String label = mAlarm.getNameOrDefault(this);
        label = getString(R.string.alarm_notify_snooze_label, label);

        // Notify the user that the alarm has been snoozed.
        Intent cancelSnooze = new Intent(this, AlarmReceiver.class);
        cancelSnooze.setAction(Alarms.CANCEL_SNOOZE);
        cancelSnooze.putExtra(Alarms.ALARM_ID, mAlarm.id);
        PendingIntent broadcast = PendingIntent.getBroadcast(this, mAlarm.id, cancelSnooze, 0);
        NotificationManager manager = getNotificationManager();
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setTicker(label)
                .setContentTitle(label)
                .setContentText(getString(R.string.alarm_notify_snooze_text, snooze))
                .setContentIntent(broadcast)
                .setAutoCancel(true)
                .setOngoing(true)
                .setDefaults(Notification.DEFAULT_SOUND
                        | Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS)
                .build();
        manager.notify(mAlarm.id, notification);

        String displayTime = getString(R.string.alarm_alert_snooze_set, snoozeMinutes);
        // Display the snooze minutes in a toast
        Toast.makeText(this, displayTime, Toast.LENGTH_LONG).show();

        stopService(new Intent(Alarms.ALARM_ALERT_ACTION));
        finish();
    }

    private void dismiss(boolean killed) {
        // The service told us that the alarm has been killed, do not modify the notification or stop the service
        if (!killed) {
            // Cancel the notification and stop playing the alarm
            NotificationManager manager = getNotificationManager();
            manager.cancel(mAlarm.id);
            stopService(new Intent(Alarms.ALARM_ALERT_ACTION));
        }
        finish();
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mAlarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
        setTextView();
    }

    @Override
    public void onResume(){
        super.onResume();
        if(Alarms.getAlarm(getContentResolver(), mAlarm.id) == null){
            findViewById(R.id.btnSnooze).setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onBackPressed() {
        //Don't allow back to dismiss. The method is overridden by AlarmAlert so that the dialog is dismissed
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Do this on key down to handle a few of the system keys.
        boolean up = event.getAction() == KeyEvent.ACTION_UP;
        switch (event.getKeyCode()) {
            // Volume keys and camera keys dismiss the alarm
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_FOCUS:
                if (up) {
                    switch (mVolumeBehavior) {
                        case 1:
                            snooze();
                            break;
                        case 2:
                            dismiss(false);
                            break;
                        default:
                            break;
                    }
                }
                return true;
            default:
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}
