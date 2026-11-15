package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.inputmethod.InputConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Trigger-to-text snippets. Typing a trigger followed by space replaces the
 * trigger with the stored expansion.
 */
public class SnippetStore {

    private static final String PREFS = "hk_snippets";
    private static final String KEY = "snippets";

    public static class Snippet {
        public final String trigger;
        public final String text;

        Snippet(String trigger, String text) {
            this.trigger = trigger;
            this.text = text;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Snippet> load(Context context) {
        List<Snippet> list = new ArrayList<>();
        String raw = prefs(context).getString(KEY, null);
        if (raw == null) return list;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String trigger = object.optString("t", "");
                if (!trigger.isEmpty()) {
                    list.add(new Snippet(trigger, object.optString("x", "")));
                }
            }
        } catch (JSONException ignored) {
            // discard a corrupted store
        }
        return list;
    }

    public static void save(Context context, List<Snippet> snippets) {
        JSONArray array = new JSONArray();
        for (Snippet snippet : snippets) {
            JSONObject object = new JSONObject();
            try {
                object.put("t", snippet.trigger);
                object.put("x", snippet.text);
            } catch (JSONException ignored) {
                continue;
            }
            array.put(object);
        }
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    public static void put(Context context, String trigger, String text) {
        if (trigger == null || trigger.trim().isEmpty()) return;
        String key = trigger.trim();
        List<Snippet> list = load(context);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).trigger.equals(key)) list.remove(i);
        }
        list.add(0, new Snippet(key, text == null ? "" : text));
        save(context, list);
    }

    public static void remove(Context context, String trigger) {
        List<Snippet> list = load(context);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).trigger.equals(trigger)) list.remove(i);
        }
        save(context, list);
    }

    /**
     * Replaces the word before the cursor if it matches a snippet trigger.
     *
     * @return true when an expansion was committed (the triggering space is swallowed).
     */
    public static boolean expand(LatinIME ime) {
        InputConnection ic = ime.getCurrentInputConnection();
        if (ic == null) return false;
        CharSequence before = ic.getTextBeforeCursor(48, 0);
        if (before == null || before.length() == 0) return false;

        String text = before.toString();
        int end = text.length();
        int start = end;
        while (start > 0 && !Character.isWhitespace(text.charAt(start - 1))) {
            start--;
        }
        if (start == end) return false;
        String word = text.substring(start, end);

        for (Snippet snippet : load(ime)) {
            if (snippet.trigger.equals(word)) {
                ic.beginBatchEdit();
                ic.deleteSurroundingText(end - start, 0);
                ic.commitText(snippet.text + " ", 1);
                ic.endBatchEdit();
                return true;
            }
        }
        return false;
    }
}
