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
