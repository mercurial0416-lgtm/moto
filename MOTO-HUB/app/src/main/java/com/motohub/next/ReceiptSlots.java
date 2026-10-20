package com.motohub.next;
import java.io.File;
import java.util.*;
/** Bounded, backwards-compatible attachment naming. Slot 1 is the original receipt filename. */
final class ReceiptSlots {
 static final int MAX=4;
 private ReceiptSlots(){}
 static boolean validId(String id){return id!=null&&id.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}");}
 static String name(String id,int slot){if(!validId(id)||slot<1||slot>MAX)throw new IllegalArgumentException("잘못된 영수증 식별자");return "record-receipt-"+id+(slot==1?"":"-"+slot)+".jpg";}
 static File file(File dir,String id,int slot){if(dir==null||!validId(id)||slot<1||slot>MAX)return null;return new File(dir,name(id,slot));}
 static int count(File dir,String id){int found=0;for(int s=1;s<=MAX;s++){File f=file(dir,id,s);if(f!=null&&f.isFile())found++;}return found;}
 static int nextFree(File dir,String id){for(int s=1;s<=MAX;s++){File f=file(dir,id,s);if(f!=null&&!f.exists())return s;}return 0;}
 static List<File> existing(File dir,String id){List<File> found=new ArrayList<>();for(int s=1;s<=MAX;s++){File f=file(dir,id,s);if(f!=null&&f.isFile())found.add(f);}return found;}
}
