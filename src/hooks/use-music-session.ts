import { useCallback, useRef, useState } from "react";
import { GoogleGenAI } from "@google/genai";
import { MusicAudioStreamer } from "../lib/music-audio-streamer";

export interface MusicConfig {
  bpm?: number;
}

const API_KEY = process.env.REACT_APP_GEMINI_API_KEY as string;

export function useMusicSession() {
  const sessionRef = useRef<any>(null);
  const streamerRef = useRef<MusicAudioStreamer | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentPrompt, setCurrentPrompt] = useState("");

  const getStreamer = useCallback(() => {
    if (!streamerRef.current) {
      streamerRef.current = new MusicAudioStreamer();
    }
    return streamerRef.current;
  }, []);

  const connectAndPlay = useCallback(
    async (prompt: string, config: MusicConfig = {}) => {
      // Stop existing session
      if (sessionRef.current) {
        try {
          sessionRef.current.stop();
          sessionRef.current.close();
        } catch (e) {
          // ignore
        }
      }

      const musicClient = new GoogleGenAI({
        apiKey: API_KEY,
        httpOptions: { apiVersion: "v1alpha" },
      });

      const streamer = getStreamer();
      await streamer.resume();

      const session = await musicClient.live.music.connect({
        model: "models/lyria-realtime-exp",
        callbacks: {
          onmessage: (message: any) => {
            const audioData = message?.serverContent?.audioChunks;
            if (audioData && audioData.length > 0) {
              for (const chunk of audioData) {
                if (chunk.data) {
                  // Decode base64 to Uint8Array
                  const binary = atob(chunk.data);
                  const bytes = new Uint8Array(binary.length);
                  for (let i = 0; i < binary.length; i++) {
                    bytes[i] = binary.charCodeAt(i);
                  }
                  streamer.addPCM16Stereo(bytes);
                }
              }
            }
          },
          onerror: (e: any) => {
            console.error("Music session error:", e);
          },
          onclose: () => {
            setIsPlaying(false);
          },
        },
      });

      sessionRef.current = session;

      await session.setWeightedPrompts({
        weightedPrompts: [{ text: prompt, weight: 1.0 }],
      });

      await session.setMusicGenerationConfig({
        musicGenerationConfig: {
          bpm: config.bpm || 120,
          temperature: 1.1,
        },
      });

      session.play();
      setIsPlaying(true);
      setCurrentPrompt(prompt);
    },
    [getStreamer]
  );

  const updatePrompt = useCallback(async (prompt: string) => {
    if (sessionRef.current) {
      await sessionRef.current.setWeightedPrompts({
        weightedPrompts: [{ text: prompt, weight: 1.0 }],
      });
      setCurrentPrompt(prompt);
    }
  }, []);

  const updateConfig = useCallback(async (config: MusicConfig) => {
    if (sessionRef.current) {
      await sessionRef.current.setMusicGenerationConfig({
        musicGenerationConfig: {
          bpm: config.bpm || 120,
        },
      });
    }
  }, []);

  const stop = useCallback(() => {
    if (sessionRef.current) {
      try {
        sessionRef.current.stop();
        sessionRef.current.close();
      } catch (e) {
        // ignore
      }
      sessionRef.current = null;
    }
    streamerRef.current?.stop();
    setIsPlaying(false);
    setCurrentPrompt("");
  }, []);

  const duck = useCallback(() => {
    streamerRef.current?.duck();
  }, []);

  const unduck = useCallback(() => {
    streamerRef.current?.unduck();
  }, []);

  return {
    connectAndPlay,
    updatePrompt,
    updateConfig,
    stop,
    duck,
    unduck,
    isPlaying,
    currentPrompt,
  };
}
