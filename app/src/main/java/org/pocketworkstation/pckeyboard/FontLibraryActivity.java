package org.pocketworkstation.pckeyboard;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Catalog of extra typefaces. Each font's preview is bundled and shown before the
 * optional download, so browsing the library costs no network traffic.
 */
public class FontLibraryActivity extends AppCompatActivity {

    private static final String PREF_KEY_FONT = "pref_key_font";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMain = new Handler(Looper.getMainLooper());
    private LinearLayout mList;
    private SharedPreferences mPrefs;
    private int mBusyIndex = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_font_library);
        EdgeInsets.apply(this, false);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        MaterialToolbar toolbar = findViewById(R.id.font_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        mList = findViewById(R.id.font_list);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRows();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExecutor.shutdownNow();
    }

    private int selectedIndex() {
        try {
            return Integer.parseInt(mPrefs.getString(PREF_KEY_FONT, "0"));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private void refreshRows() {
        if (mList == null) return;
        mList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        int selected = selectedIndex();

        // A removed font file must not leave the keyboard pointing at a missing slot.
        if (selected >= DownloadedFonts.FIRST_INDEX) {
            DownloadedFonts.Font selectedFont = DownloadedFonts.forIndex(selected);
            if (selectedFont == null || !DownloadedFonts.isInstalled(this, selectedFont)) {
                selected = 0;
                mPrefs.edit().putString(PREF_KEY_FONT, "0").apply();
            }
        }

        for (int i = 0; i < DownloadedFonts.count(); i++) {
            final DownloadedFonts.Font font = DownloadedFonts.get(i);
            final int slot = DownloadedFonts.indexOf(font);
            boolean installed = DownloadedFonts.isInstalled(this, font);
            boolean isSelected = installed && selected == slot;
            boolean isBusy = mBusyIndex == slot;

            View row = inflater.inflate(R.layout.item_font_library, mList, false);
            TextView name = row.findViewById(R.id.font_name);
            TextView meta = row.findViewById(R.id.font_meta);
            ImageView preview = row.findViewById(R.id.font_preview);
            MaterialButton previewButton = row.findViewById(R.id.font_preview_button);
            MaterialButton installButton = row.findViewById(R.id.font_install_button);
            MaterialButton removeButton = row.findViewById(R.id.font_remove_button);

            name.setText(font.name);
            meta.setText(font.category + "  ·  " + font.license);
            preview.setImageResource(font.previewRes);
            preview.setContentDescription(getString(R.string.font_preview_description, font.name));

            previewButton.setOnClickListener(v -> showPreview(font));
            if (isBusy) {
                installButton.setText(R.string.font_downloading);
                installButton.setEnabled(false);
            } else if (isSelected) {
                installButton.setText(R.string.font_selected);
                installButton.setEnabled(false);
            } else if (installed) {
                installButton.setText(R.string.font_use);
                installButton.setEnabled(true);
                installButton.setOnClickListener(v -> selectFont(font));
            } else {
                installButton.setText(R.string.font_download);
                installButton.setEnabled(true);
                installButton.setOnClickListener(v -> downloadFont(font));
            }

            removeButton.setVisibility(installed ? View.VISIBLE : View.GONE);
            removeButton.setEnabled(!isBusy);
            removeButton.setOnClickListener(v -> confirmRemove(font, isSelected));

            mList.addView(row);
        }
    }

    private void showPreview(DownloadedFonts.Font font) {
        boolean installed = DownloadedFonts.isInstalled(this, font);
        boolean selected = installed && selectedIndex() == DownloadedFonts.indexOf(font);
        View content = LayoutInflater.from(this)
                .inflate(R.layout.dialog_font_preview, null, false);
        ImageView image = content.findViewById(R.id.font_dialog_preview);
        TextView description = content.findViewById(R.id.font_dialog_description);
        image.setImageResource(font.previewRes);
        image.setContentDescription(getString(R.string.font_preview_description, font.name));
        String detail = font.category + "  ·  " + font.license + "\n\n"
                + getString(selected ? R.string.font_preview_selected_hint
                        : installed ? R.string.font_preview_installed_hint
                        : R.string.font_preview_download_hint);
        description.setText(detail);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(font.name)
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null);
        if (installed) {
            if (!selected) {
                builder.setPositiveButton(R.string.font_use,
                        (dialog, which) -> selectFont(font));
            }
        } else {
            builder.setPositiveButton(R.string.font_download,
                    (dialog, which) -> downloadFont(font));
        }
        builder.show();
    }

    private void selectFont(DownloadedFonts.Font font) {
        mPrefs.edit()
                .putString(PREF_KEY_FONT, Integer.toString(DownloadedFonts.indexOf(font)))
                .apply();
        Toast.makeText(this, getString(R.string.font_applied, font.name), Toast.LENGTH_SHORT).show();
        refreshRows();
    }

    private void downloadFont(DownloadedFonts.Font font) {
        if (mBusyIndex >= 0) return;
        mBusyIndex = DownloadedFonts.indexOf(font);
        refreshRows();
        mExecutor.execute(() -> {
            Exception failure = null;
            try {
                DownloadedFonts.download(getApplicationContext(), font);
            } catch (Exception e) {
                failure = e;
            }
            final Exception result = failure;
            mMain.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                mBusyIndex = -1;
                if (result == null) {
                    selectFont(font);
                } else {
                    Toast.makeText(FontLibraryActivity.this,
                            R.string.font_download_failed, Toast.LENGTH_LONG).show();
                    refreshRows();
                }
            });
        });
    }

    private void confirmRemove(DownloadedFonts.Font font, boolean selected) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.font_remove_title, font.name))
                .setMessage(R.string.font_remove_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.font_remove, (dialog, which) -> {
                    if (selected) {
                        mPrefs.edit().putString(PREF_KEY_FONT, "0").apply();
                    }
                    DownloadedFonts.remove(this, font);
                    Toast.makeText(this, R.string.font_removed, Toast.LENGTH_SHORT).show();
                    refreshRows();
                })
                .show();
    }
}
