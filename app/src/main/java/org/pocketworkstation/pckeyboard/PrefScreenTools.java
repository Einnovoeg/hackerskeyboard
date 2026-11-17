package org.pocketworkstation.pckeyboard;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;

/** Clipboard, snippets, macros, per-app themes and the gamepad. */
public class PrefScreenTools extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new ToolsFragment();
    }

    public static class ToolsFragment extends PrefScreenActions.BackupAwareFragment {
        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_tools;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;
            linkToActivity(screen, "pref_clipboard_history", ClipboardHistoryActivity.class);
            linkToActivity(screen, "pref_snippets", SnippetsActivity.class);
            linkToActivity(screen, "pref_macros", MacrosActivity.class);
        }
    }
}
