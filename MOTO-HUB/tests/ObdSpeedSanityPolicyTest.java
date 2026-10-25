package com.motohub.next;
public final class ObdSpeedSanityPolicyTest {
 static int checks;
 static void check(boolean b,String name){checks++;if(!b)throw new AssertionError(name);}
 public static void main(String[] args){
  ObdSpeedSanityPolicy p=new ObdSpeedSanityPolicy();
  check(p.accept(0,1000),"start");
  check(p.accept(7,2000),"normal start speed");
  check(p.accept(22,3000),"normal acceleration");
  check(!p.accept(170,4000),"reject unrealistic spike");
  check(p.accept(30,4100),"ignore spike without poisoning next frame");
  check(!p.accept(201,4300),"reject speed above max");
  check(!p.accept(-1,4400),"negative data");
  check(!p.accept(33,1000),"rollback time");
  check(p.accept(150,15000),"long gap reset");
  check(p.accept(151,16500),"new stable sequence");
  check(!p.accept(0,17500),"reject impossible drop");
  p.reset();
  check(p.accept(0,20000),"reset");
  check(p.accept(60,21000),"borderline acceleration");
  check(!p.accept(121,22000),"above threshold");
  System.out.println("PASS ObdSpeedSanityPolicyTest "+checks);
 }
}
