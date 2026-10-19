#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" app/src/main/java/com/motohub/next/{TrackFile,TrackCompletion,ObdConnectionPolicy}.java tests/{ObdReconnectBudgetTest,TrackCompletionTest}.java
java -cp "$OUT" com.motohub.next.ObdReconnectBudgetTest
java -cp "$OUT" com.motohub.next.TrackCompletionTest
