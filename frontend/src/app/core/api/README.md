# Core API layer

Domain-scoped HTTP facades for the Quarkus backend (`/api/*`).

| Service | Prefix | Responsibility |
|---------|--------|----------------|
| `ClusterApiService` | `/api/cluster` | Projects, targets, features, readiness |
| `ThreeScaleApiService` | `/api/threescale` | Products, backends, sources, discovery |
| `MigrationApiService` | `/api/migration` | Analyze, plans, apply/revert, import-export |
| `AuditApiService` | `/api/audit` | Audit reports |
| `ChatApiService` | `/api/chat` | AI assistant |

Shared DTOs live in `models/`. Import types from `core/api/models` in feature code.
