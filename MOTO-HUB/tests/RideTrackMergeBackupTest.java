package com.motohub.next;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.Files;import java.util.UUID;
final class RideTrackMergeBackupTest {
 static int assertions;
 static void ok(boolean v){if(!v)throw new AssertionError("merge/backup consistency");assertions++;}
 static File track(File internal,long time)throws Exception {
  File f=new File(TrackFile.directory(internal),time+"-"+UUID.randomUUID()+".track");
  try(FileOutputStream out=new FileOutputStream(f)){
   out.write(TrackFile.point(37,127,2,time).getBytes(StandardCharsets.UTF_8));
   out.write(TrackFile.point(37.001,127.001,2,time+10_000).getBytes(StandardCharsets.UTF_8));
  }
  TrackCompletion.mark(f,2);return f;
 }
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-merge-backup").toFile();
  File restored=Files.createTempDirectory("moto-merge-restore").toFile();
  try{
   File a=track(root,1_700_000_000_000L),b=track(root,1_700_000_030_000L);
   RideTrackMerge.Result merged=RideTrackMerge.merge(root,a,b);
   ByteArrayOutputStream zip=new ByteArrayOutputStream();
   BackupBundle.export("{\"schema\":3}",root,zip);
   try(RestoreArchive.Prepared prepared=RestoreArchive.prepare(new ByteArrayInputStream(zip.toByteArray()),restored)){
    ok(prepared.rides==3);
    prepared.install(restored,json->{ok(json.contains("schema"));});
   }
   File restoredA=new File(restored,"rides/"+a.getName());
   File restoredB=new File(restored,"rides/"+b.getName());
   File restoredMerged=new File(restored,"rides/"+merged.combined.getName());
   ok(restoredMerged.isFile());
   ok(RideTrackMerge.mergedSource(restoredA)&&RideTrackMerge.mergedSource(restoredB));
   ok(TrackFile.read(restoredA,(x,y,z,t)->{})==2);
   ok(TrackFile.read(restoredMerged,(x,y,z,t)->{})==4);
   ok(new File(restoredMerged+".done").isFile());
   System.out.println("Merged ride backup/restore tests passed: "+assertions);
  }finally{clean(root);clean(restored);}
 }
 static void clean(File f){File[] items=f.listFiles();if(items!=null)for(File child:items)clean(child);f.delete();}
}
