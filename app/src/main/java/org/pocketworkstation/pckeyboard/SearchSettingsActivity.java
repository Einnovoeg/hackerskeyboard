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
            {"Keyboard theme", "theme themes skin skins colour color layout style wood ocean forest sunset nebula rose sandstone midnight oled dark light material lagoon ember cobalt blossom glacier obsidian", "appearance"},
            {"Keyboard layout", "layout layouts qwerty azerty dvorak french english letters arrangement key caps", "appearance"},
            {"Key glyph style", "glyph glyphs symbol symbols icon icons macos windows compact spelled lowercase modifier labels arrows", "appearance"},
            {"Key style", "style flat outlined raised key keys shape border shadow", "appearance"},
            {"Key letter colour", "colour color text label labels letters tint ink", "appearance"},
            {"Key font", "font fonts typeface serif mono cursive roboto", "appearance"},
            {"Key background effect", "effect effects animation animated matrix stars starfield aurora scanlines rain bubbles neon grid fireflies plasma snow vortex ripples beams flow constellation background", "appearance"},
            {"Key press effect", "press touch glow ripple neon edge spark burst rainbow flash scanline sweep starlight shockwave tap feedback", "appearance"},
            {"RGB lighting", "rgb lighting light lights colour color rainbow breathing wave reactive static ambient glow keyboard", "appearance"},
            {"RGB animation speed", "rgb speed animation fast slow lights", "appearance"},
            {"RGB base colour", "rgb hue colour color palette lights", "appearance"},
            {"RGB brightness", "rgb brightness intensity dim lights", "appearance"},
            {"Customize keys", "customize customise keys per-key per key remap remapping vial label labels output text colour color editor", "customize"},
            {"Background image", "background image images wallpaper art photo picture picture art backdrop lagoon ember cobalt blossom glacier obsidian", "appearance"},
            {"Background dim", "dim dimming dimmer brightness dark darken opacity shade background", "appearance"},
            {"Keyboard height", "height heights size tall short scale resize bigger smaller background keys", "appearance"},
            {"Key corner radius", "corner corners radius round rounded curve curvature keys", "appearance"},
            {"Key cap opacity", "opacity transparency translucent alpha cap caps keys see through", "appearance"},
            {"Key shadow depth", "shadow depth elevation lift flat drop keys", "appearance"},
            {"Bold key labels", "bold weight heavy thick font keys labels", "appearance"},
            {"Label letter spacing", "spacing spacing tracking letter letters kerning em gap", "appearance"},
            {"Labeled alternate keys", "hint hints labels punctuation popup alternate keys", "appearance"},
            {"Key label scaling", "label labels size scale text big small keys", "appearance"},
            {"Suggestion label scaling", "suggestion suggestions size scale candidates", "appearance"},
            {"5th row height adjustment", "row fifth 5th height size scale number row", "appearance"},
            {"One-handed mode", "one hand handed left right thumb narrow", "appearance"},
            {"Drawing method", "render rendering software hardware buffer drawing", "appearance"},
            {"Clipboard history", "clipboard copy paste clips pinned history", "appearance"},
            {"Text snippets", "snippet snippets expansion shortcut trigger text", "appearance"},
            {"Macros", "macro macros replay sequence automation", "appearance"},
            {"Per-app themes", "per app apps theme themes rule rules package override", "appearance"},
            {"Gamepad buttons", "gamepad controller xbox switch playstation abxy triggers buttons", "appearance"},
            {"Speak keys", "speech voice talk tts letters spoken speak keys", "feedback"},
            {"Voice volume", "volume loudness speech voice tts", "feedback"},
            {"Speak whole words", "words word speech complete finished whole", "feedback"},
            {"Voice and speed", "voice engine tts speed rate", "feedback"},
            {"Autocorrect strength", "autocorrect correction corrections prediction suggest", "feedback"},
            {"No autocorrect in terminals", "termux terminal terminals linux command shell", "feedback"},
            {"Vibrate on keypress", "vibrate vibration haptic haptics buzz feedback keypress", "feedback"},
            {"Vibrate duration", "vibrate vibration haptic haptics duration length milliseconds ms buzz", "feedback"},
            {"Sound on keypress", "sound sounds click clicky audio keypress key", "feedback"},
            {"Key sound", "sound sounds pack profile click cherry mx mechanical topre eg spoken tone", "feedback"},
            {"Key click volume", "volume click sound loud audio", "feedback"},
            {"Click volume control method", "volume click method control system media", "feedback"},
            {"Swipe gestures", "swipe swipes gesture gestures up down left right actions", "actions"},
            {"Swipe up", "swipe up gesture action extension", "actions"},
            {"Swipe down", "swipe down gesture action close", "actions"},
            {"Swipe left", "swipe left gesture action", "actions"},
            {"Swipe right", "swipe right gesture action", "actions"},
            {"Hardware key actions", "volume hardware key keys actions mapping up down", "actions"},
            {"Volume up", "volume hardware key action up", "actions"},
            {"Volume down", "volume hardware key action down", "actions"},
            {"Keyboard height, portrait", "portrait height percentage legacy main", "main"},
            {"Keyboard mode", "mode layout full compact phone five row portrait landscape", "main"},
            {"Auto-capitalization", "capital capitalization caps sentence shift", "main"},
            {"Double-tap shift mode", "shift caps lock double tap", "main"},
            {"Input languages", "language languages switch layout input", "main"},
            {"Show settings key", "settings gear key show hide", "main"},
            {"Gamepad look", "gamepad controller theme skin background art swap sides preview", "appearance"},
            {"Key height", "height key keys rows tall short scale size", "appearance"},
            {"Keyboard height, landscape", "landscape height percentage size rows", "main"},
            {"Suggestion bar in landscape", "suggestions landscape bar candidates hide show", "main"},
            {"Settings key behaviour", "settings gear key auto never always menu", "main"},
            {"Voice input button", "voice microphone dictation button symbols", "main"},
            {"Long-press duration", "long press duration timing ms alternate keys", "main"},
            {"Slide keys", "slide swipe cursor arrow keys movement", "main"},
            {"Ctrl+A behaviour", "ctrl a select all home line override", "main"},
            {"Chording modifier keys", "chording ctrl alt meta modifiers hold", "main"},
            {"Autocorrect in terminals", "terminal termux shell autocorrect disable", "feedback"},
            {"Keyboard notification", "notification status bar icon show hide", "main"},
            {"Fullscreen in landscape", "fullscreen landscape extract mode", "main"},
            {"Debug and render mode", "debug rendering hardware software drawing touch positions", "appearance"},
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
        } else {
            target = LatinIMESettings.class;
        }
        startActivity(new Intent(this, target));
    }
}
