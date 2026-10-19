#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
javac -encoding UTF-8 -d "$work" \
  app/src/main/java/com/motohub/next/{TrackFile,TrackCompletion,RideTrackMerge,RideBikeBinding,ObdRideAutoGate,BackupFormat,BackupBundle,RestoreArchive,RestoreCapacity}.java \
  tests/{RideTrackMergeTest,RideTrackMergeBackupTest,ObdStopGpsSafetyTest}.java
java -cp "$work" com.motohub.next.RideTrackMergeTest
java -cp "$work" com.motohub.next.RideTrackMergeBackupTest
java -cp "$work" com.motohub.next.ObdStopGpsSafetyTest
