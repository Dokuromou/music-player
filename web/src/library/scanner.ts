import type { Track } from "../types";
import { parseTrackFile, extensionOf } from "../audio/parseMetadata";
import * as db from "./db";

export const AUDIO_EXTENSIONS = new Set(["mp3", "flac", "wav", "ogg", "oga", "m4a", "opus"]);

/** Stable id: root + relative path + size. Survives rescans of the same root. */
export function makeTrackId(rootId: string, path: string, sizeBytes: number): string {
  const s = `${rootId}::${path}::${sizeBytes}`;
  return hash32(s).toString(36).padStart(7, "0");
}

function hash32(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

export interface ScanProgress {
  rootId: string;
  scanned: number;
  total: number;
  currentFile: string;
}

export interface ScanResult {
  rootId: string;
  label: string;
  added: number;
  updated: number;
  failed: number;
  total: number;
}

/**
 * Recursively collects audio files from a directory handle.
 * Uses the File System Access API (Chromium browsers).
 * Unreadable files are skipped silently; they just never appear in the library.
 */
export async function collectAudioFiles(
  entry: FileSystemDirectoryHandle,
  relative = ""
): Promise<Array<{ file: File; path: string; mtimeMs: number }>> {
  const out: Array<{ file: File; path: string; mtimeMs: number }> = [];
  const stack: Array<{ handle: FileSystemDirectoryHandle; rel: string }> = [
    { handle: entry, rel: relative },
  ];

  while (stack.length > 0) {
    const { handle, rel } = stack.shift()!;
    for await (const [name, child] of handle) {
      const path = rel ? `${rel}/${name}` : name;
      if (child.kind === "file") {
        if (!AUDIO_EXTENSIONS.has(extensionOf(name))) continue;
        try {
          const file = await child.getFile();
          out.push({ file, path, mtimeMs: file.lastModified });
        } catch {
          // unreadable file: skip
        }
      } else {
        stack.push({ handle: child as FileSystemDirectoryHandle, rel: path });
      }
    }
  }
  return out;
}

/**
 * Scans a picked directory, parses metadata, persists tracks + covers.
 * Sequential per file to keep memory bounded; progress callback for UI.
 * Incremental: existing track records keep their original dateAdded.
 */
export async function scanRoot(
  entry: FileSystemDirectoryHandle,
  onProgress?: (p: ScanProgress) => void
): Promise<ScanResult> {
  const rootId = `root-${hash32(entry.name)}`;
  const label = entry.name;

  const files = await collectAudioFiles(entry);
  let added = 0;
  let updated = 0;
  let failed = 0;

  for (let i = 0; i < files.length; i++) {
    const { file, path, mtimeMs } = files[i];
    onProgress?.({ rootId, scanned: i, total: files.length, currentFile: path });

    const id = makeTrackId(rootId, path, file.size);
    const parsed = await parseTrackFile(file, path, rootId, label, mtimeMs);

    if (parsed.cover && parsed.album) {
      const albumKey = `${rootId}::${parsed.albumArtist ?? parsed.artist}::${parsed.album}`;
      const known = await db.getAlbumCover(albumKey);
      if (!known) await db.saveAlbumCover(albumKey, parsed.cover);
    }
    await db.saveCover(id, parsed.cover ?? placeholderBlob());

    const existing = await db.getTrack(id);
    const track: Track = {
      id,
      path,
      fileName: parsed.fileName,
      rootId,
      rootLabel,
      title: parsed.title,
      artist: parsed.artist,
      albumArtist: parsed.albumArtist,
      album: parsed.album,
      discNo: parsed.discNo,
      trackNo: parsed.trackNo,
      year: parsed.year,
      genre: parsed.genre,
      durationSec: parsed.durationSec,
      mtimeMs,
      sizeBytes: parsed.sizeBytes,
      metadataFailed: parsed.metadataFailed,
      dateAdded: existing?.dateAdded ?? Date.now(),
    };

    if (existing) updated++;
    else added++;
    await db.saveTracks([track]);
    if (parsed.metadataFailed) failed++;
  }

  await db.saveScanRoots([
    ...(await db.loadScanRoots()).filter((r) => r.rootId !== rootId),
    { rootId, label, scannedAt: Date.now() },
  ]);

  onProgress?.({ rootId, scanned: files.length, total: files.length, currentFile: "" });
  return { rootId, label, added, updated, failed, total: files.length };
}

function placeholderBlob(): Blob {
  return new Blob([], { type: "application/octet-stream" });
}
