# GymBro RL Data Dashboard — Design Document

**Date**: 2026-03-07
**Purpose**: Showcase dashboard for AfterQuery to browse workout sessions, view video+pose overlays, and export RL training trajectories.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Android App    │────▶│  Supabase        │◀────│  Dashboard      │
│  (data source)  │     │  (DB + Storage)  │     │  (Next.js app)  │
│                 │     │                  │     │  /dashboard     │
│ • pose_frames   │     │ • pose_frames    │     │                 │
│ • rep_events    │     │ • workout_sessions│    │ • Video player  │
│ • videos        │     │ • exercise_sets  │     │ • Pose overlay  │
│                 │     │ • gymbro_videos  │     │ • Data sidebar  │
└─────────────────┘     └──────────────────┘     │ • JSON export   │
                                                  └─────────────────┘
```

**Stack**: Next.js 14 (App Router) + @supabase/supabase-js + Tailwind CSS + HTML5 Canvas

No auth on dashboard — public showcase for AfterQuery. Read-only RLS policies on Supabase.

## New Supabase Table: `pose_frames`

```sql
CREATE TABLE pose_frames (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  session_id UUID REFERENCES workout_sessions(id) NOT NULL,
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

## Android Changes

1. `PoseDetectionManager` emits `PoseFrameData` (landmarks + angles + state) alongside overlay bitmap
2. Buffer frames in memory (batch of ~30 = 1 second of data)
3. Flush batch to Supabase via `WorkoutRepository.logPoseFramesBatch()`
4. Throttled to ~10fps upload (every 3rd frame)

## Dashboard Pages

### Index (`/`) — Session Browser
- Grid of session cards with video thumbnails
- Shows: exercise name, rep count, duration, camera mode, date
- Click to open session viewer

### Session Viewer (`/session/[id]`) — Main Page

```
┌──────────────────────────────────────────────────────────┐
│  GymBro RL Data Explorer                    [Export JSON] │
├────────────────────────────────┬─────────────────────────┤
│                                │  Session Metadata       │
│    ┌──────────────────────┐    │  User: beginner, M, 25  │
│    │   VIDEO PLAYER       │    │  Exercise: bicep_curl   │
│    │   + CANVAS OVERLAY   │    │  Duration: 4m 32s       │
│    │   (pose skeleton)    │    │  Total Reps: 12         │
│    └──────────────────────┘    │                         │
│    ▶ ━━━━━━━●━━━━━━━━━ 2:15   │  Current Frame          │
│                                │  L Elbow: 78.3°         │
│    Rep Timeline                │  R Elbow: 142.1°        │
│    ┌──────────────────────┐    │  Stage: DOWN            │
│    │ ↑↓↑↓↑↓↑↓↑↓↑↓        │    │  Rep: 8/12             │
│    └──────────────────────┘    │                         │
│                                │  Landmarks (33)         │
│    Angle Graph                 │  [0] nose: 0.52, 0.31   │
│    ┌──────────────────────┐    │  [13] L_elbow: ...      │
│    │    /\  /\  /\        │    │  ...                    │
│    │   /  \/  \/  \       │    │                         │
│    └──────────────────────┘    │                         │
├────────────────────────────────┴─────────────────────────┤
│  [Download JSON Trajectory]  [Download All Sessions]     │
└──────────────────────────────────────────────────────────┘
```

- Video scrubbing syncs canvas overlay + sidebar to current frame
- Rep timeline shows up/down transitions as waveform
- Angle graph shows elbow angle over time with rep boundaries

### Export (`/export`) — Bulk Export
- Select sessions, download JSON trajectories

## JSON Export Format

```json
{
  "version": "1.0",
  "environment": "gymbro_fitness_coaching",
  "session": {
    "id": "uuid",
    "user_context": {
      "experience_level": "beginner",
      "fitness_goal": "build_muscle",
      "age": 25, "weight_kg": 75, "height_cm": 178
    },
    "exercise": "bicep_curl",
    "duration_seconds": 272,
    "total_reps": 12,
    "camera_mode": "phone"
  },
  "video_url": "https://...supabase.co/storage/v1/.../workout_123.mp4",
  "frames": [
    {
      "frame_index": 0,
      "timestamp_ms": 0,
      "landmarks": [{"x": 0.52, "y": 0.31, "z": -0.02, "visibility": 0.99}],
      "left_elbow_angle": 156.2,
      "right_elbow_angle": 148.7,
      "active_arm": "L",
      "stage": "wait",
      "rep_count": 0
    }
  ],
  "rep_events": [
    {"rep_number": 1, "timestamp_ms": 4200, "stage": "up", "wrist_y": 0.45, "forearm_angle": 125.3}
  ]
}
```

## Out of Scope

- Auth/login on dashboard
- Editing/annotation tools
- Real-time streaming view
- Multi-user comparison
- Pre-baked overlay videos
