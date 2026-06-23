#!/usr/bin/env node
/**
 * Capture ApiShift UI screenshots for docs/assets/screenshots/.
 *
 * Prerequisites: Podman, podman-compose, curl, bash.
 *
 * Usage:
 *   cd scripts/docs && npm ci && npx playwright install chromium
 *   npm run capture-screenshots
 *
 * Options:
 *   --skip-stack   Skip local-up and E2E seed (stack already running with fixture data)
 */
import { chromium } from '@playwright/test';
import { spawnSync } from 'node:child_process';
import { existsSync, mkdirSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(__dirname, '../..');
const OUT_DIR = path.join(ROOT, 'docs/assets/screenshots');
const BASE_URL = process.env.APISHIFT_UI_URL || 'http://localhost:4200';
const SKIP_STACK = process.argv.includes('--skip-stack');

function runShell(script) {
  const result = spawnSync('bash', ['-lc', script], {
    cwd: ROOT,
    stdio: 'inherit',
    env: process.env,
  });
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

async function capture(page, file, urlPath, readyText) {
  await page.goto(`${BASE_URL}${urlPath}`, { waitUntil: 'networkidle', timeout: 120_000 });
  await page.getByText(readyText, { exact: false }).first().waitFor({ timeout: 60_000 });
  const out = path.join(OUT_DIR, file);
  await page.screenshot({ path: out, fullPage: true });
  console.log(`Wrote ${path.relative(ROOT, out)}`);
}

async function main() {
  mkdirSync(OUT_DIR, { recursive: true });

  if (!SKIP_STACK) {
    if (!existsSync(path.join(ROOT, '.env'))) {
      runShell('cp .env.example .env');
    }
    runShell('./scripts/dev/local-up.sh');
    runShell('E2E_MODE=fixture ./scripts/e2e/seed-export-analyze.sh');
  } else {
    runShell('curl -sf http://localhost:8080/q/health/ready >/dev/null');
    runShell('curl -sf http://localhost:4200 >/dev/null');
  }

  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  await capture(page, 'dashboard.png', '/', 'Welcome to ApiShift');
  await capture(page, 'threescale-explorer.png', '/threescale', '3scale');
  await capture(page, 'migration-wizard.png', '/migrate', 'Select products');
  await capture(page, 'chat-assistant.png', '/chat', 'ApiShift AI');
  await capture(page, 'audit-log.png', '/audit', 'Audit log');

  await page.goto(`${BASE_URL}/migrate`, { waitUntil: 'networkidle', timeout: 120_000 });
  await page.getByText('Import offline export').click();
  await page.locator('.export-import-panel').waitFor({ timeout: 30_000 });
  await page.screenshot({
    path: path.join(OUT_DIR, 'migration-step1.png'),
    fullPage: true,
  });
  console.log('Wrote docs/assets/screenshots/migration-step1.png');

  await browser.close();
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
