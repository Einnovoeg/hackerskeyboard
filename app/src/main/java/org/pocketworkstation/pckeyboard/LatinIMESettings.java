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

import java.util.HashMap;
import java.util.Map;

import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.text.AutoText;
import android.text.InputType;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

public class LatinIMESettings extends BasePreferenceActivity {

    /* package */ static final String PREF_SETTINGS_KEY = "settings_key";

    @Override
    protected Fragment createPreferenceFragment() {
        return new SettingsFragment();
    }

    public static class SettingsFragment extends BasePreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        private static final String QUICK_FIXES_KEY = "quick_fixes";
        private static final String PREDICTION_SETTINGS_KEY = "prediction_settings";
        private static final String VOICE_SETTINGS_KEY = "voice_mode";
        static final String INPUT_CONNECTION_INFO = "input_connection_info";

        private static final String TAG = "LatinIMESettings";

        private CheckBoxPreference mQuickFixes;
        private ListPreference mVoicePreference;
        private ListPreference mSettingsKeyPreference;
        private ListPreference mKeyboardModePortraitPreference;
        private ListPreference mKeyboardModeLandscapePreference;
        private Preference mInputConnectionInfo;
        private Preference mLabelVersion;

        private String mVoiceModeOff;

        @Override
        protected int getPreferencesResource() {
            return R.xml.prefs;
        }

        @Override
        protected void onPreferencesReady(@Nullable PreferenceScreen screen) {
            if (screen == null) return;

            mQuickFixes = screen.findPreference(QUICK_FIXES_KEY);
            mVoicePreference = screen.findPreference(VOICE_SETTINGS_KEY);
            mSettingsKeyPreference = screen.findPreference(PREF_SETTINGS_KEY);
            mInputConnectionInfo = screen.findPreference(INPUT_CONNECTION_INFO);
            mLabelVersion = screen.findPreference("label_version");
            mKeyboardModePortraitPreference = screen.findPreference("pref_keyboard_mode_portrait");
            mKeyboardModeLandscapePreference = screen.findPreference("pref_keyboard_mode_landscape");

            mVoiceModeOff = getString(R.string.voice_mode_off);

            linkToActivity(screen, "pref_screen_view", PrefScreenView.class);
            linkToActivity(screen, "pref_screen_feedback", PrefScreenFeedback.class);
            linkToActivity(screen, "pref_screen_actions", PrefScreenActions.class);
            linkToActivity(screen, "pref_screen_language", InputLanguageSelection.class);
        }

        @Override
        public void onResume() {
            super.onResume();
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .registerOnSharedPreferenceChangeListener(this);

            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) return;

            if (mQuickFixes != null) {
                int autoTextSize = AutoText.getSize(getListView());
                PreferenceGroup prediction = screen.findPreference(PREDICTION_SETTINGS_KEY);
                if (autoTextSize < 1 && prediction != null) {
                    prediction.removePreference(mQuickFixes);
                }
            }

            if (!LatinIME.sKeyboardSettings.compactModeEnabled
                    && mKeyboardModePortraitPreference != null
                    && mKeyboardModeLandscapePreference != null) {
                CharSequence[] oldEntries = mKeyboardModePortraitPreference.getEntries();
                CharSequence[] oldValues = mKeyboardModePortraitPreference.getEntryValues();

                if (oldEntries != null && oldValues != null && oldEntries.length > 2) {
                    CharSequence[] newEntries = new CharSequence[] { oldEntries[0], oldEntries[2] };
                    CharSequence[] newValues = new CharSequence[] { oldValues[0], oldValues[2] };
                    mKeyboardModePortraitPreference.setEntries(newEntries);
                    mKeyboardModePortraitPreference.setEntryValues(newValues);
                    mKeyboardModeLandscapePreference.setEntries(newEntries);
                    mKeyboardModeLandscapePreference.setEntryValues(newValues);
                }
            }

