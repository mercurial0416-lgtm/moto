#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
javac -encoding UTF-8 -d "$out" app/src/main/java/com/motohub/next/PhotoViewPolicy.java tests/PhotoViewPolicyTest.java
java -cp "$out" com.motohub.next.PhotoViewPolicyTest
# Ensure catalog previews are never bound to a current garage photo, and garage cards bind their own ID.
grep -Fq 'MotorcyclePhoto.show(this,photo,label,brand,model,year,null);' app/src/main/java/com/motohub/next/MainActivity.java
grep -Fq 'MotorcyclePhoto.show(a,visual,example,brand,model,year,bike.optString("id"));' app/src/main/java/com/motohub/next/GarageHomeUi.java
if grep -Fq 'a.bike()' app/src/main/java/com/motohub/next/MotorcyclePhoto.java; then echo 'Photo preview depends on selected garage bike' >&2; exit 1; fi
printf 'Photo view integration checks PASS (3 checks)\n'
