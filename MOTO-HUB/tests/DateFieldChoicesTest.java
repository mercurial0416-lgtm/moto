package com.motohub.next;

public final class DateFieldChoicesTest {
 private static int checks;
 private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  for(String key:new String[]{"purchaseDate","soldDate","insuranceDate","nextDate"}){
   check(DateFieldChoices.isDateField(key),key+" recognized");
   for(String form:new String[]{"vehicles","listings","services","expenses","routes","gear"})
    check(DateFieldChoices.canClear(form,key),key+" must be clearable on "+form);
  }
  check(DateFieldChoices.isDateField("date"),"date recognized");
  check(!DateFieldChoices.canClear("services","date"),"service transaction date required");
  check(!DateFieldChoices.canClear("expenses","date"),"expense transaction date required");
  check(DateFieldChoices.canClear("routes","date"),"route date optional");
  check(DateFieldChoices.canClear("gear","date"),"gear date optional");
  check(!DateFieldChoices.isDateField("mileage"),"non-date field excluded");
  check(!DateFieldChoices.canClear("vehicles","mileage"),"non-date field cannot be cleared as date");
  System.out.println("PASS: "+checks+" optional date field policy checks");
 }
}
