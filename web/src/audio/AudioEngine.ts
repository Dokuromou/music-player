/**
 * AudioEngine: thin wrapper around a single <audio> element.
 * It is the only module allowed to touch playback APIs; the store and UI
 * talk to it through a PlayHandle (a Track plus a way to get the file).
 */

export interface PlayHandle {
  /** Resolve a playable object URL / blob for the given track. */
  getAudioSource(): Promise<string>;
}

export interface AudioEngineEvents {
  onEnded: () => void;
  onTimeUpdate: (currentTime: number) => void;
  onDurationChange: (duration: number) => void;
  onPlaying: () => void;
  onPause: () => void;
  onError: (message: string) => void;
  onLoading: () => void;
}

export class AudioEngine {
  private audio: HTMLAudioElement;
  private events: AudioEngineEvents;
  private currentUrl: string | null = null;

  constructor(events: AudioEngineEvents) {
    this.audio = new Audio();
    this.audio.preload = "metadata";
    this.events = events;

    this.audio.addEventListener("ended", this.events.onEnded);
    this.audio.addEventListener("timeupdate", () => {
      this.events.onTimeUpdate(this.audio.currentTime);
    });
    this.audio.addEventListener("durationchange", () => {
      this.events.onDurationChange(this.audio.duration);
    });
    this.audio.addEventListener("playing", this.events.onPlaying);
    this.audio.addEventListener("pause", this.events.onPause);
    this.audio.addEventListener("loadstart", this.events.onLoading);
    this.audio.addEventListener("error", () => {
      const code = this.audio.error?.code;
      const meaning =
        code === 1
          ? "loading aborted"
          : code === 2
            ? "network error"
            : code === 3
              ? "decoding failed"
              : code === 4
                ? "format not supported or source missing"
                : "unknown playback error";
      this.events.onError(meaning);
    });
  }

  async load(handle: PlayHandle): Promise<void> {
    this.audio.pause();
    if (this.currentUrl) URL.revokeObjectURL(this.currentUrl);
    const url = await handle.getAudioSource();
    this.currentUrl = url;
    this.audio.src = url;
    this.audio.load();
  }

  async play(): Promise<void> {
    await this.audio.play();
  }

  pause(): void {
    this.audio.pause();
  }

  seekTo(seconds: number): void {
    if (!Number.isFinite(seconds)) return;
    const d = this.audio.duration;
    this.audio.currentTime = Number.isFinite(d)
      ? Math.min(Math.max(seconds, 0), d)
      : Math.max(seconds, 0);
  }

  seekBy(deltaSeconds: number): void {
    this.seekTo(this.audio.currentTime + deltaSeconds);
  }

  get currentTime(): number {
    return this.audio.currentTime;
  }

  get duration(): number {
    return Number.isFinite(this.audio.duration) ? this.audio.duration : 0;
  }

  setVolume(v: number): void {
    this.audio.volume = Math.min(1, Math.max(0, v));
  }

  setMuted(m: boolean): void {
    this.audio.muted = m;
  }
}
