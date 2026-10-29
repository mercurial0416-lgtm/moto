package com.motohub.next;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/** Actionable local update errors without leaking Java stack traces or private paths. */
final class UpdateErrorText {
 private UpdateErrorText(){}
 static String forError(Exception error){
  String reason=error==null?"":String.valueOf(error.getMessage());
  String lower=reason.toLowerCase(java.util.Locale.ROOT);
  if(error instanceof UnknownHostException)
   return "인터넷 연결 오류 · Wi-Fi 또는 모바일 데이터를 확인한 뒤 배포 버전 확인을 다시 눌러 주세요.";
  if(error instanceof SocketTimeoutException||lower.contains("timed out")||reason.contains("시간이 초과"))
   return "네트워크 응답 지연 · 신호가 좋은 곳에서 재시도하세요. 받아둔 정상 파일은 이어받을 수 있어요.";
  if(error instanceof SSLException)
   return "보안 연결 오류 · 휴대폰 날짜와 네트워크를 확인하세요. 인증서 검사 없이 설치하지 않습니다.";
  if(reason.contains("HTTP 404")||reason.contains("HTTP 410")||
     reason.contains("파일 주소와 버전 정보가 일치하지"))
   return "배포 파일 미공개 또는 주소 불일치 · 앱 데이터 문제는 아니에요. 공식 배포가 공개된 뒤 다시 확인하세요.";
  if(reason.contains("HTTP 429"))
   return "업데이트 서버 요청 제한 · 잠시 뒤 다시 시도하세요. 기기 기록은 보존돼요.";
  if(reason.contains("HTTP 403"))
   return "배포 파일 접근 제한 · 네트워크를 바꾸거나 공식 릴리스 페이지를 확인하세요.";
  if(reason.contains("HTTP 50"))
   return "배포 서버 일시 장애 · 나중에 재시도하세요. 기존 버전은 그대로 유지됩니다.";
  if(lower.contains("space")||lower.contains("enospc")||lower.contains("no space")||
     reason.contains("저장공간")||reason.contains("공간이 부족"))
   return "저장공간 부족 · 기기에서 여유 공간을 확보한 뒤 다시 다운로드하세요. 기존 앱을 삭제하지 마세요.";
  if(lower.contains("signature")||lower.contains("certificate")||lower.contains("signer")||
     reason.contains("서명")||reason.contains("인증서"))
   return "서명 인증서 불일치 · 현재 앱과 동일한 원본 키로 서명된 APK만 설치할 수 있어요. 앱을 삭제하지 말고 정식 배포를 확인하세요.";
  if(lower.contains("sha-256")||lower.contains("sha256")||lower.contains("checksum")||
     reason.contains("무결성")||reason.contains("해시"))
   return "다운로드 파일 무결성 실패 · 설치하지 않고 차단했어요. 파일을 다시 내려받아 검증해 주세요.";
  if(error instanceof SecurityException)
   return "권한 오류 · Android의 설치 허용 상태와 업데이트 서명 인증서를 확인해 주세요. 앱을 삭제하지 마세요.";
  if(reason.contains("APK")||lower.contains("package"))
   return "APK 패키지 또는 버전 검사 실패 · 공식 MOTO HUB 배포 파일인지 확인해 주세요. 기존 앱은 유지됩니다.";
  if(reason.matches("(?s).*[가-힣].*")&&reason.length()<=240)return reason;
  if(error instanceof IOException)
   return "업데이트 파일 저장 또는 인터넷 오류 · 여유 저장공간을 확보하고 연결 확인 후 재시도해 주세요.";
  return "업데이트 확인에 실패했어요. 기존 기록은 그대로이며 다시 시도할 수 있어요.";
 }
}
