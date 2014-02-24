/*
 * Copyright (C) 2013 The CyanogenMod project
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

package com.android.settings.cyanogenmod;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Vibrator;
import android.os.Handler;
import android.preference.PreferenceGroup;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.PreferenceCategory;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.SeekBarPreference;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Window;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.policy.IKeyguardService;
import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.hardware.VibratorIntensity;
import com.android.internal.util.paranoid.DeviceUtils;

import java.io.File;
import java.io.IOException;
import com.android.settings.util.Helpers;

public class MoreDeviceSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "MoreDeviceSettings";

    private static final String KEY_SENSORS_MOTORS_CATEGORY = "sensors_motors_category";
    private static final String KEY_DISPLAY_CALIBRATION_CATEGORY = "display_calibration_category";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";
    private static final String KEY_SCREEN_GESTURE_SETTINGS = "touch_screen_gesture_settings";
    private static final String KEY_DOUBLE_TAP_SLEEP_GESTURE = "double_tap_sleep_gesture";
    private static final String KEY_ENABLE_NAVIGATION_BAR = "enable_nav_bar";
    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";
    private static final String NAVIGATION_BAR_CATEGORY = "navigation_bar";
    private static final String NAVIGATION_BAR_LEFT = "navigation_bar_left";
    private static final String RAM_USAGE_BAR = "ram_usage_bar";

    private CheckBoxPreference mDTS;
    private CheckBoxPreference mEnableNavigationBar;
    private SeekBarPreference mNavigationBarHeight;
    private CheckBoxPreference mRamUsageBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.more_device_settings);

        ContentResolver resolver = getContentResolver();
	PreferenceScreen prefSet = getPreferenceScreen();

        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (!VibratorIntensity.isSupported() || vibrator == null || !vibrator.hasVibrator()) {
            removePreference(KEY_SENSORS_MOTORS_CATEGORY);
        }

        final PreferenceGroup calibrationCategory =
                (PreferenceGroup) findPreference(KEY_DISPLAY_CALIBRATION_CATEGORY);

        if (!DisplayColor.isSupported() && !DisplayGamma.isSupported()) {
            getPreferenceScreen().removePreference(calibrationCategory);
        } else {
            if (!DisplayColor.isSupported()) {
                calibrationCategory.removePreference(findPreference(KEY_DISPLAY_COLOR));
            }
            if (!DisplayGamma.isSupported()) {
                calibrationCategory.removePreference(findPreference(KEY_DISPLAY_GAMMA));
            }
        }

        Utils.updatePreferenceToSpecificActivityFromMetaDataOrRemove(getActivity(),
                getPreferenceScreen(), KEY_SCREEN_GESTURE_SETTINGS);

        mDTS = (CheckBoxPreference) findPreference(KEY_DOUBLE_TAP_SLEEP_GESTURE);
        mDTS.setChecked(Settings.System.getInt(getContentResolver(),
              Settings.System.DOUBLE_TAP_SLEEP_GESTURE, 0) == 1);

        boolean hasNavBarByDefault = getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);
        boolean enableNavigationBar = Settings.System.getInt(getContentResolver(),
                Settings.System.NAVIGATION_BAR_SHOW, hasNavBarByDefault ? 1 : 0) == 1;
        mEnableNavigationBar = (CheckBoxPreference) findPreference(KEY_ENABLE_NAVIGATION_BAR);
        mEnableNavigationBar.setChecked(enableNavigationBar);
        mEnableNavigationBar.setOnPreferenceChangeListener(this);

        mNavigationBarHeight = (SeekBarPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
        mNavigationBarHeight.setProgress((int)(Settings.System.getFloat(getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, 1f) * 100));
        mNavigationBarHeight.setEnabled(mEnableNavigationBar.isChecked());
        mNavigationBarHeight.setTitle(getResources().getText(R.string.navigation_bar_height) + " " + mNavigationBarHeight.getProgress() + "%");
        mNavigationBarHeight.setOnPreferenceChangeListener(this);

        PreferenceCategory navbarCategory =
            (PreferenceCategory) findPreference(NAVIGATION_BAR_CATEGORY);

        if (!DeviceUtils.isPhone(getActivity())) {
            navbarCategory.removePreference(findPreference(NAVIGATION_BAR_LEFT));
        }

        mRamUsageBar = (CheckBoxPreference) findPreference(RAM_USAGE_BAR);
        mRamUsageBar.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.RAM_USAGE_BAR, 1) == 1);
        mRamUsageBar.setOnPreferenceChangeListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
	if (preference == mEnableNavigationBar) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_SHOW,
                    ((Boolean) objValue) ? 1 : 0);
             return true;
        } else if (preference == mNavigationBarHeight) {
            Settings.System.putFloat(getActivity().getContentResolver(),
                    Settings.System.NAVIGATION_BAR_HEIGHT, (Integer)objValue / 100f);
            mNavigationBarHeight.setTitle(getResources().getText(R.string.navigation_bar_height) + " " + (Integer)objValue + "%");
	} else if (preference == mRamUsageBar) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RAM_USAGE_BAR, value ? 1 : 0);
	} else {
            return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
         ContentResolver cr = getActivity().getContentResolver();
         if (preference == mDTS) {
              Settings.System.putInt(cr, Settings.System.DOUBLE_TAP_SLEEP_GESTURE,
                     mDTS.isChecked() ? 1 : 0);
        }else {
         return super.onPreferenceTreeClick(preferenceScreen, preference);
       }
         return true;
    }
}
