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
        <button onClick={exportTrajectory} className="px-4 py-2 bg-green-600 hover:bg-green-500 rounded-lg text-sm font-medium transition">
          Export JSON Trajectory
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-4">
          {session.video_url ? (
            <PoseOverlayPlayer videoUrl={session.video_url} frames={frames} onFrameChange={onFrameChange} />
          ) : (
            <div className="aspect-video bg-gray-900 rounded-lg flex items-center justify-center text-gray-500">No video available</div>
          )}

          {repEvents.length > 0 && (
            <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-2">Rep Timeline</h3>
              <div className="flex gap-1 items-end h-12">
                {repEvents.map((rep) => (
                  <div key={rep.id} className="flex-1 rounded-sm" style={{
                    height: `${Math.max(20, (rep.forearm_angle ?? 90) / 180 * 100)}%`,
                    backgroundColor: rep.stage === "up" ? "#00ff88" : "#ff4400",
                  }} title={`Rep ${rep.rep_number}: ${rep.stage} @ ${rep.forearm_angle?.toFixed(1)}\u00B0`} />
                ))}
              </div>
            </div>
          )}

          {frames.length > 0 && (
            <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-2">Elbow Angle Over Time</h3>
              <AngleGraph frames={frames} currentTimestampMs={currentFrame?.timestamp_ms ?? 0} />
            </div>
          )}
        </div>

        <div className="space-y-4">
          <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
            <h3 className="text-sm font-medium text-gray-400 mb-3">Session Metadata</h3>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between"><dt className="text-gray-500">Exercise</dt><dd className="font-mono text-green-400">{exercise}</dd></div>
              <div className="flex justify-between"><dt className="text-gray-500">Total Reps</dt><dd className="font-mono">{session.total_reps}</dd></div>
              <div className="flex justify-between"><dt className="text-gray-500">Duration</dt><dd className="font-mono">{session.duration_minutes}m</dd></div>
              <div className="flex justify-between"><dt className="text-gray-500">Camera</dt><dd className="font-mono">{session.is_phone_mode ? "Phone" : "Meta Glasses"}</dd></div>
              <div className="flex justify-between"><dt className="text-gray-500">Pose Frames</dt><dd className="font-mono">{frames.length}</dd></div>
              <div className="flex justify-between"><dt className="text-gray-500">Date</dt><dd className="font-mono text-xs">{new Date(session.start_time).toLocaleString()}</dd></div>
            </dl>
          </div>

          {profile && (
            <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
              <h3 className="text-sm font-medium text-gray-400 mb-3">User Context</h3>
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between"><dt className="text-gray-500">Experience</dt><dd className="font-mono">{profile.experience_level}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Goal</dt><dd className="font-mono">{profile.fitness_goal}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Age</dt><dd className="font-mono">{profile.age}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Height</dt><dd className="font-mono">{profile.height_cm}cm</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Weight</dt><dd className="font-mono">{profile.weight_kg}kg</dd></div>
              </dl>
            </div>
          )}

          <div className="bg-gray-900 rounded-lg border border-gray-800 p-4">
            <h3 className="text-sm font-medium text-gray-400 mb-3">Current Frame</h3>
            {currentFrame ? (
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between"><dt className="text-gray-500">Frame</dt><dd className="font-mono">{currentFrame.frame_index}/{frames.length}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">L Elbow</dt><dd className="font-mono text-green-400">{currentFrame.left_elbow_angle?.toFixed(1) ?? "\u2014"}\u00B0</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">R Elbow</dt><dd className="font-mono text-blue-400">{currentFrame.right_elbow_angle?.toFixed(1) ?? "\u2014"}\u00B0</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Stage</dt><dd className={`font-mono ${currentFrame.stage === "down" ? "text-red-400" : "text-green-400"}`}>{currentFrame.stage?.toUpperCase() ?? "WAIT"}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Rep Count</dt><dd className="font-mono">{currentFrame.rep_count}</dd></div>
                <div className="flex justify-between"><dt className="text-gray-500">Active Arm</dt><dd className="font-mono">{currentFrame.active_arm || "\u2014"}</dd></div>
                <details className="mt-3">
                  <summary className="text-gray-500 cursor-pointer text-xs">Landmarks ({currentFrame.landmarks.length})</summary>
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
