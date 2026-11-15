package org.pocketworkstation.pckeyboard;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Shows the multi-entry clipboard history: tap a clip to copy it, pin the ones you
 * want to keep, and clear the rest.
 */
public class ClipboardHistoryActivity extends AppCompatActivity {

    private LinearLayout mContainer;
    private TextView mEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clipboard_history);
        EdgeInsets.apply(this, false);

        mContainer = findViewById(R.id.clip_container);
        mEmpty = findViewById(R.id.clip_empty);
        findViewById(R.id.clip_clear).setOnClickListener(v -> {
            ClipboardHistory.clearUnpinned(this);
            render();
        });
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        List<ClipboardHistory.Entry> entries = ClipboardHistory.load(this);
        mContainer.removeAllViews();
        mEmpty.setVisibility(entries.isEmpty() ? View.VISIBLE : View.GONE);

        final float density = getResources().getDisplayMetrics().density;
        for (final ClipboardHistory.Entry entry : entries) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (14 * density);
            row.setPadding(pad, pad, pad, pad);

            TextView text = new TextView(this);
            text.setText(entry.text);
            text.setTextSize(15);
            text.setMaxLines(4);
            text.setTypeface(entry.pinned ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            android.util.TypedValue colorValue = new android.util.TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.textColorPrimary, colorValue, true)
                    && colorValue.resourceId != 0) {
                text.setTextColor(androidx.core.content.ContextCompat
                        .getColorStateList(this, colorValue.resourceId));
            }
            row.addView(text, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setPadding(0, (int) (8 * density), 0, 0);

            Button copy = flatButton(getString(R.string.clipboard_copy));
            copy.setOnClickListener(v -> {
                ClipboardManager manager =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (manager != null) {
                    manager.setPrimaryClip(ClipData.newPlainText("clipboard", entry.text));
                }
                Toast.makeText(this, R.string.clipboard_copied, Toast.LENGTH_SHORT).show();
            });
            buttons.addView(copy);

            Button pin = flatButton(getString(
                    entry.pinned ? R.string.clipboard_unpin : R.string.clipboard_pin));
            pin.setOnClickListener(v -> {
                List<ClipboardHistory.Entry> all = ClipboardHistory.load(this);
                for (ClipboardHistory.Entry candidate : all) {
                    if (candidate.text.equals(entry.text)) {
                        candidate.pinned = !candidate.pinned;
                    }
                }
                ClipboardHistory.save(this, all);
                render();
            });
            buttons.addView(pin);

            row.addView(buttons, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            mContainer.addView(row);

            View divider = new View(this);
            divider.setBackgroundColor(0x22808080);
            mContainer.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) density)));
        }
    }

    private Button flatButton(String label) {
        Button button = new Button(this, null, android.R.attr.borderlessButtonStyle);
        button.setText(label);
        button.setAllCaps(false);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(0, 0, (int) (18 * getResources().getDisplayMetrics().density), 0);
        return button;
    }
}
