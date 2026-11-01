package com.motohub.next;

/** Process-local guard: never replace bike/ride data while a ride is running or armed. */
final class RestoreRideGate {
 static final String MESSAGE="GPS 라이딩 기록 또는 OBD2 자동 출발 감지를 중지한 뒤 복원해 주세요.";
 private static boolean restoring;
 private static boolean exporting;
 private static boolean formWriting;
 private static int startingRides;
 private RestoreRideGate() {}
 static synchronized boolean blocked(boolean running,boolean armed){return running||armed||restoring||exporting||formWriting||startingRides>0;}
 static synchronized boolean begin(boolean running,boolean armed){
  if(blocked(running,armed))return false;
  restoring=true;return true;
 }
 /** Reserves a brief ride-start window so a background ZIP restore cannot race it. */
 static synchronized boolean beginRideStart(){
  if(restoring||exporting||formWriting)return false;
  startingRides++;return true;
 }
 static synchronized void endRideStart(){if(startingRides>0)startingRides--;}
 /** Reserve a full ZIP export against concurrent ride starts and restores. */
 static synchronized boolean beginExport(boolean running,boolean armed){
  if(blocked(running,armed))return false;
  exporting=true;return true;
 }
 static synchronized void endExport(){exporting=false;}
 static synchronized boolean isExporting(){return exporting;}
 /** Form writes are one-at-a-time and cannot race ZIP export/JSON restore. */
 static synchronized boolean beginFormWrite(){
  if(restoring||exporting||formWriting||startingRides>0)return false;
  formWriting=true;return true;
 }
 static synchronized boolean isFormWriting(){return formWriting;}
 static synchronized void endFormWrite(){formWriting=false;}
 static synchronized boolean isRestoring(){return restoring;}
 static synchronized void end(){restoring=false;}
}
