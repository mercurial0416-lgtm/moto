package com.motohub.next;

public final class PhotoCacheYearPolicyTest {
 static int checks;
 static void check(boolean v,String description){checks++;if(!v)throw new AssertionError(description);}
 public static void main(String[]args){
  check(PhotoCacheYearPolicy.canShow("File:2024 KTM 390 Duke.jpg","KTM","390 Duke",2024,false),"same year");
  check(!PhotoCacheYearPolicy.canShow("File:2023 KTM 390 Duke.jpg","KTM","390 Duke",2024,false),"mismatched 2023 cached auto image");
  check(!PhotoCacheYearPolicy.canShow("File:2024 KTM 390 Adventure.jpg","KTM","390 Duke",2024,false),"mismatched trim");
  check(PhotoCacheYearPolicy.canShow("File:2023 KTM 390 Duke.jpg","KTM","390 Duke",2024,true),"person-selected image honored");
  check(PhotoCacheYearPolicy.canShow("File:KTM 390 Duke.jpeg","KTM","390 Duke",2024,false),"undated correct model");
  check(PhotoCacheYearPolicy.canShow(null,"KTM","390 Duke",2024,false),"old offline cache without metadata remains viewable");
  check(PhotoCacheYearPolicy.canShow("","KTM","390 Duke",2024,false),"legacy metadata remains viewable");
  System.out.println("PASS UX9 photo cache year safety: "+checks+" checks");
 }
}
