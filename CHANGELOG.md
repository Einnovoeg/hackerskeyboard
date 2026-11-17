# Changelog

All notable changes to HACK3RS K3YB0ARD are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [3.0] — 2026-11-15

The largest release of the project: a complete visual refresh, a rebuilt settings
experience, many more themes and backgrounds, new animation systems, and a gamepad
panel with a hidden tribute.

### Changed
- Renamed and rebranded the app as **HACK3RS K3YB0ARD 3.0** in the launcher, input
  method list, settings titles, setup/help text and about/version row.
- Version bumped to `3.0` (`versionCode 3000000`).
- Settings moved to a Material 3 interface with edge-to-edge insets, grouped
  screens, plain-language section titles and summaries, and a persistent search
  entry point.
- Appearance settings are now a dedicated **Keyboard customization** screen with
  live previews of the theme, background and key styling.
- Feedback controls are grouped into **Typing aids** and **Sound, vibration and key
  preview** sections instead of a flat list.
- Key sizing, one-handed layout and background dimming now apply immediately to the
  open keyboard rather than waiting for the next input session.

### Added — themes and appearance
- **30 themes** in total, including six new palettes: Lagoon, Ember, Cobalt,
  Blossom, Glacier and Obsidian, each with matching keyboard layouts, colours and
  preview backgrounds.
- **24 high-resolution backgrounds**, including six new 2160 × 4800 artworks
  (Lagoon light, Ember glow, Cobalt depths, Blossom haze, Glacier facets and
  Obsidian gold).
- **15 background animations**, including the new Ripples, Light beams, Liquid flow
  and Constellation effects, alongside Matrix rain, Starfield, Aurora, Scanlines,
  Rain, Bubbles, Neon grid, Fireflies, Plasma, Snow and Vortex.
- Background dimming slider with live preview.
- Key corner radius, cap opacity, shadow depth and height scaling.
- Eight key glyph styles, three key cap styles, sixteen label colours, ten fonts,
  bold labels, letter-spacing control, alternate-key hint modes and label/candidate
  scaling.
- Gamepad appearance controls with their own theme, background, glyph set and
  left/right swap.
- Nine selectable key-press effects: Glow, Ripple, Neon edge, Spark burst, Rainbow
  flash, Scanline sweep, Starlight and Shockwave, plus None.
- RGB keyboard lighting with Rainbow, Breathing, Wave, Reactive press and Static
  colour modes, animation speed, base hue and brightness controls.
- **Customize keys** editor for per-key labels, output text and text colours,
  covering letters, numbers, symbols and modifiers, with individual and global
  reset.

### Added — input and workflow
- Gamepad panel that emits real controller key events (`KEYCODE_BUTTON_*`,
  `KEYCODE_DPAD_*`), with Xbox, Switch and PlayStation glyph sets and a live preview.
- Konami-code easter egg on the gamepad: a CRT-to-synthwave Hackerman title card
  with a synthesized 8-bit arpeggio, dismissible by tap and timed automatically.
- Clipboard history, text snippets and macros, each with dedicated editors.
- Per-app theme rules.
- Ten mechanical keyboard sound packs (Cherry MX Blue/Brown/Red/Black in ABS and
  PBT, EG Crystal Purple, EG Oreo, Topre Purple Hybrid), plus the Android system
  tone and a spoken-letters mode.
- Full-text settings search covering every major option and keyword aliases.

### Added — customization and tooling
- Keyboard height and mode for portrait and landscape, one-handed mode, and
  pinch-to-resize.
- Configurable Ctrl+A behaviour, chording modifier keys, slide-key cursor movement,
  long-press duration, popup/key preview toggles, terminal-safe autocorrect and
  autocorrect strength.
- Swipe gestures and hardware volume-key actions.
- Renderer selection and touch-position debugging.

### Fixed
- Background dimming was ignored by the settings preview because the stored decimal
  was parsed as an integer.
- The preview did not reflect key radius, opacity, shadow depth or height scaling.
- Preview height scaling was overwritten by the fit-to-width scale.
- Live visual settings did not refresh the open keyboard until the next input
  session.
- One-handed mode did not refresh while the keyboard was open and could crash when
  no input view existed.
- One-handed mode kept the keyboard at full width and clipped the rightmost keys;
  it now narrows and scales the key grid so left and right modes both fit the
  screen, including after theme changes and rotation.
- Duplicate preference-category identifier in the main settings screen.
- Duplicate sound/vibration controls appeared in both appearance and feedback
  screens; they now live only in the feedback screen.
- Removed an orphan debug preference screen and its unused preference constant.
- Settings search results contained duplicate entries and omitted several newer
  options.

### Security
- No internet permission, analytics, ads or telemetry.
- Clipboard history, snippets and macros stay in the app's private data directory.
- Personal data, machine paths and build caches are excluded from the repository.

### Verification
- `:app:testDebugUnitTest` — 12 JVM tests, 0 failures: tuning parsing/clamping,
  catalogue counts, entry/value parity, press/RGB catalogue counts and drawable
  index safety.
- `:app:assembleDebug` and `:app:assembleRelease` build successfully.
- Release APK is signed and verified with `apksigner`.
- On-device checks on a Motorola moto g 2025 (Android 16): installation, IME
  activation, typing, settings search, theme/background/animation changes,
  key-press effects, RGB modes (including reactive press), per-key label/output/
  colour overrides, one-handed left/right modes and rotation, with no crashes or
  ANRs.

### Known limitations
- Gesture typing is not implemented.
- Bundled dictionaries and next-word prediction are basic.
- Some translations do not cover the new 3.0 settings.
- The gamepad panel sends key events; a few games that read the physical controller
  API will not detect it.
- Hardware verification was performed manually on a Motorola moto g 2025 running
  Android 16; wider device coverage and accessibility improvements are still wanted.

### Upgrade notes
- The package name remains `org.pocketworkstation.pckeyboard`, so existing settings
  and dictionaries are preserved when upgrading with the same signing key.
- This release is signed with a new release key. If you previously installed a
  debug build, uninstall it first or install the new APK after removing the old one.

[3.0]: https://github.com/Einnovoeg/hackerskeyboard/releases/tag/v3.0
