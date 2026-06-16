#!/usr/bin/env bash
# Backward-compatible wrapper — see scripts/fixtures/sync-export-minimal-fixture.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/fixtures/sync-export-minimal-fixture.sh" "$@"
