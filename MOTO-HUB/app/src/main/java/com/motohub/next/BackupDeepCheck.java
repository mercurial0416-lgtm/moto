package com.motohub.next;

import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Read-only content validation: verifies local DB and up to 150 actual GPS tracks.
 * This is not a substitute for export -> ZIP CRC -> import verification.
 */
final class BackupDeepCheck {
 private BackupDeepCheck(){}
 static final int MAX_TRACKS=150;
 static String check(File files)throws Exception{
  if(files==null||!files.isDirectory())throw new IOException("앱 데이터 폴더 없음");
  File db=new File(files,"motohub-v3.json");
  byte[] bytes=new AtomicFile(db).readFully();
  if(bytes.length==0||bytes.length>=8_000_000)throw new IOException("앱 데이터 파일의 크기가 비정상입니다.");
  Store.parse(new String(bytes,StandardCharsets.UTF_8));
  int valid=0,damaged=0,skipped=0;
  for(File track:TrackFile.tracks(files)){
   if(valid+damaged>=MAX_TRACKS){skipped++;continue;}
   try{if(TrackFile.read(track,(lat,lon,accuracy,time)->{})<=0)damaged++;
       else valid++;}
   catch(Exception failure){damaged++;}
  }
  return "데이터 JSON 구조: 정상\nGPS 원본 검증 "+valid+"건 · 손상/빈 파일 "+damaged+"건"+
    (skipped>0?"\n최신 "+MAX_TRACKS+"건만 점검 · 나머지 "+skipped+"건 미검증":"")+
    "\n이 검사는 앱 내부 원본만 검사합니다. 외부 백업은 ZIP 내보내기 후 재검증이 필요합니다.";
 }
}
