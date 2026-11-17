package org.pocketworkstation.pckeyboard;

import androidx.fragment.app.Fragment;

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
    }
}
