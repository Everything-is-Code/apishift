#!/usr/bin/env bash
# Backward-compatible wrapper — see scripts/dev/local-up.sh
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/dev/local-up.sh" "$@"
