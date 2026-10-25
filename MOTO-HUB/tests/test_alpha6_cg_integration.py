#!/usr/bin/env python3
"""Static source wiring; deliberately NOT an Android device or runtime test."""
from pathlib import Path
from version_consistency import version_declaration_ok, debug_alignment_ok
root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/com/motohub/next'
checks={
 'read_only_capability_whitelist': 'for(String safe:CAPABILITY_PIDS)' in (src/'ObdReadOnlyProtocol.java').read_text(),
 'odo_not_in_requested_pids': '"0100","0140"' in (src/'ObdReadOnlyProtocol.java').read_text() and '22F' not in (src/'ObdAutoSpeedMonitor.java').read_text(),
 'capability_decoder_used': 'ObdPidCapabilities.fromResponses' in (src/'ObdAutoSpeedMonitor.java').read_text(),
 'capability_in_manual_mode': 'ObdPidCapabilities.fromResponses' in (src/'DeveloperObdScreen.java').read_text(),
 'source_integration_odo_manual_only': 'OdometerSyncPolicy.checkedManual' in (src/'DeveloperObdScreen.java').read_text() and 'target.put("odo",value)' in (src/'DeveloperObdScreen.java').read_text(),
 'odo_never_auto_overwrite': 'target.put("odo",value)' not in (src/'RideService.java').read_text(),
 'odo_saved_atomic': 'activity.write(next);DailyMaintenanceJob.checkSoon(activity)' in (src/'DeveloperObdScreen.java').read_text(),
 'odo_sold_guard': 'GarageBikeStatus.sold(target.optString("soldDate"))' in (src/'DeveloperObdScreen.java').read_text(),
 'separate_disarm_command': 'if(AUTO_DISARM.equals(action))' in (src/'RideService.java').read_text(),
 'gps_survives_auto_disarm': 'onObdDisconnected("사용자가 자동 감지를 중지했습니다.")' in (src/'RideService.java').read_text(),
 'ride_stop_is_explicit': 'if(STOP.equals(action))' in (src/'RideService.java').read_text(),
 'no_automatic_group_share': 'Sharing.stop(); // Automatic local rides' in (src/'RideService.java').read_text(),
 'kakao_notification_action': '"카카오내비",nav' in (src/'RideService.java').read_text(),
 'navigation_launch_independent': 'KakaoNaviLaunch.PACKAGE' in (src/'RideService.java').read_text(),
 'battery_settings_explicit': 'ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS' in (src/'DeveloperObdScreen.java').read_text(),
 'gps_only_status_not_stale': 'gpsOnlyAfterObd=false;' in (src/'RideService.java').read_text(),
 'familiar_package_privacy': 'android:exported="false" android:foregroundServiceType="location|connectedDevice"' in (root/'app/src/main/AndroidManifest.xml').read_text(),
 'signed_release_fingerprint': 'MOTO_EXPECTED_CERT_SHA256' in (root/'.github/workflows/android-signed-release.yml').read_text(),
 'gradle_version': version_declaration_ok() and debug_alignment_ok(),
 # A source ZIP is a standalone MOTO-HUB project. The outer GitHub repository
 # workflow is deliberately not bundled; verify included self-contained CI.
 'standalone_debug_workflow': (root/'.github/workflows/android-debug.yml').is_file(),
 'standalone_build_path': './gradlew :app:assembleDebug' in (root/'.github/workflows/android-debug.yml').read_text(),
 'standalone_signed_fail_closed': 'Signing secrets are missing' in (root/'.github/workflows/android-signed-release.yml').read_text(),
 'standalone_cert_fingerprint': 'MOTO_EXPECTED_CERT_SHA256' in (root/'.github/workflows/android-signed-release.yml').read_text(),
}
for key,passed in checks.items(): print(('PASS' if passed else 'FAIL')+' '+key)
if not all(checks.values()):raise SystemExit(1)
print(f'{len(checks)} CG source wiring assertions passed (not Android integration)')
