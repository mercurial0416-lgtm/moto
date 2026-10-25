package com.motohub.next;

public final class PhotoViewPolicyTest {
 private static void check(boolean condition,String reason){if(!condition)throw new AssertionError(reason);}
 public static void main(String[] args){
  check(!PhotoViewPolicy.hasGarageOwner(null),"catalog preview must not own a garage photo");
  check(!PhotoViewPolicy.hasGarageOwner(""),"empty owner must not enable photo picker");
  check(!PhotoViewPolicy.hasGarageOwner("../../escape"),"invalid owner must not enable photo picker");
  check(PhotoViewPolicy.hasGarageOwner("123e4567-e89b-12d3-a456-426614174000"),"garage owner must enable its own photo");
  System.out.println("PhotoViewPolicyTest PASS (4 checks)");
 }
}
