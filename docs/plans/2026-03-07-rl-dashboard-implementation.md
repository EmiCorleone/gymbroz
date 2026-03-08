# GymBro RL Data Dashboard — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a public-facing dashboard for AfterQuery to browse workout sessions with synced video+pose overlays and export JSON training trajectories for RL environments.

**Architecture:** Three workstreams — (1) Supabase schema changes (pose_frames table, rep_events columns, public read RLS), (2) Android PoseDetectionManager changes to batch-upload landmark data, (3) Next.js dashboard app in `/dashboard` with session browser, video+canvas overlay viewer, data sidebar, and JSON export.

**Tech Stack:** Next.js 14 (App Router), @supabase/supabase-js, Tailwind CSS, HTML5 Canvas, Kotlin/Android (existing app)

---

## Task 1: Supabase Schema — Add `pose_frames` table and update `rep_events`

**Files:**
- Migration applied via Supabase MCP tool

**Step 1: Create `pose_frames` table**

Apply migration with this SQL:

```sql
-- New table for per-frame pose landmark data
CREATE TABLE pose_frames (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  session_id UUID REFERENCES workout_sessions(id) ON DELETE CASCADE NOT NULL,
  user_id UUID REFERENCES user_profiles(id) NOT NULL,
  frame_index INTEGER NOT NULL,
  timestamp_ms BIGINT NOT NULL,
  landmarks JSONB NOT NULL,
  left_elbow_angle FLOAT,
  right_elbow_angle FLOAT,
  active_arm TEXT,
  stage TEXT,
  rep_count INTEGER DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_pose_frames_session ON pose_frames(session_id, frame_index);
```

**Step 2: Add missing columns to `rep_events`**

```sql
ALTER TABLE rep_events
  ADD COLUMN IF NOT EXISTS wrist_y FLOAT,
  ADD COLUMN IF NOT EXISTS forearm_angle FLOAT,
  ADD COLUMN IF NOT EXISTS stage TEXT;
```

**Step 3: Add RLS policies for `pose_frames` (user insert + select own)**

```sql
ALTER TABLE pose_frames ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can insert own pose frames"
  ON pose_frames FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view own pose frames"
  ON pose_frames FOR SELECT
  USING (auth.uid() = user_id);
```

**Step 4: Add public read policies for dashboard (anon access)**

The dashboard is a public showcase — add anon SELECT on all relevant tables:

```sql
CREATE POLICY "Public read for dashboard"
  ON workout_sessions FOR SELECT
  USING (true);

CREATE POLICY "Public read for dashboard"
  ON exercise_sets FOR SELECT
  USING (true);

CREATE POLICY "Public read for dashboard"
  ON rep_events FOR SELECT
  USING (true);

CREATE POLICY "Public read for dashboard"
  ON pose_frames FOR SELECT
  USING (true);

CREATE POLICY "Public read for dashboard"
  ON user_profiles FOR SELECT
  USING (true);
```

**Step 5: Verify by querying table structure**

Run: `SELECT column_name, data_type FROM information_schema.columns WHERE table_name = 'pose_frames';`
Expected: All columns listed above.

Run: `SELECT column_name FROM information_schema.columns WHERE table_name = 'rep_events' AND column_name IN ('wrist_y', 'forearm_angle', 'stage');`
Expected: 3 rows.

---

## Task 2: Android — Emit pose frame data from PoseDetectionManager

**Files:**
- Modify: `android/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/openclaw/PoseDetectionManager.kt`

**Step 1: Add `PoseFrameData` data class and frame buffer**

Add after the `PoseOverlayData` data class (line 28):

```kotlin
data class PoseFrameData(
    val frameIndex: Int,
    val timestampMs: Long,
    val landmarks: List<List<Float>>, // 33 landmarks, each [x, y, z, visibility]
    val leftElbowAngle: Float?,
    val rightElbowAngle: Float?,
    val activeArm: String,
    val stage: String?,
    val repCount: Int
)
```

Add to the class body (after `overlayData` StateFlow):

```kotlin
private var frameIndex = 0
private val _frameBuffer = mutableListOf<PoseFrameData>()
val frameBuffer: List<PoseFrameData> get() = _frameBuffer

fun drainFrameBuffer(): List<PoseFrameData> {
    val frames = _frameBuffer.toList()
    _frameBuffer.clear()
    return frames
}
```

**Step 2: Capture landmark data in `processFrame`**

After the rep counting state update block (after line 231 — the `if (bestAngle >= 0)` block), and before drawing the HUD, add:

