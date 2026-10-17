package com.motohub.next;
import java.net.URI;
import java.util.regex.*;

/** Safe, offline parsing of shared listing links (no remote page scraping). */
final class QuickEntry {
 private QuickEntry() {}
 private static final Pattern WEB=Pattern.compile("https?://[^\\s<>\\\"']+",Pattern.CASE_INSENSITIVE);
 static String extractUrl(String shared){
  if(shared==null)return "";
  Matcher m=WEB.matcher(shared);
  while(m.find()){
   String link=m.group();
   while(!link.isEmpty()&&".,;!)]}>".indexOf(link.charAt(link.length()-1))>=0)link=link.substring(0,link.length()-1);
   if(link.length()>2048)continue;
   try{URI u=new URI(link);
    if(("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getHost()!=null&&u.getUserInfo()==null)
     return link;
   }catch(Exception ignored){}
  }
  return "";
 }
 static String sharedTitle(String raw,String url){
  if(raw==null||url==null||url.isEmpty())return "";
  String candidate=raw.replace(url,"").replaceAll("(?i)https?://\\S+", "").trim();
  candidate=candidate.replaceAll("^[\\s:|\\-]+|[\\s:|\\-]+$","").trim();
  if(candidate.length()<3||candidate.length()>80||candidate.indexOf('\n')>=0||candidate.indexOf('\r')>=0)return "";
  return candidate;
 }
}
