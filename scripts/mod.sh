#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MC="${1:-1.20.1}"
if [[ $# -gt 0 ]]; then shift; fi
case "$MC" in
  1.20.1|1.19.1) JDK=17 ;;
  1.21.1) JDK=21 ;;
  *) echo "支持的版本: 1.20.1, 1.21.1, 1.19.1" >&2; exit 2 ;;
esac
JDK_VAR="JAVA${JDK}_HOME"
SELECTED_JAVA="${!JDK_VAR:-}"
if [[ -z "$SELECTED_JAVA" && -d "$ROOT/.local/jdk-$JDK/Contents/Home" ]]; then
  SELECTED_JAVA="$ROOT/.local/jdk-$JDK/Contents/Home"
elif [[ -z "$SELECTED_JAVA" && -d "$ROOT/.local/jdk-$JDK/bin" ]]; then
  SELECTED_JAVA="$ROOT/.local/jdk-$JDK"
fi
if [[ -z "$SELECTED_JAVA" && "$(uname -s)" == Darwin ]]; then
  SELECTED_JAVA="$(/usr/libexec/java_home -v "$JDK" 2>/dev/null || true)"
fi
if [[ -z "$SELECTED_JAVA" ]]; then SELECTED_JAVA="${JAVA_HOME:-}"; fi
if [[ ! -x "$SELECTED_JAVA/bin/java" ]]; then
  echo "需要 JDK $JDK，请设置 $JDK_VAR 或运行 ./scripts/setup-java.sh" >&2; exit 2
fi
ACTUAL_VERSION="$("$SELECTED_JAVA/bin/java" -version 2>&1 | head -n 1)"
if [[ ! "$ACTUAL_VERSION" =~ \"$JDK\. ]]; then
  echo "需要 JDK $JDK，实际为 $ACTUAL_VERSION；请设置 $JDK_VAR" >&2; exit 2
fi
export JAVA_HOME="$SELECTED_JAVA"
export PATH="$JAVA_HOME/bin:$PATH"
# Local caches stay inside the project. CI uses setup-gradle's normal cache.
if [[ -z "${CI:-}" ]]; then export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$ROOT/.local/gradle}"; fi
if [[ $# -eq 0 ]]; then set -- build; fi
echo "Minecraft $MC / JDK $JDK / $JAVA_HOME"
cd "$ROOT/versions/$MC"
exec ./gradlew "$@"