```kotlin
// Capture frame data for RL export (every 3rd frame = ~10fps)
if (frameIndex % 3 == 0) {
    val landmarkData = landmarks.map { lm ->
        listOf(lm.x(), lm.y(), lm.z(), lm.visibility().orElse(0f))
    }
    // Calculate both arm angles
    val leftAngle = run {
        val (a, b, c) = LEFT_ARM
        if (a < landmarks.size && b < landmarks.size && c < landmarks.size) {
            calculateAngle(
                landmarks[a].x(), landmarks[a].y(), landmarks[a].z(),
                landmarks[b].x(), landmarks[b].y(), landmarks[b].z(),
                landmarks[c].x(), landmarks[c].y(), landmarks[c].z()
            )
        } else null
    }
    val rightAngle = run {
        val (a, b, c) = RIGHT_ARM
        if (a < landmarks.size && b < landmarks.size && c < landmarks.size) {
            calculateAngle(
                landmarks[a].x(), landmarks[a].y(), landmarks[a].z(),
                landmarks[b].x(), landmarks[b].y(), landmarks[b].z(),
                landmarks[c].x(), landmarks[c].y(), landmarks[c].z()
            )
        } else null
    }
    _frameBuffer.add(PoseFrameData(
        frameIndex = frameIndex,
        timestampMs = timestampMs,
        landmarks = landmarkData,
        leftElbowAngle = leftAngle,
        rightElbowAngle = rightAngle,
        activeArm = activeArm,
        stage = stage,
        repCount = repCount
    ))
}
frameIndex++
```

**Step 3: Reset frameIndex in `reset()`**

Add `frameIndex = 0` and `_frameBuffer.clear()` to the `reset()` method.

---

## Task 3: Android — Batch upload pose frames to Supabase

**Files:**
- Modify: `android/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/data/WorkoutRepository.kt`
- Modify: `android/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/gemini/GeminiSessionViewModel.kt`

**Step 1: Add `logPoseFramesBatch` to WorkoutRepository**

Add after the `logRepEvent` method (line 198):

```kotlin
suspend fun logPoseFramesBatch(
    sessionId: Long,
    frames: List<com.meta.wearable.dat.externalsampleapps.cameraaccess.openclaw.PoseFrameData>
) {
    if (frames.isEmpty()) return
    try {
        val auth = GymBroSupabaseClient.client.auth
        val postgrest = GymBroSupabaseClient.client.postgrest
        val userId = auth.currentUserOrNull()?.id ?: return

        @kotlinx.serialization.Serializable
        data class SupabasePoseFrame(
            val session_id: String,
            val user_id: String,
            val frame_index: Int,
            val timestamp_ms: Long,
            val landmarks: kotlinx.serialization.json.JsonElement,
            val left_elbow_angle: Float?,
            val right_elbow_angle: Float?,
            val active_arm: String,
            val stage: String?,
            val rep_count: Int
        )

        val json = kotlinx.serialization.json.Json
        val rows = frames.map { f ->
            SupabasePoseFrame(
                session_id = sessionId.toString(),
                user_id = userId,
                frame_index = f.frameIndex,
                timestamp_ms = f.timestampMs,
                landmarks = json.parseToJsonElement(
                    json.encodeToString(kotlinx.serialization.builtins.ListSerializer(
                        kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.builtins.Float.serializer())
                    ), f.landmarks)
                ),
                left_elbow_angle = f.leftElbowAngle,
                right_elbow_angle = f.rightElbowAngle,
                active_arm = f.activeArm,
                stage = f.stage,
                rep_count = f.repCount
            )
        }
        postgrest.from("pose_frames").insert(rows)
    } catch (e: Exception) {
        Log.e("WorkoutRepo", "Failed to upload pose frames batch", e)
    }
}
```

**Step 2: Periodically drain and upload in GeminiSessionViewModel**

In `GeminiSessionViewModel.kt`, inside the `stateObservationJob` while loop (around line 148), add a periodic flush after the rep counting injection block (after line 191):

```kotlin
// Flush pose frame buffer every ~3 seconds (30 iterations × 100ms delay)
if (pollCount % 30 == 0) {
    val manager = if (usePovMode) null else poseDetectionManager
    val framesToUpload = manager?.drainFrameBuffer() ?: emptyList()
    if (framesToUpload.isNotEmpty()) {
        currentSessionId?.let { sId ->
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                repository.logPoseFramesBatch(sId, framesToUpload)
            }
        }
    }
}
```

Note: Add a `var pollCount = 0` counter and increment it each loop iteration. There's already a loop counter pattern — add `pollCount++` at the end of each iteration.

**Step 3: Flush remaining frames on session stop**

In `GeminiSessionViewModel.stopSession()` (line 239), before resetting the pose detection manager, add:

