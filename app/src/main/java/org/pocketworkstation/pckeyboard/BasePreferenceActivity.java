package org.pocketworkstation.pckeyboard;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;

/**
 * Shared Material 3 host for all preference screens. Subclasses only supply the
 * fragment that renders their preference hierarchy.
 */
public abstract class BasePreferenceActivity extends AppCompatActivity {

    protected abstract Fragment createPreferenceFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        MaterialToolbar toolbar = findViewById(R.id.prefs_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setTitle(getTitle());
        toolbar.setNavigationOnClickListener(v -> finish());

        // Keep the whole screen clear of the status bar now that the theme draws edge
        // to edge. The padding goes on the root so the toolbar keeps its full height.
        final android.view.View root = findViewById(R.id.prefs_root);
        final int rootStart = root.getPaddingLeft();
        final int rootEnd = root.getPaddingRight();
        final int rootBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int top = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            view.setPadding(rootStart, top, rootEnd, rootBottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.prefs_container, createPreferenceFragment())
                    .commit();
        }
    }
}
