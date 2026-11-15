package org.pocketworkstation.pckeyboard;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

/**
 * Base fragment for the AndroidX preference screens. It also wires the custom
 * seek bar dialog, which AndroidX preference delegates to the hosting fragment.
 */
public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    private static final String DIALOG_SEEKBAR = "seekbar";

    protected abstract int getPreferencesResource();

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(getPreferencesResource(), rootKey);
        onPreferencesReady(getPreferenceScreen());
    }

    protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
    }

    /** Starts one of the secondary settings activities from a preference row. */
    protected void linkToActivity(@Nullable PreferenceScreen screen, String key,
            final Class<?> activityClass) {
        if (screen == null) return;
        Preference pref = screen.findPreference(key);
        if (pref == null) return;
        pref.setOnPreferenceClickListener(p -> {
            startActivity(new android.content.Intent(requireContext(), activityClass));
            return true;
        });
    }

    @Override
    public void onDisplayPreferenceDialog(@NonNull Preference preference) {
        if (preference instanceof SeekBarPreference) {
            SeekBarPreferenceDialog dialog =
                    SeekBarPreferenceDialog.newInstance(preference.getKey());
            dialog.setTargetFragment(this, 0);
            dialog.show(getParentFragmentManager(), DIALOG_SEEKBAR);
            return;
        }
        super.onDisplayPreferenceDialog(preference);
    }
}
