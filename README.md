# CatgirlDownloader
A GTK4 application that downloads images of catgirl based on https://nekos.moe

An Android port (Android 12+) lives in [`android/`](android/).
![](http://nyarchlinux.moe/assets/img/catgirldownloader-screenshots.png)

## Building
1. `meson setup <build_dir> [--prefix <prefix>]`
> Prefix is `/usr/local` by default
2. `meson compile -C <build_dir>`
3. `meson install -C <build_dir> [--destdir <dest_dir>]`
> Package is installed globally by default, or in `<dest_dir>` if `--destdir` is specified (path are relative to `<build_dir>` )

## Packaging
Make sure you have `nfpm` and `sh` installed, then run `./package.sh`.

## Android

The [`android/`](android/) directory holds a native Android port written in Kotlin with Jetpack Compose
and Material 3 (Material You dynamic colors). It requires **Android 12 (API 31) or newer**.

It has the same features as the desktop app: the Catgirl (nekos.moe), Waifu, Danbooru (custom tags) and
Kitsune (nekos.best) sources, NSFW filter, auto reload with a configurable interval, "About art",
a configurable download folder and auto save. On Android, images are saved to `Pictures/CatgirlDownloader`
by default (no storage permission needed), or to any folder picked in Preferences.

### Building
Requires JDK 17+ and the Android SDK (platform 35). Point `ANDROID_HOME` at the SDK, or create
`android/local.properties` with `sdk.dir=/path/to/sdk`, then:

```sh
cd android
./gradlew assembleDebug        # APK in app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit tests
```

Opening `android/` in Android Studio also works. The `Android` GitHub workflow builds a debug APK on every
push and attaches it to releases.
