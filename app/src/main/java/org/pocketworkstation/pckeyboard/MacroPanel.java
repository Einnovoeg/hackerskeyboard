package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Lists the user's macros over the keyboard; tapping one runs it. */
public class MacroPanel {

    public static void show(final LatinIME ime, View anchor) {
        final Context context = ime;
        final float density = context.getResources().getDisplayMetrics().density;
        final List<MacroStore.Macro> macros = MacroStore.load(context);

        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);

        TextView header = new TextView(context);
        header.setText(R.string.title_macros);
        header.setTextSize(12);
        header.setPadding((int) (16 * density), (int) (12 * density),
                (int) (16 * density), (int) (6 * density));
        header.setTextColor(0xFF9AA0A6);
        list.addView(header);

        if (macros.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText(R.string.macro_empty_strip);
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

        TextView gamepad = new TextView(context);
        gamepad.setText("\uD83C\uDFAE  Gamepad");
        gamepad.setTextSize(15);
        gamepad.setPadding((int) (16 * density), (int) (12 * density),
                (int) (16 * density), (int) (12 * density));
        gamepad.setTextColor(0xFF8AB4F8);
        gamepad.setOnClickListener(v -> {
            window.dismiss();
            GamepadPanel.show(ime, anchor);
        });
        list.addView(gamepad);


        for (final MacroStore.Macro macro : macros) {
            TextView row = new TextView(context);
            row.setText(macro.name);
            row.setTextSize(15);
            row.setSingleLine(true);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setPadding((int) (16 * density), (int) (12 * density),
                    (int) (16 * density), (int) (12 * density));
            row.setTextColor(0xFFEDEDED);
            row.setOnClickListener(v -> {
                ime.runMacro(macro);
                window.dismiss();
            });
            list.addView(row);
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(list);
        window.setContentView(scroll);
        window.setHeight((int) Math.min(
                ime.getResources().getDisplayMetrics().heightPixels * 0.4,
                60 * density + list.getChildCount() * 46 * density));
        window.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);
    }
}
