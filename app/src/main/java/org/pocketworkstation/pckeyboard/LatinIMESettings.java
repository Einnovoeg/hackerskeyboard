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

import android.app.backup.BackupManager;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

/** Settings hub. Every control lives on one grouped, navigable screen. */
public class LatinIMESettings extends BasePreferenceActivity {

    /* package */ static final String PREF_SETTINGS_KEY = "settings_key";

    @Override
    protected Fragment createPreferenceFragment() {
        return new SettingsFragment();
    }

    public static class SettingsFragment extends BasePreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;
            linkToActivity(screen, "pref_screen_view", PrefScreenView.class);
            linkToActivity(screen, "pref_screen_layout", PrefScreenLayout.class);
            linkToActivity(screen, "pref_screen_keys", PrefScreenKeys.class);
            linkToActivity(screen, "pref_screen_typing", PrefScreenTyping.class);
            linkToActivity(screen, "pref_screen_actions", PrefScreenActions.class);
            linkToActivity(screen, "pref_screen_feedback", PrefScreenFeedback.class);
            linkToActivity(screen, "pref_screen_tools", PrefScreenTools.class);
            linkToActivity(screen, "pref_screen_advanced", PrefScreenAdvanced.class);
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            (new BackupManager(requireContext())).dataChanged();
        }
    }
}
