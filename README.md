# Musica 🎵

A fully offline, local music player for Android. Musica plays the audio files
already stored on your phone — nothing is uploaded, streamed, or tracked.

## Features

- **Spotify-style Home** — greeting, "Jump back in" shortcuts, shelves for albums, artists, playlists and favorites
- **Search tab** — live results across songs, albums and artists + recent searches
- **Your Library tab** — filter chips for Playlists / Artists / Albums / Songs
- **Edit mode** — an edit button on every album, artist and playlist: reorder songs with arrows, remove songs, reset to default order

- **Library** — finds all music on your device automatically (MediaStore)
- **Browse by Albums & Artists** — with artwork grid and detail views
- **Playlists** — create your own, add songs with a long-press, play or delete them
- **Favorites** — tap the heart on any song
- **Recently played** — automatic history
- **Queue view** — see what's up next, jump to any song, remove songs
- **Search & sorting** — filter by title/artist/album, sort by title, artist, album, duration or recently added
- **Sleep timer** — 15/30/60 minutes
- **Playback speed** — 0.5x to 2.0x
- **Full playback controls** — play/pause, next/previous, seek bar, shuffle, repeat one/all
- **Background playback** — lock-screen / notification controls (Media3 + MediaSession)
- **Pure black (AMOLED) theme** — saves battery on OLED screens
- **No internet permission** — the app physically cannot send your data anywhere

## Tech stack

| Layer      | Choice                              |
|------------|-------------------------------------|
| Language   | Kotlin                              |
| UI         | Jetpack Compose + Material 3        |
| Playback   | Media3 (ExoPlayer + MediaSession)   |
| Metadata   | MediaStore API + Coil (artwork)     |
| Storage    | SharedPreferences (playlists, favorites) |
| Build      | Gradle 8.6, AGP 8.2.2, JDK 17       |

## Getting the app on your phone

**Direct download (no GitHub navigation needed):**

Open this link on your phone:

https://github.com/Dokuromou/music-player/raw/main/app-debug.apk

Then open the downloaded file and install (allow "install unknown apps" when asked).

**Or build it yourself:**

```bash
git clone https://github.com/Dokuromou/music-player.git
cd music-player
./gradlew assembleDebug
```

## Project structure

```
app/src/main/java/com/musica/player/
├── MainActivity.kt              # Entry point, Compose host
├── MusicPlayerViewModel.kt      # MediaController wiring + all UI state
├── data/
│   ├── Song.kt                  # Song model → MediaItem mapping
│   ├── Playlist.kt              # Playlist model
│   ├── LocalStore.kt            # Persistent favorites/playlists/history/theme
│   └── MediaStoreRepository.kt  # Reads the device music library
├── playback/
│   └── PlaybackService.kt       # MediaSessionService (background playback)
└── ui/
    ├── MusicaApp.kt             # Root, permissions, bottom navigation
    ├── SongsScreen.kt           # Song list, search, sorting
    ├── AlbumsScreen.kt          # Album grid
    ├── ArtistsScreen.kt         # Artist list
    ├── PlaylistsScreen.kt       # Playlists + dialogs
    ├── DetailScreens.kt         # Album/artist/playlist detail
    ├── MiniPlayer.kt            # Bottom mini-player bar
    ├── NowPlayingScreen.kt      # Full player: queue, sleep timer, speed
    ├── Components.kt            # Shared rows/artwork
    ├── SettingsScreen.kt        # Theme, timer, speed, rescan, about
    └── theme/Theme.kt           # Material 3 theming (+ AMOLED)
```

## Notes

- Minimum supported Android version: **8.0 (API 26)**.
- On Android 13+ the app asks for the `READ_MEDIA_AUDIO` permission; on older
  versions it uses the classic storage permission.
- Songs shorter than 15 seconds are hidden (they are usually ringtones or
  notification sounds).
