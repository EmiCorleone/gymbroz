export interface ExerciseConfig {
  name: string;
  // Each entry is a [shoulder, elbow, wrist] triplet to track
  keypointSets: [number, number, number][];
  upAngle: number;
  downAngle: number;
}

export interface PersonState {
  stage: "up" | "down" | null;
  repCount: number;
  currentAngle: number;
  activeArm: string;
}

export const EXERCISE_PRESETS: Record<string, ExerciseConfig> = {
  bicep_curl: {
    name: "Bicep Curl",
    keypointSets: [
      [11, 13, 15], // left shoulder, left elbow, left wrist
      [12, 14, 16], // right shoulder, right elbow, right wrist
    ],
    upAngle: 120,
    downAngle: 80,
  },
};

interface Point {
  x: number;
  y: number;
  z: number;
}

export class RepCountingEngine {
  private states: Map<number, PersonState> = new Map();

  calculateAngle(a: Point, b: Point, c: Point): number {
    const ba = { x: a.x - b.x, y: a.y - b.y, z: a.z - b.z };
    const bc = { x: c.x - b.x, y: c.y - b.y, z: c.z - b.z };

    const dot = ba.x * bc.x + ba.y * bc.y + ba.z * bc.z;
    const magBA = Math.sqrt(ba.x ** 2 + ba.y ** 2 + ba.z ** 2);
    const magBC = Math.sqrt(bc.x ** 2 + bc.y ** 2 + bc.z ** 2);

    if (magBA === 0 || magBC === 0) return 0;

    const cosAngle = Math.max(-1, Math.min(1, dot / (magBA * magBC)));
    return (Math.acos(cosAngle) * 180) / Math.PI;
  }

  processFrame(
    allLandmarks: Array<Array<{ x: number; y: number; z: number; visibility?: number }>>,
    config: ExerciseConfig
  ): PersonState[] {
    const results: PersonState[] = [];

    for (let personIdx = 0; personIdx < allLandmarks.length; personIdx++) {
      const landmarks = allLandmarks[personIdx];
      if (!landmarks) continue;

      // Calculate angle for each keypoint set (e.g. left arm and right arm)
      // Pick the arm with the most movement (smallest angle = most curled)
      let bestAngle = -1;
      let bestArmLabel = "";

      for (let setIdx = 0; setIdx < config.keypointSets.length; setIdx++) {
        const [kpA, kpB, kpC] = config.keypointSets[setIdx];
        if (!landmarks[kpA] || !landmarks[kpB] || !landmarks[kpC]) continue;

        const angle = this.calculateAngle(
          landmarks[kpA],
          landmarks[kpB],
          landmarks[kpC]
        );

        // Pick the arm that's curling more (lower angle = more curled)
        // On the first iteration just take whatever we get
        if (bestAngle < 0 || angle < bestAngle) {
          bestAngle = angle;
          bestArmLabel = setIdx === 0 ? "L" : "R";
        }
      }

      if (bestAngle < 0) continue; // No valid keypoints found

      let state = this.states.get(personIdx) || {
        stage: null,
        repCount: 0,
        currentAngle: 0,
        activeArm: "",
      };

      state.currentAngle = bestAngle;
      state.activeArm = bestArmLabel;

      if (bestAngle < config.downAngle) {
        state.stage = "down";
      } else if (bestAngle > config.upAngle && state.stage === "down") {
        state.stage = "up";
        state.repCount += 1;
      }

      this.states.set(personIdx, state);
      results.push({ ...state });
    }

    return results;
  }

  reset(): void {
    this.states.clear();
  }

  getStates(): PersonState[] {
    return Array.from(this.states.values()).map((s) => ({ ...s }));
  }
}
