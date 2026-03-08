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
    if (next.has(id)) next.delete(id); else next.add(id);
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
        <button onClick={exportSelected} disabled={selected.size === 0 || exporting}
          className="px-4 py-2 bg-green-600 hover:bg-green-500 disabled:bg-gray-700 disabled:text-gray-500 rounded-lg text-sm font-medium transition">
          {exporting ? "Exporting..." : `Export ${selected.size} Session${selected.size !== 1 ? "s" : ""}`}
        </button>
      </div>

      <div className="bg-gray-900 rounded-xl border border-gray-800">
        <div className="flex items-center px-4 py-3 border-b border-gray-800">
          <label className="flex items-center gap-2 text-sm text-gray-400 cursor-pointer">
            <input type="checkbox" checked={selected.size === sessions.length && sessions.length > 0} onChange={toggleAll} className="accent-green-500" />
            Select All ({sessions.length})
          </label>
        </div>
        {sessions.length === 0 ? (
          <p className="px-4 py-8 text-center text-gray-500">No sessions available</p>
        ) : (
          sessions.map((s) => (
            <label key={s.id} className="flex items-center gap-4 px-4 py-3 border-b border-gray-800/50 hover:bg-gray-800/30 cursor-pointer">
              <input type="checkbox" checked={selected.has(s.id)} onChange={() => toggle(s.id)} className="accent-green-500" />
              <div className="flex-1 flex items-center justify-between">
                <div>
                  <span className="text-sm font-mono text-green-400">{s.total_reps} reps</span>
                  <span className="text-xs text-gray-500 ml-3">{s.duration_minutes}m</span>
                </div>
                <div className="text-xs text-gray-500">
                  {s.is_phone_mode ? "Phone" : "Glasses"} &middot; {new Date(s.start_time).toLocaleDateString()}
                </div>
              </div>
            </label>
          ))
        )}
      </div>
    </main>
  );
}
