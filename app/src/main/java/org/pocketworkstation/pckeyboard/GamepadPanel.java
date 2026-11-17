package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.inputmethod.InputConnection;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Controller surface drawn over the keyboard. Every control emits real Android
 * gamepad key events (KEYCODE_BUTTON_*) through the current input connection,
 * so apps and emulators that map a controller receive them exactly as if a
 * physical pad were attached.
 */
public class GamepadPanel {

    // 0 = Xbox, 1 = Switch, 2 = PlayStation
    public static final String PREF_GLYPHS = "pref_gamepad_glyphs";
    public static final String PREF_THEME = "pref_gamepad_theme";
    public static final String PREF_BACKGROUND = "pref_gamepad_background";
    public static final String PREF_SWAP = "pref_gamepad_swap";

    private static PopupWindow sWindow;

    /** True while the controller surface is on screen. */
    public static boolean isShowing() {
        return sWindow != null && sWindow.isShowing();
    }

    /** Hides the controller surface and returns to the normal keyboard. */
    public static void dismiss() {
        if (sWindow != null) {
            sWindow.dismiss();
            sWindow = null;
        }
    }

    /** Theme order must match KeyboardSwitcher.THEMES. */
    static final String[] THEME_NAMES = {
            "ics", "gingerbread", "stone_bold", "trans_neon",
            "material_dark", "material_light", "ics_darker", "material_black",
            "modern_dark", "modern_light", "modern_clear", "modern_oled",
            "wood", "ocean", "forest", "sunset",
            "nebula", "rose", "sandstone", "midnight",
            "aurora", "cyberpunk", "paper", "carbon",
            "lagoon", "ember", "cobalt", "blossom", "glacier", "obsidian",
            "orchid", "reef", "crimson", "arctic", "graphite", "solar"
    };

    private static final int CODE_A = KeyEvent.KEYCODE_BUTTON_A;
    private static final int CODE_B = KeyEvent.KEYCODE_BUTTON_B;
    private static final int CODE_X = KeyEvent.KEYCODE_BUTTON_X;
    private static final int CODE_Y = KeyEvent.KEYCODE_BUTTON_Y;
    private static final int CODE_L1 = KeyEvent.KEYCODE_BUTTON_L1;
    private static final int CODE_R1 = KeyEvent.KEYCODE_BUTTON_R1;
    private static final int CODE_L2 = KeyEvent.KEYCODE_BUTTON_L2;
    private static final int CODE_R2 = KeyEvent.KEYCODE_BUTTON_R2;

