package com.motohub.next;
import org.junit.*;import org.junit.runner.RunWith;import static org.junit.Assert.*;import org.robolectric.*;import org.robolectric.annotation.*;import org.json.*;import java.io.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35)
public class SocialClientTest {
 @Test public void validatesEmailAndPassword(){assertThrows(IOException.class,()->SocialClient.credentials("invalid","12345678"));assertThrows(IOException.class,()->SocialClient.credentials("a@b.com","short"));}
 @Test public void rejectsUnsafeEndpoint(){for(String url:new String[]{"http://x.com","https://u:p@x.com","https://x.com/auth","https://x.com?q=a"})assertThrows(Exception.class,()->SocialClient.base(url));}
 @Test public void sessionRequiresTokensAndIdentity(){SocialClient c=new SocialClient();assertThrows(IOException.class,()->c.acceptSession(new JSONObject()));assertEquals("",c.uid);}
 @Test public void logoutClearRemovesPrivateState()throws Exception{SocialClient c=new SocialClient();c.acceptSession(new JSONObject().put("access_token","TEST").put("refresh_token","TEST").put("user",new JSONObject().put("id","test-id")));c.clear();assertEquals("",c.access);assertEquals("",c.refresh);assertEquals("",c.uid);assertEquals(0,c.requests.length());}
 @Test public void noServerNeverPretendsLoginSucceeded(){Assume.assumeFalse(SocialClient.configured());SocialClient c=new SocialClient();assertThrows(IOException.class,()->c.login("test@example.com","password123"));assertEquals("",c.uid);}
 @Test public void invalidFriendCodeDoesNotSendRequest(){SocialClient c=new SocialClient();assertThrows(IOException.class,()->c.invite("not-a-code"));}
}
