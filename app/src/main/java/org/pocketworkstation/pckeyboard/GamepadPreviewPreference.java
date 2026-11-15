package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

/** Live, non-interactive rendering of the gamepad, driven by the gamepad rows. */
public class GamepadPreviewPreference extends Preference
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private FrameLayout mHolder;
    private SharedPreferences mPrefs;

    public GamepadPreviewPreference(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference_preview);
        setSelectable(false);
        setPersistent(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mHolder = (FrameLayout) holder.findViewById(R.id.preview_slot);
        attach();
        rebuild();
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
        if (key == null) return;
        if (key.startsWith("pref_gamepad_")
                || key.equals(GamepadPanel.PREF_GLYPHS)
                || key.equals(BackgroundImages.PREF)
                || key.equals("pref_keyboard_layout")) {
            rebuild();
        }
    }

    private void rebuild() {
        if (mHolder == null) return;
        float scale = 0.62f;
        mHolder.removeAllViews();
        View pad = GamepadPanel.buildPreview(getContext(), scale);
        mHolder.addView(pad, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
}