    private static int intPref(Context context, String key, int fallback) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        try {
            return Integer.parseInt(prefs.getString(key, Integer.toString(fallback)));
        } catch (ClassCastException e) {
            // Written as a boolean by a checkbox row.
            try {
                return prefs.getBoolean(key, fallback != 0) ? 1 : 0;
            } catch (ClassCastException ignored) {
                return fallback;
            }
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Theme index for the pad: its own choice, else the keyboard theme. */
    static int resolvedTheme(Context context) {
        int value = intPref(context, PREF_THEME, -1);
        if (value < 0) {
            value = LatinIME.sKeyboardSettings == null ? 8
                    : LatinIME.sKeyboardSettings.keyboardTheme;
        }
        return Math.max(0, Math.min(value, THEME_NAMES.length - 1));
    }

    /** Background art index for the pad: its own choice, else the keyboard's. */
    static int resolvedBackground(Context context) {
        int value = intPref(context, PREF_BACKGROUND, -1);
        if (value < 0) return BackgroundImages.selected(context);
        return Math.max(0, Math.min(value, BackgroundImages.DRAWABLES.length - 1));
    }

    static boolean swapSides(Context context) {
        return intPref(context, PREF_SWAP, 0) != 0;
    }

    public static int glyphSet(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        try {
            return Integer.parseInt(prefs.getString(PREF_GLYPHS, "0"));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Face button label for a position: 0 bottom, 1 right, 2 top, 3 left. */
    static String faceLabel(int set, int position) {
        switch (set) {
            case 1: // Switch: B A Y X clockwise from bottom
                return new String[]{"B", "A", "Y", "X"}[position];
            case 2: // PlayStation: cross circle triangle square
                return new String[]{"\u2715", "\u25cb", "\u25b3", "\u25a1"}[position];
            default: // Xbox: A B Y X
                return new String[]{"A", "B", "Y", "X"}[position];
        }
    }

    /** Key code for a face button position (semantics follow the physical pad). */
    static int faceCode(int set, int position) {
        // Nintendo swaps A/B relative to Xbox; PlayStation uses the same layout
        // as Xbox but with its own marks.
        switch (set) {
            case 1:
                return new int[]{CODE_B, CODE_A, CODE_Y, CODE_X}[position];
            default:
                return new int[]{CODE_A, CODE_B, CODE_Y, CODE_X}[position];
        }
    }

    static String shoulderLabel(int set, boolean right) {
        if (set == 1) return right ? "R" : "L";
        if (set == 2) return right ? "R1" : "L1";
        return right ? "RB" : "LB";
    }

    static String triggerLabel(int set, boolean right) {
        if (set == 1) return right ? "ZR" : "ZL";
        if (set == 2) return right ? "R2" : "L2";
        return right ? "RT" : "LT";
    }

    public static void show(final LatinIME ime, View anchor) {
        if (isShowing()) {
            dismiss();
            return;
        }
        final Context context = ime;
        final float density = context.getResources().getDisplayMetrics().density;
        final int theme = resolvedTheme(context);
        final int keyColor = themeColor(context, theme, "key", 0xFF23262B);
        final int pressedColor = themeColor(context, theme, "key_pressed", 0xFF3A3F45);
        final int textColor = themeColor(context, theme, "key_text", 0xFFEDEDED);
        final int strokeColor = themeColor(context, theme, "key_stroke", 0xFF3A3F45);
        final int panelColor = themeColor(context, theme, "panel", 0xEE101216);
        final int set = glyphSet(context);

        final int bgIndex = resolvedBackground(context);
        final boolean hasArt = bgIndex > 0;
        // Art needs a lighter scrim so it stays visible behind the caps.
        final int plateColor = hasArt ? ((panelColor & 0x00FFFFFF) | 0xB4000000) : panelColor;

        FrameLayout frame = new FrameLayout(context);
        GradientDrawable panel = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{plateColor, plateColor});
        panel.setCornerRadius(16 * density);
        panel.setStroke(Math.max(1, (int) density), strokeColor);
        if (hasArt) {
            // Art is painted by a drawable that reports no intrinsic size: a plain
            // bitmap drawable would make the wrapped popup measure screen-height.
            android.graphics.drawable.Drawable art =
                    context.getDrawable(BackgroundImages.DRAWABLES[bgIndex]);
            frame.setBackground(new ArtPlate(art, plateColor, 16 * density,
                    Math.max(1, (int) density), strokeColor));
        } else {
            frame.setBackground(panel);
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (10 * density), (int) (10 * density),
                (int) (10 * density), (int) (10 * density));
        frame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final PopupWindow window = new PopupWindow(frame,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        window.setOutsideTouchable(true);

        // Back always leaves the pad, so the keyboard can never be trapped.
        frame.setFocusableInTouchMode(true);
        frame.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                dismiss();
                return true;
            }
            // A real pad plugged in gets the same easter egg.
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                Konami.onKey(keyCode);
            }
            return false;
        });
        Konami.sOnComplete = () -> celebrate(frame, density, anchor);
        Konami.reset();

                final InputConnection ic = ime.getCurrentInputConnection();

