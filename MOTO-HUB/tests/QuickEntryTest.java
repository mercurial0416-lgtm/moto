package com.motohub.next;
public final class QuickEntryTest {
 static int count=0;
 static void eq(String expected,String actual){if(!expected.equals(actual))throw new AssertionError("expected="+expected+" got="+actual);count++;}
 public static void main(String[] args){
  eq("https://example.com/a?x=1",QuickEntry.extractUrl("매물: https://example.com/a?x=1"));
  eq("https://example.com/b",QuickEntry.extractUrl("https://example.com/b."));
  eq("https://example.com/b",QuickEntry.extractUrl("(https://example.com/b)"));
  eq("https://example.com/item(2024)",QuickEntry.extractUrl("https://example.com/item(2024)"));
  eq("https://example.com/item(2024)",QuickEntry.extractUrl("(https://example.com/item(2024))"));
  eq("https://example.com/c",QuickEntry.extractUrl("https://example.com/c\n참고"));
  eq("https://example.com/b",QuickEntry.extractUrl("“https://example.com/b”"));
  eq("https://example.com/b",QuickEntry.extractUrl("「https://example.com/b」"));
  eq("",QuickEntry.extractUrl(null));
  eq("",QuickEntry.extractUrl("no url"));
  eq("",QuickEntry.extractUrl("javascript:alert(1)"));
  eq("",QuickEntry.extractUrl("https://user:pass@example.com/"));
  eq("",QuickEntry.extractUrl("https://"));
  eq("",QuickEntry.extractUrl("https://localhost/path"));
  eq("",QuickEntry.extractUrl("https://example.com:0/path"));
  eq("",QuickEntry.extractUrl("https://example.com/\u200Bhidden"));
  eq("CBR500R 2025",QuickEntry.sharedTitle("CBR500R 2025 https://example.com/a","https://example.com/a"));
  eq("",QuickEntry.sharedTitle("https://example.com/a","https://example.com/a"));
  eq("",QuickEntry.sharedTitle("\n\nhttps://example.com/a","https://example.com/a"));
  eq("2024 듀크390 판매",QuickEntry.sharedTitle("2024 듀크390 판매\nhttps://example.com/duke","https://example.com/duke"));
  eq("CBR500R 매물",QuickEntry.sharedTitle("CBR500R 매물\r\n\r\nhttps://example.com/a","https://example.com/a"));
  eq("",QuickEntry.sharedTitle("매물 설명\n추가 의견\nhttps://example.com/a","https://example.com/a"));
  eq("",QuickEntry.sharedTitle("위험\u200B제목 https://example.com/a","https://example.com/a"));
  System.out.println("PASS QuickEntryTest: "+count+" checks");
 }
}
