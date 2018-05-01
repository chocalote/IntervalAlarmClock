package com.kunxun.intervalalarmclock;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.view.View;

public class SettingActivity extends Activity {

    private static final int ALARM_STREAM_TYPE_BIT =
            1 << AudioManager.STREAM_ALARM;
    private static final String KEY_ALARM_IN_SILENT_MODE =
            "alarm_in_silent_mode";
    static final String KEY_VOLUME_BEHAVIOR =
            "volume_button_setting";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        getFragmentManager().beginTransaction()
                .replace(R.id.prefs_setting, new PrefsSettingFragment()).commit();
    }

    public static class PrefsSettingFragment extends PreferenceFragment {

        public ListPreference alertDurationPref, snoozeDurationPref, volumeButtonPref;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.prefs_setting);

            alertDurationPref = (ListPreference) findPreference("alert_duration");
            snoozeDurationPref = (ListPreference) findPreference("snooze_duration");
            volumeButtonPref = (ListPreference) findPreference("volume_button");

            alertDurationPref.setSummary("响铃" + alertDurationPref.getValue() + "分钟后自动停止");
            snoozeDurationPref.setSummary("间隔" + snoozeDurationPref.getValue() + "分钟响铃");
            volumeButtonPref.setSummary(getResources().getString(R.string.volume_button_summary) + ": " + volumeButtonPref.getValue());
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            alertDurationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    alertDurationPref.setSummary("响铃" + newValue.toString() + "分钟后自动停止");
                    return false;
                }
            });
            snoozeDurationPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    snoozeDurationPref.setSummary("间隔" + newValue.toString() + "分钟响铃");
                    return false;
                }
            });

            volumeButtonPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    volumeButtonPref.setSummary(getResources().getString(R.string.volume_button_summary) + ": " + newValue.toString());
                    return false;
                }
            });
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (KEY_ALARM_IN_SILENT_MODE.equals(preference.getKey())) {
                SwitchPreference silentModePref = (SwitchPreference) preference;
                int ringerModeStreamTypes = Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);
                if (silentModePref.isChecked()) {
                    ringerModeStreamTypes &= ~ALARM_STREAM_TYPE_BIT;
                } else {
                    ringerModeStreamTypes |= ALARM_STREAM_TYPE_BIT;
                }

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    if (!Settings.System.canWrite(getContext())) {
//                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
//                        intent.setData(Uri.parse("package:" + getContext().getPackageName()));
//                        getContext().startActivity(intent);
//                    } else {
//
//                        Settings.System.putInt(getActivity().getContentResolver(),
//                                Settings.System.MODE_RINGER_STREAMS_AFFECTED, ringerModeStreamTypes);
//                    }
//                }
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
    }
}
