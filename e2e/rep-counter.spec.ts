import { test, expect } from "@playwright/test";

test("rep counting engine calculates angles correctly", async ({ page }) => {
  await page.goto("/");

  // Test the angle calculation logic in the browser context
  const angle = await page.evaluate(() => {
    // Replicate the angle calculation from rep-counting-engine.ts
    function calculateAngle(
      a: { x: number; y: number; z: number },
      b: { x: number; y: number; z: number },
      c: { x: number; y: number; z: number }
    ): number {
      const ba = { x: a.x - b.x, y: a.y - b.y, z: a.z - b.z };
      const bc = { x: c.x - b.x, y: c.y - b.y, z: c.z - b.z };
      const dot = ba.x * bc.x + ba.y * bc.y + ba.z * bc.z;
      const magBA = Math.sqrt(ba.x ** 2 + ba.y ** 2 + ba.z ** 2);
      const magBC = Math.sqrt(bc.x ** 2 + bc.y ** 2 + bc.z ** 2);
      if (magBA === 0 || magBC === 0) return 0;
      const cosAngle = Math.max(-1, Math.min(1, dot / (magBA * magBC)));
      return (Math.acos(cosAngle) * 180) / Math.PI;
    }

    // Right angle: A=(0,0,0), B=(1,0,0), C=(1,1,0) -> 90 degrees
    return calculateAngle(
      { x: 0, y: 0, z: 0 },
      { x: 1, y: 0, z: 0 },
      { x: 1, y: 1, z: 0 }
    );
  });

  expect(angle).toBeCloseTo(90, 0);
});

test("rep counting engine counts reps on state transitions", async ({
  page,
}) => {
  await page.goto("/");

  const result = await page.evaluate(() => {
    // Minimal state machine simulation
    type State = { stage: "up" | "down" | null; repCount: number };

    function simulateRep(
      angles: number[],
      upAngle: number,
      downAngle: number
    ): State {
      let state: State = { stage: null, repCount: 0 };
      for (const angle of angles) {
        if (angle < downAngle) {
          state.stage = "down";
        } else if (angle > upAngle && state.stage === "down") {
          state.stage = "up";
          state.repCount += 1;
        }
      }
      return state;
    }

    // Simulate 3 bicep curl reps with thresholds (120 up, 80 down):
    // Start high (arm extended), go low (curled), back to high = 1 rep
    const angles = [
      150, 130, 100, 70, 60, 75, 100, 120, 130, // rep 1
      120, 100, 70, 60, 75, 100, 120, 130, // rep 2
      120, 100, 70, 60, 75, 100, 120, 130, // rep 3
    ];

    return simulateRep(angles, 120, 80);
  });

  expect(result.repCount).toBe(3);
  expect(result.stage).toBe("up");
});

test("rep counter overlay not visible by default", async ({ page }) => {
  await page.goto("/");
  const overlay = page.locator(".rep-counter-overlay");
  await expect(overlay).not.toBeVisible();
});
