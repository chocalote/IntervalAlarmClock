package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.TimePicker;

import java.util.HashSet;
import java.util.Set;

public class AddEditActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);


        getFragmentManager().beginTransaction()
                .replace(R.id.llPF, new PrefsAlarmFragment()).commit();
    }

    public static class PrefsAlarmFragment extends PreferenceFragment {

        public Preference startTimePref, endTimePref, intervalPref, alertPref, alarmnamePref;
        public SwitchPreference intvlEabledPref, vibratePref;
        public MultiSelectListPreference daysofweekPref;
        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_alarm);
            startTimePref = findPreference("start_time");
            endTimePref = findPreference("end_time");
            intervalPref = findPreference("interval");
            intvlEabledPref = (SwitchPreference) findPreference("interval_enable");
            daysofweekPref = (MultiSelectListPreference)findPreference("days_of_week");
            vibratePref = (SwitchPreference) findPreference("vibrate");
            alertPref = findPreference("ringtone");
            alarmnamePref = findPreference("name");

            Intent intent = getActivity().getIntent();
            int mId = intent.getIntExtra(Alarms.ALARM_ID, -1);
            Log.v("Kunxun", "In AddEditAlarm, alarm id = " + mId);

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
                            }
                        }, 0, 0, true);
                timeDialog.show();
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);


            daysofweekPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    daysofweekPref.setSummary(o.toString());
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
            String tmpStr = alarm.starthour + ":" + ((alarm.startminutes < 10) ? "0" + alarm.startminutes : alarm.startminutes);
            startTimePref.setSummary(tmpStr);
            tmpStr = alarm.endhour + ":" + ((alarm.endminutes < 10) ? "0" + alarm.endminutes : alarm.endminutes);
            endTimePref.setSummary(tmpStr);
            intvlEabledPref.setChecked(alarm.intervalenabled);
            intervalPref.setSummary(alarm.interval + " minutes");
            vibratePref.setChecked(alarm.vibrate);
            alertPref.setSummary(RingtoneManager.getRingtone(getActivity(), alarm.alert).getTitle(getActivity()));
            alarmnamePref.setSummary(alarm.name);

            daysofweekPref.setSummary(alarm.daysOfWeek.toString(getActivity().getApplicationContext(),true));
            Set<String>  entryChecked = new HashSet<>();
            entryChecked.add("Monday");
            daysofweekPref.setValues(entryChecked);
        }
    }
}
