#!/usr/bin/env bash
# Backward-compatible wrapper — see scripts/release/sync-versions.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/release/sync-versions.sh" "$@"
