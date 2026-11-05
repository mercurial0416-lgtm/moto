package com.motohub.next;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=35)
public class TripAndFeatureWriteTest {
 Context context;File track;JSONObject db;
 @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();track=new File(TrackFile.directory(context.getFilesDir()),"1760000000000-11111111-2222-4333-8444-555555555555.track");Files.write(track.toPath(),(TrackFile.point(37.5,127,5,1760000000000L)+TrackFile.point(37.501,127.001,5,1760000060000L)).getBytes(StandardCharsets.UTF_8));assertTrue(new File(track.getPath()+".done").createNewFile());db=Store.empty();}
 @Test public void tripStopsAndTitleEditsRoundTripWithoutChangingGps()throws Exception{
  byte[] original=Files.readAllBytes(track.toPath());RideNotes.update(db,track,"Tour","Original memo");JSONObject stop=TripNotes.stop("휴식","Coffee","rest",37.5,127.0);TripNotes.append(db,track,stop);RideNotes.update(db,track,"Updated","New memo");assertEquals(1,TripNotes.stops(db,track).length());assertEquals("Updated",RideNotes.title(db,track));assertEquals(1,TripNotes.stops(Store.parse(db.toString()),track).length());RideNotes.update(db,track,"","");assertEquals(1,TripNotes.stops(db,track).length());assertArrayEquals(original,Files.readAllBytes(track.toPath()));
 }
 @Test public void deletingOneStopPreservesOtherMetadataAndGps()throws Exception{
  JSONObject first=TripNotes.stop("A","","rest",null,null),second=TripNotes.stop("B","","visit",37.5,127.0);TripNotes.append(db,track,first);TripNotes.append(db,track,second);RideNotes.update(db,track,"Keep","Memo");byte[] gps=Files.readAllBytes(track.toPath());TripNotes.remove(db,track,first.getString("id"));assertEquals(second.getString("id"),TripNotes.stops(db,track).getJSONObject(0).getString("id"));assertEquals("Keep",RideNotes.title(db,track));assertArrayEquals(gps,Files.readAllBytes(track.toPath()));
 }
 @Test public void invalidStopsAndUnfinishedOrSymlinkedTracksAreRejected()throws Exception{
  assertThrows(IllegalArgumentException.class,()->TripNotes.stop("","","rest",null,null));assertThrows(IllegalArgumentException.class,()->TripNotes.stop("A","","rest",Double.NaN,127.0));assertThrows(IllegalArgumentException.class,()->TripNotes.stop("A","","rest",37.0,null));assertThrows(IllegalArgumentException.class,()->TripNotes.stop("A","","arbitrary",null,null));
  assertTrue(new File(track.getPath()+".done").delete());assertThrows(IllegalArgumentException.class,()->TripNotes.append(db,track,TripNotes.stop("A","","rest",null,null)));assertTrue(new File(track.getPath()+".done").createNewFile());File link=new File(track.getParentFile(),"1760000000001-11111111-2222-4333-8444-555555555555.track");Files.createSymbolicLink(link.toPath(),track.toPath());new File(link.getPath()+".done").createNewFile();assertThrows(IllegalArgumentException.class,()->TripNotes.append(db,link,TripNotes.stop("A","","rest",null,null)));
 }
 @Test public void duplicateStopIdsCannotCreateAmbiguousDeletion()throws Exception{
  JSONObject stop=TripNotes.stop("Stop","","rest",null,null);TripNotes.append(db,track,stop);assertThrows(IllegalArgumentException.class,()->TripNotes.append(db,track,stop));TripNotes.stops(db,track).put(new JSONObject(stop.toString()));String before=db.toString();assertThrows(IllegalArgumentException.class,()->TripNotes.remove(db,track,stop.getString("id")));assertEquals(before,db.toString());
 }
 @Test public void mapExportIsBoundedAndDropsMalformedImportedStops()throws Exception{
  for(int i=0;i<TripNotes.MAX_STOPS;i++)TripNotes.append(db,track,TripNotes.stop("Stop "+i,"","visit",37.5,127.0));assertThrows(IllegalArgumentException.class,()->TripNotes.append(db,track,TripNotes.stop("Overflow","","visit",37.5,127.0)));JSONArray stops=TripNotes.stops(db,track);stops.getJSONObject(0).put("name","x".repeat(10000));JSONArray safe=TripNotes.mapStops(db,track);assertEquals(63,safe.length());assertTrue(safe.toString().length()<100000);
 }
 @Test public void asynchronousWriterCorePreservesBeforeAndAfterSnapshots()throws Exception{
  AtomicFile disk=new AtomicFile(new File(context.getFilesDir(),"writer-test.json"));JSONObject next=new JSONObject(db.toString());next.getJSONObject("settings").put("tripTest",true);FeatureRecordWriter.Result result=FeatureRecordWriter.persist(context,disk,db.toString(),next.toString());assertTrue(result.data.getJSONObject("settings").getBoolean("tripTest"));assertTrue(Store.parse(new String(disk.readFully(),StandardCharsets.UTF_8)).getJSONObject("settings").getBoolean("tripTest"));assertTrue(LocalSafetyBackup.available(context).length>0);assertFalse(LocalSafetyBackup.read(LocalSafetyBackup.available(context)[0]).getJSONObject("settings").has("tripTest"));
 }
 @Test public void invalidWriterPayloadNeverReplacesAnExistingPrimary()throws Exception{
  AtomicFile disk=new AtomicFile(new File(context.getFilesDir(),"writer-test.json"));FeatureRecordWriter.persist(context,disk,db.toString(),db.toString());byte[] before=disk.readFully();assertThrows(Exception.class,()->FeatureRecordWriter.persist(context,disk,db.toString(),"{bad json}"));assertArrayEquals(before,disk.readFully());
 }
}
