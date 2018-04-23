package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.HashSet;
import java.util.Set;

public class AddEditActivity extends Activity {

    private int mId;
    private PrefsAlarmFragment prefsAlarmFragment = new PrefsAlarmFragment();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);


        getFragmentManager().beginTransaction()
                .replace(R.id.llPF, prefsAlarmFragment).commit();

        TextView title = findViewById(R.id.textViewTitle);

        mId = getIntent().getIntExtra(Alarms.ALARM_ID, -1);
        Log.v("Kunxun", "In AddEditAlarm, alarm id = " + mId);
        if(mId != -1)
        {
            title.setText(R.string.edit_layout_name);
        }
        else {
            title.setText(R.string.add_layout_name);
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAlarm();
            }
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

    }

    public static class PrefsAlarmFragment extends PreferenceFragment {

        public Preference startTimePref, endTimePref,  alertPref;
        public SwitchPreference intervalEnabledPref, vibratePref;
        public MultiSelectListPreference daysOfWeekPref;
        public EditTextPreference intervalPref,alarmNamePref;
        private int startHour = 8, startMinutes, endHour=18, endMinutes;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_alarm);
            startTimePref = findPreference("start_time");
            endTimePref = findPreference("end_time");
            intervalPref = (EditTextPreference)findPreference("interval");
            intervalEnabledPref = (SwitchPreference) findPreference("interval_enable");
            daysOfWeekPref = (MultiSelectListPreference)findPreference("days_of_week");
            daysOfWeekPref.setEntries(Alarm.DaysOfWeek.DAY_STRING_MAP);
            daysOfWeekPref.setEntryValues(Alarm.DaysOfWeek.DAY_SHORT_STRING_MAP);

            vibratePref = (SwitchPreference) findPreference("vibrate");
            alertPref = findPreference("ringtone");
            alarmNamePref =(EditTextPreference) findPreference("name");

            int mId = getActivity().getIntent().getIntExtra(Alarms.ALARM_ID, -1);


            if (mId != -1) {
                Alarm alarm = Alarms.getAlarm(getActivity().getContentResolver(), mId);
                // Bad alarm, bail to avoid a NPE.
                if (alarm == null) {
                    getActivity().finish();
                    return;
                }
                updatePrefs(alarm);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {
            if (preference == startTimePref || preference == endTimePref) {
                TimePickerDialog timeDialog = new TimePickerDialog(getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                                preference.setSummary(i + ":" + i1);
                                if (preference == startTimePref) {
                                    startHour = i;
                                    startMinutes = i1;
                                } else {
                                    endHour = i;
                                    endMinutes = i1;
                                }

                            }
                        }, 0, 0, true);
                timeDialog.show();
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);


            daysOfWeekPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    daysOfWeekPref.setSummary(o.toString());
                    return false;
                }
            });

            alertPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Uri uri = Uri.parse(o.toString());
                    alertPref.setSummary(RingtoneManager.getRingtone(getActivity(), uri).getTitle(getActivity()));
                    return false;
                }
            });

        }

        private void updatePrefs(Alarm alarm) {

            startHour = alarm.starthour;
            startMinutes = alarm.startminutes;
            endHour = alarm.endhour;
            endMinutes = alarm.endminutes;

            String tmpStr = alarm.starthour + ":" + ((alarm.startminutes < 10) ? "0" + alarm.startminutes : alarm.startminutes);
            startTimePref.setSummary(tmpStr);

            tmpStr = alarm.endhour + ":" + ((alarm.endminutes < 10) ? "0" + alarm.endminutes : alarm.endminutes);
            endTimePref.setSummary(tmpStr);

            intervalEnabledPref.setChecked(alarm.intervalenabled);
            intervalPref.setSummary(alarm.interval + " minutes");
            vibratePref.setChecked(alarm.vibrate);
            alertPref.setSummary(RingtoneManager.getRingtone(getActivity(), alarm.alert).getTitle(getActivity()));
            alarmNamePref.setSummary(alarm.name);

            daysOfWeekPref.setSummary(alarm.daysOfWeek.toString(getActivity().getApplicationContext(),true));
            daysOfWeekPref.setValues(alarm.daysOfWeek.getSetSelected());
        }


    }

    private void saveAlarm()
    {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.starthour = prefsAlarmFragment.startHour;
        alarm.startminutes = prefsAlarmFragment.startMinutes;
        alarm.intervalenabled = prefsAlarmFragment.intervalEnabledPref.isChecked();
        alarm.interval= Integer.parseInt(prefsAlarmFragment.intervalPref.getText().toString());
        alarm.endhour = prefsAlarmFragment.endHour;
        alarm.endminutes = prefsAlarmFragment.endMinutes;
        Set<String> selectStr = prefsAlarmFragment.daysOfWeekPref.getValues();



        if(mId == -1)
        {

        }
    }

}