        // --- header: title + explicit way back to the keyboard ---
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(context);
        title.setText("Gamepad");
        title.setTextSize(13);
        title.setTextColor(textColor);
        header.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        GamepadButton close = new GamepadButton(context, "\u2715  Close", keyColor,
                pressedColor, textColor, strokeColor, density);
        close.setTextSize(12);
        close.setOnPress(() -> {
            playFeedback(anchor);
            dismiss();
        }, null);
        header.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40, density)));
        root.addView(header);

        // --- triggers: wide, analog-feel with a held state ---
        LinearLayout triggerRow = new LinearLayout(context);
        triggerRow.setOrientation(LinearLayout.HORIZONTAL);
        triggerRow.setGravity(Gravity.CENTER);
        GamepadButton lt = new GamepadButton(context, triggerLabel(set, false), keyColor,
                pressedColor, textColor, strokeColor, density);
        lt.setOnPress(() -> sendKey(ic, CODE_L2, true), () -> sendKey(ic, CODE_L2, false));
        GamepadButton rt = new GamepadButton(context, triggerLabel(set, true), keyColor,
                pressedColor, textColor, strokeColor, density);
        rt.setOnPress(() -> sendKey(ic, CODE_R2, true), () -> sendKey(ic, CODE_R2, false));
        triggerRow.addView(lt, weightParams(density));
        triggerRow.addView(rt, weightParams(density));
        root.addView(triggerRow);

        // --- bumpers ---
        LinearLayout bumperRow = new LinearLayout(context);
        bumperRow.setOrientation(LinearLayout.HORIZONTAL);
        bumperRow.setGravity(Gravity.CENTER);
        GamepadButton lb = new GamepadButton(context, shoulderLabel(set, false), keyColor,
                pressedColor, textColor, strokeColor, density);
        lb.setOnPress(() -> sendKey(ic, CODE_L1, false), null);
        GamepadButton rb = new GamepadButton(context, shoulderLabel(set, true), keyColor,
                pressedColor, textColor, strokeColor, density);
        rb.setOnPress(() -> sendKey(ic, CODE_R1, false), null);
        bumperRow.addView(lb, weightParams(density));
        bumperRow.addView(rb, weightParams(density));
        root.addView(bumperRow);

        // --- middle: d-pad + face buttons ---
        LinearLayout middle = new LinearLayout(context);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setPadding(0, (int) (8 * density), 0, (int) (8 * density));

        LinearLayout dpadSide = buildDpad(context, ic, keyColor, pressedColor, textColor,
                strokeColor, density);
        LinearLayout faceSide = buildFaceButtons(context, ic, set, keyColor, pressedColor,
                textColor, strokeColor, density);
        LinearLayout.LayoutParams middleParams =
                new LinearLayout.LayoutParams(0, dp(168, density), 1f);
        if (swapSides(context)) {
            middle.addView(faceSide, middleParams);
            middle.addView(dpadSide, new LinearLayout.LayoutParams(0, dp(168, density), 1f));
        } else {
            middle.addView(dpadSide, middleParams);
            middle.addView(faceSide, new LinearLayout.LayoutParams(0, dp(168, density), 1f));
        }
        root.addView(middle);

        // --- start / select ---
        LinearLayout systemRow = new LinearLayout(context);
        systemRow.setOrientation(LinearLayout.HORIZONTAL);
        systemRow.setGravity(Gravity.CENTER);
        GamepadButton select = new GamepadButton(context, "SELECT", keyColor, pressedColor,
                textColor, strokeColor, density);
        select.setOnPress(() -> sendKey(ic, KeyEvent.KEYCODE_BUTTON_SELECT, false), null);
        GamepadButton start = new GamepadButton(context, "START", keyColor, pressedColor,
                textColor, strokeColor, density);
        start.setOnPress(() -> sendKey(ic, KeyEvent.KEYCODE_BUTTON_START, false), null);
        systemRow.addView(select, weightParams(density));
        systemRow.addView(start, weightParams(density));
        root.addView(systemRow);

        // --- explicit, always-visible exit at the bottom edge ---
        GamepadButton backToKeyboard = new GamepadButton(context,
                "Back to keyboard", keyColor, pressedColor, textColor, strokeColor,
                density);
        backToKeyboard.setTextSize(12);
        backToKeyboard.setOnPress(() -> {
            playFeedback(anchor);
            dismiss();
        }, null);
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42, density));
        int backMargin = dp(4, density);
        backParams.setMargins(backMargin, backMargin, backMargin, 0);
        root.addView(backToKeyboard, backParams);

        View feedbackAnchor = anchor;
        root.setOnClickListener(v -> { });
        feedbackAnchor.playSoundEffect(SoundEffectConstants.CLICK);
        if (Build.VERSION.SDK_INT >= 26) {
            feedbackAnchor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }

        window.setOnDismissListener(() -> {
            sWindow = null;
            Konami.reset();
            Konami.sOnComplete = null;
        });
        sWindow = window;
        window.showAtLocation(anchor, Gravity.BOTTOM, 0, 0);
    }

    /** Compact, non-interactive copy of the pad used by the settings preview. */
    public static View buildPreview(Context context, float scale) {
        final float density = context.getResources().getDisplayMetrics().density * scale;
        final int theme = resolvedTheme(context);
        final int set = glyphSet(context);
        final int keyColor = themeColor(context, theme, "key", 0xFF23262B);
        final int pressedColor = themeColor(context, theme, "key_pressed", 0xFF3A3F45);
        final int textColor = themeColor(context, theme, "key_text", 0xFFEDEDED);
        final int strokeColor = themeColor(context, theme, "key_stroke", 0xFF3A3F45);
        final int panelColor = themeColor(context, theme, "panel", 0xEE101216);
        final int bgIndex = resolvedBackground(context);
        final boolean hasArt = bgIndex > 0;
        final int plateColor = hasArt ? ((panelColor & 0x00FFFFFF) | 0xB4000000) : panelColor;

        FrameLayout frame = new FrameLayout(context);
        GradientDrawable panel = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM, new int[]{plateColor, plateColor});
        panel.setCornerRadius(16 * density);
        panel.setStroke(Math.max(1, (int) density), strokeColor);
        if (hasArt) {
            frame.setBackground(new ArtPlate(context.getDrawable(BackgroundImages.DRAWABLES[bgIndex]),
                    plateColor, 16 * density, Math.max(1, (int) density), strokeColor));
        } else {
            frame.setBackground(panel);
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (10 * density), (int) (10 * density),
                (int) (10 * density), (int) (6 * density));

        LinearLayout triggerRow = new LinearLayout(context);
        triggerRow.setOrientation(LinearLayout.HORIZONTAL);
        triggerRow.addView(new GamepadButton(context, triggerLabel(set, false), keyColor,
                pressedColor, textColor, strokeColor, density), weightParams(density));
        triggerRow.addView(new GamepadButton(context, triggerLabel(set, true), keyColor,
                pressedColor, textColor, strokeColor, density), weightParams(density));
        root.addView(triggerRow);

        LinearLayout bumperRow = new LinearLayout(context);
        bumperRow.setOrientation(LinearLayout.HORIZONTAL);
        bumperRow.addView(new GamepadButton(context, shoulderLabel(set, false), keyColor,
                pressedColor, textColor, strokeColor, density), weightParams(density));
        bumperRow.addView(new GamepadButton(context, shoulderLabel(set, true), keyColor,
                pressedColor, textColor, strokeColor, density), weightParams(density));
        root.addView(bumperRow);

        LinearLayout middle = new LinearLayout(context);
        middle.setOrientation(LinearLayout.HORIZONTAL);
        middle.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
        LinearLayout dpadSide = buildDpad(context, null, keyColor, pressedColor, textColor,
                strokeColor, density);
        LinearLayout faceSide = buildFaceButtons(context, null, set, keyColor, pressedColor,
                textColor, strokeColor, density);
        if (swapSides(context)) {
            middle.addView(faceSide, new LinearLayout.LayoutParams(0, dp(168, density), 1f));
            middle.addView(dpadSide, new LinearLayout.LayoutParams(0, dp(168, density), 1f));
        } else {
            middle.addView(dpadSide, new LinearLayout.LayoutParams(0, dp(168, density), 1f));
            middle.addView(faceSide, new LinearLayout.LayoutParams(0, dp(168, density), 1f));
        }
        root.addView(middle);

        LinearLayout systemRow = new LinearLayout(context);
        systemRow.setOrientation(LinearLayout.HORIZONTAL);
        systemRow.addView(new GamepadButton(context, "SELECT", keyColor, pressedColor,
                textColor, strokeColor, density), weightParams(density));
        systemRow.addView(new GamepadButton(context, "START", keyColor, pressedColor,
                textColor, strokeColor, density), weightParams(density));
        root.addView(systemRow);

        frame.addView(root, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        frame.setClickable(false);
        frame.setFocusable(false);
        return frame;
    }

    private static void playFeedback(View anchor) {
        if (anchor == null) return;
        anchor.playSoundEffect(SoundEffectConstants.CLICK);
        if (Build.VERSION.SDK_INT >= 26) {
            anchor.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        }
    }

    private static LinearLayout buildDpad(Context context, InputConnection ic, int keyColor,
            int pressedColor, int textColor, int strokeColor, float density) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);

        // 3x3: corners are diagonals (two directions at once).
        String[] symbols = {"\u2196", "\u2191", "\u2197", "\u2190", "", "\u2192", "\u2199", "\u2193", "\u2198"};
        int[][] codes = {
                {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT},
                {KeyEvent.KEYCODE_DPAD_UP},
                {KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_RIGHT},
                {KeyEvent.KEYCODE_DPAD_LEFT},
                {},
                {KeyEvent.KEYCODE_DPAD_RIGHT},
                {KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT},
                {KeyEvent.KEYCODE_DPAD_DOWN},
                {KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT}
        };

        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                if (index == 4) {
                    View spacer = new View(context);
                    line.addView(spacer, new LinearLayout.LayoutParams(dp(52, density), dp(52, density)));
                    continue;
                }
                final int[] pressed = codes[index];
                GamepadButton button = new GamepadButton(context, symbols[index], keyColor,
                        pressedColor, textColor, strokeColor, density);
                button.setTextSize(16);
                if (pressed.length == 2) {
                    // diagonals: press both directions, release together
                    button.setOnPress(() -> {
                        sendKey(ic, pressed[0], true);
                        sendKey(ic, pressed[1], true);
                    }, () -> {
                        sendKey(ic, pressed[1], false);
                        sendKey(ic, pressed[0], false);
                    });
                } else if (pressed.length == 1) {
                    final int code = pressed[0];
                    button.setOnPress(() -> {
                        Konami.onKey(code);
                        sendKey(ic, code, true);
                    }, () -> sendKey(ic, code, false));
                }
                line.addView(button, new LinearLayout.LayoutParams(dp(52, density), dp(52, density)));
            }
            grid.addView(line);
        }
        return grid;
    }

    private static LinearLayout buildFaceButtons(Context context, InputConnection ic, int set,
            int keyColor, int pressedColor, int textColor, int strokeColor, float density) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setGravity(Gravity.CENTER);

        for (int row = 0; row < 3; row++) {
            LinearLayout line = new LinearLayout(context);
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setGravity(Gravity.CENTER);
            for (int col = 0; col < 3; col++) {
                int position = -1;
                if (row == 0 && col == 1) position = 2; // top
                if (row == 1 && col == 0) position = 3; // left
                if (row == 1 && col == 2) position = 1; // right
                if (row == 2 && col == 1) position = 0; // bottom
                if (position < 0) {
                    View spacer = new View(context);
                    line.addView(spacer, new LinearLayout.LayoutParams(dp(52, density), dp(52, density)));
                    continue;
                }
                final int pos = position;
                final int code = faceCode(set, pos);
                final int glyph = pos == 1 ? KeyEvent.KEYCODE_BUTTON_B
                        : KeyEvent.KEYCODE_BUTTON_A;
                GamepadButton button = new GamepadButton(context, faceLabel(set, pos),
                        keyColor, pressedColor, textColor, strokeColor, density);
                button.setTextSize(18);
                button.setOnPress(() -> {
                    if (pos <= 1) Konami.onKey(glyph);
                    sendKey(ic, code, false);
                }, null);
                line.addView(button, new LinearLayout.LayoutParams(dp(52, density), dp(52, density)));
            }
            grid.addView(line);
        }
        return grid;
    }

    private static void sendKey(InputConnection ic, int code, boolean held) {
        if (ic == null) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        // Face buttons also type their letter, so the panel is usable in text fields.
        String typed = letterFor(code);
        if (typed != null) {
            ic.commitText(typed, 1);
        }
        if (!held) {
            ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        }
    }

    private static String letterFor(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_BUTTON_A: return "A";
            case KeyEvent.KEYCODE_BUTTON_B: return "B";
            case KeyEvent.KEYCODE_BUTTON_X: return "X";
            case KeyEvent.KEYCODE_BUTTON_Y: return "Y";
            default: return null;
        }
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }

    private static LinearLayout.LayoutParams weightParams(float density) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        int margin = dp(4, density);
        params.setMargins(margin, margin, margin, margin);
        return params;
    }

    static int themeColor(Context context, int theme, String suffix, int fallback) {
        String name = "kb_" + THEME_NAMES[theme] + "_" + suffix;
        int id = context.getResources().getIdentifier(name, "color", context.getPackageName());
        if (id == 0) {
            // Modern Dark is the guaranteed complete palette.
            id = context.getResources().getIdentifier("kb_modern_dark_" + suffix,
                    "color", context.getPackageName());
        }
        return id == 0 ? fallback : context.getResources().getColor(id, null);
    }

    /** Rounded art plate with a scrim, reporting no intrinsic size. */
    private static class ArtPlate extends android.graphics.drawable.Drawable {
        private final android.graphics.drawable.Drawable mArt;
        private final int mScrim;
        private final int mStroke;
        private final int mStrokeWidth;
        private final float mRadius;
        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path();

        ArtPlate(android.graphics.drawable.Drawable art, int scrim, float radius,
                int strokeWidth, int stroke) {
            mArt = art;
            mScrim = scrim;
            mRadius = radius;
            mStrokeWidth = strokeWidth;
            mStroke = stroke;
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setStrokeWidth(strokeWidth);
            mPaint.setColor(stroke);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            mPath.reset();
            mPath.addRoundRect(new android.graphics.RectF(bounds), mRadius, mRadius,
                    Path.Direction.CW);
            canvas.save();
            canvas.clipPath(mPath);
            mArt.setBounds(bounds);
            mArt.draw(canvas);
            canvas.drawColor(mScrim);
            canvas.restore();
            mPaint.setColor(mStroke);
            mPaint.setStrokeWidth(mStrokeWidth);
            canvas.drawPath(mPath, mPaint);
        }

        @Override public void setAlpha(int alpha) { mPaint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
        @Override public int getIntrinsicWidth() { return -1; }
        @Override public int getIntrinsicHeight() { return -1; }
    }

    /** Clickable pad button: gradient-free themed cap with press feedback. */
    private static class GamepadButton extends TextView {
        private final int mNormal;
        private final int mPressed;
        private final int mStroke;
        private final float mDensity;
        private Runnable mDown;
        private Runnable mUp;

        GamepadButton(Context context, String label, int normal, int pressed, int text,
                int stroke, float density) {
            super(context);
            mNormal = normal;
            mPressed = pressed;
            mStroke = stroke;
            mDensity = density;
            setText(label);
            setTextColor(text);
            setGravity(Gravity.CENTER);
            setSingleLine(true);
            setPadding(dp(2, density), 0, dp(2, density), 0);
            setBackground(cap(normal));
            setOnTouchListener((v, event) -> {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        setBackground(cap(mPressed));
                        v.playSoundEffect(SoundEffectConstants.CLICK);
                        v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                        if (mDown != null) mDown.run();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        setBackground(cap(mNormal));
                        if (mUp != null) mUp.run();
                        return true;
                    default:
                        return true;
                }
            });
        }

        void setOnPress(Runnable down, Runnable up) {
            mDown = down;
            mUp = up;
        }

        private GradientDrawable cap(int color) {
            GradientDrawable drawable = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM, new int[]{color, color});
            drawable.setCornerRadius(10 * mDensity);
            drawable.setStroke(Math.max(1, (int) mDensity), mStroke);
            return drawable;
        }
    }

    /**
     * The Konami code, entered on the pad: up up down down left right left right B A.
     * Other controls are ignored (not treated as a mistake), and a real controller
     * gets the same easter egg through the panel's key listener.
     */
    static final class Konami {
        private static final int[] SEQUENCE = {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_BUTTON_A,
        };

        private static int sIndex;
        private static Runnable sOnComplete;

        private Konami() {
        }

        static void reset() {
            sIndex = 0;
        }

        static void onKey(int code) {
            if (!isPartOfCode(code)) return;
            if (code == SEQUENCE[sIndex]) {
                sIndex++;
                if (sIndex == SEQUENCE.length) {
                    sIndex = 0;
                    if (sOnComplete != null) sOnComplete.run();
                }
            } else {
                // A wrong direction starts over, but a repeated first step counts as one.
                sIndex = code == SEQUENCE[0] ? 1 : 0;
            }
        }

        private static boolean isPartOfCode(int code) {
            for (int step : SEQUENCE) {
                if (step == code) return true;
            }
            return false;
        }
    }

    private static void celebrate(FrameLayout frame, float density, View anchor) {
        final HackermanBurst burst = new HackermanBurst(frame.getContext(), density);
        // Keep the pad exactly its current size: a MATCH_PARENT child would make the
        // wrap-content popup grow and swallow the keyboard while the card plays.
        final int overlayHeight = frame.getHeight();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                overlayHeight > 0 ? overlayHeight : ViewGroup.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.BOTTOM;
        frame.addView(burst, params);
        if (anchor != null) {
            anchor.playSoundEffect(SoundEffectConstants.CLICK);
            if (Build.VERSION.SDK_INT >= 26) {
                anchor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
        }
        burst.start();
        HackermanSound.play(frame.getContext());
    }

    /**
     * The reward for the code: a short Hackerman-style title card. A CRT wakes up,
     * a green terminal logs the break-in, then a synthwave sunset and chrome
     * HACKERMAN logo take over. Self-dismissing, and a tap closes it early.
     */
    private static class HackermanBurst extends View {
        private static final String[] TERMINAL = {
                "> CONNECTING TO MAINFRAME...",
                "> BYPASSING FIREWALL [OK]",
                "> TRACING ROUTE 0x1337",
                "> ACCESS GRANTED",
        };
        private static final int TERMINAL_CHARS = 112;

        private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mText = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mGlow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mGrid = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path mPath = new Path();
        private final float mDensity;
        private final long mStart = SystemClock.uptimeMillis();
        private boolean mClosing;

        HackermanBurst(Context context, float density) {
            super(context);
            mDensity = density;
            mText.setTextAlign(Paint.Align.CENTER);
            mText.setTypeface(Typeface.DEFAULT_BOLD);
            mGrid.setStyle(Paint.Style.STROKE);
            setClickable(true);
            setOnClickListener(v -> close());
        }

        void start() {
            postInvalidateOnAnimation();
            postDelayed(this::close, 5400);
        }

        private void close() {
            if (mClosing) return;
            mClosing = true;
            ViewGroup parent = (ViewGroup) getParent();
            if (parent != null) parent.removeView(this);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            final long elapsed = SystemClock.uptimeMillis() - mStart;
            final float t = elapsed / 1000f;
            final float w = getWidth();
            final float h = getHeight();
            if (w <= 0 || h <= 0) return;

            canvas.drawColor(0xF2000000);
            if (t < 1.15f) {
                drawTerminal(canvas, t, w, h);
            } else {
                drawTitleCard(canvas, t - 1.15f, w, h);
            }
            postInvalidateOnAnimation();
        }

        /** CRT wake-up plus a green terminal log of the alleged break-in. */
        private void drawTerminal(Canvas canvas, float t, float w, float h) {
            if (t < 0.32f) {
                // Thin bright line stretching open into a picture.
                float open = Math.min(1f, t / 0.32f);
                float half = h * 0.5f * open + 1f;
                mPaint.setStyle(Paint.Style.FILL);
                mPaint.setColor(0xCC8CFFC1);
                canvas.drawRect(0, h / 2f - half, w, h / 2f + half, mPaint);
                mPaint.setColor(0x3300FF66);
                canvas.drawRect(0, h / 2f - half - 3 * mDensity, w,
                        h / 2f + half + 3 * mDensity, mPaint);
                return;
            }
            final float typed = Math.min(1f, (t - 0.32f) / 0.7f) * TERMINAL_CHARS;
            mText.setTextAlign(Paint.Align.LEFT);
            mText.setTypeface(Typeface.MONOSPACE);
            mText.setFakeBoldText(false);
            mText.setTextSize(9.5f * mDensity);
            float lineHeight = 15 * mDensity;
            float y = h * 0.5f - lineHeight * TERMINAL.length * 0.5f + lineHeight;
            float left = 14 * mDensity;
            int budget = (int) typed;
            for (String line : TERMINAL) {
                int shown = Math.max(0, Math.min(line.length(), budget));
                budget -= line.length();
                if (shown == 0) break;
                // Occasional one-frame glitch keeps the CRT feel.
                float jitter = ((SystemClock.uptimeMillis() / 90) % 13 == 0) ? 2 * mDensity : 0;
                mText.setColor(0xFF39FF14);
                mText.setShadowLayer(6 * mDensity, 0, 0, 0x8839FF14);
                canvas.drawText(line.substring(0, shown), left + jitter, y, mText);
                mText.clearShadowLayer();
                y += lineHeight;
            }
            // Blinking cursor after the last visible character.
            if (((SystemClock.uptimeMillis() / 380) % 2) == 0) {
                mText.setColor(0xFFFFFFFF);
                canvas.drawRect(left, y - lineHeight + 2 * mDensity,
                        left + 7 * mDensity, y - lineHeight + 12 * mDensity, mPaint);
            }
        }

        /** Synthwave sky, striped sun, perspective grid and the chrome logo. */
        private void drawTitleCard(Canvas canvas, float t, float w, float h) {
            final float horizon = h * 0.60f;
            final float sunY = horizon - h * 0.10f;
            final float sunR = Math.min(w, h) * 0.19f;

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setShader(new LinearGradient(0, 0, 0, horizon,
                    new int[]{0xFF1B0533, 0xFF5B0F63, 0xFFC21B77, 0xFFFF6B6B},
                    new float[]{0f, 0.45f, 0.78f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, horizon, mPaint);
            mPaint.setShader(null);

            // Sun with the classic horizontal bands cut out of its lower half.
            mPaint.setShader(new LinearGradient(0, sunY - sunR, 0, sunY + sunR,
                    new int[]{0xFFFFE066, 0xFFFF9A3C, 0xFFFF4D6D},
                    new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawCircle(w / 2f, sunY, sunR, mPaint);
            mPaint.setShader(null);
            mPaint.setColor(0xFF5B0F63);
            for (int i = 0; i < 5; i++) {
                float bandTop = sunY + sunR * (0.10f + i * 0.17f);
                float thickness = sunR * (0.035f + i * 0.012f);
                canvas.drawRect(w / 2f - sunR, bandTop, w / 2f + sunR,
                        bandTop + thickness, mPaint);
            }

            // Lightning, flickering on alternating beats.
            if (((SystemClock.uptimeMillis() / 160) % 3) != 0) {
                mGlow.setStyle(Paint.Style.STROKE);
                mGlow.setStrokeWidth(1.6f * mDensity);
                mGlow.setColor(0x99CFFBFF);
                mGlow.setShadowLayer(8 * mDensity, 0, 0, 0xFF00E5FF);
                for (int side = 0; side < 2; side++) {
                    float baseX = side == 0 ? w * 0.16f : w * 0.84f;
                    float dir = side == 0 ? 1f : -1f;
                    mPath.reset();
                    mPath.moveTo(baseX, 0);
                    mPath.lineTo(baseX + dir * 9 * mDensity, h * 0.18f);
                    mPath.lineTo(baseX - dir * 4 * mDensity, h * 0.26f);
                    mPath.lineTo(baseX + dir * 12 * mDensity, h * 0.40f);
                    mPath.lineTo(baseX + dir * 2 * mDensity, horizon - 2 * mDensity);
                    canvas.drawPath(mPath, mGlow);
                }
                mGlow.clearShadowLayer();
            }

            // Perspective floor: verticals from the vanishing point, horizontals
            // scrolling toward the viewer.
            mGrid.setStrokeWidth(Math.max(1f, 1f * mDensity));
            mGrid.setColor(0xCC2AF7FF);
            for (int i = -7; i <= 7; i++) {
                float x = w / 2f + i * w * 0.5f;
                canvas.drawLine(w / 2f, horizon, x, h, mGrid);
            }
            mGrid.setColor(0x992AF7FF);
            float travel = (t * 0.55f) % (1f / 9f);
            for (int i = 0; i < 9; i++) {
                float p = (i / 9f + travel);
                p = p * p;
                float y = horizon + p * (h - horizon);
                canvas.drawLine(0, y, w, y, mGrid);
            }
            mPaint.setColor(0xFFFF2FB3);
            canvas.drawRect(0, horizon - 1 * mDensity, w, horizon + 1.5f * mDensity, mPaint);

            // Chrome HACKERMAN, sized to fit narrow panels.
            String title = "HACKERMAN";
            mText.setTextAlign(Paint.Align.CENTER);
            mText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            mText.setFakeBoldText(true);
            float titleSize = 40 * mDensity;
            mText.setTextSize(titleSize);
            float measured = mText.measureText(title);
            float maxWidth = w * 0.86f;
            if (measured > maxWidth) {
                titleSize *= maxWidth / measured;
                mText.setTextSize(titleSize);
            }
            float titleY = h * 0.42f;
            float glitch = ((SystemClock.uptimeMillis() / 120) % 17 == 0) ? 2 * mDensity : 0;
            mText.setColor(0xFFFF2FB3);
            mText.setShadowLayer(14 * mDensity, 0, 0, 0xFFFF2FB3);
            canvas.drawText(title, w / 2f - glitch, titleY, mText);
            mText.clearShadowLayer();
            mText.setColor(0xFFF4FBFF);
            canvas.drawText(title, w / 2f, titleY, mText);
            // A chrome highlight band and scanlines over the letters.
            mText.setColor(0xFF7FE9FF);
            canvas.save();
            canvas.clipRect(w / 2f - measured / 2f, titleY - titleSize * 0.52f,
                    w / 2f + measured / 2f, titleY - titleSize * 0.30f);
            canvas.drawText(title, w / 2f, titleY, mText);
            canvas.restore();
            mPaint.setColor(0x66000000);
            for (float y = titleY - titleSize * 0.55f; y < titleY + titleSize * 0.1f;
                    y += 3 * mDensity) {
                canvas.drawRect(w / 2f - measured / 2f, y,
                        w / 2f + measured / 2f, y + Math.max(1f, 1f * mDensity), mPaint);
            }

            // Dark band behind the strapline so it stays readable over the sun stripes.
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(0x99000000);
            canvas.drawRect(w * 0.06f, titleY + 6 * mDensity,
                    w * 0.94f, titleY + 44 * mDensity, mPaint);

            mText.setFakeBoldText(false);
            mText.setTypeface(Typeface.DEFAULT_BOLD);
            mText.setTextSize(8.5f * mDensity);
            mText.setColor(0xFFFFE3F4);
            canvas.drawText("IT'S 1980-SOMETHING AND HACKERS HACK",
                    w / 2f, titleY + 18 * mDensity, mText);
            mText.setTextSize(10 * mDensity);
            mText.setColor(0xCCBFFBFF);
            canvas.drawText("↑ ↑ ↓ ↓ ← → ← → B A",
                    w / 2f, titleY + 34 * mDensity, mText);
            mText.setTextSize(8 * mDensity);
            mText.setColor(0x99FFFFFF);
            canvas.drawText("tap to dismiss", w / 2f, h - 5 * mDensity, mText);
        }
    }

    /** A short 8-bit arpeggio, synthesised so no audio asset is needed. */
    private static class HackermanSound {
        static void play(final Context context) {
            try {
                android.media.AudioManager audio =
                        (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audio != null
                        && audio.getRingerMode() == android.media.AudioManager.RINGER_MODE_SILENT) {
                    return;
                }
            } catch (RuntimeException ignored) {
                // Never let the joke break the keyboard.
            }
            Thread thread = new Thread(() -> {
                android.media.AudioTrack track = null;
                try {
                    final int rate = 22050;
                    final double[] notes = {220.0, 277.18, 329.63, 440.0, 329.63, 277.18, 220.0};
                    final int per = rate / 8;          // eighth notes
                    final short[] samples = new short[per * notes.length];
                    for (int i = 0; i < samples.length; i++) {
                        int note = Math.min(notes.length - 1, i / per);
                        double phase = 2 * Math.PI * notes[note] * (i % per) / rate;
                        double square = Math.sin(phase) >= 0 ? 1 : -1;
                        double envelope = 1.0 - (i % per) / (double) per;
                        samples[i] = (short) (square * envelope * 0.20 * Short.MAX_VALUE);
                    }
                    track = new android.media.AudioTrack(
                            android.media.AudioManager.STREAM_MUSIC, rate,
                            android.media.AudioFormat.CHANNEL_OUT_MONO,
                            android.media.AudioFormat.ENCODING_PCM_16BIT,
                            samples.length * 2, android.media.AudioTrack.MODE_STATIC);
                    track.write(samples, 0, samples.length);
                    track.play();
                    Thread.sleep(notes.length * 1000L / 8 + 120);
                } catch (Throwable ignored) {
                    // Silent mode, no audio output, or a busy track: the visual still plays.
                } finally {
                    if (track != null) {
                        try {
                            track.stop();
                        } catch (Throwable ignored) {
                        }
                        try {
                            track.release();
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }, "konami-hackerman");
            thread.setDaemon(true);
            thread.start();
        }
    }
}
