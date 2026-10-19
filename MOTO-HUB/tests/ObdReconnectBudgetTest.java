package com.motohub.next;
public final class ObdReconnectBudgetTest {
 static int checks=0;
 static void ok(boolean x,String msg){checks++;if(!x)throw new AssertionError(msg);}
 public static void main(String[] args){
  long now=70_000_000_000L;
  int f=ObdConnectionPolicy.nextFailureCount(7,1_000_000_000L,now);
  ok(f==1,"one healthy session resets failures exactly once");
  for(int i=2;i<=ObdConnectionPolicy.MAX_RETRIES+1;i++){
   f=ObdConnectionPolicy.nextFailureCount(f,0,now+i*1000);
   ok(f==i,"failed reconnect cannot reset budget: "+i);
  }
  ok(!ObdConnectionPolicy.shouldRetry(f,true,true),"retry exhaustion must stop");
  ok(ObdConnectionPolicy.nextFailureCount(3,0,now)==4,"no connection cannot reset");
  ok(ObdConnectionPolicy.nextFailureCount(3,now+1,now)==4,"future timestamp ignored");
  ok(ObdConnectionPolicy.nextFailureCount(3,now-30_000_000_000L,now)==4,"short connection does not reset");
  ok(ObdConnectionPolicy.nextFailureCount(3,now-61_000_000_000L,now)==1,"long valid connection resets once");
  System.out.println("PASS OBD reconnect budget: "+checks+" checks");
 }
}
