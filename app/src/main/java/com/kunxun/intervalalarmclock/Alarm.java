package com.kunxun.intervalalarmclock;

import android.content.Context;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public final class Alarm implements Parcelable {

    public static final Parcelable.Creator<Alarm> CREATOR = new Parcelable.Creator<Alarm>() {
        @Override
        public Alarm createFromParcel(Parcel parcel) {
            return new Alarm(parcel);
        }

        @Override
        public Alarm[] newArray(int size) {
            return new Alarm[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flages) {
        parcel.writeInt(id);
        parcel.writeInt(enabled ? 1 : 0);
        parcel.writeInt(starthour);
        parcel.writeInt(startminutes);
        parcel.writeInt(endhour);
        parcel.writeInt(endminutes);
        parcel.writeInt(daysOfWeek.getCoded());
        parcel.writeInt(interval);
        parcel.writeInt(intervalenabled ? 1 : 0);
        parcel.writeInt(vibrate ? 1 : 0);
        parcel.writeString(message);
        parcel.writeParcelable(alert, flages);
        parcel.writeInt(silent ? 1 : 0);
    }

    public int id;
    public boolean enabled;
    public int starthour;
    public int startminutes;
    public int endhour;
    public int endminutes;
    public DaysOfWeek daysOfWeek;
    public int interval;
    public boolean intervalenabled;
    public boolean vibrate;
    public String message;
    public Uri alert;
    public boolean silent;


    public static class Columns implements BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://com.kunxun.intervalalarmclock/alarm");
        public static final String START_HOUR = "starthour";
        public static final String START_MINUTES = "startminutes";
        public static final String END_HOUR = "endhour";
        public static final String END_MINUTES = "endminutes";
        public static final String DAYS_OF_WEEK = "daysofweek";
        public static final String INTERVAL = "interval";
        public static final String INTERVAL_ENABLED = "intervalenabled";
        public static final String ENABLED = "enabled";
        public static final String VIBRATE = "vibrate";
        public static final String MESSAGE = "message";
        public static final String ALERT = "alert";
        public static final String DEFAULT_SORT_ORDER = START_HOUR + ", " + START_MINUTES + " ASC";
        public static final String WHERE_ENABLED = ENABLED + " = 1";
        public static final String WHERE_INTERVAL_ENABLED = INTERVAL_ENABLED + " = 1";
        static final String[] ALARM_QUERY_COLUMNS = {_ID, START_HOUR, START_MINUTES, END_HOUR, END_MINUTES,
                DAYS_OF_WEEK, INTERVAL, INTERVAL_ENABLED, ENABLED, VIBRATE, MESSAGE, ALERT};

        public static final int ALARM_ID_INDEX = 0;
        public static final int ALARM_START_HOUR_INDEX = 1;
        public static final int ALARM_START_MINUTES_INDEX = 2;
        public static final int ALARM_END_HOUR_INDEX = 3;
        public static final int ALARM_END_MINUTES_INDEX = 4;
        public static final int ALARM_DAYS_OF_WEEK_INDEX = 5;
        public static final int ALARM_INTERVAL_INDEX = 6;
        public static final int ALARM_INTERVAL_ENABLED_INDEX = 7;
        public static final int ALARM_ENABLED_INDEX = 8;
        public static final int ALARM_VIBRATE_INDEX = 9;
        public static final int ALARM_MESSAGE_INDEX = 10;
        public static final int ALARM_ALERT_INDEX = 11;
    }

    /*
     * Days of week code as a single int.
     * 0x00: no day
     * 0x01: Monday
     * 0x02: Tuesday
     * 0x04: Wednesday
     * 0x08: Thursday
     * 0x10: Friday
     * 0x20: Saturday
     * 0x40: Sunday
     */
    public static final class DaysOfWeek {
        private static int[] DAY_MAP = new int[]{
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY,
                Calendar.SATURDAY,
                Calendar.SUNDAY,
        };

        private int mDays;

        DaysOfWeek(int days) {
            mDays = days;
        }

        public String toString(Context context, boolean showNever) {
            StringBuilder ret = new StringBuilder();

            switch (mDays) {
                case 0:
                    return showNever ? context.getText(R.string.never).toString() : "";
                case 0x7f:
                    return context.getText(R.string.everyday).toString();
            }

            //count selected days
            int daycount = 0, days = mDays;
            while (days > 0) {
                if ((days & 1) == 1) daycount++;
                days >>= 1;
            }

            //short or long form?
            DateFormatSymbols dfs = new DateFormatSymbols();
            String[] dayList = (daycount > 1) ? dfs.getShortWeekdays() : dfs.getWeekdays();

            //selected days
            for (int i = 0; i < 7; i++) {
                if ((mDays & (1 << i)) != 0) {
                    ret.append(dayList[DAY_MAP[i]]);
                    daycount--;
                    if (daycount > 0) ret.append(", ");
                }
            }
            return ret.toString();
        }

        public int getCoded() {
            return mDays;
        }
    }

    public Alarm(Parcel parcel) {
        id = parcel.readInt();
        enabled = parcel.readInt() == 1;
        starthour = parcel.readInt();
        startminutes = parcel.readInt();
        endhour = parcel.readInt();
        endminutes = parcel.readInt();
        daysOfWeek = new DaysOfWeek(parcel.readInt());
        interval = parcel.readInt();
        intervalenabled = parcel.readInt() == 1;
        vibrate = parcel.readInt() == 1;
        message = parcel.readString();
        alert = parcel.readParcelable(null);
        silent = parcel.readInt() == 1;
    }

    //create a default alarm at the current time.
    public Alarm() {
        id = -1;
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        starthour = c.get(Calendar.HOUR_OF_DAY);
        startminutes = c.get(Calendar.MINUTE);
        interval = 0;
        intervalenabled = false;
        enabled = true;
        vibrate = true;
        daysOfWeek = new DaysOfWeek(0);
        alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
    }

    public Alarm(Cursor cursor) {
        id = cursor.getInt(Columns.ALARM_ID_INDEX);
        enabled = cursor.getInt(Columns.ALARM_ENABLED_INDEX) == 1;
        starthour = cursor.getInt(Columns.ALARM_START_HOUR_INDEX);
        startminutes = cursor.getInt(Columns.ALARM_START_MINUTES_INDEX);
        endhour = cursor.getInt(Columns.ALARM_END_HOUR_INDEX);
        endminutes = cursor.getInt(Columns.ALARM_END_MINUTES_INDEX);
        daysOfWeek = new DaysOfWeek(cursor.getInt(Columns.ALARM_DAYS_OF_WEEK_INDEX));
        interval = cursor.getInt(Columns.ALARM_INTERVAL_INDEX);
        intervalenabled = cursor.getInt(Columns.ALARM_INTERVAL_ENABLED_INDEX) == 1;
        vibrate = cursor.getInt(Columns.ALARM_VIBRATE_INDEX) == 1;
        message = cursor.getString(Columns.ALARM_MESSAGE_INDEX);
        String alertStr = cursor.getString(Columns.ALARM_ALERT_INDEX);

        if (Alarms.ALARM_ALERT_SILENT.equals(alertStr)) {
            if (true)
                Log.v("Kunxun", "Alarm is marked as silent");
        } else {
            if (alertStr != null && alertStr.length() != 0) {
                alert = Uri.parse(alertStr);
            }
            //if the database alert is null or failed to parse, use the default alert
            if (alert == null) {
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
        }
    }

    public String getMessageOrDefault(Context context){
        if(message == null || message.length() ==0)
        {
            return context.getString(R.string.default_label);
        }
        return message;
    }

}
