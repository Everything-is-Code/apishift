# GateForge frontend

Angular 19 SPA for the GateForge migration console. Served by Nginx in production; `ng serve` during local development.

## Prerequisites

- Node.js 20+
- Backend API running at `http://localhost:8080` (see root [README](../README.md) or `./scripts/dev/local-up.sh`)

## Development server

```bash
npm install
npm start          # ng serve — http://localhost:4200
```

The dev server proxies `/api` to the Quarkus backend (`proxy.conf.json`).

## Project layout

| Path | Purpose |
|------|---------|
| `src/app/app.routes.ts` | Route table (`/`, `/threescale`, `/migrate`, `/chat`, `/audit`, `/settings`) |
| `src/app/core/api/` | Domain HTTP facades (`ClusterApiService`, `ThreeScaleApiService`, `MigrationApiService`, …) and DTOs in `models/` |
| `src/app/features/migration/` | Migration wizard container and `steps/` child components |
| `src/app/shared/` | Reusable UI pieces (empty until extracted from features) |
| `src/environments/version.ts` | Build-time version stamp (synced from Helm chart on release) |

Components are **standalone** (no NgModules). Migration wizard styles live in `migration-wizard.shared.scss`.

## Build and test

```bash
npm run build      # production bundle → dist/frontend
npm test           # Karma + Jasmine (ChromeHeadless in CI)
```

## Docker

The root `frontend/Dockerfile` builds the static bundle and copies it into a UBI9 Nginx image. Used by `podman-compose.yml` and the Helm chart.

## Architecture

See [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) for how the frontend fits the monorepo and planned feature-module refactors.
