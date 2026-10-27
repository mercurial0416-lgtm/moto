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
 long speedAverageMs(){
  int n=Math.min(count[0],WINDOW);if(n==0)return -1;
  long total=0;for(int j=0;j<n;j++)total+=values[0][j];
  return (total+n/2)/n;
 }
 int speedSamples(){return count[0];}
 int speedFailures(){return failures[0];}
 String speedTrend(){
  int n=Math.min(count[0],WINDOW);if(n<4)return "추세 데이터 부족";
  int latest=(count[0]-1)%WINDOW;
  int oldest=(count[0]-n)%WINDOW;
  long current=values[0][latest],past=values[0][oldest];
  if(current>past+250)return "응답 느려지는 중";
  if(current+250<past)return "응답 개선 중";
  return "응답 대체로 일정";
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
