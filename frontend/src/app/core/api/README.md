# Core API layer

Domain-scoped HTTP facades for the Quarkus backend (`/api/*`).

| Service | Prefix | Responsibility |
|---------|--------|----------------|
| `ClusterApiService` | `/api/cluster` | Projects, targets, features, readiness |
| `ThreeScaleApiService` | `/api/threescale` | Products, backends, sources, discovery |
| `MigrationApiService` | `/api/migration` | Analyze, plans, apply/revert, import-export |
| `AuditApiService` | `/api/audit` | Audit reports |
| `ChatApiService` | `/api/chat` | AI assistant |

## Types: `models/` vs `generated/`

| Path | Role |
|------|------|
| `models/` | Hand-written DTOs used by features today |
| `generated/schema.ts` | OpenAPI types from `npm run generate:api` |
| `generated/index.ts` | Convenience aliases (`MigrationPlanDto`, …) |

**Contract workflow:** after backend REST changes, run `npm run generate:api` from `frontend/` (see root [CONTRIBUTING.md](../../../../CONTRIBUTING.md)). Facades are migrating to `generated/` types incrementally; prefer aliases from `generated/index.ts` for new code.

Import facade services from `core/api/*-api.service.ts`. Import shared types from `core/api/models` until the facade for that domain uses generated types.
