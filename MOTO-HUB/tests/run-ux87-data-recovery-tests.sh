#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
root=Path("app/src/main/java/com/motohub/next")
quick=(root/"QuickEntryUi.java").read_text()
main=(root/"MainActivity.java").read_text()
fuel=(root/"FuelDraft.java").read_text()
audit=(root/"UpdateDataFootprint.java").read_text()
unit=Path("app/src/test/java/com/motohub/next/UX86BackupFootprintTest.java").read_text()
draft_unit=Path("app/src/test/java/com/motohub/next/UX87FuelDraftDurabilityTest.java").read_text()
checks={
 "service capture immutable reviewed snapshot":'final String reviewedServiceSnapshot=a.db.toString();' in quick,
 "service compare before write":'if(!reviewedServiceSnapshot.equals(beforeSave))' in quick,
 "service owner and read-only rechecked":'if(a.readOnly||a.bike()==null||!maintenanceBikeId.equals(a.bike().optString("id")))' in quick,
 "service bike reference verified":'if(vehicle==null||!maintenanceBikeId.equals(record.optString("vehicleId")))' in quick,
 "service latest ODO never regressed":'if(odo<vehicle.optDouble("odo"))' in quick,
 "service duplicates rechecked":'if(!reviewedDuplicate&&QuickLogDuplicate.exists(a.items("services"),record,"services"))' in quick,
 "service keeps undo on successful commit":'UndoLastSave.offer(a,beforeSave,a.db.toString(),"정비주기"' in quick,
 "fuel drafts fast while editing":'FuelDraft.save(a,bikeId' in quick and '.apply();' in fuel,
 "fuel drafts fsync on activity pause":'@Override protected void onPause(){odoSyncHandler.removeCallbacks(odoSyncTick);flushQuickFuelDraft();persistDraft();super.onPause();}' in main,
 "fuel drafts fsync on dismissed page":'dialog.setOnDismissListener(d->{flushQuickFuelDraft();quickFuelDraftFlush=null;' in main,
 "fuel drafts fsync on normal close":'if(dialog!=null){flushQuickFuelDraft();quickFuelDraftFlush=null;' in main,
 "fuel drafts durable small snapshot":'.putString(key(bike),raw).commit()' in fuel and 'static boolean saveDurable(' in fuel,
 "fuel draft untouched form preserves previous draft":'if(!draftTouched[0])return;' in quick and 'draftTouched[0]=true;' in quick,
 "fuel explicit discard stays discarded":'draftTouched[0]=false; // Explicit discard must not be undone by lifecycle flush.' in quick,
 "fractional fuel and maintenance odometers preserved":'String.valueOf((long)bike.optDouble("odo"))' not in quick and quick.count('java.math.BigDecimal.valueOf(bike.optDouble("odo")).stripTrailingZeros().toPlainString()')>=2,
 "fuel drafts do not resurrect after saving":'a.write(next);a.quickFuelDraftFlush=null;FuelDraft.clear(a,bikeId);' in quick and '.remove(key(bike)).commit()' in fuel,
 "GPS orphan check runs only on expanded schema":'if(expanded&&tracks!=null)requireNoOrphanMarkers(rides,tracks)' in audit,
 "GPS quarantine orphan check":'requireNoOrphanMarkers(folder,entries)' in audit,
 "GPS orphan files never deleted by audit":'private static void requireNoOrphanMarkers(' in audit and 'GPS 원본 없이 부속 파일만 남아 있습니다' in audit,
 "legacy schema4 comparison unchanged":'static String captureLegacy(Context context,JSONObject data)' in audit,
 "orphan tests run on Android JVM":'orphanLiveSidecarIsDetectedWithoutDeletingIt' in unit and 'orphanTrashSidecarIsDetectedAndPreserved' in unit,
 "durable draft tests cover scope and bad payload":'lifecycleFlushCommitsNewestValuesAndStaysBikeScoped' in draft_unit and 'invalidPayloadCannotReplacePreviouslyCommittedDraft' in draft_unit,
}
for name,ok in checks.items():
 print(("PASS" if ok else "FAIL")+" UX87: "+name)
assert all(checks.values()),"UX87 data protection regression"
PY
