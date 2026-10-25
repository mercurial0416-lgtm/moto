package com.motohub.next;

/** Never hand an APK installer control while local user data is being written. */
final class UpdateSafety {
 private UpdateSafety(){}
 static final String MESSAGE="GPS 라이딩·OBD2 자동 감지·백업 또는 복원을 종료한 뒤 업데이트해 주세요.";
 static boolean canInstall(boolean riding,boolean obdArmed,boolean exporting,boolean restoring){
  return !riding&&!obdArmed&&!exporting&&!restoring;
 }
}
