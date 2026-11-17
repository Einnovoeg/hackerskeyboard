package org.pocketworkstation.pckeyboard;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

/** Language, prediction, correction and speech controls. */
public class PrefScreenTyping extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new TypingFragment();
    }

    public static class TypingFragment extends PrefScreenActions.BackupAwareFragment {
        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_typing;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;
            linkToActivity(screen, "pref_screen_language", InputLanguageSelection.class);
            Preference voice = screen.findPreference("pref_voice_settings");
            if (voice != null) {
                voice.setOnPreferenceClickListener(preference -> {
                    try {
                        Intent intent = new Intent("com.android.settings.TTS_SETTINGS");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } catch (RuntimeException e) {
                        startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                    }
                    return true;
                });
            }
        }
    }
}
