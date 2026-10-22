package com.motohub.next;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Pure Java simulations of process death at several points in a ZIP restore. */
public final class RestoreCrashJournalTest {
 static int checks=0;
 static void ok(boolean yes,String label){checks++;if(!yes)throw new AssertionError(label);}
 static final String P="garage-photo-12345678-1234-1234-1234-123456789abc.jpg";
 static final String N="garage-photo-87654321-4321-4321-4321-cba987654321.jpg";
 static final String T="1792122693079-12345678-1234-1234-1234-123456789abc.track";
 static File journal(File root)throws Exception{
  File dir=new File(root,"moto-rollback-"+UUID.randomUUID());if(!dir.mkdir())throw new IOException("mkdir");
  return dir;
 }
 static void clean(File f)throws Exception{
  if(Files.isDirectory(f.toPath(),LinkOption.NOFOLLOW_LINKS)){
   for(File child:Objects.requireNonNull(f.listFiles()))clean(child);
  }
  Files.deleteIfExists(f.toPath());
 }
 static void copy(File src,File dst)throws Exception{
  if(!dst.getParentFile().isDirectory()&&!dst.getParentFile().mkdirs())throw new IOException("mkdir");
  Files.copy(src.toPath(),dst.toPath());
 }
 static void expect(File file,String expected)throws Exception{
  ok(Files.readString(file.toPath()).equals(expected),"file content "+file);
 }
 public static void main(String[] args)throws Exception{
  File root=Files.createTempDirectory("moto-crash-replay").toFile();
  try{
   File rides=new File(root,"rides");ok(rides.mkdir(),"rides");
   File original=new File(root,P),newlyAdded=new File(root,N),ride=new File(rides,T);
   Files.writeString(original.toPath(),"old photo");Files.writeString(ride.toPath(),"old GPS");
   File db=new File(root,"motohub-v3.json");Files.writeString(db.toPath(),"{\"old\":true}");
   List<String> old=Arrays.asList(P,"rides/"+T),fresh=Arrays.asList(P,N,"rides/"+T);
   File rollback=journal(root);copy(original,new File(rollback,P));copy(ride,new File(rollback,"rides/"+T));
   RestoreArchive.CrashJournal.begin(root,rollback,old,fresh,Files.readAllBytes(db.toPath()));
   // Simulate a process kill AFTER modifying existing files and adding a new one.
   Files.writeString(original.toPath(),"restored photo");Files.writeString(ride.toPath(),"restored GPS");
   Files.writeString(newlyAdded.toPath(),"new photo");Files.writeString(db.toPath(),"{\"new\":true}");
   int restored=RestoreArchive.CrashJournal.recoverAll(root,bytes->Files.write(db.toPath(),bytes));
   ok(restored==1,"pending restore replayed");
   expect(original,"old photo");expect(ride,"old GPS");expect(db,"{\"old\":true}");
   ok(!newlyAdded.exists(),"new-only asset removed");
   ok(!rollback.exists(),"journal removed on successful recovery");
   ok(RestoreArchive.CrashJournal.recoverAll(root,bytes->Files.write(db.toPath(),bytes))==0,"restart idempotent");
   // A killed process BEFORE the live mutation still replays safely.
   File early=journal(root);copy(original,new File(early,P));copy(ride,new File(early,"rides/"+T));
   RestoreArchive.CrashJournal.begin(root,early,old,fresh,Files.readAllBytes(db.toPath()));
   ok(RestoreArchive.CrashJournal.recoverAll(root,bytes->Files.write(db.toPath(),bytes))==1,"early crash replay safe");
   expect(original,"old photo");
   // A restore committed before cleanup must KEEP its new files and JSON.
   File committed=journal(root);copy(original,new File(committed,P));copy(ride,new File(committed,"rides/"+T));
   RestoreArchive.CrashJournal.begin(root,committed,old,fresh,Files.readAllBytes(db.toPath()));
   Files.writeString(original.toPath(),"new committed photo");
   Files.writeString(db.toPath(),"{\"committed\":true}");
   RestoreArchive.CrashJournal.markCommitted(committed);
   ok(RestoreArchive.CrashJournal.recoverAll(root,bytes->{throw new AssertionError("must not rollback committed");})==0,"commit marker only cleans up");
   expect(original,"new committed photo");expect(db,"{\"committed\":true}");
   ok(!committed.exists(),"committed journal cleaned");
   // If writing the original JSON fails, preserve journal and block normal startup.
   File brokenWriter=journal(root);copy(original,new File(brokenWriter,P));copy(ride,new File(brokenWriter,"rides/"+T));
   RestoreArchive.CrashJournal.begin(root,brokenWriter,old,fresh,Files.readAllBytes(db.toPath()));
   Files.writeString(original.toPath(),"partial photo");
   try{
    RestoreArchive.CrashJournal.recoverAll(root,bytes->{throw new IOException("disk write refused");});
    throw new AssertionError("JSON failure not reported");
   }catch(IOException expected){checks++;}
   ok(new File(brokenWriter,"pending").exists(),"leave retryable journal after failed JSON recovery");
   ok(RestoreArchive.CrashJournal.recoverAll(root,bytes->Files.write(db.toPath(),bytes))==1,"retry recovery");
   expect(original,"new committed photo");
   // Tampered old copy (symlink to outside) must fail closed and never touch outside bytes.
   File unsafe=journal(root);copy(original,new File(unsafe,P));copy(ride,new File(unsafe,"rides/"+T));
   RestoreArchive.CrashJournal.begin(root,unsafe,old,fresh,Files.readAllBytes(db.toPath()));
   Path external=Files.createTempFile("moto-crash-outside","txt");
   Files.writeString(external,"outside untouched");
   Files.delete(new File(unsafe,P).toPath());
   Files.createSymbolicLink(new File(unsafe,P).toPath(),external);
   try{
    RestoreArchive.CrashJournal.recoverAll(root,bytes->Files.write(db.toPath(),bytes));
    throw new AssertionError("tampered safe copy accepted");
   }catch(IOException expected){checks++;}
   ok(new File(unsafe,"pending").exists(),"unsafe journal preserved for investigation");
   ok(Files.readString(external).equals("outside untouched"),"external target never modified");
   Files.delete(external);
   System.out.println("PASS crash restore journaling: "+checks+" checks");
  }finally{clean(root);}
 }
}
