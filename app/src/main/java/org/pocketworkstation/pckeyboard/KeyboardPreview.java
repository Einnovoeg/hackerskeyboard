package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Settings preview that renders the real keyboard view: the theme layout is inflated
 * exactly as the IME does and driven by a LatinKeyboard built from the same XML, so the
 * miniature cannot drift from what the keyboard actually shows.
 *
 * <p>All visual tuning is pushed through the same view methods the live keyboard uses
 * ({@link LatinKeyboardView#applyCustomization}, {@code applyKeyOpacity},
 * {@code applyKeyShadow}), including the background dim overlay, so the preview is a
 * faithful miniature rather than a separate approximation.
 */
public class KeyboardPreview extends FrameLayout {

    private final SharedPreferences mPrefs;
    private LatinKeyboardView mInner;
    private float mScale = 1f;
    private float mHeightScale = 1f;

    public KeyboardPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPrefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        setClipChildren(true);
    }

    /** Reads a float preference, falling back when it is missing or malformed. */
    private float prefFloat(String key, float fallback) {
        try {
            return Float.parseFloat(mPrefs.getString(key, Float.toString(fallback)));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Re-reads every visual preference and rebuilds the miniature keyboard. */
    public void reload() {
        Context context = getContext();
        // Refresh the shared settings the keyboard renderer reads, from the same prefs.
        Resources res = context.getResources();
        LatinIME.sKeyboardSettings.initPrefs(mPrefs, res);
        for (String key : new String[]{"pref_key_effect", "pref_key_effect_speed",
                "pref_key_effect_intensity", "pref_key_accent", "pref_key_press_effect",
                "pref_key_rgb_mode", "pref_key_rgb_speed", "pref_key_rgb_hue",
                "pref_key_rgb_brightness", "pref_key_overrides", "pref_keyboard_layout",
                "pref_key_layout", "pref_key_glyphs", "pref_key_font", "pref_key_text_color",
                "pref_key_bold", "pref_label_scale_v2", "pref_key_height_scale",
                "pref_top_row_scale"}) {
            if (mPrefs.contains(key)) {
                LatinIME.sKeyboardSettings.sharedPreferenceChanged(mPrefs, key);
            }
        }

        int theme = Math.max(0, Math.min((int) prefFloat("pref_keyboard_layout", 8f),
                KeyboardSwitcher.THEMES.length - 1));
        TuningValues tuning = TuningValues.fromStrings(
                mPrefs.getString("pref_key_height_scale", "1.0"),
                mPrefs.getString("pref_key_radius", "10"),
                mPrefs.getString("pref_background_dim", "0"),
                mPrefs.getString("pref_key_opacity", "100"),
                mPrefs.getString("pref_key_shadow", "100"));
        mHeightScale = tuning.heightScale;

        removeAllViews();
        mInner = null;
        try {
            View inflated = LayoutInflater.from(context)
                    .inflate(KeyboardSwitcher.THEMES[theme], this, false);
            if (inflated instanceof LatinKeyboardView) {
                mInner = (LatinKeyboardView) inflated;
            }
        } catch (RuntimeException e) {
            mInner = null;
        }
        if (mInner == null) {
            mInner = new LatinKeyboardView(context, null);
        }
        mInner.setPreviewMode(true);
        mInner.setClickable(false);
        mInner.setFocusable(false);
        mInner.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        mInner.setBackgroundResource(themeDrawable(context, theme));
        mInner.applyCustomization(tuning.heightScale, tuning.radiusDp, tuning.backgroundDim);
        mInner.applyKeyOpacity(tuning.keyOpacity);
        mInner.applyKeyShadow(tuning.keyShadow);
        float shadowBlur = prefFloat("pref_key_shadow_blur", 0f);
        mInner.applyKeyShadowBlur(shadowBlur);
        mInner.applyBackgroundEffect(
                prefFloat("pref_key_effect_speed", 1f),
                prefFloat("pref_key_effect_intensity", 85f) / 100f);
        int bg = BackgroundImages.selected(context);
        if (bg > 0) {
            Drawable art = context.getDrawable(BackgroundImages.DRAWABLES[bg]);
            mInner.applyBackgroundImage(art);
        }
        try {
            float heightPct = LatinIME.sKeyboardSettings.keyboardHeightPercent;
            if (heightPct <= 0f) heightPct = 40f;
            // Same source as the live keyboard, so the preview always shows the
            // layout (QWERTY/AZERTY/Dvorak) the keyboard will actually type.
            LatinKeyboard keyboard = new LatinKeyboard(context,
                    KeyboardSwitcher.fullLayoutXml(), 0, heightPct);
            mInner.setKeyboard(keyboard);
        } catch (RuntimeException ignored) {
            // A malformed custom layout must never take the settings screen down.
        }
        addView(mInner, new LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        requestLayout();
        invalidate();
    }

    private int themeDrawable(Context context, int theme) {
        int id = context.getResources().getIdentifier(
                "keyboard_background_" + themeName(context, theme), "drawable",
                context.getPackageName());
        return id != 0 ? id : R.drawable.keyboard_background_modern_dark;
    }

    private String themeName(Context context, int theme) {
        String[] names = GamepadPanel.THEME_NAMES;
        return names[Math.max(0, Math.min(theme, names.length - 1))];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width <= 0) width = getResources().getDisplayMetrics().widthPixels;
        int childWidth = width;
        int childHeight = Math.round(200 * getResources().getDisplayMetrics().density);
        if (mInner != null) {
            mInner.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            if (mInner.getMeasuredWidth() > 0 && mInner.getMeasuredHeight() > 0) {
                childWidth = mInner.getMeasuredWidth();
                childHeight = mInner.getMeasuredHeight();
            }
        }
        mScale = Math.min(width / (float) Math.max(1, childWidth),
                getResources().getDisplayMetrics().density * 170f / Math.max(1, childHeight));
        if (mScale > 1f) mScale = 1f;
        int scaledHeight = Math.max(1, Math.round(childHeight * mScale));
        setMeasuredDimension(width, scaledHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mInner == null) return;
        int width = right - left;
        int childWidth = Math.max(1, mInner.getMeasuredWidth());
        int childHeight = Math.max(1, mInner.getMeasuredHeight());
        mInner.layout(0, 0, childWidth, childHeight);
        mInner.setPivotX(0);
        mInner.setPivotY(0);
        mInner.setScaleX(mScale);
        // applyCustomization() already set the key-height scale, so the miniature
        // has to combine it with the fit-to-width scale instead of replacing it.
        mInner.setScaleY(mScale * mHeightScale);
        mInner.setTranslationX((width - childWidth * mScale) / 2f);
        mInner.setTranslationY((bottom - top - childHeight * mScale * mHeightScale) / 2f);
    }
}
