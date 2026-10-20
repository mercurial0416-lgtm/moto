package com.motohub.next;

/** Shared ZIP limits between exporter, readback validator and restorer. */
final class BackupFormat {
 private BackupFormat() {}
 static final long MAX_TOTAL_BYTES=250_000_000L;
 static final long MAX_JSON_BYTES=8_000_000L;
 static final long MAX_TRACK_BYTES=25_000_000L;
 static final long MAX_GPX_BYTES=50_000_000L;
 static final int MAX_TRACKS=10_000;
 static final int MAX_PHOTOS=1_000;
 static final int MAX_RECEIPTS=1_000;
 static final int MAX_ENTRIES=100_000;
 static final String RECEIPT="record-receipt-[a-fA-F0-9]{8}(?:-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}(?:-[2-5])?\\.jpg";
 static final String PHOTO="garage-photo-[a-fA-F0-9-]{36}\\.jpg";
 static final String TRACK="[0-9]{10,15}-[a-f0-9-]{36}\\.track";
 static String sidecarSuffix(String name){
  for(String suffix:new String[]{".done",".imported",".recovered",".unmerged",".mergedinto",".bike",".category"})
   if(name.endsWith(suffix))return suffix;
  return "";
 }
 static boolean isTrack(String name){return name!=null&&name.matches("rides/"+TRACK);}
 static boolean isReceipt(String name){return name!=null&&name.matches("receipts/"+RECEIPT);}
 static boolean isPhoto(String name){return name!=null&&name.matches("photos/"+PHOTO);}
 static boolean isGpx(String name){return name!=null&&name.matches("gpx/[0-9]{10,15}-[a-f0-9-]{36}\\.gpx");}
 static boolean isSidecar(String name){
  if(name==null||!name.startsWith("rides/"))return false;
  String suffix=sidecarSuffix(name);
  return !suffix.isEmpty()&&name.substring(0,name.length()-suffix.length()).matches("rides/"+TRACK);
 }
 static boolean isDirectory(String name){return "rides/".equals(name)||"photos/".equals(name)||"receipts/".equals(name)||"gpx/".equals(name);}
 static boolean allowed(String name,boolean directory){
  if(name==null||name.startsWith("/")||name.contains("..")||name.contains("\\")||name.indexOf('\0')>=0)return false;
  if(directory)return isDirectory(name);
  return "MOTO-HUB-data.json".equals(name)||"README-KO.txt".equals(name)||isPhoto(name)||isReceipt(name)||isTrack(name)||isGpx(name)||isSidecar(name);
 }
 static long entryLimit(String name){
  if("MOTO-HUB-data.json".equals(name))return MAX_JSON_BYTES;
  if("README-KO.txt".equals(name))return 8192;
  if(isGpx(name))return MAX_GPX_BYTES;
  if(isSidecar(name)){
   if(name.endsWith(".mergedinto"))return 100;
   if(name.endsWith(".category")||name.endsWith(".bike"))return 64;
   return 0;
  }
  return MAX_TRACK_BYTES;
 }
}
