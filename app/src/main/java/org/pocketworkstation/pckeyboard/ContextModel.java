package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.inputmethod.InputConnection;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Learns which words follow which from the sentences the user types, and predicts
 * the next word from the previous one. Small on-device model: a counting table of
 * word pairs, capped so it never grows without bound.
 */
public class ContextModel {

    private static final String PREFS = "hk_context";
    private static final String KEY = "pairs";
    private static final int MAX_PAIRS = 800;
    private static final int MAX_PREDICTIONS = 3;

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static JSONObject loadPairs(Context context) {
        try {
            return new JSONObject(prefs(context).getString(KEY, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    /** Records the pair formed by the last two words before the cursor. */
    public static void learn(LatinIME ime) {
        InputConnection ic = ime.getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(96, 0);
        if (before == null) return;

        String[] words = before.toString().trim().split("\\s+");
        if (words.length < 2) return;
        String previous = clean(words[words.length - 2]);
        String current = clean(words[words.length - 1]);
        if (previous.isEmpty() || current.isEmpty()) return;

        JSONObject pairs = loadPairs(ime);
        try {
            JSONObject followers = pairs.optJSONObject(previous);
            if (followers == null) followers = new JSONObject();
            followers.put(current, followers.optInt(current, 0) + 1);
            pairs.put(previous, followers);

            if (pairs.length() > MAX_PAIRS) {
                // drop the oldest-ish key we happen to see; keeps the table bounded
                Iterator<String> keys = pairs.keys();
                if (keys.hasNext()) pairs.remove(keys.next());
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs(ime).edit().putString(KEY, pairs.toString()).apply();
    }

    /** Most likely words to follow {@code previous}. */
    public static List<CharSequence> predict(Context context, String previous) {
        List<CharSequence> result = new ArrayList<>();
        String key = clean(previous);
        if (key.isEmpty()) return result;

        JSONObject followers = loadPairs(context).optJSONObject(key);
        if (followers == null) return result;

        List<String> words = new ArrayList<>();
        Iterator<String> keys = followers.keys();
        while (keys.hasNext()) words.add(keys.next());
        Collections.sort(words, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                return followers.optInt(b, 0) - followers.optInt(a, 0);
            }
        });
        for (int i = 0; i < words.size() && i < MAX_PREDICTIONS; i++) {
            if (followers.optInt(words.get(i), 0) >= 1) {
                result.add(words.get(i));
            }
        }
        return result;
    }

    private static String clean(String word) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (Character.isLetter(c) || c == '\'') {
                builder.append(c);
            }
        }
        return builder.toString().toLowerCase(Locale.US);
    }
}
