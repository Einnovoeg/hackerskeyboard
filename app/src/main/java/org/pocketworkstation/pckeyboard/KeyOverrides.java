package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import org.pocketworkstation.pckeyboard.Keyboard.Key;

/**
 * Persists Vial-style per-layer key bindings.
 *
 * <p>Every entry is keyed by a stable layer id (layout XML resource plus
 * keyboard mode) and the key code that appeared in the layout. The layout is
 * reparsed for every keyboard, so the original code remains a durable key
 * identity even though the visible label and emitted code are replaced.</p>
 *
 * <p>Old flat label/output/colour entries are still read as a fallback so
 * customisations made in 3.0 keep working. Editing a layer writes only that
 * layer and removes the old flat entry for the key.</p>
 */
public final class KeyOverrides {
    public static final String PREF = "pref_key_overrides";

    public static final String KIND_STYLE = "style";
    public static final String KIND_CHAR = "char";
    public static final String KIND_ACTION = "action";
    public static final String KIND_TEXT = "text";

    private KeyOverrides() {
    }

    public static final class Entry {
        public final String kind;
        public final String value;
        public final String label;
        public final int color;

        Entry(String kind, String value, String label, int color) {
            this.kind = kind == null ? KIND_STYLE : kind;
            this.value = value == null ? "" : value;
            this.label = label == null ? "" : label;
            this.color = color;
        }

        public boolean hasBinding() {
            return (KIND_CHAR.equals(kind) || KIND_ACTION.equals(kind)
                    || KIND_TEXT.equals(kind)) && !TextUtils.isEmpty(value);
        }

