package com.kunxun.intervalalarmclock;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Settings;
import android.util.Log;
import java.util.Calendar;

public class Alarms {

    // This action triggers the AlarmReceiver as well as the AlarmKlaxon. It
    // is a public action used in the manifest for receiving Alarm broadcasts
    // from the alarm manager.
    public static final String ALARM_ALERT_ACTION = "com.kunxun.intervalalarmclock.ALARM_ALERT";

    // A public action sent by AlarmKlaxon when the alarm has stopped sounding
    // for any reason (e.g. because it has been dismissed from AlarmAlertFullScreen,
    // or killed due to an incoming phone call, etc).
    public static final String ALARM_DONE_ACTION = "com.kunxun.intervalalarmclock.ALARM_DONE";

    // AlarmAlertFullScreen listen for this broadcast intent, so that other applications
    // can snooze the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_SNOOZE_ACTION = "com.kunxun.intervalalarmclock.ALARM_SNOOZE";

    // AlarmAlertFullScreen listen for this broadcast intent, so that other applications
    // can dismiss the alarm (after ALARM_ALERT_ACTION and before ALARM_DONE_ACTION).
    public static final String ALARM_DISMISS_ACTION ="com.kunxun.intervalalarmclock.ALARM_DISMISS";

    // This is a private action used by the AlarmKlaxon to update the UI to
    // show the alarm has been killed.
    public static final String ALARM_KILLED = "alarm_killed";

    // Extra in the ALARM_KILLED intent to indicate to the user how long the
    // alarm played before been killed
    public static final String ALARM_KILLED_TIMEOUT="alarm_killed_timeout";

    // This string is used to indicate a silent alarm in the db.
    public static final String ALARM_ALERT_SILENT = "silent";

    // This intent is sent from notification when the user cancels the snooze alert.
    public static final String CANCEL_SNOOZE ="cancel_snooze";

    // This string is used when passing an Alarm object through an intent
    public static final String ALARM_INTENT_EXTRA = "intent.extra.alarm";

    // This extra is the raw Alarm object data. It is used in the
    // AlarmManagerService to avoid a ClassNotFoundException when filling in
    // the Intent extras.
    public static final String ALARM_RAW_DATA = "intent.extra.alarm_raw";

    // This string is used to identify the alarm id passed to AddEditActivity from the
    // list of alarms.
    public static final String ALARM_ID = "alarm_id";

    final static String PREF_SNOOZE_TIME = "snooze_time";
    final static String PREF_SNOOZE_ID = "snooze_id";



    /**
     * Queries all alarms
     *
     * @return cursor over all alarms
     */
    public static Cursor getAlarmCursor(ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

    // Private method to get a more limited set of enabled alarms from the database.
    private static Cursor getFilterdAlarmCursor(ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                Alarm.Columns.WHERE_ENABLED, null, null);
    }

    private static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(12);

        // Set the alarm_time value if this alarm does not repeat. This will be
        // used later to disable expire alarms.
        long time = 0;
        if (alarm.daysOfWeek.nonRepeatSet()) {
            time = calculateAlarm(alarm);
        }

        values.put(Alarm.Columns.START_HOUR, alarm.starthour);
        values.put(Alarm.Columns.START_MINUTES, alarm.startminutes);
        values.put(Alarm.Columns.END_HOUR, alarm.endhour);
        values.put(Alarm.Columns.END_MINUTES, alarm.endminutes);
        values.put(Alarm.Columns.INTERVAL_ENABLED, alarm.intervalenabled);
        values.put(Alarm.Columns.INTERVAL, alarm.interval);
        values.put(Alarm.Columns.ENABLED, alarm.enabled);
        values.put(Alarm.Columns.DAYS_OF_WEEK, alarm.daysOfWeek.getCoded());
        values.put(Alarm.Columns.VIBRATE, alarm.vibrate);
        values.put(Alarm.Columns.ALERT, (alarm.alert == null) ? ALARM_ALERT_SILENT : alarm.alert.toString());
        values.put(Alarm.Columns.NAME, alarm.name);
        values.put(Alarm.Columns.TIME, time);

