package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * AndroidX dialog host for {@link SeekBarPreference}.
 */
public class SeekBarPreferenceDialog extends PreferenceDialogFragmentCompat {

    public static SeekBarPreferenceDialog newInstance(String key) {
        SeekBarPreferenceDialog fragment = new SeekBarPreferenceDialog();
        Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    private SeekBarPreference getSeekBarPreference() {
        return (SeekBarPreference) getPreference();
    }

    @Override
    protected View onCreateDialogView(@NonNull Context context) {
        return LayoutInflater.from(context).inflate(R.layout.seek_bar_dialog, null);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        SeekBarPreference preference = getSeekBarPreference();
        if (preference != null) {
            preference.bindDialogView(view);
        }
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        SeekBarPreference preference = getSeekBarPreference();
        if (preference != null) {
            preference.handleDialogClosed(positiveResult);
        }
    }
}
