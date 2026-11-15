/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.pocketworkstation.pckeyboard;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Modern Material 3 landing screen: shows the current keyboard status and the
 * actions needed to enable and select the input method.
 */
public class Main extends AppCompatActivity {

    private final static String MARKET_URI = "market://search?q=pub:\"Klaus Weidner\"";

    private TextView mStatusEnabled;
    private TextView mStatusSelected;
    private MaterialButton mEnableButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        MaterialToolbar toolbar = findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        applyWindowInsets();
        keepTestFieldVisible();

        mStatusEnabled = findViewById(R.id.main_status_enabled);
        mStatusSelected = findViewById(R.id.main_status_selected);
        mEnableButton = findViewById(R.id.main_setup_btn_configure_imes);

        mEnableButton.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.main_no_settings_activity, Toast.LENGTH_LONG).show();
            }
        });

        MaterialButton selectButton = findViewById(R.id.main_setup_btn_set_ime);
        selectButton.setOnClickListener(v -> {
            InputMethodManager mgr =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null) {
                mgr.showInputMethodPicker();
            }
        });

        MaterialButton langButton = findViewById(R.id.main_setup_btn_input_lang);
        langButton.setOnClickListener(v ->
                startActivity(new Intent(this, InputLanguageSelection.class)));

        MaterialButton settingsButton = findViewById(R.id.main_setup_btn_settings);
        settingsButton.setOnClickListener(v ->
                startActivity(new Intent(this, LatinIMESettings.class)));

        MaterialButton dictButton = findViewById(R.id.main_setup_btn_get_dicts);
        dictButton.setOnClickListener(v -> {
            Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
            try {
                startActivity(it);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(),
                        getResources().getString(R.string.no_market_warning),
                        Toast.LENGTH_LONG).show();
            }
        });

        TextView description = findViewById(R.id.main_description);
        String html = getString(R.string.main_body);
        Spanned content = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        description.setMovementMethod(LinkMovementMethod.getInstance());
        description.setText(content);

        TextView version = findViewById(R.id.main_version);
        version.setText(getString(R.string.auto_version));
    }

    /** Scrolls the try-it-out field into view when the keyboard shrinks the window. */
    private void keepTestFieldVisible() {
        final android.view.View test = findViewById(R.id.main_setup_edit_test);
        if (test == null) {
            return;
        }
        test.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.postDelayed(this::scrollTestIntoView, 350);
            }
        });
    }

    /** Brings the bottom of the try-it-out field above the keyboard, if it is covered. */
    private void scrollTestIntoView() {
        android.view.View test = findViewById(R.id.main_setup_edit_test);
        androidx.core.widget.NestedScrollView scroll = findViewById(R.id.main_scroll);
        if (test == null || scroll == null || !test.hasFocus()) {
            return;
        }
        int[] field = new int[2];
        int[] frame = new int[2];
        test.getLocationOnScreen(field);
        scroll.getLocationOnScreen(frame);
        int covered = (field[1] + test.getHeight()) - (frame[1] + scroll.getHeight());
        if (covered > 0) {
            int margin = (int) (16 * getResources().getDisplayMetrics().density);
            scroll.smoothScrollBy(0, covered + margin);
        }
    }

    private void applyWindowInsets() {
        final android.view.View appBar = (android.view.View)
                findViewById(R.id.main_toolbar).getParent();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            // Edge-to-edge windows are not resized for the keyboard, so the bottom
            // inset has to follow it or the test field ends up behind the keys.
            int bottom = Math.max(bars.bottom, ime.bottom);
            v.setPadding(bars.left, 0, bars.right, bottom);
            // Inset the app bar instead of the root: content then stays below the clock
            // even while the page scrolls.
            if (appBar != null) {
                appBar.setPadding(0, bars.top, 0, 0);
            }
            // Cover the status bar strip: scrolled page content would otherwise show
            // through it, which reads as text sitting inside the clock.
            android.view.View scrim = findViewById(R.id.main_status_scrim);
            if (scrim != null && scrim.getLayoutParams().height != bars.top) {
                scrim.getLayoutParams().height = bars.top;
                scrim.requestLayout();
            }
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                v.postDelayed(this::scrollTestIntoView, 120);
            }
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        boolean enabled = isImeEnabled();
        boolean selected = isImeSelected();

        mStatusEnabled.setText(enabled ? R.string.main_status_enabled : R.string.main_status_disabled);
        mStatusEnabled.setTextColor(getColor(enabled
                ? R.color.hk_status_ok : R.color.hk_status_pending));

        mStatusSelected.setText(selected
                ? R.string.main_status_selected : R.string.main_status_not_selected);
        mStatusSelected.setTextColor(getColor(selected
                ? R.color.hk_status_ok : R.color.hk_status_pending));

        mEnableButton.setText(enabled
                ? R.string.main_setup_configure_imes : R.string.main_setup_enable_ime);
    }

    private boolean isImeEnabled() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        List<InputMethodInfo> methods = imm.getEnabledInputMethodList();
        if (methods == null) return false;
        String pkg = getPackageName();
        for (InputMethodInfo info : methods) {
            if (pkg.equals(info.getPackageName())) return true;
        }
        return false;
    }

    private boolean isImeSelected() {
        String current = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return current != null && current.startsWith(getPackageName() + "/");
    }
}