        public String describe() {
            if (KIND_CHAR.equals(kind)) return value;
            if (KIND_ACTION.equals(kind)) {
                KeyActions.Spec spec = KeyActions.byId(value);
                return spec == null ? "Unknown action" : spec.title;
            }
            if (KIND_TEXT.equals(kind)) return "Text: " + value;
            return "Style only";
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

    /**
     * Builds the stable identity shared by the live keyboard and the editor.
     * Settings-key variants are deliberately collapsed because they are the
     * same logical layer with one extra row.
     */
    public static String layerId(Context context, int xmlLayoutResId, int modeId) {
        String xml = resourceName(context, xmlLayoutResId);
        String mode = modeId == 0 ? "mode_none" : resourceName(context, modeId);
        if (mode.startsWith("mode_normal")) {
            mode = "mode_normal";
        } else if (mode.startsWith("mode_symbols")) {
            mode = "mode_symbols";
        }
        return xml + ":" + mode;
    }

    private static String resourceName(Context context, int resId) {
        if (resId == 0) return "none";
        try {
            return context.getResources().getResourceEntryName(resId);
        } catch (RuntimeException e) {
            return "id_" + resId;
        }
    }

    private static JSONObject layers(JSONObject root, boolean create) {
        JSONObject layers = root.optJSONObject("layers");
        if (layers == null && create) {
            layers = new JSONObject();
            try {
                root.put("layers", layers);
            } catch (JSONException ignored) {
            }
        }
        return layers;
    }

    private static JSONObject layerObject(JSONObject root, String layerId, boolean create) {
        JSONObject layers = layers(root, create);
        if (layers == null) return null;
        JSONObject layer = layers.optJSONObject(layerId);
        if (layer == null && create) {
            layer = new JSONObject();
            try {
                layers.put(layerId, layer);
            } catch (JSONException ignored) {
            }
        }
        return layer;
    }

    public static Entry get(Context context, String layerId, int code) {
        JSONObject root = load(context);
        JSONObject layer = layerObject(root, layerId, false);
        JSONObject item = layer == null ? null : layer.optJSONObject(Integer.toString(code));
        if (item != null) return parseEntry(item);
        return legacyEntry(root, code);
    }

    private static Entry legacyEntry(JSONObject root, int code) {
        JSONObject item = root.optJSONObject(Integer.toString(code));
        if (item == null) return null;
        String label = item.optString("label", "");
        String output = item.optString("output", "");
        int color = item.optInt("color", 0);
        if (TextUtils.isEmpty(label) && TextUtils.isEmpty(output) && color == 0) return null;
        if (!TextUtils.isEmpty(output)) {
            String error = KeyActions.characterError(output);
            if (error == null) {
                return new Entry(KIND_CHAR, output, label, color);
            }
            return new Entry(KIND_TEXT, output, label, color);
        }
        return new Entry(KIND_STYLE, "", label, color);
    }

    private static Entry parseEntry(JSONObject item) {
        String kind = item.optString("kind", KIND_STYLE);
        String value = item.optString("value", "");
        return new Entry(kind, value, item.optString("label", ""),
                item.optInt("color", 0));
    }

    /** True if the value is a complete, usable binding for the requested kind. */
    public static boolean isValidBinding(String kind, String value) {
        if (KIND_ACTION.equals(kind)) {
            return KeyActions.byId(value) != null;
        }
        if (KIND_CHAR.equals(kind)) {
            return KeyActions.characterError(value) == null;
        }
        if (KIND_TEXT.equals(kind)) {
            return !TextUtils.isEmpty(value);
        }
        return KIND_STYLE.equals(kind);
    }

    public static void put(Context context, String layerId, int code, String kind,
            String value, String label, int color) {
        if (code == 0 || TextUtils.isEmpty(layerId)) return;
        if (!isValidBinding(kind, value)) return;
        JSONObject root = load(context);
        JSONObject layer = layerObject(root, layerId, true);
        if (layer == null) return;
        try {
            // A layer-specific edit supersedes the legacy global entry.
            root.remove(Integer.toString(code));
            boolean empty = KIND_STYLE.equals(kind) && TextUtils.isEmpty(label) && color == 0;
            if (empty) {
                layer.remove(Integer.toString(code));
            } else {
                JSONObject item = new JSONObject();
                item.put("kind", kind);
                if (!TextUtils.isEmpty(value)) item.put("value", value);
                if (!TextUtils.isEmpty(label)) item.put("label", label);
                if (color != 0) item.put("color", color);
                layer.put(Integer.toString(code), item);
            }
            save(context, root);
        } catch (JSONException ignored) {
        }
    }

    public static void remove(Context context, String layerId, int code) {
        JSONObject root = load(context);
        JSONObject layer = layerObject(root, layerId, false);
        boolean changed = false;
        if (layer != null) {
            changed = layer.remove(Integer.toString(code)) != null;
        }
        changed |= root.remove(Integer.toString(code)) != null;
        if (changed) save(context, root);
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(PREF).commit();
        LatinIME.refreshKeyOverrides();
    }

    public static int count(Context context) {
        JSONObject root = load(context);
        int count = 0;
        JSONObject layers = layers(root, false);
        if (layers != null) {
            Iterator<String> layerNames = layers.keys();
            while (layerNames.hasNext()) {
                JSONObject layer = layers.optJSONObject(layerNames.next());
                if (layer != null) count += layer.length();
            }
        }
        Iterator<String> flat = root.keys();
        while (flat.hasNext()) {
            String key = flat.next();
            if (!"layers".equals(key) && root.optJSONObject(key) != null) count++;
        }
        return count;
    }

    /** Applies saved bindings to a freshly parsed keyboard. */
    public static void apply(Context context, String layerId, List<Key> keys) {
        if (keys == null || keys.isEmpty()) return;
        JSONObject root = load(context);
        JSONObject layer = layerObject(root, layerId, false);
        if (layer == null && root.length() == 0) return;
        for (Key key : keys) {
            if (key == null || key.originalCodes == null || key.originalCodes.length == 0) {
                continue;
            }
            int code = key.originalCodes[0];
            JSONObject item = layer == null ? null : layer.optJSONObject(Integer.toString(code));
            Entry entry = item != null ? parseEntry(item) : legacyEntry(root, code);
            if (entry != null) applyEntry(key, entry);
        }
    }

    private static void applyEntry(Key key, Entry entry) {
        if (entry.color != 0) {
            key.customTextColor = entry.color;
        }

        if (entry.hasBinding()) {
            // The replacement must own all output and popup state; leaving the
            // old popup on a remapped key would make it emit its old character.
            key.icon = null;
            key.iconPreview = null;
            key.popupCharacters = null;
            key.popupResId = 0;
            key.text = null;
            key.remapped = true;
            key.sticky = false;
            key.on = false;
            key.locked = false;

            if (KIND_ACTION.equals(entry.kind)) {
                KeyActions.Spec spec = KeyActions.byId(entry.value);
                if (spec == null) return;
                if (spec.isCustomAction()) {
                    key.codes = new int[] {Keyboard.KEYCODE_CUSTOM_ACTION, spec.customActionId};
                } else {
                    key.codes = new int[] {spec.code};
                }
                CharSequence label = TextUtils.isEmpty(entry.label)
                        ? spec.keyLabel : entry.label;
                key.label = label;
                key.shiftLabel = label;
                key.capsLabel = label;
                key.modifier = spec.modifier;
                key.repeatable = spec.repeatable;
            } else if (KIND_CHAR.equals(entry.kind)) {
                if (KeyActions.characterError(entry.value) != null) return;
                int codePoint = entry.value.codePointAt(0);
                key.codes = new int[] {codePoint};
                CharSequence label = TextUtils.isEmpty(entry.label)
                        ? entry.value : entry.label;
                key.label = label;
                // Keep the exact remapped character even when shift is active.
                key.shiftLabel = entry.value;
                key.capsLabel = entry.value;
                key.modifier = false;
                key.repeatable = false;
            } else if (KIND_TEXT.equals(entry.kind)) {
                CharSequence label = TextUtils.isEmpty(entry.label)
                        ? entry.value : entry.label;
                key.label = label;
                key.shiftLabel = label;
                key.capsLabel = label;
                key.text = entry.value;
                key.modifier = false;
                key.repeatable = false;
            }
        } else if (!TextUtils.isEmpty(entry.label)) {
            // Appearance-only customisation preserves the original behaviour.
            key.label = entry.label;
            key.shiftLabel = entry.label;
            key.capsLabel = entry.label;
        }
    }
}
