import { defineConfig, devices } from '@playwright/test';

// Runs inside the Docker network of the deployed environment (see build.sh e2e).
export default defineConfig({
  testDir: './tests',
  outputDir: '/tmp/test-results',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: process.env.BASE_URL ?? 'http://bestellen-frontend:8080',
    trace: 'off',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
