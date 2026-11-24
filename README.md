<div align="center">

<!-- decor:top -->
<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sky-banner-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sky-banner-light.svg" width="100%" alt="night sky"></picture>

<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-dots-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-dots-light.svg" width="100%" alt=""></picture>
<!-- /decor:top -->

<img src="icon.png" width="160" height="160" style="display: block; margin: 0 auto"/>
<h1>Celestite</h1>
<p>A lightweight screen recorder for Android that works fully offline.</p>
<p>Hardware-backed capture with audio, a built-in trimmer, and no internet permission, so it literally can't phone home.</p>
</div>
<p align="center">
  <a href="https://github.com/rynekryz/celestite/stargazers"><img src="assets/badges/stars.svg" alt="GitHub stars" height="30"/></a>
  <a href="https://github.com/rynekryz/celestite/tags"><img src="assets/badges/release.svg" alt="Latest release" height="30"/></a>
  <a href="LICENSE"><img src="assets/badges/licencegplv3.svg" alt="Licence GPLv3"/></a>
  <img src="assets/badges/android.svg" alt="Android"/>
  <img src="assets/badges/kotlin.svg" alt="Kotlin"/>
</p>

<p align="center">
  <a href="https://github.com/rynekryz/celestite/releases"><img src="assets/badges/getgithub.svg" alt="Get it on GitHub" height="56"/></a>
</p>

<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-sparkles-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-sparkles-light.svg" width="100%" alt=""></picture>
</div>

## Features

- **Material 3 Expressive**, with dynamic color that follows your wallpaper (Android 12+)
- Hardware-backed recording through `MediaProjection`
- Audio from none, the mic, or the device itself
- H.264, or H.265 for smaller files on newer phones
- Native, 1080p, or 720p profiles that keep low-end devices smooth
- 60 or 30 fps, plus a 3, 5, or 10 second countdown
- Pause and resume for mic and silent recordings
- Optional floating pause/stop pill, off by default. Android won't let an app hide its own overlay from capture, so it shows up as a black box. The notification and QS tile are the default controls.
- Gallery, fullscreen player, and a simple trimmer, all from `Movies/Celestite`
- 100% offline, no tracking, no ads. Every recording stays on your phone
- etc.

<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-dots-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-dots-light.svg" width="100%" alt=""></picture>
</div>

## Technical Stack

- **UI:** Jetpack Compose with Material 3 Expressive
- **Architecture:** multi-module (app, core, feature, service)
- **Capture:** MediaProjection with MediaCodec / MediaRecorder
- **Persistence:** DataStore Preferences
- **Storage:** MediaStore and Scoped Storage
- **Language:** Kotlin
- **Build:** Gradle (Kotlin DSL)

<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-wave-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-wave-light.svg" width="100%" alt=""></picture>
</div>

## Building

You'll need Android Studio (latest stable), JDK 21, and SDK 37. Minimum is Android 12 (API 31).

1. Clone the repo and open it in Android Studio.
2. Sync Gradle and run the `:app` module.

```bash
./gradlew assembleDebug
```

<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-sparkles-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-sparkles-light.svg" width="100%" alt=""></picture>
</div>

## Project Structure

```
Celestite/
├── app/
├── core/
│   ├── model/
│   ├── datastore/
│   └── designsystem/
├── feature/
│   ├── recorder/
│   ├── gallery/
│   └── settings/
└── service/
```

<div align="center">
<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-dots-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-dots-light.svg" width="100%" alt=""></picture>
</div>

## License

Licensed under GPL-3.0. See [LICENSE](LICENSE).

<!-- decor:bottom -->
<div align="center">

<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-wave-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/divider-wave-light.svg" width="100%" alt=""></picture>

<picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-heart-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-heart-light.svg" width="60" alt="heart"></picture> <picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-sparkle-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-sparkle-light.svg" width="60" alt="sparkle"></picture> <picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-moon-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-moon-light.svg" width="60" alt="moon"></picture> <picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-flower-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-flower-light.svg" width="60" alt="flower"></picture> <picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-cat-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-cat-light.svg" width="60" alt="cat"></picture> <picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-ghost-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-ghost-light.svg" width="60" alt="ghost"></picture> <picture><source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-lock-dark.svg"><img src="https://raw.githubusercontent.com/rynekryz/rynekryz/main/pixel-art/sticker-lock-light.svg" width="60" alt="lock"></picture>

</div>
<!-- /decor:bottom -->

<div align="center">
<strong>Made with ♥︎ by Ryne</strong>
</div>
<div align="center">
  <a href="https://discord.gg/YKhWXXhKmC"><img src="assets/badges/discord.svg" alt="Discord"/></a>
  <a href="https://tiktok.com/@rynekryz"><img src="assets/badges/tiktok.svg" alt="TikTok"/></a>
</div>
