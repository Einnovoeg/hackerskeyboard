package org.pocketworkstation.pckeyboard;

import android.os.Build;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

/** Input behaviour, rendering and diagnostics. */
public class PrefScreenAdvanced extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new AdvancedFragment();
    }

    public static class AdvancedFragment extends PrefScreenActions.BackupAwareFragment {
        private ListPreference mRenderModePreference;
        private Preference mInputConnectionInfo;
        private Preference mLabelVersion;

        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_advanced;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;
            mRenderModePreference = screen.findPreference(LatinIME.PREF_RENDER_MODE);
            mInputConnectionInfo = screen.findPreference("input_connection_info");
            mLabelVersion = screen.findPreference("label_version");
        }

        @Override
        public void onResume() {
            super.onResume();
            if (mRenderModePreference != null) {
                if (LatinKeyboardBaseView.sSetRenderMode == null) {
                    mRenderModePreference.setEnabled(false);
                    mRenderModePreference.setSummary(R.string.render_mode_unavailable);
                } else {
                    mRenderModePreference.setEnabled(true);
                }
            }
            if (mInputConnectionInfo != null && LatinIME.sKeyboardSettings != null) {
                String pkg = LatinIME.sKeyboardSettings.editorPackageName;
                mInputConnectionInfo.setSummary(pkg == null || pkg.isEmpty()
                        ? getString(R.string.summary_no_editor)
                        : pkg + "  type=0x"
                                + Integer.toHexString(LatinIME.sKeyboardSettings.editorInputType));
            }
            if (mLabelVersion != null) {
                String version = "";
                try {
                    version = requireContext().getPackageManager()
                            .getPackageInfo(requireContext().getPackageName(), 0).versionName;
                } catch (Exception ignored) {
                }
                mLabelVersion.setSummary(version + "  Android " + Build.VERSION.RELEASE);
            }
        }
    }
}
