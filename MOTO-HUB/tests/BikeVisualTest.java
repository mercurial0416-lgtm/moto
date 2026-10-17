package com.motohub.next;
public class BikeVisualTest {
 public static void main(String[] args){
  String[] cub={"슈퍼커브 110","Honda Super Cub 110","SuperCub110","슈퍼컵110"};
  for(String model:cub)if(!BikeVisual.superCub(model))throw new AssertionError(model);
  for(String model:new String[]{"390 DUKE","KTM 390 Duke","Ninja 400","R3", "PCX", "", "커브가 아닌 모델", "Honda Super Cub C125", "슈퍼커브 125", "슈퍼커브 50", "슈퍼컵", "Super Cub"})if(BikeVisual.superCub(model))throw new AssertionError(model);
  if(!BikeVisual.caption("Super Cub 110",false).contains("슈퍼커브"))throw new AssertionError("caption");
  if(!BikeVisual.caption("DUKE",false).contains("미등록"))throw new AssertionError("fallback");
  if(!BikeVisual.caption("DUKE",true).contains("내 바이크"))throw new AssertionError("photo");
  System.out.println("PASS: BikeVisual model-specific placeholder checks");
 }
}