```kotlin
// Flush any remaining pose frames
val remainingFrames = poseDetectionManager?.drainFrameBuffer() ?: emptyList()
if (remainingFrames.isNotEmpty()) {
    currentSessionId?.let { sId ->
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            repository.logPoseFramesBatch(sId, remainingFrames)
        }
    }
}
```

---

## Task 4: Dashboard — Scaffold Next.js app

**Files:**
- Create: `dashboard/package.json`
- Create: `dashboard/tsconfig.json`
- Create: `dashboard/tailwind.config.ts`
- Create: `dashboard/postcss.config.mjs`
- Create: `dashboard/next.config.ts`
- Create: `dashboard/app/layout.tsx`
- Create: `dashboard/app/globals.css`
- Create: `dashboard/.env.example`

**Step 1: Scaffold Next.js project**

Run from project root:
```bash
cd /Users/amaru-mac/Documents/hackathons/gemini-yc-hack-march/gymbro
npx create-next-app@latest dashboard --typescript --tailwind --eslint --app --no-src-dir --import-alias "@/*"
```

**Step 2: Install Supabase client**

```bash
cd dashboard && npm install @supabase/supabase-js
```

**Step 3: Create `.env.local` with Supabase credentials**

Create `dashboard/.env.example`:
```
NEXT_PUBLIC_SUPABASE_URL=your_supabase_url_here
NEXT_PUBLIC_SUPABASE_ANON_KEY=your_supabase_anon_key_here
```

Copy to `.env.local` with real values (from root `.env`). Verify `dashboard/.env.local` is in `.gitignore`.

**Step 4: Create Supabase client lib**

Create `dashboard/lib/supabase.ts`:

```typescript
import { createClient } from "@supabase/supabase-js";

export const supabase = createClient(
  process.env.NEXT_PUBLIC_SUPABASE_URL!,
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!
);
```

**Step 5: Update layout with dark theme**

Replace `dashboard/app/layout.tsx`:

```tsx
import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "GymBro RL Data Explorer",
  description: "Browse workout sessions with pose estimation overlays for RL training",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark">
      <body className="bg-gray-950 text-gray-100 min-h-screen">{children}</body>
    </html>
  );
}
```

**Step 6: Verify dev server starts**

Run: `cd dashboard && npm run dev`
Expected: Server starts on http://localhost:3000

---

## Task 5: Dashboard — Session browser (index page)

**Files:**
- Create: `dashboard/lib/types.ts`
- Create: `dashboard/app/page.tsx`

**Step 1: Define TypeScript types**

Create `dashboard/lib/types.ts`:

```typescript
export interface UserProfile {
  id: string;
  name: string;
  gender: string | null;
  age: number | null;
  height_cm: number | null;
  weight_kg: number | null;
  fitness_goal: string | null;
  experience_level: string | null;
}

export interface WorkoutSession {
  id: string;
  user_id: string;
  start_time: string;
  end_time: string | null;
  duration_minutes: number;
  video_url: string | null;
  total_reps: number;
  total_exercises: number;
  is_phone_mode: boolean;
  user_profiles?: UserProfile;
}

export interface ExerciseSet {
  id: string;
  session_id: string;
  exercise_name: string;
  rep_count: number;
  start_time: string;
  end_time: string | null;
}

export interface RepEvent {
  id: string;
  session_id: string;
  exercise_name: string;
  rep_number: number;
  timestamp: string;
  wrist_y: number | null;
  forearm_angle: number | null;
  stage: string | null;
}

export interface PoseFrame {
  id: string;
  session_id: string;
  frame_index: number;
  timestamp_ms: number;
  landmarks: number[][]; // 33 × [x, y, z, visibility]
  left_elbow_angle: number | null;
  right_elbow_angle: number | null;
  active_arm: string | null;
  stage: string | null;
  rep_count: number;
}

export interface SessionTrajectory {
  version: string;
  environment: string;
  session: {
    id: string;
    user_context: {
      experience_level: string | null;
      fitness_goal: string | null;
      age: number | null;
      weight_kg: number | null;
      height_cm: number | null;
    };
    exercise: string;
    duration_seconds: number;
    total_reps: number;
    camera_mode: string;
  };
  video_url: string | null;
  frames: PoseFrame[];
  rep_events: RepEvent[];
}
```

**Step 2: Build session browser page**

Create `dashboard/app/page.tsx`:

