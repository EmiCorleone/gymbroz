"use client";

import { useRef, useEffect, useCallback, useState } from "react";
import { PoseFrame } from "@/lib/types";
import { drawPoseSkeleton } from "@/lib/pose-renderer";

interface Props {
  videoUrl: string;
  frames: PoseFrame[];
  onFrameChange?: (frame: PoseFrame | null) => void;
}

export default function PoseOverlayPlayer({ videoUrl, frames, onFrameChange }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(0);
  const animRef = useRef<number>(0);

  const findClosestFrame = useCallback(
    (timeMs: number): PoseFrame | null => {
      if (frames.length === 0) return null;
      let closest = frames[0];
      let minDiff = Math.abs(frames[0].timestamp_ms - timeMs);
      for (const f of frames) {
        const diff = Math.abs(f.timestamp_ms - timeMs);
        if (diff < minDiff) {
          minDiff = diff;
          closest = f;
        }
      }
      return minDiff < 200 ? closest : null;
    },
    [frames]
  );

  const renderOverlay = useCallback(() => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;

    canvas.width = video.videoWidth || video.clientWidth;
    canvas.height = video.videoHeight || video.clientHeight;

    const timeMs = video.currentTime * 1000;
    const frame = findClosestFrame(timeMs);
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    if (frame) {
      drawPoseSkeleton(ctx, frame.landmarks, canvas.width, canvas.height);
    } else {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    onFrameChange?.(frame);
    setCurrentTime(video.currentTime);

    if (!video.paused) {
      animRef.current = requestAnimationFrame(renderOverlay);
    }
  }, [findClosestFrame, onFrameChange]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const onPlay = () => { setIsPlaying(true); animRef.current = requestAnimationFrame(renderOverlay); };
    const onPause = () => { setIsPlaying(false); cancelAnimationFrame(animRef.current); renderOverlay(); };
    const onSeeked = () => renderOverlay();
    const onLoaded = () => setDuration(video.duration);

    video.addEventListener("play", onPlay);
    video.addEventListener("pause", onPause);
    video.addEventListener("seeked", onSeeked);
    video.addEventListener("loadedmetadata", onLoaded);

    return () => {
      video.removeEventListener("play", onPlay);
      video.removeEventListener("pause", onPause);
      video.removeEventListener("seeked", onSeeked);
      video.removeEventListener("loadedmetadata", onLoaded);
      cancelAnimationFrame(animRef.current);
    };
  }, [renderOverlay]);

  const togglePlay = () => {
    const video = videoRef.current;
    if (!video) return;
    if (video.paused) video.play(); else video.pause();
  };

  const seek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const video = videoRef.current;
    if (!video) return;
    video.currentTime = parseFloat(e.target.value);
  };

  const formatTime = (s: number) => {
    const m = Math.floor(s / 60);
    const sec = Math.floor(s % 60);
    return `${m}:${sec.toString().padStart(2, "0")}`;
  };

  return (
    <div>
      <div className="relative aspect-video bg-black rounded-lg overflow-hidden">
        <video ref={videoRef} src={videoUrl} className="w-full h-full object-contain" preload="auto" crossOrigin="anonymous" />
        <canvas ref={canvasRef} className="absolute inset-0 w-full h-full pointer-events-none" />
      </div>
      <div className="flex items-center gap-3 mt-3">
        <button onClick={togglePlay} className="px-3 py-1 bg-gray-800 hover:bg-gray-700 rounded text-sm font-mono">
          {isPlaying ? "\u23F8" : "\u25B6"}
        </button>
        <input type="range" min={0} max={duration || 0} step={0.033} value={currentTime} onChange={seek} className="flex-1 accent-green-500" />
        <span className="text-xs text-gray-400 font-mono w-20 text-right">
          {formatTime(currentTime)} / {formatTime(duration)}
        </span>
      </div>
    </div>
  );
}
