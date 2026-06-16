#!/usr/bin/env bash
# Backward-compatible wrapper — see scripts/ci/verify-export-minimal-fixture.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/ci/verify-export-minimal-fixture.sh" "$@"
