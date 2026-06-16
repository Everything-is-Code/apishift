# GateForge scripts

Automation for local development, CI, E2E, fixtures, releases, and docs screenshots.

Top-level paths such as `./scripts/local-up.sh` are **backward-compatible wrappers** that delegate to the canonical scripts below. Prefer the category paths for new references.

## Layout

| Directory | Purpose |
|-----------|---------|
| `lib/` | Sourceable helpers (`common.sh`, `version.sh`, `helm-repo-url.sh`) — do not execute directly |
| `dev/` | Local Podman Compose stack |
| `ci/` | CI verification helpers |
| `e2e/` | End-to-end lab pipelines |
| `fixtures/` | Test fixture sync from 3scaleextract |
| `release/` | Version propagation across artifacts |
| `docs/` | Playwright screenshot capture |

## Scripts

| Script | Category | Purpose | Prerequisites | Example |
|--------|----------|---------|---------------|---------|
| `lib/common.sh` | lib | `ROOT`, `die()`, `log()` for other scripts | bash | `source scripts/lib/common.sh` |
| `lib/version.sh` | lib | Read semver from `helm/gateforge/Chart.yaml` | bash | `source scripts/lib/version.sh && echo $VERSION` |
| `lib/helm-repo-url.sh` | lib | Export `HELM_REPO_URL` for GitHub Pages | bash | `source scripts/lib/helm-repo-url.sh` |
| `dev/local-up.sh` | dev | Build and start stack via podman-compose | Podman, podman-compose, `.env` | `./scripts/local-up.sh` |
| `dev/local-down.sh` | dev | Stop local stack | podman-compose | `./scripts/local-down.sh` |
| `ci/verify-export-minimal-fixture.sh` | ci | Verify export-minimal tarball SHA256 | bash, sha256sum | `./scripts/verify-export-minimal-fixture.sh` |
| `e2e/seed-export-analyze.sh` | e2e | 3scaleextract seed → analyze via GateForge API | curl, jq; optional Go/zip for live | `E2E_MODE=fixture ./scripts/e2e-seed-export-analyze.sh` |
| `fixtures/sync-export-minimal-fixture.sh` | fixtures | Copy export-minimal from 3scaleextract | 3scaleextract checkout | `THREESCALEEXTRACT_ROOT=../3scaleextract ./scripts/sync-export-minimal-fixture.sh` |
| `release/sync-versions.sh` | release | Propagate Chart.yaml version to Maven, npm, Helm, docs | bash, sed | `./scripts/sync-versions.sh` |
| `docs/capture-screenshots.mjs` | docs | Capture UI PNGs for `docs/assets/screenshots/` | Node 20+, Playwright, Podman stack | `cd scripts && npm ci && npm run capture-screenshots` |

## Backward-compatible wrappers

These paths still work and forward to the canonical script:

- `scripts/local-up.sh` → `dev/local-up.sh`
- `scripts/local-down.sh` → `dev/local-down.sh`
- `scripts/verify-export-minimal-fixture.sh` → `ci/verify-export-minimal-fixture.sh`
- `scripts/e2e-seed-export-analyze.sh` → `e2e/seed-export-analyze.sh`
- `scripts/sync-export-minimal-fixture.sh` → `fixtures/sync-export-minimal-fixture.sh`
- `scripts/sync-versions.sh` → `release/sync-versions.sh`
- `scripts/version.sh` → sources `lib/version.sh`
- `scripts/helm-repo-url.sh` → sources `lib/helm-repo-url.sh`
- `scripts/capture-screenshots.mjs` → imports `docs/capture-screenshots.mjs`
