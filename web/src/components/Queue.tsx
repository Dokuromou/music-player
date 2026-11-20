import { useStore } from "../state/playerStore";
import { formatTime } from "./TrackList";

export function Queue() {
  const queue = useStore((s) => s.queue);
  const tracks = useStore((s) => s.tracks);
  const queueIndex = useStore((s) => s.queueIndex);
  const removeFromQueue = useStore((s) => s.removeFromQueue);
  const jumpToQueuePos = useStore((s) => s.jumpToQueuePos);

  const byId = new Map(tracks.map((t) => [t.id, t]));

  if (queue.length === 0) {
    return (
      <section className="queue">
        <h3>Queue</h3>
        <p className="empty">Queue is empty. Add tracks with the + button.</p>
      </section>
    );
  }

  return (
    <section className="queue">
      <h3>
        Queue <span className="count">{queue.length}</span>
      </h3>
      <ol>
        {queue.map((id, i) => {
          const t = byId.get(id);
          if (!t) return null;
          return (
            <li
              key={`${id}-${i}`}
              className={`queue-row${i === queueIndex ? " now" : ""}`}
              onClick={() => void jumpToQueuePos(i)}
            >
              <span className="q-pos">{i + 1}</span>
              <span className="q-main">
                <span className="q-title">{t.title}</span>
                <span className="q-artist">{t.artist}</span>
              </span>
              <span className="q-dur">{formatTime(t.durationSec)}</span>
              <button
                className="q-remove"
                title="Remove from queue"
                onClick={(e) => {
                  e.stopPropagation();
                  removeFromQueue(i);
                }}
              >
                ✕
              </button>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
