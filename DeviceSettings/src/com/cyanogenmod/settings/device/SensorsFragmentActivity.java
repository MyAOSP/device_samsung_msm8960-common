/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;
import android.util.Log;

public class SensorsFragmentActivity extends PreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "GalaxyS3Parts_General";

    private static final String FILE_USE_GYRO_CALIB = "/sys/class/sec/gsensorcal/calibration";
    private static final String FILE_TOUCHKEY_TOGGLE = "/sys/class/leds/button-backlight/max_brightness";
    private static final String FILE_BLN_TOGGLE = "/sys/class/misc/backlightnotification/enabled";
    private static final int KEY_LIGHT_DEFAULT_DURATION = 5;

    private static final boolean sHasTouchkeyToggle = Utils.fileExists(FILE_TOUCHKEY_TOGGLE);
    private static final boolean sHasTouchkeyBLN = Utils.fileExists(FILE_BLN_TOGGLE);

    private ListPreference mLightDuration;
    private CheckBoxPreference mEnableHardwareKeys;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.sensors_preferences);

        PreferenceCategory prefs = (PreferenceCategory) findPreference(DeviceSettings.CATEGORY_TOUCHKEY);
        if (!sHasTouchkeyToggle) {
            prefs.removePreference(findPreference(DeviceSettings.KEY_TOUCHKEY_LIGHT));
            prefs.removePreference(findPreference(DeviceSettings.HARDWARE_KEYS_LIGHT_DURATION));
            prefs.removePreference(findPreference(DeviceSettings.HARDWARE_KEYS_ENABLE));
        } else {
            mLightDuration = (ListPreference) findPreference(DeviceSettings.HARDWARE_KEYS_LIGHT_DURATION);
            mEnableHardwareKeys = (CheckBoxPreference) findPreference(DeviceSettings.HARDWARE_KEYS_ENABLE);

            int keyLightDuration = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.KEY_HARDWARE_LIGHT_DURATION, KEY_LIGHT_DEFAULT_DURATION);
            mLightDuration.setValue(Integer.toString(keyLightDuration));
            mLightDuration.setSummary(mLightDuration.getEntry());
            mLightDuration.setOnPreferenceChangeListener(this);

            int disableHardwareKeys = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.HARDWARE_KEY_DISABLE, 0);

            mEnableHardwareKeys.setChecked(disableHardwareKeys == 0);

        }
        if (!sHasTouchkeyBLN) {
            prefs.removePreference(findPreference(DeviceSettings.KEY_TOUCHKEY_BLN));
        }
        if (prefs.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(prefs);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        String boxValue;
        String key = preference.getKey();

        Log.w(TAG, "key: " + key);

        if (key.compareTo(DeviceSettings.KEY_USE_GYRO_CALIBRATION) == 0) {
            boxValue = (((CheckBoxPreference)preference).isChecked() ? "1" : "0");
            Utils.writeValue(FILE_USE_GYRO_CALIB, boxValue);
        } else if (key.compareTo(DeviceSettings.KEY_CALIBRATE_GYRO) == 0) {
            // when calibration data utilization is disablen and enabled back,
            // calibration is done at the same time by driver
            Utils.writeValue(FILE_USE_GYRO_CALIB, "0");
            Utils.writeValue(FILE_USE_GYRO_CALIB, "1");
            Utils.showDialog((Context)getActivity(), "Calibration done", "The gyroscope has been successfully calibrated!");
        } else if (key.compareTo(DeviceSettings.KEY_TOUCHKEY_LIGHT) == 0) {
            Utils.writeValue(FILE_TOUCHKEY_TOGGLE, ((CheckBoxPreference)preference).isChecked() ? "255" : "0");
        } else if (key.compareTo(DeviceSettings.KEY_TOUCHKEY_BLN) == 0) {
            Utils.writeValue(FILE_BLN_TOGGLE, ((CheckBoxPreference)preference).isChecked() ? "1" : "0");
        } else if (key.compareTo(DeviceSettings.HARDWARE_KEYS_ENABLE) == 0) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HARDWARE_KEY_DISABLE, 
                    (((CheckBoxPreference)preference).isChecked() ? 0: 1));

            Utils.writeValue(FILE_TOUCHKEY_TOGGLE, ((CheckBoxPreference)preference).isChecked() ? "255" : "0");
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mLightDuration) {
            int value = Integer.valueOf((String) newValue);
            int index = mLightDuration.findIndexOfValue((String) newValue);
            mLightDuration.setSummary(
                    mLightDuration.getEntries()[index]);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEY_HARDWARE_LIGHT_DURATION, value);
            return true;
        }
        
        return false;
    } 

    public static void restore(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // When use gyro calibration value is set to 1, calibration is done at the same time, which
        // means it is reset at each boot, providing wrong calibration most of the time at each reboot.
        // So we only set it to "0" if user wants it, as it defaults to 1 at boot
        if (!sharedPrefs.getBoolean(DeviceSettings.KEY_USE_GYRO_CALIBRATION, true))
            Utils.writeValue(FILE_USE_GYRO_CALIB, "0");

        if (sHasTouchkeyToggle) {
            Utils.writeValue(FILE_TOUCHKEY_TOGGLE, sharedPrefs.getBoolean(DeviceSettings.KEY_TOUCHKEY_LIGHT, true) ? "255" : "0");
        }

        if (sHasTouchkeyBLN) {
            Utils.writeValue(FILE_BLN_TOGGLE, sharedPrefs.getBoolean(DeviceSettings.KEY_TOUCHKEY_BLN, true) ? "1" : "0");
        }
    }
}