            updateSummaries();
            updateVersionSummary();
        }

        @Override
        public void onPause() {
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }

        private void updateVersionSummary() {
            if (mLabelVersion == null) return;
            String version = "";
            try {
                version = requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            } catch (Exception e) {
                Log.e(TAG, "Could not find version info.");
            }
            version += " " + Build.VERSION.RELEASE;
            mLabelVersion.setSummary(version);
        }

        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            (new BackupManager(requireContext())).dataChanged();
            updateVoiceModeSummary();
            updateSummaries();
        }

        static Map<Integer, String> INPUT_CLASSES = new HashMap<Integer, String>();
        static Map<Integer, String> DATETIME_VARIATIONS = new HashMap<Integer, String>();
        static Map<Integer, String> TEXT_VARIATIONS = new HashMap<Integer, String>();
        static Map<Integer, String> NUMBER_VARIATIONS = new HashMap<Integer, String>();
        static {
            INPUT_CLASSES.put(0x00000004, "DATETIME");
            INPUT_CLASSES.put(0x00000002, "NUMBER");
            INPUT_CLASSES.put(0x00000003, "PHONE");
            INPUT_CLASSES.put(0x00000001, "TEXT");
            INPUT_CLASSES.put(0x00000000, "NULL");

            DATETIME_VARIATIONS.put(0x00000010, "DATE");
            DATETIME_VARIATIONS.put(0x00000020, "TIME");

            NUMBER_VARIATIONS.put(0x00000010, "PASSWORD");

            TEXT_VARIATIONS.put(0x00000020, "EMAIL_ADDRESS");
            TEXT_VARIATIONS.put(0x00000030, "EMAIL_SUBJECT");
            TEXT_VARIATIONS.put(0x000000b0, "FILTER");
            TEXT_VARIATIONS.put(0x00000050, "LONG_MESSAGE");
            TEXT_VARIATIONS.put(0x00000080, "PASSWORD");
            TEXT_VARIATIONS.put(0x00000060, "PERSON_NAME");
            TEXT_VARIATIONS.put(0x000000c0, "PHONETIC");
            TEXT_VARIATIONS.put(0x00000070, "POSTAL_ADDRESS");
            TEXT_VARIATIONS.put(0x00000040, "SHORT_MESSAGE");
            TEXT_VARIATIONS.put(0x00000010, "URI");
            TEXT_VARIATIONS.put(0x00000090, "VISIBLE_PASSWORD");
            TEXT_VARIATIONS.put(0x000000a0, "WEB_EDIT_TEXT");
            TEXT_VARIATIONS.put(0x000000d0, "WEB_EMAIL_ADDRESS");
            TEXT_VARIATIONS.put(0x000000e0, "WEB_PASSWORD");
        }

        private static void addBit(StringBuffer buf, int bit, String str) {
            if (bit != 0) {
                buf.append("|");
                buf.append(str);
            }
        }

        private static String inputTypeDesc(int type) {
            int cls = type & 0x0000000f; // MASK_CLASS
            int flags = type & 0x00fff000; // MASK_FLAGS
            int var = type & 0x00000ff0; // MASK_VARIATION

            StringBuffer out = new StringBuffer();
            String clsName = INPUT_CLASSES.get(cls);
            out.append(clsName != null ? clsName : "?");

            if (cls == InputType.TYPE_CLASS_TEXT) {
                String varName = TEXT_VARIATIONS.get(var);
                if (varName != null) {
                    out.append(".");
                    out.append(varName);
                }
                addBit(out, flags & 0x00010000, "AUTO_COMPLETE");
                addBit(out, flags & 0x00008000, "AUTO_CORRECT");
                addBit(out, flags & 0x00001000, "CAP_CHARACTERS");
                addBit(out, flags & 0x00004000, "CAP_SENTENCES");
                addBit(out, flags & 0x00002000, "CAP_WORDS");
                addBit(out, flags & 0x00040000, "IME_MULTI_LINE");
                addBit(out, flags & 0x00020000, "MULTI_LINE");
                addBit(out, flags & 0x00080000, "NO_SUGGESTIONS");
            } else if (cls == InputType.TYPE_CLASS_NUMBER) {
                String varName = NUMBER_VARIATIONS.get(var);
                if (varName != null) {
                    out.append(".");
                    out.append(varName);
                }
                addBit(out, flags & 0x00002000, "DECIMAL");
                addBit(out, flags & 0x00001000, "SIGNED");
            } else if (cls == InputType.TYPE_CLASS_DATETIME) {
                String varName = DATETIME_VARIATIONS.get(var);
                if (varName != null) {
                    out.append(".");
                    out.append(varName);
                }
            }
            return out.toString();
        }

        private void updateSummaries() {
            Resources res = getResources();

            if (mSettingsKeyPreference != null) {
                String[] modes = res.getStringArray(R.array.settings_key_modes);
                int index = mSettingsKeyPreference.findIndexOfValue(
                        mSettingsKeyPreference.getValue());
                if (index >= 0 && index < modes.length) {
                    mSettingsKeyPreference.setSummary(modes[index]);
                }
            }

            if (mInputConnectionInfo != null) {
                mInputConnectionInfo.setSummary(String.format("%s type=%s",
                        LatinIME.sKeyboardSettings.editorPackageName,
                        inputTypeDesc(LatinIME.sKeyboardSettings.editorInputType)));
            }
            updateVoiceModeSummary();
        }

        private void updateVoiceModeSummary() {
            if (mVoicePreference == null) return;
            String[] summaries = getResources().getStringArray(R.array.voice_input_modes_summary);
            int index = mVoicePreference.findIndexOfValue(mVoicePreference.getValue());
            if (index >= 0 && index < summaries.length) {
                mVoicePreference.setSummary(summaries[index]);
            }
        }
    }
}
