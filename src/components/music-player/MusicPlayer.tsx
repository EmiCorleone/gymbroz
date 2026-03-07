import { memo } from "react";
import "./music-player.scss";

interface MusicPlayerProps {
  active: boolean;
  prompt: string;
  isPlaying: boolean;
}

function MusicPlayerComponent({ active, prompt, isPlaying }: MusicPlayerProps) {
  if (!active) return null;

  return (
    <div className="music-player-indicator">
      <span className={`music-icon ${isPlaying ? "playing" : ""}`}>
        &#9835;
      </span>
      <div className="music-info">
        <span className="music-label">
          {isPlaying ? "Playing" : "Starting..."}
        </span>
        <span className="music-prompt">{prompt}</span>
      </div>
    </div>
  );
}

export const MusicPlayer = memo(MusicPlayerComponent);
