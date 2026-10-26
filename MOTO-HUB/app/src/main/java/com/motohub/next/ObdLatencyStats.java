package com.motohub.next;

/** Session-only bounded read-only OBD query latency; never stores Bluetooth IDs or ECU values. */
final class ObdLatencyStats {
 private static final String[] PIDS={"010D","010C","0105","0142"};
 private static final String[] NAMES={"속도","RPM","수온","전압"};
 private static final int WINDOW=8;
 private final long[][] values=new long[PIDS.length][WINDOW];
 private final int[] count=new int[PIDS.length],failures=new int[PIDS.length];
 private int slot(String pid){
  for(int i=0;i<PIDS.length;i++)if(PIDS[i].equals(pid))return i;
  return -1;
 }
 void success(String pid,long durationMs){
  int i=slot(pid);if(i<0||durationMs<0||durationMs>30_000)return;
  values[i][count[i]%WINDOW]=durationMs;
  if(count[i]<Integer.MAX_VALUE)count[i]++;
 }
 void failure(String pid){
  int i=slot(pid);if(i>=0&&failures[i]<Integer.MAX_VALUE)failures[i]++;
 }
 String summary(){
  StringBuilder result=new StringBuilder();
  for(int i=0;i<PIDS.length;i++){
   if(i>0)result.append(" · ");
   result.append(NAMES[i]).append(' ');
   int n=Math.min(count[i],WINDOW);
   if(n==0)result.append("대기");
   else{
    long total=0;
    for(int j=0;j<n;j++)total+=values[i][j];
    result.append("평균 ").append((total+n/2)/n).append("ms");
   }
   if(failures[i]>0)result.append(" / 실패 ").append(failures[i]).append("회");
  }
  return result.toString();
 }
}
