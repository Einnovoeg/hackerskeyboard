package org.pocketworkstation.pckeyboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class KeyActionsTest {

    @Test
    public void characterBindingAcceptsOnePrintableCharacter() {
        assertNull(KeyActions.characterError("x"));
        assertNull(KeyActions.characterError("@"));
        assertNull(KeyActions.characterError("\u00e9"));
    }

    @Test
    public void characterBindingRejectsMissingMultiAndControlCharacters() {
        assertNotNull(KeyActions.characterError(""));
        assertNotNull(KeyActions.characterError("ab"));
        assertNotNull(KeyActions.characterError(" "));
        assertNotNull(KeyActions.characterError("\n"));
        assertNotNull(KeyActions.characterError("\ud83d\ude00"));
    }

    @Test
    public void namedActionsAreStableAndComplete() {
        assertNotNull(KeyActions.byId("shift"));
        assertNotNull(KeyActions.byId("backspace"));
        assertNotNull(KeyActions.byId("enter"));
        assertNotNull(KeyActions.byId("space"));
        assertNotNull(KeyActions.byId("layer"));
        assertNotNull(KeyActions.byId("settings"));
        assertNotNull(KeyActions.byId("emoji"));
        assertNull(KeyActions.byId("not-an-action"));
        assertFalse(KeyActions.all().isEmpty());
    }

    @Test
    public void actionsMapToTheirRealKeyCodes() {
        assertEquals(Keyboard.KEYCODE_SHIFT, KeyActions.SHIFT.code);
        assertEquals(Keyboard.KEYCODE_DELETE, KeyActions.BACKSPACE.code);
        assertEquals(Keyboard.KEYCODE_MODE_CHANGE, KeyActions.LAYER.code);
        assertEquals(LatinKeyboardView.KEYCODE_OPTIONS, KeyActions.SETTINGS.code);
        assertEquals(Keyboard.KEYCODE_CUSTOM_ACTION, KeyActions.EMOJI.code);
        assertTrue(KeyActions.EMOJI.isCustomAction());
    }

    @Test
    public void defaultDescriptionsUseReadableNames() {
        assertEquals("Shift", KeyActions.describeCode(Keyboard.KEYCODE_SHIFT, null));
        assertEquals("Backspace", KeyActions.describeCode(Keyboard.KEYCODE_DELETE, null));
        assertEquals("a", KeyActions.describeCode('a', "a"));
        assertEquals("Language / comma key",
                KeyActions.describeCode(LatinKeyboardView.KEYCODE_F1, null));
    }
}
