# RedGIF

An unofficial Android client for [RedGifs](https://www.redgifs.com), built with Jetpack Compose and Media3 ExoPlayer. Its layout and behavior follow [Infinity for Reddit](https://github.com/Docile-Alligator/Infinity-For-Reddit): a drawer for navigation, a tabbed feed, full-screen vertical viewer, and a dark theme.

## Features

- **Browse feed** ordered by Trending, Latest, Top 7 days, Top 28 days, or Top all time, plus a GIF/video vs. image filter.
- **Search** RedGifs by text, with recent searches kept on the device.
- **Creator pages** showing a creator's media.
- **Full-screen viewer** with vertical paging, tap to play/pause, mute, favorite, share, and open in browser. The next clip is prefetched while you watch.
- **Favorites** with a dropdown that switches between **Media** and **Creators**.
- **Account sign-in** with your own RedGifs account, through the RedGifs website inside the app. Signing out clears the website session cookies.
- **Settings**: theme (system / light / dark / black), autoplay policy (Wi-Fi only or always), playback quality (auto / SD / HD), default mute, clear cache, and version info.
- **Playback**: ExoPlayer with a shared disk cache (up to 384 MB) and tuned buffer sizes for fast starts with little rebuffering. HD is preferred when the setting allows it.

## Requirements

- Android Studio Ladybug (2024.2) or newer, or any recent Android Studio build.
- JDK 17 (bundled with Android Studio).
- Android SDK Platform 35.
- Android device or emulator running Android 8.0 (API 26) or newer.

## Open in Android Studio

1. Clone the repository:
   ```bash
   git clone https://github.com/louaysadok/REDGIF.git
   ```
2. In Android Studio choose **File → Open** and select the `REDGIF` folder.
3. Let Gradle sync. Android Studio downloads the Gradle version in `gradle/wrapper/gradle-wrapper.properties` (8.9) and all dependencies from `gradle/libs.versions.toml`.
4. Press **Run**.

## Build the APK with GitHub Actions

No local setup is needed. The workflow in `.github/workflows/build-apk.yml` builds a debug APK on every push and pull request.

1. Open the repository on GitHub and go to the **Actions** tab.
2. Choose **Build APK** in the left list. To start a build manually, click **Run workflow**, pick the branch, and click **Run workflow** again.
3. When the run finishes (about 5–10 minutes), open it and download **RedGIF-debug-apk** from the **Artifacts** section.
4. Unzip the download and install the APK on an Android 8.0+ device. You may need to allow installs from unknown sources.

The APK is signed with the default debug key, so it is for testing, not for the Play Store.

## Build from the command line

The Gradle wrapper jar and `gradlew` scripts are not committed to the repository. Generate them once with a local Gradle 8.9 install (or any Gradle that can run the wrapper task):

```bash
cd REDGIF
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

On Windows, use `gradlew.bat` in place of `./gradlew`.

## Project layout

```
app/src/main/java/com/louaysadok/redgif/
  AppContainer.kt         manual dependency container (OkHttp, Room, DataStore, Coil, media cache)
  MainActivity.kt         single activity hosting the Compose app
  data/
    net/                  RedGifs API client, DTOs, and repository
    media/                ExoPlayer/disk-cache setup, prefetching, viewer queue
    local/                Room database (favorites, recent searches)
    settings/             DataStore-backed app settings
    model/                UI-facing media model
  ui/
    AppRoot.kt            navigation drawer, NavHost routes, age gate
    feed/                 feed screen, view model, media cards
    player/               full-screen viewer and video surface
    search/               search screen
    favorites/            favorites screen (Media / Creators)
    account/              account screen and RedGifs login WebView
    settings/             settings screen
    common/               shared components
    theme/                Material 3 theme
```

## Limitations

- **Sign-in is not verified against the RedGifs session.** The "I'm logged in" button in the login screen sets the signed-in flag in the app. It does not check that the website session is valid.
- **API calls use RedGifs' public temporary token.** RedGifs does not publish an official account-login or favorites API, so the app does not yet send your account session to the API. Results therefore come from the public API whether or not you are signed in.
- **Favorites are stored on the device** (Room), not synced to your RedGifs account.
- **Adult content.** The app shows an age gate on first launch. RedGifs content is intended for adults only.
- **Not tested on a device in this repository.** The code was written and syntax-checked without an Android SDK. Run a debug build before relying on it, and report any compile errors.
- The app is not affiliated with or endorsed by RedGifs or Docile-Alligator.

## Contributing

Open an issue or a pull request against `main`. Keep changes consistent with the Infinity-style layout and behavior.
