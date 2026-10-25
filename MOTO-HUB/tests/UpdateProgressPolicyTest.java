package com.motohub.next;
public final class UpdateProgressPolicyTest {
 private static int checks=0;
 static void eq(Object want,Object actual){
  checks++;
  if(!want.equals(actual))throw new AssertionError("Expected="+want+" actual="+actual);
 }
 public static void main(String[] args){
  eq(0,UpdateProgressPolicy.percentage(0,100));
  eq(0,UpdateProgressPolicy.percentage(-1,100));
  eq(0,UpdateProgressPolicy.percentage(5,0));
  eq(0,UpdateProgressPolicy.percentage(5,-1));
  eq(1,UpdateProgressPolicy.percentage(1,51));
  eq(25,UpdateProgressPolicy.percentage(25,100));
  eq(99,UpdateProgressPolicy.percentage(999,1000));
  eq(100,UpdateProgressPolicy.percentage(1000,1000));
  eq(100,UpdateProgressPolicy.percentage(1100,1000));
  eq(99,UpdateProgressPolicy.percentage(Long.MAX_VALUE-1,Long.MAX_VALUE));
  eq("다운로드 중 · 25%",UpdateProgressPolicy.label("다운로드 중",25));
  eq("검사 중",UpdateProgressPolicy.label("검사 중",-1));
  eq("다운로드 중 · 100%",UpdateProgressPolicy.label("다운로드 중",101));
  System.out.println("PASS UpdateProgressPolicyTest "+checks+" assertions");
 }
}
