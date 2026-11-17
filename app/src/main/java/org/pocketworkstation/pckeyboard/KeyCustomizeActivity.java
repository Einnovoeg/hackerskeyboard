package org.pocketworkstation.pckeyboard;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.pocketworkstation.pckeyboard.Keyboard.Key;

/**
 * Vial-style per-layer key remapping. A key's XML code is its durable identity;
 * the binding replaces what the key emits while the row keeps showing which
 * physical key is being edited and what it currently does.
 */
public class KeyCustomizeActivity extends AppCompatActivity {

    private static final class Layer {
        final String name;
        final int xml;
        final int mode;
        final int extraMode;

        Layer(String name, int xml, int mode, int extraMode) {
            this.name = name;
            this.xml = xml;
            this.mode = mode;
            this.extraMode = extraMode;
        }
    }

    private static final class Board {
        final String name;
        final int lettersXml;
        final int symbolsXml;
        final int shiftedSymbolsXml;

        Board(String name, int lettersXml, int symbolsXml, int shiftedSymbolsXml) {
            this.name = name;
            this.lettersXml = lettersXml;
            this.symbolsXml = symbolsXml;
            this.shiftedSymbolsXml = shiftedSymbolsXml;
        }

        List<Layer> layers() {
            List<Layer> layers = new ArrayList<>();
            boolean phone = lettersXml == R.xml.kbd_phone;
            int lettersMode = phone ? 0 : KeyboardSwitcher.KEYBOARDMODE_NORMAL;
            int symbolsMode = phone ? 0 : KeyboardSwitcher.KEYBOARDMODE_SYMBOLS;
            int lettersExtra = phone ? 0
                    : KeyboardSwitcher.KEYBOARDMODE_NORMAL_WITH_SETTINGS_KEY;
            int symbolsExtra = phone ? 0
                    : KeyboardSwitcher.KEYBOARDMODE_SYMBOLS_WITH_SETTINGS_KEY;
            layers.add(new Layer("Letters", lettersXml, lettersMode, lettersExtra));
            layers.add(new Layer("Symbols", symbolsXml, symbolsMode, symbolsExtra));
            if (shiftedSymbolsXml != 0) {
                layers.add(new Layer("Symbols (shift)", shiftedSymbolsXml,
                        symbolsMode, symbolsExtra));
            }
            return layers;
        }
    }

    private static final Board[] BOARDS = {
            new Board("Full 5-row QWERTY", R.xml.kbd_full, R.xml.kbd_full_fn, 0),
            new Board("Full 5-row AZERTY", R.xml.kbd_full_azerty, R.xml.kbd_full_fn, 0),
            new Board("Full 5-row Dvorak", R.xml.kbd_full_dvorak, R.xml.kbd_full_fn, 0),
            new Board("Compact 5-row", R.xml.kbd_compact, R.xml.kbd_compact_fn, 0),
            new Board("Standard QWERTY", R.xml.kbd_qwerty, R.xml.kbd_symbols,
                    R.xml.kbd_symbols_shift),
            new Board("Phone", R.xml.kbd_phone, R.xml.kbd_phone_symbols, 0)
    };

