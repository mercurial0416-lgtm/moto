package com.motohub.next;
public final class ListPageLimitTest {
 static int pass=0;
 static void eq(int actual,int expected){if(actual!=expected)throw new AssertionError(actual+" != "+expected);pass++;}
 static void ok(boolean bool){if(!bool)throw new AssertionError();pass++;}
 public static void main(String[] args){
  eq(ListPageLimit.start(0,0),0);eq(ListPageLimit.end(0,0),0);
  eq(ListPageLimit.end(10,0),10);eq(ListPageLimit.end(10000,0),30);
  eq(ListPageLimit.next(10000,0),30);eq(ListPageLimit.end(10000,30),60);
  eq(ListPageLimit.previous(10000,30),0);eq(ListPageLimit.previous(10000,0),0);
  eq(ListPageLimit.start(10000,3000000),9990);eq(ListPageLimit.end(10000,9990),10000);
  ok(ListPageLimit.canNext(10000,30));ok(!ListPageLimit.canNext(10000,9990));
  ok(ListPageLimit.canPrevious(10000,9990));ok(!ListPageLimit.canPrevious(10,0));
  for(int total: new int[]{1,29,30,31,99,300,10000}){
   int offset=0,count=0;
   do {int start=ListPageLimit.start(total,offset),end=ListPageLimit.end(total,offset);
    eq(start,count);count+=end-start;
    if(!ListPageLimit.canNext(total,offset))break;
    int next=ListPageLimit.next(total,offset);ok(next>offset);offset=next;
   }while(true);
   eq(count,total);
  }
  System.out.println("PASS DI paging "+pass+" assertions, all 10000 records reachable");
 }
}
