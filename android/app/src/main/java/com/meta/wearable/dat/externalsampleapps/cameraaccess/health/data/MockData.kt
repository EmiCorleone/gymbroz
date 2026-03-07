package com.meta.wearable.dat.externalsampleapps.cameraaccess.health.data

import androidx.compose.ui.graphics.Color

object MockData {

    val mockUser = UserProfile(
        name = "Antony Thomas", age = 28, avatar = "", height = "5'11\"", weight = 175,
        bloodType = "O+",
        emergencyContact = EmergencyContact("Maria Thomas", "(555) 234-5678"),
        goals = UserGoals(steps = 10000, calories = 2200, sleepHours = 8, targetWeight = 170),
        preferences = UserPreferences()
    )

    val mockWorkouts = listOf(
        Workout("w1", "HIIT Blast", WorkoutCategory.HIIT, Difficulty.Advanced, 30, listOf(
            Exercise("Burpees", sets = 3, reps = 15),
            Exercise("Mountain Climbers", sets = 3, reps = 20),
            Exercise("Jump Squats", sets = 3, reps = 12),
            Exercise("High Knees", durationSec = 60),
            Exercise("Box Jumps", sets = 3, reps = 10)
        )),
        Workout("w2", "Upper Body Strength", WorkoutCategory.Strength, Difficulty.Intermediate, 45, listOf(
            Exercise("Bench Press", sets = 4, reps = 10),
            Exercise("Shoulder Press", sets = 3, reps = 12),
            Exercise("Bent Over Rows", sets = 4, reps = 10),
            Exercise("Bicep Curls", sets = 3, reps = 15),
            Exercise("Tricep Dips", sets = 3, reps = 12)
        )),
        Workout("w3", "Morning Yoga Flow", WorkoutCategory.Yoga, Difficulty.Beginner, 25, listOf(
            Exercise("Sun Salutation", durationSec = 120),
            Exercise("Warrior Pose", durationSec = 90),
            Exercise("Tree Pose", durationSec = 60),
            Exercise("Downward Dog", durationSec = 90),
            Exercise("Savasana", durationSec = 180)
        )),
        Workout("w4", "Cardio Burn", WorkoutCategory.Cardio, Difficulty.Intermediate, 40, listOf(
            Exercise("Jumping Jacks", durationSec = 120),
            Exercise("Running in Place", durationSec = 180),
            Exercise("Jump Rope", durationSec = 120),
            Exercise("Lateral Shuffles", durationSec = 90),
            Exercise("Stair Climbers", durationSec = 120)
        )),
        Workout("w5", "Evening Walk", WorkoutCategory.Walking, Difficulty.Beginner, 30, listOf(
            Exercise("Warm-up Walk", durationSec = 300),
            Exercise("Brisk Walk", durationSec = 900),
            Exercise("Cool-down Walk", durationSec = 300)
        )),
        Workout("w6", "Leg Day", WorkoutCategory.Strength, Difficulty.Advanced, 50, listOf(
            Exercise("Squats", sets = 4, reps = 12),
            Exercise("Lunges", sets = 3, reps = 15),
            Exercise("Leg Press", sets = 4, reps = 10),
            Exercise("Calf Raises", sets = 3, reps = 20),
            Exercise("Deadlifts", sets = 4, reps = 8)
        ))
    )

    val mockSteps = (0 until 30).map { i ->
        val steps = listOf(8234, 12456, 6789, 10234, 9876, 11234, 7654, 13456, 8901, 10567,
            9234, 11789, 6543, 12890, 8765, 10432, 9123, 11567, 7890, 13234,
            8456, 10789, 6234, 12567, 8901, 10234, 9567, 11890, 7234, 13567)[i]
        StepDay(
            date = "2026-02-${String.format("%02d", i + 1)}",
            steps = steps,
            caloriesBurned = (steps * 0.04).toInt(),
            activeMinutes = (steps / 100)
        )
    }

    val todaysMeals = listOf(
        Meal("m1", "Oatmeal & Berries", 350, 45, 12, 8, MealType.Breakfast, "8:00 AM", "2026-03-07"),
        Meal("m2", "Greek Yogurt Parfait", 280, 32, 18, 9, MealType.Breakfast, "10:00 AM", "2026-03-07"),
        Meal("m3", "Grilled Chicken Salad", 420, 15, 38, 22, MealType.Lunch, "12:30 PM", "2026-03-07"),
        Meal("m4", "Turkey Wrap", 380, 35, 28, 14, MealType.Lunch, "1:00 PM", "2026-03-07"),
        Meal("m5", "Salmon & Vegetables", 520, 18, 42, 28, MealType.Dinner, "7:00 PM", "2026-03-07"),
        Meal("m6", "Protein Shake", 180, 8, 30, 3, MealType.Snack, "4:00 PM", "2026-03-07")
    )

    val mockNutrition = DailyNutrition(
        date = "2026-03-07",
        totalCalories = todaysMeals.sumOf { it.calories },
        goalCalories = 2200,
        carbs = MacroInfo(current = todaysMeals.sumOf { it.carbs }, goal = 275),
        proteins = MacroInfo(current = todaysMeals.sumOf { it.proteins }, goal = 150),
        fats = MacroInfo(current = todaysMeals.sumOf { it.fats }, goal = 73),
        meals = todaysMeals
    )

