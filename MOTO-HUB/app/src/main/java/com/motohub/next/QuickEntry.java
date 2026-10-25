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
   while(!link.isEmpty()){
    char tail=link.charAt(link.length()-1);
    if(".,;!?:>”’」』】》".indexOf(tail)>=0){link=link.substring(0,link.length()-1);continue;}
    // A closing bracket is part of the URL when paired inside its path/query.
    char open=tail==')'?'(':tail==']'?'[':tail=='}'?'{':'\0';
    if(open!='\0'){
     int opens=0,closes=0;
     for(int i=0;i<link.length();i++){if(link.charAt(i)==open)opens++;if(link.charAt(i)==tail)closes++;}
     if(closes>opens){link=link.substring(0,link.length()-1);continue;}
    }
    break;
   }
   if(link.length()>2048)continue;
   // Reject invisible Unicode controls in pasted links.
   if(link.codePoints().anyMatch(cp->Character.isISOControl(cp)||Character.getType(cp)==Character.FORMAT))continue;
   try{URI u=new URI(link);
    if(("https".equalsIgnoreCase(u.getScheme())||"http".equalsIgnoreCase(u.getScheme()))&&u.getHost()!=null&&u.getUserInfo()==null&&u.getHost().indexOf('.')>0&&u.getPort()!=0)
     return link;
   }catch(Exception ignored){}
  }
  return "";
 }
 static String sharedTitle(String raw,String url){
  if(raw==null||url==null||url.isEmpty())return "";
  // Android shares commonly put the title on one line and the link on the next.
  // Never turn a second link or multiline commentary into a listing title.
  String withoutLinks=raw.replace(url,"").replaceAll("(?i)https?://\\S+", "");
  String title="";
  for(String line:withoutLinks.split("[\\r\\n]+")){
   String cleaned=line.replaceAll("^[\\s:|\\-]+|[\\s:|\\-]+$","").trim();
   if(cleaned.isEmpty())continue;
   if(!title.isEmpty())return "";
   title=cleaned;
  }
  if(title.length()<3||title.length()>80||title.codePoints().anyMatch(cp->Character.isISOControl(cp)||Character.getType(cp)==Character.FORMAT))return "";
  return title;
 }
}
