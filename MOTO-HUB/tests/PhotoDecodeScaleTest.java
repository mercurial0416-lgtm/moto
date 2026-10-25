package com.motohub.next;

public final class PhotoDecodeScaleTest {
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 public static void main(String[] args){
  check(PhotoDecodeScale.sampleSize(300,200,360)==1,"small preview unchanged");
  check(PhotoDecodeScale.sampleSize(1100,825,360)==4,"Commons preview bounded");
  check(PhotoDecodeScale.sampleSize(4000,3000,360)==16,"large image bounded");
  check(PhotoDecodeScale.sampleSize(4000,3000,1300)==4,"selected image remains larger");
  check(PhotoDecodeScale.sampleSize(360,360,360)==1,"boundary inclusive");
  check(PhotoDecodeScale.sampleSize(361,360,360)==2,"boundary above limit");
  try{PhotoDecodeScale.sampleSize(0,300,360);throw new AssertionError("invalid width accepted");}catch(IllegalArgumentException expected){}
  try{PhotoDecodeScale.sampleSize(100,300,0);throw new AssertionError("invalid limit accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PhotoDecodeScaleTest PASS (8 assertions)");
 }
}
