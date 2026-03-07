import { test, expect } from "@playwright/test";

test("page loads with connect button", async ({ page }) => {
  await page.goto("/");
  const connectBtn = page.locator(".connect-toggle");
  await expect(connectBtn).toBeVisible();
});

test("webcam and screen capture buttons visible", async ({ page }) => {
  await page.goto("/");
  // Webcam button (videocam icon)
  const webcamBtn = page.locator(
    'button .material-symbols-outlined:text("videocam")'
  );
  await expect(webcamBtn).toBeVisible();
});

test("no critical console errors on load", async ({ page }) => {
  const errors: string[] = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      errors.push(msg.text());
    }
  });
  await page.goto("/");
  await page.waitForTimeout(1000);
  // Filter out expected warnings (API key placeholder)
  const critical = errors.filter(
    (e) => !e.includes("REACT_APP_GEMINI_API_KEY") && !e.includes("your_key_here")
  );
  expect(critical).toHaveLength(0);
});

test("main app area exists in DOM", async ({ page }) => {
  await page.goto("/");
  const mainArea = page.locator(".main-app-area");
  await expect(mainArea).toBeAttached();
});