```tsx
import { supabase } from "@/lib/supabase";
import { WorkoutSession } from "@/lib/types";
import Link from "next/link";

export const revalidate = 0; // always fresh

export default async function Home() {
  const { data: sessions } = await supabase
    .from("workout_sessions")
    .select("*, user_profiles(*)")
    .order("start_time", { ascending: false });

  return (
    <main className="max-w-7xl mx-auto px-6 py-10">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-white">GymBro RL Data Explorer</h1>
          <p className="text-gray-400 mt-1">
            Browse workout sessions with paired video + pose estimation trajectories
          </p>
        </div>
        <Link
          href="/export"
          className="px-4 py-2 bg-green-600 hover:bg-green-500 rounded-lg text-sm font-medium transition"
        >
          Export All
        </Link>
      </div>

      {!sessions || sessions.length === 0 ? (
        <div className="text-center py-20 text-gray-500">
          <p className="text-lg">No workout sessions yet.</p>
          <p className="text-sm mt-2">Record a workout with the GymBro app to see data here.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {(sessions as WorkoutSession[]).map((session) => (
            <Link
              key={session.id}
              href={`/session/${session.id}`}
              className="block bg-gray-900 border border-gray-800 rounded-xl p-5 hover:border-green-500/50 transition group"
            >
              {session.video_url && (
                <div className="aspect-video bg-gray-800 rounded-lg mb-4 overflow-hidden">
                  <video
                    src={session.video_url}
                    className="w-full h-full object-cover"
                    muted
                    preload="metadata"
                  />
                </div>
              )}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-sm font-mono text-green-400">
                    {session.total_reps} reps
                  </span>
                  <span className="text-xs text-gray-500">
                    {session.is_phone_mode ? "Phone" : "Glasses"}
                  </span>
                </div>
                <p className="text-sm text-gray-400">
                  {session.duration_minutes}m &middot;{" "}
                  {new Date(session.start_time).toLocaleDateString()}
                </p>
                {session.user_profiles && (
                  <p className="text-xs text-gray-600">
                    {session.user_profiles.experience_level} &middot;{" "}
                    {session.user_profiles.fitness_goal}
                  </p>
                )}
              </div>
            </Link>
          ))}
        </div>
      )}
    </main>
  );
}
```

**Step 3: Verify page renders**

Run: `cd dashboard && npm run dev`
Visit: http://localhost:3000
Expected: "GymBro RL Data Explorer" header with empty state message (no sessions yet).

---

## Task 6: Dashboard — Session viewer with video + canvas pose overlay

**Files:**
- Create: `dashboard/app/session/[id]/page.tsx`
- Create: `dashboard/components/PoseOverlayPlayer.tsx`
- Create: `dashboard/components/AngleGraph.tsx`
- Create: `dashboard/lib/pose-renderer.ts`

**Step 1: Create pose skeleton renderer**

Create `dashboard/lib/pose-renderer.ts`:

```typescript
// MediaPipe pose landmark connections (upper body focus)
export const POSE_CONNECTIONS = [
  [11, 12], [11, 13], [13, 15], [12, 14], [14, 16],
  [11, 23], [12, 24], [23, 24], [23, 25], [24, 26],
  [25, 27], [26, 28],
];

const ARM_LANDMARKS = new Set([11, 12, 13, 14, 15, 16]);

export function drawPoseSkeleton(
  ctx: CanvasRenderingContext2D,
  landmarks: number[][], // 33 × [x, y, z, visibility]
  width: number,
  height: number
) {
  ctx.clearRect(0, 0, width, height);

  // Draw connections
  ctx.strokeStyle = "#00ff88";
  ctx.lineWidth = 3;
  for (const [a, b] of POSE_CONNECTIONS) {
    if (!landmarks[a] || !landmarks[b]) continue;
    if (landmarks[a][3] < 0.3 || landmarks[b][3] < 0.3) continue;
    ctx.beginPath();
    ctx.moveTo(landmarks[a][0] * width, landmarks[a][1] * height);
    ctx.lineTo(landmarks[b][0] * width, landmarks[b][1] * height);
    ctx.stroke();
  }

  // Draw arm connections highlighted
  ctx.strokeStyle = "#ff4400";
  ctx.lineWidth = 4;
  const armConnections = [[11, 13], [13, 15], [12, 14], [14, 16]];
  for (const [a, b] of armConnections) {
    if (!landmarks[a] || !landmarks[b]) continue;
    if (landmarks[a][3] < 0.3 || landmarks[b][3] < 0.3) continue;
    ctx.beginPath();
    ctx.moveTo(landmarks[a][0] * width, landmarks[a][1] * height);
    ctx.lineTo(landmarks[b][0] * width, landmarks[b][1] * height);
    ctx.stroke();
  }

  // Draw joint dots
  for (let i = 0; i < landmarks.length; i++) {
    const lm = landmarks[i];
    if (!lm || lm[3] < 0.3) continue;
    ctx.fillStyle = ARM_LANDMARKS.has(i) ? "#ff4400" : "#00ff88";
    ctx.beginPath();
    ctx.arc(lm[0] * width, lm[1] * height, ARM_LANDMARKS.has(i) ? 8 : 4, 0, Math.PI * 2);
    ctx.fill();
  }
}
```

