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

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class PrefScreenFeedback extends BasePreferenceActivity {

    @Override
    protected Fragment createPreferenceFragment() {
        return new FeedbackFragment();
    }

    public static class FeedbackFragment extends PrefScreenActions.BackupAwareFragment {
        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs_feedback;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;
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
