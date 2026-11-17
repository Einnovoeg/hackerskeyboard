package org.pocketworkstation.pckeyboard;

import android.content.Context;
import android.content.SharedPreferences;

/** The bundled high-resolution background art, indexed like the picker. */
public final class BackgroundImages {

    public static final String PREF = "pref_background_image";

    public static final int[] DRAWABLES = {
            0,                      // None
            R.drawable.bg_mesh,
            R.drawable.bg_bokeh,
            R.drawable.bg_aurora,
            R.drawable.bg_waves,
            R.drawable.bg_dusk,
            R.drawable.bg_dawn,
            R.drawable.bg_marble,
            R.drawable.bg_silk,
            R.drawable.bg_neon,
            R.drawable.bg_minimal,
            R.drawable.bg_emerald,
            R.drawable.bg_copper,
            R.drawable.bg_prism,
            R.drawable.bg_dunes,
            R.drawable.bg_lavender,
            R.drawable.bg_circuit,
            R.drawable.bg_rainglass,
            R.drawable.bg_gold,
            R.drawable.bg_lagoon,
            R.drawable.bg_ember,
            R.drawable.bg_cobalt,
            R.drawable.bg_blossom,
            R.drawable.bg_glacier,
            R.drawable.bg_obsidian,
            R.drawable.bg_abyss,
            R.drawable.bg_solaris,
            R.drawable.bg_orchid,
            R.drawable.bg_titanium,
            R.drawable.bg_reef,
            R.drawable.bg_volcano,
            R.drawable.bg_arctic,
            R.drawable.bg_mirage,
    };

    private BackgroundImages() {
    }

    public static int selected(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        try {
            int index = Integer.parseInt(prefs.getString(PREF, "0"));
            return index >= 0 && index < DRAWABLES.length ? index : 0;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public static void select(Context context, int index) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF, Integer.toString(index)).apply();
    }

    public static int drawableFor(int index) {
        return index > 0 && index < DRAWABLES.length ? DRAWABLES[index] : 0;
    }
}
