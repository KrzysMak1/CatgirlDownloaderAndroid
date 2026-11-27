# Catgirl Downloader for Android

A native Android app (Android 12 or newer) for browsing and saving random anime-style images of catgirls,
waifus and foxgirls. It is a port of the GTK4 desktop app
[CatgirlDownloader](https://github.com/NyarchLinux/CatgirlDownloader) by the Nyarch Linux team, rewritten
in Kotlin with Jetpack Compose and Material 3.

## Download

Get `CatgirlDownloader-<version>.apk` from the [**latest release**](../../releases/latest) and open it on
your phone. Updates are signed with the same key, so new versions install over the old one.

Coming from a 0.5.x pre-release? Uninstall it once first: pre-releases used a temporary signing key. Android asks you to allow installing
apps from your browser or file manager the first time.

Requirements: **Android 12 (API 31) or newer**, internet access.

## Features

- **Four image sources**, switchable from the title bar:
  - **Catgirl**: [nekos.moe](https://nekos.moe)
  - **Waifu**: waifu.im-compatible API
  - **Danbooru**: [danbooru.donmai.us](https://danbooru.donmai.us), with your own search tags,
    tag suggestions while typing and a counter for Danbooru's 2-tag limit
  - **Kitsune**: foxgirls from [nekos.best](https://nekos.best) (SFW only)
- **NSFW filter**: *Block NSFW* (default), *Show everything* or *Only NSFW*.
- **Blacklisted tags**: images with any of these tags are skipped (Catgirl, Waifu, Danbooru).
- **Auto reload**: loads a new image every *N* seconds (1–3600). It pauses while the app is in the background.
- **About art**: shows the artist and a link to the image's original source.
- **Saving**:
  - default folder `Pictures/CatgirlDownloader`, which needs no storage permission;
  - or any folder you pick in *Preferences*;
  - with *Auto save* off, the save button opens the system "Save as" picker.
- **Fast loading**: the next image is downloaded in the background while you look at the current one
  (can be turned off in *Preferences* to save data). Danbooru shows a lighter preview and downloads the
  full-resolution original only when you save. Turn on *Original quality* in *Preferences* to load
  (and preload) full-resolution originals instead; this only affects what is shown, nothing is saved
  automatically.
- Pinch-to-zoom and double-tap zoom, animated GIF/WebP support.
- Material You colors from your wallpaper, light and dark theme, themed launcher icon (Android 13+).
- Custom background behind the images, with adjustable darkening.
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
push. Releases are published automatically:

- on a push to `master`, as `v<versionName>` (from `android/app/build.gradle.kts`) if that release does not
  exist yet. Published releases are never overwritten, so **bump `versionName` and `versionCode` to
  release a new version**, and update [`android/release-notes.md`](android/release-notes.md);
- when a tag starting with `v` is pushed;
- or manually from **Actions → Android → Run workflow** with a release tag.

Release APKs are signed with the project's key, stored in these repository secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | the `.jks` keystore, base64-encoded (`base64 -w0 release.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password |

The workflow refuses to publish a release without them or with a debug-signed APK. Keep a backup of the
keystore and its password: without them no further updates can be released. Builds without the secrets
(e.g. pull requests from forks) are signed with a debug key and are not published.

A keystore can be created with
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
