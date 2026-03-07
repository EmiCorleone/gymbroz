import { useCallback, useRef, useState } from "react";
import { GoogleGenAI } from "@google/genai";

const API_KEY = process.env.REACT_APP_GEMINI_API_KEY as string;

export interface ExerciseGuideResult {
  imageDataUrl: string;
  description: string;
}

export function useExerciseGuide() {
  const [isGenerating, setIsGenerating] = useState(false);
  const [result, setResult] = useState<ExerciseGuideResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const generate = useCallback(
    async (
      videoRef: React.RefObject<HTMLVideoElement>,
      exerciseDescription: string
    ) => {
      const video = videoRef.current;
      if (!video || video.videoWidth === 0) {
        setError("Camera not active");
        return;
      }

      setIsGenerating(true);
      setError(null);
      setResult(null);

      try {
        // Capture current frame from video
        if (!canvasRef.current) {
          canvasRef.current = document.createElement("canvas");
        }
        const canvas = canvasRef.current;
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext("2d")!;
        ctx.drawImage(video, 0, 0);
        const dataUrl = canvas.toDataURL("image/jpeg", 0.9);
        const base64Image = dataUrl.split(",")[1];

        const ai = new GoogleGenAI({ apiKey: API_KEY });

        const response = await ai.models.generateContent({
          model: "gemini-2.5-flash-image",
          contents: [
            {
              role: "user",
              parts: [
                {
                  inlineData: {
                    mimeType: "image/jpeg",
                    data: base64Image,
                  },
                },
                {
                  text: `You are a professional fitness coach and image editor.
Look at this photo of a person near a gym machine/equipment.

Generate a NEW image that shows the SAME person in the SAME location, but now performing the exercise "${exerciseDescription}" with CORRECT form on the machine/equipment visible in the photo.

The generated image should:
- Show proper body positioning and form for the exercise
- Keep the same gym environment/background
- Clearly demonstrate the correct posture and grip
- Be realistic and helpful as a visual guide

Also provide a brief 1-2 sentence text description of the key form cues.`,
                },
              ],
            },
          ],
          config: {
            responseModalities: ["text", "image"],
          },
        });

        let imageDataUrl = "";
        let description = "";

        if (response.candidates && response.candidates[0]) {
          const parts = response.candidates[0].content?.parts || [];
          for (const part of parts) {
            if (part.inlineData) {
              const mimeType = part.inlineData.mimeType || "image/png";
              imageDataUrl = `data:${mimeType};base64,${part.inlineData.data}`;
            }
            if (part.text) {
              description += part.text;
            }
          }
        }

        if (imageDataUrl) {
          setResult({ imageDataUrl, description });
        } else {
          setError(
            description || "No image was generated. Try a different prompt."
          );
        }
      } catch (err: any) {
        console.error("Exercise guide generation error:", err);
        setError(err.message || "Failed to generate exercise guide");
      } finally {
        setIsGenerating(false);
      }
    },
    []
  );

  const dismiss = useCallback(() => {
    setResult(null);
    setError(null);
  }, []);

  return { generate, dismiss, isGenerating, result, error };
}
