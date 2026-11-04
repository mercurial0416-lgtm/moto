package com.motohub.next;
import java.util.regex.Pattern;
/** Active GPS sessions may contain earlier completed sections. They are not complete rides. */
final class RideActiveSessionPolicy {
 private static final Pattern TRACK=Pattern.compile("[0-9]{10,15}-[a-f0-9-]{36}\\.track");
 private RideActiveSessionPolicy(){}
 static String session(String name){
  if(name==null||!TRACK.matcher(name).matches())return "";
  return name.substring(name.indexOf('-')+1,name.length()-6);
 }
 static boolean exclude(String candidate,boolean running,String activeName){
  if(!running||candidate==null||activeName==null||activeName.isEmpty())return false;
  if(candidate.equals(activeName))return true;
  String activeSession=session(activeName);
  return !activeSession.isEmpty()&&activeSession.equals(session(candidate));
 }
}