        return values;
    }

    private static void clearSnoozeIfNeeded(Context context, long alarmTime) {
        // If this alarm fires before the next snooze, clear the snooze to
        // enable this alarm.
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
        long snoozeTime = prefs.getLong(PREF_SNOOZE_TIME, 0);
        if (alarmTime < snoozeTime) {
            clearSnoozePreference(context, prefs);
        }
    }

    /**
     * Return an Alarm object representing the alarm id in the database.
     * Returns null if no alarm exists.
     */
    public static Alarm getAlarm(ContentResolver contentResolver, int alarmId) {
        Cursor cursor = contentResolver.query(
                ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId),
                Alarm.Columns.ALARM_QUERY_COLUMNS, null, null, null);

        Alarm alarm = null;
        if (cursor != null) {
            if (cursor.moveToFirst())
                alarm = new Alarm(cursor);
            cursor.close();
        }
        return alarm;
    }

    /**
     * Creates a new Alarm and fills in the given alarm's id.
     */
    public static void addAlarm(Context context, Alarm alarm) {
        long timeInMillis = calculateAlarm(alarm);
        alarm.time = timeInMillis;
        ContentValues values = createContentValues(alarm);
        Uri uri = context.getContentResolver().insert(Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);

        if (alarm.enabled) {
            clearSnoozeIfNeeded(context, timeInMillis);
        }
        setNextAlert(context);
    }

    /**
     * Removes an existing Alarm.  If this alarm is snoozing, disables
     * snooze.  Sets next alert.
     */
    public static void deleteAlarm(Context context, int alarmId){
        if(alarmId ==-1) return;
        ContentResolver contentResolver = context.getContentResolver();

        disableSnoozeAlert(context, alarmId);
        Uri uri = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarmId);
        contentResolver.delete(uri, "", null);

        setNextAlert(context);
    }

    /**
     * A convenience method to update an alarm in the Alarms
     * content provider.
     */
    public static void updateAlarm(Context context, Alarm alarm) {
        long timeInMillis = calculateAlarm(alarm);
        alarm.time = timeInMillis;
        ContentValues values = createContentValues(alarm);
        ContentResolver resolver = context.getContentResolver();
        resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
        if (alarm.enabled) {
            // Disable the snooze if we just changed the snoozed alarm. This
            // only does work if the snoozed alarm is the same as the given
            // alarm.
            disableSnoozeAlert(context, alarm.id);

            // Disable the snooze if this alarm fires before the snoozed alarm.
            // This works on every alarm since the user most likely intends to
            // have the modified alarm fire next.
            clearSnoozeIfNeeded(context, timeInMillis);
        }

        setNextAlert(context);
    }

    /**
     * A convenience method to enable or disable an alarm.
     *
     * @param id      corresponds to the _id column
     * @param enabled corresponds to the ENABLED column
     */
    public static void enableAlarm(
            final Context context, final int id, boolean enabled) {
        enableAlarmInternal(context, id, enabled);
        setNextAlert(context);
    }

    private static void enableAlarmInternal(final Context context, final int id, boolean enabled) {
        enableAlarmInternal(context, getAlarm(context.getContentResolver(), id),
                enabled);
    }

    private static void enableAlarmInternal(final Context context, final Alarm alarm, boolean enabled) {
        if (alarm == null) {
            return;
        }
        ContentResolver resolver = context.getContentResolver();

        ContentValues values = new ContentValues(2);
        values.put(Alarm.Columns.ENABLED, enabled ? 1 : 0);

        // If we are enabling the alarm, calculate alarm time since the time
        // value in Alarm may be old.
        if (enabled) {
            long time = 0;
            if (alarm.daysOfWeek.nonRepeatSet()) {
                time = calculateAlarm(alarm);
            }
            values.put(Alarm.Columns.TIME, time);
        } else {
            // Clear the snooze if the id matches.
            disableSnoozeAlert(context, alarm.id);
        }

        resolver.update(ContentUris.withAppendedId(
                Alarm.Columns.CONTENT_URI, alarm.id), values, null, null);
    }

    private static Alarm calculateNextAlert(final Context context) {
        Alarm alarm = null;
        long minTime = Long.MAX_VALUE;
        long now = System.currentTimeMillis();
        Cursor cursor = getFilterdAlarmCursor(context.getContentResolver());

        if (cursor != null && cursor.moveToFirst()) {
            do {
                Alarm a = new Alarm(cursor);
                if (a.time == 0) {
                    a.time = calculateAlarm(a);
                } else if (a.time < now) {
                    Log.v("Kunxun", "Disabling expired alarm");
                    enableAlarmInternal(context, a, false);
                    continue;
                }
                if (a.time < minTime) {
                    minTime = a.time;
                    alarm = a;
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return alarm;
    }

    public static long calculateAlarm(Alarm alarm) {
        Calendar c = Calendar.getInstance();
        int nowTime = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int alarmStartTime = alarm.starthour * 60 + alarm.startminutes;
        int alarmEndTime = alarm.endhour * 60 + alarm.endminutes;

        //Interval enabled = false
        c.set(Calendar.HOUR_OF_DAY, alarm.starthour);
        c.set(Calendar.MINUTE, alarm.startminutes);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        int addDays = alarm.daysOfWeek.getNextAlarmDay(c);
        if (addDays > 0) {
            c.add(Calendar.DAY_OF_WEEK, addDays);
        } else {
            if (!alarm.intervalenabled) {
                if (nowTime > alarmStartTime) {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                }
            } else {
                if (nowTime > alarmEndTime) {
                    c.add(Calendar.DAY_OF_YEAR, 1);
                } else if (nowTime > alarmStartTime) {
                    int i = 1;
                    while (nowTime <= alarmEndTime) {
                        if (nowTime <= (alarmStartTime + i * alarm.interval)) {
                            c.add(Calendar.MINUTE, i * alarm.interval);
                            break;
                        }
                        i++;
                    }
                }
            }
        }
        return c.getTimeInMillis();
    }


    private static void clearSnoozePreference(final Context context, final SharedPreferences prefs) {
        final int alarmId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (alarmId != -1) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            assert nm != null;
            nm.cancel(alarmId);
        }
        final SharedPreferences.Editor ed = prefs.edit();
        ed.remove(PREF_SNOOZE_ID);
        ed.remove(PREF_SNOOZE_TIME);
        ed.apply();
    }

    public static void saveSnoozeAlert(final Context context, final int id,final long time) {
        SharedPreferences prefs = context.getSharedPreferences(
                MainActivity.PREFERENCES, 0);
        if (id == -1) {
            clearSnoozePreference(context, prefs);
        } else {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putInt(PREF_SNOOZE_ID, id);
            ed.putLong(PREF_SNOOZE_TIME, time);
            ed.apply();
        }
        // Set the next alert after updating the snooze.
        setNextAlert(context);
    }

    /**
     * If there is a snooze set, enable it in AlarmManager
     *
     * @return true if snooze is set
     */
    private static boolean enableSnoozeAlert(final Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
        int id = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (id == -1) {
            return false;
        }
        long time = prefs.getLong(PREF_SNOOZE_TIME, 0);
        final Alarm alarm = getAlarm(context.getContentResolver(), id);
        if (alarm == null)
            return false;

        alarm.time = time;
        enableAlert(context, alarm, time);
        return true;
    }

    /**
     * Disable the snooze alert if the given id matches the snooze id.
     */
    public static void disableSnoozeAlert(final Context context, final int id) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
        int snoozeId = prefs.getInt(PREF_SNOOZE_ID, -1);
        if (snoozeId == id) {
            clearSnoozePreference(context, prefs);
        }
    }


    /**
     * Called at system startup, on time/timezone change, and whenever
     * the user changes alarm settings.  Activates snooze if set,
     * otherwise loads all alarms, activates next alert.
     */
    public static void setNextAlert(final Context context) {

        if (!enableSnoozeAlert(context)) {
            Alarm alarm = calculateNextAlert(context);
            if (alarm != null) {
                enableAlert(context, alarm, alarm.time);
            } else {
                disableAlert(context);
            }
        }
    }

    /**
     * Sets alert in AlarmManger and StatusBar.  This is what will
     * actually launch the alert when the alarm triggers.
     *
     * @param alarm          Alarm.
     * @param atTimeInMillis milliseconds since epoch
     */
    private static void enableAlert(Context context, final Alarm alarm, final long atTimeInMillis) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Log.v("Kunxun", "** setAlert id " + alarm.id + " atTime " + atTimeInMillis);

        Intent intent = new Intent(ALARM_ALERT_ACTION);
        // This is a slight hack to avoid an exception in the remote
        // AlarmManagerService process. The AlarmManager adds extra data to
        // this Intent which causes it to inflate. Since the remote process
        // does not know about the Alarm class, it throws a
        // ClassNotFoundException.
        //
        // To avoid this, we marshall the data ourselves and then parcel a plain
        // byte[] array. The AlarmReceiver class knows to build the Alarm
        // object from the byte[] array.
        Parcel out = Parcel.obtain();
        alarm.writeToParcel(out, 0);
        out.setDataPosition(0);
        intent.putExtra(ALARM_RAW_DATA, out.marshall());

        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        assert am != null;
        am.set(AlarmManager.RTC_WAKEUP, atTimeInMillis, sender);

        setStatusBarIcon(context,true);

//        Calendar c = Calendar.getInstance();
//        c.setTimeInMillis(atTimeInMillis);
//        String format = android.text.format.DateFormat.is24HourFormat(context) ? "E k:mm" : "E h:mm aa";
//        String timeString = (String) android.text.format.DateFormat.format(format, c);
//
//        saveNextAlarm(context, timeString);
    }

    private static void disableAlert(Context context) {
        AlarmManager am = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0,
                new Intent(ALARM_ALERT_ACTION),PendingIntent.FLAG_CANCEL_CURRENT);

        am.cancel(sender);
        setStatusBarIcon(context,false);
//        saveNextAlarm(context, "");
    }

    /**
     * Tells the StatusBar whether the alarm is enabled or disabled
     */
    private static void setStatusBarIcon(Context context, boolean enabled) {
        Intent alarmChanged = new Intent("android.intent.action.ALARM_CHANGED");
        alarmChanged.putExtra("alarmSet", enabled);
        context.sendBroadcast(alarmChanged);
    }

//    /**
//     * Save time of the next alarm, as a formatted string, into the system
//     * settings so those who care can make use of it.
//     */
//    private static void saveNextAlarm(final Context context, String timeString) {
//        Settings.System.putString(context.getContentResolver(), Settings.System.NEXT_ALARM_FORMATTED, timeString);
//    }

}
