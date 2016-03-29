/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

/**
 * Tool class providing QiMessaging<->Java type system loading and
 * dynamic library loader designed to load libraries included in jar package.
 * @author proullon
 *
 */
public class EmbeddedTools
{

  private File tmpDir = null;

  public static boolean LOADED_EMBEDDED_LIBRARY = false;
  private static native void initTypeSystem(Object str, Object i, Object f, Object d, Object l, Object m, Object al, Object t, Object o, Object b, Object fut);

  /**
   * To work correctly, QiMessaging<->java type system needs to compare type class template.
   * Unfortunately, call template cannot be retrieve on native android thread.
   * The only available way to do is to store instance of wanted object
   * and get fresh class template from it right before using it.
   */
  private boolean initTypeSystem()
  {
    String  str = new String();
    Integer i   = new Integer(0);
    Float   f   = new Float(0);
    Double  d   = new Double(0);
    Long    l   = new Long(0);
    Tuple   t   = new Tuple(0);
    Boolean b   = new Boolean(true);
    Future<Object> fut = new Future<Object>(0);

    DynamicObjectBuilder ob = new DynamicObjectBuilder();
    AnyObject obj  = ob.object();

    Map<Object, Object> m  = new Hashtable<Object, Object>();
    ArrayList<Object>             al = new ArrayList<Object>();

    // Initialize generic type system
    EmbeddedTools.initTypeSystem(str, i, f, d, l, m, al, t, obj, b, fut);

    return true;
  }

  /**
   * Override directory where native libraries are extracted.
   */
  public void overrideTempDirectory(File newValue)
  {
    SharedLibrary.overrideTempDirectory(newValue);

  }

  /**
   * Native C++ libraries are packaged with java sources.
   * This way, we are able to load them anywhere, anytime.
   */
  public boolean loadEmbeddedLibraries()
  {
    if (LOADED_EMBEDDED_LIBRARY == true)
    {
      System.out.print("Native libraries already loaded");
      return true;
    }

    String osName = System.getProperty("os.name");

    String javaVendor = System.getProperty("java.vendor");
    if (javaVendor.contains("Android"))
    {
      // Using System.loadLibrary will find the libraries automatically depending on the platform,
      // but we still need to load the dependencies manually and in the correct order.
      System.loadLibrary("gnustl_shared");
      System.loadLibrary("qi");
      System.loadLibrary("qimessagingjni");
    }
    else
    {
      // Windows is built with static boost libs,
      // but the name of the SSL dlls are different
      if (osName.contains("Windows"))
      {
        // Load vcredist libs
        SharedLibrary.loadLib("msvcr120");
        SharedLibrary.loadLib("msvcp120");
        SharedLibrary.loadLib("libeay32");
        SharedLibrary.loadLib("ssleay32");
      }
      else
      {
        SharedLibrary.loadLib("crypto");
        SharedLibrary.loadLib("ssl");
        SharedLibrary.loadLib("icudata"); // deps for boost_regexp.so
        SharedLibrary.loadLib("icuuc");
        SharedLibrary.loadLib("icui18n");
        SharedLibrary.loadLib("boost_atomic");
        SharedLibrary.loadLib("boost_date_time");
        SharedLibrary.loadLib("boost_system");
        SharedLibrary.loadLib("boost_thread");
        SharedLibrary.loadLib("boost_chrono");
        SharedLibrary.loadLib("boost_locale");
        SharedLibrary.loadLib("boost_filesystem");
        SharedLibrary.loadLib("boost_program_options");
        SharedLibrary.loadLib("boost_regex");
      } // linux, mac

      // Not on android, need to load qi and qimessagingjni
      if (SharedLibrary.loadLib("qi") == false
              || SharedLibrary.loadLib("qimessagingjni") == false)
      {
        LOADED_EMBEDDED_LIBRARY = false;
        return false;
      }
    }

    System.out.printf("Libraries loaded. Initializing type system...\n");
    LOADED_EMBEDDED_LIBRARY = true;
    if (initTypeSystem() == false)
    {
      System.out.printf("Cannot initialize type system\n");
      LOADED_EMBEDDED_LIBRARY = false;
      return false;
    }

    return true;
  }

}
