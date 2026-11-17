# Settings guide

Open the keyboard's settings from the gear key next to the space bar, from the
keyboard notification, or from **Settings → System → Languages & input →
On-screen keyboard → HACK3RS K3YB0ARD 3.0**.

The main screen is a searchable hub. **Search settings** finds any option by name or
keyword (`oled`, `dvorak`, `orchid`, `tidal`, `font`, `mute`, `haptic`, `gamepad`) and
opens the grouped screen that contains it. Appearance changes are previewed on a
live miniature of the real keyboard.

## Settings hub

| Screen | What lives there |
| --- | --- |
| Theme & background | Themes, background art, background animations with speed and intensity, dimming, key-press accents and RGB lighting. |
| Layout & sizing | Portrait/landscape keyboard mode and height, QWERTY/AZERTY/Dvorak, one-handed mode, label/suggestion scaling, fifth-row height, settings and voice keys. |
| Keys & remapping | Per-layer key remapping, key glyph/style/colour/font, the downloadable font library, bold and tracking, hints, radius, opacity, shadow and shadow softness, key behaviour and long-press controls. |
| Typing & prediction | Input languages, suggestions, autocorrect strength, recorrection, terminal-safe typing and speech. |
| Gesture and key actions | Swipe gestures and hardware volume-key actions. |
| Sounds & haptics | Key sounds, sound packs, volume, mute-when-device-muted, vibration and the key preview popup. |
| Tools & gamepad | Clipboard history, snippets, macros, per-app themes and gamepad appearance. |
| Advanced | Input behaviour, fullscreen/notification options, render mode, diagnostics and version. |

Every control is reachable from exactly one of these screens; the old mixed
appearance and feedback lists have been retired.

## Theme & background

### Theme

**Keyboard Theme** offers 36 palettes. These include the original Gingerbread/ICS
looks, Material dark/light/OLED, glass, wood, ocean, forest, sunset, nebula, rose,
sandstone, midnight, aurora, cyberpunk, paper and carbon, the 3.0 palettes Lagoon,
Ember, Cobalt, Blossom, Glacier and Obsidian, and the additional palettes **Orchid,
Reef, Crimson, Arctic, Graphite and Solar**. Each palette includes matching key
states, borders, hints, panel colours and a preview background.

### Background image

**Background image** opens a grid of **32 bundled artworks**, including eight new
2160 × 4800 images: **Abyss, Solar corona, Orchid bloom, Brushed titanium, Reef
light, Volcanic fissures, Arctic ice and Mirage dunes**. The art is bundled at
device resolution rather than stretched from a thumbnail. Choose **None** to use the
theme background.

### Background animation

**Key background effect** offers 19 entries (None plus 18 animations):

| Effect | Character |
| --- | --- |
| Matrix rain | Falling glyph columns. |
| Starfield | Drifting stars. |
| Aurora | Broad shifting colour bands. |
| Scanlines | CRT sweep and line texture. |
| Rain | Diagonal streaks. |
| Bubbles | Rising rings. |
| Neon grid | Perspective synthwave grid. |
| Fireflies | Warm drifting motes. |
| Plasma | Slow colour bands. |
| Snow | Falling flakes with drift. |
| Vortex | Rotating spiral arms. |
| Ripples | Expanding earthen rings. |
| Light beams | Sweeping searchlights. |
| Liquid flow | Silk ribbons of colour. |
| Constellation | Linked, twinkling stars. |
| Lava lamp | Luminous folding blobs. |
| Tidal waves | Layered rolling sine waves. |
| Meteor shower | Diagonal meteors with glowing heads. |

**Animation speed** ranges from 0.2× to 3× and retimes every effect through one
shared virtual clock, so changing speed never restarts or breaks a running effect.
**Animation intensity** from 10% to 100% controls how strongly the animated layer is
composited over the selected background image. **Background dim** can then darken
the result so labels stay readable.

### Lighting and key effects

- **Key press effect** — None, Glow, Ripple, Neon edge, Spark burst, Rainbow flash,
  Scanline sweep, Starlight or Shockwave.
- **Accent colour** — Follow theme, Electric blue, Mint, Violet, Amber, Crimson,
  Rose or Monochrome. It recolours the press effect independently of the theme.
- **RGB lighting** — Off, Rainbow, Breathing, Wave, Reactive press or Static colour,
  with speed, base hue and brightness controls.

The single keyboard preview at the top of the screen reflects the theme, background
image, animation, dimming and key styling as they change.

## Layout & sizing

- **Keyboard mode, portrait / landscape** — Full (five rows), Compact, Phone or the
  symbols layout per orientation.
- **Keyboard layout** — QWERTY, AZERTY or Dvorak for the five-row layout.
- **One-handed mode** — Off, left or right; narrows and scales the key grid.
- **Suggestions in landscape** — keeps the suggestion bar visible in landscape.
- **Keyboard height, portrait / landscape** — percentage of the screen. The keyboard
  can also be resized by dragging its top edge or pinching.
