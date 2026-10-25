package com.motohub.next;
import java.net.*;
/** HTTPS-only, bounded redirects for signed GitHub release assets. */
final class UpdateDownloadPolicy {
 private UpdateDownloadPolicy(){}
 static final int MAX_REDIRECTS=4;
 static boolean safeRedirect(String source,String location){
  try{
   URI old=new URI(source),next=new URI(location);
   if(!"https".equalsIgnoreCase(old.getScheme())||!"https".equalsIgnoreCase(next.getScheme()))return false;
   if(next.getUserInfo()!=null||next.getFragment()!=null||next.getHost()==null||next.getPort()!=-1)return false;
   String a=old.getHost().toLowerCase(java.util.Locale.ROOT),b=next.getHost().toLowerCase(java.util.Locale.ROOT);
   if(a.equals(b))return true;
   if((a.equals("github.com")||a.equals("raw.githubusercontent.com")) &&
      (b.equals("release-assets.githubusercontent.com")||b.equals("objects.githubusercontent.com")))return true;
   return false;
  }catch(Exception malformed){return false;}
 }
}