    private LinearLayout mList;
    private Spinner mLayoutSpinner;
    private Spinner mLayerSpinner;
    private Board mBoard;
    private boolean mUpdatingSelectors;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_customize);
        EdgeInsets.apply(this, false);
        MaterialToolbar toolbar = findViewById(R.id.key_customize_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        mList = findViewById(R.id.key_customize_list);
        mLayoutSpinner = findViewById(R.id.key_customize_layout_spinner);
        mLayerSpinner = findViewById(R.id.key_customize_layer_spinner);

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

        String[] boardNames = new String[BOARDS.length];
        for (int i = 0; i < BOARDS.length; i++) boardNames[i] = BOARDS[i].name;
        mLayoutSpinner.setAdapter(spinnerAdapter(boardNames));
        mLayoutSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mUpdatingSelectors) return;
                mBoard = BOARDS[position];
                updateLayers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mLayerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!mUpdatingSelectors) render();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        int currentLayout = KeyboardSwitcher.fullLayoutXml();
        int selected = 0;
        for (int i = 0; i < BOARDS.length; i++) {
            if (BOARDS[i].lettersXml == currentLayout) {
                selected = i;
                break;
            }
        }
        mBoard = BOARDS[selected];
        mLayoutSpinner.setSelection(selected, false);
        updateLayers();
    }

    private ArrayAdapter<String> spinnerAdapter(String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void updateLayers() {
        mUpdatingSelectors = true;
        List<Layer> layers = mBoard.layers();
        String[] names = new String[layers.size()];
        for (int i = 0; i < layers.size(); i++) names[i] = layers.get(i).name;
        mLayerSpinner.setAdapter(spinnerAdapter(names));
        mLayerSpinner.setSelection(0, false);
        mUpdatingSelectors = false;
        render();
    }

    private Layer selectedLayer() {
        if (mBoard == null) return null;
        List<Layer> layers = mBoard.layers();
        int position = mLayerSpinner.getSelectedItemPosition();
        if (position < 0 || position >= layers.size()) return null;
        return layers.get(position);
    }

    private void render() {
        mList.removeAllViews();
        Layer layer = selectedLayer();
        if (layer == null) {
            addEmpty();
            return;
        }
        String layerId = KeyOverrides.layerId(this, layer.xml, layer.mode);
        Set<Integer> seen = new HashSet<>();
        int rows = addLayer(layer.xml, layer.mode, layerId, seen);
        if (layer.extraMode != 0 && layer.extraMode != layer.mode) {
            rows += addLayer(layer.xml, layer.extraMode, layerId, seen);
        }
        if (rows == 0) addEmpty();
    }

    private int addLayer(int xml, int mode, String layerId, Set<Integer> seen) {
        List<Key> keys;
        try {
            keys = new LatinKeyboard(this, xml, mode, 40f).getKeys();
        } catch (RuntimeException e) {
            return 0;
        }
        int rows = 0;
        for (Key key : keys) {
            if (key == null || key.originalCodes == null || key.originalCodes.length == 0) {
                continue;
            }
            int code = key.originalCodes[0];
            if (code == 0 || !seen.add(code)) continue;
            addRow(key, layerId, code);
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

    private String keyName(Key key, int code) {
        CharSequence original = key.originalLabel;
        if (original != null && !TextUtils.isEmpty(original.toString().trim())) {
            return original.toString().trim();
        }
        CharSequence current = key.label;
        if (current != null && !TextUtils.isEmpty(current.toString().trim())) {
            return current.toString().trim();
        }
        return KeyActions.describeCode(code, original);
    }

    private void addRow(final Key key, final String layerId, final int code) {
        final KeyOverrides.Entry current = KeyOverrides.get(this, layerId, code);
        final String originalName = keyName(key, code);
        final String defaultBinding = KeyActions.describeCode(code, key.originalLabel);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 6, 0, 6);
        row.setOnClickListener(v -> editKey(key, layerId, code, originalName, defaultBinding));

        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        TextView label = new TextView(this);
        label.setText(originalName);
        label.setTextSize(17);
        label.setTextColor(current != null && current.color != 0 ? current.color
                : 0xFFEDEDED);
        TextView detail = new TextView(this);
        String detailText;
        if (current != null && current.hasBinding()) {
            detailText = getString(R.string.key_customize_now, current.describe())
                    + "  \u00b7  " + getString(R.string.key_customize_default, defaultBinding);
        } else if (current != null) {
            detailText = getString(R.string.key_customize_default, defaultBinding)
                    + "  \u00b7  " + getString(R.string.key_customize_styled);
        } else {
            detailText = getString(R.string.key_customize_default, defaultBinding);
        }
        if (current != null && !current.label.isEmpty()) {
            detailText += "  \u00b7  label " + current.label;
        }
        detail.setText(detailText);
        detail.setTextSize(11);
        detail.setTextColor(0xFF888888);
        text.addView(label);
        text.addView(detail);
        row.addView(text, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button edit = new Button(this, null, android.R.attr.borderlessButtonStyle);
        edit.setText(R.string.key_customize_edit);
        edit.setAllCaps(false);
        edit.setOnClickListener(v -> editKey(key, layerId, code, originalName, defaultBinding));
        row.addView(edit);

        mList.addView(row);
        View divider = new View(this);
        divider.setBackgroundColor(0x22808080);
        mList.addView(divider, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private void editKey(final Key key, final String layerId, final int code,
            String originalName, final String defaultBinding) {
        final KeyOverrides.Entry current = KeyOverrides.get(this, layerId, code);
        final List<KeyActions.Spec> actions = KeyActions.all();

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        form.setPadding(pad, pad / 2, pad, 0);

        TextView currentView = new TextView(this);
        String currentText = current != null && current.hasBinding()
                ? current.describe() : defaultBinding;
        currentView.setText(getString(R.string.key_customize_current, currentText));
        currentView.setTextSize(13);
        currentView.setTextColor(0xFF9AA0A6);
        currentView.setPadding(0, 0, 0, pad / 2);
        form.addView(currentView);

        TextView bindingLabel = new TextView(this);
        bindingLabel.setText(R.string.key_customize_binding);
        bindingLabel.setTextSize(12);
        bindingLabel.setTextColor(0xFF9AA0A6);
        form.addView(bindingLabel);

        final String[] options = new String[actions.size() + 2];
        options[0] = getString(R.string.key_customize_default_binding);
        options[1] = getString(R.string.key_customize_character);
        for (int i = 0; i < actions.size(); i++) {
            options[i + 2] = actions.get(i).title;
        }

        final Spinner binding = new Spinner(this);
        binding.setAdapter(spinnerAdapter(options));
        int selected = 0;
        if (current != null && current.hasBinding()) {
            if (KeyOverrides.KIND_CHAR.equals(current.kind)) {
                selected = 1;
            } else if (KeyOverrides.KIND_ACTION.equals(current.kind)) {
                for (int i = 0; i < actions.size(); i++) {
                    if (actions.get(i).id.equals(current.value)) {
                        selected = i + 2;
                        break;
                    }
                }
            }
        }
        binding.setSelection(selected, false);
        form.addView(binding);

        final EditText character = new EditText(this);
        character.setHint(R.string.key_customize_character_hint);
        character.setText(current != null && KeyOverrides.KIND_CHAR.equals(current.kind)
                ? current.value : "");
        character.setInputType(InputType.TYPE_CLASS_TEXT);
        character.setSingleLine(true);
        character.setVisibility(selected == 1 ? View.VISIBLE : View.GONE);
        form.addView(character);

        binding.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                character.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        final EditText label = new EditText(this);
        label.setHint(R.string.key_customize_label);
        label.setText(current == null ? "" : current.label);
        label.setInputType(InputType.TYPE_CLASS_TEXT);
        label.setSingleLine(true);
        form.addView(label);

        final EditText color = new EditText(this);
        color.setHint(R.string.key_customize_color);
        color.setText(current == null || current.color == 0 ? ""
                : String.format("#%08X", current.color));
        color.setInputType(InputType.TYPE_CLASS_TEXT);
        color.setSingleLine(true);
        form.addView(color);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(originalName)
                .setView(form)
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.key_customize_reset_key, (d, which) -> {
                    KeyOverrides.remove(this, layerId, code);
                    Toast.makeText(this, R.string.key_customize_saved,
                            Toast.LENGTH_SHORT).show();
                    render();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    int parsedColor = 0;
                    String colorText = color.getText().toString().trim();
                    if (!colorText.isEmpty()) {
                        try {
                            parsedColor = Color.parseColor(colorText);
                        } catch (IllegalArgumentException e) {
                            color.setError(getString(R.string.key_customize_invalid_color));
                            return;
                        }
                    }

                    int position = binding.getSelectedItemPosition();
                    String kind = KeyOverrides.KIND_STYLE;
                    String value = "";
                    if (position == 1) {
                        value = character.getText().toString();
                        String error = KeyActions.characterError(value);
                        if (error != null) {
                            character.setError(error);
                            character.requestFocus();
                            return;
                        }
                        kind = KeyOverrides.KIND_CHAR;
                    } else if (position >= 2) {
                        KeyActions.Spec spec = actions.get(position - 2);
                        if (spec == null) {
                            Toast.makeText(this, R.string.key_customize_unavailable,
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        kind = KeyOverrides.KIND_ACTION;
                        value = spec.id;
                    }

                    KeyOverrides.put(this, layerId, code, kind, value,
                            label.getText().toString().trim(), parsedColor);
                    Toast.makeText(this, R.string.key_customize_saved,
                            Toast.LENGTH_SHORT).show();
                    render();
                    dialog.dismiss();
                }));
        dialog.show();
    }
}
