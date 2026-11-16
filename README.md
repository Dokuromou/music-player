# Musica 🎵

A fully offline, local music player for Android. Musica plays the audio files
already stored on your phone — nothing is uploaded, streamed, or tracked.

## Features

- **Library** — automatically finds all music on your device (via MediaStore)
- **Search** — filter by title, artist, or album
- **Full playback controls** — play/pause, next/previous, seek bar
- **Shuffle & repeat** — repeat one, repeat all, shuffle
- **Background playback** — keeps playing when you leave the app, with
  lock-screen / notification controls (Media3 + MediaSession)
- **Album artwork** — shown in the library, mini-player, and Now Playing screen
- **Modern UI** — Jetpack Compose, Material 3, light & dark themes
- **No internet permission** — the app physically cannot send your data anywhere

## Tech stack

| Layer      | Choice                              |
|------------|-------------------------------------|
| Language   | Kotlin                              |
| UI         | Jetpack Compose + Material 3        |
| Playback   | Media3 (ExoPlayer + MediaSession)   |
| Metadata   | MediaStore API + Coil (artwork)     |
| Build      | Gradle 8.6, AGP 8.2.2, JDK 17       |

## Getting the app on your phone

### Option A — download the ready-made APK (easiest)

1. Open this repo on your phone's browser:
   `https://github.com/Dokuromou/music-player`
2. Go to the **Actions** tab and open the latest green **Build APK** run.
3. Scroll to **Artifacts** and tap **Musica-debug-apk** to download it.
4. Open the downloaded file. Android will ask to allow installing from your
   browser ("Install unknown apps") — allow it once, then tap **Install**.
5. Open **Musica**, grant access to your music, and enjoy.

> If the Actions tab has no runs yet, they start automatically on every push to
> `main`. You can also trigger one manually: **Actions → Build APK → Run workflow**.

### Option B — build it yourself

```bash
git clone https://github.com/Dokuromou/music-player.git
cd music-player
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and press **Run**.

### Option C — install from a computer (ADB)

Enable **Developer options → USB debugging** on the phone, connect it, then:

```bash
adb install app-debug.apk
```

## Project structure

```
app/src/main/java/com/musica/player/
├── MainActivity.kt              # Entry point, Compose host
├── MusicPlayerViewModel.kt      # MediaController wiring + UI state
├── data/
│   ├── Song.kt                  # Song model → MediaItem mapping
│   └── MediaStoreRepository.kt  # Reads the device music library
├── playback/
│   └── PlaybackService.kt       # MediaSessionService (background playback)
└── ui/
    ├── MusicaApp.kt             # Root, permission flow, navigation
    ├── LibraryScreen.kt         # Song list + search
    ├── MiniPlayer.kt            # Bottom mini-player bar
    ├── NowPlayingScreen.kt      # Full-screen player
    └── theme/Theme.kt           # Material 3 theming
```

## Notes

- Minimum supported Android version: **8.0 (API 26)**.
- On Android 13+ the app asks for the `READ_MEDIA_AUDIO` permission; on older
  versions it uses the classic storage permission.
- Songs shorter than 15 seconds are hidden (they are usually ringtones or
  notification sounds).
