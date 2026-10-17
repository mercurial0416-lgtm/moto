package com.motohub.next;

public final class PhotoRequestGuardTest {
 static int checks=0;
 static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
 public static void main(String[] args){
  Object preview=new Object();
  long honda2024=PhotoRequestGuard.begin(preview);
  check(PhotoRequestGuard.isCurrent(preview,honda2024),"first request should be active");
  long yamaha2023=PhotoRequestGuard.begin(preview);
  check(!PhotoRequestGuard.isCurrent(preview,honda2024),"stale request must not repaint preview");
  check(PhotoRequestGuard.isCurrent(preview,yamaha2023),"new model/year should own preview");
  long yamaha2023Again=PhotoRequestGuard.begin(preview);
  check(!PhotoRequestGuard.isCurrent(preview,yamaha2023),"same model requested again also invalidates old response");
  check(PhotoRequestGuard.isCurrent(preview,yamaha2023Again),"latest repeated request is active");
  Object garage=new Object();
  long garageToken=PhotoRequestGuard.begin(garage);
  check(PhotoRequestGuard.isCurrent(garage,garageToken),"different image view must remain independent");
  PhotoRequestGuard.cancel(preview);
  check(!PhotoRequestGuard.isCurrent(preview,yamaha2023Again),"dismissed preview rejects late response");
  check(PhotoRequestGuard.isCurrent(garage,garageToken),"cancel must not affect other view");
  check(!PhotoRequestGuard.isCurrent(new Object(),garageToken),"unregistered view rejects response");
  boolean threw=false;
  try{PhotoRequestGuard.begin(null);}catch(IllegalArgumentException expected){threw=true;}
  check(threw,"null target should fail explicitly");
  System.out.println("PASS: "+checks+" photo request race checks");
 }
}