- **Keyboard height** (key scale) — additional whole-keyboard scale.
- **5th row height adjustment**, **Key label scaling**, **Suggestion label scaling**
  — fine-tune the number row, key labels and suggestion bar.
- **Show settings key** — never, always or automatically.
- **Voice input** — microphone key on the main layout, symbols layout or nowhere.

## Keys & remapping

### Remapping

**Customize keys** opens the per-layer remapper. Choose a board (full QWERTY,
AZERTY, Dvorak, compact, standard or phone) and a layer, then bind any key to a
single character or a named action. The list shows the current and original binding.
Optional label and text-colour styling sits on top of the binding. Individual keys
or everything can be reset.

Bindable actions include Shift, Backspace, Enter, Space, Tab, Escape, layer switch,
settings, emoji, clipboard, voice input, next language, Ctrl, Alt, Meta, Fn, arrows,
Home, End, Page Up/Down, forward delete, close keyboard, macros and gamepad. The full
symbols board includes an **ABC** return key.

### Key appearance

- **Key glyph style** — arrows/spelled modifiers, macOS/Windows symbols, compact
  labels, lowercase and other alternate glyph sets.
- **Key style** — flat, outlined or raised.
- **Key letter colour**, **Key font** (built-in and downloaded typefaces), **Bold key
  labels**, **Label letter spacing**.
- **Font library** — preview and download 13 extra Google Fonts typefaces without
  leaving the app.
- **Labelled alternative keys** — whether alternate characters are printed on keys.
- **Key corner radius**, **Key cap opacity**, **Key shadow depth** and **Key shadow
  softness** (0–16 dp). Shadow softness layers translucent shadows outward so raised
  caps can look crisp or softly diffused.

### Font library

The **Font library** row under Key font opens 13 extra typefaces from Google Fonts:
**Space Mono, Bebas Neue, Anton, Press Start 2P, VT323, Righteous, Russo One,
Pacifico, Lobster, Audiowide, Archivo Black, Playfair Display and Oswald**. Every
entry has a bundled specimen image, so you can compare the actual lettering before
downloading anything. Tap **Preview** for a larger sample, then **Download** to
fetch the font at the current slot. The font is validated, stored in the app's
private storage and applied immediately; it then appears in the **Key font** list.
Installed fonts can be removed from the same screen, and removing the selected font
falls back safely to the default. Downloading requires an internet connection; the
rest of the keyboard stays fully offline.

### Key behaviour

Auto-capitalisation, double-tap Shift/caps-lock, modifier locking, Ctrl+A override,
chording Ctrl/Alt/Meta keys, slide-key cursor movement, long-press duration and
popup content.

## Typing & prediction

- **Input languages** — choose the languages and layouts offered by the
  language-switch key.
- **Quick fixes**, **Show suggestions**, **Auto-complete**, **Suggested
  punctuation**, **Autocorrect strength** and **Recorrection**.
- **No autocorrect in terminals** — leaves commands alone in Termux, ConnectBot and
  similar apps.
- **Speak keys**, **Speak whole words**, **Voice volume** and **Voice settings** —
  Android TTS feedback.

## Gesture and key actions

Assign actions to swiping up/down/left/right and to volume-up/volume-down: none,
close keyboard, extension row, settings, suggestions, previous/next language, full
layout, clipboard or keyboard height up/down.

## Sounds & haptics

- **Vibrate on keypress** and **Vibrate duration**.
- **Sound on keypress**, **Key sound** pack (mechanical packs, system tone or spoken
  letters) and **Key click volume**.
- **Mute when device is muted** — on by default. Key clicks stay silent when the
  phone is in silent/vibrate mode, an audio stream is muted, or media/system volume
  is zero; the keyboard follows ring, volume and mute changes live. Turn it off only
  if you want key clicks to ignore the device's mute state.
- **Click volume control method**.
- **Key preview popup** — the enlarged key shown while pressing.

## Tools & gamepad

Clipboard history, text snippets, macros and per-app theme rules are stored locally
in the app's private storage. Gamepad appearance controls choose the glyph set, pad
theme, pad background and left/right side swap, with a live pad preview.

Tap the ⌘ key on the bottom row, then **Gamepad** to open the panel. Every button
sends a real controller key event. Enter ↑ ↑ ↓ ↓ ← → ← → B A for the hidden
Hackerman title card.

## Advanced

- **Fullscreen in landscape**, **Force keyboard on**, **Keyboard notification** and
  **ConnectBot Tab hack**.
- **Drawing method** — hardware/software rendering where supported.
- **Touch positions** — draws touch coordinates for debugging.
- **Input connection details** and **Version** — diagnostics for the last focused
  editor and the installed app version.

## Backup and reset

Settings live in Android's app preferences and are included in Android backup when
enabled. To reset everything, clear the app's storage in Android Settings; this also
removes clipboard history, snippets, macros and per-app rules.
