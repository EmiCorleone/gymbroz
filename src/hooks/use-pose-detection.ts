import { RefObject, useEffect, useRef, useState } from "react";
import {
  PoseLandmarker,
  FilesetResolver,
  NormalizedLandmark,
} from "@mediapipe/tasks-vision";

export function usePoseDetection(
  videoRef: RefObject<HTMLVideoElement>,
  enabled: boolean
) {
  const [landmarks, setLandmarks] = useState<NormalizedLandmark[][]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [modelReady, setModelReady] = useState(false);
  const landmarkerRef = useRef<PoseLandmarker | null>(null);
  const animFrameRef = useRef<number>(0);
  const lastTimestampRef = useRef<number>(-1);

  // Initialize PoseLandmarker once with the FULL model for accuracy
  useEffect(() => {
    let cancelled = false;

    async function init() {
      if (landmarkerRef.current) return;
      setIsLoading(true);
      try {
        const vision = await FilesetResolver.forVisionTasks(
          "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
        );
        const poseLandmarker = await PoseLandmarker.createFromOptions(vision, {
          baseOptions: {
            modelAssetPath:
              "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task",
            delegate: "GPU",
          },
          runningMode: "VIDEO",
          numPoses: 1,
        });
        if (!cancelled) {
          landmarkerRef.current = poseLandmarker;
          setModelReady(true);
        } else {
          poseLandmarker.close();
        }
      } catch (err) {
        console.error("Failed to initialize PoseLandmarker:", err);
      }
      if (!cancelled) setIsLoading(false);
    }

    init();
    return () => {
      cancelled = true;
    };
  }, []);

  // Detection loop — re-runs when model becomes ready OR enabled changes
  useEffect(() => {
    if (!enabled || !landmarkerRef.current) {
      setLandmarks([]);
      return;
    }

    const detect = () => {
      const video = videoRef.current;
      const landmarker = landmarkerRef.current;

      if (
        video &&
        landmarker &&
        video.readyState >= 2 &&
        video.videoWidth > 0
      ) {
        const timestamp = performance.now();
        // MediaPipe requires strictly increasing timestamps
        if (timestamp > lastTimestampRef.current) {
          lastTimestampRef.current = timestamp;
          try {
            const result = landmarker.detectForVideo(video, timestamp);
            if (result.landmarks) {
              setLandmarks(result.landmarks);
            }
          } catch (e) {
            // Silently skip frames that fail
          }
        }
      }

      animFrameRef.current = requestAnimationFrame(detect);
    };

    animFrameRef.current = requestAnimationFrame(detect);
    return () => {
      cancelAnimationFrame(animFrameRef.current);
    };
  }, [enabled, modelReady, videoRef]);

  return { landmarks, isLoading };
}
