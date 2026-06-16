#!/usr/bin/env bash
# Backward-compatible wrapper — see scripts/dev/local-down.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/dev/local-down.sh" "$@"
