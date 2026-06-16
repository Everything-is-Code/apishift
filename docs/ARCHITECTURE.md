# GateForge architecture

GateForge is a monorepo that migrates **Red Hat 3scale API Management** configurations to **Red Hat Connectivity Link** (Kuadrant) resources on OpenShift. This document maps modules, runtime boundaries, and data flows so contributors can navigate the codebase without reading every large class first.

**Quick path for new contributors**

1. Skim the [repository layout](#repository-layout) table.
2. Follow the [migration data flow](#migration-data-flow) for the core product path.
3. Use [scripts/README.md](../scripts/README.md) for local stack and E2E commands.
4. See [architecture hardening archive](archive/2026-06-16-architecture-hardening.md) for the completed refactor initiative (Phases 1–4).

---

## Repository layout

| Path | Role | Runtime |
|------|------|---------|
| `backend/` | Quarkus REST API, migration engine, AI agent, MCP tools | JVM container (`gateforge-backend`) |
| `frontend/` | Angular SPA (dashboard, explorer, wizard, chat, audit) | Nginx container (`gateforge-frontend`) |
| `gateforge-devhub-plugin/` | RHDH backend module — catalog ingestion, migration webhooks | Dynamic plugin sidecar |
| `gateforge-devhub-frontend/` | RHDH frontend plugin — topology, component editor tabs | Dynamic plugin bundle |
| `helm/gateforge/` | OpenShift deployment (backend, frontend, PostgreSQL, Data Grid) | Helm release |
| `scripts/` | Dev, CI, E2E, fixtures, release automation | Host shell / CI |
| `docs/` | GitHub Pages site, screenshots, this file | Published static site |

**Version source of truth:** `helm/gateforge/Chart.yaml` (`version` / `appVersion`). Propagate with `./scripts/release/sync-versions.sh`.

---

## Migration data flow

```mermaid
flowchart LR
  subgraph sources [Discovery sources]
    A[3scale Admin API]
    B[K8s 3scale CRDs]
    C[Offline export tarball]
  end

  subgraph backend [Quarkus backend]
    TS[ThreeScaleService]
    EXP[service/export/*]
    MS[MigrationService]
    MR[MigrationResource]
  end

  subgraph outputs [Generated artifacts]
    P[MigrationPlan + YAML]
    DB[(PostgreSQL)]
    K8s[Target cluster apply]
  end

  A --> TS
  B --> TS
  C --> EXP --> TS
  TS --> MS
  MS --> P
  MR --> MS
  P --> DB
  P --> K8s
```

**Typical paths**

| Entry | Flow |
|-------|------|
| Live discovery | Admin API or in-cluster CRDs → `ThreeScaleService` cache (Data Grid) → wizard selects products → `POST /api/migration/analyze` |
| Offline import | Upload export zip → `ExportImportService` / `ThreeScaleExportParser` → same analyze pipeline |
| Apply | `POST /api/migration/plans/{id}/apply` → Fabric8 client + `kuadrantctl` on target cluster |
| Developer Hub | Plugin receives registration events → catalog entities enriched via `HubResource` |

---

## Backend (`backend/src/main/java/io/gateforge`)

Quarkus 3.x / Java 17. Packages follow a loose layered layout:

| Package | Responsibility |
|---------|----------------|
| `resource/` | JAX-RS REST endpoints (`/api/*`) |
| `port/threescale/` | Outbound port for 3scale Admin API (`ThreeScaleAdminPort`; HTTP adapter in `ThreeScaleAdminApiClient`) |
| `service/` | Business logic, K8s integration, 3scale clients |
| `service/export/` | Offline export parse/validate/import — **reference sub-domain** (focused classes, good test coverage) |
| `service/developerhub/` | Developer Hub catalog and scaffolder HTTP client |
| `service/generator/` | Kuadrant/Gateway API YAML builders (Gateway, HTTPRoute, AuthPolicy, PlanPolicy, RateLimitPolicy) |
| `model/` | Immutable records / DTOs shared across layers |
| `entity/` | JPA entities (plans, audit) |
| `repository/` | Panache persistence for migration plans and audit entries |
| `ai/` | LangChain4j migration agent and tool wiring |
| `mcp/` | Model Context Protocol tool definitions |

### REST surface (summary)

| Prefix | Resource | Purpose |
|--------|----------|---------|
| `/api/migration` | `MigrationResource` | Analyze, import-export, plan CRUD, apply/revert, drift |
| `/api/threescale` | `ThreeScaleResource` | Product/backend discovery, sources, refresh |
| `/api/cluster` | `ClusterResource` | OpenShift projects, target clusters, readiness |
| `/api/apicast` | `APICastResource` | APICast discovery and Istio mapping |
| `/api/hub` | `HubResource` | Developer Hub overview, topology, audit |
| `/api/chat` | `ChatResource` | AI assistant |
| `/api/audit` | `AuditResource` | Audit reports |

Persistence: **PostgreSQL** (Flyway migrations in `src/main/resources/db/migration/`). Discovery cache: **Infinispan / Data Grid** (remote Hot Rod client).

### Outbound ports and adapters

Phase 4 introduced an outbound **port** for the 3scale Admin API:

| Layer | Type | Class |
|-------|------|-------|
| Port | Interface | `port/threescale/ThreeScaleAdminPort` |
| Adapter | HTTP client | `ThreeScaleAdminApiClient` |
| Registry | CDI wiring | `ThreeScaleSourceRegistry` |

`ThreeScaleService` and `MigrationService` depend on `ThreeScaleAdminPort`, not raw HTTP. When adding new external integrations, prefer the same pattern (port interface + adapter + registry or factory) before growing service classes.

### Known concentration points

These classes still carry most migration logic. Further extractions are optional follow-ups (see [post-hardening waves](#architecture-hardening-complete)) — not blockers for feature work:

| Class | ~LOC | Notes |
|-------|------|-------|
| `MigrationService` | 1,350+ | Plan orchestration; resource YAML delegated to `service/generator/` |
| `ThreeScaleService` | 760+ | Cache, CRD discovery, Admin API enrichment |
| `MigrationResource` | 350+ | REST surface; K8s apply/revert still in resource layer |
| `service/export/*` | small files | Preferred pattern for new backend code |

---

## Frontend (`frontend/src/app`)

Angular 19 standalone components. Layout uses `core/`, `shared/`, and `features/` folders:

| Path | Route | Role |
|------|-------|------|
| `features/dashboard/` | `/` | Hub overview |
| `features/threescale-explorer/` | `/threescale` | Product/backend browser |
| `features/migration/` | `/migrate` | Multi-step migration UI (`migration-wizard` container + `steps/*`) |
| `features/chat/` | `/chat` | AI assistant |
| `features/audit/` | `/audit` | Audit trail |
| `features/settings/` | `/settings` | Cluster/sources configuration |
| `core/api/` | Domain HTTP facades and `models/` DTOs |
| `shared/` | — | Reusable UI (scaffolded; empty) |

`ng serve` proxies `/api` to `http://localhost:8080` (see `proxy.conf.json`). Production build is static assets behind Nginx.

**Known concentration:** `migration-wizard.component.ts` orchestrates wizard state; step UI lives under `features/migration/steps/`.

---

## Developer Hub plugins

| Package | Type | Integration |
|---------|------|-------------|
| `gateforge-devhub-plugin` | Backstage backend module | Catalog processor, HTTP router for GateForge events |
| `gateforge-devhub-frontend` | Dynamic frontend plugin | Entity tabs (topology, component editor) |

Packaged as OCI images for RHDH dynamic plugin mounting. Version synced with the Helm chart via `scripts/release/sync-versions.sh`.

---

## Scripts and automation

Organized by purpose under `scripts/`. See **[scripts/README.md](../scripts/README.md)** for the full index.

| Category | Examples |
|----------|----------|
| `dev/` | `./scripts/dev/local-up.sh` — Podman Compose stack |
| `e2e/` | `E2E_MODE=fixture ./scripts/e2e/seed-export-analyze.sh` |
| `ci/` | `verify-export-minimal-fixture.sh`, `export-openapi.sh`, `generate-frontend-api-types.sh` |
| `release/` | `./scripts/release/sync-versions.sh` |
| `lib/` | `source scripts/lib/version.sh` (Chart.yaml reader) |

---

## API contracts (OpenAPI → TypeScript)

REST shapes are exported from the Quarkus build and synced to the frontend:

```text
backend (SmallRye OpenAPI)
  → backend/openapi/openapi.yaml          # emitted by OpenApiBuildTest / mvn test
  → frontend/openapi/gateforge.openapi.yaml
  → frontend/src/app/core/api/generated/schema.ts   # openapi-typescript
```

| Step | Command |
|------|---------|
| Export schema only | `./scripts/ci/export-openapi.sh` |
| Full sync + typegen | `./scripts/ci/generate-frontend-api-types.sh` or `npm run generate:api` from `frontend/` |
| CI smoke | `OpenApiBuildTest` in [Backend tests](.github/workflows/backend-tests.yml) |

Hand-written DTOs in `frontend/src/app/core/api/models/` are being migrated to generated types incrementally. After changing a REST response shape, regenerate and commit artifacts (see [CONTRIBUTING.md](../CONTRIBUTING.md)).

---

## Deployment topology

**Local:** `podman-compose.yml` — backend, frontend, PostgreSQL, optional Data Grid. Requires `.env` from `.env.example`.

**OpenShift:** `helm/gateforge/` chart — configurable image tags, secrets, routes, Data Grid StatefulSet. Published to GitHub Pages Helm repo on release.

---

## Architecture hardening (complete)

Phases 1–4 of the **gateforge-architecture-hardening** initiative are merged to `main`. Full PR/issue audit trail: [docs/archive/2026-06-16-architecture-hardening.md](archive/2026-06-16-architecture-hardening.md).

| Phase | Result |
|-------|--------|
| 1 — structure | Script taxonomy, `ARCHITECTURE.md`, contributor docs |
| 2 — backend | DevHub client, generators, repositories, `ExceptionMapper`, REST smoke tests |
| 3 — frontend | `core/` / `shared/` / `features/*`, API facades, wizard step components |
| 4 — contracts | `ThreeScaleAdminPort`, OpenAPI typegen, optional E2E workflow |

**Optional follow-ups (post-hardening):** tighten OpenAPI schemas and adopt generated frontend types; further `MigrationService` / `ThreeScaleService` decomposition; expand `@QuarkusTest` coverage; wizard state extraction and `shared/` UI components.

For future structural work, prefer stacked PRs under ~400 changed lines and avoid mixing large refactors with release-only changes.

---

## Related documentation

- [README.md](../README.md) — product overview, policy mapping tables, quick start
- [CONTRIBUTING.md](../CONTRIBUTING.md) — branch workflow, CI, local dev
- [scripts/README.md](../scripts/README.md) — automation index
- [helm/gateforge/README.md](../helm/gateforge/README.md) — chart values and install
