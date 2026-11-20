import type { Playlist } from "../types";

/**
 * Playlists are small, so they live in localStorage as JSON.
 * Corrupt or missing data resolves to an empty list, never a crash.
 */

const KEY = "localtune.playlists.v1";

export function loadPlaylists(): Playlist[] {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (p): p is Playlist =>
        p && typeof p.id === "string" && typeof p.name === "string" && Array.isArray(p.trackIds)
    );
  } catch {
    return [];
  }
}

export function savePlaylists(playlists: Playlist[]): void {
  try {
    localStorage.setItem(KEY, JSON.stringify(playlists));
  } catch {
    // storage full or blocked: keep working in-memory
  }
}

export function newPlaylist(name: string): Playlist {
  return { id: crypto.randomUUID(), name, trackIds: [], dateCreated: Date.now() };
}
