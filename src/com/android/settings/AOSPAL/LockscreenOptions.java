/*
 * Copyright (C) 2014 ParanoidAndroid project
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

package com.android.settings.AOSPAL;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.view.WindowManagerGlobal;
import android.view.IWindowManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.settings.R;
import android.provider.Settings.SettingNotFoundException;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.internal.policy.IKeyguardService;

import java.io.File;
import java.io.IOException;

public class LockscreenOptions extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockscreenOptions";

    private static final String KEY_ALWAYS_BATTERY_PREF = "lockscreen_battery_status";

    private CheckBoxPreference mBatteryStatus;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreenoptions);
        ContentResolver resolver = getContentResolver();
	PreferenceScreen prefs = getPreferenceScreen();

        mBatteryStatus = (CheckBoxPreference) prefs.findPreference(KEY_ALWAYS_BATTERY_PREF);

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mBatteryStatus != null) {
            mBatteryStatus.setChecked(Settings.System.getIntForUser(getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY, 0,
                    UserHandle.USER_CURRENT) != 0);
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	    return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mBatteryStatus) {
            Settings.System.putIntForUser(getContentResolver(),
                    Settings.System.LOCKSCREEN_ALWAYS_SHOW_BATTERY,
                    ((Boolean) objValue) ? 1 : 0, UserHandle.USER_CURRENT);
        }
        return true;
    }

    private boolean removePreferenceIfPackageNotInstalled(Preference preference) {
        String intentUri=((PreferenceScreen) preference).getIntent().toUri(1);
        Pattern pattern = Pattern.compile("component=([^/]+)/");
        Matcher matcher = pattern.matcher(intentUri);

        String packageName=matcher.find()?matcher.group(1):null;
        if(packageName != null) {
            try {
                getPackageManager().getPackageInfo(packageName, 0);
            } catch (NameNotFoundException e) {
                Log.e(TAG,"package "+packageName+" not installed, hiding preference.");
                getPreferenceScreen().removePreference(preference);
                return true;
            }
        }
        return false;
    }
}
