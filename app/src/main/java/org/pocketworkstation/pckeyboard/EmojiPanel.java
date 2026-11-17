package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

/** A compact emoji grid shown over the keyboard for the Emoji key action. */
public final class EmojiPanel {

    private static final String[] EMOJI = {
            "\ud83d\ude00", "\ud83d\ude02", "\ud83d\ude05", "\ud83d\ude0a",
            "\ud83d\ude0d", "\ud83d\ude18", "\ud83d\ude1c", "\ud83d\ude21",
            "\ud83d\ude22", "\ud83d\ude2d", "\ud83d\ude31", "\ud83d\ude44",
            "\ud83d\ude4c", "\ud83d\udc4d", "\ud83d\udc4e", "\ud83d\udc4f",
            "\ud83d\ude4f", "\ud83d\udcaa", "\ud83d\udc40", "\ud83d\udc4b",
            "\ud83d\udc95", "\ud83d\udd25", "\u2b50", "\ud83c\udf89",
            "\ud83c\udf82", "\ud83c\udf88", "\ud83c\udf7e", "\ud83c\udf55",
            "\ud83c\udf5f", "\u2615", "\ud83c\udf1f", "\ud83c\udf08",
            "\ud83d\ude08", "\ud83d\udc7b", "\ud83d\udc7d", "\ud83e\udd16",
            "\ud83d\udc31", "\ud83d\udc36", "\ud83d\udc3c", "\ud83e\udd84",
            "\ud83c\udfb8", "\ud83c\udfa7", "\ud83c\udfac", "\ud83c\udfae",
            "\ud83d\udcf1", "\ud83d\udcbb", "\u2328", "\ud83d\udcbe",
            "\u2764", "\ud83d\udc99", "\ud83d\udc9a", "\ud83d\udc9c",
            "\u2705", "\u274c", "\u26a0", "\u2757",
            "\ud83d\udca1", "\ud83d\ude80", "\ud83c\udfaf", "\ud83c\udfc6"
    };

    private EmojiPanel() {
    }

    public static void show(final LatinIME ime, View anchor) {
        final Context context = ime;
        final float density = context.getResources().getDisplayMetrics().density;

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(context);
        header.setText("Emoji");
        header.setTextSize(13);
        header.setPadding((int) (16 * density), (int) (12 * density),
                (int) (16 * density), (int) (6 * density));
        header.setTextColor(0xFF9AA0A6);
        content.addView(header);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(8);
        grid.setPadding((int) (8 * density), 0, (int) (8 * density),
                (int) (8 * density));

        final PopupWindow window = new PopupWindow(content,
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) Math.min(context.getResources().getDisplayMetrics().heightPixels * 0.4,
                        300 * density), true);
        window.setBackgroundDrawable(new ColorDrawable(0xF0121214));
        window.setOutsideTouchable(true);

        int cell = (int) (38 * density);
        for (final String emoji : EMOJI) {
            TextView key = new TextView(context);
            key.setText(emoji);
            key.setTextSize(23);
            key.setGravity(Gravity.CENTER);
            key.setContentDescription(emoji);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cell;
            params.height = cell;
            key.setLayoutParams(params);
            key.setOnClickListener(v -> {
                InputConnection ic = ime.getCurrentInputConnection();
                if (ic != null) {
                    ic.commitText(emoji, 1);
                }
                window.dismiss();
            });
            grid.addView(key);
        }

        content.addView(grid);
        ScrollView scroll = new ScrollView(context);
        scroll.addView(content);
        window.setContentView(scroll);
        window.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);
    }
}
