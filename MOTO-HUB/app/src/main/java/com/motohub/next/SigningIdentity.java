package com.motohub.next;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

/** Public signing certificate fingerprints only. Never reads or exports signing private keys. */
final class SigningIdentity {
 private SigningIdentity(){}
 static String current(Context context){
  try{
   PackageManager pm=context.getPackageManager();
   int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
   PackageInfo info=pm.getPackageInfo(context.getPackageName(),flags);
   Signature[] signers=Build.VERSION.SDK_INT>=28&&info.signingInfo!=null?
    info.signingInfo.getApkContentsSigners():info.signatures;
   if(signers==null||signers.length==0)return "";
   ArrayList<String> digests=new ArrayList<>();
   for(Signature signer:signers){
    byte[] hash=MessageDigest.getInstance("SHA-256").digest(signer.toByteArray());
    StringBuilder hex=new StringBuilder(64);
    for(byte b:hash)hex.append(String.format(Locale.ROOT,"%02x",b&0xff));
    digests.add(hex.toString());
   }
   Collections.sort(digests);
   return String.join(", ",digests);
  }catch(Exception unavailable){return "";}
 }
}
