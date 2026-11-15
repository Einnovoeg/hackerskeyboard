package org.pocketworkstation.pckeyboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** Create and delete macros: a name plus a sequence of text and {key} steps. */
public class MacrosActivity extends AppCompatActivity {

    private EditText mName;
    private EditText mSteps;
    private LinearLayout mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_macros);
        EdgeInsets.apply(this, false);

        mName = findViewById(R.id.macro_name);
        mSteps = findViewById(R.id.macro_steps);
        mList = findViewById(R.id.macro_list);

        Button add = findViewById(R.id.macro_add);
        add.setOnClickListener(v -> {
            String name = mName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.macro_need_name, Toast.LENGTH_SHORT).show();
                return;
            }
            MacroStore.put(this, name, mSteps.getText().toString());
            mName.setText("");
            mSteps.setText("");
            render();
        });
        render();
    }

    private void render() {
        mList.removeAllViews();
        List<MacroStore.Macro> macros = MacroStore.load(this);
        if (macros.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.macro_empty);
            empty.setPadding(0, 24, 0, 24);
            empty.setTextColor(0xFF888888);
            mList.addView(empty);
            return;
        }
        for (final MacroStore.Macro macro : macros) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            TextView label = new TextView(this);
            label.setText(macro.name + "  ·  " + macro.steps);
            label.setTextSize(14);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = new Button(this, null, android.R.attr.borderlessButtonStyle);
            remove.setText(android.R.string.cancel);
            remove.setAllCaps(false);
            remove.setOnClickListener(v -> {
                MacroStore.remove(this, macro.name);
                render();
            });
            row.addView(remove);
            mList.addView(row);
        }
    }
}
