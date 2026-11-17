package org.pocketworkstation.pckeyboard;

import androidx.fragment.app.Fragment;

/** Layout, keyboard shape and sizing controls. */
public class PrefScreenLayout extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new LayoutFragment();
    }

    public static class LayoutFragment extends PrefScreenActions.BackupAwareFragment {
        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_layout;
        }
    }
}
