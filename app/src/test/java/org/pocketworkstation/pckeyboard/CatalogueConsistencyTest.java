package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Guards the link between the values in the settings menus and the tables the
 * renderer indexes. These run as plain JVM tests (no device, no emulator): the
 * resources are read from the source tree because unit tests do not receive a
 * merged resource table.
 */
public class CatalogueConsistencyTest {

    private static Document sStrings;

    @BeforeClass
    public static void loadStrings() throws Exception {
        File file = new File("src/main/res/values/strings.xml");
        if (!file.isFile()) {
            // Gradle may run the test from the repository root instead of app/.
            file = new File("app/src/main/res/values/strings.xml");
        }
        assertTrue("strings.xml not found from " + new File(".").getAbsolutePath(),
                file.isFile());
        sStrings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
    }

    private static int countItems(String arrayName) {
        NodeList arrays = sStrings.getElementsByTagName("string-array");
        for (int i = 0; i < arrays.getLength(); i++) {
            Element array = (Element) arrays.item(i);
            if (arrayName.equals(array.getAttribute("name"))) {
                return array.getElementsByTagName("item").getLength();
            }
        }
        throw new AssertionError("Missing string-array " + arrayName);
    }

    @Test
    public void effectCatalogueMatchesRenderer() {
        assertEquals(16, countItems("key_effect_values"));
        assertEquals(16, countItems("key_effect_entries"));
        // 0 = none plus fifteen animated effects, see LatinKeyboardBaseView.
        assertEquals(15, LatinKeyboardBaseView.EFFECT_CONSTELLATION);
    }

    @Test
    public void pressAndRgbCataloguesMatchRenderer() {
        assertEquals(9, countItems("key_press_effect_values"));
        assertEquals(9, countItems("key_press_effect_entries"));
        assertEquals(6, countItems("key_rgb_mode_values"));
        assertEquals(6, countItems("key_rgb_mode_entries"));
        assertEquals(5, GlobalKeyboardSettings.RGB_STATIC);
    }

    @Test
    public void backgroundCatalogueMatchesDrawables() {
        assertEquals(BackgroundImages.DRAWABLES.length, countItems("background_image_values"));
        assertEquals(BackgroundImages.DRAWABLES.length, countItems("background_image_entries"));
        assertEquals(25, BackgroundImages.DRAWABLES.length);
    }

    @Test
    public void themeCataloguesMatchRenderer() {
        assertEquals(KeyboardSwitcher.THEMES.length, countItems("keyboard_layout_modes_values"));
        assertEquals(KeyboardSwitcher.THEMES.length, countItems("keyboard_layout_modes"));
        assertEquals(30, KeyboardSwitcher.THEMES.length);
        // Gamepad theme values include the "Follow keyboard" (-1) entry.
        assertEquals(KeyboardSwitcher.THEMES.length + 1, countItems("gamepad_theme_values"));
    }

    @Test
    public void keyStyleCataloguesMatchRenderer() {
        assertEquals(3, countItems("key_style_values"));
        assertEquals(8, countItems("key_glyph_values"));
        assertEquals(10, countItems("key_font_values"));
        assertEquals(16, countItems("key_text_color_values"));
    }

    @Test
    public void backgroundIndexIsSafeForEveryValue() {
        assertEquals(0, BackgroundImages.drawableFor(-1));
        assertEquals(0, BackgroundImages.drawableFor(0));
        assertEquals(0, BackgroundImages.drawableFor(BackgroundImages.DRAWABLES.length));
        assertEquals(BackgroundImages.DRAWABLES[BackgroundImages.DRAWABLES.length - 1],
                BackgroundImages.drawableFor(BackgroundImages.DRAWABLES.length - 1));
    }

    /**
     * Any list preference whose arrays follow the *_entries / *_values convention
     * must keep the two arrays the same length; a mismatch would let the user pick
     * an entry that has no backing value (or vice versa).
     */
    @Test
    public void entryAndValueArraysStayInStep() {
        NodeList arrays = sStrings.getElementsByTagName("string-array");
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int i = 0; i < arrays.getLength(); i++) {
            Element array = (Element) arrays.item(i);
            counts.put(array.getAttribute("name"),
                    array.getElementsByTagName("item").getLength());
        }
        for (java.util.Map.Entry<String, Integer> entry : counts.entrySet()) {
            String name = entry.getKey();
            if (!name.endsWith("_entries")) continue;
            String values = name.substring(0, name.length() - "_entries".length()) + "_values";
            if (!counts.containsKey(values)) continue;
            assertEquals(name + " and " + values + " differ in length",
                    (int) entry.getValue(), (int) counts.get(values));
        }
    }
}
