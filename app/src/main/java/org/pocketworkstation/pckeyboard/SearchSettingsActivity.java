package org.pocketworkstation.pckeyboard;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Type to find a setting; tapping a result opens the screen that holds it. */
public class SearchSettingsActivity extends AppCompatActivity {

    private static final String[][] CATALOGUE = {
            // title, keywords, destination
            {"Keyboard theme", "theme themes skin skins colour color layout style wood ocean forest sunset nebula rose sandstone midnight aurora cyberpunk paper carbon oled material lagoon ember cobalt blossom glacier obsidian orchid reef crimson arctic graphite solar", "appearance"},
            {"Background image", "background image images wallpaper art photo picture backdrop abyss solar corona orchid titanium reef volcano arctic mirage lagoon ember cobalt blossom glacier obsidian high resolution", "appearance"},
            {"Background animation", "effect effects animation animated background matrix stars starfield aurora scanlines rain bubbles neon grid fireflies plasma snow vortex ripples beams flow constellation lava lamp tidal waves meteor shower", "appearance"},
            {"Animation speed", "animation background speed fast slow motion tempo", "appearance"},
            {"Animation intensity", "animation intensity strength opacity fade background effect", "appearance"},
            {"Background dim", "dim dimming dimmer brightness dark darken opacity shade background", "appearance"},
            {"Key press effect", "press touch glow ripple neon edge spark burst rainbow flash scanline sweep starlight shockwave tap feedback", "appearance"},
            {"Accent colour", "accent color colour hue tint press effect lighting blue mint violet amber crimson rose monochrome", "appearance"},
            {"RGB lighting", "rgb lighting light lights colour color rainbow breathing wave reactive static ambient glow keyboard", "appearance"},
            {"RGB animation speed", "rgb speed animation fast slow lights", "appearance"},
            {"RGB base colour", "rgb hue colour color palette lights", "appearance"},
            {"RGB brightness", "rgb brightness intensity dim lights", "appearance"},
            {"Keyboard layout", "layout layouts qwerty azerty dvorak french english letters arrangement full compact phone five row", "layout"},
            {"Keyboard height, portrait", "portrait height percentage size rows tall short resize", "layout"},
            {"Keyboard height, landscape", "landscape height percentage size rows tall short resize", "layout"},
            {"Key height", "height key keys rows tall short scale size", "layout"},
            {"5th row height adjustment", "row fifth 5th height size scale number row top row", "layout"},
            {"Key label scaling", "label labels size scale text big small keys", "layout"},
            {"Suggestion label scaling", "suggestion suggestions size scale candidates", "layout"},
            {"One-handed mode", "one hand handed left right thumb narrow", "layout"},
            {"Suggestion bar in landscape", "suggestions landscape bar candidates hide show", "layout"},
            {"Settings key behaviour", "settings gear key auto never always menu show hide", "layout"},
            {"Voice input button", "voice microphone dictation button symbols", "layout"},
            {"Customize keys", "customize customise keys per-key per key remap remapping label labels output text colour color editor", "customize"},
            {"Key glyph style", "glyph glyphs symbol symbols icon icons macos windows compact spelled lowercase modifier labels arrows", "keys"},
            {"Key style", "style flat outlined raised key keys shape border shadow", "keys"},
            {"Key letter colour", "colour color text label labels letters tint ink", "keys"},
            {"Key font", "font fonts typeface serif mono cursive roboto", "keys"},
            {"Font library", "font fonts typeface download library preview google extra", "keys"},
            {"Bold key labels", "bold weight heavy thick font keys labels", "keys"},
            {"Label letter spacing", "spacing tracking letter letters kerning em gap", "keys"},
            {"Labeled alternate keys", "hint hints labels punctuation popup alternate keys", "keys"},
            {"Key corner radius", "corner corners radius round rounded curve curvature keys", "keys"},
            {"Key cap opacity", "opacity transparency translucent alpha cap caps keys see through", "keys"},
            {"Key shadow depth", "shadow depth elevation lift flat drop keys", "keys"},
            {"Key shadow softness", "shadow softness blur soft diffusion raised keys", "keys"},
            {"Auto-capitalization", "capital capitalization caps sentence shift", "keys"},
            {"Double-tap shift mode", "shift caps lock double tap", "keys"},
            {"Ctrl+A behaviour", "ctrl a select all home line override", "keys"},
            {"Chording modifier keys", "chording ctrl alt meta modifiers hold", "keys"},
            {"Slide keys", "slide swipe cursor arrow keys movement", "keys"},
            {"Long-press duration", "long press duration timing ms alternate keys", "keys"},
            {"Long-press alternate keys", "popup alternate characters accents symbols long press", "keys"},
            {"Input languages", "language languages switch layout input dictionary", "typing"},
            {"Word suggestions", "suggest suggestions prediction quick fixes complete", "typing"},
            {"Autocorrect strength", "autocorrect correction corrections prediction suggest", "typing"},
            {"Suggested punctuation", "punctuation comma period apostrophe suggestion", "typing"},
            {"Recorrection", "recorrection correction previous word", "typing"},
            {"No autocorrect in terminals", "termux terminal terminals linux command shell", "typing"},
            {"Speak keys", "speech voice talk tts letters spoken speak keys", "typing"},
            {"Voice volume", "volume loudness speech voice tts", "typing"},
            {"Speak whole words", "words word speech complete finished whole", "typing"},
            {"Voice and speed", "voice engine tts speed rate", "typing"},
            {"Swipe gestures", "swipe swipes gesture gestures up down left right actions", "actions"},
            {"Swipe up", "swipe up gesture action extension", "actions"},
            {"Swipe down", "swipe down gesture action close", "actions"},
            {"Swipe left", "swipe left gesture action", "actions"},
            {"Swipe right", "swipe right gesture action", "actions"},
            {"Hardware key actions", "volume hardware key keys actions mapping up down", "actions"},
            {"Vibrate on keypress", "vibrate vibration haptic haptics buzz feedback keypress", "feedback"},
            {"Vibrate duration", "vibrate vibration haptic haptics duration length milliseconds ms buzz", "feedback"},
            {"Sound on keypress", "sound sounds click clicky audio keypress key", "feedback"},
            {"Mute when device is muted", "mute muted silent silence vibrate volume zero key sounds click audio", "feedback"},
            {"Key sound", "sound sounds pack profile click cherry mx mechanical topre eg spoken tone", "feedback"},
            {"Key click volume", "volume click sound loud audio", "feedback"},
            {"Click volume control method", "volume click method control system media", "feedback"},
            {"Key preview popup", "popup preview key press magnify", "feedback"},
            {"Clipboard history", "clipboard copy paste clips pinned history", "tools"},
            {"Text snippets", "snippet snippets expansion shortcut trigger text", "tools"},
            {"Macros", "macro macros replay sequence automation", "tools"},
            {"Per-app themes", "per app apps theme themes rule rules package override", "tools"},
            {"Gamepad look", "gamepad controller theme skin background art swap sides preview buttons", "tools"},
            {"Drawing method", "render rendering software hardware buffer drawing", "advanced"},
            {"Fullscreen in landscape", "fullscreen landscape extract mode", "advanced"},
            {"Force keyboard on", "force keyboard show input method", "advanced"},
            {"Keyboard notification", "notification status bar icon show hide", "advanced"},
            {"Application-specific settings", "connectbot terminal tab hack application", "advanced"},
            {"Touch position debugging", "debug touch positions pointer", "advanced"},
            {"Version and diagnostics", "version input connection details debug", "advanced"},
    };

