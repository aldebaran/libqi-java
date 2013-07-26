package com.aldebaran.qimessaging;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Integration test for QiMessaging java bindings.
 */
public class DetailsTest
{

  /**
   * Using EmbeddedTools class, get suitable extension for given platform.
   */
  @Test
  public void testGetSuitableExtention()
  {
    String os = System.getProperty("os.name");

    if (os.compareTo("Windows") == 0)
      assertTrue("Windows extension is .dll", EmbeddedTools.getSuitableLibraryExtention().compareTo(".dll") == 0);
    else if (os.compareTo("Mac") == 0)
      assertTrue("Mac extension is .dylib", EmbeddedTools.getSuitableLibraryExtention().compareTo(".dylib") == 0);
    else
      assertTrue("Unix extension is .so", EmbeddedTools.getSuitableLibraryExtention().compareTo(".so") == 0);
  }

}
