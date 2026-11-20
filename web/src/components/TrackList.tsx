import { useEffect, useMemo, useState } from "react";
import type { Track } from "../types";
import { useStore } from "../state/playerStore";
import { getCover, getAlbumCover } from "../library/db";

/* ---------------- Cover art ---------------- */

export function CoverArt({ track, size }: { track?: Track | null; size: number }) {
  const [url, setUrl] = useState<string | null>(null);
  const trackId = track?.id;
  const albumKey = track
    ? `${track.rootId}::${track.albumArtist ?? track.artist}::${track.album ?? ""}`
    : null;

  useEffect(() => {
    let active = true;
    let objUrl: string | null = null;
    (async () => {
      if (!trackId || !albumKey) return;
      const blob = (await getCover(trackId)) ?? (await getAlbumCover(albumKey));
      if (!active || !blob || blob.size === 0) return;
      objUrl = URL.createObjectURL(blob);
      setUrl(objUrl);
    })();
    return () => {
      active = false;
      if (objUrl) URL.revokeObjectURL(objUrl);
      setUrl(null);
    };
  }, [trackId, albumKey]);

  return (
    <div className="cover" style={{ width: size, height: size }} aria-hidden="true">
      {url ? (
        <img src={url} alt="" />
      ) : (
        <span className="cover-fallback" style={{ fontSize: size / 3 }}>
          ♪
        </span>
      )}
    </div>
  );
}

/* ---------------- Track list ---------------- */

export function TrackList({
  context,
}: {
  context: { id: string; ids: string[]; title: string };
}) {
  const tracks = useStore((s) => s.tracks);
  const currentTrack = useStore((s) => s.currentTrack);
  const playing = useStore((s) => s.playing);
  const playTrack = useStore((s) => s.playTrack);
  const firstPlaylistId = useStore((s) => (s.playlists.length ? s.playlists[0].id : null));
  const addToPlaylist = useStore((s) => s.addToPlaylist);
  const enqueue = useStore((s) => s.enqueue);
  const [query, setQuery] = useState("");

  const list = useMemo(() => {
    const byId = new Map(tracks.map((t) => [t.id, t]));
    let ordered = context.ids.map((id) => byId.get(id)).filter((t): t is Track => Boolean(t));
    if (context.ids.length === 0) ordered = tracks;
    const q = query.trim().toLowerCase();
    if (!q) return ordered;
    return ordered.filter((t) =>
      `${t.title} ${t.artist} ${t.album ?? ""}`.toLowerCase().includes(q)
    );
  }, [tracks, context, query]);

  if (list.length === 0) {
    return <p className="empty">{query ? "No tracks match your search." : "No tracks here yet."}</p>;
  }

  return (
    <div className="tracklist-wrap">
      <input
        className="search"
        type="search"
        placeholder="Search title, artist, album…"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        aria-label="Search tracks"
      />
      <div className="tracklist" role="list">
        {list.map((t) => {
          const isCurrent = currentTrack?.id === t.id;
          return (
            <div
              key={t.id}
              role="listitem"
              className={`track-row${isCurrent ? " current" : ""}`}
              onDoubleClick={() => void playTrack(t.id, context.ids)}
            >
              <button
                className="track-play"
                title={isCurrent && playing ? "Playing" : "Play"}
                onClick={() => void playTrack(t.id, context.ids)}
              >
                {isCurrent && playing ? "▶" : "▷"}
              </button>
              <div className="track-main">
                <span className="track-title">{t.title}</span>
                <span className="track-artist">
                  {t.artist}
                  {t.album ? ` · ${t.album}` : ""}
                  {t.metadataFailed ? " · (no tags)" : ""}
                </span>
              </div>
              <span className="track-dur">{formatTime(t.durationSec)}</span>
              <div className="track-actions">
                <button title="Play next" onClick={() => enqueue([t.id], true)}>↳</button>
                <button title="Add to queue" onClick={() => enqueue([t.id])}>+</button>
                {firstPlaylistId && (
                  <button
                    title="Add to playlist"
                    onClick={() => addToPlaylist(firstPlaylistId, [t.id])}
                  >
                    ♥
                  </button>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

export function formatTime(sec: number): string {
  if (!Number.isFinite(sec) || sec <= 0) return "--:--";
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = Math.floor(sec % 60);
  if (h > 0) {
    return `${h}:${m.toString().padStart(2, "0")}:${s.toString().padStart(2, "0")}`;
  }
  return `${m}:${s.toString().padStart(2, "0")}`;
}
