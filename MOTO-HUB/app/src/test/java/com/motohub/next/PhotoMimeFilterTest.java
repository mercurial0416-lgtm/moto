package com.motohub.next;

import org.junit.Test;
import static org.junit.Assert.*;

public class PhotoMimeFilterTest {
 @Test public void acceptsBitmapCompatibleFormats() {
  assertTrue(PhotoMimeFilter.supported("image/jpeg"));
  assertTrue(PhotoMimeFilter.supported("image/png"));
  assertTrue(PhotoMimeFilter.supported("image/webp"));
  assertTrue(PhotoMimeFilter.supported(" IMAGE/JPEG "));
 }
 @Test public void rejectsUnsupportedCommonsFormats() {
  assertFalse(PhotoMimeFilter.supported("image/svg+xml"));
  assertFalse(PhotoMimeFilter.supported("image/tiff"));
  assertFalse(PhotoMimeFilter.supported("application/pdf"));
  assertFalse(PhotoMimeFilter.supported("image/gif"));
  assertFalse(PhotoMimeFilter.supported(null));
 }
}
