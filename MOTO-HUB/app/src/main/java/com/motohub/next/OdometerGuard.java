package com.motohub.next;
/** Never infer an odometer from GPS. Explicit numeric input and jump checks only. */
final class OdometerGuard {
 private OdometerGuard(){}
 static double require(double prior,double entered){
  if(!Double.isFinite(prior)||prior<0||!Double.isFinite(entered)||entered<prior||entered>1e9)
   throw new IllegalArgumentException("현재 계기판과 같거나 큰 올바른 주행거리를 입력해 주세요.");
  return entered;
 }
 static boolean largeJump(double prior,double entered){
  return Double.isFinite(prior)&&Double.isFinite(entered)&&entered-prior>=2000;
 }
}