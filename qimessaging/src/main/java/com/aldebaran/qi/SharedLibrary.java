/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class SharedLibrary
{
  private static String osName = System.getProperty("os.name");
  private static String tmpDir = System.getProperty("java.io.tmpdir");

  public static String getLibraryName(String name)
  {
    return getPrefix() + name + getSuffix();
  }

  public static void overrideTempDirectory(File newValue)
  {
    tmpDir = newValue.getAbsolutePath();
  }

  public static boolean loadLib(String name)
  {
    boolean firstTry = false;
    String libName = getLibraryName(name);
    firstTry = loadLibHelper(libName);
    if (firstTry)
    {
      return true;
    }
    // Try in debug too on windows:
    if (!osName.startsWith("Windows"))
    {
      return firstTry;
    }
    String debugLibName = getLibraryName(name + "_d");
    boolean secondTry = loadLibHelper(debugLibName);
    return secondTry;
  }

  private static boolean loadLibHelper(String libraryName)
  {
    System.out.format("Loading %s\n", libraryName);
    String resourcePath = "/" + libraryName;
    URL libraryUrl = SharedLibrary.class.getResource(resourcePath);
    if (libraryUrl == null)
    {
      System.out.format("No such resource %s\n", resourcePath);
      return false;
    }

    String libPath = libraryUrl.getPath();
    File libFile = new File(libPath);
    if (libFile.exists())
    {
      // first, try as a real file
      System.out.format("Loading %s from filesystem\n", libraryUrl);
      System.load(libPath);
      return true;
    }
    else
    {
      // then, try as a file in the jar
      return extractAndLoad(libraryUrl, libraryName);
    }
  }

  private static boolean extractAndLoad(URL libraryUrl, String libraryName)
  {

    String tmpLibraryPath = tmpDir + File.separator + libraryName;
    File libraryFile = new File(tmpLibraryPath);
    if (libraryFile.exists())
    {
      libraryFile.delete();
    }
    // re-open it
    libraryFile = new File(tmpLibraryPath);
    libraryFile.deleteOnExit();
    InputStream in;
    OutputStream out;
    try
    {
      in = libraryUrl.openStream();
    }
    catch (IOException e)
    {
      System.out.format("Could not open %s for reading. Error was: %s\n",
          libraryUrl, e.getMessage());
      return false;
    }
    try
    {
      out = new BufferedOutputStream(new FileOutputStream(libraryFile));
    }
    catch (IOException e)
    {
      System.out.format("Could not open %s for writing. Error was: %s\n",
          libraryFile, e.getMessage());
      return false;
    }

    try
    {
      // Extract it in a temporary file
      int len = 0;
      byte[] buffer = new byte[10000];
      while ((len = in.read(buffer)) > -1)
      {
        out.write(buffer, 0, len);
      }

      // Close files
      out.close();
      in.close();
    }
    catch (IOException e)
    {
      System.out.format("Could not copy from %s to %s: %s\n", libraryUrl,
          tmpLibraryPath, e.getMessage());
      return false;
    }

    System.out.format("Loading: %s \n", tmpLibraryPath);
    System.load(tmpLibraryPath);

    return true;
  }

  private static String getPrefix()
  {
    if (osName.startsWith("Linux") || osName.startsWith("Mac"))
    {
      return "lib";
    }
    return "";
  }

  private static String getSuffix()
  {
    if (osName.startsWith("Linux"))
    {
      return ".so";
    }
    if (osName.startsWith("Mac"))
    {
      return ".dylib";
    }
    if (osName.startsWith("Windows"))
    {
      return ".dll";
    }
    // Android, atom, ....
    return ".so";
  }

}
