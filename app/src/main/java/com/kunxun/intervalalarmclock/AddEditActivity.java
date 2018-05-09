package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

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
        if (mId != -1) {
            title.setText(R.string.edit_layout_name);
        } else {
            title.setText(R.string.add_layout_name);
        }

        Button btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveAlarm();
                finish();
            }
        });

        Button btnCancel = findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Button btnDelete = findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteAlarm();
            }
        });
    }

    public static class PrefsAlarmFragment extends PreferenceFragment {

        public Preference startTimePref, endTimePref;
        public SwitchPreference intervalEnabledPref, vibratePref;
        public MultiSelectListPreference daysOfWeekPref;
        public EditTextPreference intervalPref, alarmNamePref;
        public RingtonePreference alertPref;
        private Alarm mAlarm = new Alarm();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_alarm);
            startTimePref = findPreference("start_time");
            endTimePref = findPreference("end_time");
            intervalPref = (EditTextPreference) findPreference("interval");
            intervalEnabledPref = (SwitchPreference) findPreference("interval_enable");
            daysOfWeekPref = (MultiSelectListPreference) findPreference("days_of_week");
            daysOfWeekPref.setEntries(Alarm.DaysOfWeek.DAY_STRING_MAP);
            daysOfWeekPref.setEntryValues(Alarm.DaysOfWeek.DAY_SHORT_STRING_MAP);

            vibratePref = (SwitchPreference) findPreference("vibrate");
            alertPref = (RingtonePreference) findPreference("ringtone");
            alarmNamePref = (EditTextPreference) findPreference("name");

            int mId = getActivity().getIntent().getIntExtra(Alarms.ALARM_ID, -1);
            if (mId != -1) {
                mAlarm = Alarms.getAlarm(getActivity().getContentResolver(), mId);
                // Bad alarm, bail to avoid a NPE.
                if (mAlarm == null) {
                    getActivity().finish();
                    return;
                }
            }
            updatePrefs();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {


            if (preference == startTimePref || preference == endTimePref) {
                int hourOfDay = 8, minute =0;
                if (mAlarm.id !=-1){
                    if (preference == startTimePref){
                        hourOfDay = mAlarm.starthour;
                        minute = mAlarm.startminutes;
                    }
                    else{
                        hourOfDay = mAlarm.endhour;
                        minute = mAlarm.endminutes;
                    }
                }
                TimePickerDialog timeDialog = new TimePickerDialog(getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker timePicker, int i, int i1) {
                                preference.setSummary(i + ":" + i1);
                                if (preference == startTimePref) {
                                    mAlarm.starthour = i;
                                    mAlarm.startminutes = i1;
                                } else {
                                    mAlarm.endhour = i;
                                    mAlarm.endminutes = i1;
                                }

                            }
                        }, hourOfDay, minute, true);

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

                    mAlarm.daysOfWeek.set(o.toString());
                    daysOfWeekPref.setValues(mAlarm.daysOfWeek.getSetSelected());
                    daysOfWeekPref.setSummary(mAlarm.daysOfWeek.toString(getActivity().getApplicationContext(), true));
                    return true;
                }
            });

            alertPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Uri uri = Uri.parse(o.toString());
                    alertPref.setSummary(RingtoneManager.getRingtone(getActivity(), uri).getTitle(getActivity()));
                    mAlarm.alert = uri;
                    return true;
                }
            });

            intervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    intervalPref.setSummary(newValue.toString() + " 分钟");
                    mAlarm.interval = Integer.parseInt(newValue.toString());
                    return true;
                }
            });

            alarmNamePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    alarmNamePref.setSummary(newValue.toString());
                    mAlarm.name = newValue.toString();
                    return true;
                }
            });
        }

        private void updatePrefs() {

            String tmpStr = mAlarm.starthour + ":" + ((mAlarm.startminutes < 10) ? "0" + mAlarm.startminutes : mAlarm.startminutes);
            startTimePref.setSummary(tmpStr);

            tmpStr = mAlarm.endhour + ":" + ((mAlarm.endminutes < 10) ? "0" + mAlarm.endminutes : mAlarm.endminutes);
            endTimePref.setSummary(tmpStr);

            intervalEnabledPref.setChecked(mAlarm.intervalenabled);
            intervalPref.setSummary(mAlarm.interval + " 分钟");
            //intervalPref.setText(Integer.toString(mAlarm.interval));
            vibratePref.setChecked(mAlarm.vibrate);
            alertPref.setSummary(RingtoneManager.getRingtone(getActivity(), mAlarm.alert).getTitle(getActivity()));
            alarmNamePref.setSummary(mAlarm.name);

            daysOfWeekPref.setSummary(mAlarm.daysOfWeek.toString(getActivity().getApplicationContext(), true));
            daysOfWeekPref.setValues(mAlarm.daysOfWeek.getSetSelected());
        }
    }

    private void saveAlarm() {
        Alarm alarm = new Alarm();
        alarm.id = mId;
        alarm.starthour = prefsAlarmFragment.mAlarm.starthour;
        alarm.startminutes = prefsAlarmFragment.mAlarm.startminutes;
        alarm.intervalenabled = prefsAlarmFragment.intervalEnabledPref.isChecked();
        alarm.interval = prefsAlarmFragment.mAlarm.interval;
        alarm.endhour = prefsAlarmFragment.mAlarm.endhour;
        alarm.endminutes = prefsAlarmFragment.mAlarm.endminutes;
        alarm.daysOfWeek = prefsAlarmFragment.mAlarm.daysOfWeek;
        alarm.vibrate = prefsAlarmFragment.vibratePref.isChecked();
        alarm.alert = prefsAlarmFragment.mAlarm.alert;
        alarm.name = prefsAlarmFragment.mAlarm.name;

        if (alarm.id == -1) {
            Alarms.addAlarm(this, alarm);
            mId = alarm.id;
        } else {
            Alarms.updateAlarm(this, alarm);
        }
    }

    private void deleteAlarm() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_alarm))
                .setMessage(getString(R.string.delete_alarm_confirm))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Alarms.deleteAlarm(AddEditActivity.this, mId);
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
