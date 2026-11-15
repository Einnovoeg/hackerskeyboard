package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent multi-entry clipboard history.
 *
 * <p>Entries are kept newest-first. Pinned entries are never trimmed, unpinned ones
 * fall off the end once {@link #MAX_ENTRIES} is reached.
 */
public class ClipboardHistory {
    public static final int MAX_ENTRIES = 60;

    private static final String PREFS = "hk_clipboard";
    private static final String KEY_ENTRIES = "entries";

    public static class Entry {
        public final String text;
        public final long time;
        public boolean pinned;

        Entry(String text, long time, boolean pinned) {
            this.text = text;
            this.time = time;
            this.pinned = pinned;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Entry> load(Context context) {
        List<Entry> entries = new ArrayList<>();
        String raw = prefs(context).getString(KEY_ENTRIES, null);
        if (raw == null) return entries;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                entries.add(new Entry(object.optString("text", ""),
                        object.optLong("time", 0), object.optBoolean("pinned", false)));
            }
        } catch (JSONException ignored) {
            // a corrupted history is simply discarded
        }
        return entries;
    }

    public static void save(Context context, List<Entry> entries) {
        JSONArray array = new JSONArray();
        for (Entry entry : entries) {
            JSONObject object = new JSONObject();
            try {
                object.put("text", entry.text);
                object.put("time", entry.time);
                object.put("pinned", entry.pinned);
            } catch (JSONException ignored) {
                continue;
            }
            array.put(object);
        }
        prefs(context).edit().putString(KEY_ENTRIES, array.toString()).apply();
    }

    /** Adds a clip, moving an identical older entry back to the top. */
    public static void add(Context context, String text) {
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        List<Entry> entries = load(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).text.equals(text)) {
                entries.remove(i);
            }
        }
        entries.add(0, new Entry(text, System.currentTimeMillis(), false));

        int unpinned = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).pinned) continue;
            unpinned++;
            if (unpinned > MAX_ENTRIES) {
                entries.remove(i);
            }
        }
        save(context, entries);
    }

    public static void clearUnpinned(Context context) {
        List<Entry> entries = load(context);
        List<Entry> kept = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.pinned) kept.add(entry);
        }
        save(context, kept);
    }
}
