package com.motohub.next;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Opt-in session persistence encrypted with hardware-backed AndroidKeyStore when available. */
final class SessionVault {
 private static final String ALIAS="MOTO_HUB_SOCIAL_SESSION_V1",PREFS="moto_session_v1";
 private SessionVault(){}
 static SecretKey secret()throws Exception{
  KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
  if(store.containsAlias(ALIAS))return (SecretKey)store.getKey(ALIAS,null);
  KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
  generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());
  return generator.generateKey();
 }
 static void save(Context context,String json)throws Exception{
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,secret());
  String iv=android.util.Base64.encodeToString(cipher.getIV(),android.util.Base64.NO_WRAP);
  String ciphertext=android.util.Base64.encodeToString(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)),android.util.Base64.NO_WRAP);
  SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  if(!prefs.edit().putString("iv",iv).putString("data",ciphertext).commit())throw new IllegalStateException("로그인 정보 저장 실패");
 }
 static String read(Context context)throws Exception{
  SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
  String iv=prefs.getString("iv",null),encrypted=prefs.getString("data",null);
  if(iv==null||encrypted==null)return "";
  Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
  cipher.init(Cipher.DECRYPT_MODE,secret(),new GCMParameterSpec(128,android.util.Base64.decode(iv,android.util.Base64.DEFAULT)));
  return new String(cipher.doFinal(android.util.Base64.decode(encrypted,android.util.Base64.DEFAULT)),StandardCharsets.UTF_8);
 }
 static void clear(Context c){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().clear().apply();}
}
