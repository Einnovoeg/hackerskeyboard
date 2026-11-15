package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Per-app theme overrides: package name -> theme index. */
public final class AppThemeRules {

    private static final String PREFS = "hk_app_themes";
    private static final String KEY = "rules";
    public static final int NO_RULE = -1;

    public static class Rule {
        public final String packageName;
        public final int theme;

        Rule(String packageName, int theme) {
            this.packageName = packageName;
            this.theme = theme;
        }
    }

    private AppThemeRules() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static JSONObject read(Context context) {
        try {
            return new JSONObject(prefs(context).getString(KEY, "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public static int themeFor(Context context, String packageName) {
        if (packageName == null) return NO_RULE;
        JSONObject rules = read(context);
        if (!rules.has(packageName)) return NO_RULE;
        return rules.optInt(packageName, NO_RULE);
    }

    public static void put(Context context, String packageName, int theme) {
        if (packageName == null || packageName.trim().isEmpty()) return;
        JSONObject rules = read(context);
        try {
            rules.put(packageName.trim(), theme);
        } catch (JSONException e) {
            return;
        }
        prefs(context).edit().putString(KEY, rules.toString()).apply();
    }

    public static void remove(Context context, String packageName) {
        JSONObject rules = read(context);
        rules.remove(packageName);
        prefs(context).edit().putString(KEY, rules.toString()).apply();
    }

    public static List<Rule> all(Context context) {
        List<Rule> list = new ArrayList<>();
        JSONObject rules = read(context);
        Iterator<String> keys = rules.keys();
        while (keys.hasNext()) {
            String pkg = keys.next();
            list.add(new Rule(pkg, rules.optInt(pkg, 0)));
        }
        return list;
    }

    /** Packages currently targeted, for display. */
    public static String describe(Context context, int theme) {
        String[] labels = context.getResources().getStringArray(R.array.keyboard_layout_modes);
        return theme >= 0 && theme < labels.length ? labels[theme] : Integer.toString(theme);
    }

    static JSONArray emptyArray() {
        return new JSONArray();
    }
}
