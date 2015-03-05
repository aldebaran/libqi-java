/*
**  Copyright (C) 2015 Aldebaran Robotics
**  See COPYING for the license
*/
package com.aldebaran.qi;

public class RawApplication
{

  static
  {
    // Loading native C++ libraries.
    if (!EmbeddedTools.LOADED_EMBEDDED_LIBRARY)
    {
      EmbeddedTools loader = new EmbeddedTools();
      loader.loadEmbeddedLibraries();
    }
  }

  // Native function
  private static native long qiApplicationCreate(String[] args);
  private static native void qiApplicationRun(long pApp);
  private static native void qiApplicationStop(long pApp);
  private static native void qiApplicationDestroy(long pApplication);

  // Members
  private long _application;

  /**
   * RawApplication constructor.
   * @param args Arguments given to main() function.
   * @param defaultUrl Default url to connect to if none was provided in the
   * program arguments
   * @param listen If no argument was provided, will start a listening session
   * with a ServiceDirectory (this argument is ignored for the moment)
   */
  public RawApplication(String[] args)
  {
    if (args == null)
      throw new NullPointerException("Creating application with null args");
    _application = RawApplication.qiApplicationCreate(args);
  }

  /**
   * Stop RawApplication eventloops and calls atStop() callbacks.
   * @since 1.20
   */
  public void stop()
  {
    RawApplication.qiApplicationStop(_application);
  }

  /**
   * Blocking function. RawApplication.run() join eventloop thread.
   * Return when :
   * - Eventloop is stopped.
   * - RawApplication.stop() is called
   * @since 1.20
   */
  public void run()
  {
    RawApplication.qiApplicationRun(_application);
  }

}
