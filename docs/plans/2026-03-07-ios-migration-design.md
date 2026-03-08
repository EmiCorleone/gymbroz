# GymBro iOS Migration Design

**Date:** 2026-03-07
**Approach:** Layer-by-Layer (Data → Services → ViewModels → UI)
**Target:** Full feature parity with Android app

## Decisions

- **Pose detection:** MediaPipe iOS (same engine as Android)
- **Local database:** SwiftData (iOS 17+)
- **Project base:** New Xcode project in `gymbro/ios/`
- **Deployment target:** iOS 17.0 (matches Meta SDK requirement)

## Project Structure

```
gymbro/ios/
├── GymBro.xcodeproj/
├── GymBro/
│   ├── GymBroApp.swift                    # @main entry point
│   ├── Secrets.swift                      # API keys from xcconfig
│   │
│   ├── Data/
│   │   ├── Models/
│   │   │   ├── UserProfile.swift          # @Model
│   │   │   ├── WorkoutSession.swift       # @Model
│   │   │   └── ExerciseSet.swift          # @Model
│   │   ├── GymBroSupabaseClient.swift     # Supabase Swift SDK
│   │   ├── WorkoutRepository.swift        # Business logic
│   │   └── SettingsManager.swift          # @AppStorage / UserDefaults
│   │
│   ├── Services/
│   │   ├── Gemini/
│   │   │   ├── GeminiLiveService.swift    # URLSessionWebSocketTask
│   │   │   ├── GeminiConfig.swift
│   │   │   └── AudioManager.swift         # AVAudioEngine I/O
│   │   ├── PoseDetection/
│   │   │   ├── PoseDetectionManager.swift # MediaPipe iOS
│   │   │   ├── PovRepCounter.swift
│   │   │   └── GymToolHandler.swift       # Gemini tool calls
│   │   ├── Wearables/
│   │   │   └── WearablesManager.swift     # MWDATCore + MWDATCamera
│   │   ├── Camera/
│   │   │   └── PhoneCameraManager.swift   # AVCaptureSession fallback
│   │   └── Streaming/
│   │       └── StreamingManager.swift     # Background streaming
│   │
│   ├── ViewModels/
│   │   ├── OnboardingViewModel.swift
│   │   ├── StreamViewModel.swift
│   │   ├── GeminiSessionViewModel.swift
│   │   ├── WearablesViewModel.swift
│   │   ├── DashboardViewModel.swift
│   │   └── MockDeviceKitViewModel.swift
│   │
│   ├── Views/
│   │   ├── Onboarding/
│   │   │   └── OnboardingFlow.swift       # 10-step flow
│   │   ├── Navigation/
│   │   │   └── MainTabView.swift          # TabView
│   │   ├── Dashboard/
│   │   │   └── DashboardView.swift
│   │   ├── Stream/
│   │   │   ├── StreamView.swift
│   │   │   ├── GeminiOverlayView.swift
│   │   │   └── ControlsRow.swift
│   │   ├── Profile/
│   │   │   └── ProfileView.swift
│   │   ├── Settings/
│   │   │   └── SettingsView.swift
│   │   └── Components/
│   │       ├── CircleButton.swift
│   │       ├── GlassModifiers.swift
│   │       └── AnimatedMeshGradient.swift
│   │
│   ├── Theme/
│   │   ├── GymBroTheme.swift
│   │   └── AppColor.swift
│   │
│   ├── Assets.xcassets/
│   └── Info.plist
│
├── GymBroTests/
└── README.md
```

## Data Layer

### SwiftData Models

**UserProfile** — Maps from Android's Room entity:
- name, gender, age, heightCm, weightKg
- fitnessGoal, experienceLevel, weeklyWorkouts
- mirrorPhotoPath (Supabase storage URL)
- createdAt, updatedAt

**WorkoutSession:**
- startTime, endTime, durationMinutes
- totalReps, totalExercises, isPhoneMode
- videoUrl, exercises relationship → [ExerciseSet]

**ExerciseSet:**
- exerciseName, repCount, startTime, endTime
- guideImagePath, inverse relationship → WorkoutSession

### Supabase Client

Uses `supabase-swift` SDK:
- **Auth:** Anonymous + email/password sign-in
- **Postgrest:** CRUD for user_profiles, workout_sessions, exercise_sets
- **Storage:** gymbro_assets (photos), gymbro_videos (recordings)
- Credentials via xcconfig files (never hardcoded)

### Settings

