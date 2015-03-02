package com.aldebaran.qi;

import com.aldebaran.qi.ServiceDirectory;
import com.aldebaran.qi.Session;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Integration test for QiMessaging java bindings.
 */
public class ClientTest
{

  /**
   * Create a QiMessaging session,
   * Connect to Service Directory,
   */
  @Test
  public void testConnection()
  {
    System.out.println("testConnection");
    ServiceDirectory sd = new ServiceDirectory();
    Session s = new Session();

    // Get Service directory listening url.
    String url = sd.listenUrl();

    // Try to connect session to service directory.
    try {
      s.connect(url).sync();
    } catch (Exception e)
    {
      fail("Connection to service directory must succeed : "+e.getMessage());
    }
  }

  /**
   * Create a QiMessaging session,
   * Connect to Service Directory,
   * Close session,
   * Close service directory.
   */
  @Test
  public void testDisconnection()
  {
    System.out.println("testDisconnection");
    ServiceDirectory sd = new ServiceDirectory();
    Session s = new Session();

    // Get Service directory listening url.
    String url = sd.listenUrl();

    // Try to connect session to service directory.
    try {
      s.connect(url).sync();
    } catch (Exception e)
    {
      System.out.printf("Cannot connect to service directory (%s) : %s", url, e.getMessage());
      assert(false);
    }

    s.close();
  }

  /**
   * Test Session constructor
   */
  @Test
  public void testSessionConstructor()
  {
    System.out.println("testSessionConstructor");
    ServiceDirectory sd = new ServiceDirectory();
    Session s = null;

    try {
      s = new Session(sd.listenUrl());
    }
    catch (Exception e)
    {
      fail("Session must be connected to ServiceDirectory");
    }

    assertTrue("Session must be connected to ServiceDirectory", s.isConnected());
  }
}
