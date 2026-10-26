package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class RideMileageIntegrityTest {
 private static int count;
 private static void check(boolean works,String label){
  count++;if(!works)throw new AssertionError(label);
 }
 private static boolean fails(File track,boolean applied,File parent,long bytes,long modified){
  try{RideMileageIntegrity.requireApplyable(track,applied,parent,bytes,modified);return false;}
  catch(IOException expected){return true;}
 }
 private static boolean failsDigest(File track,File parent,long bytes,long modified,String sha){
  try{RideMileageIntegrity.requireApplyable(track,false,parent,bytes,modified,sha);return false;}
  catch(IOException expected){return true;}
 }
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-mileage-").toFile();
  File dir=new File(root,"rides");
  check(dir.mkdirs(),"rides directory");
  String id=UUID.randomUUID().toString();
  String bike=UUID.randomUUID().toString();
  File track=new File(dir,"1760000000000-"+id+".track");
  Files.write(track.toPath(),"37.123,127.123,5,1000\n37.124,127.124,5,2000\n".getBytes(StandardCharsets.UTF_8));
  check(!RideMileageIntegrity.validCompleted(track),"unverified track excluded");
  new File(track.getPath()+".done").createNewFile();
  RideBikeBinding.write(track,bike);
  long length=track.length(),lastModified=track.lastModified();
  check(RideMileageIntegrity.validCompleted(track),"done marker eligible");
  Set<String> ledger=new HashSet<>();
  check(RideMileageIntegrity.eligibleForReview(track,ledger,bike),"own bike pending ride");
  check(!RideMileageIntegrity.eligibleForReview(track,ledger,UUID.randomUUID().toString()),"another bike excluded");
  ledger.add(track.getName());
  check(!RideMileageIntegrity.eligibleForReview(track,ledger,bike),"already applied excluded");
  check(fails(track,true,dir,length,lastModified),"already applied commit denied");
  ledger.clear();
  RideMileageIntegrity.requireApplyable(track,false,dir,length,lastModified);
  String pinned=RideMileageIntegrity.fingerprint(track);
  check(pinned.matches("[a-f0-9]{64}"),"source SHA-256 fingerprint");
  RideMileageIntegrity.requireApplyable(track,false,dir,length,lastModified,pinned);
  check(failsDigest(track,dir,length,lastModified,""),"missing fingerprint rejected");
  byte[] original=Files.readAllBytes(track.toPath());
  String altered=new String(original,StandardCharsets.UTF_8).replace("37.123","37.923");
  check(altered.length()==original.length,"equal-length fixture");
  Files.write(track.toPath(),altered.getBytes(StandardCharsets.UTF_8));
  check(track.setLastModified(lastModified),"restore original timestamp");
  check(track.length()==length&&track.lastModified()==lastModified,"metadata alone cannot spot content changes");
  check(failsDigest(track,dir,length,lastModified,pinned),"same size and timestamp but different SHA rejected");
  Files.write(track.toPath(),original);
  check(track.setLastModified(lastModified),"restore original track timestamp");
  RideMileageIntegrity.requireApplyable(track,false,dir,length,lastModified,pinned);
  check(fails(track,false,new File(root,"wrong"),length,lastModified),"different directory blocked");
  check(fails(track,false,dir,length+1,lastModified),"modified length blocked");
  check(fails(track,false,dir,length,lastModified+1),"modified time blocked");
  File mergedInto=new File(track.getPath()+".mergedinto");
  Files.write(mergedInto.toPath(),("1760000000000-"+UUID.randomUUID()+".track").getBytes(StandardCharsets.UTF_8));
  check(!RideMileageIntegrity.validCompleted(track),"merged source excluded");
  check(fails(track,false,dir,length,lastModified),"merged source cannot commit");
  mergedInto.delete();
  File unmerged=new File(track.getPath()+".unmerged");
  unmerged.createNewFile();
  check(!RideMileageIntegrity.validCompleted(track),"archived combined excluded");
  unmerged.delete();
  new File(track.getPath()+".done").delete();
  check(!RideMileageIntegrity.validCompleted(track),"missing done excluded");
  File recovered=new File(track.getPath()+".recovered");
  recovered.createNewFile();
  check(RideMileageIntegrity.validCompleted(track),"recovered local track permitted");
  recovered.delete();
  check(!RideMileageIntegrity.validCompleted(track),"no valid completion marker");
  check(!RideMileageIntegrity.eligibleForReview(null,ledger,bike),"null excluded");
  check(!RideMileageIntegrity.eligibleForReview(track,null,bike),"no ledger snapshot");
  System.out.println("PASS RideMileageIntegrityTest "+count+" checks");
 }
}
