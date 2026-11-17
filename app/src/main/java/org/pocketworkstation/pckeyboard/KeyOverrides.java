package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import org.pocketworkstation.pckeyboard.Keyboard.Key;

/**
 * Persists per-key label, output and text-colour overrides.
 *
 * <p>Keys are identified by their primary key code. The layout XML is reparsed
 * fresh for every keyboard, so the code remains the stable identity even when
 * an override changes the visible label or the text the key emits.
 */
public final class KeyOverrides {
    public static final String PREF = "pref_key_overrides";

    private KeyOverrides() {
    }

    public static final class Entry {
        public final String label;
        public final String output;
        public final int color;

        Entry(String label, String output, int color) {
            this.label = label == null ? "" : label;
            this.output = output == null ? "" : output;
            this.color = color;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    private static JSONObject load(Context context) {
        try {
            String raw = prefs(context).getString(PREF, "{}");
            return new JSONObject(raw == null || raw.isEmpty() ? "{}" : raw);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void save(Context context, JSONObject root) {
        prefs(context).edit().putString(PREF, root.toString()).commit();
        LatinIME.refreshKeyOverrides();
    }

    public static Entry get(Context context, int code) {
        JSONObject item = load(context).optJSONObject(Integer.toString(code));
        if (item == null) return null;
        return new Entry(item.optString("label", ""),
                item.optString("output", ""), item.optInt("color", 0));
    }

    public static void put(Context context, int code, String label, String output, int color) {
        if (code == 0) return;
        JSONObject root = load(context);
        try {
            if (TextUtils.isEmpty(label) && TextUtils.isEmpty(output) && color == 0) {
                root.remove(Integer.toString(code));
            } else {
                JSONObject item = new JSONObject();
                if (!TextUtils.isEmpty(label)) item.put("label", label);
                if (!TextUtils.isEmpty(output)) item.put("output", output);
                if (color != 0) item.put("color", color);
                root.put(Integer.toString(code), item);
            }
            save(context, root);
        } catch (JSONException ignored) {
        }
    }

    public static void remove(Context context, int code) {
        JSONObject root = load(context);
        root.remove(Integer.toString(code));
        save(context, root);
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(PREF).commit();
        LatinIME.refreshKeyOverrides();
    }

    public static int count(Context context) {
        return load(context).length();
    }

    /** Applies saved overrides to a freshly parsed keyboard. */
    public static void apply(Context context, List<Key> keys) {
        if (keys == null || keys.isEmpty()) return;
        JSONObject root = load(context);
        if (root.length() == 0) return;
        for (Key key : keys) {
            if (key == null || key.codes == null || key.codes.length == 0) continue;
            JSONObject item = root.optJSONObject(Integer.toString(key.codes[0]));
            if (item == null) continue;
            String label = item.optString("label", "");
            String output = item.optString("output", "");
            int color = item.optInt("color", 0);
            if (!TextUtils.isEmpty(label)) {
                key.label = label;
                key.shiftLabel = label;
                key.capsLabel = label;
            }
            if (!TextUtils.isEmpty(output) && key.codes[0] >= 32) {
                // Emit the custom text directly; this keeps the key's identity
                // stable while allowing words and symbols, not just characters.
                // Modifier keys keep their special behaviour.
                key.text = output;
                if (TextUtils.isEmpty(label)) {
                    key.label = output;
                    key.shiftLabel = output;
                    key.capsLabel = output;
                }
            }
            if (color != 0) key.customTextColor = color;
        }
    }
}
