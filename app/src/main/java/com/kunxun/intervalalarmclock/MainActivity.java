package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    static final String PREFERENCES = "IntervalAlarmClock";
    private LayoutInflater mFactory;
    private Cursor mCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFactory = LayoutInflater.from(this);
        mCursor = Alarms.getAlarmCursor(getContentResolver());
        updateLayout();
    }


    private void updateLayout() {
        ListView mListView = findViewById(R.id.mListView);

        AlarmAdapter adapter = new AlarmAdapter(this, mCursor);
        mListView.setAdapter(adapter);

        //Set the alarm
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
                intent.putExtra(Alarms.ALARM_ID, (int) l);
                startActivity(intent);
            }
        });

        Button btnAdd, btnSetting;
        btnAdd = findViewById(R.id.btnAdd);
        //New an alarm
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AddEditActivity.class));
            }
        });
        btnSetting = findViewById(R.id.btnSetting);
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(MainActivity.this, SettingActivity.class));
            }
        });
    }

    private class AlarmAdapter extends CursorAdapter {

        private AlarmAdapter(Context context, Cursor cursor) {
            super(context, cursor);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mFactory.inflate(R.layout.list_alarm, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            final Alarm alarm = new Alarm(cursor);
            final TextView itemTitle = view.findViewById(R.id.Item_Title);
            String tmpStr = alarm.starthour + ":" + ((alarm.startminutes < 10) ? "0" + alarm.startminutes : alarm.startminutes);
            if (alarm.intervalenabled) {
                tmpStr += "~" + alarm.endhour + ":" + ((alarm.endminutes < 10) ? "0" + alarm.endminutes : alarm.endminutes) + " ";
                tmpStr += alarm.interval + " minutes interval";
            }

            itemTitle.setText(tmpStr);
            itemTitle.setEnabled(alarm.enabled);

            final TextView itemText = view.findViewById(R.id.Item_Text);
            tmpStr = alarm.name + ", " + alarm.daysOfWeek.toString(MainActivity.this, false);

            itemText.setText(tmpStr);
            itemText.setEnabled(alarm.enabled);

            final Switch itemEnabled = view.findViewById(R.id.Item_Enabled);
            itemEnabled.setChecked(alarm.enabled);
            itemEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    itemTitle.setEnabled(b);
                    itemText.setEnabled(b);
                    //Alarms.enableAlarm(MainActivity.this, alarm.id, b);
                    if(b)
                    {
                        Toast.makeText(MainActivity.this, "aa",Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        mCursor.close();
    }
}
