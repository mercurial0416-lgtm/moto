package com.motohub.next;
public final class ObdOdoPollPolicyTest {
 private static int count=0;
 private static void expect(boolean expected,boolean actual){count++;if(actual!=expected)throw new AssertionError("read decision #"+count);}
 public static void main(String[] args){
  for(int sample=0;sample<4;sample++)expect(false,ObdOdoPollPolicy.due(sample,true,true,true));
  expect(true,ObdOdoPollPolicy.due(4,true,true,true));
  expect(false,ObdOdoPollPolicy.due(5,true,true,true));
  expect(true,ObdOdoPollPolicy.due(60,true,true,true));
  expect(true,ObdOdoPollPolicy.due(120,true,true,true));
  expect(false,ObdOdoPollPolicy.due(4,false,true,true));
  expect(false,ObdOdoPollPolicy.due(4,true,false,true));
  expect(false,ObdOdoPollPolicy.due(4,true,true,false));
  expect(false,ObdOdoPollPolicy.due(-1,true,true,true));
  System.out.println("PASS ObdOdoPollPolicyTest "+count);
 }
}
