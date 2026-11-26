# Catgirl Downloader for Android

A native Android app (Android 12 or newer) for browsing and saving random anime-style images of catgirls,
waifus and foxgirls. It is a port of the GTK4 desktop app
[CatgirlDownloader](https://github.com/NyarchLinux/CatgirlDownloader) by the Nyarch Linux team, rewritten
in Kotlin with Jetpack Compose and Material 3.

## Download

Get `CatgirlDownloader-<version>.apk` from the [**Releases**](../../releases) page and open it on your phone.
Current builds are **pre-releases** (testing versions), not yet a finished release. Android asks you to allow installing
apps from your browser or file manager the first time.

Requirements: **Android 12 (API 31) or newer**, internet access.

## Features

- **Four image sources**, switchable from the title bar:
  - **Catgirl**: [nekos.moe](https://nekos.moe)
  - **Waifu**: waifu.im-compatible API
  - **Danbooru**: [danbooru.donmai.us](https://danbooru.donmai.us), with your own search tags
  - **Kitsune**: foxgirls from [nekos.best](https://nekos.best) (SFW only)
- **NSFW filter**: *Block NSFW* (default), *Show everything* or *Only NSFW*.
- **Auto reload**: loads a new image every *N* seconds (1–3600). It pauses while the app is in the background.
- **About art**: shows the artist and a link to the image's original source.
- **Saving**:
  - default folder `Pictures/CatgirlDownloader`, which needs no storage permission;
  - or any folder you pick in *Preferences*;
  - with *Auto save* off, the save button opens the system "Save as" picker.
- **Fast loading**: the next image is downloaded in the background while you look at the current one
  (can be turned off in *Preferences* to save data). Danbooru shows a lighter preview and downloads the
  full-resolution original only when you save.
- Pinch-to-zoom and double-tap zoom, animated GIF/WebP support.
- Material You colors from your wallpaper, light and dark theme, themed launcher icon (Android 13+).
- English and Polish interface.

Danbooru hides certain tags from anonymous users. The app strips those tags from your search and tells you
when it does.

## Building from source

Requirements: JDK 17+ and the Android SDK with platform 35. Set `ANDROID_HOME` to the SDK location, or create
`android/local.properties` containing `sdk.dir=/path/to/android/sdk`.

```sh
cd android
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # app/build/outputs/apk/release/app-release.apk (minified)
./gradlew testDebugUnitTest      # unit tests
```

You can also open the `android/` folder in Android Studio.

### Project layout

```
android/app/src/main/java/moe/nyarchlinux/catgirldownloader/
├── api/        image sources (nekos.moe, waifu, Danbooru, nekos.best) and the HTTP client
├── data/       settings (NSFW mode, auto reload, Danbooru tags, download folder, auto save)
├── storage/    image format detection and saving (MediaStore / Storage Access Framework)
├── ui/         Compose screens: main screen, preferences, dialogs, theme
└── MainViewModel.kt   loading, auto reload and saving logic
```

## Releases and signing

The [`Android`](.github/workflows/android.yml) GitHub workflow runs the unit tests and builds the APK on every
push. Releases with the APK attached are published automatically, as **pre-releases**:

- on a push to `master`, as `v<versionName>` (from `android/app/build.gradle.kts`); pushing again with the
  same version refreshes that pre-release's APK, so **bump `versionName` and `versionCode` for a new version**;
- when a tag starting with `v` is pushed;
- or manually from **Actions → Android → Run workflow** with a release tag.

When the app is ready for a stable release, set `prerelease: false` (and `make_latest: true`) in the
workflow's *Publish GitHub release* step, or edit the release on GitHub.

By default the release APK is signed with a temporary debug key. To sign it with your own key, so that
users can update the app without uninstalling it first, add these repository secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | your `.jks` keystore, base64-encoded (`base64 -w0 release.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

You can create a keystore with
`keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 4096 -validity 10000 -alias catgirldownloader`.

## Desktop version

The original GTK4/Libadwaita desktop app still lives in this repository (`src/`, `data/`, `meson.build`).
Build it with Meson:

```sh
meson setup build
meson compile -C build
meson install -C build
```

Run `./package.sh` (needs `nfpm`) to build distribution packages.

## License

GNU General Public License v3.0 or later. See [COPYING](COPYING).
Original app © Nyarch Linux developers team / SilverOS.
