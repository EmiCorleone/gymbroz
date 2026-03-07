import { test, expect } from "@playwright/test";

test("music player indicator not visible by default", async ({ page }) => {
  await page.goto("/");
  const indicator = page.locator(".music-player-indicator");
  await expect(indicator).not.toBeVisible();
});
