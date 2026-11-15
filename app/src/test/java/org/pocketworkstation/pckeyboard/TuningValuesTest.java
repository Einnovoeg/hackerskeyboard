package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Verifies the preference-to-renderer values stay in their documented ranges. */
public class TuningValuesTest {

    private static final float EPS = 0.0001f;

    @Test
    public void missingValuesUseDefaults() {
        TuningValues values = TuningValues.fromStrings(null, null, null, null, null);
        assertEquals(TuningValues.DEFAULT_HEIGHT_SCALE, values.heightScale, EPS);
        assertEquals(TuningValues.DEFAULT_RADIUS_DP, values.radiusDp, EPS);
        assertEquals(TuningValues.DEFAULT_BACKGROUND_DIM, values.backgroundDim, EPS);
        assertEquals(TuningValues.DEFAULT_KEY_OPACITY, values.keyOpacity, EPS);
        assertEquals(TuningValues.DEFAULT_KEY_SHADOW, values.keyShadow, EPS);
    }

    @Test
    public void malformedValuesUseDefaults() {
        TuningValues values = TuningValues.fromStrings("", "abc", "NaN", "Infinity", "-");
        assertEquals(TuningValues.DEFAULT_HEIGHT_SCALE, values.heightScale, EPS);
        assertEquals(TuningValues.DEFAULT_RADIUS_DP, values.radiusDp, EPS);
        assertEquals(TuningValues.DEFAULT_BACKGROUND_DIM, values.backgroundDim, EPS);
        assertEquals(TuningValues.DEFAULT_KEY_OPACITY, values.keyOpacity, EPS);
        assertEquals(TuningValues.DEFAULT_KEY_SHADOW, values.keyShadow, EPS);
    }

    @Test
    public void validValuesAreConvertedFromPercent() {
        TuningValues values = TuningValues.fromStrings("1.25", "18", "0.4", "80", "150");
        assertEquals(1.25f, values.heightScale, EPS);
        assertEquals(18f, values.radiusDp, EPS);
        assertEquals(0.4f, values.backgroundDim, EPS);
        assertEquals(0.8f, values.keyOpacity, EPS);
        assertEquals(1.5f, values.keyShadow, EPS);
    }

    @Test
    public void outOfRangeValuesAreClamped() {
        TuningValues low = TuningValues.fromStrings("0", "-5", "-1", "10", "-50");
        assertEquals(TuningValues.MIN_HEIGHT_SCALE, low.heightScale, EPS);
        assertEquals(0f, low.radiusDp, EPS);
        assertEquals(0f, low.backgroundDim, EPS);
        assertEquals(TuningValues.MIN_KEY_OPACITY, low.keyOpacity, EPS);
        assertEquals(0f, low.keyShadow, EPS);

        TuningValues high = TuningValues.fromStrings("9", "999", "5", "1000", "900");
        assertEquals(TuningValues.MAX_HEIGHT_SCALE, high.heightScale, EPS);
        assertEquals(TuningValues.MAX_RADIUS_DP, high.radiusDp, EPS);
        assertEquals(TuningValues.MAX_BACKGROUND_DIM, high.backgroundDim, EPS);
        assertEquals(1f, high.keyOpacity, EPS);
        assertEquals(TuningValues.MAX_KEY_SHADOW, high.keyShadow, EPS);
    }

    @Test
    public void clampRejectsNaN() {
        assertEquals(3f, TuningValues.clamp(Float.NaN, 3f, 7f), EPS);
        assertEquals(3f, TuningValues.clamp(-10f, 3f, 7f), EPS);
        assertEquals(7f, TuningValues.clamp(10f, 3f, 7f), EPS);
        assertEquals(5f, TuningValues.clamp(5f, 3f, 7f), EPS);
    }
}
