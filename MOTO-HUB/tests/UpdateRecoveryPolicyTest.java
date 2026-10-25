package com.motohub.next;
public final class UpdateRecoveryPolicyTest {
 static int checks=0;
 static void test(boolean actual,String name){checks++;if(!actual)throw new AssertionError(name);}
 public static void main(String[] args){
  String old="motohub-1792900000000-1234abcd.json";
  String current="motohub-1792900000001-1234abcd.json";
  test(UpdateRecoveryPolicy.validSnapshotName(old),"valid snapshot");
  test(!UpdateRecoveryPolicy.validSnapshotName("../motohub-1792900000000-1234abcd.json"),"traversal");
  test(!UpdateRecoveryPolicy.validSnapshotName("motohub-snapshot.json"),"wrong format");
  test(UpdateRecoveryPolicy.isPinned(old,old,""),"pending pinned");
  test(UpdateRecoveryPolicy.isPinned(old,"",old),"last successful pinned");
  test(UpdateRecoveryPolicy.isPinned(old,current,old),"last good pinned even when new install pending");
  test(!UpdateRecoveryPolicy.isPinned(old,current,""),"ordinary older backup is not pinned");
  test(!UpdateRecoveryPolicy.isPinned(old,"",""),"no pin");
  test(!UpdateRecoveryPolicy.isPinned(null,old,old),"null rejected");
  System.out.println("PASS UpdateRecoveryPolicyTest "+checks+" checks");
 }
}
