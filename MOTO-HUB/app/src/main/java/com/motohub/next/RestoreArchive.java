package com.motohub.next;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Restore MOTO HUB's exported ZIP without following archive paths or trusting external file sizes. */
public final class RestoreArchive {
 private RestoreArchive() {}
 private static final long MAX_TOTAL=BackupFormat.MAX_TOTAL_BYTES, MAX_ENTRY=BackupFormat.MAX_TRACK_BYTES;
 private static final int MAX_ENTRIES=BackupFormat.MAX_ENTRIES;
 private static final String PHOTO=BackupFormat.PHOTO, RECEIPT=BackupFormat.RECEIPT;
 private static final String RIDE=BackupFormat.TRACK;
 public interface DataWriter {void write(String json)throws Exception;}
 public static final class Prepared implements AutoCloseable {
  private final File staging; public final String json,preferences,batchRecord; public final int photos,receipts,rides; public final String part;
  private Prepared(File dir,String data,int p,int receipts,int r,String part,String preferences,String batchRecord){staging=dir;json=data;photos=p;this.receipts=receipts;rides=r;this.part=part;this.preferences=preferences;this.batchRecord=batchRecord;}
  public boolean isSplit(){return !"FULL".equals(part);}
  /** Snapshot disk-space requirement before replacing any of the current assets. */
  public long requiredSpace(File internal)throws IOException {
   List<String> fresh=stagedAssets(staging);
   return RestoreCapacity.required(
    RestoreCapacity.sum(internal,oldAssets(internal,fresh)),
    RestoreCapacity.sum(staging,fresh),
    json.getBytes(StandardCharsets.UTF_8).length);
  }
  /** Compatibility API for existing direct callers; Android ZIP restore uses the journal overload. */
  public void install(File internal,DataWriter writer)throws Exception {install(internal,null,writer);}
  /** Protected restore: retain the original JSON alongside the old assets for crash replay. */
  public void install(File internal,byte[] previousJson,DataWriter writer)throws Exception {
   if(isSplit())throw new IOException("분할 ZIP은 전체 복원할 수 없습니다. 분할 ZIP 추가 복원 메뉴를 사용하세요.");
   // Existing assets are copied into a rollback directory while staged assets
   // are copied into place. Reserve space for BOTH copies before mutation.
   RestoreCapacity.ensure(RestoreCapacity.available(internal),requiredSpace(internal));
   File backup=new File(internal,"moto-rollback-"+UUID.randomUUID());
   if(!backup.mkdirs())throw new IOException("복원 안전 사본 폴더 생성 실패");
   List<String> fresh=stagedAssets(staging);
   List<String> old=oldAssets(internal,fresh);
   Set<String> existing=new HashSet<>(old);
   boolean preserveRollback=false;
   try {
    for(String name:old)copy(new File(internal,name),new File(backup,name));
    // Persist a replayable rollback BEFORE touching any live GPS/photo file.
    if(previousJson!=null)CrashJournal.begin(internal,backup,old,fresh,previousJson);
    for(String name:fresh){
     File staged=new File(staging,name),installed=new File(internal,name);
     copy(staged,installed);
     if(!sameContent(staged,installed))throw new IOException("복원된 파일 검증 실패: "+name);
    }
    // Remove obsolete recognized assets BEFORE committing JSON. A failed delete
    // must abort the restore, not leave a mixed old/new garage after success.
    // Rollback copies above already protect every original asset.
    Set<String> restoredNames=new HashSet<>(fresh);
    for(String name:old)if(!restoredNames.contains(name))
     Files.deleteIfExists(new File(internal,name).toPath());
    // The caller's atomic JSON write is the last committing operation.
    writer.write(json);
    if(previousJson!=null)CrashJournal.markCommitted(backup);
   }catch(Exception failure){
    if(previousJson!=null&&new File(backup,"pending").isFile()){
     try{
      // Restore JSON as well as every asset, even if writer failed AFTER saving new JSON.
      CrashJournal.recoverOne(internal,backup,bytes->writer.write(new String(bytes,StandardCharsets.UTF_8)));
     }catch(Exception recovered){preserveRollback=true;failure.addSuppressed(recovered);}
    }else{
     for(String name:fresh)if(!existing.contains(name))new File(internal,name).delete();
     for(String name:old)try{
      File saved=new File(backup,name);
      if(saved.isFile())copy(saved,new File(internal,name));
      else {preserveRollback=true;failure.addSuppressed(new IOException("복구용 안전 사본 누락: "+name));}
     }catch(Exception e){preserveRollback=true;failure.addSuppressed(e);}
    }
    throw failure;
   }finally{if(!preserveRollback)clean(backup);}
  }
  /** Additive partition restore. Never overwrites current assets or JSON, even on partial failure. */
  public int installPart(File internal)throws Exception{
   if(!isSplit())throw new IOException("전체 ZIP은 분할 추가 복원으로 가져올 수 없습니다.");
   List<String> assets=stagedAssets(staging);
   for(String path:assets){
    if("GPS".equals(part)&&!path.startsWith("rides/")&&!path.startsWith("rides-trash/"))throw new IOException("GPS 분할 파일에 다른 저장 자료가 있습니다.");
    if("MEDIA".equals(part)&&(path.startsWith("rides/")||path.startsWith("rides-trash/")))throw new IOException("사진 분할 파일에 GPS 기록이 포함됐습니다.");
   }
   RestoreCapacity.ensure(RestoreCapacity.available(internal),RestoreCapacity.sum(staging,assets));
   List<File> created=new ArrayList<>();
   try{
    for(String name:assets){
     File from=new File(staging,name),to=new File(internal,name);
     if(Files.exists(to.toPath(),LinkOption.NOFOLLOW_LINKS)){
      if(!Files.isRegularFile(to.toPath(),LinkOption.NOFOLLOW_LINKS)||!sameContent(from,to))
       throw new IOException("기존 파일과 이름이 같지만 내용이 다릅니다. 원본을 덮어쓰지 않습니다: "+name);
      continue;
     }
     if(!to.getParentFile().isDirectory()&&!to.getParentFile().mkdirs())
      throw new IOException("복원 위치 준비 실패");
     copy(from,to);
     created.add(to);
     if(!sameContent(from,to))throw new IOException("분할 ZIP 자료 검증 실패: "+name);
    }
    return created.size();
   }catch(Exception failure){
    for(int i=created.size()-1;i>=0;i--)try{Files.deleteIfExists(created.get(i).toPath());}
    catch(IOException cleanup){failure.addSuppressed(cleanup);}
    throw failure;
   }
  }
  @Override public void close(){clean(staging);}
 }
 public static Prepared prepare(InputStream source,File internal)throws Exception{
  File stage=new File(internal,"moto-stage-"+UUID.randomUUID());if(!stage.mkdirs())throw new IOException("임시 폴더를 생성하지 못했어요.");
  String json=null,part="FULL",preferences="",batchRecord="";int photos=0,receipts=0,rides=0,entries=0;long total=0;
  Set<String> incomplete=new HashSet<>();
  Set<String> seen=new HashSet<>();byte[] buffer=new byte[32768];
  try(ZipInputStream zip=new ZipInputStream(new BufferedInputStream(source),StandardCharsets.UTF_8)){
   ZipEntry entry;
   while((entry=zip.getNextEntry())!=null){
    String name=entry.getName();if(++entries>MAX_ENTRIES)throw new IOException("백업 항목이 너무 많아요.");
    if(!seen.add(name))throw new IOException("중복 파일: "+name);
    if(entry.isDirectory()){
     if(!BackupFormat.allowed(name,true))throw new IOException("지원하지 않는 폴더입니다.");
     zip.closeEntry();continue;
    }
    if(name.contains("..")||name.contains("\\")||name.startsWith("/")||name.indexOf('\0')>=0)throw new IOException("안전하지 않은 ZIP 경로입니다.");
    boolean data=name.equals("MOTO-HUB-data.json"),
       modelPhoto=BackupFormat.isModelPhoto(name),
       modelMeta=BackupFormat.isModelMetadata(name),
       photo=name.startsWith("photos/")&&name.substring(7).matches(PHOTO),
       receipt=name.startsWith("receipts/")&&name.substring(9).matches(RECEIPT),
       track=(name.startsWith("rides/")&&name.substring(6).matches(RIDE))||BackupFormat.isTrashTrack(name),
       marker=BackupFormat.isTrashSidecar(name)||(name.startsWith("rides/")&&(name.substring(6).matches(RIDE+"\\.done")||name.substring(6).matches(RIDE+"\\.imported")||name.substring(6).matches(RIDE+"\\.recovered")||name.substring(6).matches(RIDE+"\\.mergedinto")||name.substring(6).matches(RIDE+"\\.unmerged")||name.substring(6).matches(RIDE+"\\.category")||name.substring(6).matches(RIDE+"\\.bike")||name.substring(6).matches(RIDE+"\\.favorite"))),
       gpx=name.startsWith("gpx/")&&name.substring(4).matches("[0-9]{10,15}-[a-f0-9-]{36}\\.gpx"),
       readme=name.equals("README-KO.txt"),ui=name.equals("MOTO-HUB-preferences.json"),batch=name.equals("MOTO-HUB-batch.txt");
    if(!data&&!ui&&!batch&&!photo&&!receipt&&!track&&!marker&&!gpx&&!readme&&!modelPhoto&&!modelMeta)throw new IOException("지원하지 않는 ZIP 항목: "+name);
    long limit=BackupFormat.entryLimit(name);long size=0;
    ByteArrayOutputStream jsonBytes=(data||readme||ui||batch)?new ByteArrayOutputStream():null;
    File destination=(photo||receipt||track||marker||modelPhoto||modelMeta)?
     new File(stage,(modelPhoto||modelMeta)?"moto-year-photos/"+name.substring(13):
       (photo?name.substring(7):(receipt?name.substring(9):name))):null;
    if(destination!=null&&!destination.getParentFile().isDirectory()&&!destination.getParentFile().mkdirs())throw new IOException("임시 파일 저장 실패");
    try(OutputStream out=destination==null?(jsonBytes==null?new OutputStream(){public void write(int b){}public void write(byte[] b,int off,int len){}}:jsonBytes):new BufferedOutputStream(new FileOutputStream(destination))){
     int n;while((n=zip.read(buffer))!=-1){size+=n;total+=n;if(size>limit||total>MAX_TOTAL)throw new IOException("백업 ZIP 용량 제한을 초과했어요.");out.write(buffer,0,n);}
    }
    if(data){json=new String(jsonBytes.toByteArray(),StandardCharsets.UTF_8);if(json.isEmpty())throw new IOException("JSON 기록이 비어 있어요.");}
    if(ui)preferences=jsonBytes.toString("UTF-8");
    if(batch){
     batchRecord=jsonBytes.toString("UTF-8");
     if(!batchRecord.matches("MOTO-HUB-BATCH:v1:[a-f0-9-]{36}:(GPS|MEDIA):[0-9]{1,5}/[0-9]{1,5}\\n"))
      throw new IOException("분할 ZIP 배치 정보 오류");
    }
    if(readme){
     String label=jsonBytes.toString("UTF-8");
     if(label.startsWith("MOTO-HUB-PART:GPS\n"))part="GPS";
     else if(label.startsWith("MOTO-HUB-PART:MEDIA\n"))part="MEDIA";
     else if(label.startsWith("MOTO-HUB-PART:"))throw new IOException("지원하지 않는 분할 백업 형식입니다.");
    }
    if(photo||receipt||modelPhoto){
     if(size<4||!jpeg(destination))throw new IOException("잘못된 JPG 사진입니다.");
     if((photo||modelPhoto)&&++photos>BackupFormat.MAX_PHOTOS*2)throw new IOException("바이크 사진이 너무 많습니다.");
     if(receipt&&++receipts>BackupFormat.MAX_RECEIPTS)throw new IOException("영수증 사진이 너무 많습니다.");
    }
    if(track){
     int points=TrackFile.read(destination,(a,b,c,d)->{});
     if(points<1){
      // An interrupted append can contain only one unfinished row. Preserve
      // those exact bytes for manual diagnostics, not fabricated GPX points.
      byte[] raw=Files.readAllBytes(destination.toPath());
      String partial=new String(raw,StandardCharsets.UTF_8);
      if(raw.length==0||raw.length>256||partial.indexOf('\n')>=0||!partial.matches("[0-9eE+.,-]+"))
       throw new IOException("복원할 수 없는 GPS 기록입니다.");
      incomplete.add(name);
     }
     if(++rides>BackupFormat.MAX_TRACKS)throw new IOException("주행 기록이 10,000건을 초과합니다.");
    }
    zip.closeEntry();
   }
   if(json==null)throw new IOException("MOTO-HUB-data.json이 들어 있지 않아요.");
   for(String name:seen){
    if(modelPhotoName(name)&&!seen.contains(name+".json"))
     throw new IOException("모델 사진의 출처 정보가 누락됐습니다.");
    if(BackupFormat.isModelMetadata(name)){
     if(!seen.contains(name.substring(0,name.length()-5))||
        !BackupFormat.selectedModelMetadata(new String(Files.readAllBytes(new File(stage,"moto-year-photos/"+name.substring(13)).toPath()),StandardCharsets.UTF_8)))
      throw new IOException("모델 사진의 선택 정보가 손상됐습니다.");
    }
   }
   for(String name:incomplete)if(seen.contains(name+".done")||seen.contains(name+".imported")||seen.contains(name+".recovered")||seen.contains(name+".mergedinto"))
    throw new IOException("완료 처리된 GPS 기록은 좌표가 있어야 합니다.");
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".mergedinto")){
    File link=new File(stage,name);
    String target=new String(Files.readAllBytes(link.toPath()),StandardCharsets.US_ASCII);
    if(!target.matches(RIDE)||!seen.contains("rides/"+target)||!seen.contains("rides/"+target+".done"))
     throw new IOException("통합 라이딩 대상이 누락되거나 손상됐어요.");
   }
   for(String name:seen)if((name.startsWith("rides/")||name.startsWith("rides-trash/"))&&(name.endsWith(".done")||name.endsWith(".imported")||name.endsWith(".recovered")||name.endsWith(".mergedinto")||name.endsWith(".unmerged")||name.endsWith(".category")||name.endsWith(".bike")||name.endsWith(".favorite")) &&!seen.contains(name.substring(0,name.lastIndexOf('.'))))throw new IOException("원본 GPS가 없는 기록 표시입니다.");
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".bike")){
    String id=new String(Files.readAllBytes(new File(stage,name).toPath()),StandardCharsets.US_ASCII);
    if(!id.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))throw new IOException("잘못된 바이크 연결 정보입니다.");
   }
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".category")){
    File label=new File(stage,name);
    if(!java.util.Arrays.asList("자동 분류","일상","출퇴근","와인딩","장거리","투어").contains(new String(Files.readAllBytes(label.toPath()),StandardCharsets.UTF_8)))throw new IOException("지원하지 않는 라이딩 분류입니다.");
   }
   for(String name:seen)if(name.startsWith("rides/")&&name.endsWith(".unmerged")&&!seen.contains(name.substring(0,name.length()-9)+".done"))
    throw new IOException("완료되지 않은 통합 기록입니다.");
   if("GPS".equals(part)&&(photos>0||receipts>0))throw new IOException("GPS 분할 백업의 사진 항목이 유효하지 않습니다.");
   if("MEDIA".equals(part)&&rides>0)throw new IOException("사진 분할 백업의 GPS 항목이 유효하지 않습니다.");
   return new Prepared(stage,json,photos,receipts,rides,part,preferences,batchRecord);
  }catch(Exception e){clean(stage);throw e;}
 }

 /** Bounded, internal-only journal. A pending marker is written after all old files are safe.
  * A crash before commit replays original JSON + assets; after commit only cleanup remains.
  * Never read external ZIP filenames here or follow symbolic links. */
 static final class CrashJournal {
  private static final String OLD_JSON="old-db.json", MANIFEST="journal.bin";
  private static final String PENDING="pending", COMMITTED="committed";
  private static final String DIRECTORY="moto-rollback-[0-9a-fA-F-]{36}";
  private static final int MAX_NAMES=BackupFormat.MAX_ENTRIES;
  interface OriginalWriter {void write(byte[] oldJson)throws Exception;}
  static void begin(File internal,File backup,List<String> old,List<String> fresh,byte[] oldJson)throws IOException {
   safeDir(internal,backup);
   if(oldJson==null||oldJson.length<2||oldJson.length>BackupFormat.MAX_JSON_BYTES)
    throw new IOException("복원 이전 JSON 크기가 올바르지 않습니다.");
   for(String name:old)validate(name);
   for(String name:fresh)validate(name);
   if(old.size()+fresh.size()>MAX_NAMES)throw new IOException("복원 목록이 너무 큽니다.");
   // No live file is modified until this manifest, JSON and all asset copies exist.
   writeSync(new File(backup,OLD_JSON),oldJson);
   File f=new File(backup,MANIFEST);
   try(FileOutputStream stream=new FileOutputStream(f);
       DataOutputStream out=new DataOutputStream(new BufferedOutputStream(stream))){
    out.writeInt(0x4d4f544f);out.writeInt(1);
    out.writeInt(old.size());for(String name:old)out.writeUTF(name);
    out.writeInt(fresh.size());for(String name:fresh)out.writeUTF(name);
    out.flush();stream.getFD().sync();
   }
   for(String name:old){
    File saved=new File(backup,name);
    if(!Files.isRegularFile(saved.toPath(),LinkOption.NOFOLLOW_LINKS)||
       Files.isSymbolicLink(saved.toPath()))throw new IOException("복원 안전 사본이 불완전합니다.");
   }
   Files.createFile(new File(backup,PENDING).toPath());
  }
  static void markCommitted(File backup)throws IOException {
   File pending=new File(backup,PENDING);
   if(!Files.isRegularFile(pending.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("복원 저널이 누락됐습니다.");
   Files.createFile(new File(backup,COMMITTED).toPath());
  }
  static int recoverAll(File internal,OriginalWriter writer)throws Exception {
   if(internal==null||!Files.isDirectory(internal.toPath(),LinkOption.NOFOLLOW_LINKS)||
      Files.isSymbolicLink(internal.toPath()))throw new IOException("앱 데이터 폴더 안전 확인 실패");
   File[] dirs=internal.listFiles(f->f.getName().matches(DIRECTORY));
   if(dirs==null)return 0;
   Arrays.sort(dirs,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
   int recovered=0;
   for(File dir:dirs){
    safeDir(internal,dir);
    File pending=new File(dir,PENDING),committed=new File(dir,COMMITTED);
    if(Files.exists(committed.toPath(),LinkOption.NOFOLLOW_LINKS)){
     // All files and JSON were committed; interrupted cleanup is safe.
     if(Files.isSymbolicLink(committed.toPath())||!Files.isRegularFile(committed.toPath(),LinkOption.NOFOLLOW_LINKS))
      throw new IOException("손상된 복원 완료 표시입니다.");
     if(Files.exists(pending.toPath(),LinkOption.NOFOLLOW_LINKS)&&
        (!Files.isRegularFile(pending.toPath(),LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(pending.toPath())))
      throw new IOException("손상된 복원 진행 표시입니다.");
     safeClean(dir);
    }else if(Files.exists(pending.toPath(),LinkOption.NOFOLLOW_LINKS)){
     if(!Files.isRegularFile(pending.toPath(),LinkOption.NOFOLLOW_LINKS)||
        Files.isSymbolicLink(pending.toPath()))throw new IOException("손상된 복원 진행 표시입니다.");
     recoverOne(internal,dir,writer);recovered++;
    }
    // A crash before PENDING is harmless: old live files were not yet touched.
   }
   return recovered;
  }
  static void recoverOne(File internal,File backup,OriginalWriter writer)throws Exception {
   safeDir(internal,backup);
   File pending=new File(backup,PENDING);
   if(Files.isSymbolicLink(pending.toPath())||
      !Files.isRegularFile(pending.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("복원 진행 표시가 없습니다.");
   File oldJson=new File(backup,OLD_JSON),manifest=new File(backup,MANIFEST);
   if(Files.isSymbolicLink(oldJson.toPath())||Files.isSymbolicLink(manifest.toPath())||
      !Files.isRegularFile(oldJson.toPath(),LinkOption.NOFOLLOW_LINKS)||
      !Files.isRegularFile(manifest.toPath(),LinkOption.NOFOLLOW_LINKS)||
      oldJson.length()>BackupFormat.MAX_JSON_BYTES||manifest.length()>12_000_000)
    throw new IOException("이전 데이터 안전 사본이 유효하지 않습니다.");
   List<String> old=new ArrayList<>(),fresh=new ArrayList<>();
   try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(manifest)))){
    if(in.readInt()!=0x4d4f544f||in.readInt()!=1)throw new IOException("복원 저널 형식 오류");
    int count=in.readInt();if(count<0||count>MAX_NAMES)throw new IOException("이전 파일 목록 초과");
    for(int i=0;i<count;i++){String path=in.readUTF();validate(path);old.add(path);}
    count=in.readInt();if(count<0||count>MAX_NAMES-old.size())throw new IOException("복원 파일 목록 초과");
    for(int i=0;i<count;i++){String path=in.readUTF();validate(path);fresh.add(path);}
    if(in.read()!=-1)throw new IOException("복원 저널 추가 데이터");
   }
   Set<String> original=new HashSet<>(old);
   if(original.size()!=old.size()||new HashSet<>(fresh).size()!=fresh.size())
    throw new IOException("중복된 복원 목록입니다.");
   // Verify EVERY old copy before deleting any newly installed file.
   for(String name:old){
    File saved=new File(backup,name),target=new File(internal,name);
    safePath(backup,saved);safePath(internal,target);
    if(!Files.isRegularFile(saved.toPath(),LinkOption.NOFOLLOW_LINKS)||
       Files.isSymbolicLink(saved.toPath()))throw new IOException("안전 사본이 누락됐습니다: "+name);
   }
   byte[] bytes=Files.readAllBytes(oldJson.toPath());
   if(bytes.length<2)throw new IOException("복원 JSON 안전 사본이 비어 있습니다.");
   // Never remove newly restored assets unless the complete rollback set is intact.
   // Validate target paths in advance so a late symlink cannot cause partial rollback.
   for(String name:fresh)safePath(internal,new File(internal,name));
   for(String name:fresh)if(!original.contains(name)){
    File target=new File(internal,name);safePath(internal,target);
    if(Files.exists(target.toPath(),LinkOption.NOFOLLOW_LINKS)){
     if(Files.isSymbolicLink(target.toPath())||!Files.isRegularFile(target.toPath(),LinkOption.NOFOLLOW_LINKS))
      throw new IOException("복원 새 파일이 안전하지 않습니다: "+name);
     Files.delete(target.toPath());
    }
   }
   for(String name:old){
    File target=new File(internal,name);safePath(internal,target);
    copy(new File(backup,name),target);
   }
   // LAST: pair original JSON with original asset state. This is idempotent across crashes.
   writer.write(bytes);
   Files.delete(pending.toPath());
   safeClean(backup);
  }
  private static void validate(String name)throws IOException {
   if(name==null||name.length()>180||name.contains("..")||name.startsWith("/")||name.contains("\\"))throw new IOException("위험한 복원 저널 경로");
   boolean photo=name.matches(BackupFormat.PHOTO),receipt=name.matches(BackupFormat.RECEIPT),
     ride=BackupFormat.isTrashAsset(name)||name.startsWith("rides/")&&(name.substring(6).matches(BackupFormat.TRACK)||
      BackupFormat.isSidecar(name)),model=name.startsWith("moto-year-photos/")&&
      (name.substring(17).matches(BackupFormat.MODEL_PHOTO)||name.substring(17).matches(BackupFormat.MODEL_META));
   if(!photo&&!receipt&&!ride&&!model)throw new IOException("지원되지 않는 복원 저널 파일: "+name);
  }
  private static void safeDir(File internal,File backup)throws IOException {
   if(!backup.getName().matches(DIRECTORY)||!backup.getParentFile().equals(internal)||
      Files.isSymbolicLink(backup.toPath())||!Files.isDirectory(backup.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("복원 저널 폴더가 안전하지 않습니다.");
  }
  private static void safePath(File root,File target)throws IOException{
   String relative=root.toPath().relativize(target.toPath()).toString().replace(File.separatorChar,'/');
   validate(relative);
   File parent=target.getParentFile();
   if(Files.isSymbolicLink(parent.toPath())||
      (Files.exists(parent.toPath(),LinkOption.NOFOLLOW_LINKS)&&
       !Files.isDirectory(parent.toPath(),LinkOption.NOFOLLOW_LINKS))||
      Files.isSymbolicLink(target.toPath()))throw new IOException("복원 대상 경로가 안전하지 않습니다.");
  }
  private static void writeSync(File file,byte[] data)throws IOException {
   try(FileOutputStream out=new FileOutputStream(file)){out.write(data);out.getFD().sync();}
  }
  private static void safeClean(File path)throws IOException {
   if(Files.isSymbolicLink(path.toPath()))throw new IOException("복원 저널 삭제 경로가 안전하지 않습니다.");
   if(Files.isDirectory(path.toPath(),LinkOption.NOFOLLOW_LINKS)){
    File[] files=path.listFiles();
    if(files==null)throw new IOException("복원 저널 폴더를 읽지 못했습니다.");
    for(File file:files)safeClean(file);
   }
   Files.deleteIfExists(path.toPath());
  }
 }

 private static boolean jpeg(File f)throws IOException{
  try(RandomAccessFile in=new RandomAccessFile(f,"r")){if(in.readUnsignedByte()!=0xff||in.readUnsignedByte()!=0xd8)return false;in.seek(in.length()-2);return in.readUnsignedByte()==0xff&&in.readUnsignedByte()==0xd9;}
 }
 private static boolean modelPhotoName(String name){return BackupFormat.isModelPhoto(name);}
 private static List<String> stagedAssets(File stage)throws IOException{
  List<String> out=existingAssets(stage);
  File folder=new File(stage,"moto-year-photos");
  if(Files.isSymbolicLink(folder.toPath()))throw new IOException("안전하지 않은 모델 사진 폴더입니다.");
  File[] modelFiles=folder.listFiles();
  if(modelFiles!=null)for(File file:modelFiles){
   String name=file.getName();
   if(name.matches(BackupFormat.MODEL_PHOTO)||name.matches(BackupFormat.MODEL_META))
    addSafeAsset(out,stage,file,"moto-year-photos/"+name);
  }
  return out;
 }
 private static List<String> oldAssets(File internal,List<String> incoming)throws IOException{
  File folder=new File(internal,"moto-year-photos");
  if(Files.isSymbolicLink(folder.toPath())||
      (Files.exists(folder.toPath(),LinkOption.NOFOLLOW_LINKS)&&
       !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("기존 모델 사진 폴더가 안전하지 않습니다.");
  List<String> old=existingAssets(internal);
  for(String path:incoming)if(path.startsWith("moto-year-photos/")){
   File file=new File(internal,path);
   if(Files.exists(file.toPath(),LinkOption.NOFOLLOW_LINKS))
    addSafeAsset(old,internal,file,path);
  }
  return old;
 }
 private static void addSafeAsset(List<String> out,File root,File file,String path)throws IOException{
  if(Files.isSymbolicLink(file.toPath())||
    !Files.isRegularFile(file.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("복원 대상에 안전하지 않은 파일이 있습니다: "+path);
  File expected=path.startsWith("rides-trash/")?new File(root,"rides-trash/"+path.split("/")[1]):
   path.startsWith("rides/")?new File(root,"rides"):
   (path.startsWith("moto-year-photos/")?new File(root,"moto-year-photos"):root);
  if(!file.getParentFile().equals(expected))
   throw new IOException("복원 파일 경로가 앱 저장소 밖입니다.");
  out.add(path);
 }
 private static List<String> existingAssets(File root)throws IOException{
  if(root==null||!Files.isDirectory(root.toPath(),LinkOption.NOFOLLOW_LINKS))
   throw new IOException("복원할 저장 공간이 안전하지 않습니다.");
  List<String> out=new ArrayList<>();
  File[] photos=root.listFiles((d,n)->n.matches(PHOTO));if(photos!=null)
   for(File f:photos)addSafeAsset(out,root,f,f.getName());
  File[] receipts=root.listFiles((d,n)->n.matches(RECEIPT));if(receipts!=null)
   for(File f:receipts)addSafeAsset(out,root,f,f.getName());
  File rides=new File(root,"rides");
  if(Files.isSymbolicLink(rides.toPath())||
    (rides.exists()&&!Files.isDirectory(rides.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("복원 대상 GPS 폴더가 안전하지 않습니다.");
  File[] files=rides.listFiles();if(files!=null)for(File f:files){
   String n=f.getName();
   if(n.matches(RIDE)||n.matches(RIDE+"\\.done")||n.matches(RIDE+"\\.imported")||
      n.matches(RIDE+"\\.recovered")||n.matches(RIDE+"\\.mergedinto")||
      n.matches(RIDE+"\\.unmerged")||n.matches(RIDE+"\\.category")||n.matches(RIDE+"\\.bike")||n.matches(RIDE+"\\.favorite"))
    addSafeAsset(out,root,f,"rides/"+n);
  }
  File trash=new File(root,"rides-trash");
  if(Files.isSymbolicLink(trash.toPath())||
      (trash.exists()&&!Files.isDirectory(trash.toPath(),LinkOption.NOFOLLOW_LINKS)))
   throw new IOException("복구함 경로 안전 검증 실패");
  File[] folders=trash.listFiles();
  if(folders!=null)for(File folder:folders){
   if(!folder.getName().matches(BackupFormat.TRACK)||
      Files.isSymbolicLink(folder.toPath())||
      !Files.isDirectory(folder.toPath(),LinkOption.NOFOLLOW_LINKS))
    throw new IOException("복구함에 확인되지 않은 폴더가 있습니다.");
   File[] markers=folder.listFiles();
   if(markers!=null)for(File file:markers){
    String name="rides-trash/"+folder.getName()+"/"+file.getName();
    if(!BackupFormat.isTrashAsset(name))throw new IOException("복구함 파일 형식 오류: "+name);
    addSafeAsset(out,root,file,name);
   }
  }
  return out;
 }
 private static boolean sameContent(File a,File b)throws IOException{
  if(a.length()!=b.length())return false;
  try(InputStream x=new BufferedInputStream(new FileInputStream(a));InputStream y=new BufferedInputStream(new FileInputStream(b))){
   byte[] left=new byte[16384],right=new byte[16384];int n;
   while((n=x.read(left))!=-1){int done=0;while(done<n){int read=y.read(right,done,n-done);if(read<0)return false;done+=read;}
    for(int i=0;i<n;i++)if(left[i]!=right[i])return false;
   }
   return y.read()==-1;
  }
 }
 private static void copy(File source,File dest)throws IOException{
  if(!dest.getParentFile().isDirectory()&&!dest.getParentFile().mkdirs())throw new IOException("복원 폴더를 만들지 못했어요.");
  File temp=new File(dest.getParentFile(),dest.getName()+".restoring");
  try{Files.copy(source.toPath(),temp.toPath(),StandardCopyOption.REPLACE_EXISTING);Files.move(temp.toPath(),dest.toPath(),StandardCopyOption.REPLACE_EXISTING);}finally{temp.delete();}
 }
 /** Never follow links encountered while discarding temporary restore directories.
  * A compromised staged link must not delete external files during cleanup. */
 private static void clean(File path){
  if(path==null)return;
  if(Files.isDirectory(path.toPath(),LinkOption.NOFOLLOW_LINKS)
      &&!Files.isSymbolicLink(path.toPath())){
   File[] children=path.listFiles();
   if(children!=null)for(File child:children)clean(child);
  }
  path.delete();
 }
}
