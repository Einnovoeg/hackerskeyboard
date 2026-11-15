/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pocketworkstation.pckeyboard;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

public class PrefScreenView extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new ViewFragment();
    }

    public static class ViewFragment extends PrefScreenActions.BackupAwareFragment {
        private ListPreference mRenderModePreference;

        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_view;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;
            mRenderModePreference = screen.findPreference(LatinIME.PREF_RENDER_MODE);
            linkToActivity(screen, "pref_clipboard_history", ClipboardHistoryActivity.class);
            linkToActivity(screen, "pref_snippets", SnippetsActivity.class);
            linkToActivity(screen, "pref_macros", MacrosActivity.class);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (LatinKeyboardBaseView.sSetRenderMode == null && mRenderModePreference != null) {
                mRenderModePreference.setEnabled(false);
                mRenderModePreference.setSummary(R.string.render_mode_unavailable);
            }
        }
    }
}
