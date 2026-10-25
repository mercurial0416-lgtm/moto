package com.motohub.next;

public final class PhotoGalleryLoadGateTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 public static void main(String[] args){
  PhotoGalleryLoadGate gate=new PhotoGalleryLoadGate();
  int first=gate.begin();check(gate.accepts(first),"first request accepted");
  int retry=gate.begin();check(!gate.accepts(first),"old search result rejected after retry");
  check(gate.accepts(retry),"new search result accepted");
  gate.close();check(!gate.accepts(retry),"dismissed dialog rejects result");
  check(gate.begin()==-1,"closed dialog cannot start another search");
  check(!gate.accepts(-1),"invalid request rejected");
  System.out.println("PhotoGalleryLoadGateTest PASS (6 assertions)");
 }
}
