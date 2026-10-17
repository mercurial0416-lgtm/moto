package com.motohub.next;

public final class DraftBannerTest {
 public static void main(String[] args) {
  String[][] expected={{"vehicles","바이크"},{"services","정비"},{"expenses","주유·비용"},
   {"listings","매물"},{"routes","코스"},{"gear","보호장구"},{"unknown",""},{"",""}};
  int passed=0;
  for(String[] row:expected){if(!DraftBanner.label(row[0]).equals(row[1]))throw new AssertionError(row[0]);passed++;}
  if(!DraftBanner.label(null).isEmpty())throw new AssertionError("null key");passed++;
  System.out.println("PASS: "+passed+" draft banner label checks");
 }
}
