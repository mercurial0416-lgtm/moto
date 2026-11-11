package com.motohub.next;
public final class MapPathDetailPolicyTest{
 public static void main(String[] args){
  if(!MapPathDetailPolicy.corner(37,127,37.00015,127,37.00015,127.0002,5))
   throw new AssertionError("turn missing");
  if(MapPathDetailPolicy.corner(37,127,37.00015,127,37.0003,127,5))
   throw new AssertionError("straight route marked as corner");
  if(MapPathDetailPolicy.corner(37,127,37.00015,127,37.00015,127.0002,41))
   throw new AssertionError("low accuracy corner accepted");
  System.out.println("PASS MapPathDetailPolicyTest");
 }
}
