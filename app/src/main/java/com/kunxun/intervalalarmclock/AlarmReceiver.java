package com.kunxun.intervalalarmclock;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.util.Log;

import java.text.SimpleDateFormat;


public class AlarmReceiver extends BroadcastReceiver {

    /** If the alarm is older than STALE_WINDOW, ignore.  It
     is probably the result of a time or timezone change */
    private final static int STALE_WINDOW = 30*60*1000;

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Alarms.ALARM_KILLED.equals(intent.getAction())) {
            // The alarm has been killed, update the notification
            updateNotification(context, (Alarm)intent.getParcelableExtra(Alarms.ALARM_INTENT_EXTRA),
                    intent.getIntExtra(Alarms.ALARM_KILLED_TIMEOUT, -1));
            return;
        } else if (Alarms.CANCEL_SNOOZE.equals(intent.getAction())) {
            Alarms.saveSnoozeAlert(context,-1,-1);
            return;
        } else if (!Alarms.ALARM_ALERT_ACTION.equals(intent.getAction())) {
            // Unknown intent, bail.
            return;
        }

        Alarm alarm = null;
        // Grab the alarm from the intent. Since the remote AlarmManagerService
        // fills in the Intent to add some extra data, it must unparcel the
        // Alarm object. It throws a ClassNotFoundException when unparcelling.
        // To avoid this, do the marshalling ourselves.
        final byte[] data = intent.getByteArrayExtra(Alarms.ALARM_RAW_DATA);
        if (data != null) {
            Parcel in = Parcel.obtain();
//            in.readParcelable(Alarm.class.getClassLoader());
            in.unmarshall(data, 0, data.length);
            in.setDataPosition(0);
            alarm = Alarm.CREATOR.createFromParcel(in);
            Log.v("Lily", "AlarmID: "+ alarm.id +", Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(alarm.time));
        }

        if (alarm == null) {
            Log.v("Lily", "Failed to parse the alarm from the intent");

            // Make sure we set the next alert if needed.
            Alarms.setNextAlert(context);
            return;
        }

        // Disable the snooze alert if this alarm is the snooze.
        Alarms.disableSnoozeAlert(context, alarm.id);
        // Disable this alarm if it does not repeat.
        if (alarm.daysOfWeek.nonRepeatSet()) {
            Alarms.enableAlarm(context, alarm.id, false);
        } else {
            // Enable the next alert if there is one, The above call to
            // enableAlarm will call setNextAlert so avoid calling it twice.
            Alarms.setNextAlert(context);
        }

        // Intentionally verbose: always log the alarm time to provide useful
        // information in bug report.
        long now = System.currentTimeMillis();

        // Always verbose to tack down time change problems
        if (now > alarm.time + STALE_WINDOW) {
            Log.v("Lily", "Ignoring stale alarm");
            return;
        }

        // Maintain a CPU wake lock until the AlarmAlert and AlarmKlaxon can pick it up
        AlertWakeLock.acquireCpuWakeLock(context);

        // Close dialogs and window shad
        Intent closeDialogs = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.sendBroadcast(closeDialogs);

        // Decide which activity to start based on the state of the keyguard
        Class c = AlertDialog.class;
        KeyguardManager km = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);
        assert km != null;
        if (km.inKeyguardRestrictedInputMode()) {
            // Use the full screen activity for security.
            c = AlertActivity.class;
        }

        // Play the alarm alert and vibrate the device.
        Intent playAlarm = new Intent(Alarms.ALARM_ALERT_ACTION);
        playAlarm.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        playAlarm.setPackage("com.kunxun.intervalalarmclock");
        context.startService(playAlarm);

        // Trigger a notification that, when clicked, will show the alarm alert dialog.
        // No need to check for fullscreen since this will always be launched from a user action.
        Intent notify = new Intent(context, AlertDialog.class);
        notify.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        notify.setPackage("com.kunxun.intervalalarmclock");
        PendingIntent pendingNotify = PendingIntent.getActivity(context, alarm.id, notify, 0);

        // Use the alarm's label or the default label as the ticker text and main text of the notification.
        String label = alarm.getNameOrDefault(context);
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setTicker(label)
                .setWhen(alarm.time)
                .setContentTitle(label)
                .setContentText(context.getString(R.string.alarm_notify_text))
                .setContentIntent(pendingNotify)
                .setDefaults(Notification.DEFAULT_SOUND
                        | Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS)
                .build();// getNotification()

        // New: Embed the full-screen UI here. The notification manager will take care of
        // displaying it if it's OK to do so.
        Intent alarmAlert = new Intent(context, c);
        alarmAlert.putExtra(Alarms.ALARM_INTENT_EXTRA, alarm);
        alarmAlert.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        alarmAlert.setPackage("com.kunxun.intervalalarmclock");
        notification.fullScreenIntent = PendingIntent.getActivity(context, alarm.id, alarmAlert, 0);

        // Send the notification using the alarm id to easily identify the correct notification
        NotificationManager notificationManager = getNotificationManager(context);
        notificationManager.notify(alarm.id, notification);
    }

    private NotificationManager getNotificationManager(Context context){
        return (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void updateNotification(Context context, Alarm alarm, int timeout) {
        NotificationManager notificationManager = getNotificationManager(context);

        if (alarm == null) {
            return;
        }

        Intent viewAlarm = new Intent(context, AddEditActivity.class);
        viewAlarm.putExtra(Alarms.ALARM_ID, alarm.id);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, alarm.id, viewAlarm,0);

        // Update the notification to indicate that the alert has been silenced.
        String label = alarm.getNameOrDefault(context);
        Notification notification = new Notification.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setTicker(label)
                .setWhen(alarm.time)
                .setContentTitle(label)
                .setContentText(context.getString(R.string.alarm_alert_silenced, timeout))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_SOUND
                        | Notification.DEFAULT_VIBRATE
                        | Notification.DEFAULT_LIGHTS)
                .build();// getNotification()

        // We have to cancel the original notification since it is in the
        // ongoing section and we want the "killed" notification to be a plain notification.
        notificationManager.cancel(alarm.id);
        notificationManager.notify(alarm.id, notification);
    }


}
