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
  landmarks: number[][];
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
