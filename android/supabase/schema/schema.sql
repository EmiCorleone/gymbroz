-- GymBro Supabase Schema & Setup Script
-- Paste this entire script into the Supabase SQL Editor and click "Run"

-----------------------------------------------------
-- 1. Create Tables
-----------------------------------------------------

-- User Profiles Table
-- This stores the fitness profile collected during onboarding.
CREATE TABLE public.user_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    gender TEXT,
    age INTEGER,
    height_cm INTEGER,
    weight_kg INTEGER,
    fitness_goal TEXT,
    experience_level TEXT,
    weekly_workouts TEXT,
    mirror_photo_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Workout Sessions Table
-- Logs the overarching session when the user is wearing the glasses or using the phone.
CREATE TABLE public.workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time TIMESTAMPTZ,
    duration_minutes INTEGER DEFAULT 0,
    video_url TEXT,
    total_reps INTEGER DEFAULT 0,
    total_exercises INTEGER DEFAULT 0,
    is_phone_mode BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Exercise Sets Table
-- Logs individual exercises inside a workout session.
CREATE TABLE public.exercise_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES public.workout_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    exercise_name TEXT NOT NULL,
    rep_count INTEGER DEFAULT 0,
    start_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    end_time TIMESTAMPTZ,
    guide_image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Rep Events Table
-- Logs precise timestamps and pose estimation data for every single rep detected.
CREATE TABLE public.rep_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES public.workout_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
    exercise_name TEXT NOT NULL,
    rep_number INTEGER NOT NULL,
    wrist_y REAL DEFAULT 0,
    forearm_angle REAL DEFAULT 0,
    stage TEXT,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-----------------------------------------------------
-- 2. Enable Row Level Security (RLS)
-----------------------------------------------------

-- Enable RLS on all tables
-- Enable RLS on all tables
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.workout_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.exercise_sets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rep_events ENABLE ROW LEVEL SECURITY;

-- Note: `auth.uid()` securely references the authenticated user's ID.
-- In Supabase, Anonymous Login automatically provisions a valid `auth.uid()` in the users table, 
-- meaning these policies natively support Anonymous users right alongside regular ones!

-- Policies for user_profiles: Users can only see and modify their own profile
CREATE POLICY "Users can view own profile" 
ON public.user_profiles FOR SELECT 
USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile" 
ON public.user_profiles FOR INSERT 
WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile" 
ON public.user_profiles FOR UPDATE 
USING (auth.uid() = id);

-- Policies for workout_sessions: Users can only see and modify their own sessions
CREATE POLICY "Users can view own workout sessions" 
ON public.workout_sessions FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own workout sessions" 
ON public.workout_sessions FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own workout sessions" 
ON public.workout_sessions FOR UPDATE 
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own workout sessions" 
ON public.workout_sessions FOR DELETE 
USING (auth.uid() = user_id);

-- Policies for exercise_sets: Users can only see and modify their own sets
CREATE POLICY "Users can view own exercise sets" 
ON public.exercise_sets FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own exercise sets" 
ON public.exercise_sets FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own exercise sets" 
ON public.exercise_sets FOR UPDATE 
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own exercise sets" 
ON public.exercise_sets FOR DELETE 
USING (auth.uid() = user_id);

-- Policies for rep_events: Users can only see and modify their own rep tracking events
CREATE POLICY "Users can view own rep events" 
ON public.rep_events FOR SELECT 
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own rep events" 
ON public.rep_events FOR INSERT 
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own rep events" 
ON public.rep_events FOR UPDATE 
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own rep events" 
ON public.rep_events FOR DELETE 
USING (auth.uid() = user_id);


-----------------------------------------------------
-- 3. Set Up Storage Bucket for Photos
-----------------------------------------------------

-- Create a public bucket called 'gymbro_assets' for mirror photos and exercise guides
INSERT INTO storage.buckets (id, name, public) 
VALUES ('gymbro_assets', 'gymbro_assets', true)
ON CONFLICT (id) DO NOTHING;

-- Enable RLS on the storage schema
-- ALTER TABLE storage.objects ENABLE ROW LEVEL SECURITY;

-- Allow public read access to the photos
CREATE POLICY "Public Access" 
ON storage.objects FOR SELECT 
USING ( bucket_id = 'gymbro_assets' );

-- Allow authenticated (including anonymous) users to upload, update, and delete their own files
CREATE POLICY "Users can upload their own assets" 
ON storage.objects FOR INSERT 
WITH CHECK ( bucket_id = 'gymbro_assets' AND auth.uid()::text = (storage.foldername(name))[1] );

CREATE POLICY "Users can update their own assets" 
ON storage.objects FOR UPDATE 
USING ( bucket_id = 'gymbro_assets' AND auth.uid()::text = (storage.foldername(name))[1] );

CREATE POLICY "Users can delete their own assets" 
ON storage.objects FOR DELETE 
USING ( bucket_id = 'gymbro_assets' AND auth.uid()::text = (storage.foldername(name))[1] );

-----------------------------------------------------
-- 4. Set Up Storage Bucket for Videos
-----------------------------------------------------

-- Create a public bucket called 'gymbro_videos' for workout recordings
INSERT INTO storage.buckets (id, name, public) 
VALUES ('gymbro_videos', 'gymbro_videos', true)
ON CONFLICT (id) DO NOTHING;

-- Allow public read access to the videos
CREATE POLICY "Public Access for Videos" 
ON storage.objects FOR SELECT 
USING ( bucket_id = 'gymbro_videos' );

-- Allow authenticated and anonymous users to upload, update, and delete their own videos securely
CREATE POLICY "Users can upload their own videos" 
ON storage.objects FOR INSERT 
WITH CHECK ( bucket_id = 'gymbro_videos' AND auth.uid()::text = (storage.foldername(name))[1] );

CREATE POLICY "Users can update their own videos" 
ON storage.objects FOR UPDATE 
USING ( bucket_id = 'gymbro_videos' AND auth.uid()::text = (storage.foldername(name))[1] );

CREATE POLICY "Users can delete their own videos" 
ON storage.objects FOR DELETE 
USING ( bucket_id = 'gymbro_videos' AND auth.uid()::text = (storage.foldername(name))[1] );

