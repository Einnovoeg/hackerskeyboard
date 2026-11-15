# Contributing

Thanks for helping improve HACK3RS K3YB0ARD 3.0.

## Before you start

- Search the existing issues before opening a new one.
- For a large change, open an issue first so the approach can be discussed.
- Keep changes focused: one fix or feature per pull request.
- Do not include personal data, machine paths, signing keys, keystores or build
  output in commits.

## Building

Requirements: JDK 17, Android SDK platform 35+ (the project compiles against 37),
and the Android NDK version declared in `app/build.gradle`.

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Install the debug APK with `adb install -r app/build/outputs/apk/debug/app-debug.apk`,
then enable the keyboard in **Settings → System → Languages & input → On-screen
keyboard → Manage keyboards**.

## Testing

- Add JVM unit tests for parsing, catalogue and preference logic where possible.
- Test UI changes on a real device or emulator; the keyboard is custom-drawn and
  does not render correctly in a plain JVM test.
- Check your change in both light and dark system themes, portrait and landscape,
  and with at least one background image and animation enabled.
- Report in the pull request what you tested and on which Android version/device.

## Reporting bugs

Include:

1. Device model and Android version.
2. The keyboard version from **Settings → Debugging → Version**.
3. Steps to reproduce and the expected/actual behaviour.
4. Whether it happens with the default theme, no background image and no animation.
5. A screenshot or screen recording when the issue is visual.

## Translations

Translation strings live in `app/src/main/res/values-*/strings.xml`. Add missing
strings without removing existing translations, and keep placeholders intact.

## Licensing

By contributing you agree that your changes are licensed under the Apache License
2.0. Do not submit code, images, audio or fonts you do not have the right to
redistribute; add attribution to `NOTICE` when you introduce third-party material.
