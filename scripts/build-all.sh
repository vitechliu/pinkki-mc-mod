#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
for MC in 1.20.1 1.21.1 1.19.1; do
  "$ROOT/scripts/mod.sh" "$MC" clean build
  python3 "$ROOT/scripts/verify-artifact.py" "$MC" "$ROOT/dist"
done
