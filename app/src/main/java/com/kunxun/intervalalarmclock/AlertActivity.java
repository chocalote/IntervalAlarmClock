package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AlertActivity extends Activity implements SlideBar.OnTriggerListener {

    private static final String DEFAULT_SNOOZE_TIME= "10";
    private static final String DEFAULT_ALERT_TIME  ="10";
    private static final String DEFAULT_VOLUME_BEHAVIOR = "2";
    private static final String SCREEN_OFF = "screen_off";

    protected Alarm mAlarm;
    private int mVolumeBehavior;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(Alarms.ALARM_SNOOZE_ACTION)){
                snooze();
            }
            else if(action.equals(Alarms.ALARM_DISMISS_ACTION)){
                dismiss(false);
            }
            else {
                Alarm alarm = intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA);
                if(alarm != null && alarm.id == mAlarm.id)
                {
                    dismiss(true);
                }
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
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED| WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        // Turn on the screen unless we are being launched from the AlarmAlert subclass.
        if(!getIntent().getBooleanExtra(SCREEN_OFF, false)) {
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

    private void updateLayout(){
        Button btnSnooze = findViewById(R.id.btnSnooze);
        btnSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snooze();
            }
        });

        SlideBar slideToUnLock = findViewById(R.id.slideBar);
        slideToUnLock.setOnTriggerListener(this);

        TextView textViewTime, textViewName;
        textViewTime = findViewById(R.id.textViewTime);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
        textViewTime.setText(dateFormat.format(new Date()));

        textViewName = findViewById(R.id.textViewName);
        textViewName.setText(mAlarm.getNameOrDefault(this));

    }

    @Override
    public void onTrigger() {
        dismiss(false);

    }
    private void snooze(){

    }

    private void dismiss(boolean killed){

    }
}
