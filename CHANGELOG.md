# Changelog

All notable changes to HACK3RS K3YB0ARD are documented here. The project remains on
version **3.0** until a new version is explicitly started.

## [3.0] — 2026-11-17

The finished 3.0 build: a five-row PC-style Android keyboard with a clean Material
look, a grouped and searchable settings hub, a much larger theme/background/effect
catalogue, deeper key customisation, a downloadable font library, and reliable
silent-mode handling for key sounds.

### Looks, themes and backgrounds
- **36 themes**, including the original Gingerbread/ICS looks, Material
  dark/light/OLED, glass, wood, ocean, forest, sunset, nebula, rose, sandstone,
  midnight, aurora, cyberpunk, paper, carbon, Lagoon, Ember, Cobalt, Blossom,
  Glacier, Obsidian, Orchid, Reef, Crimson, Arctic, Graphite and Solar. Each has a
  matching layout, key states, borders, hints, panel colours and preview.
- **32 high-resolution background images**, including eight 2160 × 4800 artworks:
  Abyss, Solar corona, Orchid bloom, Brushed titanium, Reef light, Volcanic
  fissures, Arctic ice and Mirage dunes. Art is bundled at device resolution.
- **18 background animations** (plus None): Matrix rain, Starfield, Aurora,
  Scanlines, Rain, Bubbles, Neon grid, Fireflies, Plasma, Snow, Vortex, Ripples,
  Light beams, Liquid flow, Constellation, Lava lamp, Tidal waves and Meteor
  shower.
- **Animation speed** (0.2×–3×) and **intensity** (10–100%) apply to every
  background effect through one shared virtual clock and a composited draw layer.
- **Background dim**, per-key cap opacity, corner radius, shadow depth and shadow
  softness (layered translucent shadows).
- **Nine key-press effects** — Glow, Ripple, Neon edge, Spark burst, Rainbow flash,
  Scanline sweep, Starlight, Shockwave, plus None — with an accent colour
  independent of the theme.
- **RGB lighting** — Rainbow, Breathing, Wave, Reactive press and Static colour
  modes, with speed, base hue and brightness.
- **Key appearance** — eight glyph styles, three cap styles, sixteen letter
  colours, bold labels, letter-spacing control, alternate-key hint modes and
  label/candidate scaling.

### Fonts and customisation
- **Ten built-in typefaces** plus a **font library** of 13 downloadable Google
  Fonts: Space Mono, Bebas Neue, Anton, Press Start 2P, VT323, Righteous, Russo
  One, Pacifico, Lobster, Audiowide, Archivo Black, Playfair Display and Oswald.
- Every downloadable font has a bundled specimen image, so the **preview is shown
  before any download**. Font files are fetched only when chosen, validated by
  Android's typeface parser, and stored in private app storage.
- Downloaded fonts appear in the **Key font** list, apply live to key labels and
  the key preview popup, and can be removed. Removing the selected font falls back
  safely to the default.
- **Vial-style per-layer key remapping** for every key on the full, compact,
  standard and phone boards, including symbol layers. Keys can emit a character or
  a named action: Shift, Backspace, Enter, Space, Tab, Escape, layer switch,
  settings, emoji, clipboard, voice, next language, Ctrl, Alt, Meta, Fn, arrows,
  Home, End, Page Up/Down, forward delete, close keyboard, macros and gamepad.
- Per-key label and text-colour styling, current/original binding display,
  per-layer overrides, individual reset and reset-all.
- **Gamepad appearance** with its own theme, background, glyph set and left/right
  swap; the panel emits real controller key events.

### Settings and workflow
- Rebuilt settings as a clean hub with **eight grouped screens**: Theme &
  background, Layout & sizing, Keys & remapping, Typing & prediction, Gesture and
  key actions, Sounds & haptics, Tools & gamepad, and Advanced.
- Every control lives on exactly one screen; full-text search finds options by
  name or keyword and opens the correct screen.
- A live miniature of the real keyboard previews theme, background, font, shape,
  effects and sizing changes as they happen.
- **Mute when device is muted** (on by default) suppresses key clicks in
  silent/vibrate mode, when an audio stream is muted, and when media or system
  volume is zero. It follows ring, volume and mute changes live; spoken key
  feedback is covered by the same guard.
- Ten mechanical keyboard sound packs (Cherry MX Blue/Brown/Red/Black in ABS and
  PBT, EG Crystal Purple, EG Oreo, Topre Purple Hybrid), the Android system tone
  and a spoken-letters mode.
- Clipboard history, text snippets, macros and per-app theme rules, each with a
  dedicated editor.
- Adjustable portrait/landscape height, one-handed left/right mode,
  pinch-to-resize, swipe gestures and hardware volume-key actions.
- Terminal-safe autocorrect, input languages, suggested punctuation,
  recorrection, popup/key preview and TTS controls.

### Fixed
- The Sym key and layer-switch remaps did nothing on full five-row layouts.
- A mode-change tap could toggle in both `onPress` and `onKey`, switching straight
  back to the previous layer.
- Added an **ABC** return key to the full symbols board so it is no longer a dead
  end.
- Background dimming used an integer parse and was ignored by the preview.
- The preview did not reflect key radius, opacity, shadow depth or height scaling,
  and height scaling was overwritten by the fit-to-width scale.
- Live visual settings did not refresh the open keyboard until the next input
  session.
- One-handed mode kept the keyboard at full width and clipped the rightmost keys;
  it now narrows and scales the grid so left and right modes both fit.
- Duplicate sound/vibration controls appeared in two screens; they now live only
  under Sounds & haptics.
- Removed an orphan debug preference screen and duplicate settings-search entries.
- Key sounds could still play when the phone was muted through media/system volume
  rather than the ringer; the new mute guard covers those states.

### Security and privacy
- No analytics, ads or telemetry.
- The only network use is the explicit, user-requested font download. The
  `INTERNET` permission is not needed for typing, themes, backgrounds or previews.
- Clipboard history, snippets, macros, per-app rules and downloaded fonts stay in
  the app's private data directory.
- Personal data, machine paths and build caches are excluded from the repository.

### Verification
- `:app:testDebugUnitTest` — 18 JVM tests, 0 failures: tuning parsing/clamping,
  catalogue counts, entry/value parity, press/RGB catalogue counts, drawable index
  safety and downloadable-font slot/URL checks.
- `:app:assembleDebug` and `:app:assembleRelease` build successfully.
- Release APK is signed and verified with `apksigner`.
- On-device checks on a Motorola moto g 2025 (Android 16): installation and IME
  activation, grouped settings screens, Orchid theme with Abyss background on the
  live keyboard, Lava lamp animation between frames, 2.6× speed/50% intensity
  persistence across restart, blue accent and Glow press effect, and visibly
  different raised-key shadows at 0 and 16 dp.

### Known limitations
- Gesture typing is not implemented.
- Bundled dictionaries and next-word prediction are basic.
- Some translations do not cover the newer settings.
- Font downloads require an internet connection; the previews and the rest of the
  keyboard work offline.
- The gamepad panel sends key events; games that read the physical controller API
  will not detect it.
- Wider device coverage and accessibility improvements are still wanted.

### Upgrade notes
- The package name remains `org.pocketworkstation.pckeyboard`, so settings and
  dictionaries are preserved when upgrading with the same signing key.
- This release is signed with the project's release key. If a debug build was
  installed previously, uninstall it first or install the release APK with the
  debug package removed.

[3.0]: https://github.com/Einnovoeg/hackerskeyboard/releases/tag/v3.0
