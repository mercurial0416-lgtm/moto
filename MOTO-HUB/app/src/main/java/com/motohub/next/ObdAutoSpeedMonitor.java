package com.motohub.next;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Explicitly armed, read-only ELM327 telemetry with bounded backoff foreground standby. */
final class ObdAutoSpeedMonitor {
 interface Listener {
  void speed(int kmh);
  void disconnected(String reason);
  default void reconnecting(int attempt,int max){}
  default void telemetry(String pid,double value){}
  default boolean allowOdometer(){return false;}
  default void odometer(double km){}
  default void odometerStatus(String reason){}
  default boolean allowDtcDistance(){return false;}
  default void dtcDistance(int km){}
  default void dtcDistanceStatus(String reason){}
  default void connected(){}
  default void capabilities(ObdPidCapabilities capability){}
  default void auxiliaryDiagnostics(String summary){}
  default void linkQuality(long avgMs,int samples,int failures){}
 }
 private static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 private final BluetoothDevice device;
 private final Listener listener;
 private final boolean persistentStandby;
 private volatile BluetoothSocket socket;
 private volatile boolean stopped;
 private Thread worker;
 private volatile long connectedAtNanos;
 private static final class UnsupportedSpeed extends IOException {UnsupportedSpeed(){super("차량 속도 PID 010D 미지원");}}
 ObdAutoSpeedMonitor(BluetoothDevice device,Listener listener){this(device,listener,false);}
 ObdAutoSpeedMonitor(BluetoothDevice device,Listener listener,boolean persistentStandby){
  this.device=device;this.listener=listener;this.persistentStandby=persistentStandby;
 }
 void start(){worker=new Thread(this::readLoop,"moto-obd-autoride");worker.setDaemon(true);worker.start();}
 void stop(){stopped=true;closeSocket();}
 private void closeSocket(){BluetoothSocket s=socket;socket=null;if(s!=null)try{s.close();}catch(Exception ignored){}}
 private void readLoop(){
  String reason="OBD2 연결이 종료됐습니다.";
  int failures=0;
  try{
   while(!stopped){
    try{
     pollConnection();
     if(!stopped)throw new IOException("OBD2 연결이 종료됐습니다.");
    }catch(UnsupportedSpeed e){reason=e.getMessage();break;}
     catch(Exception e){
      if(stopped)break;
      reason=ObdFailureGuide.classify(e.getClass().getSimpleName()+": "+(e.getMessage()==null?"":e.getMessage()));
      // A healthy long session gets a fresh, bounded retry budget.
      failures=ObdConnectionPolicy.nextFailureCount(failures,connectedAtNanos,System.nanoTime());
      // Consume the healthy-session credit. Otherwise every subsequent failed
      // reconnect reuses the stale timestamp and retries forever.
      connectedAtNanos=0;
      // The user-approved foreground standby remains armed across key-off
      // and repeated failures. Back off to one check per minute after the
      // initial eight retries, instead of silently giving up overnight.
      if(!persistentStandby&&
         !ObdConnectionPolicy.shouldRetry(failures,true,true))break;
      if(persistentStandby&&!ObdConnectionPolicy.standbyAllowed(true,stopped))break;
      listener.reconnecting(failures,persistentStandby&&
        failures>ObdConnectionPolicy.MAX_RETRIES?0:ObdConnectionPolicy.MAX_RETRIES);
      closeSocket();
      long until=System.nanoTime()+
       (persistentStandby?ObdConnectionPolicy.standbyDelayMs(failures):
         ObdConnectionPolicy.delayMs(failures))*1_000_000L;
      while(!stopped&&System.nanoTime()<until)Thread.sleep(200);
     }
    finally{closeSocket();}
   }
  }catch(InterruptedException e){Thread.currentThread().interrupt();reason="OBD2 연결 중단";}
  finally{closeSocket();if(!stopped)listener.disconnected(reason);}
 }
 private void pollConnection()throws Exception{
  BluetoothSocket s=device.createRfcommSocketToServiceRecord(SPP);socket=s;
  if(stopped)return;
  s.connect();if(stopped)return;
  InputStream in=s.getInputStream();OutputStream out=s.getOutputStream();
  for(String setup:new String[]{"ATZ\r","ATE0\r","ATL0\r","ATS0\r","ATSP0\r"}){
   query(in,out,setup,4000);if(stopped)return;
  }
  // PID support is a standard ECU read and may be unavailable on cheap adapters.
  // Missing responses mean unknown, never invented values.
  String supported00="",supported20="",supported40="",supportedA0="";
  try{supported00=query(in,out,ObdReadOnlyProtocol.query("0100"),2500);}catch(Exception ignored){}
  try{supported20=query(in,out,ObdReadOnlyProtocol.query("0120"),2500);}catch(Exception ignored){}
  try{supported40=query(in,out,ObdReadOnlyProtocol.query("0140"),2500);}catch(Exception ignored){}
  if(listener.allowOdometer())try{
   supportedA0=query(in,out,ObdReadOnlyProtocol.query("01A0"),2500);
  }catch(Exception ignored){}
  ObdPidCapabilities capabilities=ObdPidCapabilities.fromAllResponses(supported00,supported20,supported40,supportedA0);
  listener.capabilities(capabilities);
  if(ObdSpeedResponsePolicy.unsupported(capabilities.forPid("010D")))throw new UnsupportedSpeed();
  connectedAtNanos=System.nanoTime();listener.connected();
  int unsupported=0,samples=0;
  int priorDtcCandidate=-1,dtcFailures=0;
  long dtcRetryAfterNanos=0L;
  boolean directOdoProbeAttempted=false;
  final boolean dtcCandidateEnabled=listener.allowDtcDistance()&&
    capabilities.forPid("0131")!=ObdPidCapabilities.Support.NO;
  if(listener.allowDtcDistance()&&!dtcCandidateEnabled)
   listener.dtcDistanceStatus("PID 0131 지원 목록에서 미지원 · GPS 추정 사용");
  ObdAuxSensorPolicy auxiliary=new ObdAuxSensorPolicy();
  final ObdPidCapabilities.Support odoSupport=capabilities.forPid("01A6");
  if(listener.allowOdometer()&&odoSupport!=ObdPidCapabilities.Support.YES)
   listener.odometerStatus("ECU ODO 지원 목록 미확인 · 정차 상태에서 1회 직접 확인 예정");
  ObdLatencyStats latency=new ObdLatencyStats();
  while(!stopped){
   long started=System.nanoTime();
   String response;
   try{
    response=query(in,out,ObdReadOnlyProtocol.query("010D"),2500);
    latency.success("010D",(System.nanoTime()-started)/1_000_000L);
    listener.linkQuality(latency.speedAverageMs(),latency.speedSamples(),latency.speedFailures());
   }catch(Exception error){
    latency.failure("010D");
    throw error;
   }
   int speed=ObdReadOnlyProtocol.speedKmh(response);
   if(speed<0){
    // A supported PID can still time out on a noisy adapter. Reconnect rather
    // than permanently disarming; only an explicit negative capability bitmap
    // proves that the ECU does not expose vehicle speed.
    if(ObdSpeedResponsePolicy.retryConnection(++unsupported,capabilities.forPid("010D")))
     throw new IOException("차량 속도 응답 6회 연속 무효 · 안전하게 재연결합니다.");
   }
   else {unsupported=0;listener.speed(speed);}
   // Optional and explicitly opted-in ODO: two independent read-only samples.
   // Failure must never disrupt safety-critical speed-triggered GPS tracking.
   if(!stopped&&ObdOdoPollPolicy.shouldProbe(listener.allowOdometer(),
      odoSupport,speed,samples,directOdoProbeAttempted)){
    if(odoSupport!=ObdPidCapabilities.Support.YES)directOdoProbeAttempted=true;
    try{
     double a=ObdOdometer.parseKm(query(in,out,ObdReadOnlyProtocol.query("01A6"),2500));
     double b=ObdOdometer.parseKm(query(in,out,ObdReadOnlyProtocol.query("01A6"),2500));
     double verified=OdoAutoSyncPolicy.consistent(a,b);
     if(Double.isFinite(verified))listener.odometer(verified);
     else listener.odometerStatus("ODO 2회 수신값이 일치하지 않아 반영 보류");
    }catch(InterruptedException e){throw e;}
    catch(Exception e){listener.odometerStatus("ODO 응답 실패 · 기존 계기판 km 유지");}
   }
   // Rotate low-rate supported sensors without ever requesting a write-capable PID.
   if(!stopped&&++samples%4==0){
    String pid=new String[]{"010C","0105","0142","012F"}[(samples/4-1)%4];
    if((!"012F".equals(pid)||capabilities.forPid(pid)==ObdPidCapabilities.Support.YES)&&
      capabilities.forPid(pid)!=ObdPidCapabilities.Support.NO&&auxiliary.mayPoll(pid,System.nanoTime())){
     long startedAt=System.nanoTime();
     try{
      double value=ObdSensorReading.number(pid,query(in,out,ObdReadOnlyProtocol.query(pid),2500));
      latency.success(pid,(System.nanoTime()-startedAt)/1_000_000L);
      if(ObdSensorReading.valid(pid,value)){
       auxiliary.succeeded(pid,System.nanoTime());
       listener.telemetry(pid,value);
      }else auxiliary.failed(pid,System.nanoTime());
     }catch(InterruptedException e){
      // Shutdown/interruption still terminates the worker normally.
      throw e;
     }catch(Exception e){
      // An optional sensor timing out must not disconnect speed monitoring or
      // split a GPS ride. After two failures, retry it after a quiet minute.
      auxiliary.failed(pid,System.nanoTime());
      latency.failure(pid);
     }
     listener.auxiliaryDiagnostics(auxiliary.summary(System.nanoTime())+
      "\\n응답시간(최근 8회): "+latency.summary());
    }
   }
   // Periodic, explicitly opted-in PID 0131 fallback. It does not require
   // ODO PID A6; its value is not an actual vehicle lifetime odometer.
   // Require two plausible reads across different speed cycles. Unlike a
   // failed speed PID, failure never stops GPS capture or the bike session.
   if(!stopped&&samples%20==0&&dtcCandidateEnabled&&
      DtcPollingRetry.due(System.nanoTime(),dtcRetryAfterNanos)){
    try{
     int counter=DtcDistanceSinceClear.parseKm(query(in,out,DtcDistanceSinceClear.REQUEST,2200));
     if(counter>=0){
      dtcFailures=0;
      // Always require two separate, mutually consistent decoded replies.
      // A single corrupted low value must never pause this bike's ODO
      // calibration; the next low reply confirms a genuine decrease.
      int verified=DtcCounterConfirmPolicy.confirmed(priorDtcCandidate,counter);
      if(verified>=0)listener.dtcDistance(verified);
      priorDtcCandidate=counter;
     }else if(++dtcFailures>=DtcPollingRetry.FAILURE_THRESHOLD){
      dtcRetryAfterNanos=System.nanoTime()+DtcPollingRetry.COOLDOWN_MS*1_000_000L;
      dtcFailures=0;priorDtcCandidate=-1;
      listener.dtcDistanceStatus("PID 0131 수신 실패 · 60초 후 자동 재시도 · GPS 기록 유지");
     }
    }catch(InterruptedException e){throw e;}
    catch(Exception e){
     if(++dtcFailures>=DtcPollingRetry.FAILURE_THRESHOLD){
      dtcRetryAfterNanos=System.nanoTime()+DtcPollingRetry.COOLDOWN_MS*1_000_000L;
      dtcFailures=0;priorDtcCandidate=-1;
      listener.dtcDistanceStatus("PID 0131 타임아웃 · 60초 후 자동 재시도 · GPS 기록 유지");
     }
    }
   }
   if(!stopped)Thread.sleep(ObdSpeedResponsePolicy.pollDelayMs(speed));
  }
 }
 private String query(InputStream in,OutputStream out,String command,long timeoutMs)throws Exception{
  if(stopped)throw new IOException("OBD2 감지가 종료됐습니다.");
  int cleared=0;while(in.available()>0&&cleared++<1024)in.read();
  out.write(command.getBytes(StandardCharsets.US_ASCII));out.flush();
  StringBuilder response=new StringBuilder();long until=System.nanoTime()+timeoutMs*1_000_000L;
  while(!stopped&&System.nanoTime()<until&&response.length()<2048){
   if(in.available()==0){Thread.sleep(30);continue;}
   int c=in.read();if(c<0)throw new IOException("OBD2 stream closed");
   if(c=='>')return response.toString();response.append((char)c);
  }
  if(stopped)throw new IOException("OBD2 감지가 종료됐습니다.");
  // An ELM reply without the terminating prompt is incomplete, even with bytes received.
  throw new IOException("OBD2 응답 완료 신호 없음");
 }
}
