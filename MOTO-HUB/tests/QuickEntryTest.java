package com.motohub.next;
public final class QuickEntryTest {
 static int count=0;
 static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError("expected="+expected+" got="+actual);count++;}
 public static void main(String[] args){
  eq("https://example.com/a?x=1",QuickEntry.extractUrl("매물: https://example.com/a?x=1"));
  eq("https://example.com/b",QuickEntry.extractUrl("https://example.com/b."));
  eq("https://example.com/b",QuickEntry.extractUrl("(https://example.com/b)"));
  eq("https://example.com/c",QuickEntry.extractUrl("https://example.com/c\n참고"));
  eq("",QuickEntry.extractUrl(null));
  eq("",QuickEntry.extractUrl("no url"));
  eq("",QuickEntry.extractUrl("javascript:alert(1)"));
  eq("",QuickEntry.extractUrl("https://user:pass@example.com/"));
  eq("",QuickEntry.extractUrl("https://"));
  eq("CBR500R 2025",QuickEntry.sharedTitle("CBR500R 2025 https://example.com/a","https://example.com/a"));
  eq("",QuickEntry.sharedTitle("https://example.com/a","https://example.com/a"));
  eq("",QuickEntry.sharedTitle("\n\nhttps://example.com/a","https://example.com/a"));
  System.out.println("PASS QuickEntryTest: "+count+" checks");
 }
}
