import { memo, RefObject, useCallback, useEffect, useRef } from "react";
import { usePoseDetection } from "../../hooks/use-pose-detection";
import {
  RepCountingEngine,
  ExerciseConfig,
  PersonState,
} from "../../lib/rep-counting-engine";
import "./rep-counter.scss";

// MediaPipe pose landmark connections for drawing skeleton
const POSE_CONNECTIONS: [number, number][] = [
  [11, 12], [11, 13], [13, 15], [12, 14], [14, 16],
  [11, 23], [12, 24], [23, 24], [23, 25], [24, 26],
  [25, 27], [26, 28],
];

interface RepCounterProps {
  videoRef: RefObject<HTMLVideoElement>;
  exerciseConfig: ExerciseConfig | null;
  active: boolean;
  onRepUpdate: (states: PersonState[]) => void;
}

function RepCounterComponent({
  videoRef,
  exerciseConfig,
  active,
  onRepUpdate,
}: RepCounterProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const engineRef = useRef(new RepCountingEngine());
  // Always keep pose detection running so the model stays warm
  const { landmarks, isLoading } = usePoseDetection(videoRef, active);

  // Process landmarks through rep counting engine
  useEffect(() => {
    if (!active || !exerciseConfig || landmarks.length === 0) return;

    const states = engineRef.current.processFrame(landmarks, exerciseConfig);
    onRepUpdate(states);
  }, [active, exerciseConfig, landmarks, onRepUpdate]);

  // Reset engine when exercise changes or when reactivated
  useEffect(() => {
    if (active && exerciseConfig) {
      engineRef.current.reset();
    }
  }, [active, exerciseConfig]);

  // Clear canvas when deactivated
  useEffect(() => {
    if (!active && canvasRef.current) {
      const ctx = canvasRef.current.getContext("2d");
      if (ctx) ctx.clearRect(0, 0, canvasRef.current.width, canvasRef.current.height);
    }
  }, [active]);

  // Draw pose overlay
  const drawOverlay = useCallback(() => {
    const canvas = canvasRef.current;
    const video = videoRef.current;
    if (!canvas || !video || !active || landmarks.length === 0) return;

    canvas.width = video.videoWidth || video.clientWidth;
    canvas.height = video.videoHeight || video.clientHeight;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const w = canvas.width;
    const h = canvas.height;

    for (let personIdx = 0; personIdx < landmarks.length; personIdx++) {
      const person = landmarks[personIdx];
      if (!person) continue;

      // Draw skeleton connections
      ctx.strokeStyle = "#00ff88";
      ctx.lineWidth = 3;
      for (const [a, b] of POSE_CONNECTIONS) {
        if (person[a] && person[b]) {
          ctx.beginPath();
          ctx.moveTo(person[a].x * w, person[a].y * h);
          ctx.lineTo(person[b].x * w, person[b].y * h);
          ctx.stroke();
        }
      }

      // Draw live angle for EACH arm so user can see what's detected
      if (exerciseConfig) {
        const armLabels = ["L", "R"];
        for (let setIdx = 0; setIdx < exerciseConfig.keypointSets.length; setIdx++) {
          const [kpA, kpB, kpC] = exerciseConfig.keypointSets[setIdx];

          // Highlight the 3 keypoints
          ctx.fillStyle = "#ff4400";
          for (const kp of [kpA, kpB, kpC]) {
            if (person[kp]) {
              ctx.beginPath();
              ctx.arc(person[kp].x * w, person[kp].y * h, 8, 0, Math.PI * 2);
              ctx.fill();
            }
          }

          // Draw connection lines for this arm (shoulder-elbow-wrist)
          ctx.strokeStyle = "#ff4400";
          ctx.lineWidth = 4;
          if (person[kpA] && person[kpB]) {
            ctx.beginPath();
            ctx.moveTo(person[kpA].x * w, person[kpA].y * h);
            ctx.lineTo(person[kpB].x * w, person[kpB].y * h);
            ctx.stroke();
          }
          if (person[kpB] && person[kpC]) {
            ctx.beginPath();
            ctx.moveTo(person[kpB].x * w, person[kpB].y * h);
            ctx.lineTo(person[kpC].x * w, person[kpC].y * h);
            ctx.stroke();
          }

          // Show live angle at the elbow for this arm
          if (person[kpA] && person[kpB] && person[kpC]) {
            const armAngle = engineRef.current.calculateAngle(
              person[kpA],
              person[kpB],
              person[kpC]
            );
            ctx.fillStyle = "#ffff00";
            ctx.font = "bold 16px monospace";
            ctx.fillText(
              `${armLabels[setIdx]}: ${Math.round(armAngle)}°`,
              person[kpB].x * w + 15,
              person[kpB].y * h - 10
            );
          }
        }
      }

      // Draw landmark dots
      ctx.fillStyle = "#00ff88";
      for (const lm of person) {
        if (lm) {
          ctx.beginPath();
          ctx.arc(lm.x * w, lm.y * h, 4, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    }

    // Draw rep count HUD
    const states = engineRef.current.getStates();
    if (states.length > 0 && exerciseConfig) {
      const s = states[0];
      const boxH = 100;
      ctx.fillStyle = "rgba(0, 0, 0, 0.8)";
      ctx.roundRect(10, 10, 260, boxH, 12);
      ctx.fill();

      // Rep count
      ctx.fillStyle = "#00ff88";
      ctx.font = "bold 40px monospace";
      ctx.fillText(`REPS: ${s.repCount}`, 20, 52);

      // Current angle + active arm
      ctx.fillStyle = "#ffff00";
      ctx.font = "bold 16px monospace";
      ctx.fillText(
        `Angle: ${Math.round(s.currentAngle)}° (${s.activeArm || "?"})`,
        20,
        74
      );

      // Stage + threshold hints
      const stageColor = s.stage === "down" ? "#ff4400" : s.stage === "up" ? "#00ff88" : "#888888";
      ctx.fillStyle = stageColor;
      ctx.font = "14px monospace";
      ctx.fillText(
        `${(s.stage || "wait").toUpperCase()} | curl<${exerciseConfig.downAngle}° extend>${exerciseConfig.upAngle}°`,
        20,
        96
      );
    }
  }, [active, landmarks, exerciseConfig, videoRef]);

  useEffect(() => {
    drawOverlay();
  }, [drawOverlay]);

  // Always render — keep hooks alive, only show overlay when active
  return (
    <div className="rep-counter-overlay" style={{ display: active ? "block" : "none" }}>
      {isLoading && <div className="loading-indicator">Loading pose model...</div>}
      <canvas ref={canvasRef} className="pose-canvas" />
    </div>
  );
}

export const RepCounter = memo(RepCounterComponent);
