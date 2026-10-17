package com.motohub.next;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public final class PhotoRetryTest {
 static int checks;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args){
  AtomicInteger calls=new AtomicInteger();
  String result=PhotoRetry.firstSuccess(Arrays.asList("bad","good","later"),3,item->{
   calls.incrementAndGet();return "good".equals(item)?item:null;
  });
  check("good".equals(result),"second image must be tried");
  check(calls.get()==2,"stop after successful image");
  calls.set(0);
  result=PhotoRetry.firstSuccess(Arrays.asList("throws","good"),3,item->{
   calls.incrementAndGet();if("throws".equals(item))throw new IllegalStateException("broken image");return item;
  });
  check("good".equals(result),"exception must not prevent next image");
  check(calls.get()==2,"retry after exception");
  calls.set(0);
  result=PhotoRetry.firstSuccess(Arrays.asList("bad1","bad2","good"),2,item->{
   calls.incrementAndGet();return "good".equals(item)?item:null;
  });
  check(result==null,"never exceed attempt limit");
  check(calls.get()==2,"attempt limit enforced");
  check(PhotoRetry.firstSuccess(Collections.emptyList(),3,item->item)==null,"empty results");
  check(PhotoRetry.firstSuccess(Arrays.asList("good"),0,item->item)==null,"zero attempts");
  System.out.println("PASS: "+checks+" photo fallback checks");
 }
}