    val mockSleep = listOf(
        SleepLog("2026-03-06", "11:00 PM", "6:45 AM", 7.75f, 4, 2.1f, 3.5f, 2.15f),
        SleepLog("2026-03-05", "10:30 PM", "6:30 AM", 8.0f, 5, 2.5f, 3.2f, 2.3f),
        SleepLog("2026-03-04", "11:30 PM", "6:00 AM", 6.5f, 3, 1.8f, 3.0f, 1.7f),
        SleepLog("2026-03-03", "10:00 PM", "6:15 AM", 8.25f, 5, 2.6f, 3.4f, 2.25f),
        SleepLog("2026-03-02", "11:15 PM", "5:45 AM", 6.5f, 3, 1.7f, 3.1f, 1.7f),
        SleepLog("2026-03-01", "10:45 PM", "7:00 AM", 8.25f, 4, 2.3f, 3.6f, 2.35f),
        SleepLog("2026-02-28", "10:15 PM", "6:30 AM", 8.25f, 5, 2.4f, 3.5f, 2.35f)
    )

    val mockStress = listOf(
        StressEntry("2026-03-07", 6, "Anxious"),
        StressEntry("2026-03-06", 4, "Calm"),
        StressEntry("2026-03-05", 7, "Stressed", "Deadline approaching")
    )

    val mockMedications = listOf(
        Medication("med1", "Lisinopril", "10mg", MedFrequency.Daily, "8:00 AM", true),
        Medication("med2", "Metformin", "500mg", MedFrequency.TwiceDaily, "8:00 AM, 8:00 PM", true),
        Medication("med3", "Aspirin", "81mg", MedFrequency.Daily, "8:00 AM", false),
        Medication("med4", "Vitamin D", "2000 IU", MedFrequency.Daily, "9:00 AM", true)
    )

    val mockAppointments = listOf(
        Appointment("a1", "Dr. Sarah Chen", "Cardiologist", "2026-03-15", "10:00 AM", "Metro Heart Center"),
        Appointment("a2", "Dr. James Wilson", "Primary Care", "2026-03-20", "2:30 PM", "City Health Clinic", "Annual checkup"),
        Appointment("a3", "Dr. Lisa Park", "Endocrinologist", "2026-04-02", "9:00 AM", "Endocrine Associates")
    )

    val mockCommunityPosts = listOf(
        CommunityPost("p1", "Emma Thomas", Color(0xFFFF6B6B), "Just finished a 5k run! Personal best time today.", "2h ago", 24, 5, false),
        CommunityPost("p2", "Marcus Chen", Color(0xFF4ECDC4), "Meal prep Sunday! Made chicken quinoa bowls for the week.", "4h ago", 42, 8, true),
        CommunityPost("p3", "Sofia Ramirez", Color(0xFFFFE66D), "Hit a new deadlift PR - 225lbs! Months of training paying off.", "6h ago", 67, 12, false),
        CommunityPost("p4", "Jake Wilson", Color(0xFFA8D86E), "Morning yoga changed my life. 30 days streak!", "8h ago", 31, 6, true),
        CommunityPost("p5", "Aisha Patel", Color(0xFFD9A8FF), "Any recommendations for post-workout recovery meals?", "12h ago", 18, 15, false),
        CommunityPost("p6", "Ryan Brooks", Color(0xFF34C759), "Swimming is the most underrated cardio. 45 min session done!", "1d ago", 29, 4, false)
    )

    val mockJournal = listOf(
        JournalEntry("j1", "2026-03-07", "Had a productive morning workout. Feeling energized and focused for the day ahead.", listOf("Focused", "Productive")),
        JournalEntry("j2", "2026-03-06", "Rest day today. Spent time stretching and doing light yoga. Feeling relaxed.", listOf("Relaxed", "Happy")),
        JournalEntry("j3", "2026-03-05", "Long day at work but managed to fit in an evening run. Tired but satisfied.", listOf("Tired", "Reflective"))
    )

    val mockVitals = listOf(
        VitalReading("2026-03-07", systolic = 122, diastolic = 78, heartRate = 68, weight = 175.2f),
        VitalReading("2026-03-06", systolic = 118, diastolic = 76, heartRate = 72, weight = 175.0f),
        VitalReading("2026-03-05", systolic = 125, diastolic = 80, heartRate = 70, weight = 175.5f),
        VitalReading("2026-03-04", systolic = 120, diastolic = 77, heartRate = 65, weight = 174.8f),
        VitalReading("2026-03-03", systolic = 119, diastolic = 75, heartRate = 69, weight = 175.1f),
        VitalReading("2026-03-02", systolic = 123, diastolic = 79, heartRate = 71, weight = 175.3f),
        VitalReading("2026-03-01", systolic = 121, diastolic = 76, heartRate = 67, weight = 174.9f)
    )

    val mockMeditation = listOf(
        MeditationSession("2026-03-07", 10, MeditationType.Guided),
        MeditationSession("2026-03-06", 15, MeditationType.Unguided),
        MeditationSession("2026-03-05", 5, MeditationType.Guided)
    )

    val meditationStreak = 12
}
