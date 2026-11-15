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

import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

public class PrefScreenActions extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new ActionsFragment();
    }

    /** Common behaviour for the secondary settings screens: notify backup on change. */
    public abstract static class BackupAwareFragment extends BasePreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {
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

    public static class ActionsFragment extends BackupAwareFragment {
        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_actions;
        }
    }
}
