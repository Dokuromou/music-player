import { parseBlob } from "music-metadata";
import type { Track } from "../types";

/**
 * Metadata parsing. Any failure degrades gracefully: we still keep the track
 * with a filename-derived title so the library never loses playable files.
 */

const MISSING = "Unknown";

// keep in sync with library/scanner.ts
const AUDIO_EXTENSIONS = new Set(["mp3", "flac", "wav", "ogg", "oga", "m4a", "opus"]);

function nullify(v: unknown): string | null {
  return typeof v === "string" && v.trim() !== "" ? v.trim() : null;
}

export interface ParsedTrack extends Omit<Track, "id" | "dateAdded"> {
  /** Cover art as a raw blob, if present. */
  cover: Blob | null;
}

export async function parseTrackFile(
  file: File,
  relativePath: string,
  rootId: string,
  rootLabel: string,
  mtimeMs: number
): Promise<ParsedTrack> {
  const base: ParsedTrack = {
    path: relativePath,
    fileName: file.name,
    rootId,
    rootLabel,
    title: prettifyFileName(file.name),
    artist: MISSING,
    albumArtist: null,
    album: null,
    discNo: null,
    trackNo: null,
    year: null,
    genre: null,
    durationSec: 0,
    mtimeMs,
    sizeBytes: file.size,
    metadataFailed: true,
    cover: null,
  };

  const ext = extensionOf(file.name);
  if (!AUDIO_EXTENSIONS.has(ext)) return base;

  try {
    const meta = await parseBlob(file, { duration: true, skipPostHeaders: true });
    const c = meta.common;
    const cover = meta.common.picture?.length
      ? new Blob([new Uint8Array(meta.common.picture[0].data)], {
          type: meta.common.picture[0].format,
        })
      : null;

    return {
      ...base,
      title: c.title?.toString().trim() || base.title,
      artist: c.artist?.toString().trim() || MISSING,
      albumArtist: nullify(c.albumartist),
      album: nullify(c.album),
      discNo: c.disk?.no ?? null,
      trackNo: c.track?.no ?? null,
      year: c.year ?? null,
      genre: nullify(c.genre),
      durationSec: Number.isFinite(meta.format.duration ?? NaN)
        ? Math.round((meta.format.duration as number) * 100) / 100
        : 0,
      metadataFailed: false,
      cover,
    };
  } catch {
    return base; // graceful degradation
  }
}

export function prettifyFileName(name: string): string {
  return name
    .replace(/\.[a-z0-9]+$/i, "")
    .replace(/^\d{1,3}[\s._-]+/, "") // leading "01 - "
    .replace(/[_]+/g, " ")
    .trim();
}

export function extensionOf(name: string): string {
  const i = name.lastIndexOf(".");
  return i === -1 ? "" : name.slice(i + 1).toLowerCase();
}
