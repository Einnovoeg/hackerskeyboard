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

/** Manage text-expansion snippets: type a trigger, get a phrase. */
public class SnippetsActivity extends AppCompatActivity {

    private EditText mTrigger;
    private EditText mExpansion;
    private LinearLayout mList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snippets);
        EdgeInsets.apply(this, false);

        mTrigger = findViewById(R.id.snippet_trigger);
        mExpansion = findViewById(R.id.snippet_expansion);
        mList = findViewById(R.id.snippet_list);

        Button add = findViewById(R.id.snippet_add);
        add.setOnClickListener(v -> {
            String trigger = mTrigger.getText().toString().trim();
            if (trigger.isEmpty()) {
                Toast.makeText(this, R.string.snippet_need_trigger, Toast.LENGTH_SHORT).show();
                return;
            }
            SnippetStore.put(this, trigger, mExpansion.getText().toString());
            mTrigger.setText("");
            mExpansion.setText("");
            render();
        });
        render();
    }

    private void render() {
        mList.removeAllViews();
        List<SnippetStore.Snippet> snippets = SnippetStore.load(this);
        if (snippets.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(R.string.snippet_empty);
            empty.setPadding(0, 24, 0, 24);
            empty.setTextColor(0xFF888888);
            mList.addView(empty);
            return;
        }
        for (final SnippetStore.Snippet snippet : snippets) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 10, 0, 10);

            TextView label = new TextView(this);
            label.setText(snippet.trigger + "  →  " + snippet.text);
            label.setTextSize(14);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label, new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            Button remove = new Button(this, null, android.R.attr.borderlessButtonStyle);
            remove.setText(android.R.string.cancel);
            remove.setAllCaps(false);
            remove.setOnClickListener(v -> {
                SnippetStore.remove(this, snippet.trigger);
                render();
            });
            row.addView(remove);
            mList.addView(row);
        }
    }
}
