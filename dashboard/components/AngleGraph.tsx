"use client";

import { useRef, useEffect } from "react";
import { PoseFrame } from "@/lib/types";

interface Props {
  frames: PoseFrame[];
  currentTimestampMs: number;
}

export default function AngleGraph({ frames, currentTimestampMs }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || frames.length === 0) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const W = canvas.width;
    const H = canvas.height;
    ctx.clearRect(0, 0, W, H);

    const maxTs = frames[frames.length - 1].timestamp_ms;
    const minTs = frames[0].timestamp_ms;
    const range = maxTs - minTs || 1;

    // Grid
    ctx.strokeStyle = "#333";
    ctx.lineWidth = 1;
    for (const angle of [0, 45, 90, 135, 180]) {
      const y = H - (angle / 180) * H;
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(W, y); ctx.stroke();
      ctx.fillStyle = "#555"; ctx.font = "10px monospace"; ctx.fillText(`${angle}\u00B0`, 2, y - 2);
    }

    // Threshold lines
    ctx.strokeStyle = "#ff440066";
    ctx.setLineDash([4, 4]);
    ctx.beginPath(); ctx.moveTo(0, H - (80 / 180) * H); ctx.lineTo(W, H - (80 / 180) * H); ctx.stroke();
    ctx.beginPath(); ctx.moveTo(0, H - (120 / 180) * H); ctx.lineTo(W, H - (120 / 180) * H); ctx.stroke();
    ctx.setLineDash([]);

    // Left elbow
    ctx.strokeStyle = "#00ff88"; ctx.lineWidth = 2; ctx.beginPath();
    for (let i = 0; i < frames.length; i++) {
      const x = ((frames[i].timestamp_ms - minTs) / range) * W;
      const y = H - ((frames[i].left_elbow_angle ?? 0) / 180) * H;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Right elbow
    ctx.strokeStyle = "#4488ff"; ctx.lineWidth = 2; ctx.beginPath();
    for (let i = 0; i < frames.length; i++) {
      const x = ((frames[i].timestamp_ms - minTs) / range) * W;
      const y = H - ((frames[i].right_elbow_angle ?? 0) / 180) * H;
      if (i === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Current time
    const curX = ((currentTimestampMs - minTs) / range) * W;
    ctx.strokeStyle = "#fff"; ctx.lineWidth = 2;
    ctx.beginPath(); ctx.moveTo(curX, 0); ctx.lineTo(curX, H); ctx.stroke();

    // Legend
    ctx.font = "11px monospace";
    ctx.fillStyle = "#00ff88"; ctx.fillText("L elbow", W - 70, 14);
    ctx.fillStyle = "#4488ff"; ctx.fillText("R elbow", W - 70, 28);
  }, [frames, currentTimestampMs]);

  return <canvas ref={canvasRef} width={600} height={150} className="w-full h-36 bg-gray-900 rounded-lg border border-gray-800" />;
}
