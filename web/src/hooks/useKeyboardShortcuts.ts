import { useEffect } from "react";
import { useStore } from "../state/playerStore";

/**
 * Global keyboard shortcuts:
 *  Space        play/pause
 *  ← / →        seek -5s / +5s
 *  ↑ / ↓        volume -/+ 5%
 *  N / P        next / previous
 *  M            mute
 *  R            cycle repeat
 *  S            shuffle
 * Ignored while typing in an input.
 */
export function useKeyboardShortcuts(): void {
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      const target = e.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === "INPUT" || target.tagName === "TEXTAREA" || target.isContentEditable)
      ) {
        return;
      }
      const s = useStore.getState();
      switch (e.key) {
        case " ":
          e.preventDefault();
          void s.togglePlay();
          break;
        case "ArrowRight":
          e.preventDefault();
          s.seekBy(5);
          break;
        case "ArrowLeft":
          e.preventDefault();
          s.seekBy(-5);
          break;
        case "ArrowUp":
          e.preventDefault();
          s.setVolume(s.settings.volume + 0.05);
          break;
        case "ArrowDown":
          e.preventDefault();
          s.setVolume(s.settings.volume - 0.05);
          break;
        case "n":
        case "N":
          void s.next();
          break;
        case "p":
        case "P":
          void s.previous();
          break;
        case "m":
        case "M":
          s.toggleMute();
          break;
        case "r":
        case "R":
          s.cycleRepeat();
          break;
        case "s":
        case "S":
          s.toggleShuffle();
          break;
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
}