**Step 2: Create PoseOverlayPlayer component**

Create `dashboard/components/PoseOverlayPlayer.tsx`:

```tsx
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

  // Find the closest frame to the current video time
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
      return minDiff < 200 ? closest : null; // within 200ms tolerance
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

    const onPlay = () => {
      setIsPlaying(true);
      animRef.current = requestAnimationFrame(renderOverlay);
    };
    const onPause = () => {
      setIsPlaying(false);
      cancelAnimationFrame(animRef.current);
      renderOverlay(); // render one more time at pause position
    };
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
    if (video.paused) video.play();
    else video.pause();
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
        <video ref={videoRef} src={videoUrl} className="w-full h-full object-contain" preload="auto" />
        <canvas
          ref={canvasRef}
          className="absolute inset-0 w-full h-full pointer-events-none"
        />
      </div>
      <div className="flex items-center gap-3 mt-3">
        <button
          onClick={togglePlay}
          className="px-3 py-1 bg-gray-800 hover:bg-gray-700 rounded text-sm font-mono"
        >
          {isPlaying ? "⏸" : "▶"}
        </button>
        <input
          type="range"
          min={0}
          max={duration || 0}
          step={0.033}
          value={currentTime}
          onChange={seek}
          className="flex-1 accent-green-500"
        />
        <span className="text-xs text-gray-400 font-mono w-20 text-right">
          {formatTime(currentTime)} / {formatTime(duration)}
        </span>
      </div>
    </div>
  );
}
```

**Step 3: Create AngleGraph component**

Create `dashboard/components/AngleGraph.tsx`:

```tsx
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

    // Draw grid
    ctx.strokeStyle = "#333";
    ctx.lineWidth = 1;
    for (const angle of [0, 45, 90, 135, 180]) {
      const y = H - (angle / 180) * H;
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(W, y);
      ctx.stroke();
      ctx.fillStyle = "#555";
      ctx.font = "10px monospace";
      ctx.fillText(`${angle}°`, 2, y - 2);
    }

    // Draw threshold lines
    ctx.strokeStyle = "#ff440066";
    ctx.setLineDash([4, 4]);
    const downY = H - (80 / 180) * H;
    const upY = H - (120 / 180) * H;
    ctx.beginPath();
    ctx.moveTo(0, downY);
    ctx.lineTo(W, downY);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(0, upY);
    ctx.lineTo(W, upY);
    ctx.stroke();
    ctx.setLineDash([]);

    // Draw left elbow angle line
    ctx.strokeStyle = "#00ff88";
    ctx.lineWidth = 2;
    ctx.beginPath();
    for (let i = 0; i < frames.length; i++) {
      const x = ((frames[i].timestamp_ms - minTs) / range) * W;
      const angle = frames[i].left_elbow_angle ?? 0;
      const y = H - (angle / 180) * H;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Draw right elbow angle line
    ctx.strokeStyle = "#4488ff";
    ctx.lineWidth = 2;
    ctx.beginPath();
    for (let i = 0; i < frames.length; i++) {
      const x = ((frames[i].timestamp_ms - minTs) / range) * W;
      const angle = frames[i].right_elbow_angle ?? 0;
      const y = H - (angle / 180) * H;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Draw current time indicator
    const curX = ((currentTimestampMs - minTs) / range) * W;
    ctx.strokeStyle = "#ffffff";
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(curX, 0);
    ctx.lineTo(curX, H);
    ctx.stroke();

    // Legend
    ctx.font = "11px monospace";
    ctx.fillStyle = "#00ff88";
    ctx.fillText("L elbow", W - 70, 14);
    ctx.fillStyle = "#4488ff";
    ctx.fillText("R elbow", W - 70, 28);
  }, [frames, currentTimestampMs]);

  return (
    <canvas
      ref={canvasRef}
      width={600}
      height={150}
      className="w-full h-36 bg-gray-900 rounded-lg border border-gray-800"
    />
  );
}
```

**Step 4: Create session viewer page**

Create `dashboard/app/session/[id]/page.tsx`:

