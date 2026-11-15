package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** User-defined macros: a name plus a sequence of text and {key} steps. */
public class MacroStore {

    private static final String PREFS = "hk_macros";
    private static final String KEY = "macros";

    public static class Macro {
        public final String name;
        public final String steps;

        Macro(String name, String steps) {
            this.name = name;
            this.steps = steps;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static List<Macro> load(Context context) {
        List<Macro> macros = new ArrayList<>();
        String raw = prefs(context).getString(KEY, null);
        if (raw == null) return macros;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String name = object.optString("n", "");
                if (!name.isEmpty()) {
                    macros.add(new Macro(name, object.optString("s", "")));
                }
            }
        } catch (JSONException ignored) {
            // discard a corrupted store
        }
        return macros;
    }

    public static void save(Context context, List<Macro> macros) {
        JSONArray array = new JSONArray();
        for (Macro macro : macros) {
            JSONObject object = new JSONObject();
            try {
                object.put("n", macro.name);
                object.put("s", macro.steps);
            } catch (JSONException ignored) {
                continue;
            }
            array.put(object);
        }
        prefs(context).edit().putString(KEY, array.toString()).apply();
    }

    public static void put(Context context, String name, String steps) {
        if (name == null || name.trim().isEmpty()) return;
        List<Macro> macros = load(context);
        for (int i = macros.size() - 1; i >= 0; i--) {
            if (macros.get(i).name.equals(name.trim())) macros.remove(i);
        }
        macros.add(0, new Macro(name.trim(), steps == null ? "" : steps));
        save(context, macros);
    }

    public static void remove(Context context, String name) {
        List<Macro> macros = load(context);
        for (int i = macros.size() - 1; i >= 0; i--) {
            if (macros.get(i).name.equals(name)) macros.remove(i);
        }
        save(context, macros);
    }
}
