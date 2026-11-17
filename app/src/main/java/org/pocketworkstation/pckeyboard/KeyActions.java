package org.pocketworkstation.pckeyboard;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of behaviours a key can be bound to from Keyboard customization.
 *
 * <p>Characters are stored separately. This class only describes the named
 * actions, keeping the wire format and the UI labels in one place so a saved
 * binding can never silently mean something different in the editor than it
 * does in the live keyboard.</p>
 */
public final class KeyActions {

    public static final class Spec {
        public final String id;
        public final String title;
        public final String keyLabel;
        public final int code;
        public final int customActionId;
        public final boolean modifier;
        public final boolean repeatable;

        private Spec(String id, String title, String keyLabel, int code) {
            this(id, title, keyLabel, code, 0, false, false);
        }

        private Spec(String id, String title, String keyLabel, int code,
                int customActionId, boolean modifier, boolean repeatable) {
            this.id = id;
            this.title = title;
            this.keyLabel = keyLabel;
            this.code = code;
            this.customActionId = customActionId;
            this.modifier = modifier;
            this.repeatable = repeatable;
        }

        public boolean isCustomAction() {
            return customActionId != 0;
        }
    }

    private static final List<Spec> ACTIONS = new ArrayList<Spec>();

    public static final Spec SHIFT = new Spec("shift", "Shift", "\u21e7",
            Keyboard.KEYCODE_SHIFT, 0, true, false);
    public static final Spec BACKSPACE = new Spec("backspace", "Backspace", "\u232b",
            Keyboard.KEYCODE_DELETE, 0, false, true);
    public static final Spec ENTER = new Spec("enter", "Enter", "\u21b5",
            LatinIME.ASCII_ENTER, 0, false, false);
    public static final Spec SPACE = new Spec("space", "Space", "space",
            LatinIME.ASCII_SPACE, 0, false, true);
    public static final Spec TAB = new Spec("tab", "Tab", "\u21e5", 9);
    public static final Spec ESCAPE = new Spec("escape", "Escape", "Esc",
            LatinKeyboardView.KEYCODE_ESCAPE);
    public static final Spec LAYER = new Spec("layer", "Layer switch", "SYM",
            Keyboard.KEYCODE_MODE_CHANGE, 0, true, false);
    public static final Spec SETTINGS = new Spec("settings", "Settings shortcut", "\u2699",
            LatinKeyboardView.KEYCODE_OPTIONS);
    public static final Spec EMOJI = new Spec("emoji", "Emoji picker", "\ud83d\ude00",
            Keyboard.KEYCODE_CUSTOM_ACTION, LatinIME.CUSTOM_ACTION_EMOJI, false, false);
    public static final Spec CLIPBOARD = new Spec("clipboard", "Clipboard", "\ud83d\udccb",
            Keyboard.KEYCODE_CUSTOM_ACTION, LatinIME.CUSTOM_ACTION_CLIPBOARD, false, false);
    public static final Spec VOICE = new Spec("voice", "Voice input", "\ud83c\udfa4",
            LatinKeyboardView.KEYCODE_VOICE);
    public static final Spec LANGUAGE = new Spec("language", "Next language", "\ud83c\udf10",
            LatinKeyboardView.KEYCODE_NEXT_LANGUAGE);
    public static final Spec CTRL = new Spec("ctrl", "Ctrl", "Ctrl",
            LatinKeyboardView.KEYCODE_CTRL_LEFT, 0, true, false);
    public static final Spec ALT = new Spec("alt", "Alt", "Alt",
            LatinKeyboardView.KEYCODE_ALT_LEFT, 0, true, false);
    public static final Spec META = new Spec("meta", "Meta", "\u2318",
            LatinKeyboardView.KEYCODE_META_LEFT, 0, true, false);
    public static final Spec FN = new Spec("fn", "Fn", "Fn",
            LatinKeyboardView.KEYCODE_FN, 0, true, false);
    public static final Spec UP = new Spec("up", "Arrow up", "\u2191",
            LatinKeyboardView.KEYCODE_DPAD_UP);
    public static final Spec DOWN = new Spec("down", "Arrow down", "\u2193",
            LatinKeyboardView.KEYCODE_DPAD_DOWN);
    public static final Spec LEFT = new Spec("left", "Arrow left", "\u2190",
            LatinKeyboardView.KEYCODE_DPAD_LEFT);
    public static final Spec RIGHT = new Spec("right", "Arrow right", "\u2192",
            LatinKeyboardView.KEYCODE_DPAD_RIGHT);
    public static final Spec HOME = new Spec("home", "Home", "Home",
            LatinKeyboardView.KEYCODE_HOME);
    public static final Spec END = new Spec("end", "End", "End",
            LatinKeyboardView.KEYCODE_END);
    public static final Spec PAGE_UP = new Spec("page_up", "Page up", "PgUp",
            LatinKeyboardView.KEYCODE_PAGE_UP);
    public static final Spec PAGE_DOWN = new Spec("page_down", "Page down", "PgDn",
            LatinKeyboardView.KEYCODE_PAGE_DOWN);
    public static final Spec FORWARD_DELETE = new Spec("delete", "Forward delete", "Del",
            LatinKeyboardView.KEYCODE_FORWARD_DEL);
    public static final Spec CLOSE = new Spec("close", "Close keyboard", "\u2715",
            Keyboard.KEYCODE_CANCEL);
    public static final Spec MACRO = new Spec("macro", "Macros", "M",
            Keyboard.KEYCODE_CUSTOM_ACTION, LatinIME.CUSTOM_ACTION_MACRO, false, false);
    public static final Spec GAMEPAD = new Spec("gamepad", "Gamepad", "GP",
            Keyboard.KEYCODE_CUSTOM_ACTION, LatinIME.CUSTOM_ACTION_GAMEPAD, false, false);

