package com.motohub.next;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
public final class UpdateErrorTextTest {
 static int checks=0;
 static void test(boolean trueCondition,String label){
  checks++;if(!trueCondition)throw new AssertionError(label);
 }
 public static void main(String[] args){
  test(UpdateErrorText.forError(new UnknownHostException("private host")).contains("인터넷"),"DNS actionable");
  test(UpdateErrorText.forError(new SocketTimeoutException("timeout")).contains("네트워크"),"timeout actionable");
  test(UpdateErrorText.forError(new SSLException("handshake")).contains("보안"),"TLS guidance");
  test(UpdateErrorText.forError(new SecurityException("private")).contains("권한"),"permission guidance");
  test(UpdateErrorText.forError(new IOException("기기에 저장할 공간이 부족합니다.")).contains("공간"),"Korean curated message");
  test(!UpdateErrorText.forError(new IOException("internal.server.name")).contains("internal.server.name"),"no raw internal paths");
  test(UpdateErrorText.forError(new IllegalArgumentException("bad")).contains("다시"),"fallback");
  test(UpdateErrorText.forError(null).contains("다시"),"null fallback");
  System.out.println("PASS UpdateErrorTextTest "+checks+" checks");
 }
}
