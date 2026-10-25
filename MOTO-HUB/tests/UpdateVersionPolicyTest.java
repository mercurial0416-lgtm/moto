package com.motohub.next;
public final class UpdateVersionPolicyTest {
 private static int checks;
 private static void check(boolean condition,String message){
  checks++;if(!condition)throw new AssertionError(message);
 }
 public static void main(String[] args){
  check(UpdateVersionPolicy.isUpgrade(519,518),"new release upgrades older installation");
  check(!UpdateVersionPolicy.isUpgrade(518,518),"equal code is not upgrade");
  check(!UpdateVersionPolicy.isUpgrade(517,518),"older published version cannot downgrade debug");
  check(!UpdateVersionPolicy.isUpgrade(517,519),"older published version cannot downgrade release");
  check("새 업데이트가 배포됐습니다.".equals(UpdateVersionPolicy.currentMessage(519,518)),"update available message");
  check(UpdateVersionPolicy.currentMessage(518,518).contains("동일"),"equal published version label");
  check(UpdateVersionPolicy.currentMessage(517,518).contains("아직 배포되지"),"never label development ahead as latest released");
  check(!UpdateVersionPolicy.isUpgrade(0,518),"invalid empty release cannot upgrade");
  System.out.println("PASS UpdateVersionPolicyTest "+checks+" checks");
 }
}
