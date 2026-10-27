package com.motohub.next;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/** User instructions rather than raw networking/Java exception messages. */
final class UpdateErrorText {
 private UpdateErrorText(){}
 static String forError(Exception error){
  if(error instanceof UnknownHostException)
   return "인터넷 연결을 확인하고 다시 시도해 주세요.";
  if(error instanceof SocketTimeoutException)
   return "네트워크 응답이 지연됐습니다. 연결을 확인한 뒤 다시 시도해 주세요.";
  if(error instanceof SSLException)
   return "보안 연결에 실패했습니다. 네트워크와 기기 날짜를 확인해 주세요.";
  if(error instanceof SecurityException)
   return "설치 또는 저장 권한을 확인해 주세요.";
  String reason=error==null?"":String.valueOf(error.getMessage());
  if(reason.contains("HTTP 429"))return "업데이트 서버 요청 제한입니다. 잠시 후 다시 시도해 주세요. 저장된 기록은 유지됩니다.";
  if(reason.contains("HTTP 403"))return "업데이트 파일 접근이 제한됐습니다. 네트워크를 바꾸거나 배포 페이지를 확인해 주세요.";
  if(reason.contains("HTTP 50"))return "배포 서버에 일시적인 문제가 있습니다. 다운로드 파일은 보관되므로 다시 확인해 주세요.";
  if(reason.contains("다운로드 시간이 초과"))return "다운로드 시간이 초과됐습니다. 다시 시도하면 확인된 일부 파일을 이어받습니다.";

  String message=error==null?null:error.getMessage();
  if(message!=null&&message.matches("(?s).*[가-힣].*")&&message.length()<=240)
   return message;
  if(error instanceof IOException)
   return "업데이트 파일을 읽거나 저장하지 못했습니다. 저장공간과 네트워크를 확인해 주세요.";
  return "업데이트 중 문제가 발생했습니다. 다시 시도해 주세요.";
 }
}
