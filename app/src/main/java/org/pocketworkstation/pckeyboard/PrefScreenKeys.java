package org.pocketworkstation.pckeyboard;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

/** Key remapping, key appearance and key behaviour controls. */
public class PrefScreenKeys extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new KeysFragment();
    }

    public static class KeysFragment extends PrefScreenActions.BackupAwareFragment {
        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_keys;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            linkToActivity(screen, "pref_font_library", FontLibraryActivity.class);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (getPreferenceScreen() == null) return;
            Preference font = getPreferenceScreen().findPreference("pref_key_font");
            if (font instanceof DownloadableFontPreference) {
                ((DownloadableFontPreference) font).refreshFonts();
            }
        }
    }
}
