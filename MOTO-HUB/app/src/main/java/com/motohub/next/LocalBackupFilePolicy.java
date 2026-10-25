package com.motohub.next;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/** Private, bounded, non-symlink JSON restore points; never follow outside-app aliases. */
final class LocalBackupFilePolicy {
 static final long MAX_BYTES=8_000_000L;
 private LocalBackupFilePolicy(){}
 static boolean safeFolder(File folder){
  return folder!=null && Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)
   && !Files.isSymbolicLink(folder.toPath());
 }
 static boolean readable(File file){
  return file!=null && Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS)
   && !Files.isSymbolicLink(file.toPath())
   && file.length()>0 && file.length()<=MAX_BYTES;
 }
 /** Only truly usable local files may count toward the retention quota.
  *  Never let a linked, empty or oversized fake checkpoint evict real backups.
  */
 static File[] eligible(File folder,String namePattern){
  if(!safeFolder(folder)||namePattern==null)return new File[0];
  File[] files=folder.listFiles((parent,name)->name.matches(namePattern)
    &&readable(new File(parent,name)));
  return files==null?new File[0]:files;
 }
 /** Count only checkpoints whose JSON content passes the caller's full schema check.
  *  Corrupt files are left on disk for diagnosis, not used to evict good backups.
  */
 static File[] eligible(File folder,String namePattern,java.util.function.Predicate<File> validContent){
  if(validContent==null)return new File[0];
  File[] candidates=eligible(folder,namePattern);
  java.util.ArrayList<File> good=new java.util.ArrayList<>();
  for(File file:candidates){
   try{if(validContent.test(file))good.add(file);}
   catch(RuntimeException ignored){/* One damaged backup must not hide other restore points. */}
  }
  return good.toArray(new File[0]);
 }
 /** Bound the actual stream, not just the preflight length. A file that grows
  * between stat() and open() can never allocate an unbounded byte array.
  */
 static byte[] readBounded(File file)throws IOException{
  requireReadable(file);
  try(InputStream input=Files.newInputStream(file.toPath());
      ByteArrayOutputStream output=new ByteArrayOutputStream((int)Math.min(file.length(),64*1024))){
   byte[] part=new byte[16*1024];
   int n;long total=0;
   while((n=input.read(part))!=-1){
    total+=n;
    if(total>MAX_BYTES)throw new IOException("로컬 백업 크기가 허용 범위를 넘었습니다.");
    output.write(part,0,n);
   }
   if(total==0)throw new IOException("비어 있는 로컬 백업입니다.");
   return output.toByteArray();
  }
 }
 static void requireReadable(File file)throws IOException{
  if(!readable(file))throw new IOException("안전하지 않거나 손상된 로컬 복원 파일입니다.");
 }
}
