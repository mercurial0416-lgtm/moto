package com.motohub.next;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Safe per-record attachment names; slot one preserves all existing ZIP backups. */
final class ReceiptFiles {
 private ReceiptFiles(){}
 static final int MAX_PER_RECORD=5;
 private static final String UUID="[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}";
 static boolean validId(String id){return id!=null&&id.matches(UUID);}
 static String name(String id,int slot){
  if(!validId(id)||slot<1||slot>MAX_PER_RECORD)throw new IllegalArgumentException("영수증 ID/번호 오류");
  return "record-receipt-"+id+(slot==1?"":"-"+slot)+".jpg";
 }
 static boolean recognized(String name){
  return name!=null&&name.matches("record-receipt-"+UUID+"(?:-[2-5])?\\.jpg");
 }
 static File file(File root,String id,int slot){return new File(root,name(id,slot));}
 static List<File> existing(File root,String id){
  List<File> result=new ArrayList<>();if(!validId(id))return result;
  for(int n=1;n<=MAX_PER_RECORD;n++){File f=file(root,id,n);if(f.isFile())result.add(f);}
  return result;
 }
 static int nextSlot(File root,String id){
  if(!validId(id))return -1;
  for(int n=1;n<=MAX_PER_RECORD;n++)if(!file(root,id,n).exists())return n;
  return -1;
 }
}
