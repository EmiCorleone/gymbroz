# AI Gym Assistant

A real-time AI-powered gym coach built on **Gemini Live API**. Designed for AI glasses, it sees through your camera, counts your reps with computer vision, generates workout music on the fly, and shows you proper exercise form on any machine.

```
┌─────────────────────────────────────────────┐
│            Gemini Live API                  │
│        voice + video + tool calling         │
│                                             │
│   ┌─────────┐  ┌─────────┐  ┌───────────┐  │
│   │   Rep   │  │  Music  │  │  Exercise  │  │
│   │ Counter │  │ Player  │  │   Guide    │  │
│   │MediaPipe│  │  Lyria  │  │  Imagen    │  │
│   └─────────┘  └─────────┘  └───────────┘  │
└─────────────────────────────────────────────┘
```

## Features

### Rep Counting (MediaPipe Pose)
Tracks your body pose in real-time using MediaPipe's full pose landmarker running entirely in the browser. Counts reps by measuring the angle at your elbow joint — both left and right arms are tracked simultaneously. The model detects when you curl (angle drops below threshold) and extend (angle rises above threshold) to count each rep.

- Skeleton overlay with highlighted exercise keypoints
- Live angle readout on both elbows
- Rep count HUD with stage indicator (UP/DOWN)
- Gemini proactively starts counting when it sees you exercising

### Music Generation (Lyria RealTime)
Generates instrumental workout music in real-time using Google's Lyria model. The music streams at 48kHz stereo through a separate audio context, with automatic volume ducking when Gemini speaks so you always hear your coach.

- Text-prompt driven music style (e.g. "intense EDM workout beats")
- Adjustable BPM
- Plays alongside Gemini voice without interference

### Exercise Guide (Gemini Image Generation)
Point your camera at any gym machine and ask "how do I use this?" — the system captures a frame and sends it to Gemini's image generation model, which produces a visual guide showing correct exercise form on that specific machine.

- Camera frame capture + contextual image generation
- Full-screen overlay with form description
- Works with any equipment the camera can see

## Quick Start

### Prerequisites
- Node.js 18+
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/apikey)

### Setup

```bash
# Clone the repo
git clone <your-repo-url>
cd ai_gym

# Install dependencies
npm install

# Set your API key
echo "REACT_APP_GEMINI_API_KEY=your_key_here" > .env

# Start the dev server
npm start
```

Open [http://localhost:3000](http://localhost:3000) in Chrome.

### Usage

1. Click the **play button** to connect to Gemini
2. Click the **camera icon** to enable your webcam (or stream from the Android sample app)
3. **Talk to your coach** — it hears you and sees your camera feed

### Meta AI Glasses Integration (Android Companion)
This project includes an Android companion app built with the Meta Wearables Device Access Toolkit to stream video directly from your Meta AI glasses.
- Navigate to the `android/` directory and open the project in Android Studio.
- Turn 'Developer Mode' on in your Meta AI app.
- Run the app and press "Connect" to stream your POV camera feed into the gym assistant.
- *Refer to `android/README.md` for full setup instructions.*

Try saying:
- *"Count my bicep curls"* — activates pose detection and rep counting
- *"Play some intense workout music"* — starts real-time music generation
- *"How do I use this machine?"* — generates a visual exercise guide
- *"Stop counting"* / *"Stop the music"* — deactivates tools

The assistant is **proactive** — it will automatically start counting reps when it sees you exercising, and offer form guidance when you look unsure near a machine.

## Architecture

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Conversational AI | Gemini Live API (`gemini-2.5-flash-native-audio-latest`) | Real-time voice + video conversation with function calling |
| Pose Estimation | MediaPipe Pose Landmarker (full model, in-browser WASM) | Detects body landmarks at ~30 FPS from webcam |
| Rep Counting | Custom TypeScript engine | Angle-based state machine (shoulder-elbow-wrist) |
| Music Generation | Lyria RealTime (`models/lyria-realtime-exp`) | Streams instrumental music at 48kHz stereo |
| Exercise Guide | Gemini Image Generation (`gemini-2.5-flash-image`) | Generates form guide images from camera frames |
| Frontend | React 18 + TypeScript + CRA | Single-page app, no backend needed |
| Smart Glasses Capture | Android / Kotlin / Meta Wearables DAT | Connects and streams video from Meta AI Glasses |

### Gemini Tools

The app registers 7 function declarations that Gemini can call:

| Tool | What it does |
|------|-------------|
| `start_rep_counting(exercise)` | Activate pose detection + rep counter |
| `stop_rep_counting()` | Stop and report final count |
| `get_rep_count()` | Query current reps without stopping |
| `play_music(prompt, bpm)` | Start Lyria music generation |
| `stop_music()` | Stop music playback |
| `change_music(prompt?, bpm?)` | Update music style/tempo mid-stream |
| `generate_exercise_guide(exercise_description)` | Capture frame + generate form guide image |

## Project Structure

```
android/                                  # Meta AI Glasses companion app
│   └── app/src/main/                     # Android Kotlin entry point
src/
├── components/
│   ├── gym-tools/GymTools.tsx            # Central orchestrator — tool declarations + handlers
│   ├── rep-counter/RepCounter.tsx        # Pose overlay + rep count HUD
│   ├── music-player/MusicPlayer.tsx      # Music status indicator
│   ├── exercise-guide/ExerciseGuide.tsx  # Generated image overlay
│   ├── control-tray/ControlTray.tsx      # Webcam/mic/connect controls (upstream)
│   └── side-panel/SidePanel.tsx          # Log panel (upstream)
├── hooks/
│   ├── use-pose-detection.ts             # MediaPipe PoseLandmarker hook
│   ├── use-music-session.ts              # Lyria RealTime session hook
│   ├── use-exercise-guide.ts             # Image generation hook
│   └── use-live-api.ts                   # Gemini Live API connection (upstream)
├── lib/
│   ├── rep-counting-engine.ts            # Angle calculation + rep state machine
│   ├── music-audio-streamer.ts           # 48kHz stereo audio playback
│   ├── audio-streamer.ts                 # 24kHz mono Gemini voice (upstream)
│   └── genai-live-client.ts              # WebSocket client (upstream)
├── contexts/
│   └── LiveAPIContext.tsx                # Gemini Live API React context (upstream)
└── App.tsx                               # Entry point
```

## Testing

```bash
# Run Playwright e2e tests
npm run test:e2e

# Tests cover:
# - Page loads with connect button
# - Webcam controls visible
# - No console errors
# - Angle calculation correctness (90 degree test)
# - Rep counting state machine (3 reps simulation)
# - Rep counter overlay hidden by default
# - Music player hidden by default
```

## How Rep Counting Works

The engine measures the angle at your **elbow** between three points: shoulder, elbow, wrist.

```
Arm extended:  ~150 degrees        Arm curled:  ~60 degrees
shoulder──elbow──wrist             shoulder──elbow
                                                \
                                                wrist
```

One rep = curl below 80 degrees (DOWN) then extend above 120 degrees (UP).

Both arms are tracked simultaneously — the engine picks whichever arm shows more movement. The HUD displays live angles so you can see exactly what's being detected.

## Credits

Built on top of [google-gemini/live-api-web-console](https://github.com/google-gemini/live-api-web-console). Rep counting logic inspired by [Ultralytics AI Gym](https://docs.ultralytics.com/reference/solutions/ai_gym/).

_This is an experiment showcasing the Gemini Live API, not an official Google product._
