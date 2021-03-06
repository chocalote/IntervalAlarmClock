package com.kunxun.intervalalarmclock;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.Objects;


public class AlarmProvider extends ContentProvider {

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "alarms.db";
        private static final int version = 1;

        private DatabaseHelper(Context context) {
            super(context, DB_NAME, null, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE alarms (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "starthour INTEGER NOT NULL, " +
                    "startminutes INTEGER NOT NULL, " +
                    "endhour INTEGER, " +
                    "endminutes INTEGER, " +
                    "daysofweek INTEGER, " +
                    "interval INTEGER, " +
                    "intervalenabled INTEGER, " +
                    "enabled INTEGER, " +
                    "vibrate INTEGER, " +
                    "name TEXT, " +
                    "alert TEXT," +
                    "time INTEGER);";

            db.execSQL(sql);

            // insert default alarms
            String insertSQL = "INSERT INTO alarms " +
                    "(starthour, startminutes, endhour, endminutes, daysofweek, " +
                    "interval, intervalenabled, enabled, vibrate, name, alert, time) " +
                    "VALUES ";
            db.execSQL(insertSQL + "(8, 30, 18, 0, 31, 60, 1, 1, 1, '闹钟', '', 0);");
            db.execSQL(insertSQL + "(9, 00, 17, 30, 96, 30, 1, 0, 1, '闹钟', '', 0);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            Log.v("Lily",
                    "Upgrading alarms database from version " +
                            oldVersion + " to " + newVersion +
                            ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS alarms");
            onCreate(db);
        }

    }

    private SQLiteOpenHelper mDatabaseHelper;

    private static final int ALARMS = 1;
    private static final int ALARMS_ID = 2;
    private static final UriMatcher sURLMatcher = new UriMatcher(
            UriMatcher.NO_MATCH);

    static {
        sURLMatcher.addURI("com.kunxun.intervalalarmclock", "alarm", ALARMS);
        sURLMatcher.addURI("com.kunxun.intervalalarmclock", "alarm/#", ALARMS_ID);
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri url, String[] projectionIn,
                        String selection, String[] selectionArgs, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        int match = sURLMatcher.match(url);
        switch (match) {
            case ALARMS:
                qb.setTables("alarms");
                break;
            case ALARMS_ID:
                qb.setTables("alarms");
                qb.appendWhere("_id=");
                qb.appendWhere(url.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + url);
        }

        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        Cursor ret = qb.query(db, projectionIn, selection, selectionArgs,
                null, null, sort);

        if (ret == null) {
            Log.v("Lily", "Alarms.query: failed");
        } else {
            ret.setNotificationUri(Objects.requireNonNull(getContext()).getContentResolver(), url);
        }

        return ret;
    }

    @Override
    public String getType(@NonNull Uri url) {

        int match = sURLMatcher.match(url);
        switch (match) {
            case ALARMS:
                return "vnd.android.cursor.dir/alarms";
            case ALARMS_ID:
                return "vnd.android.cursor.item/alarms";
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public int update(@NonNull Uri url, ContentValues values,
                      String where, String[] whereArgs){
        int count;
        long rowId;
        int match = sURLMatcher.match(url);
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        switch (match) {
            case ALARMS_ID: {
                String segment = url.getPathSegments().get(1);
                rowId = Long.parseLong(segment);
                count = db.update("alarms", values, "_id=" + rowId, null);
                break;
            }
            default: {
                throw new UnsupportedOperationException(
                        "Cannot update URL: " + url);
            }
        }
        Log.v("Lily", "*** update() alarmId: " + rowId + " url " + url);
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(url, null);
        return count;
    }

    @Override
    public Uri insert(@NonNull Uri url, ContentValues initialValues) {
        if (sURLMatcher.match(url) != ALARMS) {
            throw new IllegalArgumentException("Cannot insert into URL: " + url);
        }

        ContentValues values = new ContentValues(initialValues);

        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        long rowId = db.insert("alarms", Alarm.Columns.NAME, values);
        if (rowId < 0) {
            throw new SQLException("Failed to insert row into " + url);
        }
        Log.v("Lily", "Added alarm rowId = " + rowId);

        Uri newUrl = ContentUris.withAppendedId(Alarm.Columns.CONTENT_URI, rowId);

        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(newUrl, null);
        return newUrl;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String [] whereArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        int count;

        switch (sURLMatcher.match(uri)) {
            case ALARMS:
                count = db.delete("alarms", where, whereArgs);
                break;
            case ALARMS_ID:
                String segment = uri.getPathSegments().get(1);
                if (TextUtils.isEmpty(where)) {
                    where = "_id = " + segment;
                } else {
                    where = "_id = " + segment + " AND (" + where + ")";
                }
                count = db.delete("alarms", where, whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + uri);
        }
        Objects.requireNonNull(getContext()).getContentResolver().notifyChange(uri, null);
        return count;
    }

}


