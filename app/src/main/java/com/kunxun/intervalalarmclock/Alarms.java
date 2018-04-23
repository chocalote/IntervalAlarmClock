package com.kunxun.intervalalarmclock;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;

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
}
