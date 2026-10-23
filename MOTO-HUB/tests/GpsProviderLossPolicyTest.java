package com.motohub.next;
public final class GpsProviderLossPolicyTest {
 static int checks=0;
 static void eq(GpsProviderLossPolicy.Action expected,GpsProviderLossPolicy.Action actual,String note){
  checks++;if(expected!=actual)throw new AssertionError(note+": "+actual+" expected "+expected);
 }
 public static void main(String[] args){
  eq(GpsProviderLossPolicy.Action.IGNORE,GpsProviderLossPolicy.onLoss(false,false),"idle GPS change");
  eq(GpsProviderLossPolicy.Action.IGNORE,GpsProviderLossPolicy.onLoss(true,false),"consented manual GPS track stays running");
  eq(GpsProviderLossPolicy.Action.DISARM_WAITING,GpsProviderLossPolicy.onLoss(false,true),"OBD waiting without GPS track ends");
  eq(GpsProviderLossPolicy.Action.KEEP_ACTIVE_TRACK,GpsProviderLossPolicy.onLoss(true,true),"recorded GPS track not discarded");
  System.out.println("PASS GPS provider toggle preservation: "+checks);
 }
}
