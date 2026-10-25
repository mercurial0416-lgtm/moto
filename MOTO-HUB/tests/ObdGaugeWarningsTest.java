package com.motohub.next;
public final class ObdGaugeWarningsTest {
 static int checks;
 static void check(boolean ok,String name){checks++;if(!ok)throw new AssertionError(name);}
 public static void main(String[] args){
  check(ObdGaugeWarnings.rpmBar(Double.NaN)==0,"no stale RPM");
  check(ObdGaugeWarnings.rpmBar(-1)==0,"negative");
  check(ObdGaugeWarnings.rpmBar(5200.5)==5201,"rounded RPM");
  check(ObdGaugeWarnings.rpmBar(18000)==15000,"bounded meter");
  check(ObdGaugeWarnings.advisory(Double.NaN,Double.NaN,Double.NaN).contains("참고용"),"unknown does not alarm");
  check(ObdGaugeWarnings.advisory(Double.NaN,111,Double.NaN).contains("냉각수"),"hot coolant");
  check(ObdGaugeWarnings.advisory(1700,85,11.4).contains("전압 낮음"),"engine-on low voltage");
  check(ObdGaugeWarnings.advisory(1700,85,16.0).contains("전압 높음"),"engine-on high voltage");
  check(ObdGaugeWarnings.advisory(0,85,11.4).contains("참고용"),"engine-off does not imply charging defect");
  check(ObdGaugeWarnings.advisory(Double.NaN,80,14.1).contains("참고용"),"no engine evidence");
  System.out.println("PASS ObdGaugeWarningsTest "+checks);
 }
}
