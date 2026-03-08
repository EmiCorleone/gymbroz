export const POSE_CONNECTIONS = [
  [11, 12], [11, 13], [13, 15], [12, 14], [14, 16],
  [11, 23], [12, 24], [23, 24], [23, 25], [24, 26],
  [25, 27], [26, 28],
];

const ARM_LANDMARKS = new Set([11, 12, 13, 14, 15, 16]);

export function drawPoseSkeleton(
  ctx: CanvasRenderingContext2D,
  landmarks: number[][],
  width: number,
  height: number
) {
  ctx.clearRect(0, 0, width, height);

  // Draw body connections
  ctx.strokeStyle = "#00ff88";
  ctx.lineWidth = 3;
  for (const [a, b] of POSE_CONNECTIONS) {
    if (!landmarks[a] || !landmarks[b]) continue;
    if (landmarks[a][3] < 0.3 || landmarks[b][3] < 0.3) continue;
    ctx.beginPath();
    ctx.moveTo(landmarks[a][0] * width, landmarks[a][1] * height);
    ctx.lineTo(landmarks[b][0] * width, landmarks[b][1] * height);
    ctx.stroke();
  }

  // Draw arm connections highlighted
  ctx.strokeStyle = "#ff4400";
  ctx.lineWidth = 4;
  for (const [a, b] of [[11, 13], [13, 15], [12, 14], [14, 16]]) {
    if (!landmarks[a] || !landmarks[b]) continue;
    if (landmarks[a][3] < 0.3 || landmarks[b][3] < 0.3) continue;
    ctx.beginPath();
    ctx.moveTo(landmarks[a][0] * width, landmarks[a][1] * height);
    ctx.lineTo(landmarks[b][0] * width, landmarks[b][1] * height);
    ctx.stroke();
  }

  // Draw joint dots
  for (let i = 0; i < landmarks.length; i++) {
    const lm = landmarks[i];
    if (!lm || lm[3] < 0.3) continue;
    ctx.fillStyle = ARM_LANDMARKS.has(i) ? "#ff4400" : "#00ff88";
    ctx.beginPath();
    ctx.arc(lm[0] * width, lm[1] * height, ARM_LANDMARKS.has(i) ? 8 : 4, 0, Math.PI * 2);
    ctx.fill();
  }
}
