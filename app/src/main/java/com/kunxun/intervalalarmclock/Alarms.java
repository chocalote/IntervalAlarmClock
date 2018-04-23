package com.kunxun.intervalalarmclock;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class Alarms {


    public static final String ALARM_ALERT_SILENT = "silent";
    public static final String ALARM_ID = "alarm_id";


    public static Cursor getAlarmCursor(ContentResolver contentResolver) {
        return contentResolver.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null, null, Alarm.Columns.DEFAULT_SORT_ORDER);
    }

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

    public static void addAlarm(Context context, Alarm alarm) {
        ContentValues values = createContentValues(alarm);
        Uri uri = context.getContentResolver().insert(Alarm.Columns.CONTENT_URI, values);
        alarm.id = (int) ContentUris.parseId(uri);
    }

    public static void updateAlarm(Context context, Alarm alarm){
        ContentValues values = createContentValues(alarm);
        ContentResolver resolver = context.getContentResolver();
        resolver.update(ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, alarm.id),values,null,null);
    }

    private static ContentValues createContentValues(Alarm alarm) {
        ContentValues values = new ContentValues(11);

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

        return values;
    }
}
