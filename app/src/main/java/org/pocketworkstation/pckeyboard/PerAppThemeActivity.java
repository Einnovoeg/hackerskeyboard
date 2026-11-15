package org.pocketworkstation.pckeyboard;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** Editor for per-app theme rules. */
public class PerAppThemeActivity extends AppCompatActivity {

    private EditText mPackage;
    private LinearLayout mList;
    private String mPendingPackage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_per_app_theme);
        EdgeInsets.apply(this, false);

        mPackage = findViewById(R.id.rule_package);
        mList = findViewById(R.id.rule_list);

        Button add = findViewById(R.id.rule_add);
        add.setOnClickListener(v -> {
            mPendingPackage = mPackage.getText().toString().trim();
            if (mPendingPackage.isEmpty()) {
                Toast.makeText(this, R.string.rule_need_package, Toast.LENGTH_SHORT).show();
                return;
            }
            chooseTheme();
        });
        render();
    }

    private void chooseTheme() {
        final String[] labels = getResources().getStringArray(R.array.keyboard_layout_modes);
        new AlertDialog.Builder(this)
                .setTitle(R.string.rule_choose_theme)
                .setItems(labels, (dialog, which) -> {
                    AppThemeRules.put(this, mPendingPackage, which);
                    mPackage.setText("");
                    render();
                })
                .show();
    }

    private void render() {
        mList.removeAllViews();
        List<AppThemeRules.Rule> rules = AppThemeRules.all(this);
        if (rules.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.rule_empty);
            empty.setPadding(0, 24, 0, 24);
            empty.setTextColor(0xFF888888);
            mList.addView(empty);
            return;
        }
        for (final AppThemeRules.Rule rule : rules) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            TextView label = new TextView(this);
            label.setText(rule.packageName + "  ·  "
                    + AppThemeRules.describe(this, rule.theme));
            label.setTextSize(14);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = new Button(this, null, android.R.attr.borderlessButtonStyle);
            remove.setText(android.R.string.cancel);
            remove.setAllCaps(false);
            remove.setOnClickListener(v -> {
                AppThemeRules.remove(this, rule.packageName);
                render();
            });
            row.addView(remove);

            View divider = new View(this);
            divider.setBackgroundColor(0x22808080);
            mList.addView(row);
            mList.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
        }
    }
}
