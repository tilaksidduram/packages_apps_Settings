package com.android.settings.paranoid;

import android.app.Activity;
import android.os.Bundle;
import android.content.ContentResolver;
import android.content.Context;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;

import com.android.internal.util.cm.QSUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class ScreenAndAnimations extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "ScreenAndAnimations";

    private static final String DISABLE_TORCH_ON_SCREEN_OFF = "disable_torch_on_screen_off";
    private static final String DISABLE_TORCH_ON_SCREEN_OFF_DELAY = "disable_torch_on_screen_off_delay";

    private Context mContext;

    private SwitchPreference mTorchOff;
    private ListPreference mTorchOffDelay;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.screen_and_animations);

        ContentResolver resolver = getActivity().getContentResolver();
        Activity activity = getActivity();
        PreferenceScreen prefSet = getPreferenceScreen();

        mContext = getActivity().getApplicationContext();

        mTorchOff = (SwitchPreference) prefSet.findPreference(DISABLE_TORCH_ON_SCREEN_OFF);
        mTorchOffDelay = (ListPreference) prefSet.findPreference(DISABLE_TORCH_ON_SCREEN_OFF_DELAY);
        int torchOffDelay = Settings.System.getInt(resolver,
                Settings.System.DISABLE_TORCH_ON_SCREEN_OFF_DELAY, 10);
        mTorchOffDelay.setValue(String.valueOf(torchOffDelay));
        mTorchOffDelay.setSummary(mTorchOffDelay.getEntry());
        mTorchOffDelay.setOnPreferenceChangeListener(this);

        if (!QSUtils.deviceSupportsFlashLight(activity)) {
            prefSet.removePreference(mTorchOff);
            prefSet.removePreference(mTorchOffDelay);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (preference == mTorchOffDelay) {
            int torchOffDelay = Integer.valueOf((String) objValue);
            int index = mTorchOffDelay.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DISABLE_TORCH_ON_SCREEN_OFF_DELAY, torchOffDelay);
            mTorchOffDelay.setSummary(mTorchOffDelay.getEntries()[index]);
            return true;
        }
        return false;
    }
}
