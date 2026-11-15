package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import androidx.preference.ListPreference;

public class AutoSummaryListPreference extends ListPreference {
    private static final String TAG = "HK/AutoSummaryListPreference";

    public AutoSummaryListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void trySetSummary() {
        CharSequence entry = null;
        try {
            entry = getEntry();
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.i(TAG, "Malfunctioning ListPreference, can't get entry");
        }
        if (entry != null) {
            String percent = "percent";
            setSummary(entry.toString().replace("%", " " + percent));
        }
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        trySetSummary();
    }

    @Override
    public void setEntryValues(CharSequence[] entryValues) {
        super.setEntryValues(entryValues);
        trySetSummary();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        trySetSummary();
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return super.onGetDefaultValue(a, index);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        super.onSetInitialValue(defaultValue);
        trySetSummary();
    }
}
