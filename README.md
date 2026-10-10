# World Time

An Android app for comparing local times across cities.

## Features

- Shared hourly timeline with date and time selection.
- Search, add, reorder, and remove cities; choose a home time zone.
- Share city times and a link to the selected view.
- Resizable home screen widget with live clocks and an hourly comparison.
- Material 3 dynamic colors with light and dark themes.

## Requirements

- Android Studio or the Android Gradle plugin toolchain.
- Android SDK 36.1.
- Android 14 or newer device or emulator.

## Build

```sh
./gradlew :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Test

```sh
./gradlew :app:testDebugUnitTest
```