```tsx
import { supabase } from "@/lib/supabase";
import SessionViewer from "./SessionViewer";

export const revalidate = 0;

export default async function SessionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  const [sessionRes, framesRes, repsRes, setsRes] = await Promise.all([
    supabase
      .from("workout_sessions")
      .select("*, user_profiles(*)")
      .eq("id", id)
      .single(),
    supabase
      .from("pose_frames")
      .select("*")
      .eq("session_id", id)
      .order("frame_index", { ascending: true }),
    supabase
      .from("rep_events")
      .select("*")
      .eq("session_id", id)
      .order("rep_number", { ascending: true }),
    supabase
      .from("exercise_sets")
      .select("*")
      .eq("session_id", id),
  ]);

  if (!sessionRes.data) {
    return <div className="p-10 text-center text-red-400">Session not found</div>;
  }

  return (
    <SessionViewer
      session={sessionRes.data}
      frames={framesRes.data ?? []}
      repEvents={repsRes.data ?? []}
      exerciseSets={setsRes.data ?? []}
    />
  );
}
```

**Step 5: Create SessionViewer client component**

Create `dashboard/app/session/[id]/SessionViewer.tsx`:

```tsx
"use client";

import { useState, useCallback } from "react";
import Link from "next/link";
import PoseOverlayPlayer from "@/components/PoseOverlayPlayer";
import AngleGraph from "@/components/AngleGraph";
import { WorkoutSession, PoseFrame, RepEvent, ExerciseSet, SessionTrajectory } from "@/lib/types";

interface Props {
  session: WorkoutSession;
  frames: PoseFrame[];
  repEvents: RepEvent[];
  exerciseSets: ExerciseSet[];
}

const LANDMARK_NAMES = [
  "nose", "left_eye_inner", "left_eye", "left_eye_outer", "right_eye_inner",
  "right_eye", "right_eye_outer", "left_ear", "right_ear", "mouth_left",
  "mouth_right", "left_shoulder", "right_shoulder", "left_elbow", "right_elbow",
  "left_wrist", "right_wrist", "left_pinky", "right_pinky", "left_index",
  "right_index", "left_thumb", "right_thumb", "left_hip", "right_hip",
  "left_knee", "right_knee", "left_ankle", "right_ankle", "left_heel",
  "right_heel", "left_foot_index", "right_foot_index",
];

export default function SessionViewer({ session, frames, repEvents, exerciseSets }: Props) {
  const [currentFrame, setCurrentFrame] = useState<PoseFrame | null>(null);
  const profile = session.user_profiles;
  const exercise = exerciseSets[0]?.exercise_name ?? "unknown";

  const onFrameChange = useCallback((frame: PoseFrame | null) => {
    setCurrentFrame(frame);
  }, []);

  const exportTrajectory = () => {
    const trajectory: SessionTrajectory = {
      version: "1.0",
      environment: "gymbro_fitness_coaching",
      session: {
        id: session.id,
        user_context: {
          experience_level: profile?.experience_level ?? null,
          fitness_goal: profile?.fitness_goal ?? null,
          age: profile?.age ?? null,
          weight_kg: profile?.weight_kg ?? null,
          height_cm: profile?.height_cm ?? null,
        },
        exercise,
        duration_seconds: session.duration_minutes * 60,
        total_reps: session.total_reps,
        camera_mode: session.is_phone_mode ? "phone" : "glasses",
      },
      video_url: session.video_url,
      frames,
      rep_events: repEvents,
    };

    const blob = new Blob([JSON.stringify(trajectory, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `gymbro-trajectory-${session.id.slice(0, 8)}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <main className="max-w-7xl mx-auto px-6 py-8">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-4">
          <Link href="/" className="text-gray-400 hover:text-white text-sm">&larr; Back</Link>
          <h1 className="text-2xl font-bold">Session Viewer</h1>
        </div>
        <button
          onClick={exportTrajectory}
          className="px-4 py-2 bg-green-600 hover:bg-green-500 rounded-lg text-sm font-medium transition"
        >
          Export JSON Trajectory
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left: Video + Graphs (2 cols) */}
        <div className="lg:col-span-2 space-y-4">
          {session.video_url ? (
            <PoseOverlayPlayer
              videoUrl={session.video_url}
              frames={frames}
              onFrameChange={onFrameChange}
            />
          ) : (
            <div className="aspect-video bg-gray-900 rounded-lg flex items-center justify-center text-gray-500">
              No video available
            </div>
          )}

          {/* Rep Timeline */}
          {repEvents.length > 0 && (
            <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-2">Rep Timeline</h3>
              <div className="flex gap-1 items-end h-12">
                {repEvents.map((rep) => (
                  <div
                    key={rep.id}
                    className="flex-1 rounded-sm"
                    style={{
                      height: `${Math.max(20, (rep.forearm_angle ?? 90) / 180 * 100)}%`,
                      backgroundColor: rep.stage === "up" ? "#00ff88" : "#ff4400",
                    }}
                    title={`Rep ${rep.rep_number}: ${rep.stage} @ ${rep.forearm_angle?.toFixed(1)}°`}
                  />
                ))}
              </div>
            </div>
          )}

          {/* Angle Graph */}
          {frames.length > 0 && (
            <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-2">Elbow Angle Over Time</h3>
              <AngleGraph
                frames={frames}
                currentTimestampMs={currentFrame?.timestamp_ms ?? 0}
              />
            </div>
          )}
        </div>

        {/* Right: Data Sidebar (1 col) */}
        <div className="space-y-4">
          {/* Session Metadata */}
          <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
            <h3 className="text-sm font-medium text-gray-400 mb-3">Session Metadata</h3>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-gray-500">Exercise</dt>
                <dd className="font-mono text-green-400">{exercise}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Total Reps</dt>
                <dd className="font-mono">{session.total_reps}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Duration</dt>
                <dd className="font-mono">{session.duration_minutes}m</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Camera</dt>
                <dd className="font-mono">{session.is_phone_mode ? "Phone" : "Meta Glasses"}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Pose Frames</dt>
                <dd className="font-mono">{frames.length}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-500">Date</dt>
                <dd className="font-mono text-xs">{new Date(session.start_time).toLocaleString()}</dd>
              </div>
            </dl>
          </div>

          {/* User Context */}
          {profile && (
            <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-3">User Context</h3>
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-gray-500">Experience</dt>
                  <dd className="font-mono">{profile.experience_level}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Goal</dt>
                  <dd className="font-mono">{profile.fitness_goal}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Age</dt>
                  <dd className="font-mono">{profile.age}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Height</dt>
                  <dd className="font-mono">{profile.height_cm}cm</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Weight</dt>
                  <dd className="font-mono">{profile.weight_kg}kg</dd>
                </div>
              </dl>
            </div>
          )}

          {/* Current Frame Data */}
          <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
            <h3 className="text-sm font-medium text-gray-400 mb-3">Current Frame</h3>
            {currentFrame ? (
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-gray-500">Frame</dt>
                  <dd className="font-mono">{currentFrame.frame_index}/{frames.length}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">L Elbow</dt>
                  <dd className="font-mono text-green-400">
                    {currentFrame.left_elbow_angle?.toFixed(1) ?? "—"}°
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">R Elbow</dt>
                  <dd className="font-mono text-blue-400">
                    {currentFrame.right_elbow_angle?.toFixed(1) ?? "—"}°
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Stage</dt>
                  <dd className={`font-mono ${currentFrame.stage === "down" ? "text-red-400" : "text-green-400"}`}>
                    {currentFrame.stage?.toUpperCase() ?? "WAIT"}
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Rep Count</dt>
                  <dd className="font-mono">{currentFrame.rep_count}</dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-gray-500">Active Arm</dt>
                  <dd className="font-mono">{currentFrame.active_arm || "—"}</dd>
                </div>

                {/* Landmarks accordion */}
                <details className="mt-3">
                  <summary className="text-gray-500 cursor-pointer text-xs">
                    Landmarks ({currentFrame.landmarks.length})
                  </summary>
                  <div className="mt-2 max-h-60 overflow-y-auto text-xs font-mono space-y-1">
                    {currentFrame.landmarks.map((lm, i) => (
                      <div key={i} className="flex justify-between text-gray-500">
                        <span className="text-gray-600">[{i}] {LANDMARK_NAMES[i]}</span>
                        <span>{lm[0]?.toFixed(3)}, {lm[1]?.toFixed(3)}</span>
                      </div>
                    ))}
                  </div>
                </details>
              </dl>
            ) : (
              <p className="text-xs text-gray-600">Play video to see frame data</p>
            )}
          </div>
        </div>
      </div>
    </main>
  );
}
```

---

## Task 7: Dashboard — Bulk export page

**Files:**
- Create: `dashboard/app/export/page.tsx`

**Step 1: Create export page**

Create `dashboard/app/export/page.tsx`:

```tsx
import { supabase } from "@/lib/supabase";
import ExportClient from "./ExportClient";

