import { create } from "zustand";
import type { Playlist, PlayerSettings, RepeatMode, Track } from "../types";
import * as db from "../library/db";
import { loadPlaylists, savePlaylists, newPlaylist } from "../library/playlists";
import { AudioEngine } from "../audio/AudioEngine";

/**
 * Single zustand store. AudioEngine sits behind it: store methods call the
 * engine, engine events flow back through a subscription installed at boot.
 */

const SETTINGS_KEY = "localtune.settings.v1";

function loadSettings(): PlayerSettings {
  const defaults: PlayerSettings = {
    volume: 0.8,
    muted: false,
    shuffle: false,
    repeat: "off" as RepeatMode,
    lastTrackId: null,
  };
  try {
    const raw = localStorage.getItem(SETTINGS_KEY);
    if (!raw) return defaults;
    return { ...defaults, ...(JSON.parse(raw) as Partial<PlayerSettings>) };
  } catch {
    return defaults;
  }
}

function persistSettings(s: PlayerSettings): void {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(s));
  } catch {
    /* non-fatal */
  }
}

/** Resolve a track to a playable object URL via the persisted directory handle. */
async function resolveAudioSource(track: Track): Promise<string> {
  const handle = (await db.getMeta<FileSystemFileHandle extends never ? never : FileSystemDirectoryHandle>(
    `handle:${track.rootId}`
  )) as FileSystemDirectoryHandle | null;
  if (!handle) {
    throw new Error(`the folder "${track.rootLabel}" needs to be re-added (browser storage was cleared)`);
  }
  if (handle.queryPermission) {
    const perm = (await handle.queryPermission({ mode: "read" })) as PermissionState;
    if (perm !== "granted") {
      const granted = (await handle.requestPermission({ mode: "read" })) as PermissionState;
      if (granted !== "granted") throw new Error("read permission denied");
    }
  }
  const fileHandle = await handle.getFileHandle(track.path, { create: false });
  const file = await fileHandle.getFile();
  return URL.createObjectURL(file);
}

let engine: AudioEngine | null = null;

export function getEngine(): AudioEngine | null {
  return engine;
}

export function initEngine(): void {
  if (engine) return;
  engine = new AudioEngine({
    onEnded: () => useStore.getState().onTrackEnded(),
    onTimeUpdate: (t) => useStore.setState({ currentTime: t }),
    onDurationChange: (d) => {
      if (Number.isFinite(d) && d > 0) useStore.setState({ duration: d });
    },
    onPlaying: () => useStore.setState({ playing: true, error: null }),
    onPause: () => useStore.setState({ playing: false }),
    onLoading: () => useStore.setState({ error: null }),
    onError: () =>
      useStore.setState({
        playing: false,
        error:
          "Could not play this file. It may be missing, unreadable, or in a format this browser cannot decode.",
      }),
  });
  const s = useStore.getState();
  engine.setVolume(s.settings.volume);
  engine.setMuted(s.settings.muted);
}

export interface Toast {
  id: number;
  text: string;
}

interface PlayerState {
  // library
  tracks: Track[];
  libraryLoaded: boolean;
  scanning: boolean;
  scanProgress: { scanned: number; total: number; currentFile: string } | null;
  scanMessage: string | null;

  // playback
  currentTrack: Track | null;
  queue: string[]; // ordered track ids
  queueIndex: number;
  playing: boolean;
  currentTime: number;
  duration: number;
  error: string | null;
  settings: PlayerSettings;

  playlists: Playlist[];
  toasts: Toast[];

  // ---- library actions ----
  loadLibrary(): Promise<void>;
  addDirectory(): Promise<void>;
  removeRoot(rootId: string): Promise<void>;

  // ---- playback actions ----
  playTrack(id: string, contextIds: string[]): Promise<void>;
  togglePlay(): Promise<void>;
  next(): Promise<void>;
  previous(): Promise<void>;
  seekTo(t: number): void;
  seekBy(d: number): void;
  setVolume(v: number): void;
  toggleMute(): void;
  toggleShuffle(): void;
  cycleRepeat(): void;
  onTrackEnded(): void;
  enqueue(ids: string[], playNext?: boolean): void;
  removeFromQueue(queuePos: number): void;
  jumpToQueuePos(pos: number): Promise<void>;

