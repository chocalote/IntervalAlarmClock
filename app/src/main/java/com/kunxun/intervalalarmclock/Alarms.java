package com.kunxun.intervalalarmclock;

import android.content.ContentResolver;
import android.database.Cursor;

public class Alarms {


    public static final String ALARM_ALERT_SILENT = "silent";


    public static Cursor getAlarmCursor(ContentResolver contentResolver){
        Cursor cursor = contentResolver.query(Alarm.Columns.CONTENT_URI, Alarm.Columns.ALARM_QUERY_COLUMNS,
                null,null,Alarm.Columns.DEFAULT_SORT_ORDER);
        return cursor;
    }

}
