import { useState } from "react";
import { useStore } from "../state/playerStore";
import { formatTime, CoverArt } from "./TrackList";

export function PlayerBar() {
  const currentTrack = useStore((s) => s.currentTrack);
  const playing = useStore((s) => s.playing);
  const currentTime = useStore((s) => s.currentTime);
  const duration = useStore((s) => s.duration);
  const settings = useStore((s) => s.settings);
  const togglePlay = useStore((s) => s.togglePlay);
  const next = useStore((s) => s.next);
  const previous = useStore((s) => s.previous);
  const seekTo = useStore((s) => s.seekTo);
  const setVolume = useStore((s) => s.setVolume);
  const toggleMute = useStore((s) => s.toggleMute);
  const toggleShuffle = useStore((s) => s.toggleShuffle);
  const cycleRepeat = useStore((s) => s.cycleRepeat);

  const [scrubbing, setScrubbing] = useState(false);
  const [scrubValue, setScrubValue] = useState(0);

  const total = duration || currentTrack?.durationSec || 0;
  const pos = scrubbing ? scrubValue : currentTime;
  const pct = total > 0 ? (pos / total) * 100 : 0;
  const step = Math.max(total / 1000, 0.05);

  const commitScrub = () => {
    if (scrubbing) {
      seekTo(scrubValue);
      setScrubbing(false);
    }
  };

  return (
    <footer className="player-bar">
      <div className="pb-track">
        <CoverArt track={currentTrack} size={52} />
        <div className="pb-meta">
          <div className="pb-title" title={currentTrack?.title ?? ""}>
            {currentTrack ? currentTrack.title : "Nothing playing"}
          </div>
          <div className="pb-sub">
            {currentTrack
              ? `${currentTrack.artist}${currentTrack.album ? " · " + currentTrack.album : ""}`
              : "Add a folder to get started"}
          </div>
        </div>
      </div>

      <div className="pb-center">
        <div className="pb-buttons">
          <button
            className={`icon-btn${settings.shuffle ? " on" : ""}`}
            title="Shuffle (S)"
            aria-pressed={settings.shuffle}
            onClick={toggleShuffle}
          >
            ⤨
          </button>
          <button className="icon-btn" title="Previous (P)" onClick={() => void previous()}>⏮</button>
          <button className="icon-btn big" title="Play/Pause (Space)" onClick={() => void togglePlay()}>
            {playing ? "⏸" : "▶"}
          </button>
          <button className="icon-btn" title="Next (N)" onClick={() => void next()}>⏭</button>
          <button
            className={`icon-btn${settings.repeat !== "off" ? " on" : ""}`}
            title={`Repeat: ${settings.repeat} (R)`}
            onClick={cycleRepeat}
          >
            {settings.repeat === "one" ? "🔂" : "🔁"}
          </button>
        </div>
        <div className="seek-row">
          <span className="time">{formatTime(pos)}</span>
          <input
            type="range"
            min={0}
            max={Math.max(total, step)}
            step={step}
            value={pos}
            onChange={(e) => {
              setScrubbing(true);
              setScrubValue(Number(e.target.value));
            }}
            onMouseUp={commitScrub}
            onTouchEnd={commitScrub}
            onKeyUp={commitScrub}
            style={{ backgroundSize: `${pct}% 100%` }}
            aria-label="Seek"
          />
          <span className="time">{formatTime(total)}</span>
        </div>
      </div>

      <div className="pb-volume">
        <button className="icon-btn" title="Mute (M)" onClick={toggleMute}>
          {settings.muted || settings.volume === 0 ? "🔇" : settings.volume < 0.5 ? "🔉" : "🔊"}
        </button>
        <input
          type="range"
          min={0}
          max={1}
          step={0.01}
          value={settings.muted ? 0 : settings.volume}
          onChange={(e) => setVolume(Number(e.target.value))}
          aria-label="Volume"
        />
      </div>
    </footer>
  );
}

/** Big "now playing" detail panel. */
export function CurrentTrack() {
  const track = useStore((s) => s.currentTrack);
  const currentTime = useStore((s) => s.currentTime);
  const playing = useStore((s) => s.playing);

  return (
    <aside className="now-playing">
      <CoverArt track={track} size={220} />
      <div className="np-title">{track?.title ?? "Nothing playing"}</div>
      <div className="np-artist">{track?.artist ?? ""}</div>
      <div className="np-album">{track?.album ?? ""}</div>
      <div className="np-status">
        {playing ? "Playing" : "Paused"}
        {track ? ` · ${formatTime(currentTime)} / ${formatTime(track.durationSec)}` : ""}
      </div>
      {track?.metadataFailed && (
        <p className="np-warn">Tags could not be read; showing file name instead.</p>
      )}
    </aside>
  );
}
