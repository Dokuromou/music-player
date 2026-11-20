export type RepeatMode = "off" | "all" | "one";

export type LibraryView = "artists" | "albums" | "folders" | "playlists";

export interface Track {
  /** Stable id derived from file path + size. */
  id: string;
  /** Path relative to the picked directory root. */
  path: string;
  fileName: string;
  /** Directory id referencing the scan root, so multiple roots can coexist. */
  rootId: string;
  rootLabel: string;
  title: string;
  artist: string;
  albumArtist: string | null;
  album: string | null;
  discNo: number | null;
  trackNo: number | null;
  year: number | null;
  genre: string | null;
  durationSec: number;
  /** Last modified time of the file, used for incremental rescans. */
  mtimeMs: number;
  sizeBytes: number;
  /** True when tag parsing failed; file may still be playable. */
  metadataFailed: boolean;
  dateAdded: number;
}

export interface Playlist {
  id: string;
  name: string;
  /** Ordered track ids (may reference tracks removed from the library). */
  trackIds: string[];
  dateCreated: number;
}

export interface ScanRoot {
  rootId: string;
  label: string;
  /** Time of the most recent scan of this root. */
  scannedAt: number;
}

export interface PlayerSettings {
  volume: number;
  muted: boolean;
  shuffle: boolean;
  repeat: RepeatMode;
  lastTrackId: string | null;
}