export const revalidate = 0;

export default async function ExportPage() {
  const { data: sessions } = await supabase
    .from("workout_sessions")
    .select("*, user_profiles(*)")
    .order("start_time", { ascending: false });

  return <ExportClient sessions={sessions ?? []} />;
}
```

Create `dashboard/app/export/ExportClient.tsx`:

```tsx
"use client";

import { useState } from "react";
import Link from "next/link";
import { supabase } from "@/lib/supabase";
import { WorkoutSession, SessionTrajectory } from "@/lib/types";

export default function ExportClient({ sessions }: { sessions: WorkoutSession[] }) {
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [exporting, setExporting] = useState(false);

  const toggleAll = () => {
    if (selected.size === sessions.length) setSelected(new Set());
    else setSelected(new Set(sessions.map((s) => s.id)));
  };

  const toggle = (id: string) => {
    const next = new Set(selected);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setSelected(next);
  };

  const exportSelected = async () => {
    if (selected.size === 0) return;
    setExporting(true);

    const trajectories: SessionTrajectory[] = [];
    for (const id of selected) {
      const session = sessions.find((s) => s.id === id)!;
      const [framesRes, repsRes] = await Promise.all([
        supabase.from("pose_frames").select("*").eq("session_id", id).order("frame_index"),
        supabase.from("rep_events").select("*").eq("session_id", id).order("rep_number"),
      ]);

      const profile = session.user_profiles;
      trajectories.push({
        version: "1.0",
        environment: "gymbro_fitness_coaching",
        session: {
          id: session.id,
          user_context: {
            experience_level: profile?.experience_level ?? null,
            fitness_goal: profile?.fitness_goal ?? null,
            age: profile?.age ?? null,
            weight_kg: profile?.weight_kg ?? null,
            height_cm: profile?.height_cm ?? null,
          },
          exercise: "bicep_curl",
          duration_seconds: session.duration_minutes * 60,
          total_reps: session.total_reps,
          camera_mode: session.is_phone_mode ? "phone" : "glasses",
        },
        video_url: session.video_url,
        frames: framesRes.data ?? [],
        rep_events: repsRes.data ?? [],
      });
    }

    const blob = new Blob([JSON.stringify(trajectories, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `gymbro-trajectories-${trajectories.length}-sessions.json`;
    a.click();
    URL.revokeObjectURL(url);
    setExporting(false);
  };

  return (
    <main className="max-w-4xl mx-auto px-6 py-10">
      <div className="flex items-center justify-between mb-8">
        <div className="flex items-center gap-4">
          <Link href="/" className="text-gray-400 hover:text-white text-sm">&larr; Back</Link>
          <h1 className="text-2xl font-bold">Export Training Data</h1>
        </div>
        <button
          onClick={exportSelected}
          disabled={selected.size === 0 || exporting}
          className="px-4 py-2 bg-green-600 hover:bg-green-500 disabled:bg-gray-700 disabled:text-gray-500 rounded-lg text-sm font-medium transition"
        >
          {exporting ? "Exporting..." : `Export ${selected.size} Session${selected.size !== 1 ? "s" : ""}`}
        </button>
      </div>

      <div className="bg-gray-900 rounded-xl border border-gray-800">
        <div className="flex items-center px-4 py-3 border-b border-gray-800">
          <label className="flex items-center gap-2 text-sm text-gray-400 cursor-pointer">
            <input
              type="checkbox"
              checked={selected.size === sessions.length && sessions.length > 0}
              onChange={toggleAll}
              className="accent-green-500"
            />
            Select All ({sessions.length})
          </label>
        </div>
        {sessions.length === 0 ? (
          <p className="px-4 py-8 text-center text-gray-500">No sessions available</p>
        ) : (
          sessions.map((s) => (
            <label
              key={s.id}
              className="flex items-center gap-4 px-4 py-3 border-b border-gray-800/50 hover:bg-gray-800/30 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={selected.has(s.id)}
                onChange={() => toggle(s.id)}
                className="accent-green-500"
              />
              <div className="flex-1 flex items-center justify-between">
                <div>
                  <span className="text-sm font-mono text-green-400">{s.total_reps} reps</span>
                  <span className="text-xs text-gray-500 ml-3">{s.duration_minutes}m</span>
                </div>
                <div className="text-xs text-gray-500">
                  {s.is_phone_mode ? "Phone" : "Glasses"} &middot;{" "}
                  {new Date(s.start_time).toLocaleDateString()}
                </div>
              </div>
            </label>
          ))
        )}
      </div>
    </main>
  );
}
```

---

## Task 8: Final verification and commit

**Step 1: Verify Supabase schema**

Run SQL query to confirm `pose_frames` table exists with correct columns.

**Step 2: Verify dashboard builds**

```bash
cd /Users/amaru-mac/Documents/hackathons/gemini-yc-hack-march/gymbro/dashboard
npm run build
```

Expected: Build completes with no errors.

**Step 3: Commit all changes**

```bash
git add dashboard/ docs/plans/2026-03-07-rl-dashboard-*.md
git add android/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/openclaw/PoseDetectionManager.kt
git add android/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/data/WorkoutRepository.kt
git add android/app/src/main/java/com/meta/wearable/dat/externalsampleapps/cameraaccess/gemini/GeminiSessionViewModel.kt
git commit -m "feat: add RL data dashboard with video+pose overlay and JSON trajectory export"
```
