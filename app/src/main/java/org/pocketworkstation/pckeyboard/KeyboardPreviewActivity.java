package org.pocketworkstation.pckeyboard;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows the keyboard with the current theme, font and effects so settings can be
 * tried out before leaving the app.
 */
public class KeyboardPreviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard_preview);
        EdgeInsets.apply(this, false);

        EditText field = findViewById(R.id.preview_field);
        field.requestFocus();

        findViewById(R.id.preview_done).setOnClickListener(v -> finish());

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
