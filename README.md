<div align="center">

<img src="logo/logo-splash-screen.png" alt="Nekuva logo" width="128" height="128" />

# Nekuva

**A cross-platform manga reader built with Kotlin Multiplatform & Compose Multiplatform.**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
![Platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS%20%7C%20Desktop%20(Windows%20%7C%20macOS%20%7C%20Linux)-success)
![Status](https://img.shields.io/badge/status-active%20development-orange)

</div>

---

## About

**Nekuva** is a manga reader that lets you browse many online sources, read chapters online,
and keep a local library — on **Android**, **iOS (15.0+)**, and **Desktop (Windows, macOS & Linux)**
from a single shared codebase.

Manga sources (the parsers/scrapers) live in a separate repository,
**[nekuva-exts](https://github.com/Neko-Koubou/nekuva-exts)**, which Nekuva consumes as a
published dependency. This keeps the UI app and the source library cleanly separated.

> **Project status:** Nekuva is under active development. It started as a fork of
> [DokiTeam/Doki](https://github.com/DokiTeam/Doki) (an Android app built with XML Views) and has been
> **rewritten into Kotlin Multiplatform + Compose Multiplatform**. All targets (Android, Desktop,
> and iOS) build and run today. Expect rapid improvements and updates.

## Features

- 📚 Browse and search a large catalog of online manga sources (via `nekuva-exts`)
- 📖 Online reader with a continuous vertical/webtoon view
- 💾 Local library — read your own `.cbz` archives
- ⭐ Favourites with categories, reading history, and bookmarks
- 🌙 Dark-first Material 3 theme (with an AMOLED option)
- 🌐 Multi-language UI (localized via Compose Multiplatform Resources)
- 🖥️ Adaptive layout — bottom navigation on phones, navigation rail on desktop/tablet

*Some features are still being migrated; see the in-repo `MIGRATION.md` for current progress.*

## Tech stack

| Concern | Choice |
|---|---|
| UI | Compose Multiplatform (Material 3) |
| DI | Koin |
| Database | Room KMP (bundled SQLite) |
| Networking | OkHttp (JVM) / Ktor + Apple Network Framework (iOS) |
| Images | Coil 3 |
| Preferences | multiplatform-settings (NSUserDefaults on iOS) |
| Navigation | Compose Navigation (type-safe routes) |
| Dates / i18n | kotlinx-datetime + Compose Resources |
| Sources | [nekuva-exts](https://github.com/Neko-Koubou/nekuva-exts) (JitPack) |

### Source-set layout

```
composeApp/src/
├── commonMain/      # platform-agnostic Compose UI + logic
├── jvmSharedMain/   # shared Android + Desktop JVM code (OkHttp + source models)
├── androidMain/     # Android entry point & platform actuals
├── desktopMain/     # Desktop entry point & platform actuals
└── iosMain/         # iOS actuals (Apple Network DoH, DocumentPicker, Keychain, Room SQLite)
```

## Building & running

Requires **JDK 17**, Android SDK (for Android), and **Xcode 15+** on macOS (for iOS).

**Desktop (Windows, macOS & Linux):**
```bash
./gradlew :composeApp:run
```

Native installers (`.msi` / `.dmg` / `.deb`):
```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

**Android** (device with USB debugging connected):
```bash
./gradlew :composeApp:installDebug
```

**Android APK only:**
```bash
./gradlew :composeApp:assembleDebug
# output: composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

**iOS (macOS host with Xcode):**

- Minimum deployment target: **iOS 15.0+** (compatible with iPhone 6s through iPhone 16).

Run on iOS Simulator:
```bash
# Open Xcode project and select target simulator / device
open iosApp/iosApp.xcodeproj
```

Compile iOS framework:
```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64
./gradlew :composeApp:compileKotlinIosArm64
```

Package unsigned iOS `.ipa` for sideloading:
```bash
# Clean build and package unsigned IPA
xcodebuild clean build -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -configuration Release -destination 'generic/platform=iOS' \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO CODE_SIGN_IDENTITY="" \
  BUILD_DIR=build/derivedData
mkdir -p build/ipa/Payload
cp -R build/derivedData/Release-iphoneos/Nekuva.app build/ipa/Payload/
cd build/ipa && zip -qr Nekuva-unsigned.ipa Payload
```

## iOS Installation (Sideloading)

Nekuva provides pre-built **unsigned `.ipa`** binaries on [GitHub Releases](https://github.com/Neko-Koubou/nekuva/releases) for iOS 15.0+. You do **not** need a paid Apple Developer Account to install it on your device.

You can install it using any sideloading tool:

1. **[SideStore](https://sidestore.io/) / [AltStore](https://altstore.io/)** *(Recommended)*:
   - Sideload directly using your free Apple ID.
   - Automatically refreshes the 7-day provisioning certificate over local Wi-Fi.
2. **[TrollStore](https://github.com/opa334/TrollStore)** *(Permanently signed, no refresh needed)*:
   - Supported on iOS 14.0 – 16.6.1 and 17.0 (CoreTrust bug).
   - Install `.ipa` directly in TrollStore with no expiration.
3. **[Sideloadly](https://sideloadly.io/) / [Scarlet](https://usescarlet.com/)**:
   - Connect iPhone via USB to Mac/PC and sideload with your Apple ID.

> **Privacy & Permissions:**
> - When first backing up or downloading manga to custom directories, iOS will prompt for folder access via the native Files picker (`UIDocumentPickerViewController`).
> - Biometric authentication (FaceID / TouchID) uses Apple's native `LocalAuthentication` framework and stays entirely on-device.

## Contributing

Bug reports and pull requests are welcome.

- **App issues:** <https://github.com/Neko-Koubou/nekuva/issues>
- **New manga sources / source fixes:** these belong in
  **[nekuva-exts](https://github.com/Neko-Koubou/nekuva-exts)**, *not* this repo — Nekuva never
  contains site-specific scraping code.

## Credits & lineage

Nekuva is a fork of [DokiTeam/Doki](https://github.com/DokiTeam/Doki), which is itself based on
[KotatsuApp/Kotatsu](https://github.com/KotatsuApp/Kotatsu). Huge thanks to the Doki and Kotatsu
teams and all upstream contributors. Nekuva is now developed independently by
[Neko-Koubou](https://github.com/Neko-Koubou).

## License

Licensed under the **GNU General Public License v3.0**. See [`LICENSE`](LICENSE).

You may copy, distribute and modify the software as long as you track changes/dates in source
files. Any modifications to, or software including (via compiler) GPL-licensed code must also be
made available under the GPL along with build & install instructions.

## Disclaimer

Nekuva does **not** host any manga. It aggregates publicly available, third-party sources through
`nekuva-exts`. Nekuva is not affiliated with any of those sources. Please support the original
creators and official/licensed releases wherever they are available.
