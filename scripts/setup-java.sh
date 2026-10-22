#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
case "$(uname -s)" in Darwin) OS=mac ;; Linux) OS=linux ;; *) echo 'Windows: use JDK 17/21 and gradlew.bat; see README.' >&2; exit 2 ;; esac
case "$(uname -m)" in arm64|aarch64) ARCH=aarch64 ;; x86_64) ARCH=x64 ;; *) exit 2 ;; esac
mkdir -p "$ROOT/.local/downloads"
for JDK in 17 21; do
  if [[ -d "$ROOT/.local/jdk-$JDK" ]]; then continue; fi
  if [[ "$OS" == mac ]] && /usr/libexec/java_home -v "$JDK" >/dev/null 2>&1; then continue; fi
  ARCHIVE="$ROOT/.local/downloads/jdk$JDK.tar.gz"
  curl -fL --retry 3 "https://api.adoptium.net/v3/binary/latest/$JDK/ga/$OS/$ARCH/jdk/hotspot/normal/eclipse" -o "$ARCHIVE"
  mkdir -p "$ROOT/.local/jdk-$JDK"
  tar -xzf "$ARCHIVE" --strip-components=1 -C "$ROOT/.local/jdk-$JDK"
done
