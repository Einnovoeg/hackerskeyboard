package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/** Hosts the miniature keyboard and redraws it whenever a visual option changes. */
public class KeyboardPreviewPreference extends Preference
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private FrameLayout mHolder;
    private KeyboardPreview mPreview;
    private SharedPreferences mPrefs;

    public KeyboardPreviewPreference(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_preview);
        setSelectable(false);
        setPersistent(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHolder = (FrameLayout) holder.findViewById(R.id.preview_slot);
        if (mPreview == null) {
            mPreview = new KeyboardPreview(getContext(), null);
        }
        if (mPreview.getParent() != mHolder) {
            if (mPreview.getParent() instanceof ViewGroup) {
                ((ViewGroup) mPreview.getParent()).removeView(mPreview);
            }
            mHolder.removeAllViews();
            int height = Math.round(170 * getContext().getResources().getDisplayMetrics().density);
            mHolder.addView(mPreview, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, height));
        }
        attach();
        mPreview.reload();
    }

    private void attach() {
        if (mPrefs == null) {
            mPrefs = getContext().getSharedPreferences(
                    getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }
    }

    @Override
    public void onDetached() {
        if (mPrefs != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(this);
            mPrefs = null;
        }
        mHolder = null;
        super.onDetached();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (mPreview == null || key == null) return;
        if (key.equals("pref_keyboard_layout") || key.equals("pref_key_layout")
                || key.equals(BackgroundImages.PREF)
                || key.equals("pref_key_effect") || key.equals("pref_background_dim")
                || key.equals("pref_key_effect_speed") || key.equals("pref_key_effect_intensity")
                || key.equals("pref_key_accent") || key.equals("pref_key_shadow_blur")
                || key.equals("pref_key_radius") || key.equals("pref_key_opacity")
                || key.equals("pref_key_shadow") || key.equals("pref_key_glyphs")
                || key.equals("pref_key_font") || key.equals("pref_key_text_color")
                || key.equals("pref_key_bold") || key.equals("pref_key_height_scale")
                || key.equals("pref_key_style") || key.equals("pref_key_tracking")
                || key.equals("pref_key_press_effect") || key.equals("pref_key_rgb_mode")
                || key.equals("pref_key_rgb_speed") || key.equals("pref_key_rgb_hue")
                || key.equals("pref_key_rgb_brightness") || key.equals("pref_key_overrides")
                || key.equals("pref_label_scale_v2") || key.equals("pref_top_row_scale")
                || key.equals("pref_candidate_scale") || key.equals("pref_hint_mode")) {
            mPreview.post(mPreview::reload);
        }
    }
}
