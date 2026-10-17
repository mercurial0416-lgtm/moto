package com.motohub.next;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/** Public Supabase connection settings. Never accept a secret/service-role key. */
final class ServerSettings {
 private ServerSettings() {}
 static String validateUrl(String input) throws IOException {
  try {
   String value = input == null ? "" : input.trim();
   URI uri = new URI(value);
   if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null ||
       uri.getPort() != -1 || uri.getUserInfo() != null || uri.getRawQuery() != null ||
       uri.getRawFragment() != null || !(uri.getRawPath() == null ||
       uri.getRawPath().isEmpty() || uri.getRawPath().equals("/"))) {
    throw new IOException("Supabase 프로젝트 HTTPS 주소를 확인하세요.");
   }
   String host = uri.getHost().toLowerCase(Locale.ROOT);
   if (!host.matches("[a-z0-9-]+\\.supabase\\.co")) {
    throw new IOException("공식 Supabase 프로젝트 주소(*.supabase.co)만 지원합니다.");
   }
   return "https://" + host;
  } catch (Exception e) {
   if (e instanceof IOException) throw (IOException) e;
   throw new IOException("Supabase 프로젝트 주소 형식이 올바르지 않습니다.");
  }
 }
 static String validateKey(String input) throws IOException {
  String key = input == null ? "" : input.trim();
  if (key.length() > 512 || !key.matches("sb_publishable_[A-Za-z0-9_-]{12,}"))
   throw new IOException("sb_publishable_ 공개키만 입력하세요. 관리자/secret 키는 절대 입력하지 마세요.");
  return key;
 }
}
