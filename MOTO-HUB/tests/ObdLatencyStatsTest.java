package com.motohub.next;
public final class ObdLatencyStatsTest {
 private static int checks;
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  ObdLatencyStats x=new ObdLatencyStats();
  check(x.summary().contains("속도 대기"),"unknown never invented");
  x.success("010D",100);x.success("010D",200);
  check(x.summary().contains("속도 평균 150ms"),"moving measurement");
  x.failure("010C");
  check(x.summary().contains("RPM 대기 / 실패 1회"),"failures without values");
  x.success("ABCD",10);
  check(!x.summary().contains("ABCD"),"unrecognized PID ignored");
  x.success("010D",-1);
  check(x.summary().contains("속도 평균 150ms"),"invalid negative ignored");
  for(int i=0;i<8;i++)x.success("010D",400);
  check(x.summary().contains("속도 평균 400ms"),"rolling eight-window replaces stale data");
  x.success("0105",0);x.success("0142",300);
  check(x.summary().contains("수온 평균 0ms"),"zero ms allowed");
  check(x.summary().contains("전압 평균 300ms"),"voltage stats");
  x.success("0142",35_000);
  check(x.summary().contains("전압 평균 300ms"),"absurd duration ignored");
  System.out.println("PASS ObdLatencyStatsTest "+checks);
 }
}