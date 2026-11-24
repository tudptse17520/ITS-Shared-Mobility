"use client";

import { ArrowRightIcon } from "@phosphor-icons/react";
import { useEffect, useRef } from "react";

const SESSION_KEY = "its-opening-film-seen";
const START_LIMIT_MS = 4_500;
const PLAYBACK_LIMIT_MS = 13_000;
const EXIT_MS = 260;

export function OpeningFilm() {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const titleRef = useRef<HTMLHeadingElement>(null);
  const progressRef = useRef<HTMLProgressElement>(null);
  const dismissRef = useRef<() => void>(() => {});

  useEffect(() => {
    const dialog = dialogRef.current;
    const video = videoRef.current;
    if (!dialog || !video) return;

    const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)");
    let playbackTimer: number | undefined;
    let closeTimer: number | undefined;

    const close = () => {
      window.clearTimeout(playbackTimer);
      window.clearTimeout(closeTimer);
      if (dialog.open) dialog.close();
      delete dialog.dataset.closing;
      video.pause();
      if (video.hasAttribute("src")) {
        video.removeAttribute("src");
        video.load();
      }
    };

    const dismiss = () => {
      if (!dialog.open || dialog.dataset.closing) return;
      video.pause();
      dialog.dataset.closing = "true";
      closeTimer = window.setTimeout(close, EXIT_MS);
    };
    dismissRef.current = dismiss;

    const onMotionChange = () => {
      if (reducedMotion.matches) close();
    };
    const onPlaybackStarted = () => {
      window.clearTimeout(playbackTimer);
      playbackTimer = window.setTimeout(dismiss, PLAYBACK_LIMIT_MS);
    };
    video.addEventListener("playing", onPlaybackStarted, { once: true });
    reducedMotion.addEventListener("change", onMotionChange);

    const startTimer = window.setTimeout(() => {
      if (reducedMotion.matches) return;
      try {
        if (window.sessionStorage.getItem(SESSION_KEY)) return;
        window.sessionStorage.setItem(SESSION_KEY, "1");
      } catch {
        // Playback still works when the browser blocks session storage.
      }
      video.muted = true;
      video.src = window.matchMedia("(max-aspect-ratio: 4/5)").matches
        ? "/intro/its-opening-portrait.mp4"
        : "/intro/its-opening-landscape.mp4";
      dialog.showModal();
      titleRef.current?.focus({ preventScroll: true });
      playbackTimer = window.setTimeout(dismiss, START_LIMIT_MS);
      void video.play().catch(dismiss);
    }, 0);

    return () => {
      window.clearTimeout(startTimer);
      video.removeEventListener("playing", onPlaybackStarted);
      reducedMotion.removeEventListener("change", onMotionChange);
      dismissRef.current = () => {};
      close();
    };
  }, []);

  return (
    <dialog
      ref={dialogRef}
      className="opening-film"
      aria-labelledby="opening-film-title"
      onCancel={(event) => {
        event.preventDefault();
        dismissRef.current();
      }}
    >
      <h2 id="opening-film-title" ref={titleRef} className="sr-only" tabIndex={-1}>
        Giới thiệu ITS Mobility
      </h2>
      <video
        ref={videoRef}
        className="opening-film-video"
        muted
        playsInline
        aria-hidden="true"
        onError={() => dismissRef.current()}
        onEnded={() => dismissRef.current()}
        onTimeUpdate={(event) => {
          const { currentTime, duration } = event.currentTarget;
          if (progressRef.current && Number.isFinite(duration) && duration > 0) {
            progressRef.current.value = currentTime / duration;
          }
        }}
      />
      <button type="button" className="opening-film-skip" onClick={() => dismissRef.current()}>
        <span className="opening-film-skip-label">
          Bỏ qua <ArrowRightIcon size={14} aria-hidden="true" />
        </span>
      </button>
      <progress ref={progressRef} className="opening-film-progress" value={0} max={1} aria-hidden="true" />
    </dialog>
  );
}
