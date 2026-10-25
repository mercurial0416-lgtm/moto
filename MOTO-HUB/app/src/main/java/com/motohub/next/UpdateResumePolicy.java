package com.motohub.next;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/** Fail-closed HTTP Range policy, independent of Android and network libraries. */
final class UpdateResumePolicy {
 private static final Pattern RANGE=Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)");
 private UpdateResumePolicy(){}

 static boolean canResume(long size,long expected){
  return size>0&&expected>0&&size<expected;
 }
 static boolean validPartial(int status,String header,long start,long expected){
  if(status!=206||!canResume(start,expected)||header==null)return false;
  Matcher matcher=RANGE.matcher(header.trim());
  if(!matcher.matches())return false;
  try{
   long actualStart=Long.parseLong(matcher.group(1));
   long actualEnd=Long.parseLong(matcher.group(2));
   long total=Long.parseLong(matcher.group(3));
   return actualStart==start&&actualEnd>=start&&actualEnd<total&&total==expected;
  }catch(NumberFormatException malformed){return false;}
 }
}
