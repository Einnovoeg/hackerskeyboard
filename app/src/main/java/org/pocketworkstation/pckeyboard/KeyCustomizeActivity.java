package org.pocketworkstation.pckeyboard;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.pocketworkstation.pckeyboard.Keyboard.Key;

/** Vial-style per-key label, output and text-colour editor. */
public class KeyCustomizeActivity extends AppCompatActivity {

    private LinearLayout mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_customize);
        EdgeInsets.apply(this, false);
        MaterialToolbar toolbar = findViewById(R.id.key_customize_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        mList = findViewById(R.id.key_customize_list);
        findViewById(R.id.key_customize_reset_all).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setMessage(R.string.key_customize_reset_all_confirm)
                        .setPositiveButton(R.string.key_customize_reset_all, (dialog, which) -> {
                            KeyOverrides.clear(this);
                            Toast.makeText(this, R.string.key_customize_saved,
                                    Toast.LENGTH_SHORT).show();
                            render();
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show());
        render();
    }

    private void render() {
        mList.removeAllViews();
        List<Key> keys = null;
        try {
            LatinKeyboard keyboard = new LatinKeyboard(this,
                    KeyboardSwitcher.fullLayoutXml(), 0, 40f);
            keys = keyboard.getKeys();
        } catch (RuntimeException ignored) {
        }
        if (keys == null) {
            addEmpty();
            return;
        }
        Set<Integer> seen = new HashSet<>();
        int rows = addKeys(keys, seen);
        // Include the symbols layer so punctuation is editable too, not just letters.
        try {
            LatinKeyboard symbols = new LatinKeyboard(this,
                    R.xml.kbd_symbols, KeyboardSwitcher.KEYBOARDMODE_SYMBOLS, 40f);
            rows += addKeys(symbols.getKeys(), seen);
        } catch (RuntimeException ignored) {
        }
        if (rows == 0) addEmpty();
    }

    private int addKeys(List<Key> keys, Set<Integer> seen) {
        int rows = 0;
        for (Key key : keys) {
            if (key == null || key.codes == null || key.codes.length == 0) continue;
            if (key.label == null || key.label.toString().trim().isEmpty()) continue;
            if (!seen.add(key.codes[0])) continue;
            addRow(key);
            rows++;
        }
        return rows;
    }

    private void addEmpty() {
        TextView empty = new TextView(this);
        empty.setText(R.string.key_customize_empty);
        empty.setTextColor(0xFF888888);
        empty.setPadding(0, 32, 0, 32);
        mList.addView(empty);
    }

    private void addRow(final Key key) {
        final int code = key.codes[0];
        final KeyOverrides.Entry current = KeyOverrides.get(this, code);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6, 0, 6);

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(key.label);
        label.setTextSize(17);
        label.setTextColor(current != null && current.color != 0 ? current.color
                : 0xFFEDEDED);
        TextView detail = new TextView(this);
        detail.setText(getString(R.string.key_customize_code, code)
                + (current == null ? "" : "  •  " + getString(R.string.key_customize_customized)));
        detail.setTextSize(11);
        detail.setTextColor(0xFF888888);
        text.addView(label);
        text.addView(detail);
        row.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button edit = new Button(this, null, android.R.attr.borderlessButtonStyle);
        edit.setText(R.string.key_customize_edit);
        edit.setAllCaps(false);
        edit.setOnClickListener(v -> editKey(key));
        row.addView(edit);

        mList.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(0x22808080);
        mList.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private void editKey(final Key key) {
        final int code = key.codes[0];
        final KeyOverrides.Entry current = KeyOverrides.get(this, code);
        final String defaultLabel = key.label == null ? "" : key.label.toString();
        String defaultOutput = "";
        if (key.text != null) {
            defaultOutput = key.text.toString();
        } else if (code >= 32) {
            defaultOutput = Character.toString((char) code);
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad / 2, pad, 0);

        final EditText label = new EditText(this);
        label.setHint(R.string.key_customize_label);
        label.setText(current == null ? "" : current.label);
        label.setInputType(InputType.TYPE_CLASS_TEXT);
        label.setSingleLine(true);
        form.addView(label);

        final EditText output = new EditText(this);
        output.setHint(getString(R.string.key_customize_default_output, defaultOutput));
        output.setText(current == null ? "" : current.output);
        output.setInputType(InputType.TYPE_CLASS_TEXT);
        output.setSingleLine(true);
        form.addView(output);

        final EditText color = new EditText(this);
        color.setHint(R.string.key_customize_color);
        color.setText(current == null || current.color == 0 ? ""
                : String.format("#%08X", current.color));
        color.setInputType(InputType.TYPE_CLASS_TEXT);
        color.setSingleLine(true);
        form.addView(color);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(defaultLabel.isEmpty() ? getString(R.string.key_customize_edit)
                        : defaultLabel)
                .setView(form)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.key_customize_reset_key, (d, which) -> {
                    KeyOverrides.remove(this, code);
                    render();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int parsed = 0;
                    String colorText = color.getText().toString().trim();
                    if (!colorText.isEmpty()) {
                        try {
                            parsed = Color.parseColor(colorText);
                        } catch (IllegalArgumentException e) {
                            color.setError(getString(R.string.key_customize_invalid_color));
                            return;
                        }
                    }
                    KeyOverrides.put(this, code, label.getText().toString().trim(),
                            output.getText().toString(), parsed);
                    Toast.makeText(this, R.string.key_customize_saved,
                            Toast.LENGTH_SHORT).show();
                    render();
                    dialog.dismiss();
                }));
        dialog.show();
    }
}
