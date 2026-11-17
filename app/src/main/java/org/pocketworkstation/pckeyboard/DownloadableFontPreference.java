package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.util.AttributeSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Key-font list that appends fonts installed through the font library. The XML
 * arrays remain the ten built-in choices; downloaded fonts use fixed slots from
 * {@link DownloadedFonts#FIRST_INDEX} so their selection survives catalogue changes.
 */
public class DownloadableFontPreference extends AutoSummaryListPreference {

    private CharSequence[] mBaseEntries;
    private CharSequence[] mBaseValues;

    public DownloadableFontPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        refreshFonts();
    }

    /** Rebuilds the list, for example when returning from the font library. */
    public void refreshFonts() {
        if (mBaseEntries == null || mBaseValues == null) {
            CharSequence[] entries = getEntries();
            CharSequence[] values = getEntryValues();
            if (entries == null || values == null) return;
            mBaseEntries = entries.clone();
            mBaseValues = values.clone();
        }

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        for (int i = 0; i < mBaseEntries.length && i < mBaseValues.length; i++) {
            entries.add(mBaseEntries[i]);
            values.add(mBaseValues[i]);
        }
        for (DownloadedFonts.Font font : DownloadedFonts.installedFonts(getContext())) {
            entries.add(font.name);
            values.add(Integer.toString(DownloadedFonts.indexOf(font)));
        }
        setEntries(entries.toArray(new CharSequence[0]));
        setEntryValues(values.toArray(new CharSequence[0]));

        String current = getValue();
        if (current != null) {
            try {
                int index = Integer.parseInt(current);
                if (index >= DownloadedFonts.FIRST_INDEX
                        && DownloadedFonts.forIndex(index) == null) {
                    setValue("0");
                    return;
                }
                if (index >= DownloadedFonts.FIRST_INDEX) {
                    DownloadedFonts.Font font = DownloadedFonts.forIndex(index);
                    if (font != null && !DownloadedFonts.isInstalled(getContext(), font)) {
                        setValue("0");
                        return;
                    }
                }
            } catch (RuntimeException ignored) {
            }
            // Refresh the summary after the entries changed.
            setValue(current);
        }
    }
}
