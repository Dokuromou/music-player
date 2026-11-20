import { useMemo } from "react";
import type { LibraryView, Playlist, Track } from "../types";
import { useStore } from "../state/playerStore";
import { TrackList } from "./TrackList";

export interface Selection {
  kind: "album" | "artist" | "folder" | "playlist" | "all";
  /** artist name, album key, rootId or playlist id */
  key: string;
}

export const ALL: Selection = { kind: "all", key: "" };

export function Sidebar({
  view,
  setView,
  onAddDirectory,
  scanning,
}: {
  view: LibraryView;
  setView: (v: LibraryView) => void;
  onAddDirectory: () => void;
  scanning: boolean;
}) {
  const tracks = useStore((s) => s.tracks);
  const playlists = useStore((s) => s.playlists);

  const counts = useMemo(() => {
    const artists = new Set(tracks.map((t) => t.artist));
    const albums = new Set(tracks.map((t) => albumKeyOf(t)));
    const folders = new Set(tracks.map((t) => t.rootId));
    return { artists: artists.size, albums: albums.size, folders: folders.size };
  }, [tracks]);

  return (
    <nav className="sidebar">
      <div className="lib-controls">
        <button className="btn primary" disabled={scanning} onClick={onAddDirectory}>
          {scanning ? "Scanning…" : "＋ Add music folder"}
        </button>
      </div>

      <ul className="nav-list">
        {(
          [
            ["artists", "Artists"],
            ["albums", "Albums"],
            ["folders", "Folders"],
          ] as LibraryView[]
        ).map(([v, label]) => (
          <li key={v}>
            <button className={view === v ? "active" : ""} onClick={() => setView(v)}>
              {label} <span className="count">{counts[v as "artists" | "albums" | "folders"]}</span>
            </button>
          </li>
        ))}
        <li>
          <button className={view === "playlists" ? "active" : ""} onClick={() => setView("playlists")}>
            Playlists <span className="count">{playlists.length}</span>
          </button>
        </li>
      </ul>
    </nav>
  );
}

export function albumKeyOf(t: Track): string {
  return `${t.albumArtist ?? t.artist}::${t.album ?? ""}`;
}

/* ---------------- Main browsing panel ---------------- */

export function MainPanel({ selection }: { selection: Selection }) {
  const tracks = useStore((s) => s.tracks);
  const playlists = useStore((s) => s.playlists);

  if (selection.kind === "playlist") {
    const pl = playlists.find((p) => p.id === selection.key);
    if (!pl) return <p className="empty">Playlist not found.</p>;
    return <PlaylistView playlist={pl} />;
  }

  let ids: string[] = [];
  let title = tracks.length ? "All tracks" : "Library";

  if (selection.kind === "album") {
    const [aa, al] = splitAlbumKey(selection.key);
    ids = tracks
      .filter((t) => albumKeyOf(t) === selection.key)
      .sort(
        (a, b) => (a.discNo ?? 1) - (b.discNo ?? 1) || (a.trackNo ?? 0) - (b.trackNo ?? 0)
      )
      .map((t) => t.id);
    void aa;
    title = al || "Untitled album";
  } else if (selection.kind === "artist") {
    ids = tracks.filter((t) => t.artist === selection.key).map((t) => t.id);
    title = selection.key;
  } else if (selection.kind === "folder") {
    ids = tracks.filter((t) => t.rootId === selection.key).map((t) => t.id);
    title =
      tracks.find((t) => t.rootId === selection.key)?.rootLabel ?? "Folder";
  }

  return <TrackList context={{ id: selection.kind + ":" + selection.key, ids, title }} />;
}

/** album key format: "albumArtist::album" */
export function splitAlbumKey(key: string): [string, string] {
  const i = key.indexOf("::");
  return i === -1 ? [key, ""] : [key.slice(0, i), key.slice(i + 2)];
}

/* ---------------- Browse grid ---------------- */

