# Localtune (web)

A local-first music player. Your files never leave your machine: no uploads, no servers, no external APIs. Localtune reads audio from a folder you pick with your browser's File System Access API, parses tags in the browser, and keeps your library metadata in IndexedDB on your own device.

> This lives in the \`web/\` folder of this repository, on the \`web-player\` branch. The Android app (Musica) at the root is unaffected.

## Features

- **Local playback** of MP3, FLAC, WAV, OGG (plus M4A/Opus, browser-permitting) directly from a user-selected directory.
- **Metadata & library**: full ID3/Vorbis/FLAC tag parsing via \`music-metadata\`, including cover art, duration, disc/track numbers, year, and genre. Organize by artist, album, folder, or playlists.
- **Core controls**: play/pause, next/previous, seek bar with scrubbing, volume + mute, shuffle, repeat (off / all / one).
- **Queue**: play next, add to queue, remove, jump to any position.
- **Graceful degradation**: files with unreadable tags still show up (file-name-based title), unreadable files are skipped, missing cover art falls back to a placeholder, and everything keeps working if IndexedDB is blocked.
- **Persistence** (all local): IndexedDB for track metadata, covers, scan roots and directory handles; localStorage (JSON) for playlists and player settings (volume, mute, shuffle, repeat, last track). Incremental rescans keep existing entries.
- **Keyboard shortcuts**: Space play/pause, arrows for seek/volume, N next, P previous, M mute, R repeat cycle, S shuffle.

## Tech stack

| Layer      | Choice |
| ---------- | ------ |
| Language   | TypeScript (strict) |
| UI         | React 18, hand-rolled CSS (no UI framework) |
| Build      | Vite |
| State      | Zustand |
| Tags       | \`music-metadata\` |
| Audio      | \`HTMLAudioElement\` wrapped by a single \`AudioEngine\` class |
| Persistence| IndexedDB (\`library/db.ts\`), JSON + localStorage (settings, playlists) |
| File access| File System Access API (\`showDirectoryPicker\`, persisted handles) |

## Setup & run

Requirements: Node.js 18+, and a Chromium-based browser (Chrome, Edge, Opera, Brave).

\`\`\`bash
cd web
npm install
npm run dev        # http://localhost:5173
\`\`\`

Then click **Add music folder** and grant read access to your music directory.

\`\`\`bash
npm run build      # production build in web/dist
npm run preview
npm run typecheck
\`\`\`

To publish with GitHub Pages: serve the \`web/dist\` output, or point Pages at a workflow that builds \`web/\`. The app works offline once loaded.

## Architecture

\`\`\`
web/src/
├── types/            shared domain types (Track, Playlist, RepeatMode…)
├── audio/
│   ├── AudioEngine.ts    the only module that touches playback APIs
│   └── parseMetadata.ts  tag + cover parsing with graceful fallbacks
├── library/
│   ├── db.ts             IndexedDB: tracks, covers, scan meta, dir handles
│   ├── scanner.ts        directory walk, incremental scans, orchestration
│   └── playlists.ts      playlist CRUD (JSON in localStorage)
├── state/
│   └── playerStore.ts    zustand store; wires engine events to UI
├── hooks/
│   └── useKeyboardShortcuts.ts
└── components/       Sidebar, TrackList, PlayerBar, CurrentTrack, Queue
\`\`\`

Design rules:

1. **One-way dependencies**: \`components → state → (audio, library) → types\`. Nothing in \`audio/\` or \`library/\` imports React.
2. **No hardcoded paths**: directories are chosen by the user at runtime; handles are persisted per root.
3. **No external APIs**: nothing talks to the network after the app is loaded.

## Keyboard shortcuts

| Key | Action |
| --- | ------ |
| \`Space\` | Play / pause |
| \`←\` / \`→\` | Seek −5s / +5s |
| \`↑\` / \`↓\` | Volume −/+ 5% |
| \`N\` / \`P\` | Next / previous track |
| \`M\` | Mute toggle |
| \`R\` | Cycle repeat: off → all → one |
| \`S\` | Toggle shuffle |

## Privacy

- Audio files are opened locally through the File System Access API; they are never uploaded.
- Cover art and parsed tags are stored in your browser's IndexedDB.
- No telemetry, no analytics, no third-party network calls.

## License

MIT, see [LICENSE](./LICENSE).
