import { supabase } from "@/lib/supabase";
import { WorkoutSession } from "@/lib/types";
import Link from "next/link";

export const revalidate = 0;

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