export function BrowseGrid({
  view,
  onOpenPlaylist,
  onNewPlaylist,
}: {
  view: LibraryView;
  onOpenPlaylist: (s: Selection) => void;
  onNewPlaylist: () => void;
}) {
  const tracks = useStore((s) => s.tracks);
  const playlists = useStore((s) => s.playlists);

  const groups = useMemo(() => {
    if (view === "artists") {
      const m = new Map<string, { id: string; title: string; count: number }>();
      for (const t of tracks) {
        const g = m.get(t.artist) ?? { id: t.artist, title: t.artist, count: 0 };
        g.count++;
        m.set(t.artist, g);
      }
      return [...m.values()];
    }
    if (view === "albums") {
      const m = new Map<string, { id: string; title: string; count: number }>();
      for (const t of tracks) {
        const key = albumKeyOf(t);
        const g =
          m.get(key) ??
          { id: key, title: t.album ?? "Untitled album", count: 0 };
        g.count++;
        m.set(key, g);
      }
      return [...m.values()];
    }
    const m = new Map<string, { id: string; title: string; count: number }>();
    for (const t of tracks) {
      const g = m.get(t.rootId) ?? { id: t.rootId, title: t.rootLabel, count: 0 };
      g.count++;
      m.set(t.rootId, g);
    }
    return [...m.values()];
  }, [tracks, view]);

  return (
    <div className="grid">
      {groups.map((g) =>
        view === "albums" ? (
          <AlbumCard key={g.id} albumKey={g.id} title={g.title} count={g.count} onSelect={onOpenPlaylist} />
        ) : (
          <button
            key={g.id}
            className="card"
            onClick={() =>
              onOpenPlaylist({
                kind: view === "artists" ? "artist" : "folder",
                key: g.id,
              })
            }
          >
            <div className="card-art">{view === "artists" ? "🎤" : "📁"}</div>
            <div className="card-title">{g.title}</div>
            <div className="card-sub">{g.count} tracks</div>
          </button>
        )
      )}
      {view === "playlists" && (
        <>
          <button className="card" onClick={onNewPlaylist}>
            <div className="card-art">＋</div>
            <div className="card-title">New playlist</div>
          </button>
          {playlists.map((p) => (
            <button
              key={p.id}
              className="card"
              onClick={() => onOpenPlaylist({ kind: "playlist", key: p.id })}
            >
              <div className="card-art">☰</div>
              <div className="card-title">{p.name}</div>
              <div className="card-sub">{p.trackIds.length} tracks</div>
            </button>
          ))}
        </>
      )}
      {groups.length === 0 && view !== "playlists" && (
        <p className="empty">Nothing here yet. Add a music folder.</p>
      )}
    </div>
  );
}

function AlbumCard({
  albumKey,
  title,
  count,
  onSelect,
}: {
  albumKey: string;
  title: string;
  count: number;
  onSelect: (s: Selection) => void;
}) {
  const tracks = useStore((s) => s.tracks);
  const first = useMemo(
    () => tracks.find((t) => albumKeyOf(t) === albumKey) ?? null,
    [tracks, albumKey]
  );
  return (
    <button className="card" onClick={() => onSelect({ kind: "album", key: albumKey })}>
      <div className="card-art card-art-cover">
        <CoverMini track={first} />
      </div>
      <div className="card-title">{title}</div>
      <div className="card-sub">{count} tracks</div>
    </button>
  );
}

import { CoverArt } from "./TrackList";

function CoverMini({ track }: { track: Track | null }) {
  return <CoverArt track={track} size={999} />;
}

/* ---------------- Playlist view ---------------- */

export function PlaylistView({ playlist }: { playlist: Playlist }) {
  const deletePlaylist = useStore((s) => s.deletePlaylist);
  const removeFromPlaylist = useStore((s) => s.removeFromPlaylist);

  return (
    <div>
      <h2 className="panel-title">
        {playlist.name}
        <button
          className="btn small danger"
          style={{ marginLeft: 12 }}
          onClick={() => {
            if (window.confirm(`Delete playlist "${playlist.name}"?`)) {
              deletePlaylist(playlist.id);
            }
          }}
        >
          Delete
        </button>
      </h2>
      <TrackList context={{ id: playlist.id, ids: playlist.trackIds, title: playlist.name }} />
      {playlist.trackIds.length > 0 && (
        <button
          className="btn small"
          style={{ marginTop: 8 }}
          onClick={() =>
            removeFromPlaylist(playlist.id, playlist.trackIds[playlist.trackIds.length - 1])
          }
        >
          Remove last
        </button>
      )}
    </div>
  );
}
