import { useEffect, useRef, useState, memo, useCallback, RefObject } from "react";
import { useLiveAPIContext } from "../../contexts/LiveAPIContext";
import {
  FunctionDeclaration,
  LiveServerToolCall,
  Modality,
  Type,
} from "@google/genai";
import { RepCounter } from "../rep-counter/RepCounter";
import { MusicPlayer } from "../music-player/MusicPlayer";
import { ExerciseGuide } from "../exercise-guide/ExerciseGuide";
import {
  ExerciseConfig,
  PersonState,
  EXERCISE_PRESETS,
} from "../../lib/rep-counting-engine";
import { useMusicSession } from "../../hooks/use-music-session";
import { useExerciseGuide } from "../../hooks/use-exercise-guide";

// --- Function Declarations ---

const startRepCountingDeclaration: FunctionDeclaration = {
  name: "start_rep_counting",
  description:
    "Start counting exercise repetitions using the camera. Call this when the user wants to track their workout reps. The camera must be active. Currently supports bicep_curl.",
  parameters: {
    type: Type.OBJECT,
    properties: {
      exercise: {
        type: Type.STRING,
        description: "The type of exercise to count. Currently supported: bicep_curl",
      },
    },
    required: ["exercise"],
  },
};

const stopRepCountingDeclaration: FunctionDeclaration = {
  name: "stop_rep_counting",
  description: "Stop counting exercise repetitions and report the final count.",
  parameters: {
    type: Type.OBJECT,
    properties: {},
  },
};

const getRepCountDeclaration: FunctionDeclaration = {
  name: "get_rep_count",
  description:
    "Get the current rep count and exercise status without stopping the counter.",
  parameters: {
    type: Type.OBJECT,
    properties: {},
  },
};

const playMusicDeclaration: FunctionDeclaration = {
  name: "play_music",
  description:
    "Generate and play motivational workout music in real-time. Describe the style of music to generate. Music is instrumental only.",
  parameters: {
    type: Type.OBJECT,
    properties: {
      prompt: {
        type: Type.STRING,
        description:
          "Description of the music style, e.g. 'high energy EDM workout music with heavy bass'",
      },
      bpm: {
        type: Type.NUMBER,
        description:
          "Beats per minute, between 60 and 200. Default 120 for moderate intensity.",
      },
    },
    required: ["prompt"],
  },
};

const stopMusicDeclaration: FunctionDeclaration = {
  name: "stop_music",
  description: "Stop the currently playing workout music.",
  parameters: {
    type: Type.OBJECT,
    properties: {},
  },
};

const changeMusicDeclaration: FunctionDeclaration = {
  name: "change_music",
  description:
    "Change the style or tempo of the currently playing music without stopping.",
  parameters: {
    type: Type.OBJECT,
    properties: {
      prompt: {
        type: Type.STRING,
        description: "New music style description",
      },
      bpm: {
        type: Type.NUMBER,
        description: "New BPM (60-200)",
      },
    },
  },
};

const generateExerciseGuideDeclaration: FunctionDeclaration = {
  name: "generate_exercise_guide",
  description:
    "Takes a photo of the gym machine the user is looking at and generates an image showing proper exercise form on that machine. Use this when the user asks how to use a machine, wants to see correct form, or asks about an exercise on specific equipment.",
  parameters: {
    type: Type.OBJECT,
    properties: {
      exercise_description: {
        type: Type.STRING,
        description:
          "Description of the exercise the user wants to perform, e.g. 'chest press', 'lat pulldown', 'leg press', 'cable rows'",
      },
    },
    required: ["exercise_description"],
  },
};

const allDeclarations = [
  startRepCountingDeclaration,
  stopRepCountingDeclaration,
  getRepCountDeclaration,
  playMusicDeclaration,
  stopMusicDeclaration,
  changeMusicDeclaration,
  generateExerciseGuideDeclaration,
];

// --- Component ---

interface GymToolsProps {
  videoRef: RefObject<HTMLVideoElement>;
}

