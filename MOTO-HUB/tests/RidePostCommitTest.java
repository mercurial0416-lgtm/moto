package com.motohub.next;
public final class RidePostCommitTest {
 static int checks;
 static void check(boolean yes,String title){checks++;if(!yes)throw new AssertionError(title);}
 public static void main(String[] args){
  int[] calls={0,0,0};
  RidePostCommit.afterWrite(()->calls[0]++,()->calls[1]++,()->calls[2]++);
  check(calls[0]==1&&calls[1]==0&&calls[2]==1,"healthy sharing and notification");
  RidePostCommit.afterWrite(()->{throw new SecurityException("network sharing unavailable");},
   ()->calls[1]++,()->calls[2]++);
  check(calls[1]==1&&calls[2]==2,"failed sharing stops optional channel without blocking notification");
  RidePostCommit.afterWrite(()->calls[0]++,()->calls[1]++,()->{throw new SecurityException("OS notifications revoked");});
  check(calls[0]==2&&calls[1]==1,"failed notification doesn't abort recording callback");
  RidePostCommit.afterWrite(()->{throw new IllegalStateException("sharing failed");},
   ()->{throw new IllegalStateException("share cleanup also failed");},
   ()->calls[2]++);
  check(calls[2]==3,"notification still runs after double optional error");
  RidePostCommit.afterWrite(null,null,null);
  check(true,"null optional side effects are safe");
  System.out.println("PASS GPS post-write optional failures: "+checks+" assertions");
 }
}
