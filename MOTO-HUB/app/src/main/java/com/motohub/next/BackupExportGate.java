package com.motohub.next;

/** Backups are immutable snapshots: do not copy a track while it is being appended. */
final class BackupExportGate {
 private BackupExportGate(){}
 static final String MESSAGE="GPS 라이딩을 종료하고 OBD2 자동 출발 감지를 해제한 뒤 전체 ZIP을 백업해 주세요. 진행 중인 경로가 잘릴 수 있습니다.";
 static boolean canExport(boolean running,boolean obdArmed,boolean restoring){
  return !running&&!obdArmed&&!restoring;
 }
}
