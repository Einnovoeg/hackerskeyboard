package org.pocketworkstation.pckeyboard;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Keeps a screen clear of the status and navigation bars. The app targets a recent
 * Android release, where windows are drawn edge to edge, so each screen has to apply
 * the system bar insets itself or its top row ends up under the clock.
 */
final class EdgeInsets {

    private EdgeInsets() {
    }

    /**
     * Pads the screen root by the system bar insets. When {@code imeAware} is set the
     * padding also follows the on-screen keyboard, so text fields stay visible.
     */
    static void apply(Activity activity, boolean imeAware) {
        final View root = activity.findViewById(android.R.id.content);
        if (root == null) {
            return;
        }
        final int startLeft = root.getPaddingLeft();
        final int startTop = root.getPaddingTop();
        final int startRight = root.getPaddingRight();
        final int startBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int bottom = bars.bottom;
            if (imeAware) {
                bottom = Math.max(bottom,
                        insets.getInsets(WindowInsetsCompat.Type.ime()).bottom);
            }
            view.setPadding(startLeft + bars.left, startTop + bars.top,
                    startRight + bars.right, startBottom + bottom);
            return insets;
        });
    }
}
