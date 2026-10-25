package com.motohub.next;
public final class RideDistanceReviewPolicyTest {
 static int count;
 static void yes(boolean v){count++;if(!v)throw new AssertionError("test "+count);}
 public static void main(String[] args){
  yes(!RideDistanceReviewPolicy.plausible(50,false));
  yes(!RideDistanceReviewPolicy.plausible(0,true));
  yes(!RideDistanceReviewPolicy.plausible(Double.NaN,true));
  yes(!RideDistanceReviewPolicy.plausible(Double.POSITIVE_INFINITY,true));
  yes(!RideDistanceReviewPolicy.plausible(1501,true));
  yes(RideDistanceReviewPolicy.plausible(42.6,true));
  yes(RideDistanceReviewPolicy.suggestion(42.6,true).contains("42.6 km"));
  yes(RideDistanceReviewPolicy.suggestion(42.6,true).contains("자동 변경하지 않습니다"));
  yes(RideDistanceReviewPolicy.suggestion(42.6,false).contains("검증 전"));
  System.out.println("PASS RideDistanceReviewPolicyTest "+count);
 }
}