`@AppStorage` / `UserDefaults` replacing Android SharedPreferences:
- Gemini API key, system prompt
- Connection URLs
- Onboarding completion flag

## Service Layer

### GeminiLiveService

WebSocket connection to Gemini Live API:
- **Transport:** `URLSessionWebSocketTask` (replaces OkHttp)
- **Endpoint:** `wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent`
- **Model:** `gemini-2.5-flash-native-audio-preview-12-2025`
- **Audio in:** 16kHz mono, 16-bit PCM
- **Audio out:** 24kHz mono, 16-bit PCM
- **Video:** 1 frame/second, JPEG 50% quality
- **Tools:** start_rep_counting, stop_rep_counting, get_rep_count, play_music, stop_music, change_music, generate_exercise_guide

### AudioManager

Replaces Android AudioRecord/AudioTrack with `AVAudioEngine`:
- Input node at 16kHz mono for mic capture
- `AVAudioPlayerNode` at 24kHz for Gemini output
- `AVAudioSession` category: `.playAndRecord` with `.duckOthers`
- Interrupt handling for phone calls / Siri

### PoseDetectionManager

MediaPipe iOS SDK (`PoseLandmarker`):
- Same `pose_landmarker_full.task` model bundled in app
- Angle-based rep counting (elbow joint angles for bicep curls)
- Dual-arm simultaneous tracking
- HUD overlay as SwiftUI overlay view

### WearablesManager

Wraps Meta DAT SDK (`MWDATCore` + `MWDATCamera`):
- Registration via URL scheme callback (`gymbro://`)
- `AutoDeviceSelector` for automatic glasses discovery
- `StreamSession` for video frames
- Frames routed to Gemini (1/sec) + pose detection
- Photo capture support

### PhoneCameraManager

`AVCaptureSession` fallback (replaces CameraX):
- Front/back camera switching
- Frame output via `AVCaptureVideoDataOutput` delegate
- JPEG conversion for Gemini streaming

### StreamingManager

Background streaming support:
- Background modes: `bluetooth-peripheral`, `external-accessory`
- `AVAssetWriter` for MP4 recording (replaces MediaCodec)

## ViewModel Layer

| Android ViewModel | iOS @Observable | Responsibility |
|---|---|---|
| OnboardingViewModel | OnboardingViewModel | 10-step flow, Supabase auth, profile save |
| StreamViewModel | StreamViewModel | Camera lifecycle, frame routing, recording |
| GeminiSessionViewModel | GeminiSessionViewModel | Gemini connection, audio/video, tool calls |
| WearablesViewModel | WearablesViewModel | Device state, registration, permissions |
| DashboardViewModel | DashboardViewModel | Workout history, stats queries |

State management: `@Observable` macro (iOS 17+) replaces Kotlin StateFlow.

## UI Layer

All SwiftUI, matching Android Compose screens:

- **Onboarding** — 10-step flow: splash, auth, name, gender, stats, goal, experience, workouts, mirror photo, completion
- **MainTabView** — 3 tabs: Dashboard, Workout, Profile
- **StreamView** — Live camera feed + Gemini overlay + controls + rep counter HUD
- **DashboardView** — Greeting, workout stats, quick start
- **ProfileView** — User stats, mirror photo, logout
- **SettingsView** — API key, system prompt, URLs

## Dependencies (SPM)

```swift
.package(url: "https://github.com/facebook/meta-wearables-dat-ios", exact: "0.4.0"),
.package(url: "https://github.com/supabase-community/supabase-swift", from: "2.0.0"),
.package(url: "https://github.com/google/mediapipe", from: "0.10.0"),
```

## Technology Mapping

| Android | iOS |
|---------|-----|
| Jetpack Compose | SwiftUI |
| Room (SQLite) | SwiftData |
| Kotlin Coroutines | Swift async/await + AsyncStream |
| StateFlow | @Observable / @Published |
| SharedPreferences | @AppStorage / UserDefaults |
| OkHttp WebSocket | URLSessionWebSocketTask |
| CameraX | AVCaptureSession |
| MediaCodec (MP4) | AVAssetWriter |
| Foreground Service | Background Modes |
| Navigation Compose | NavigationStack |
| Gson | Codable |

## Permissions (Info.plist)

- NSCameraUsageDescription
- NSMicrophoneUsageDescription
- NSBluetoothAlwaysUsageDescription
- Background modes: bluetooth-peripheral, external-accessory
- UISupportedExternalAccessoryProtocols: com.meta.ar.wearable
- MWDAT config: AppLinkURLScheme, MetaAppID, ClientToken, TeamID
