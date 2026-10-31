package com.motohub.next;

import java.util.Arrays;

/** Keeps one document-picker handoff across Activity recreation, never on disk or in a Bundle.
 * Process death deliberately invalidates the handoff; an old picker result cannot write a
 * bogus empty ZIP or persist the encryption password. */
final class BackupPickerTransfer {
 private static final long MAX_AGE_NANOS=60L*60L*1_000_000_000L;
 private static Entry pending;
 private BackupPickerTransfer(){}
 static final class Entry {
  final int request;
  final String json;
  final char[] password;
  final long createdAt;
  Entry(int request,String json,char[] password,long createdAt){
   this.request=request;this.json=json==null?"":json;
   this.password=password==null?null:Arrays.copyOf(password,password.length);
   this.createdAt=createdAt;
  }
  void clearPassword(){if(password!=null)Arrays.fill(password,'\0');}
 }
 static synchronized void begin(int request,String json,char[] password) {
  if(pending!=null)pending.clearPassword();
  pending=new Entry(request,json,password,System.nanoTime());
  if(password!=null)Arrays.fill(password,'\0');
 }
 static synchronized Entry take(int request) {
  if(pending==null)return null;
  long elapsed=System.nanoTime()-pending.createdAt;
  if(elapsed<0||elapsed>MAX_AGE_NANOS){
   pending.clearPassword();pending=null;return null;
  }
  if(pending.request!=request)return null;
  Entry result=pending;pending=null;return result;
 }
 static synchronized void cancel(int request){
  if(pending!=null&&pending.request==request){
   pending.clearPassword();pending=null;
  }
 }
}
