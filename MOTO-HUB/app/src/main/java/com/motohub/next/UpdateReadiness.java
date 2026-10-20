package com.motohub.next;
/** Pure policy: fail closed if installed package is not signed by the fixed update identity. */
final class UpdateReadiness {
 private UpdateReadiness(){}
 static boolean fixedIdentity(String installed,String expected){
  return expected!=null&&expected.matches("[a-f0-9]{64}")&&installed!=null&&installed.equals(expected);
 }
 static String state(String installed,String expected){
  if(installed==null||installed.isEmpty())return "앱 서명 확인에 실패했습니다. 자동 업데이트를 차단합니다.";
  if(!fixedIdentity(installed,expected))return "이 앱은 고정 서명 설치본이 아닙니다. 먼저 전체 백업 후 고정 서명판으로 전환해 주세요. 앱 삭제는 백업 전 금지입니다.";
  return "고정 서명 인증서가 일치합니다. 데이터 유지 업데이트를 확인할 수 있습니다.";
 }
}
