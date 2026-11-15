package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/**
 * A strip of recent and pinned clips shown over the keyboard. Tapping a clip
 * inserts it at the cursor; long-pressing a clip toggles its pin.
 */
public class ClipboardPanel {

    private static final int MAX_SHOWN = 14;

    public static void show(final LatinIME ime, View anchor) {
        final List<ClipboardHistory.Entry> entries = ClipboardHistory.load(ime);
        final Context context = ime;
        final float density = context.getResources().getDisplayMetrics().density;

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(context);
        header.setText(R.string.title_clipboard_history);
        header.setTextSize(12);
        header.setPadding((int) (16 * density), (int) (12 * density),
                (int) (16 * density), (int) (6 * density));
        header.setTextColor(0xFF9AA0A6);
        list.addView(header);

        if (entries.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(R.string.clipboard_empty_strip);
            empty.setTextSize(14);
            empty.setPadding((int) (16 * density), (int) (8 * density),
                    (int) (16 * density), (int) (16 * density));
            empty.setTextColor(0xFFBBBBBB);
            list.addView(empty);
        }

        final PopupWindow window = new PopupWindow(list,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        window.setBackgroundDrawable(new ColorDrawable(0xF0121214));
        window.setOutsideTouchable(true);

        int shown = 0;
        for (final ClipboardHistory.Entry entry : entries) {
            if (shown++ >= MAX_SHOWN) break;

            TextView row = new TextView(context);
            row.setText(entry.text.replace('\n', ' '));
            row.setSingleLine(true);
            row.setEllipsize(TextUtils.TruncateAt.END);
            row.setTextSize(15);
            row.setPadding((int) (16 * density), (int) (11 * density),
                    (int) (16 * density), (int) (11 * density));
            row.setTextColor(0xFFEDEDED);
            row.setBackgroundColor(entry.pinned ? 0x1AFFC107 : Color.TRANSPARENT);
            if (entry.pinned) {
                row.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.btn_star_big_on, 0);
            }
            row.setOnClickListener(v -> {
                InputConnection ic = ime.getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(entry.text, 1);
                }
                window.dismiss();
            });
            row.setOnLongClickListener(v -> {
                List<ClipboardHistory.Entry> all = ClipboardHistory.load(context);
                for (ClipboardHistory.Entry candidate : all) {
                    if (candidate.text.equals(entry.text)) {
                        candidate.pinned = !candidate.pinned;
                    }
                }
                ClipboardHistory.save(context, all);
                window.dismiss();
                show(ime, anchor);
                return true;
            });
            list.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        window.setContentView(scroll);
        window.setHeight((int) Math.min(ime.getResources().getDisplayMetrics().heightPixels * 0.4,
                60 * density + list.getChildCount() * 44 * density));
        window.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);
    }
}
