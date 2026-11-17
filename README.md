# HACK3RS K3YB0ARD 3.0

A five-row, PC-style soft keyboard for Android, rebuilt for modern devices with a
clean Material look, deep visual customisation, and the desktop keys Android
keyboards usually leave out: **Esc, Tab, Ctrl, Alt, arrows, a real function row and
a five-row layout** that fits terminals, SSH clients and remote desktops.

This is a fork of the original [Hacker's Keyboard](https://github.com/klausw/hackerskeyboard)
by Klaus Weidner, which in turn descends from the AOSP Gingerbread keyboard. The
upstream project name is kept in the credits; this fork ships as HACK3RS K3YB0ARD 3.0. See
[Credits and licences](#credits-and-licences).

[![Add to Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_beta.svg)](https://obtainium.imranr.dev/redirect?r=obtainium://add/https%3A%2F%2Fgithub.com%2FEinnovoeg%2Fhackerskeyboard)

[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-support-FFDD00?logo=buymeacoffee&logoColor=black)](https://buymeacoffee.com/einnovoeg)

---

## What you get

**A real keyboard layout**
- Five-row QWERTY, AZERTY and Dvorak layouts, switchable in Settings.
- Dedicated Esc, Tab, Ctrl, Alt/Meta, function row and arrow cluster.
- Multi-touch modifiers (hold Ctrl with one thumb and type with the other).
- Long-press popups for accents, symbols and alternate characters.
- Compact and phone layouts for smaller screens and landscape use.

**Looks**
- **36 themes**, including modern dark/light/OLED, glass, wood, ocean, forest,
  nebula, carbon, the 3.0 palettes Lagoon, Ember, Cobalt, Blossom, Glacier and
  Obsidian, plus six new 3.0.2 palettes: Orchid, Reef, Crimson, Arctic, Graphite and
  Solar.
- **32 high-resolution background images**, including eight new 2160 × 4800
  artworks in 3.0.2: Abyss, Solar corona, Orchid bloom, Brushed titanium, Reef
  light, Volcanic fissures, Arctic ice and Mirage dunes.
- **18 background animations**: matrix rain, starfield, aurora, scanlines, rain,
  bubbles, neon grid, fireflies, plasma, snow, vortex, ripples, light beams, liquid
  flow, constellation, lava lamp, tidal waves and meteor shower.
- **Animation speed (0.2×–3×) and intensity (10–100%)** apply to every background
  effect, not just one style.
- Per-element styling: key glyph style, key cap style, key letter colour, font,
  corner radius, opacity, shadow depth, shadow softness, accent colour, hint colour
  and label scaling.
- **Nine key-press effects** — Glow, Ripple, Neon edge, Spark burst, Rainbow flash,
  Scanline sweep, Starlight and Shockwave, plus None — with an optional accent
  colour independent of the theme.
- **RGB keyboard lighting** — Rainbow, Breathing, Wave, Reactive press and Static
  colour modes, with speed, base hue and brightness controls.
- **Vial-style per-layer key remapping** — give any key on any board a different
  character or a special action (Shift, Backspace, Enter, Space, layer switch,
  emoji, settings, clipboard, voice, arrows, macros and more), with the current
  binding shown, per-layer overrides, optional label/colour styling, and individual
  or global reset.

**Useful extras**
- Gamepad panel that emits real controller key events, with Xbox/Switch/PlayStation
  glyph sets, its own theme and background, and side swapping.
- Clipboard history, text snippets, macros and per-app theme rules.
- Adjustable keyboard height (with pinch-to-resize), one-handed mode, key sounds and
  haptics.
- Full-text search across every setting.
- Settings preview that shows the real keyboard as you change things.
- A Konami code easter egg on the gamepad panel.

## Screenshots

| Keyboard | 32 background images | Hackerman easter egg |
| --- | --- | --- |
| ![Keyboard](docs/screenshots/keyboard.png) | ![Backgrounds](docs/screenshots/backgrounds.png) | ![Hackerman](docs/screenshots/hackerman.png) |

## Install

### From a release (recommended)

1. Download `hack3rs-keyboard-3.0.2.apk` from the [Releases](../../releases) page.
   The earlier `hack3rs-keyboard-3.0.apk` remains available on the v3.0 release.
2. Install it (allow "install unknown apps" for your browser or file manager if
   asked).
3. Open **HACK3RS K3YB0ARD 3.0** and follow the two setup buttons: enable the
   keyboard in system settings, then select it as the current keyboard.

### With Obtainium (automatic updates)

Tap the Obtainium badge above, or add this repository URL manually in
[Obtainium](https://github.com/ImranR98/Obtainium).

### Build from source

Requirements: JDK 17, Android SDK with platform 35+, and an internet connection for
the first Gradle run.

```bash
git clone <this repository>
cd hackerskeyboard
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The debug APK is unsigned for release purposes; use your own keystore to build a
release variant.

### Enable the keyboard

1. **Settings → System → Languages & input → On-screen keyboard → Manage keyboards**
   and enable **HACK3RS K3YB0ARD 3.0**.
2. Choose it from the keyboard switcher, or set it as the default input method.
3. To use it only sometimes, keep your normal keyboard as default and switch with
   the keyboard button in the navigation bar.

## Using it

- **Modifiers**: tap Ctrl/Alt/Fn once to latch them for the next key, or hold them
  with another finger.
- **Arrows and Tab** behave like hardware keys; they work in terminals, editors and
  text fields.
- **Gesture typing is not included** — this is a precision keyboard, not a
  swipe-typing one.
- **Height**: drag the top edge of the keyboard, or use Settings → Keyboard height.
- **Gamepad**: tap the ⌘ key on the bottom row, then **Gamepad**. Every button sends
  a real `KEYCODE_BUTTON_*` / `KEYCODE_DPAD_*` event, so emulators and games see a
  controller. Enter ↑ ↑ ↓ ↓ ← → ← → B A for a surprise.

Full documentation of every setting lives in the [settings guide](docs/SETTINGS.md).

## Settings overview

Settings are now a grouped hub, searchable and previewed live:

- **Theme & background** — 36 themes, 32 background images, 18 animations with
  speed and intensity, dimming, key-press effects, accent colour and RGB lighting.
- **Layout & sizing** — portrait/landscape modes and heights, QWERTY/AZERTY/Dvorak,
  one-handed mode, label/suggestion scaling, fifth-row height, settings and voice
  keys.
- **Keys & remapping** — per-layer key remapping, glyph/style/colour/font, bold,
  tracking, hints, radius, opacity, shadow depth, shadow softness, key behaviour
  and long-press controls.
- **Typing & prediction** — input languages, suggestions, autocorrect strength,
  recorrection, terminal-safe mode and speech.
- **Gesture and key actions** — swipe gestures and hardware volume keys.
- **Sounds & haptics** — audio, vibration, sound packs and key preview popup.
- **Tools & gamepad** — clipboard history, text snippets, macros, per-app themes
  and gamepad appearance.
- **Advanced** — render mode, input behaviour, touch diagnostics and version.

Each entry has a one-line description; the search box at the top finds any of them
by name or by keyword (`oled`, `dvorak`, `orchid`, `tidal`, `accent`, …).

## Known limitations and work needed

- **No gesture typing.** The original swipe-typing code was never ported; adding it
  would need a new decoder and dictionary pipeline.
- **Suggestions and dictionaries are basic.** The bundled dictionaries are old and
  some language packs are incomplete; better word lists and next-word prediction are
  wanted.
- **Some translations are incomplete** for the many new settings added in 3.0.
- **The gamepad panel is visual-only for hardware-less devices** — it sends key
  events through the input connection, which most emulators accept, but a few games
  read the physical controller API and will not see it.
- **Tablets and foldables** have only basic layout tuning; per-window-size layouts
  would help.
- **Accessibility**: the custom-drawn keys expose limited spoken feedback.
- **Testing** is manual on a Motorola moto g 2025 running Android 16; wider device
  coverage is needed.

If you hit a bug, please [open an issue](../../issues) with your device, Android
version and steps to reproduce. Contributions are very welcome — especially
translations, dictionary packs, gesture typing, and device-specific fixes. See
[CONTRIBUTING](docs/CONTRIBUTING.md).

## Security and privacy

- The keyboard requests **no internet permission**. It cannot send anything anywhere.
- No analytics, ads, telemetry or third-party trackers.
- Clipboard history, snippets and macros are stored locally in the app's private
  data directory and never leave the device.
- The only network-capable component is the user's own browser if they open a link
  from this README.

## Credits and licences

This project is a derivative work and would not exist without the original authors:

- **Hacker's Keyboard** (the original project this fork is based on) — Copyright Klaus Weidner and contributors,
  <https://github.com/klausw/hackerskeyboard>, Apache License 2.0.
- **AOSP Gingerbread keyboard** — Copyright The Android Open Source Project,
  Apache License 2.0.
- **LatinIME dictionary and native code** — derived from AOSP, Apache License 2.0.
- **AndroidX, Material Components** — Apache License 2.0.
- The Konami code is a cultural reference; the Hackerman title card here is an
  original, non-infringing tribute drawn in code (no film footage or audio is used).

This fork's changes are released under the **Apache License 2.0** as well; see
[LICENSE](LICENSE) and [NOTICE](NOTICE). You may reuse, modify and redistribute the
code, provided you keep the copyright notices and licence texts.

## Contributing

Bug reports, feature requests and pull requests are welcome. Please read
[CONTRIBUTING](docs/CONTRIBUTING.md) first, keep changes focused, and test on a real
device where you can.

See [CHANGELOG.md](CHANGELOG.md) for the full 3.0 release notes, the 3.0.1
key-remapping update and the 3.0.2 appearance/settings release.
