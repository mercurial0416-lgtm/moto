package com.motohub.next;
import java.io.IOException;

/** Standalone JDK test so connection validation works even without Android SDK. */
public final class ServerSettingsTest {
 static int assertions;
 static void eq(String got,String expected){assertions++;if(!got.equals(expected))throw new AssertionError("Expected " +expected+" got "+got);}
 static void rejectUrl(String input){assertions++;try{ServerSettings.validateUrl(input);throw new AssertionError("Unsafe URL accepted: "+input);}catch(IOException expected){}}
 static void rejectKey(String input){assertions++;try{ServerSettings.validateKey(input);throw new AssertionError("Unsafe key accepted");}catch(IOException expected){}}
 public static void main(String[] args)throws Exception {
  eq(ServerSettings.validateUrl(" https://demo-project.supabase.co/ "),"https://demo-project.supabase.co");
  eq(ServerSettings.validateUrl("https://demo.supabase.co"),"https://demo.supabase.co");
  rejectUrl("http://demo.supabase.co");rejectUrl("https://demo.supabase.co.evil.com");
  rejectUrl("https://demo.supabase.co/path");rejectUrl("https://user@demo.supabase.co");
  rejectUrl("https://demo.supabase.co?x=y");rejectUrl("https://demo.supabase.co#fragment");
  rejectUrl("https://demo.supabase.co:444");rejectUrl("https://127.0.0.1");
  rejectKey("service_role_secret");rejectKey("sb_secret_badbadbadbad");rejectKey("eyJhbGciOiJIUzI1NiJ9");
  rejectKey("sb_publishable_too");rejectKey("sb_publishable_abc def 12345678");
  eq(ServerSettings.validateKey("sb_publishable_abcdefghijklmnop"),"sb_publishable_abcdefghijklmnop");
  System.out.println("PASS ServerSettingsTest: "+assertions+" checks");
 }
}
