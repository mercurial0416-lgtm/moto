#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p /tmp/motohub-visual-classes
javac -encoding UTF-8 -d /tmp/motohub-visual-classes app/src/main/java/com/motohub/next/BikeVisual.java tests/BikeVisualTest.java
java -cp /tmp/motohub-visual-classes com.motohub.next.BikeVisualTest
