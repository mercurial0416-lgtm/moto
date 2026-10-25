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
  String message=error==null?null:error.getMessage();
  if(message!=null&&message.matches("(?s).*[가-힣].*")&&message.length()<=240)
   return message;
  if(error instanceof IOException)
   return "업데이트 파일을 읽거나 저장하지 못했습니다. 저장공간과 네트워크를 확인해 주세요.";
  return "업데이트 중 문제가 발생했습니다. 다시 시도해 주세요.";
 }
}