    private EditText mQuery;
    private LinearLayout mResults;
    private TextView mEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_search);
        EdgeInsets.apply(this, true);

        mQuery = findViewById(R.id.search_query);
        mResults = findViewById(R.id.search_results);
        mEmpty = findViewById(R.id.search_empty);

        mQuery.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                render(s.toString());
            }
        });
        render("");
        mQuery.requestFocus();
    }

    private void render(String query) {
        mResults.removeAllViews();
        String q = query.trim().toLowerCase(Locale.US);
        List<String[]> matches = new ArrayList<>();
        for (String[] entry : CATALOGUE) {
            if (q.isEmpty() || entry[0].toLowerCase(Locale.US).contains(q)
                    || entry[1].contains(q)) {
                matches.add(entry);
            }
        }
        mEmpty.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);

        final float density = getResources().getDisplayMetrics().density;
        for (final String[] entry : matches) {
            TextView row = new TextView(this);
            row.setText(entry[0]);
            row.setTextSize(16);
            row.setPadding((int) (8 * density), (int) (16 * density),
                    (int) (8 * density), (int) (16 * density));
            // Follow the screen theme: the catalogue rows used to be hard-coded light and
            // disappeared against the light background.
            TypedValue primary = new TypedValue();
            if (getTheme().resolveAttribute(android.R.attr.textColorPrimary, primary, true)) {
                row.setTextColor(primary.resourceId != 0
                        ? getResources().getColor(primary.resourceId, getTheme())
                        : primary.data);
            }
            row.setOnClickListener(v -> open(entry[2]));
            mResults.addView(row);

            View divider = new View(this);
            divider.setBackgroundColor(0x22808080);
            mResults.addView(divider, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, (int) density)));
        }
    }

    private void open(String destination) {
        Class<?> target;
        if ("customize".equals(destination)) {
            target = KeyCustomizeActivity.class;
        } else if ("feedback".equals(destination)) {
            target = PrefScreenFeedback.class;
        } else if ("actions".equals(destination)) {
            target = PrefScreenActions.class;
        } else if ("appearance".equals(destination)) {
            target = PrefScreenView.class;
        } else if ("layout".equals(destination)) {
            target = PrefScreenLayout.class;
        } else if ("keys".equals(destination)) {
            target = PrefScreenKeys.class;
        } else if ("typing".equals(destination)) {
            target = PrefScreenTyping.class;
        } else if ("tools".equals(destination)) {
            target = PrefScreenTools.class;
        } else if ("advanced".equals(destination)) {
            target = PrefScreenAdvanced.class;
        } else {
            target = LatinIMESettings.class;
        }
        startActivity(new Intent(this, target));
    }
}
