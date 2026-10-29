import { defineConfig, devices } from '@playwright/test';

// 默认连本机 8080；CI 和手工联调都可以用 E2E_BASE_URL 覆盖。
const baseURL = process.env.E2E_BASE_URL || 'http://127.0.0.1:8080';
// 本机没有下载 Chromium 时可以设 E2E_CHANNEL=msedge / chrome，直接驱动已装浏览器。
const channel = process.env.E2E_CHANNEL;

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  timeout: 90_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI
    ? [['list'], ['html', { open: 'never' }]]
    : [['list']],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'zh-CN',
  },
  projects: [
    // 先跑 seed.setup.ts：等应用完成 Flyway 迁移后，用 SQL 准备榜单数据
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], ...(channel ? { channel } : {}) },
      dependencies: ['setup'],
    },
  ],
  // 默认由 Playwright 拉起 jar；已有服务（E2E_NO_WEBSERVER=1 或本机复用）时不动它
  webServer: process.env.E2E_NO_WEBSERVER
    ? undefined
    : {
        command: 'node ./start-app.mjs',
        url: `${baseURL}/actuator/health`,
        reuseExistingServer: !process.env.CI,
        timeout: 180_000,
        env: process.env as Record<string, string>,
      },
});
