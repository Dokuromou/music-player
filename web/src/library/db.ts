import type { ScanRoot, Track } from "../types";

/**
 * IndexedDB persistence for the library.
 * Stores: tracks (records), covers (blobs), meta (scan state), handles (dir handles).
 * If IndexedDB is unavailable (private mode, old browser), methods resolve to
 * empty values so the app still works for one session.
 */

const DB_NAME = "localtune";
const DB_VERSION = 1;

type StoreName = "tracks" | "covers" | "meta" | "handles";

let dbPromise: Promise<IDBDatabase> | null = null;

function openDb(): Promise<IDBDatabase> {
  if (dbPromise) return dbPromise;
  dbPromise = new Promise((resolve, reject) => {
    if (typeof indexedDB === "undefined") {
      reject(new Error("IndexedDB unavailable"));
      return;
    }
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains("tracks")) {
        const s = db.createObjectStore("tracks", { keyPath: "id" });
        s.createIndex("rootId", "rootId");
        s.createIndex("album", "album");
      }
      if (!db.objectStoreNames.contains("covers")) db.createObjectStore("covers");
      if (!db.objectStoreNames.contains("meta")) db.createObjectStore("meta");
      if (!db.objectStoreNames.contains("handles")) db.createObjectStore("handles");
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error ?? new Error("IndexedDB open failed"));
  });
  return dbPromise;
}

async function tx<T>(
  store: StoreName,
  mode: IDBTransactionMode,
  fn: (s: IDBObjectStore) => IDBRequest<T>
): Promise<T | null> {
  try {
    const db = await openDb();
    return await new Promise((resolve, reject) => {
      const t = db.transaction(store, mode);
      const req = fn(t.objectStore(store));
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  } catch {
    return null; // graceful: memory-only session
  }
}

// ---- tracks ----

export async function loadAllTracks(): Promise<Track[]> {
  return (await tx<Track[]>("tracks", "readonly", (s) => s.getAll())) ?? [];
}

export async function getTrack(id: string): Promise<Track | null> {
  return (await tx<Track>("tracks", "readonly", (s) => s.get(id))) ?? null;
}

export async function saveTracks(tracks: Track[]): Promise<void> {
  if (tracks.length === 0) return;
  await tx("tracks", "readwrite", (s) => {
    for (const t of tracks) s.put(t);
    return s.getAllKeys();
  });
}

export async function deleteTracks(ids: string[]): Promise<void> {
  if (ids.length === 0) return;
  await tx("tracks", "readwrite", (s) => {
    for (const id of ids) s.delete(id);
    return s.getAllKeys();
  });
}

// ---- covers ----

export async function saveCover(trackId: string, blob: Blob): Promise<void> {
  await tx("covers", "readwrite", (s) => s.put(blob, `t:${trackId}`));
}

export async function getCover(trackId: string): Promise<Blob | null> {
  return (await tx<Blob>("covers", "readonly", (s) => s.get(`t:${trackId}`))) ?? null;
}

export async function saveAlbumCover(albumKey: string, blob: Blob): Promise<void> {
  await tx("covers", "readwrite", (s) => s.put(blob, `a:${albumKey}`));
}

export async function getAlbumCover(albumKey: string): Promise<Blob | null> {
  return (await tx<Blob>("covers", "readonly", (s) => s.get(`a:${albumKey}`))) ?? null;
}

// ---- meta / scan state ----

export async function setMeta(key: string, value: unknown): Promise<void> {
  await tx("meta", "readwrite", (s) => s.put(value, key));
}

export async function getMeta<T>(key: string): Promise<T | null> {
  return ((await tx<T>("meta", "readonly", (s) => s.get(key))) as T) ?? null;
}

export async function saveScanRoots(roots: ScanRoot[]): Promise<void> {
  await setMeta("scanRoots", roots);
}

export async function loadScanRoots(): Promise<ScanRoot[]> {
  return (await getMeta<ScanRoot[]>("scanRoots")) ?? [];
}
