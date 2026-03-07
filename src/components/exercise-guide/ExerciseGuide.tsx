import { memo } from "react";
import { ExerciseGuideResult } from "../../hooks/use-exercise-guide";
import "./exercise-guide.scss";

interface ExerciseGuideProps {
  isGenerating: boolean;
  result: ExerciseGuideResult | null;
  error: string | null;
  onDismiss: () => void;
}

function ExerciseGuideComponent({
  isGenerating,
  result,
  error,
  onDismiss,
}: ExerciseGuideProps) {
  if (!isGenerating && !result && !error) return null;

  return (
    <div className="exercise-guide-overlay" onClick={onDismiss}>
      <div
        className="exercise-guide-content"
        onClick={(e) => e.stopPropagation()}
      >
        {isGenerating && (
          <div className="generating">
            <div className="spinner" />
            <p>Analyzing the machine and generating exercise guide...</p>
          </div>
        )}

        {error && (
          <div className="guide-error">
            <p>{error}</p>
            <button onClick={onDismiss}>Dismiss</button>
          </div>
        )}

        {result && (
          <div className="guide-result">
            <img
              src={result.imageDataUrl}
              alt="Exercise form guide"
              className="guide-image"
            />
            {result.description && (
              <div className="guide-description">
                <p>{result.description}</p>
              </div>
            )}
            <button className="dismiss-btn" onClick={onDismiss}>
              Got it
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export const ExerciseGuide = memo(ExerciseGuideComponent);