  // ---- playlist actions ----
  createPlaylist(name: string): void;
  renamePlaylist(id: string, name: string): void;
  deletePlaylist(id: string): void;
  addToPlaylist(playlistId: string, trackIds: string[]): void;
  removeFromPlaylist(playlistId: string, trackId: string): void;

  notify(text: string): void;
  dismissToast(id: number): void;
}

function nextShuffle(queue: string[], current: number): number {
  if (queue.length <= 1) return current;
  let n = current;
  while (n === current) n = Math.floor(Math.random() * queue.length);
  return n;
}

export const useStore = create<PlayerState>((set, get) => {
  const persist = () => persistSettings(get().settings);

  return {
    tracks: [],
    libraryLoaded: false,
    scanning: false,
    scanProgress: null,
    scanMessage: null,

    currentTrack: null,
    queue: [],
    queueIndex: -1,
    playing: false,
    currentTime: 0,
    duration: 0,
    error: null,
    settings: loadSettings(),

    playlists: loadPlaylists(),
    toasts: [],

    async loadLibrary() {
      const tracks = await db.loadAllTracks();
      const settings = get().settings;
      set({ tracks, libraryLoaded: true });
      if (settings.lastTrackId) {
        const t = tracks.find((tr) => tr.id === settings.lastTrackId);
        if (t) set({ currentTrack: t, duration: t.durationSec });
      }
    },

    async addDirectory() {
      const w = window as unknown as {
        showDirectoryPicker?: (o?: unknown) => Promise<FileSystemDirectoryHandle>;
      };
      if (!w.showDirectoryPicker) {
        get().notify("Your browser does not support local folder access (try Chrome, Edge or Opera).");
        return;
      }
      try {
        set({ scanning: true, scanProgress: null, scanMessage: null });
        const handle = await w.showDirectoryPicker({ id: "localtune-music", mode: "read" });

        await db.setMeta(`handle:root-${hashString(handle.name)}`, handle);

        const { scanRoot } = await import("../library/scanner");
        const result = await scanRoot(handle, (p) =>
          set({ scanProgress: { scanned: p.scanned, total: p.total, currentFile: p.currentFile } })
        );

        const tracks = await db.loadAllTracks();
        set({
          tracks,
          scanProgress: null,
          scanMessage: `Scanned "${result.label}": ${result.added} added, ${result.updated} updated, ${result.failed} unreadable.`,
        });
      } catch (e) {
        if ((e as Error).name === "AbortError") return; // user cancelled the picker
        set({ scanMessage: `Scan failed: ${(e as Error).message}` });
      } finally {
        set({ scanning: false });
      }
    },

    async removeRoot(rootId) {
      const tracks = get().tracks.filter((t) => t.rootId === rootId);
      await db.deleteTracks(tracks.map((t) => t.id));
      const remaining = await db.loadAllTracks();
      set({ tracks: remaining });
      get().notify("Removed library root");
    },

    async playTrack(id, contextIds) {
      const track = get().tracks.find((t) => t.id === id);
      if (!track || !engine) return;
      const queue = contextIds.length ? contextIds : [id];
      const idx = queue.indexOf(id);
      set({ currentTrack: track, queue, queueIndex: idx, currentTime: 0, duration: track.durationSec });
      set((s) => {
        const settings = { ...s.settings, lastTrackId: id };
        persistSettings(settings);
        return { settings };
      });
      try {
        await engine.load({ getAudioSource: () => resolveAudioSource(track) });
        await engine.play();
      } catch (e) {
        set({ error: `Cannot open "${track.fileName}": ${(e as Error).message}`, playing: false });
      }
    },

    async togglePlay() {
      if (!engine) return;
      const { currentTrack, queue } = get();
      if (!currentTrack) {
        const first = get().tracks[0];
        if (first) await get().playTrack(first.id, get().tracks.map((t) => t.id));
        return;
      }
      if (get().playing) {
        engine.pause();
        return;
      }
      try {
        await engine.play();
      } catch {
        set({ error: "Playback failed. Grant folder access and try again." });
      }
    },

    async next() {
      const { queue, queueIndex, settings } = get();
      if (queue.length === 0) return;
      let nextIdx: number;
      if (settings.shuffle) nextIdx = nextShuffle(queue, queueIndex);
      else nextIdx = queueIndex + 1;

      if (nextIdx >= queue.length) {
        if (settings.repeat === "all") nextIdx = 0;
        else {
          engine?.pause();
          set({ playing: false });
          return;
        }
      }
      await get().playTrack(queue[nextIdx], queue);
    },

    async previous() {
      const { queue, queueIndex } = get();
      if (queue.length === 0) return;
      if (engine && engine.currentTime > 3) {
        engine.seekTo(0);
        return;
      }
      const prevIdx = queueIndex > 0 ? queueIndex - 1 : queue.length - 1;
      await get().playTrack(queue[prevIdx], queue);
    },

    seekTo(t) {
      engine?.seekTo(t);
      set({ currentTime: t });
    },

    seekBy(d) {
      engine?.seekBy(d);
      set({ currentTime: engine?.currentTime ?? 0 });
    },

    setVolume(v) {
      const volume = Math.min(1, Math.max(0, Number(v.toFixed(2))));
      engine?.setVolume(volume);
      set((s) => {
        const settings = { ...s.settings, volume, muted: volume === 0 ? s.settings.muted : false };
        persistSettings(settings);
        return { settings };
      });
    },

    toggleMute() {
      engine?.setMuted(!get().settings.muted);
      set((s) => {
        const settings = { ...s.settings, muted: !s.settings.muted };
        persistSettings(settings);
        return { settings };
      });
    },

    toggleShuffle() {
      set((s) => {
        const settings = { ...s.settings, shuffle: !s.settings.shuffle };
        persistSettings(settings);
        return { settings };
      });
    },

    cycleRepeat() {
      const order: RepeatMode[] = ["off", "all", "one"];
      set((s) => {
        const settings = {
          ...s.settings,
          repeat: order[(order.indexOf(s.settings.repeat) + 1) % order.length],
        };
        persistSettings(settings);
        return { settings };
      });
    },

    async onTrackEnded() {
      if (get().settings.repeat === "one") {
        engine?.seekTo(0);
        await engine?.play();
        return;
      }
      await get().next();
    },

    enqueue(ids, playNext = false) {
      set((s) => {
        const queue = [...s.queue];
        const at = playNext ? s.queueIndex + 1 : queue.length;
        for (let i = 0; i < ids.length; i++) queue.splice(at + i, 0, ids[i]);
        return { queue };
      });
      get().notify(playNext ? "Playing next" : "Added to queue");
    },

    removeFromQueue(queuePos) {
      set((s) => {
        const queue = s.queue.filter((_, i) => i !== queuePos);
        return { queue, queueIndex: queuePos < s.queueIndex ? s.queueIndex - 1 : s.queueIndex };
      });
    },

    async jumpToQueuePos(pos) {
      const { queue } = get();
      if (pos < 0 || pos >= queue.length) return;
      await get().playTrack(queue[pos], queue);
    },

    // ---- playlists ----

    createPlaylist(name) {
      const p = newPlaylist(name || "New Playlist");
      set((s) => {
        const playlists = [...s.playlists, p];
        savePlaylists(playlists);
        return { playlists };
      });
    },

    renamePlaylist(id, name) {
      set((s) => {
        const playlists = s.playlists.map((p) => (p.id === id ? { ...p, name } : p));
        savePlaylists(playlists);
        return { playlists };
      });
    },

    deletePlaylist(id) {
      set((s) => {
        const playlists = s.playlists.filter((p) => p.id !== id);
        savePlaylists(playlists);
        return { playlists };
      });
    },

    addToPlaylist(playlistId, trackIds) {
      set((s) => {
        const playlists = s.playlists.map((p) =>
          p.id === playlistId ? { ...p, trackIds: [...p.trackIds, ...trackIds] } : p
        );
        savePlaylists(playlists);
        return { playlists };
      });
      get().notify("Added to playlist");
    },

    removeFromPlaylist(playlistId, trackId) {
      set((s) => {
        const playlists = s.playlists.map((p) =>
          p.id === playlistId ? { ...p, trackIds: p.trackIds.filter((t) => t !== trackId) } : p
        );
        savePlaylists(playlists);
        return { playlists };
      });
    },

    notify(text) {
      const id = Date.now() + Math.random();
      set((s) => ({ toasts: [...s.toasts, { id, text }] }));
      setTimeout(() => get().dismissToast(id), 3500);
    },

    dismissToast(id) {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
    },
  };
});

function hashString(s: string): number {
  let h = 2166136261;
  for (let i = 0; i < s.length; i++) {
    h ^= s.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}
