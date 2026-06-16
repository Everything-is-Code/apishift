#!/usr/bin/env bash
# Backward-compatible wrapper — see scripts/e2e/seed-export-analyze.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/e2e/seed-export-analyze.sh" "$@"
