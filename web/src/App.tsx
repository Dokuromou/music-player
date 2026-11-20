import { useEffect, useState } from "react";
import type { LibraryView } from "./types";
import { initEngine, useStore } from "./state/playerStore";
import { useKeyboardShortcuts } from "./hooks/useKeyboardShortcuts";
import { Sidebar, MainPanel, BrowseGrid, ALL, type Selection } from "./components/Sidebar";
import { PlayerBar, CurrentTrack } from "./components/PlayerBar";
import { Queue } from "./components/Queue";

export default function App() {
  const [view, setView] = useState<LibraryView>("albums");
  const [selection, setSelection] = useState<Selection>(ALL);
  const libraryLoaded = useStore((s) => s.libraryLoaded);
  const scanMessage = useStore((s) => s.scanMessage);
  const scanning = useStore((s) => s.scanning);
  const scanProgress = useStore((s) => s.scanProgress);
  const toasts = useStore((s) => s.toasts);
  const error = useStore((s) => s.error);
  const loadLibrary = useStore((s) => s.loadLibrary);
  const addDirectory = useStore((s) => s.addDirectory);

  useKeyboardShortcuts();

  useEffect(() => {
    initEngine();
    void loadLibrary();
  }, [loadLibrary]);

  const selecting = selection.kind !== "all";

  return (
    <div className="app">
      <header className="topbar">
        <h1>Localtune</h1>
        <span className="tagline">Local music, private by design</span>
        <nav className="view-tabs">
          {(["albums", "artists", "folders", "playlists"] as LibraryView[]).map((v) => (
            <button
              key={v}
              className={view === v ? "active" : ""}
              onClick={() => {
                setView(v);
                setSelection(ALL);
              }}
            >
              {v[0].toUpperCase() + v.slice(1)}
            </button>
          ))}
          <button
            className={selecting ? "active" : ""}
            onClick={() => setSelection(ALL)}
          >
            All tracks
          </button>
        </nav>
      </header>

      <div className="body">
        <Sidebar view={view} setView={setView} onAddDirectory={() => void addDirectory()} scanning={scanning} />

        <main className="main">
          {scanning && scanProgress && scanProgress.total > 0 && (
            <div className="banner scan-banner">
              Reading {scanProgress.scanned + 1} / {scanProgress.total} · {scanProgress.currentFile}
            </div>
          )}
          {error && (
            <div className="banner error-banner" role="alert">
              {error}
            </div>
          )}
          {scanMessage && <div className="banner">{scanMessage}</div>}

          <div className="panel-grid">
            <section className="library-panel">
              {selecting ? (
                <div>
                  <button className="btn small" onClick={() => setSelection(ALL)}>
                    ← Back
                  </button>
                  <MainPanel selection={selection} />
                </div>
              ) : (
                <BrowseGrid
                  view={view}
                  onOpenPlaylist={(s) => setSelection(s)}
                  onNewPlaylist={() => {
                    const name = window.prompt("Playlist name");
                    if (name !== null) {
                      useStore.getState().createPlaylist(name);
                      setView("playlists");
                    }
                  }}
                />
              )}
            </section>
            <Queue />
          </div>
        </main>

        <CurrentTrack />
      </div>

      <PlayerBar />

      <div className="toasts" aria-live="polite">
        {toasts.map((t) => (
          <div key={t.id} className="toast">
            {t.text}
          </div>
        ))}
      </div>

      {!libraryLoaded && <div className="loading-overlay">Loading library…</div>}
    </div>
  );
}
