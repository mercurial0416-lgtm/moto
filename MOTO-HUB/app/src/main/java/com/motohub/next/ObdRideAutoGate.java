package com.motohub.next;

/** Pure state machine for optional OBD speed-triggered GPS rides. No GPS or ECU writes. */
final class ObdRideAutoGate {
 enum Event { NONE, START, FINISH }
 static final int START_KMH=7, KEEP_MOVING_KMH=4, FINISH_KMH=2;
 static final int REQUIRED_MOVING_SAMPLES=3;
 static final long STOP_AFTER_MS=180_000L;
 static final long IGNITION_OFF_GRACE_MS=15_000L;
 private long stopAfterMs=STOP_AFTER_MS;
 void setStopAfterMs(long value){
  if(value!=180_000L&&value!=300_000L&&value!=600_000L)
   throw new IllegalArgumentException("지원되지 않는 정차 간격");
  if(recording)throw new IllegalStateException("기록 중 변경 불가");
  stopAfterMs=value;movingSamples=0;stationarySince=-1;
 }
 long stopAfterMs(){return stopAfterMs;}
 private int movingSamples;
 private boolean recording;
 private long stationarySince=-1;
 private long lastSample=-1;

 Event accept(int speedKmh,long elapsedMs){
  if(elapsedMs<0||lastSample>elapsedMs)return Event.NONE;
  if(lastSample>=0&&elapsedMs-lastSample>15_000){movingSamples=0;stationarySince=-1;}
  lastSample=elapsedMs;
  if(speedKmh<0||speedKmh>200){movingSamples=0;stationarySince=-1;return Event.NONE;}
  if(!recording){
   if(speedKmh>=START_KMH){if(++movingSamples>=REQUIRED_MOVING_SAMPLES){recording=true;movingSamples=0;stationarySince=-1;return Event.START;}}
   else movingSamples=0;
   return Event.NONE;
  }
  if(speedKmh>=KEEP_MOVING_KMH){stationarySince=-1;return Event.NONE;}
  if(speedKmh<=FINISH_KMH){
   if(stationarySince<0)stationarySince=elapsedMs;
   if(elapsedMs-stationarySince>=stopAfterMs){recording=false;stationarySince=-1;return Event.FINISH;}
  }else stationarySince=-1;
  return Event.NONE;
 }
 /** A verified GPS movement cancels a spurious ECU zero-speed stop countdown. */
 void gpsMovement(double gpsKmh,long elapsedMs){
  if(!recording||!Double.isFinite(gpsKmh)||gpsKmh<8||elapsedMs<lastSample)return;
  stationarySince=-1;
 }
 /** -1: moving/not recording; otherwise time until this rest becomes a trip end. */
 long stationaryRemainingMs(long elapsedMs){
  if(!recording||stationarySince<0||elapsedMs<stationarySince||lastSample<0||elapsedMs-lastSample>15_000)return -1;
  return Math.max(0,stopAfterMs-(elapsedMs-stationarySince));
 }
 boolean stoppedBeforeDisconnect(long elapsedMs){return recording&&stationarySince>=0&&elapsedMs-stationarySince>=IGNITION_OFF_GRACE_MS;}
 /** Keep the active ride but reset idle detection when samples go stale. */
 void connectionLost(){movingSamples=0;stationarySince=-1;lastSample=-1;}
 boolean recording(){return recording;}
 void reset(){recording=false;movingSamples=0;stationarySince=-1;lastSample=-1;}
}
