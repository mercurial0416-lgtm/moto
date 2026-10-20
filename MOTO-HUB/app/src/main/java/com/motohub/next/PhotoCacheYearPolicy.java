package com.motohub.next;

/** A user's explicit selection wins; an automatic cached photo with the wrong model/year does not. */
final class PhotoCacheYearPolicy {
 private PhotoCacheYearPolicy(){}
 static boolean canShow(String title,String brand,String model,int year,boolean userSelected){
  if(userSelected)return true;
  // Old offline caches without metadata must remain accessible, but are not certified.
  if(title==null||title.trim().isEmpty())return true;
  return PhotoTitleMatcher.matchesFallback(title,brand,model,year);
 }
}
