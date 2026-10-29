package com.motohub.next;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
public final class UX81ErrorMessagesTest {
 public static void main(String[] args){
  test(new IOException("저장공간이 부족합니다"),"저장공간 부족");
  test(new IOException("서명 인증서가 불일치"),"서명 인증서 불일치");
  test(new IOException("APK SHA-256 무결성 확인 오류"),"무결성 실패");
  test(new IOException("업데이트 서버 응답 오류 (HTTP 404)"),"미공개");
  test(new UnknownHostException("offline"),"인터넷");
  test(new SocketTimeoutException("timeout"),"응답 지연");
  System.out.println("PASS UX81ErrorMessagesTest");
 }
 private static void test(Exception error,String expected){
  String result=UpdateErrorText.forError(error);
  if(!result.contains(expected))throw new AssertionError(expected+": "+result);
 }
}
