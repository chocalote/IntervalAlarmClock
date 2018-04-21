package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TimePicker;

public class AddEditActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        getFragmentManager().beginTransaction()
                .replace(R.id.llPF, new PrefsAlarmFragment()).commit();
    }

    public static class PrefsAlarmFragment extends PreferenceFragment {

        public Preference startTimePref, endTimePref;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.prefs_alarm);
            startTimePref = findPreference("start_time");
            endTimePref = findPreference("end_time");
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {
            if (preference == startTimePref || preference == endTimePref)
            {
                TimePickerDialog timdDialog = new TimePickerDialog(getActivity(),
                        new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        preference.setSummary(i+":"+i1);
                    }
                },0,0,true);
                timdDialog.show();
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            final Preference daysofweek = findPreference("days_of_week");
            daysofweek.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    daysofweek.setSummary(o.toString());
                    return false;
                }
            });

            final Preference alert = findPreference("ringtone");
            alert.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Uri uri = Uri.parse(o.toString());
                    alert.setSummary(RingtoneManager.getRingtone(getActivity(),uri).getTitle(getActivity()));
                    return false;
                }
            });

        }


    }





}
