# scripts/lib

Sourceable Bash helpers only — **not** meant to be executed directly.

| File | Exports |
|------|---------|
| `common.sh` | `ROOT`, `die()`, `log()` |
| `version.sh` | `VERSION`, `VERSION_V` (from `helm/apishift/Chart.yaml`) |
| `helm-repo-url.sh` | `HELM_REPO_URL` |

Example:

```bash
source scripts/lib/common.sh
source scripts/lib/version.sh
echo "ApiShift ${VERSION_V}"
```