function GymToolsComponent({ videoRef }: GymToolsProps) {
  const { client, setConfig, setModel } = useLiveAPIContext();

  // Rep counter state
  const [repCounterActive, setRepCounterActive] = useState(false);
  const [exerciseConfig, setExerciseConfig] = useState<ExerciseConfig | null>(
    null
  );
  const repStateRef = useRef<PersonState[]>([]);

  // Music state
  const [musicActive, setMusicActive] = useState(false);
  const music = useMusicSession();

  // Exercise guide state
  const exerciseGuide = useExerciseGuide();

  const onRepUpdate = useCallback((states: PersonState[]) => {
    repStateRef.current = states;
  }, []);

  // Configure Gemini model with gym-specific system instruction and tools
  useEffect(() => {
    setModel("models/gemini-2.0-flash-exp-image-generation");
    setConfig({
      responseModalities: [Modality.AUDIO],
      speechConfig: {
        voiceConfig: { prebuiltVoiceConfig: { voiceName: "Aoede" } },
      },
      systemInstruction: {
        parts: [
          {
            text: `You are a motivational AI gym assistant on smart glasses. You can see through the user's camera and hear them in real time.

CORE BEHAVIOR:
1. AUTOMATICALLY start counting reps when you see the user performing an exercise — do NOT wait for them to ask. As soon as you see repetitive exercise movement (like bicep curls, arm movements, etc.), immediately call start_rep_counting with exercise "bicep_curl".
2. Use get_rep_count frequently (every few seconds) while counting is active to give verbal encouragement and updates like "Great form! That's 5 reps!"
3. When the user clearly stops exercising or says they're done, call stop_rep_counting and announce the final count.
4. If they start a new set, call start_rep_counting again — the tool can be called multiple times, it resets each time.
5. When they ask for music, use play_music with an energetic style matching the workout intensity.

EXERCISE GUIDE:
6. When the user asks how to use a gym machine, wants to see proper form, or says things like "how do I use this?", "show me how to do this exercise", or "what exercise can I do here" — call generate_exercise_guide with a description of the exercise. This will capture the current camera view and generate an image showing correct form on that machine.
7. While the image is generating, explain the key form cues verbally so the user can start getting ready.

IMPORTANT RULES:
- Be PROACTIVE — don't wait to be told to count. If you see exercise happening, start counting immediately.
- You can always restart rep counting by calling start_rep_counting again. It will reset and begin fresh.
- Be concise and energetic in your voice responses — this is real-time conversation.
- Give form feedback based on what you see through the camera.
- Provide encouragement and motivation throughout the workout.
- When near a machine, if the user seems unsure, proactively offer to show them the correct form using generate_exercise_guide.`,
          },
        ],
      },
      tools: [
        { googleSearch: {} },
        { functionDeclarations: allDeclarations },
      ],
    });
  }, [setConfig, setModel]);

  // Handle tool calls from Gemini
  useEffect(() => {
    const onToolCall = (toolCall: LiveServerToolCall) => {
      if (!toolCall.functionCalls) return;

      const responses = toolCall.functionCalls.map((fc) => {
        let output: Record<string, unknown> = { success: true };

        switch (fc.name) {
          case "start_rep_counting": {
            const exercise = (fc.args as any)?.exercise || "bicep_curl";
            const config = EXERCISE_PRESETS[exercise];
            if (config) {
              setExerciseConfig(config);
              setRepCounterActive(true);
              output = {
                success: true,
                message: `Started counting ${config.name} reps`,
              };
            } else {
              output = {
                success: false,
                error: `Unknown exercise: ${exercise}. Supported: bicep_curl`,
              };
            }
            break;
          }

          case "stop_rep_counting": {
            const finalCounts = repStateRef.current.map((s) => s.repCount);
            setRepCounterActive(false);
            setExerciseConfig(null);
            output = {
              success: true,
              totalReps: finalCounts,
              message: `Stopped. Final rep counts: ${finalCounts.join(", ") || "0"}`,
            };
            break;
          }

          case "get_rep_count": {
            const counts = repStateRef.current.map((s) => ({
              reps: s.repCount,
              stage: s.stage,
              angle: Math.round(s.currentAngle),
            }));
            output = {
              success: true,
              active: repCounterActive,
              people: counts,
            };
            break;
          }

          case "play_music": {
            const prompt = (fc.args as any)?.prompt || "energetic workout music";
            const bpm = (fc.args as any)?.bpm || 120;
            setMusicActive(true);
            music.connectAndPlay(prompt, { bpm }).catch((err: Error) => {
              console.error("Music error:", err);
            });
            output = {
              success: true,
              message: `Playing: ${prompt} at ${bpm} BPM`,
            };
            break;
          }

          case "stop_music": {
            music.stop();
            setMusicActive(false);
            output = { success: true, message: "Music stopped" };
            break;
          }

          case "change_music": {
            const newPrompt = (fc.args as any)?.prompt;
            const newBpm = (fc.args as any)?.bpm;
            if (newPrompt) {
              music.updatePrompt(newPrompt);
            }
            if (newBpm) {
              music.updateConfig({ bpm: newBpm });
            }
            output = { success: true, message: "Music updated" };
            break;
          }

          case "generate_exercise_guide": {
            const exerciseDesc =
              (fc.args as any)?.exercise_description || "exercise";
            exerciseGuide.generate(videoRef, exerciseDesc).then(() => {
              // Image will be shown via the ExerciseGuide component
            });
            output = {
              success: true,
              message: `Generating exercise guide for "${exerciseDesc}". The user will see the image on screen shortly. Describe what they should focus on while they wait.`,
            };
            break;
          }

          default:
            output = { success: false, error: `Unknown tool: ${fc.name}` };
        }

        return { response: { output }, id: fc.id, name: fc.name };
      });

      // Send tool response back to Gemini
      setTimeout(() => {
        client.sendToolResponse({ functionResponses: responses });
      }, 200);
    };

    client.on("toolcall", onToolCall);
    return () => {
      client.off("toolcall", onToolCall);
    };
  }, [client, repCounterActive, music, exerciseGuide, videoRef]);

  // Duck music volume when Gemini is speaking
  useEffect(() => {
    if (!musicActive) return;

    const onAudio = () => music.duck();
    const onInterrupted = () => music.unduck();

    client.on("audio", onAudio);
    client.on("interrupted", onInterrupted);
    return () => {
      client.off("audio", onAudio);
      client.off("interrupted", onInterrupted);
    };
  }, [client, musicActive, music]);

  return (
    <>
      <RepCounter
        videoRef={videoRef}
        exerciseConfig={exerciseConfig}
        active={repCounterActive}
        onRepUpdate={onRepUpdate}
      />
      <MusicPlayer
        active={musicActive}
        prompt={music.currentPrompt}
        isPlaying={music.isPlaying}
      />
      <ExerciseGuide
        isGenerating={exerciseGuide.isGenerating}
        result={exerciseGuide.result}
        error={exerciseGuide.error}
        onDismiss={exerciseGuide.dismiss}
      />
    </>
  );
}

export const GymTools = memo(GymToolsComponent);
