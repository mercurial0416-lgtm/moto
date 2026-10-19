package com.motohub.next;
public final class OptionalFieldDisclosureTest {
 private static int checks=0;
 static void yes(boolean value, String why) {checks++;if(!value)throw new AssertionError(why);}
 public static void main(String[] ignored){
  yes(!OptionalFieldDisclosure.hasValue("nextDate",null),"missing next date");
  yes(!OptionalFieldDisclosure.hasValue("nextDate","  "),"blank next date");
  yes(OptionalFieldDisclosure.hasValue("nextDate","2026-11-03"),"date must be shown");
  yes(OptionalFieldDisclosure.hasValue("note","  chain adjustment  "),"note must be shown");
  yes(!OptionalFieldDisclosure.hasValue("note","  "),"empty note can stay folded");
  for(String value:new String[]{"0","0.0"," 0.00 ","0e0","-0"}){
   yes(!OptionalFieldDisclosure.hasValue("nextKm",value),"empty mileage placeholder: "+value);
  }
  yes(OptionalFieldDisclosure.hasValue("nextKm","6000"),"service threshold visible");
  yes(OptionalFieldDisclosure.hasValue("nextKm","not-a-number"),"invalid value must remain visible for repair");
  yes(!OptionalFieldDisclosure.hasValue("purchase","0"),"new bike purchase default");
  yes(OptionalFieldDisclosure.hasValue("purchase","1000000"),"existing price visible");
  yes(!OptionalFieldDisclosure.hasValue("distance","0"),"route distance default");
  yes(OptionalFieldDisclosure.hasValue("distance","25.5"),"saved route distance visible");
  yes(OptionalFieldDisclosure.hasValue("insuranceDate","2027-01-01"),"insurance date visible");
  System.out.println("PASS: BY optional field disclosure "+checks+" checks");
 }
}
