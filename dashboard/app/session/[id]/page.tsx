import { supabase } from "@/lib/supabase";
import SessionViewer from "./SessionViewer";

export const revalidate = 0;

export default async function SessionPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;

  const [sessionRes, framesRes, repsRes, setsRes] = await Promise.all([
    supabase.from("workout_sessions").select("*, user_profiles(*)").eq("id", id).single(),
    supabase.from("pose_frames").select("*").eq("session_id", id).order("frame_index", { ascending: true }),
    supabase.from("rep_events").select("*").eq("session_id", id).order("rep_number", { ascending: true }),
    supabase.from("exercise_sets").select("*").eq("session_id", id),
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
