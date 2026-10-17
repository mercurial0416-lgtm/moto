package com.motohub.next;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.OpenableColumns;import java.io.*;
/** Grants read access to one hash-named APK only. Never exposes local records. */
public final class UpdateApkProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 File file(Uri uri)throws FileNotFoundException{String name=uri.getLastPathSegment();if(!getContext().getPackageName().concat(".updates").equals(uri.getAuthority())||!("/apk/"+name).equals(uri.getPath())||name==null||!name.matches("[a-f0-9]{64}\\.apk"))throw new FileNotFoundException("Unknown update");return new File(new File(getContext().getCacheDir(),"updates"),name);}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public String getType(Uri u){return "application/vnd.android.package-archive";}
 @Override public Cursor query(Uri u,String[] projection,String selection,String[] args,String order){try{File f=file(u);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"MOTO-HUB-update.apk":OpenableColumns.SIZE.equals(columns[i])?f.length():null;c.addRow(row);return c;}catch(FileNotFoundException e){throw new IllegalArgumentException(e);}}
 @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}@Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}@Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