    static {
        ACTIONS.add(SHIFT);
        ACTIONS.add(BACKSPACE);
        ACTIONS.add(ENTER);
        ACTIONS.add(SPACE);
        ACTIONS.add(TAB);
        ACTIONS.add(ESCAPE);
        ACTIONS.add(LAYER);
        ACTIONS.add(SETTINGS);
        ACTIONS.add(EMOJI);
        ACTIONS.add(CLIPBOARD);
        ACTIONS.add(VOICE);
        ACTIONS.add(LANGUAGE);
        ACTIONS.add(CTRL);
        ACTIONS.add(ALT);
        ACTIONS.add(META);
        ACTIONS.add(FN);
        ACTIONS.add(UP);
        ACTIONS.add(DOWN);
        ACTIONS.add(LEFT);
        ACTIONS.add(RIGHT);
        ACTIONS.add(HOME);
        ACTIONS.add(END);
        ACTIONS.add(PAGE_UP);
        ACTIONS.add(PAGE_DOWN);
        ACTIONS.add(FORWARD_DELETE);
        ACTIONS.add(CLOSE);
        ACTIONS.add(MACRO);
        ACTIONS.add(GAMEPAD);
    }

    private KeyActions() {
    }

    public static List<Spec> all() {
        return ACTIONS;
    }

    public static Spec byId(String id) {
        if (id == null) return null;
        for (Spec spec : ACTIONS) {
            if (spec.id.equals(id)) return spec;
        }
        return null;
    }

    /**
     * Returns null when the character is a valid single-character remap, or a
     * user-facing explanation when it is not.
     */
    public static String characterError(String value) {
        if (value == null || value.length() == 0) {
            return "Enter one character.";
        }
        int count = value.codePointCount(0, value.length());
        if (count != 1) {
            return "Enter exactly one character.";
        }
        int codePoint = value.codePointAt(0);
        if (codePoint > 0xFFFF) {
            return "Use the Emoji action for emoji characters.";
        }
        if (Character.isISOControl(codePoint)) {
            return "Use one of the special actions for control characters.";
        }
        if (Character.isWhitespace(codePoint)) {
            return "Use the Space action for spaces.";
        }
        if (codePoint == 0) {
            return "That character is not a valid binding.";
        }
        return null;
    }

    /** A readable label for a key that has no override. */
    public static String describeCode(int code, CharSequence originalLabel) {
        for (Spec spec : ACTIONS) {
            if (!spec.isCustomAction() && spec.code == code) {
                return spec.title;
            }
        }
        if (code == Keyboard.KEYCODE_CUSTOM_ACTION) {
            return "Custom action";
        }
        if (code == LatinKeyboardView.KEYCODE_F1) {
            return "Language / comma key";
        }
        if (code == LatinKeyboardView.KEYCODE_PREV_LANGUAGE) {
            return "Previous language";
        }
        if (code >= 32 && code <= 0xFFFF && !Character.isISOControl(code)) {
            return Character.toString((char) code);
        }
        if (originalLabel != null && originalLabel.length() > 0) {
            return originalLabel.toString();
        }
        return "code " + code;
    }
}
