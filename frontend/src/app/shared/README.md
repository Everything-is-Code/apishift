# Shared UI

Reusable standalone components used across features.

| Component | Selector | Purpose |
|-----------|----------|---------|
| `LoadingSkeletonComponent` | `gf-loading-skeleton` | Card/bar loading placeholder |
| `StatusBadgeComponent` | `gf-status-badge` | Prerequisite and status pills |
| `BusyOverlayComponent` | `gf-busy-overlay` | Modal busy spinner overlay |

Import from `shared/ui/` in feature components. Styles are scoped per component; wizard-specific layout remains in `migration-wizard.shared.scss`.
