package org.pocketworkstation.pckeyboard;

/**
 * Parsed, clamped values for the visual tuning preferences.
 *
 * <p>The settings UI stores everything as strings because these keys date back to
 * the original preference implementation. Keeping the parsing here means the live
 * keyboard, the settings preview and the JVM tests all agree on defaults, units and
 * limits. Values are:
 * <ul>
 *   <li>height: multiplier for the key row height (0.6 – 1.6)</li>
 *   <li>radius: key corner radius in dp (0 – 32)</li>
 *   <li>dim: black overlay opacity over the background (0 – 0.85)</li>
 *   <li>opacity: key cap opacity percent, converted to 0.4 – 1.0</li>
 *   <li>shadow: drop shadow percent, converted to 0 – 2.0</li>
 * </ul>
 */
public final class TuningValues {

    public static final float DEFAULT_HEIGHT_SCALE = 1.0f;
    public static final float DEFAULT_RADIUS_DP = 10.0f;
    public static final float DEFAULT_BACKGROUND_DIM = 0.0f;
    public static final float DEFAULT_KEY_OPACITY = 1.0f;
    public static final float DEFAULT_KEY_SHADOW = 1.0f;

    public static final float MIN_HEIGHT_SCALE = 0.6f;
    public static final float MAX_HEIGHT_SCALE = 1.6f;
    public static final float MIN_RADIUS_DP = 0.0f;
    public static final float MAX_RADIUS_DP = 32.0f;
    public static final float MAX_BACKGROUND_DIM = 0.85f;
    public static final float MIN_KEY_OPACITY = 0.4f;
    public static final float MAX_KEY_SHADOW = 2.0f;

    public final float heightScale;
    public final float radiusDp;
    public final float backgroundDim;
    public final float keyOpacity;
    public final float keyShadow;

    private TuningValues(float heightScale, float radiusDp, float backgroundDim,
            float keyOpacity, float keyShadow) {
        this.heightScale = heightScale;
        this.radiusDp = radiusDp;
        this.backgroundDim = backgroundDim;
        this.keyOpacity = keyOpacity;
        this.keyShadow = keyShadow;
    }

    /**
     * Parses the raw preference strings. Missing or malformed values fall back to
     * the documented defaults instead of throwing, so a corrupted preference file
     * can never stop the keyboard from rendering.
     */
    public static TuningValues fromStrings(String height, String radius, String dim,
            String opacityPercent, String shadowPercent) {
        float heightScale = clamp(parse(height, DEFAULT_HEIGHT_SCALE),
                MIN_HEIGHT_SCALE, MAX_HEIGHT_SCALE);
        float radiusDp = clamp(parse(radius, DEFAULT_RADIUS_DP),
                MIN_RADIUS_DP, MAX_RADIUS_DP);
        float backgroundDim = clamp(parse(dim, DEFAULT_BACKGROUND_DIM),
                0f, MAX_BACKGROUND_DIM);
        float keyOpacity = clamp(parse(opacityPercent, 100f) / 100f,
                MIN_KEY_OPACITY, 1f);
        float keyShadow = clamp(parse(shadowPercent, 100f) / 100f,
                0f, MAX_KEY_SHADOW);
        return new TuningValues(heightScale, radiusDp, backgroundDim, keyOpacity, keyShadow);
    }

    /** Convenience overload for callers that already read the preference strings. */
    public static TuningValues fromStrings(String height, String radius, String dim) {
        return fromStrings(height, radius, dim, "100", "100");
    }

    private static float parse(String value, float fallback) {
        if (value == null) return fallback;
        try {
            float parsed = Float.parseFloat(value);
            return Float.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    static float clamp(float value, float min, float max) {
        if (Float.isNaN(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}
