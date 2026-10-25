package com.motohub.next;
/** Regression: never save a stale calculated fuel amount after incomplete or invalid input. */
public final class FuelAutoAmountTest {
 static int assertions;
 static void eq(String expected,String actual,String note){
  assertions++;if(!expected.equals(actual))throw new AssertionError(note+": "+actual+" expected "+expected);
 }
 public static void main(String[] args){
  FuelAutoAmount entry=new FuelAutoAmount();
  eq("9000",entry.update("","5","1800"),"valid calculation");
  eq("",entry.update("9000","","1800"),"blank liters clears auto result");
  eq("9000",entry.update("9000","5","1800"),"valid after edit");
  eq("9250",entry.update("9000","5","1850"),"new price overwrites old calculated amount");
  eq("",entry.update("9250","5",""),"blank unit price clears auto result");
  eq("9411",entry.update("","5.25","1792.5"),"KRW rounded");
  eq("9100",entry.update("9100","bad","1792.5"),"manually overridden amount retained when invalid");
  eq("",entry.update("","5","0"),"zero price is invalid");
  eq("",entry.update("","0","1800"),"zero liters invalid");
  eq("8000",entry.update("8000",null,"1800"),"null input preserves manual amount");
  eq("9000",entry.update("","5","1,800"),"thousands separator accepted");
  entry.reset();
  eq("9000",entry.update("9000","",""),"manual reused amount not cleared after reset");
  entry.reset();
  eq("9000",entry.update("","5","1800"),"calculate for later manual edit");
  entry.noteAmountEdited("9000"); // programmatic setText of our generated amount
  eq("9250",entry.update("9000","5","1850"),"generated value may still update");
  entry.noteAmountEdited("9300"); // explicit user edit
  eq("9300",entry.update("9300","6","1800"),"manual correction survives liters edit");
  eq("9300",entry.update("9300","6","1700"),"manual correction survives unit price edit");
  entry.noteAmountEdited(""); // clearing amount means automatic entry again
  eq("10200",entry.update("","6","1700"),"clearing restores auto calculation");
  entry.noteAmountEdited("10200");
  entry.reset();
  eq("9000",entry.update("","5","1800"),"reset starts a new automatic session");
  System.out.println("PASS safe auto fuel amount: "+assertions+" cases");
 }
}
