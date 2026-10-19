package com.motohub.next;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
/** Informational only. Local writes never depend on connectivity checks. */
final class OfflineStatus {
 private OfflineStatus(){}
 static boolean hasInternet(Context c){
  try{ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);
   if(cm==null)return true;
   NetworkCapabilities caps=cm.getNetworkCapabilities(cm.getActiveNetwork());
   return caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
  }catch(Exception ignored){return true;}
 }
}
